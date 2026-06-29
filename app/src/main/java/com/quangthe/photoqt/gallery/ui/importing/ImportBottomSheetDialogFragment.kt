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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.R
import com.quangthe.photoqt.model.repositories.ImportSource
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessBottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class ImportBottomSheetDialogFragment(
    uris: List<Uri>,
    private val albumUUID: String? = "",
    private val importSource: ImportSource,
) : BaseProcessBottomSheetDialogFragment<Uri>(
    uris,
    R.string.import_importing,
    true
) {

    override val viewModel: ImportViewModel by viewModels()

    private var deleteConsentPending: ((Boolean) -> Unit)? = null

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val approved = result.resultCode == Activity.RESULT_OK
        Timber.d("deleteRequestLauncher: approved=%s", approved)
        // MediaStore.createDeleteRequest handles deletion if approved.
        deleteConsentPending?.let { it(approved) }
        deleteConsentPending = null
    }

    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch {
                deleteFilesDirectly(viewModel.items)
                viewModel.setDeleteConsentResult(true)
            }
        } else {
            viewModel.setDeleteConsentResult(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.deleteConsentFlow.collect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // On Android 11+ we use the system dialog, so skip our own to avoid double dialogs.
                    requestDeletePermission()
                } else {
                    val context = requireContext()
                    val title = getString(R.string.import_delete_originals_title)
                    val message = getString(R.string.import_delete_originals_message)

                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.common_yes) { _, _ ->
                            lifecycleScope.launch { requestDeletePermission() }
                        }
                        .setNegativeButton(R.string.common_no) { _, _ ->
                            viewModel.setDeleteConsentResult(false)
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.duplicateFlow.collect { fileName ->
                showDuplicateDialog(fileName)
            }
        }
    }

    private fun showDuplicateDialog(fileName: String) {
        val context = requireContext()
        val title = getString(R.string.import_duplicate_title)
        val message = getString(R.string.import_duplicate_message, fileName)

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.import_duplicate_import_anyway) { _, _ ->
                viewModel.setDuplicateResult(true)
            }
            .setNegativeButton(R.string.import_duplicate_skip) { _, _ ->
                viewModel.setDuplicateResult(false)
            }
            .setCancelable(false)
            .show()
    }

    override fun prepareViewModel(items: List<Uri>?) {
        viewModel.albumUUID = albumUUID
        viewModel.importSource = importSource
        super.prepareViewModel(items?.reversed())
    }

    private fun toCollectionUri(uri: Uri, contentResolver: android.content.ContentResolver): Uri? {
        val mediaUri = ImportViewModel.toMediaStoreUri(uri) ?: return null
        val authority = mediaUri.authority
        val path = mediaUri.path ?: ""

        // 1. Already a specific collection URI?
        if (authority == "media" && (path.contains("/images/") || path.contains("/video/") || path.contains("/audio/"))) {
            return mediaUri
        }

        // 2. Try to resolve ID and query table
        val id = mediaUri.lastPathSegment
        if (authority == "media" && id != null && id.all { it.isDigit() }) {
            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE,
                android.provider.MediaStore.Files.FileColumns.MIME_TYPE
            )
            try {
                contentResolver.query(mediaUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val mediaType = cursor.getInt(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE))
                        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.MIME_TYPE))

                        val table = when {
                            mediaType == android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            mediaType == android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            mediaType == android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            mimeType?.startsWith("image/") == true -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            mimeType?.startsWith("video/") == true -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            mimeType?.startsWith("audio/") == true -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> null
                        }
                        if (table != null) {
                            return table.buildUpon().appendPath(id).build()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w("toCollectionUri: query failed for %s", mediaUri)
            }

            // Fallback to getType if query fails or returns generic type
            val mimeType = contentResolver.getType(uri) ?: contentResolver.getType(mediaUri)
            val table = when {
                mimeType?.startsWith("image/") == true -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mimeType?.startsWith("video/") == true -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                mimeType?.startsWith("audio/") == true -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }
            if (table != null) {
                return table.buildUpon().appendPath(id).build()
            }
        }

        return if (authority == "media") mediaUri else null
    }

    private fun requestDeletePermission() {
        val cr = requireContext().contentResolver
        val collectionUris = mutableListOf<Uri>()
        val otherUris = mutableListOf<Uri>()

        for (uri in viewModel.items) {
            val collectionUri = toCollectionUri(uri, cr)
            if (collectionUri != null) {
                collectionUris.add(collectionUri)
            } else {
                otherUris.add(uri)
            }
        }

        if (collectionUris.isEmpty() && otherUris.isEmpty()) {
            viewModel.setDeleteConsentResult(false)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && collectionUris.isNotEmpty()) {
            try {
                Timber.d("requestDeletePermission: requesting system delete for %d uris: %s", collectionUris.size, collectionUris)
                val pendingIntent = MediaStore.createDeleteRequest(
                    cr,
                    collectionUris,
                )
                deleteConsentPending = { approved ->
                    if (approved && otherUris.isNotEmpty()) {
                        lifecycleScope.launch { deleteFilesDirectly(otherUris) }
                    }
                    viewModel.setDeleteConsentResult(approved)
                }
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent).build()
                )
            } catch (e: Exception) {
                Timber.e(e, "createDeleteRequest failed for collectionUris: %s", collectionUris)
                lifecycleScope.launch {
                    deleteFilesDirectly(viewModel.items)
                    viewModel.setDeleteConsentResult(true)
                }
            }
        } else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                writePermissionLauncher.launch(permission)
            } else {
                lifecycleScope.launch {
                    deleteFilesDirectly(viewModel.items)
                    viewModel.setDeleteConsentResult(true)
                }
            }
        }
    }

    private suspend fun deleteFilesDirectly(uris: List<Uri>) = withContext(Dispatchers.IO) {
        val cr = requireContext().contentResolver
        for (uri in uris) {
            try {
                val mediaUri = toCollectionUri(uri, cr) ?: uri
                val deleted = cr.delete(mediaUri, null, null)
                Timber.d("deleteFilesDirectly: %s (original: %s) -> %d rows", mediaUri, uri, deleted)
            } catch (e: Exception) {
                Timber.e(e, "deleteFilesDirectly failed for %s", uri)
            }
        }
    }
}