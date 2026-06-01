package dev.leonlatsch.photoqt.gallery.albums.smartcollection.ui

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.leonlatsch.photoqt.R
import dev.leonlatsch.photoqt.gallery.albums.domain.model.SmartCollectionType
import dev.leonlatsch.photoqt.gallery.components.PhotoTile
import dev.leonlatsch.photoqt.gallery.ui.navigation.PhotoAction
import dev.leonlatsch.photoqt.model.database.entity.Photo
import dev.leonlatsch.photoqt.model.repositories.PhotoRepository
import dev.leonlatsch.photoqt.sort.domain.Sort
import dev.leonlatsch.photoqt.sort.domain.SortConfig
import dev.leonlatsch.photoqt.sort.domain.SortRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartCollectionUiState(
    val title: String = "",
    val photos: List<PhotoTile> = emptyList(),
    val isTrash: Boolean = false,
)

@HiltViewModel
class SmartCollectionViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val sortRepository: SortRepository,
    private val resources: Resources,
) : ViewModel() {

    private val collectionTypeFlow = MutableStateFlow<SmartCollectionType?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sortFlow: StateFlow<Sort> = collectionTypeFlow.flatMapLatest { type ->
        if (type != null) {
            sortRepository.observeSortFor(albumUuid = "_smart_${type.id}", default = SortConfig.Gallery.default)
        } else {
            kotlinx.coroutines.flow.flowOf(SortConfig.Gallery.default)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SortConfig.Gallery.default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val photosFlow: Flow<List<Photo>> = combine(collectionTypeFlow, sortFlow) { type, sort ->
        type to sort
    }.flatMapLatest { (type, sort) ->
        when (type) {
            SmartCollectionType.AllPhotos -> photoRepository.observeAll(sort)
            SmartCollectionType.Favorites -> photoRepository.observeFavorites(sort)
            SmartCollectionType.Videos -> photoRepository.observeVideos(sort)
            SmartCollectionType.Photos -> photoRepository.observeImages(sort)
            SmartCollectionType.RecentlyAdded -> photoRepository.observeRecentlyAdded(sort)
            SmartCollectionType.Trash -> photoRepository.observeTrashed(sort)
            null -> kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    val uiState: StateFlow<SmartCollectionUiState> = combine(
        collectionTypeFlow, photosFlow
    ) { type, photos ->
        SmartCollectionUiState(
            title = if (type != null) resources.getString(type.labelRes) else "",
            photos = photos.map {
                PhotoTile(
                    fileName = it.internalThumbnailFileName,
                    type = it.type,
                    uuid = it.uuid,
                    isFavorite = it.isFavorite,
                )
            },
            isTrash = type == SmartCollectionType.Trash,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SmartCollectionUiState())

    private val photoActionsChannel = Channel<PhotoAction>()
    val photoActions = photoActionsChannel.receiveAsFlow()

    fun init(type: SmartCollectionType) {
        collectionTypeFlow.value = type
    }

    fun handleSortChanged(sort: Sort) {
        val type = collectionTypeFlow.value ?: return
        viewModelScope.launch {
            sortRepository.updateSortFor(albumUuid = "_smart_${type.id}", sort = sort)
        }
    }

    fun handleEvent(event: SmartCollectionUiEvent) {
        when (event) {
            is SmartCollectionUiEvent.ToggleFavorite -> viewModelScope.launch(Dispatchers.IO) {
                event.photoUuids.forEach { uuid ->
                    photoRepository.toggleFavorite(uuid, true)
                }
            }
            is SmartCollectionUiEvent.MoveToTrash -> viewModelScope.launch(Dispatchers.IO) {
                event.photoUuids.forEach { uuid ->
                    photoRepository.get(uuid)?.let { photoRepository.moveToTrash(it) }
                }
            }
            is SmartCollectionUiEvent.RestoreFromTrash -> viewModelScope.launch(Dispatchers.IO) {
                event.photoUuids.forEach { uuid ->
                    photoRepository.restoreFromTrash(uuid)
                }
            }
            is SmartCollectionUiEvent.PermanentDelete -> viewModelScope.launch(Dispatchers.IO) {
                event.photoUuids.forEach { uuid ->
                    photoRepository.get(uuid)?.let { photoRepository.permanentDelete(it) }
                }
            }
        }
    }
}

sealed interface SmartCollectionUiEvent {
    data class ToggleFavorite(val photoUuids: List<String>) : SmartCollectionUiEvent
    data class MoveToTrash(val photoUuids: List<String>) : SmartCollectionUiEvent
    data class RestoreFromTrash(val photoUuids: List<String>) : SmartCollectionUiEvent
    data class PermanentDelete(val photoUuids: List<String>) : SmartCollectionUiEvent
}
