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

package com.quangthe.photoqt.forwarddialer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.forwarddialer.usecase.IsAirplaneModeOnUseCase
import com.quangthe.photoqt.other.SingleLiveEvent
import com.quangthe.photoqt.settings.data.Config
import com.quangthe.photoqt.settings.data.Config.Companion.TIMESTAMP_LAST_RECOVERY_START_DEFAULT
import timber.log.Timber
import javax.inject.Inject

const val RECOVERY_MENU_MILLIS_THRESHOLD = 5000L

@HiltViewModel
class ForwardDialerViewModel @Inject constructor(
    private val isAirplaneModeOn: IsAirplaneModeOnUseCase,
    private val config: Config
) : ViewModel() {

    val navigationEvent = SingleLiveEvent<ForwardDialerNavigator.NavigationEvent>()

    fun evaluateNavigation() {
        Timber.d("ForwardDialerViewModel.evaluateNavigation: isAirplaneMode=%s", isAirplaneModeOn())
        if (isAirplaneModeOn()) {
            val now = System.currentTimeMillis()
            val lastRecoveryStart = config.timestampLastRecoveryStart
            val millisSinceLastRecoveryStart = now - lastRecoveryStart
            Timber.d("ForwardDialerViewModel: now=%d, lastRecovery=%d, diff=%d, threshold=%d",
                now, lastRecoveryStart, millisSinceLastRecoveryStart, RECOVERY_MENU_MILLIS_THRESHOLD)

            if (millisSinceLastRecoveryStart < RECOVERY_MENU_MILLIS_THRESHOLD) {
                Timber.d("ForwardDialerViewModel: opening recovery menu")
                navigationEvent.value = ForwardDialerNavigator.NavigationEvent.OpenRecoveryMenu
                config.timestampLastRecoveryStart = TIMESTAMP_LAST_RECOVERY_START_DEFAULT
            } else {
                config.timestampLastRecoveryStart = now
                navigateToDialer()
            }
        } else {
            navigateToDialer()
        }
    }

    private fun navigateToDialer() {
        navigationEvent.value = ForwardDialerNavigator.NavigationEvent.ForwardToDialer
    }
}