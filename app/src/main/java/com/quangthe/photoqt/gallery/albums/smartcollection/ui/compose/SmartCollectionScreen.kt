package com.quangthe.photoqt.gallery.albums.smartcollection.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.smartcollection.ui.SmartCollectionUiEvent
import com.quangthe.photoqt.gallery.albums.smartcollection.ui.SmartCollectionViewModel
import com.quangthe.photoqt.gallery.components.PhotoGallery
import com.quangthe.photoqt.gallery.components.rememberMultiSelectionState
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.sort.ui.SortingMenu
import com.quangthe.photoqt.sort.ui.SortingMenuIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCollectionScreen(viewModel: SmartCollectionViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sort by viewModel.sortFlow.collectAsStateWithLifecycle(initialValue = SortConfig.Gallery.default)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val photos = uiState.photos
    val multiSelectionState = rememberMultiSelectionState(items = photos.map { it.uuid })

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.process_close)
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (!uiState.isTrash) {
                        SortingMenuIconButton(
                            config = SortConfig.Gallery,
                            sort = sort,
                            onClick = { showSortMenu = true }
                        )
                        SortingMenu(
                            config = SortConfig.Gallery,
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            sort = sort,
                            onSortChanged = { viewModel.handleSortChanged(it) },
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        if (uiState.isTrash && photos.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding())
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp).alpha(0.5f)
                    )
                    Text(
                        text = stringResource(R.string.trash_is_empty),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            PhotoGallery(
                photos = photos,
                albumName = null,
                multiSelectionState = multiSelectionState,
                onOpenPhoto = { },
                onExport = { },
                onDelete = {
                    if (uiState.isTrash) {
                        viewModel.handleEvent(SmartCollectionUiEvent.PermanentDelete(
                            multiSelectionState.selectedItems.value.toList()
                        ))
                    } else {
                        viewModel.handleEvent(SmartCollectionUiEvent.MoveToTrash(
                            multiSelectionState.selectedItems.value.toList()
                        ))
                    }
                    multiSelectionState.cancelSelection()
                },
                onImportChoice = { },
                additionalMultiSelectionActions = {
                    val selectedUuids = multiSelectionState.selectedItems.value.toList()
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.menu_ms_add_to_favorites)) },
                        onClick = {
                            viewModel.handleEvent(SmartCollectionUiEvent.ToggleFavorite(selectedUuids))
                            multiSelectionState.dismissMore()
                        },
                    )
                    if (uiState.isTrash) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_restore),
                                    contentDescription = null
                                )
                            },
                            text = { Text(stringResource(R.string.trash_restore)) },
                            onClick = {
                                viewModel.handleEvent(SmartCollectionUiEvent.RestoreFromTrash(selectedUuids))
                                multiSelectionState.cancelSelection()
                            },
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = null
                                )
                            },
                            text = { Text(stringResource(R.string.trash_permanently_delete)) },
                            onClick = {
                                viewModel.handleEvent(SmartCollectionUiEvent.PermanentDelete(selectedUuids))
                                multiSelectionState.cancelSelection()
                            },
                        )
                    }
                },
                modifier = Modifier
                    .padding(top = contentPadding.calculateTopPadding())
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            )
        }
    }
}
