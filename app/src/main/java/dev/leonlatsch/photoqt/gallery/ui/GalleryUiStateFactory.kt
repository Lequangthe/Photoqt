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

import dev.leonlatsch.photoqt.sort.domain.Sort
import android.net.Uri
import dev.leonlatsch.photoqt.gallery.albums.domain.model.SmartCollectionType
import dev.leonlatsch.photoqt.gallery.components.PhotoTile
import dev.leonlatsch.photoqt.model.database.entity.Photo
import javax.inject.Inject

class GalleryUiStateFactory @Inject constructor() {
    fun create(
        photos: List<Photo>,
        showAlbumSelectionDialog: Boolean,
        sort: Sort,
        showUnassignedOnly: Boolean = false,
        selectedSmartCollection: SmartCollectionType? = null,
    ): GalleryUiState {
        val mapped = photos.map {
            PhotoTile(
                fileName = it.fileName,
                type = it.type,
                uuid = it.uuid,
                isFavorite = it.isFavorite,
            )
        }
        return if (mapped.isEmpty() && selectedSmartCollection == null) {
            GalleryUiState.Empty
        } else {
            GalleryUiState.Content(
                photos = mapped,
                showAlbumSelectionDialog = showAlbumSelectionDialog,
                sort = sort,
                showUnassignedOnly = showUnassignedOnly,
                selectedSmartCollection = selectedSmartCollection,
            )
        }
    }
}