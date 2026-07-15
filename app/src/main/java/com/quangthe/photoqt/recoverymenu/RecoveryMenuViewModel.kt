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

package com.quangthe.photoqt.recoverymenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.other.SingleLiveEvent
import com.quangthe.photoqt.settings.ui.hideapp.usecase.ToggleMainComponentUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryMenuViewModel @Inject constructor(
    private val toggleMainComponentUseCase: ToggleMainComponentUseCase,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    val navigationEvent = SingleLiveEvent<RecoveryMenuNavigator.NavigationEvent>()
    val mergeResultEvent = SingleLiveEvent<Int>()

    fun openPhotoqt() {
        navigationEvent.value = RecoveryMenuNavigator.NavigationEvent.OpenPhotoqt
    }

    fun resetHidePhotoSetting() {
        toggleMainComponentUseCase()

        navigationEvent.value = RecoveryMenuNavigator.NavigationEvent.AfterResetHideApp
    }

    fun mergeDuplicates() {
        viewModelScope.launch {
            val result = photoRepository.mergeDuplicateContent()
            mergeResultEvent.value = result
        }
    }
}
