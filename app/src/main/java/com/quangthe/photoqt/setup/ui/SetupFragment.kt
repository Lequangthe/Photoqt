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

package com.quangthe.photoqt.setup.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.BR
import com.quangthe.photoqt.R
import com.quangthe.photoqt.databinding.FragmentSetupBinding
import com.quangthe.photoqt.other.extensions.empty
import com.quangthe.photoqt.other.extensions.finishOnBackWhileStarted
import com.quangthe.photoqt.other.extensions.hide
import com.quangthe.photoqt.other.extensions.show
import com.quangthe.photoqt.other.systemBarsPadding
import com.quangthe.photoqt.uicomponnets.Dialogs
import com.quangthe.photoqt.uicomponnets.base.hideKeyboard
import com.quangthe.photoqt.uicomponnets.bindings.BindableFragment
import timber.log.Timber

/**
 * Fragment for the setup.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class SetupFragment : BindableFragment<FragmentSetupBinding>(R.layout.fragment_setup) {

    private val viewModel: SetupViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.systemBarsPadding()
        finishOnBackWhileStarted()

        viewModel.password = "abc123"
        viewModel.confirmPassword = "abc123"

        viewModel.addOnPropertyChange<String>(BR.password) {
            if (it.isNotEmpty()) {
                val value = when (it.length) {
                    1, 2, 3, 4, 5 -> {
                        binding.setupPasswordStrengthValue.setTextColor(requireContext().getColor(R.color.darkRed))
                        getString(R.string.setup_password_strength_weak)
                    }
                    6, 7, 8, 9, 10 -> {
                        binding.setupPasswordStrengthValue.setTextColor(requireContext().getColor(R.color.darkYellow))
                        getString(R.string.setup_password_strength_moderate)
                    }
                    else -> {
                        binding.setupPasswordStrengthValue.setTextColor(requireContext().getColor(R.color.darkGreen))
                        getString(R.string.setup_password_strength_strong)
                    }
                }
                binding.setupPasswordStrengthLayout.show()
                binding.setupPasswordStrengthValue.text = value
            } else {
                binding.setupPasswordStrengthLayout.hide()
            }

            if (viewModel.validatePassword()) {
                binding.setupConfirmPasswordEditText.show()
            } else {
                binding.setupConfirmPasswordEditText.setTextValue(String.empty)
                binding.setupConfirmPasswordEditText.hide()
            }

            enableOrDisableSetup()
        }

        viewModel.addOnPropertyChange<String>(BR.confirmPassword) {
            enableOrDisableSetup()
        }

        viewModel.addOnPropertyChange<SetupState>(BR.setupState) {
            when (it) {
                SetupState.LOADING -> binding.loadingOverlay.show()
                SetupState.SETUP -> binding.loadingOverlay.hide()
                SetupState.FINISHED -> finishSetup()
            }
        }
    }

    private fun finishSetup() {
        try {
            val activity = activity
            requireNotNull(activity)

            activity.hideKeyboard()
            binding.loadingOverlay.hide()

            findNavController().navigate(SetupFragmentDirections.actionSetupFragmentToGalleryFragment())
        } catch (e: Exception) {
            Timber.e(e)
            Dialogs.showLongToast(
                requireContext(),
                getString(R.string.common_error)
            )
        }
    }

    private fun enableOrDisableSetup() {
        if (!viewModel.passwordsEqual()
            && binding.setupConfirmPasswordEditText.isVisible
        ) {
            binding.setupPasswordMatchWarningTextView.show()
            binding.setupButton.isEnabled = false
        } else {
            binding.setupPasswordMatchWarningTextView.hide()
            if (viewModel.validateBothPasswords()) {
                binding.setupButton.isEnabled = true
            }
        }
    }

    override fun bind(binding: FragmentSetupBinding) {
        super.bind(binding)
        binding.viewModel = viewModel
    }
}