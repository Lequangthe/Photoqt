package com.quangthe.photoqt.model.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.quangthe.photoqt.model.database.entity.Photo
import com.quangthe.photoqt.model.database.ref.AlbumPhotoCrossRefTable
import com.quangthe.photoqt.sort.domain.Sort
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Delete
    suspend fun delete(photo: Photo): Int

    @Query("DELETE FROM photo")
    suspend fun deleteAll()

    @Query("SELECT * FROM photo WHERE photo_uuid = :uuid")
    suspend fun get(uuid: String): Photo

    @Query("SELECT * FROM photo ORDER BY importedAt DESC")
    suspend fun findAllPhotosByImportDateDesc(): List<Photo>

    @Query("SELECT * FROM photo ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<Photo>>

    @Query("SELECT COUNT(*) FROM photo")
    suspend fun countAll(): Int

    @Query("SELECT * FROM photo WHERE fileName = :fileName AND size = :size LIMIT 1")
    suspend fun findDuplicate(fileName: String, size: Long): Photo?

    @Query("UPDATE photo SET is_favorite = :favorite WHERE photo_uuid = :uuid")
    suspend fun updateFavorite(uuid: String, favorite: Boolean)

    @Query("UPDATE photo SET deleted_at = :deletedAt WHERE photo_uuid = :uuid")
    suspend fun updateDeletedAt(uuid: String, deletedAt: Long?)

    @Query("SELECT * FROM photo WHERE deleted_at IS NULL ORDER BY importedAt DESC")
    fun observeAllNotDeleted(): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE is_favorite = 1 AND deleted_at IS NULL ORDER BY importedAt DESC")
    fun observeFavorites(): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE type IN (4,5,6,7,9) AND deleted_at IS NULL ORDER BY importedAt DESC")
    fun observeVideos(): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE type NOT IN (4,5,6,7,9) AND deleted_at IS NULL ORDER BY importedAt DESC")
    fun observeImages(): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE importedAt > :since AND deleted_at IS NULL ORDER BY importedAt DESC")
    fun observeRecentlyAdded(since: Long): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeTrashed(): Flow<List<Photo>>

    @Query("SELECT * FROM photo WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun getTrashedOlderThan(before: Long): List<Photo>

    @Query("DELETE FROM photo WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun deleteTrashedOlderThan(before: Long): Int

    fun observeAllSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    fun observeAllNotDeletedSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    fun observeUnassignedSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_DELETED_AT} IS NULL AND photo_uuid NOT IN (SELECT photo_uuid FROM ${AlbumPhotoCrossRefTable.TABLE_NAME}) ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAllUnassigned(query)
    }

    fun observeFavoritesSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_IS_FAVORITE} = 1 AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    fun observeVideosSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE type IN (4,5,6,7,9) AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    fun observeImagesSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE type NOT IN (4,5,6,7,9) AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    fun observeRecentlyAddedSorted(since: Long, sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_IMPORTED_AT} > ? AND ${Photo.COL_DELETED_AT} IS NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}",
            arrayOf(since)
        )
        return observeAll(query)
    }

    fun observeTrashedSorted(sort: Sort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM ${Photo.TABLE_NAME} WHERE ${Photo.COL_DELETED_AT} IS NOT NULL ORDER BY ${sort.field.columnName} ${sort.order.sql}"
        )
        return observeAll(query)
    }

    @RawQuery(observedEntities = [Photo::class])
    fun observeAll(query: SupportSQLiteQuery): Flow<List<Photo>>

    @RawQuery(observedEntities = [Photo::class, AlbumPhotoCrossRefTable::class])
    fun observeAllUnassigned(query: SupportSQLiteQuery): Flow<List<Photo>>

    @RawQuery(observedEntities = [Photo::class])
    fun pagingSource(query: SupportSQLiteQuery): PagingSource<Int, Photo>
}
