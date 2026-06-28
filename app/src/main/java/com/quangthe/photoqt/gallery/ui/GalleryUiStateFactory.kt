package com.quangthe.photoqt.gallery.ui

import com.quangthe.photoqt.sort.domain.Sort
import android.net.Uri
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.gallery.albums.ui.compose.AlbumItem
import com.quangthe.photoqt.gallery.components.PhotoTile
import com.quangthe.photoqt.model.database.entity.Photo
import javax.inject.Inject

class GalleryUiStateFactory @Inject constructor() {
    fun create(
        photos: List<Photo>?,
        showAlbumSelectionDialog: Boolean,
        sort: Sort,
        showUnassignedOnly: Boolean = false,
        selectedSmartCollection: SmartCollectionType? = null,
        showClassification: Boolean = false,
        classificationAlbums: List<AlbumItem> = emptyList(),
    ): GalleryUiState {
        val mapped = photos?.map {
            PhotoTile(
                fileName = it.fileName,
                type = it.type,
                uuid = it.uuid,
                isFavorite = it.isFavorite,
            )
        } ?: emptyList()
        return GalleryUiState.Content(
            photos = mapped,
            showAlbumSelectionDialog = showAlbumSelectionDialog,
            sort = sort,
            showUnassignedOnly = showUnassignedOnly,
            selectedSmartCollection = selectedSmartCollection,
            showClassification = showClassification,
            classificationAlbums = classificationAlbums,
        )
    }
}
