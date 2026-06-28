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

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.R
import com.quangthe.photoqt.model.repositories.ImportSource
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessBottomSheetDialogFragment
import kotlinx.coroutines.launch
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
    private var lastItems: List<Uri>? = null

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val approved = result.resultCode == Activity.RESULT_OK
        Timber.d("deleteRequestLauncher: approved=%s", approved)
        deleteConsentPending?.invoke(approved)
        deleteConsentPending = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.deleteConsentFlow.collect {
                requestDeletePermission()
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
        lastItems = items
        viewModel.albumUUID = albumUUID
        viewModel.importSource = importSource
        super.prepareViewModel(items?.reversed())
    }

    private suspend fun requestDeletePermission() {
        val mediaUris = (lastItems ?: emptyList()).mapNotNull { uri ->
            ImportViewModel.toMediaStoreUri(uri)
        }
        if (mediaUris.isEmpty()) {
            viewModel.setDeleteConsentResult(false)
            return
        }
        try {
            val pendingIntent = MediaStore.createDeleteRequest(
                requireContext().contentResolver,
                mediaUris,
            )
            deleteConsentPending = { approved -> viewModel.setDeleteConsentResult(approved) }
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent).build()
            )
        } catch (e: Exception) {
            Timber.Forest.e("Error launching delete request: $e")
            viewModel.setDeleteConsentResult(false)
        }
    }
}