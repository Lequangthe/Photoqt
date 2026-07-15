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

package com.quangthe.photoqt.gallery.ui.importing

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.gallery.albums.domain.AlbumRepository
import com.quangthe.photoqt.model.repositories.ImportSource
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.other.getMetadataFor
import com.quangthe.photoqt.settings.data.Config
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    app: Application,
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val sharedUrisStore: SharedUrisStore,
    private val config: Config,
) : BaseProcessViewModel<Uri>(app) {

    var albumUUID: String? = null
    var importSource = ImportSource.InApp

    private val deleteConsentRequest = Channel<Unit>(Channel.RENDEZVOUS)
    private var deleteConsentResult = kotlinx.coroutines.CompletableDeferred<Boolean>()

    val deleteConsentFlow = deleteConsentRequest.receiveAsFlow()

    private val duplicateRequest = Channel<Pair<String, Boolean>>(Channel.RENDEZVOUS)
    private var duplicateResult = kotlinx.coroutines.CompletableDeferred<Boolean>()

    val duplicateFlow = duplicateRequest.receiveAsFlow()

    fun setDeleteConsentResult(approved: Boolean) {
        if (!deleteConsentResult.isCompleted) {
            deleteConsentResult.complete(approved)
        }
    }

    fun setDuplicateResult(importAnyway: Boolean) {
        if (!duplicateResult.isCompleted) {
            duplicateResult.complete(importAnyway)
        }
    }

    override suspend fun preProcess() {
        super.preProcess()

        val itemsToRemove = mutableListOf<Uri>()

        for (item in items) {
            val metaData = getApplication<Application>().contentResolver.getMetadataFor(item)
            val duplicate = photoRepository.findDuplicate(
                metaData.fileName ?: "",
                metaData.size ?: 0
            )

            if (duplicate != null) {
                duplicateResult = kotlinx.coroutines.CompletableDeferred()
                val isInTrash = duplicate.deletedAt != null
                duplicateRequest.send((metaData.fileName ?: "") to isInTrash)
                val importAnyway = duplicateResult.await()
                if (!importAnyway) {
                    itemsToRemove.add(item)
                }
            }
        }

        if (itemsToRemove.isNotEmpty()) {
            items = items.filter { it !in itemsToRemove }
            elementsToProcess = items.size
        }
    }

    override suspend fun processItem(item: Uri) {
        val photoUUID = photoRepository.safeImportPhoto(
            sourceUri = item,
        )

        if (photoUUID.isEmpty()) {
            Timber.w("Failed to import item: %s", item)
            failuresOccurred = true
            return
        }

        albumUUID?.let {
            Timber.d("Linking photo %s to album %s", photoUUID, it)
            albumRepository.link(listOf(photoUUID), it)
        }
    }

    override suspend fun postProcess() {
        if (config.deleteImportedFiles && importSource != ImportSource.Share) {
            if (items.isNotEmpty()) {
                deleteConsentResult = kotlinx.coroutines.CompletableDeferred()
                deleteConsentRequest.send(Unit)
                val approved = deleteConsentResult.await()
                if (approved) {
                    Timber.d("postProcess: user approved deletion of %d files", items.size)
                } else {
                    Timber.d("postProcess: user denied deletion")
                }
            }
        }

        super.postProcess()
        sharedUrisStore.reset()
    }

    override fun cancel() {
        super.cancel()
        if (!deleteConsentResult.isCompleted) {
            deleteConsentResult.complete(false)
        }
    }

    companion object {
        fun toMediaStoreUri(fileUri: Uri): Uri? {
            val authority = fileUri.authority
            val path = fileUri.path ?: ""

            // Handle Photo Picker URIs (both old and new styles)
            if ((authority == "media" && path.contains("photopicker")) || authority == "com.android.providers.media.photopicker") {
                val id = fileUri.lastPathSegment
                if (id != null && id.all { it.isDigit() }) {
                    return MediaStore.Files.getContentUri("external", id.toLong())
                }
            }

            if (authority == "media") {
                return fileUri
            }

            if (authority == "com.android.providers.media.documents") {
                return try {
                    val docId = DocumentsContract.getDocumentId(fileUri)
                    val parts = docId.split(":")
                    if (parts.size < 2) return null
                    val id = parts.last()
                    val table = when (parts[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> return null
                    }
                    table.buildUpon().appendPath(id).build()
                } catch (e: Exception) {
                    null
                }
            }

            return null
        }
    }
}