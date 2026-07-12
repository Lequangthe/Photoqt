### Tinh gọn màn hình "All files" → "Phân loại"
- File thay đổi:
  - app/src/main/res/values/strings.xml
  - app/src/main/java/.../gallery/ui/GalleryUiState.kt
  - app/src/main/java/.../gallery/ui/GalleryUiEvent.kt
  - app/src/main/java/.../gallery/ui/GalleryUiStateFactory.kt
  - app/src/main/java/.../gallery/ui/GalleryViewModel.kt
  - app/src/main/java/.../gallery/ui/compose/GalleryScreen.kt
  - app/src/main/java/.../gallery/ui/compose/GalleryContent.kt
- Chi tiết:
  - Đổi tên tab "All files" → "Phân loại"
  - Xoá FilterChip "All/Unassigned" và "Phân loại" khỏi toolbar
  - Giữ lại nút Sort (sắp xếp) và Trash (thùng rác)
  - Xoá LazyRow smart collection cards (All Photos, Favorites, Videos, Photos, Recently Added)
  - Luôn hiển thị chế độ phân loại: Album grid + ảnh chưa phân loại
  - Đơn giản hoá ViewModel: bỏ showUnassignedOnly, selectedSmartCollection, showClassification
  - Giữ nguyên PhotoGallery + AlbumsGrid cho tương lai (dễ thêm kiểu hiển thị mới)

### Content-hash deduplication (SHA-256) – Chống trùng file theo nội dung
- File thay đổi:
  - app/src/main/java/com/quangthe/photoqt/model/database/entity/Photo.kt
  - app/src/main/java/com/quangthe/photoqt/model/database/dao/PhotoDao.kt
  - app/src/main/java/com/quangthe/photoqt/model/database/PhotokDatabase.kt
  - app/src/main/java/com/quangthe/photoqt/model/repositories/PhotoRepository.kt
- Chi tiết:
  - Thêm cột `sha256` vào bảng `photo` (nullable TEXT)
  - Room auto migration từ v7 → v8
  - Tính toán SHA-256 hash trong lúc copy + encrypt file (dùng DigestInputStream)
  - Kiểm tra trùng nội dung trước khi insert vào DB, skip nếu trùng
  - Thêm DAO query `findDuplicateBySha256`

### View modes (Grid/List/Column/Timeline) cho màn hình Album Detail
- File thay đổi:
  - AlbumDetailUiState.kt – thêm field `viewMode`
  - AlbumDetailUiEvent.kt – thêm event `ViewModeChanged`
  - AlbumDetailViewModel.kt – thêm `viewModeFlow`, xử lý event, populate `size`/`importedAt` vào `PhotoTile`
  - AlbumDetailScreen.kt – thêm dropdown toggle chế độ xem trên toolbar
  - AlbumDetailContent.kt – truyền `viewMode` vào `PhotoGallery`
- Chi tiết:
  - Mirror nguyên cách làm từ màn hình chính Gallery
  - 4 chế độ: Grid, List, Column, Timeline
  - View mode được giữ local (không lưu vào database)
  - Button (icon ảnh) + DropdownMenu trên toolbar, giữa Sort và More

### Việt hoá toàn bộ UI strings + Fix hardcoded strings
- File thay đổi:
  - app/src/main/res/values/strings.xml – dịch toàn bộ \~150 string user-visible sang tiếng Việt
  - GalleryContent.kt – `"Album"` → `stringResource(gallery_albums_label)`, `"Chưa phân loại"` → `stringResource(gallery_filter_unassigned)`
  - GalleryScreen.kt – `"Chế độ xem"` hardcoded → `stringResource(view_mode_button)`
  - AlbumDetailScreen.kt – contentDescription dùng `view_mode_button` resource
- Chi tiết:
  - Tất cả common strings, settings, import/export, onboarding, backup, biometric, sorting, video player, telemetry, migration, v.v. đều đã dịch
  - Thêm string resource `view_mode_button` cho contentDescription nút chế độ xem
  - Sửa lỗi resource warning `migration_running_progress` thêm `formatted="false"`

### Fix Delete after Import – Improved deletion logic based on Scoped Storage guidelines
- File thay đổi: D:/AndroidStudioProjects/PHOTOQT/app/src/main/java/com/quangthe/photoqt/gallery/ui/importing/ImportBottomSheetDialogFragment.kt
- Chi tiết: 
    - Triển khai `MediaStore.createDeleteRequest` cho Android 11+ để tránh double dialog.
    - Chuyển đổi URI từ Photo Picker sang Collection URI (Images/Video) để MediaStore chấp nhận xoá.
    - Thêm fallback xoá qua `DocumentsContract.deleteDocument` cho các file SAF (Downloads, SD Card).
    - Tách biệt logic xử lý URI MediaStore và các loại URI khác.
