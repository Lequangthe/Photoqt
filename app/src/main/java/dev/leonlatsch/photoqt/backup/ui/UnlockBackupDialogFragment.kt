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

package dev.leonlatsch.photoqt.backup.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.leonlatsch.photoqt.BR
import dev.leonlatsch.photoqt.R
import dev.leonlatsch.photoqt.backup.data.BackupMetaData
import dev.leonlatsch.photoqt.backup.domain.UnlockBackupUseCase
import dev.leonlatsch.photoqt.databinding.DialogBackupUnlockBinding
import dev.leonlatsch.photoqt.encryption.domain.models.Session
import dev.leonlatsch.photoqt.other.extensions.hide
import dev.leonlatsch.photoqt.other.extensions.show
import dev.leonlatsch.photoqt.uicomponnets.bindings.BindableDialogFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog for unlocking a backup.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class UnlockBackupDialogFragment(
    private val uri: Uri,
    private val metaData: BackupMetaData,
    val onUnlockSuccess: (session: Session) -> Unit
) : BindableDialogFragment<DialogBackupUnlockBinding>(R.layout.dialog_backup_unlock) {

    private val viewModel: UnlockBackupViewModel by viewModels()

    @Inject
    lateinit var unlockBackupUseCase: UnlockBackupUseCase

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.addOnPropertyChange<String>(BR.password) {
            binding.unlockBackupWrongPasswordWarning.hide()
        }
    }

    fun onUnlock() {
        binding.unlockBackupWrongPasswordWarning.hide()

        lifecycleScope.launch {
            unlockBackupUseCase(uri, metaData, viewModel.password)
                .onSuccess { session ->
                    dismiss()
                    onUnlockSuccess(session)
                }
                .onFailure {
                    binding.unlockBackupWrongPasswordWarning.show()
                }
        }
    }

    override fun bind(binding: DialogBackupUnlockBinding) {
        super.bind(binding)
        binding.viewModel = viewModel
        binding.context = this
    }
}