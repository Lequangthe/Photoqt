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

package com.quangthe.photoqt.gallery.components

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.components.GalleryViewMode
import com.quangthe.photoqt.model.database.entity.PhotoType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.quangthe.photoqt.other.extensions.launchAndIgnoreTimer
import com.quangthe.photoqt.settings.ui.compose.LocalConfig
import com.quangthe.photoqt.transcoding.compose.model.EncryptedImageRequestData
import com.quangthe.photoqt.transcoding.compose.rememberEncryptedImagePainter
import com.quangthe.photoqt.ui.components.ConfirmationDialog
import com.quangthe.photoqt.ui.components.MagicFab
import com.quangthe.photoqt.ui.components.MultiSelectionMenu
import com.quangthe.photoqt.ui.theme.AppTheme

private const val PORTRAIT_COLUMN_COUNT = 3
private const val LANDSCAPE_COLUMN_COUNT = 6

@Composable
fun PhotoGallery(
    viewMode: GalleryViewMode = GalleryViewMode.Grid,
    photos: List<PhotoTile>,
    albumName: String?,
    multiSelectionState: MultiSelectionState,
    onOpenPhoto: (PhotoTile) -> Unit,
    onExport: (Uri?) -> Unit,
    onDelete: () -> Unit,
    onImportChoice: (ImportChoice) -> Unit,
    additionalMultiSelectionActions: @Composable (ColumnScope.() -> Unit),
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
    onViewModeChanged: (GalleryViewMode) -> Unit = {},
) {
    val activity = LocalActivity.current
    var importMenuBottomSheetVisible by remember { mutableStateOf(false) }


    // Hide magic fab menu when multi selection active
    LaunchedEffect(multiSelectionState.isActive.value) {
        if (multiSelectionState.isActive.value) {
            importMenuBottomSheetVisible = false
        }
    }

    var scale by remember { mutableStateOf(1f) }

    Box(modifier = modifier
        .fillMaxSize()
        .pointerInput(viewMode) {
            detectTransformGestures { _, _, zoom, _ ->
                scale *= zoom
                if (scale > 1.35f) {
                    // Zoom in -> Fewer columns (Bigger items)
                    val nextMode = when (viewMode) {
                        GalleryViewMode.GridCompact -> GalleryViewMode.GridSmall
                        GalleryViewMode.GridSmall -> GalleryViewMode.Grid
                        GalleryViewMode.Grid -> GalleryViewMode.Column
                        GalleryViewMode.List -> GalleryViewMode.Grid
                        GalleryViewMode.Timeline -> GalleryViewMode.Grid
                        else -> null
                    }
                    nextMode?.let { onViewModeChanged(it) }
                    scale = 1f
                } else if (scale < 0.65f) {
                    // Zoom out -> More columns (Smaller items)
                    val nextMode = when (viewMode) {
                        GalleryViewMode.Column -> GalleryViewMode.Grid
                        GalleryViewMode.Grid -> GalleryViewMode.GridSmall
                        GalleryViewMode.GridSmall -> GalleryViewMode.GridCompact
                        GalleryViewMode.List -> GalleryViewMode.GridSmall
                        GalleryViewMode.Timeline -> GalleryViewMode.GridSmall
                        else -> null
                    }
                    nextMode?.let { onViewModeChanged(it) }
                    scale = 1f
                }
            }
        }
    ) {
        when (viewMode) {
            GalleryViewMode.Grid,
            GalleryViewMode.GridSmall,
            GalleryViewMode.GridCompact -> PhotoGrid(
                viewMode = viewMode,
                photos = photos,
                multiSelectionState = multiSelectionState,
                openPhoto = onOpenPhoto,
                lazyPhotos = lazyPhotos,
            )
            GalleryViewMode.List -> PhotoListView(
                photos = photos,
                multiSelectionState = multiSelectionState,
                openPhoto = onOpenPhoto,
                lazyPhotos = lazyPhotos,
            )
            GalleryViewMode.Column -> PhotoColumnView(
                photos = photos,
                multiSelectionState = multiSelectionState,
                openPhoto = onOpenPhoto,
                lazyPhotos = lazyPhotos,
            )
            GalleryViewMode.Timeline -> PhotoTimelineView(
                photos = photos,
                multiSelectionState = multiSelectionState,
                openPhoto = onOpenPhoto,
                lazyPhotos = lazyPhotos,
            )
        }

        AnimatedVisibility(
            visible = multiSelectionState.isActive.value.not(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
        ) {
            MagicFab(
                label = stringResource(R.string.import_menu_fab_label),
                onClick = {
                    importMenuBottomSheetVisible = true
                }
            )
        }

        ImportMenuBottomSheet(
            open = importMenuBottomSheetVisible,
            onDismissRequest = {
                importMenuBottomSheetVisible = false
            },
            onImportChoice = onImportChoice,
            albumName = albumName,
        )

        var showDeleteConfirmationDialog by remember {
            mutableStateOf(false)
        }

        var showExportConfirmationDialog by remember {
            mutableStateOf(false)
        }

        var exportDirectoryUri by remember { mutableStateOf<Uri?>(null) }

        val pickExportTargetLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { exportTarget ->
                exportTarget ?: return@rememberLauncherForActivityResult
                exportDirectoryUri = exportTarget
                showExportConfirmationDialog = true
            }

        ConfirmationDialog(
            show = showDeleteConfirmationDialog,
            onDismissRequest = { showDeleteConfirmationDialog = false },
            text = stringResource(
                R.string.delete_are_you_sure,
                multiSelectionState.selectedItems.value.size
            ),
            onConfirm = {
                onDelete()
                multiSelectionState.cancelSelection()
            }
        )

        ConfirmationDialog(
            show = showExportConfirmationDialog,
            onDismissRequest = { showExportConfirmationDialog = false },
            text = stringResource(
                if (LocalConfig.current?.deleteExportedFiles == true) {
                    R.string.export_and_delete_are_you_sure
                } else {
                    R.string.export_are_you_sure
                },
                multiSelectionState.selectedItems.value.size
            ),
            onConfirm = {
                onExport(exportDirectoryUri)
                multiSelectionState.cancelSelection()
            }
        )

        MultiSelectionMenu(
            modifier = Modifier.align(Alignment.BottomCenter),
            multiSelectionState = multiSelectionState,
        ) {
            if (multiSelectionState.selectedItems.value.size == 1) {
                val singlePhoto = photos.find {
                    it.uuid == multiSelectionState.selectedItems.value.first()
                }
                if (singlePhoto != null) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_eye),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.common_open)) },
                        onClick = {
                            onOpenPhoto(singlePhoto)
                            multiSelectionState.cancelSelection()
                        },
                    )
                }
            }
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_select_all),
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.menu_ms_select_all)) },
                onClick = {
                    multiSelectionState.selectAll()
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    RadioButton(
                        selected = multiSelectionState.rectangleSelectEnabled.value,
                        onClick = null,
                    )
                },
                text = { Text(stringResource(R.string.menu_ms_rectangle_select)) },
                onClick = {
                    multiSelectionState.rectangleSelectEnabled.value = true
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    RadioButton(
                        selected = !multiSelectionState.rectangleSelectEnabled.value,
                        onClick = null,
                    )
                },
                text = { Text(stringResource(R.string.menu_ms_free_select)) },
                onClick = {
                    multiSelectionState.rectangleSelectEnabled.value = false
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.common_delete)) },
                onClick = {
                    showDeleteConfirmationDialog = true
                    multiSelectionState.dismissMore()
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_export),
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(R.string.common_export)) },
                onClick = {
                    pickExportTargetLauncher.launchAndIgnoreTimer(
                        input = null,
                        activity = activity,
                    )
                    multiSelectionState.dismissMore()
                },
            )

            additionalMultiSelectionActions()
        }
    }
}

