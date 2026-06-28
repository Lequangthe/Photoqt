package com.quangthe.photoqt.gallery.albums.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.ui.AlbumsUiEvent
import com.quangthe.photoqt.gallery.components.AlbumsGrid
import com.quangthe.photoqt.ui.components.MagicFab
import com.quangthe.photoqt.ui.theme.AppTheme

@Composable
fun AlbumsContent(
    content: AlbumsUiState.Content,
    handleUiEvent: (AlbumsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    showEmptyHint: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        if (!showEmptyHint) {
            AlbumsGrid(
                albums = content.albums,
                onAlbumClicked = { handleUiEvent(AlbumsUiEvent.OpenAlbum(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(92.dp).alpha(0.5f)
                )
                Text(
                    text = stringResource(R.string.gallery_albums_placeholder),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }

        MagicFab(
            label = stringResource(R.string.magic_fab_new_album_label),
            onClick = {
                handleUiEvent(AlbumsUiEvent.ShowCreateDialog)
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AlbumsContentPreview() {
    AppTheme {
        AlbumsContent(
            content = AlbumsUiState.Content(
                listOf(
                    AlbumItem(
                        id = "1",
                        name = "Album 1",
                        itemCount = 10,
                    ),
                    AlbumItem(
                        id = "2",
                        name = "Album 2",
                        itemCount = 20,
                    ),
                    AlbumItem(
                        id = "3",
                        name = "Album 3",
                        itemCount = 30,
                    ),
                    AlbumItem(
                        id = "4",
                        name = "Album 4",
                        itemCount = 40
                    ),
                    AlbumItem(
                        id = "5",
                        name = "Album 5",
                        itemCount = 50
                    ),
                )
            ),
            handleUiEvent = {}
        )
    }
}
