/*
 *   Copyright 2020–2026 Leon Latsch
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.leonlatsch.photoqt.gallery.ui

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.leonlatsch.photoqt.R
import dev.leonlatsch.photoqt.gallery.albums.domain.AlbumRepository
import dev.leonlatsch.photoqt.gallery.albums.domain.model.SmartCollectionType
import dev.leonlatsch.photoqt.gallery.components.ImportChoice
import dev.leonlatsch.photoqt.gallery.components.PhotoTile
import dev.leonlatsch.photoqt.gallery.ui.importing.SharedUrisStore
import dev.leonlatsch.photoqt.gallery.ui.navigation.GalleryNavigationEvent
import dev.leonlatsch.photoqt.gallery.ui.navigation.PhotoAction
import dev.leonlatsch.photoqt.model.repositories.ImportSource
import dev.leonlatsch.photoqt.model.repositories.PhotoRepository
import dev.leonlatsch.photoqt.sort.domain.SortConfig
import dev.leonlatsch.photoqt.sort.domain.SortRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val galleryUiStateFactory: GalleryUiStateFactory,
    private val sortRepository: SortRepository,
) : ViewModel() {

    private val sortFlow = sortRepository.observeSortFor(albumUuid = null, default = SortConfig.Gallery.default)

    private val showUnassignedOnly = MutableStateFlow(false)

    private val selectedSmartCollection = MutableStateFlow<SmartCollectionType?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val photosFlow = combine(sortFlow, showUnassignedOnly, selectedSmartCollection) { sort, unassigned, collection ->
        Triple(sort, unassigned, collection)
    }.flatMapLatest { (sort, unassigned, collection) ->
        when {
            unassigned -> photoRepository.observeUnassigned(sort)
            collection != null -> when (collection) {
                SmartCollectionType.AllPhotos -> photoRepository.observeAll(sort)
                SmartCollectionType.Favorites -> photoRepository.observeFavorites(sort)
                SmartCollectionType.Videos -> photoRepository.observeVideos(sort)
                SmartCollectionType.Photos -> photoRepository.observeImages(sort)
                SmartCollectionType.RecentlyAdded -> photoRepository.observeRecentlyAdded(sort)
                SmartCollectionType.Trash -> photoRepository.observeTrashed(sort)
            }
            else -> photoRepository.observeAll(sort)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val showAlbumSelectionDialog = MutableStateFlow(false)

    val uiState: StateFlow<GalleryUiState> = combine(
        photosFlow,
        showAlbumSelectionDialog,
        sortFlow,
        showUnassignedOnly,
        selectedSmartCollection,
    ) { photos, showAlbumSelection, sort, unassigned, collection ->
        galleryUiStateFactory.create(photos, showAlbumSelection, sort, unassigned, collection)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), GalleryUiState.Empty)

    private val eventsChannel = Channel<GalleryNavigationEvent>()
    val eventsFlow = eventsChannel.receiveAsFlow()

    private val photoActionsChannel = Channel<PhotoAction>()
    val photoActions = photoActionsChannel.receiveAsFlow()

    fun handleUiEvent(event: GalleryUiEvent) {
        when (event) {
            is GalleryUiEvent.OpenPhoto -> navigateToPhoto(event.item)
            is GalleryUiEvent.OnDelete -> onDeleteSelectedItems(event.items)
            is GalleryUiEvent.OnExport -> onExportSelectedItems(event.items, event.target)
            is GalleryUiEvent.OnAddToAlbum -> showAlbumSelectionDialog.value = true
            GalleryUiEvent.CancelAlbumSelection -> showAlbumSelectionDialog.value = false
            is GalleryUiEvent.OnImportChoice -> onImportChoice(event.choice)
            is GalleryUiEvent.SortChanged -> viewModelScope.launch {
                sortRepository.updateSortFor(albumUuid = null, sort = event.sort)
            }
            GalleryUiEvent.ToggleUnassignedFilter -> {
                showUnassignedOnly.value = !showUnassignedOnly.value
                selectedSmartCollection.value = null
            }
            is GalleryUiEvent.SelectSmartCollection -> {
                selectedSmartCollection.value = event.type
                showUnassignedOnly.value = false
            }
            is GalleryUiEvent.RemoveFromFavorites -> viewModelScope.launch {
                event.photoUuids.forEach { uuid ->
                    photoRepository.toggleFavorite(uuid, false)
                }
            }
            is GalleryUiEvent.AddToFavorites -> viewModelScope.launch {
                event.photoUuids.forEach { uuid ->
                    photoRepository.toggleFavorite(uuid, true)
                }
            }
        }
    }

    fun openSmartCollection(type: SmartCollectionType) {
        eventsChannel.trySend(GalleryNavigationEvent.OpenSmartCollection(type))
    }

    private fun onImportChoice(choice: ImportChoice) {
        val navEvent = when (choice) {
            is ImportChoice.AddNewFiles -> GalleryNavigationEvent.StartImport(
                fileUris = choice.fileUris,
                importSource = ImportSource.InApp,
            )
            is ImportChoice.AddNewFilesWithAlbum -> GalleryNavigationEvent.StartImport(
                fileUris = choice.fileUris,
                importSource = ImportSource.InApp,
                albumUuid = choice.albumUuid,
            )
            is ImportChoice.RestoreBackup -> GalleryNavigationEvent.StartRestoreBackup(choice.backupUri)
        }

        eventsChannel.trySend(navEvent)
    }

    private fun onExportSelectedItems(selectedItems: List<String>, target: Uri?) {
        target ?: return
        photoActionsChannel.trySend(
            PhotoAction.ExportPhotos(
                photosFlow.value.filter { selectedItems.contains(it.uuid) },
                target,
            )
        )
    }

    private fun onDeleteSelectedItems(selectedItems: List<String>) {
        photoActionsChannel.trySend(
            PhotoAction.DeletePhotos(
                photosFlow.value.filter { selectedItems.contains(it.uuid) }
            )
        )
    }

    private fun navigateToPhoto(item: PhotoTile) {
        photoActionsChannel.trySend(PhotoAction.OpenPhoto(item.uuid))
    }
}

