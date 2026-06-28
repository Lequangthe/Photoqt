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

package com.quangthe.photoqt

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.di.DaggerBroadcastReceiver
import com.quangthe.photoqt.main.ui.MainActivity
import com.quangthe.photoqt.settings.data.Config
import timber.log.Timber
import javax.inject.Inject

/**
 * Broadcast receiver for receiving android secret codes.
 *
 * @since 1.2.0
 * @author Leon Latsch
 */
@AndroidEntryPoint
class DialLauncher : DaggerBroadcastReceiver() {

    @Inject
    lateinit var config: Config

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context ?: return
        val data = intent?.data
        val host = data?.host
        val expectedCode = config.securityDialLaunchCode
        Timber.d("DialLauncher received: data=%s, host=%s, expectedCode=%s", data, host, expectedCode)
        if (host == expectedCode) {
            Timber.d("DialLauncher: code matched, launching MainActivity")
            val launchIntent = Intent(context, MainActivity::class.java)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(launchIntent)
        } else {
            Timber.d("DialLauncher: code NOT matched")
        }
    }
}