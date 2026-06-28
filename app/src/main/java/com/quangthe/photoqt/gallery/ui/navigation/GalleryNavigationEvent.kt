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

package com.quangthe.photoqt.gallery.ui.navigation

import android.net.Uri
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.model.repositories.ImportSource

sealed interface GalleryNavigationEvent {
    data class ShowToast(val text: String) : GalleryNavigationEvent
    data class StartImport(val fileUris: List<Uri>, val importSource: ImportSource, val albumUuid: String? = null) : GalleryNavigationEvent
    data class StartRestoreBackup(val backupUri: Uri) : GalleryNavigationEvent
    data class OpenSmartCollection(val type: SmartCollectionType) : GalleryNavigationEvent
    data class OpenAlbum(val albumUuid: String) : GalleryNavigationEvent
}