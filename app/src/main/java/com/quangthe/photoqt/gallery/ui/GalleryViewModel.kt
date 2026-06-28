package com.quangthe.photoqt.gallery.ui


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.domain.AlbumRepository
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.gallery.albums.ui.compose.AlbumItem
import com.quangthe.photoqt.gallery.components.ImportChoice
import com.quangthe.photoqt.gallery.components.PhotoTile
import com.quangthe.photoqt.gallery.ui.importing.SharedUrisStore
import com.quangthe.photoqt.gallery.ui.navigation.GalleryNavigationEvent
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction
import com.quangthe.photoqt.model.repositories.ImportSource
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.sort.domain.SortRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val galleryUiStateFactory: GalleryUiStateFactory,
    private val sortRepository: SortRepository,
) : ViewModel() {

    private val sortFlow = sortRepository.observeSortFor(albumUuid = null, default = SortConfig.Gallery.default)

    private val showUnassignedOnly = MutableStateFlow(false)

    private val selectedSmartCollection = MutableStateFlow<SmartCollectionType?>(null)

    private val showClassification = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow: Flow<PagingData<PhotoTile>> = combine(
        sortFlow, showUnassignedOnly, selectedSmartCollection, showClassification
    ) { sort, unassigned, collection, classification ->
        Pair(Triple(sort, unassigned, collection), classification)
    }.flatMapLatest { (triple, classification) ->
        val (sort, unassigned, collection) = triple
        val photoFlow: Flow<PagingData<com.quangthe.photoqt.model.database.entity.Photo>> = when {
            classification -> photoRepository.observeUnassigned(sort).map { PagingData.from(it) }
            unassigned -> photoRepository.observeUnassigned(sort).map { PagingData.from(it) }
            collection != null -> when (collection) {
                SmartCollectionType.AllPhotos -> photoRepository.observeAllPaged(sort)
                SmartCollectionType.Favorites -> photoRepository.observeFavoritesPaged(sort)
                SmartCollectionType.Videos -> photoRepository.observeVideosPaged(sort)
                SmartCollectionType.Photos -> photoRepository.observeImagesPaged(sort)
                SmartCollectionType.RecentlyAdded -> photoRepository.observeRecentlyAdded(sort).map { PagingData.from(it) }
                SmartCollectionType.Trash -> photoRepository.observeTrashedPaged(sort)
            }
            else -> photoRepository.observeAllPaged(sort)
        }
        photoFlow.map { pagingData ->
            pagingData.map { photo ->
                PhotoTile(
                    fileName = photo.fileName,
                    type = photo.type,
                    uuid = photo.uuid,
                    isFavorite = photo.isFavorite,
                )
            }
        }
    }.cachedIn(viewModelScope)

    private val showAlbumSelectionDialog = MutableStateFlow(false)

    private val albumsFlow = albumRepository.observeAllAlbumsWithPhotos().map { albums ->
        albums.map { album ->
            AlbumItem(
                id = album.uuid,
                name = album.name,
                itemCount = album.files.size,
            )
        }
    }

    val uiState: StateFlow<GalleryUiState> = combine(
        combine(showAlbumSelectionDialog, sortFlow) { a, b -> Pair(a, b) },
        combine(showUnassignedOnly, selectedSmartCollection, showClassification) { a, b, c -> Triple(a, b, c) },
        albumsFlow,
    ) { (dialog, sort), (unassigned, collection, classification), albums ->
        galleryUiStateFactory.create(
            photos = null,
            showAlbumSelectionDialog = dialog,
            sort = sort,
            showUnassignedOnly = unassigned,
            selectedSmartCollection = collection,
            showClassification = classification,
            classificationAlbums = albums,
        )
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
                showClassification.value = false
            }
            is GalleryUiEvent.SelectSmartCollection -> {
                selectedSmartCollection.value = event.type
                showUnassignedOnly.value = false
                showClassification.value = false
            }
            GalleryUiEvent.ToggleClassification -> {
                showClassification.value = !showClassification.value
                if (showClassification.value) {
                    selectedSmartCollection.value = null
                    showUnassignedOnly.value = false
                }
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

    fun openAlbum(albumUuid: String) {
        eventsChannel.trySend(GalleryNavigationEvent.OpenAlbum(albumUuid))
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
                selectedItems = selectedItems,
                target = target,
            )
        )
    }

    private fun onDeleteSelectedItems(selectedItems: List<String>) {
        photoActionsChannel.trySend(
            PhotoAction.DeletePhotos(
                selectedItems = selectedItems
            )
        )
    }

    private fun navigateToPhoto(item: PhotoTile) {
        photoActionsChannel.trySend(PhotoAction.OpenPhoto(item.uuid))
    }
}
