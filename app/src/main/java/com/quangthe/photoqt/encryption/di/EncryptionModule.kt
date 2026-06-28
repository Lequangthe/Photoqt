/*
 *   Copyright 2020-2026 Leon Latsch
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

package com.quangthe.photoqt.encryption.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.quangthe.photoqt.encryption.data.SessionRepositoryImpl
import com.quangthe.photoqt.encryption.data.VaultProtectionRepositoryImpl
import com.quangthe.photoqt.encryption.domain.SessionRepository
import com.quangthe.photoqt.encryption.domain.VaultProtectionRepository
import com.quangthe.photoqt.encryption.domain.crypto.CbcCryptoEngine
import com.quangthe.photoqt.encryption.domain.crypto.CryptoEngine
import com.quangthe.photoqt.encryption.domain.handlers.BiometricVaultProtectionHandler
import com.quangthe.photoqt.encryption.domain.handlers.PasswordVaultProtectionHandler
import com.quangthe.photoqt.encryption.domain.handlers.VaultProtectionHandler
import com.quangthe.photoqt.encryption.domain.models.CreateRequest
import com.quangthe.photoqt.encryption.domain.models.UnlockRequest
import com.quangthe.photoqt.model.database.PhotokDatabase

@Module
@InstallIn(SingletonComponent::class)
interface EncryptionBindingModule {

    @Binds
    fun bindVaultProtectionRepository(impl: VaultProtectionRepositoryImpl): VaultProtectionRepository

    @Binds
    fun bindPasswordUnlocker(impl: PasswordVaultProtectionHandler): VaultProtectionHandler<UnlockRequest.Password, CreateRequest.Password>

    @Binds
    fun bindBiometricUnlocker(impl: BiometricVaultProtectionHandler): VaultProtectionHandler<UnlockRequest.Biometric, CreateRequest.Biometric>

    @Binds
    fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    fun bindCryptoEngine(impl: CbcCryptoEngine): CryptoEngine
}

@Module
@InstallIn(SingletonComponent::class)
class EncryptionModule {

    @Provides
    fun provideVaultProtectionDao(database: PhotokDatabase) = database.getVaultProtectionDao()
}