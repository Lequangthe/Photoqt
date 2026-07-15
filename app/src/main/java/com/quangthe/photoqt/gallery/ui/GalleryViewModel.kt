package com.quangthe.photoqt.gallery.ui


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.gallery.albums.domain.AlbumRepository
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.gallery.albums.ui.compose.AlbumItem
import com.quangthe.photoqt.gallery.components.GalleryViewMode
import com.quangthe.photoqt.gallery.components.ImportChoice
import com.quangthe.photoqt.gallery.components.PhotoTile
import com.quangthe.photoqt.gallery.ui.navigation.GalleryNavigationEvent
import com.quangthe.photoqt.gallery.ui.navigation.PhotoAction
import com.quangthe.photoqt.model.repositories.ImportSource
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.sort.domain.SortRepository
import com.quangthe.photoqt.settings.data.Config
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
    private val config: Config,
) : ViewModel() {

    private val sortFlow = sortRepository.observeSortFor(albumUuid = null, default = SortConfig.Gallery.default)

    private val viewModeFlow = MutableStateFlow(config.galleryViewMode)

    private val selectedTabFlow = MutableStateFlow(GalleryTab.Classification)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val unassignedPhotosFlow: Flow<List<PhotoTile>> = sortFlow.flatMapLatest { sort ->
        photoRepository.observeUnassigned(sort).map { list ->
            list.map { photo ->
                PhotoTile(
                    fileName = photo.fileName,
                    type = photo.type,
                    uuid = photo.uuid,
                    size = photo.size,
                    importedAt = photo.importedAt,
                    isFavorite = photo.isFavorite,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allPhotosFlow: Flow<List<PhotoTile>> = sortFlow.flatMapLatest { sort ->
        photoRepository.observeAll(sort).map { list ->
            list.map { photo ->
                PhotoTile(
                    fileName = photo.fileName,
                    type = photo.type,
                    uuid = photo.uuid,
                    size = photo.size,
                    importedAt = photo.importedAt,
                    isFavorite = photo.isFavorite,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow: Flow<PagingData<PhotoTile>> = selectedTabFlow.flatMapLatest { tab ->
        sortFlow.flatMapLatest { sort ->
            val sourceFlow = if (tab == GalleryTab.All) {
                photoRepository.observeAll(sort)
            } else {
                photoRepository.observeUnassigned(sort)
            }

            sourceFlow.map { list ->
                PagingData.from(list.map { photo ->
                    PhotoTile(
                        fileName = photo.fileName,
                        type = photo.type,
                        uuid = photo.uuid,
                        size = photo.size,
                        importedAt = photo.importedAt,
                        isFavorite = photo.isFavorite,
                    )
                })
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
        combine(showAlbumSelectionDialog, sortFlow, selectedTabFlow) { a, b, c -> Triple(a, b, c) },
        combine(viewModeFlow, albumsFlow, unassignedPhotosFlow, allPhotosFlow) { mode, albums, unassigned, all ->
            Quad(mode, albums, unassigned, all)
        },
    ) { (dialog, sort, tab), (mode, albums, unassigned, all) ->
        galleryUiStateFactory.create(
            showAlbumSelectionDialog = dialog,
            sort = sort,
            viewMode = mode,
            classificationAlbums = albums,
            photos = unassigned,
            allPhotos = all,
            selectedTab = tab,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), GalleryUiState.Empty)

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
            is GalleryUiEvent.ViewModeChanged -> {
                viewModeFlow.value = event.viewMode
                config.galleryViewMode = event.viewMode
            }
            is GalleryUiEvent.TabChanged -> {
                selectedTabFlow.value = event.tab
                if (event.tab == GalleryTab.All) {
                    viewModeFlow.value = GalleryViewMode.Timeline
                } else {
                    viewModeFlow.value = config.galleryViewMode
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
