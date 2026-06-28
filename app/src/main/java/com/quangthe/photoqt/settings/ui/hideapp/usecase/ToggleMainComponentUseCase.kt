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

package com.quangthe.photoqt.settings.ui.hideapp.usecase

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import javax.inject.Inject

class ToggleMainComponentUseCase @Inject constructor(
    private val app: Application
) {

    private val mainLauncherComponent =
        ComponentName(app.packageName, "${app.packageName}.MainLauncher")

    private val stealthLauncherComponent =
        ComponentName(app.packageName, "${app.packageName}.StealthLauncher")

    operator fun invoke() {
        if (isMainComponentDisabled()) {
            app.packageManager.setComponentEnabledSetting(
                mainLauncherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0 // Removed DONT_KILL_APP to ensure launcher refreshes
            )
            app.packageManager.setComponentEnabledSetting(
                stealthLauncherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0
            )
        } else {
            app.packageManager.setComponentEnabledSetting(
                mainLauncherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0
            )
            app.packageManager.setComponentEnabledSetting(
                stealthLauncherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0
            )
        }
    }

    fun isMainComponentDisabled(): Boolean {
        val enabledSetting = app.packageManager.getComponentEnabledSetting(
            mainLauncherComponent
        )
        return enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
}