@Composable
private fun PhotoGrid(
    viewMode: GalleryViewMode,
    photos: List<PhotoTile>,
    multiSelectionState: MultiSelectionState,
    openPhoto: (PhotoTile) -> Unit,
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
) {
    val gridState: LazyGridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current

    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    val columnCount = when (viewMode) {
        GalleryViewMode.Grid -> if (isPortrait) 3 else 6
        GalleryViewMode.GridSmall -> if (isPortrait) 4 else 8
        GalleryViewMode.GridCompact -> if (isPortrait) 6 else 12
        else -> if (isPortrait) 3 else 6
    }

    val currentPhotos by rememberUpdatedState(photos)
    val currentLazyPhotos by rememberUpdatedState(lazyPhotos)
    val currentMsState by rememberUpdatedState(multiSelectionState)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position

                    fun itemInfoAt(pos: Offset): Pair<PhotoTile, Int>? {
                        val info = gridState.layoutInfo.visibleItemsInfo.find {
                            pos.x >= it.offset.x &&
                                    pos.x <= it.offset.x + it.size.width &&
                                    pos.y >= it.offset.y &&
                                    pos.y <= it.offset.y + it.size.height
                        } ?: return null
                        val item = currentLazyPhotos?.peek(info.index) ?: currentPhotos.getOrNull(info.index)
                        return if (item != null) item to info.index else null
                    }

                    val downInfo = itemInfoAt(downPos) ?: return@awaitEachGesture
                    val downItem = downInfo.first
                    val downIndex = downInfo.second

                    var isDrag = false

                    val released = withTimeoutOrNull(
                        viewConfiguration.longPressTimeoutMillis
                    ) {
                        var event = awaitPointerEvent()
                        while (event.changes.any { it.pressed }) {
                            val change = event.changes.first()
                            if (!isDrag &&
                                (change.position - downPos).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                isDrag = true
                                return@withTimeoutOrNull false
                            }
                            event = awaitPointerEvent()
                        }
                        true
                    }

                    when {
                        released == true -> {
                            if (currentMsState.selectedItems.value.isNotEmpty()) {
                                if (currentMsState.selectedItems.value.contains(downItem.uuid)) {
                                    currentMsState.deselectItem(downItem.uuid)
                                } else {
                                    currentMsState.selectItem(downItem.uuid)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            } else {
                                openPhoto(downItem)
                            }
                        }

                        released == null -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentMsState.selectItem(downItem.uuid)

                            if (currentMsState.rectangleSelectEnabled.value) {
                                var lastMinRow = downIndex / columnCount
                                var lastMinCol = downIndex % columnCount
                                var lastMaxRow = lastMinRow
                                var lastMaxCol = lastMinCol

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.pressed } ?: break
                                    change.consume()
                                    val currentItemInfo = itemInfoAt(change.position) ?: continue
                                    val currentIndex = currentItemInfo.second

                                    val curRow = currentIndex / columnCount
                                    val curCol = currentIndex % columnCount

                                    val minRow = minOf(downIndex / columnCount, curRow)
                                    val maxRow = maxOf(downIndex / columnCount, curRow)
                                    val minCol = minOf(downIndex % columnCount, curCol)
                                    val maxCol = maxOf(downIndex % columnCount, curCol)

                                    if (minRow != lastMinRow || maxRow != lastMaxRow ||
                                        minCol != lastMinCol || maxCol != lastMaxCol
                                    ) {
                                        val uuids = mutableSetOf<String>()
                                        val totalCount = currentLazyPhotos?.itemCount ?: currentPhotos.size
                                        for (r in minRow..maxRow) {
                                            for (c in minCol..maxCol) {
                                                val idx = r * columnCount + c
                                                if (idx in 0 until totalCount) {
                                                    val item = currentLazyPhotos?.peek(idx) ?: currentPhotos.getOrNull(idx)
                                                    if (item != null) uuids.add(item.uuid)
                                                }
                                            }
                                        }

                                        currentMsState.replaceSelection(uuids)
                                        lastMinRow = minRow
                                        lastMaxRow = maxRow
                                        lastMinCol = minCol
                                        lastMaxCol = maxCol
                                    }
                                }
                            } else {
                                var lastItemUuid = downItem.uuid
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.pressed } ?: break
                                    change.consume()
                                    val currentItem = itemInfoAt(change.position)?.first
                                    if (currentItem != null &&
                                        currentItem.uuid != lastItemUuid
                                    ) {
                                        if (!currentMsState.selectedItems.value.contains(
                                                currentItem.uuid
                                            )
                                        ) {
                                            currentMsState.selectItem(currentItem.uuid)
                                        }
                                        lastItemUuid = currentItem.uuid
                                    }
                                }
                            }
                        }
                    }
                }
            },
        state = gridState
    ) {
        if (lazyPhotos != null) {
            items(lazyPhotos.itemCount, key = lazyPhotos.itemKey { it.uuid }) { index ->
                val photo = lazyPhotos[index] ?: return@items
                GalleryPhotoTile(
                    photoTile = photo,
                    multiSelectionActive = multiSelectionState.isActive.value,
                    selected = multiSelectionState.selectedItems.value.contains(photo.uuid),
                    modifier = Modifier.animateItem(),
                )
            }
        } else {
            items(photos, key = { it.uuid }) {
                GalleryPhotoTile(
                    photoTile = it,
                    multiSelectionActive = multiSelectionState.isActive.value,
                    selected = multiSelectionState.selectedItems.value.contains(it.uuid),
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private val VideoIconSize = 20.dp
private val SelectedPadding = 15.dp
private val CheckmarkPadding = SelectedPadding - 9.dp

@Composable
fun Modifier.multiSelectionItem(selected: Boolean): Modifier {
    val animatedPadding by animateDpAsState(
        targetValue = if (selected) { SelectedPadding } else { 0.dp }
    )
    val animatedShape by animateDpAsState(
        targetValue = if (selected) { 12.dp } else { 0.dp }
    )

    return this
        .padding(animatedPadding)
        .clip(RoundedCornerShape(animatedShape))
}

@Composable
private fun GalleryPhotoTile(
    modifier: Modifier = Modifier,
    photoTile: PhotoTile,
    multiSelectionActive: Boolean,
    selected: Boolean,
) {
    Box(
        modifier = modifier.padding(.5.dp)
    ) {
        val contentModifier = Modifier
            .multiSelectionItem(selected)
            .fillMaxSize()
            .aspectRatio(1f)

        if (LocalInspectionMode.current) {
            Box(
                modifier = contentModifier.background(Color.DarkGray)
            )
        } else {
            val requestData = remember(photoTile) {
                EncryptedImageRequestData(
                    internalFileName = photoTile.internalThumbnailFileName,
                    mimeType = photoTile.type.mimeType
                )
            }

            Image(
                painter = rememberEncryptedImagePainter(requestData),
                contentDescription = photoTile.fileName,
                modifier = contentModifier
            )
        }

        AnimatedVisibility(
            visible = photoTile.type.isVideo && !selected,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .padding(2.dp)
                .size(VideoIconSize)
                .align(Alignment.BottomStart)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_videocam),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(12.dp),
                        shadow = Shadow(
                            radius = 6.dp,
                            alpha = 0.3f
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = photoTile.isFavorite && !selected,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .padding(2.dp)
                .size(VideoIconSize)
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(12.dp),
                        shadow = Shadow(
                            radius = 6.dp,
                            alpha = 0.3f
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = photoTile.pinned && !selected,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .padding(2.dp)
                .size(VideoIconSize)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pin),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(12.dp),
                        shadow = Shadow(
                            radius = 6.dp,
                            alpha = 0.3f
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = multiSelectionActive && selected,
            enter = scaleIn(),
            exit = scaleOut(),
            ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(CheckmarkPadding)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .align(Alignment.TopStart)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
        else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun PhotoListView(
    photos: List<PhotoTile>,
    multiSelectionState: MultiSelectionState,
    openPhoto: (PhotoTile) -> Unit,
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
) {
    val totalCount = lazyPhotos?.itemCount ?: photos.size

    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier.fillMaxWidth()) {
        val list = if (lazyPhotos != null) {
            (0 until totalCount).mapNotNull { lazyPhotos.peek(it) }
        } else photos

        items(list, key = { it.uuid }) { photo ->
            ListPhotoRow(
                photoTile = photo,
                multiSelectionActive = multiSelectionState.isActive.value,
                selected = multiSelectionState.selectedItems.value.contains(photo.uuid),
                onClick = {
                    if (multiSelectionState.isActive.value) {
                        multiSelectionState.selectItem(photo.uuid)
                    } else {
                        openPhoto(photo)
                    }
                },
                onLongClick = {
                    multiSelectionState.selectItem(photo.uuid)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListPhotoRow(
    photoTile: PhotoTile,
    multiSelectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            val requestData = remember(photoTile) {
                EncryptedImageRequestData(
                    internalFileName = photoTile.internalThumbnailFileName,
                    mimeType = photoTile.type.mimeType
                )
            }
            Image(
                painter = rememberEncryptedImagePainter(requestData),
                contentDescription = photoTile.fileName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = photoTile.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = formatDate(photoTile.importedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatFileSize(photoTile.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (photoTile.type.isVideo) {
            Icon(
                painter = painterResource(R.drawable.ic_videocam),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp).size(18.dp),
            )
        }
        if (photoTile.isFavorite) {
            Icon(
                painter = painterResource(R.drawable.ic_favorite),
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun PhotoColumnView(
    photos: List<PhotoTile>,
    multiSelectionState: MultiSelectionState,
    openPhoto: (PhotoTile) -> Unit,
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
) {
    val totalCount = lazyPhotos?.itemCount ?: photos.size

    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier.fillMaxWidth()) {
        val list = if (lazyPhotos != null) {
            (0 until totalCount).mapNotNull { lazyPhotos.peek(it) }
        } else photos

        items(list, key = { it.uuid }) { photo ->
            ColumnPhotoTile(
                photoTile = photo,
                multiSelectionActive = multiSelectionState.isActive.value,
                selected = multiSelectionState.selectedItems.value.contains(photo.uuid),
                onClick = {
                    if (multiSelectionState.isActive.value) {
                        multiSelectionState.selectItem(photo.uuid)
                    } else {
                        openPhoto(photo)
                    }
                },
                onLongClick = {
                    multiSelectionState.selectItem(photo.uuid)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnPhotoTile(
    photoTile: PhotoTile,
    multiSelectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        val shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp,
        )
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)) {
            if (!LocalInspectionMode.current) {
                val requestData = remember(photoTile) {
                    EncryptedImageRequestData(
                        internalFileName = photoTile.internalThumbnailFileName,
                        mimeType = photoTile.type.mimeType
                    )
                }
                Image(
                    painter = rememberEncryptedImagePainter(requestData),
                    contentDescription = photoTile.fileName,
                    modifier = Modifier.fillMaxSize().clip(shape)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().clip(shape).background(Color.DarkGray))
            }
            if (photoTile.type.isVideo) {
                Icon(
                    painter = painterResource(R.drawable.ic_videocam),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp).size(24.dp).align(Alignment.BottomStart),
                )
            }
            if (multiSelectionActive && selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(4.dp).align(Alignment.TopStart),
                )
            }
        }
        Text(
            text = photoTile.fileName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
    }
}

private data class TimelineGroup(
    val label: String,
    val items: List<PhotoTile>,
)

private fun groupByDate(photos: List<PhotoTile>): List<TimelineGroup> {
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val grouped = photos.groupBy { photo ->
        val cal = Calendar.getInstance().apply { timeInMillis = photo.importedAt }
        when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hôm nay"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Hôm qua"
            else -> dateFormat.format(Date(photo.importedAt))
        }
    }

    val order = listOf("Hôm nay", "Hôm qua") + grouped.keys
        .filter { it != "Hôm nay" && it != "Hôm qua" }
        .sortedByDescending { dateString ->
            try { dateFormat.parse(dateString)?.time ?: 0L } catch (_: Exception) { 0L }
        }

    return order.mapNotNull { label ->
        grouped[label]?.let { TimelineGroup(label, it) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTimelineView(
    photos: List<PhotoTile>,
    multiSelectionState: MultiSelectionState,
    openPhoto: (PhotoTile) -> Unit,
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
) {
    val totalCount = lazyPhotos?.itemCount ?: photos.size
    val list = if (lazyPhotos != null) {
        (0 until totalCount).mapNotNull { lazyPhotos.peek(it) }
    } else photos

    val groups = remember(list) { groupByDate(list) }

    androidx.compose.foundation.lazy.LazyColumn(modifier = modifier.fillMaxWidth()) {
        groups.forEach { group ->
            stickyHeader {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            items(group.items, key = { it.uuid }) { photo ->
                TimelinePhotoRow(
                    photoTile = photo,
                    multiSelectionActive = multiSelectionState.isActive.value,
                    selected = multiSelectionState.selectedItems.value.contains(photo.uuid),
                    onClick = {
                        if (multiSelectionState.isActive.value) {
                            multiSelectionState.selectItem(photo.uuid)
                        } else {
                            openPhoto(photo)
                        }
                    },
                    onLongClick = {
                        multiSelectionState.selectItem(photo.uuid)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelinePhotoRow(
    photoTile: PhotoTile,
    multiSelectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            val requestData = remember(photoTile) {
                EncryptedImageRequestData(
                    internalFileName = photoTile.internalThumbnailFileName,
                    mimeType = photoTile.type.mimeType
                )
            }
            Image(
                painter = rememberEncryptedImagePainter(requestData),
                contentDescription = photoTile.fileName,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
            )
            if (multiSelectionActive && selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(2.dp).align(Alignment.TopStart),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = photoTile.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = formatTimeAgo(photoTile.importedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatFileSize(photoTile.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Vừa xong"
        minutes < 60 -> "${minutes} phút trước"
        hours < 24 -> "${hours} giờ trước"
        days < 7 -> "${days} ngày trước"
        else -> formatDate(timestamp)
    }
}

@Preview
@Composable
private fun PhotoGridPreview() {
    AppTheme {
        Scaffold {
            PhotoGallery(
                modifier = Modifier.padding(it),
                photos = listOf(
                    PhotoTile("", PhotoType.JPEG, "1"),
                    PhotoTile("", PhotoType.MP4, "2"),
                    PhotoTile("", PhotoType.MP4, "3"),
                    PhotoTile("", PhotoType.JPEG, "4"),
                    PhotoTile("", PhotoType.JPEG, "5"),
                    PhotoTile("", PhotoType.MP4, "6"),
                ),
                albumName = null,
                multiSelectionState = MultiSelectionState(
                    allItems = listOf("1", "2", "3"),
                ),
                onOpenPhoto = {},
                onDelete = {},
                onExport = {},
                onImportChoice = {},
                additionalMultiSelectionActions = {},
            )
        }
    }
}

@Preview
@Composable
private fun PhotoGridPreviewWithSelection() {
    AppTheme {
        Scaffold {
            PhotoGallery(
                modifier = Modifier.padding(it),
                photos = listOf(
                    PhotoTile("", PhotoType.JPEG, "1"),
                    PhotoTile("", PhotoType.MP4, "2"),
                    PhotoTile("", PhotoType.MP4, "3"),
                    PhotoTile("", PhotoType.JPEG, "4"),
                    PhotoTile("", PhotoType.JPEG, "5"),
                    PhotoTile("", PhotoType.MP4, "6"),
                ),
                albumName = null,
                multiSelectionState = MultiSelectionState(
                    allItems = listOf("1", "2", "3"),
                ).apply {
                    selectItem("2")
                    selectItem("3")
                },
                onOpenPhoto = {},
                onDelete = {},
                onExport = {},
                onImportChoice = {},
                additionalMultiSelectionActions = {},
            )
        }
    }
}
