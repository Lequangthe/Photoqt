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

package com.quangthe.photoqt.model.repositories

import android.app.Application
import android.net.Uri
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.quangthe.photoqt.io.IO
import com.quangthe.photoqt.io.VaultFileStorage
import com.quangthe.photoqt.model.database.dao.AlbumDao
import com.quangthe.photoqt.model.database.dao.PhotoDao
import com.quangthe.photoqt.model.database.entity.Photo
import com.quangthe.photoqt.model.database.entity.PhotoType
import com.quangthe.photoqt.model.io.CreateThumbnailsUseCase
import com.quangthe.photoqt.other.extensions.empty
import com.quangthe.photoqt.other.extensions.lazyClose
import com.quangthe.photoqt.other.getMetadataFor
import com.quangthe.photoqt.settings.data.Config
import com.quangthe.photoqt.sort.domain.Sort
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow


/**
 * Repository for [Photo].
 * Uses [PhotoDao] and accesses the filesystem to read and write encrypted photos.
 *
 * @since 1.0.0
 * @author Leon Latsch
 */
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val albumDao: AlbumDao,
    private val vaultFileStorage: VaultFileStorage,
    private val createThumbnail: CreateThumbnailsUseCase,
    private val app: Application,
    private val config: Config,
    private val io: IO,
) {

    companion object {
        private val PAGE_CONFIG = PagingConfig(pageSize = 60, enablePlaceholders = false)
    }

    // region DATABASE

    suspend fun insert(photo: Photo) = photoDao.insert(photo)

    private suspend fun delete(photo: Photo) = photoDao.delete(photo)

    suspend fun deleteAll() = photoDao.deleteAll()

    suspend fun get(uuid: String) = photoDao.get(uuid)

    suspend fun findAllPhotosByImportDateDesc() = photoDao.findAllPhotosByImportDateDesc()

    fun observeAll(sort: Sort) = photoDao.observeAllSorted(sort)

    suspend fun countAll() = photoDao.countAll()

    fun observeFavorites(sort: Sort) = photoDao.observeFavoritesSorted(sort)

    fun observeVideos(sort: Sort) = photoDao.observeVideosSorted(sort)

    fun observeImages(sort: Sort) = photoDao.observeImagesSorted(sort)

    fun observeRecentlyAdded(sort: Sort) = photoDao.observeRecentlyAddedSorted(
        since = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L,
        sort = sort,
    )

    fun observeTrashed(sort: Sort) = photoDao.observeTrashedSorted(sort)

    fun observeUnassigned(sort: Sort) = photoDao.observeUnassignedSorted(sort)

    fun observeAllPaged(sort: Sort): Flow<PagingData<Photo>> = Pager(PAGE_CONFIG) {
        photoDao.pagingSource(
            SimpleSQLiteQuery("SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}")
        )
    }.flow

    fun observeFavoritesPaged(sort: Sort): Flow<PagingData<Photo>> = Pager(PAGE_CONFIG) {
        photoDao.pagingSource(
            SimpleSQLiteQuery("SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_IS_FAVORITE} = 1 AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}")
        )
    }.flow

    fun observeVideosPaged(sort: Sort): Flow<PagingData<Photo>> = Pager(PAGE_CONFIG) {
        photoDao.pagingSource(
            SimpleSQLiteQuery("SELECT * FROM ${Photo.TABLE_NAME} WHERE type IN (4,5,6,7,9) AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}")
        )
    }.flow

    fun observeImagesPaged(sort: Sort): Flow<PagingData<Photo>> = Pager(PAGE_CONFIG) {
        photoDao.pagingSource(
            SimpleSQLiteQuery("SELECT * FROM ${Photo.TABLE_NAME} WHERE type NOT IN (4,5,6,7,9) AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}")
        )
    }.flow

    fun observeTrashedPaged(sort: Sort): Flow<PagingData<Photo>> = Pager(PAGE_CONFIG) {
        photoDao.pagingSource(
            SimpleSQLiteQuery("SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_DELETED_AT} IS NOT NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}")
        )
    }.flow

    suspend fun toggleFavorite(uuid: String, favorite: Boolean) = photoDao.updateFavorite(uuid, favorite)

    suspend fun permanentDelete(photo: Photo) {
        delete(photo)
        deleteInternalPhotoData(photo)
        albumDao.unlink(photo.uuid)
    }

    suspend fun moveToTrash(photo: Photo) {
        photoDao.updateDeletedAt(photo.uuid, System.currentTimeMillis())
    }

    suspend fun restoreFromTrash(uuid: String) {
        photoDao.updateDeletedAt(uuid, null)
    }

    suspend fun getTrashedOlderThan(before: Long) = photoDao.getTrashedOlderThan(before)

    suspend fun deleteTrashedOlderThan(before: Long) = photoDao.deleteTrashedOlderThan(before)

    suspend fun findDuplicate(fileName: String, size: Long) = photoDao.findDuplicate(fileName, size)

    // endregion

    // region IO

    // region WRITE

    /**
     * Import a photo from a url.
     *
     * Collects meta data and calls [safeCreatePhoto].
     * Returns re created uuid
     */
    suspend fun safeImportPhoto(sourceUri: Uri): String {
        val metaData = app.contentResolver.getMetadataFor(sourceUri)

        val type = PhotoType.fromMimeType(metaData.mimeType)
        if (type == PhotoType.UNDEFINED) return String.empty


        val inputStream = io.openFileInput(sourceUri)
        val photo = Photo(
            fileName = metaData.fileName ?: UUID.randomUUID().toString(),
            importedAt = System.currentTimeMillis(),
            lastModified = metaData.lastModified,
            type = type,
            size = metaData.size ?: 0,
        )

        val created = safeCreatePhoto(photo, inputStream, sourceUri)
        inputStream?.lazyClose()

        if (!created) {
            return String.empty
        }

        return photo.uuid
    }

    /**
     * Writes and encrypts the [source] into internal storage.
     * Saves the [photo] afterwords.
     * It is up to the caller to close the [source].
     * Does create a thumbnail, IF [origUri] is specified.
     *
     * @return true, if everything worked
     */
    private suspend fun safeCreatePhoto(
        photo: Photo,
        source: InputStream?,
        origUri: Uri? = null
    ): Boolean {
        val fileLen = createPhotoFile(photo, source)
        var success = fileLen != -1L

        if (success) {
            photo.size = fileLen

            if (origUri != null) {
                createThumbnail(photo, origUri)
            }

            val photoId = insert(photo)
            success = photoId != -1L
        }

        if (!success) {
            deleteInternalPhotoData(photo)
        }

        return success
    }

    /**
     * Create the internal file for a photo.
     */
    fun createPhotoFile(photo: Photo, source: InputStream?): Long {
        try {
            val encryptedDestination = vaultFileStorage.openEncryptedOutput(photo.internalFileName)

            source ?: return -1L
            encryptedDestination ?: return -1L

            val fileLen = source.copyTo(encryptedDestination)
            encryptedDestination.lazyClose()

            return fileLen
        } catch (e: IOException) {
            Timber.e("Error while writing file: $e")
            return -1L
        }
    }

    // endregion

    // region DELETE

    /**
     * Move a photo to trash (soft delete). Sets deletedAt timestamp.
     * The photo stays on disk until permanently deleted or auto-purged.
     */
    suspend fun safeDeletePhoto(photo: Photo): Boolean {
        moveToTrash(photo)
        return true
    }

    /**
     * Delete a photos bytes and thumbnail bytes on the filesystem.
     *
     * @param photo the photo to delete
     *
     * @return true, if photo and thumbnail could be deleted
     */
    fun deleteInternalPhotoData(photo: Photo): Boolean =
        vaultFileStorage.deleteEncryptedFile(photo.internalFileName)
                && vaultFileStorage.deleteEncryptedFile(photo.internalThumbnailFileName)
                && (!photo.type.isVideo || vaultFileStorage.deleteEncryptedFile(photo.internalVideoPreviewFileName))


    // endregion

    // region EXPORT

    /**
     * Export a photo to a specific directory.
     *
     * @param photo The Photo to be saved
     */
    suspend fun exportPhoto(photo: Photo, target: Uri): Boolean {
        return try {
            val inputStream =
                vaultFileStorage.openEncryptedInput(photo.internalFileName)
            inputStream ?: return false

            val outputStream = createExternalOutputStream(photo, target)
            outputStream ?: return false

            val wrote = inputStream.copyTo(outputStream)
            outputStream.lazyClose()

            var deleted = true
            if (config.deleteExportedFiles) {
                deleted = safeDeletePhoto(photo)
            }

            wrote != -1L && deleted
        } catch (e: IOException) {
            Timber.d("Error exporting file: ${photo.fileName} $e")
            false
        }
    }

    private fun createExternalOutputStream(photo: Photo, uri: Uri): OutputStream? {
        val fileName = photo.fileName
        val mimeType = photo.type.mimeType

        return io.openFileOutput(
            app.contentResolver,
            fileName,
            mimeType,
            uri,
        )
    }

    // endregion
    // endregion
}