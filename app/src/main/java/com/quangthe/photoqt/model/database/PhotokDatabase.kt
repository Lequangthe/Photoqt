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

package com.quangthe.photoqt.model.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.quangthe.photoqt.encryption.data.VaultProtectionDao
import com.quangthe.photoqt.encryption.data.VaultProtectionTable
import com.quangthe.photoqt.model.database.dao.AlbumDao
import com.quangthe.photoqt.model.database.dao.PhotoDao
import com.quangthe.photoqt.model.database.entity.AlbumTable
import com.quangthe.photoqt.model.database.entity.Photo
import com.quangthe.photoqt.model.database.ref.AlbumPhotoCrossRefTable
import com.quangthe.photoqt.sort.data.db.SortDao
import com.quangthe.photoqt.sort.data.db.model.SortTable

private const val DATABASE_VERSION = 8
const val DATABASE_NAME = "photok.db"

/**
 * Abstract Room Database.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
@Database(
    entities = [
        Photo::class,
        AlbumTable::class,
        AlbumPhotoCrossRefTable::class,
        SortTable::class,
        VaultProtectionTable::class,
    ],
    version = DATABASE_VERSION,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
            spec = MigrationSpec1To2::class,
        ),
        AutoMigration(
            from = 2,
            to = 3,
        ),
        AutoMigration(
            from = 3,
            to = 4,
        ),
        AutoMigration(
            from = 4,
            to = 5,
        ),
        AutoMigration(
            from = 5,
            to = 6,
        ),
        AutoMigration(
            from = 6,
            to = 7,
        ),
        AutoMigration(
            from = 7,
            to = 8,
        ),
    ]
)
@TypeConverters(Converters::class)
abstract class PhotokDatabase : RoomDatabase() {

    /**
     * Get the data access object for [Photo]
     */
    abstract fun getPhotoDao(): PhotoDao

    abstract fun getAlbumDao(): AlbumDao
    abstract fun getSortDao(): SortDao
    abstract fun getVaultProtectionDao(): VaultProtectionDao
}

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "photo",
        columnName = "id"
    ),
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "photo",
        fromColumnName = "uuid",
        toColumnName = "photo_uuid",
    )
)
class MigrationSpec1To2 : AutoMigrationSpec