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

package com.quangthe.photoqt.gallery.albums.detail.ui

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.detail.ui.AlbumDetailNavigator.NavigationEvent.ShowToast
import com.quangthe.photoqt.gallery.albums.domain.AlbumRepository
import com.quangthe.photoqt.gallery.albums.domain.model.Album
import com.quangthe.photoqt.gallery.components.GalleryViewMode
import com.quangthe.photoqt.gallery.components.ImportChoice
import com.quangthe.photoqt.gallery.components.PhotoTile
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction.DeletePhotos
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction.ExportPhotos
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction.OpenPhoto
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.sort.domain.SortRepository
import com.quangthe.photoqt.settings.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val ALBUM_DETAIL_UUID = "album_uuid"

@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
class AlbumDetailViewModel @AssistedInject constructor(
    @Assisted(ALBUM_DETAIL_UUID) private val albumUUID: String,
    private val albumsRepository: AlbumRepository,
    private val sortRepository: SortRepository,
    private val resources: Resources,
    private val config: Config,
) : ViewModel() {

    private val sortFlow = sortRepository.observeSortFor(albumUuid = albumUUID, default = SortConfig.Album.default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val albumFlow = sortFlow.flatMapLatest { sort ->
        albumsRepository.observeAlbumWithPhotos(albumUUID, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Album.Placeholder)

    private val pinnedPhotoIdsFlow = albumsRepository.observePinnedPhotoUUIDs(albumUUID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    private val viewModeFlow = MutableStateFlow(config.galleryViewMode)

    private val photoActionsChannel = Channel<PhotoAction>()
    val photoActions = photoActionsChannel.receiveAsFlow()

    val uiState = combine(
        albumFlow,
        sortFlow,
        viewModeFlow,
        pinnedPhotoIdsFlow,
    ) { album, sort, viewMode, pinnedIds ->
        AlbumDetailUiState(
            albumId = album.uuid,
            albumName = album.name,
            photos = album.files.map {
                PhotoTile(
                    it.internalThumbnailFileName,
                    it.type,
                    it.uuid,
                    pinned = it.uuid in pinnedIds,
                    size = it.size,
                    importedAt = it.importedAt,
                )
            },
            sort = sort,
            viewMode = viewMode,
            pinnedPhotoIds = pinnedIds,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AlbumDetailUiState())


    private val navEventsChannel = Channel<AlbumDetailNavigator.NavigationEvent>()
    val navEvents = navEventsChannel.receiveAsFlow()

    fun handleUiEvent(event: AlbumDetailUiEvent) {
        when (event) {
            is AlbumDetailUiEvent.OnDelete -> {
                photoActionsChannel.trySend(DeletePhotos(event.items))
            }

            is AlbumDetailUiEvent.OnExport -> {
                if (event.target != null) {
                    photoActionsChannel.trySend(ExportPhotos(event.items, event.target))
                }
            }

            is AlbumDetailUiEvent.OpenPhoto -> {
                photoActionsChannel.trySend(
                    OpenPhoto(
                        event.item.uuid,
                        albumFlow.value.uuid
                    )
                )
            }

            AlbumDetailUiEvent.DeleteAlbum -> {
                viewModelScope.launch {
                    albumsRepository.deleteAlbum(albumFlow.value)
                        .onSuccess {
                            navEventsChannel.trySend(
                                ShowToast(
                                    resources.getString(R.string.gallery_albums_deleted)
                                )
                            )
                            navEventsChannel.trySend(AlbumDetailNavigator.NavigationEvent.Close)
                        }
                        .onFailure {
                            navEventsChannel.trySend(
                                ShowToast(
                                    resources.getString(R.string.common_error)
                                )
                            )
                        }
                }
            }

            is AlbumDetailUiEvent.RemoveFromAlbum -> {
                viewModelScope.launch {
                    albumsRepository.unlink(event.items, albumFlow.value.uuid)
                    navEventsChannel.trySend(
                        ShowToast(
                            resources.getString(R.string.common_ok)
                        )
                    )
                }
            }

            is AlbumDetailUiEvent.RenameAlbum -> renameAlbum(event.newName)
            is AlbumDetailUiEvent.OnImportChoice -> onImportChoice(event.choice)
            is AlbumDetailUiEvent.SortChanged -> viewModelScope.launch {
                sortRepository.updateSortFor(albumUuid = albumUUID, sort = event.sort)
            }
            is AlbumDetailUiEvent.ViewModeChanged -> {
                viewModeFlow.value = event.viewMode
                config.galleryViewMode = event.viewMode
            }
            is AlbumDetailUiEvent.SetPinned -> viewModelScope.launch {
                albumsRepository.setPinned(event.items, albumFlow.value.uuid, event.pinned)
            }
        }
    }

    private fun onImportChoice(choice: ImportChoice) {
        val navEvent = when (choice) {
            is ImportChoice.AddNewFiles -> AlbumDetailNavigator.NavigationEvent.StartImport(
                fileUris = choice.fileUris,
                albumUuid = albumFlow.value.uuid,
            )
            is ImportChoice.AddNewFilesWithAlbum -> AlbumDetailNavigator.NavigationEvent.StartImport(
                fileUris = choice.fileUris,
                albumUuid = choice.albumUuid,
            )
            is ImportChoice.RestoreBackup -> AlbumDetailNavigator.NavigationEvent.StartRestoreBackup(
                choice.backupUri,
            )
        }

        navEventsChannel.trySend(navEvent)
    }

    private fun renameAlbum(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            albumsRepository.rename(
                albumUUID = albumFlow.value.uuid,
                newName = newName,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted(ALBUM_DETAIL_UUID) albumUUID: String): AlbumDetailViewModel
    }
}
