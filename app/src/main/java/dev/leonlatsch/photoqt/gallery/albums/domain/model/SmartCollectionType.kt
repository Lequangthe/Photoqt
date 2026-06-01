package dev.leonlatsch.photoqt.gallery.albums.domain.model

import dev.leonlatsch.photoqt.R

enum class SmartCollectionType(
    val id: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    AllPhotos("all_photos", R.string.collection_all_photos, R.drawable.ic_image),
    Favorites("favorites", R.string.collection_favorites, R.drawable.ic_favorite),
    Videos("videos", R.string.collection_videos, R.drawable.ic_videocam),
    Photos("photos", R.string.collection_photos, R.drawable.ic_image),
    RecentlyAdded("recently_added", R.string.collection_recently_added, R.drawable.ic_schedule),
    Trash("trash", R.string.collection_trash, R.drawable.ic_delete);
}
