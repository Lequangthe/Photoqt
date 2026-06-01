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

package dev.leonlatsch.photoqt.gallery.ui.importing

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.leonlatsch.photoqt.gallery.albums.domain.AlbumRepository
import dev.leonlatsch.photoqt.model.repositories.ImportSource
import dev.leonlatsch.photoqt.model.repositories.PhotoRepository
import dev.leonlatsch.photoqt.settings.data.Config
import dev.leonlatsch.photoqt.uicomponnets.base.processdialogs.BaseProcessViewModel
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

    fun setDeleteConsentResult(approved: Boolean) {
        if (!deleteConsentResult.isCompleted) {
            deleteConsentResult.complete(approved)
        }
    }

    override suspend fun processItem(item: Uri) {
        val photoUUID = photoRepository.safeImportPhoto(
            sourceUri = item,
            importSource = importSource,
        )
        if (photoUUID.isEmpty()) {
            failuresOccurred = true
            return
        }

        albumUUID?.let {
            albumRepository.link(listOf(photoUUID), it)
        }
    }

    override suspend fun postProcess() {
        if (config.deleteImportedFiles && importSource != ImportSource.Share) {
            val mediaUris = items.mapNotNull { toMediaStoreUri(it) }
            if (mediaUris.isNotEmpty()) {
                deleteConsentRequest.send(Unit)
                val approved = deleteConsentResult.await()
                if (approved) {
                    Timber.d("postProcess: user approved deletion of %d files", mediaUris.size)
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
            if (fileUri.authority != "com.android.providers.media.documents") return null
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
    }
}