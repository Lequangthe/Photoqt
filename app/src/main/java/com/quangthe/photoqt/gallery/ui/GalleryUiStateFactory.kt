package com.quangthe.photoqt.gallery.ui

import com.quangthe.photoqt.gallery.components.GalleryViewMode
import com.quangthe.photoqt.sort.domain.Sort
import com.quangthe.photoqt.gallery.albums.ui.compose.AlbumItem
import javax.inject.Inject

class GalleryUiStateFactory @Inject constructor() {
    fun create(
        showAlbumSelectionDialog: Boolean,
        sort: Sort,
        viewMode: GalleryViewMode = GalleryViewMode.Grid,
        classificationAlbums: List<AlbumItem> = emptyList(),
    ): GalleryUiState {
        return GalleryUiState.Content(
            showAlbumSelectionDialog = showAlbumSelectionDialog,
            sort = sort,
            viewMode = viewMode,
            classificationAlbums = classificationAlbums,
        )
    }
}
