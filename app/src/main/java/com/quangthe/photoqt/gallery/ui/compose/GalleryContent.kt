package com.quangthe.photoqt.gallery.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.gallery.albums.ui.compose.AlbumItem
import com.quangthe.photoqt.gallery.components.AlbumsGrid
import com.quangthe.photoqt.gallery.components.MultiSelectionState
import com.quangthe.photoqt.gallery.components.PhotoGallery
import com.quangthe.photoqt.gallery.components.PhotoTile
import com.quangthe.photoqt.gallery.components.rememberMultiSelectionState
import com.quangthe.photoqt.gallery.ui.GalleryUiEvent
import com.quangthe.photoqt.gallery.ui.GalleryUiState
import com.quangthe.photoqt.model.database.entity.PhotoType
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.ui.theme.AppTheme
import java.util.UUID

@Composable
fun GalleryContent(
    uiState: GalleryUiState.Content,
    handleUiEvent: (GalleryUiEvent) -> Unit,
    multiSelectionState: MultiSelectionState,
    modifier: Modifier = Modifier,
    lazyPhotos: LazyPagingItems<PhotoTile>? = null,
    onAlbumClicked: (String) -> Unit = {},
) {
    val selectedType = uiState.selectedSmartCollection

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.showClassification) {
            if (uiState.classificationAlbums.isNotEmpty()) {
                Text(
                    text = "Album",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                )
                AlbumsGrid(
                    albums = uiState.classificationAlbums,
                    onAlbumClicked = onAlbumClicked,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Chưa phân loại",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                )
            } else {
                Text(
                    text = "Chưa phân loại",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    SmartCollectionType.entries.filter { it != SmartCollectionType.Trash }
                ) { type ->
                    val isSelected = type == selectedType
                    Card(
                        onClick = {
                            handleUiEvent(
                                if (isSelected) GalleryUiEvent.SelectSmartCollection(null)
                                else GalleryUiEvent.SelectSmartCollection(type)
                            )
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier
                            .size(width = 96.dp, height = 72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                        ) {
                            Text(
                                text = stringResource(type.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        PhotoGallery(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            photos = uiState.photos,
            lazyPhotos = lazyPhotos,
            albumName = null,
            multiSelectionState = multiSelectionState,
            onOpenPhoto = { handleUiEvent(GalleryUiEvent.OpenPhoto(it)) },
            onExport = { targetUri ->
                handleUiEvent(
                    GalleryUiEvent.OnExport(
                        multiSelectionState.selectedItems.value.toList(),
                        targetUri
                    )
                )
            },
            onDelete = {
                handleUiEvent(
                    GalleryUiEvent.OnDelete(
                        multiSelectionState.selectedItems.value.toList()
                    )
                )
            },
            onImportChoice = {
                handleUiEvent(GalleryUiEvent.OnImportChoice(it))
            },
            additionalMultiSelectionActions = {
                val selectedUuids = multiSelectionState.selectedItems.value.toList()
                HorizontalDivider()
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(R.string.menu_ms_add_to_album)) },
                    onClick = {
                        handleUiEvent(GalleryUiEvent.OnAddToAlbum)
                        multiSelectionState.dismissMore()
                    },
                )
                if (selectedType == SmartCollectionType.Favorites) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.menu_ms_remove_from_favorites)) },
                        onClick = {
                            handleUiEvent(GalleryUiEvent.RemoveFromFavorites(selectedUuids))
                            multiSelectionState.dismissMore()
                        },
                    )
                } else {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.menu_ms_add_to_favorites)) },
                        onClick = {
                            handleUiEvent(GalleryUiEvent.AddToFavorites(selectedUuids))
                            multiSelectionState.dismissMore()
                        },
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF, showSystemUi = true)
@Composable
fun GalleryContentPreview() {
    AppTheme {
        GalleryContent(
            uiState = GalleryUiState.Content(
                photos = listOf(
                    PhotoTile("", PhotoType.JPEG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.MP4, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.GIF, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.MPEG, "1"),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, "2"),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                    PhotoTile("", PhotoType.PNG, UUID.randomUUID().toString()),
                ),
                showAlbumSelectionDialog = false,
                sort = SortConfig.Gallery.default,
            ),

            handleUiEvent = {},
            multiSelectionState = rememberMultiSelectionState(items = emptyList()),
        )
    }
}
