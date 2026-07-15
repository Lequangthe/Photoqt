package com.quangthe.photoqt.gallery.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import com.quangthe.photoqt.R
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quangthe.photoqt.gallery.components.AlbumPickerDialog
import com.quangthe.photoqt.gallery.components.GalleryViewMode
import com.quangthe.photoqt.gallery.components.ImportSharedDialog
import com.quangthe.photoqt.gallery.components.rememberMultiSelectionState
import com.quangthe.photoqt.gallery.ui.GalleryTab
import com.quangthe.photoqt.gallery.ui.GalleryUiEvent
import com.quangthe.photoqt.gallery.ui.GalleryUiState
import com.quangthe.photoqt.gallery.ui.GalleryViewModel
import com.quangthe.photoqt.news.newfeatures.ui.NewFeaturesSheet
import com.quangthe.photoqt.sort.domain.SortConfig
import com.quangthe.photoqt.sort.ui.SortingMenu
import com.quangthe.photoqt.sort.ui.SortingMenuIconButton
import com.quangthe.photoqt.ui.components.AppName
import com.quangthe.photoqt.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyPhotos = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    AppTheme {
        Scaffold(
            topBar = {
                Column {
                    LargeTopAppBar(
                        title = { AppName() },
                        windowInsets = WindowInsets.statusBars,
                        scrollBehavior = scrollBehavior,
                        actions = {
                            if (uiState is GalleryUiState.Content) {
                                val contentState = uiState as GalleryUiState.Content
                                val sort = contentState.sort
                                val viewMode = contentState.viewMode

                                var showSortMenu by remember { mutableStateOf(false) }
                                var showViewModeMenu by remember { mutableStateOf(false) }

                                SortingMenuIconButton(
                                    config = SortConfig.Gallery,
                                    sort = sort,
                                    onClick = { showSortMenu = true },
                                )

                                SortingMenu(
                                    config = SortConfig.Gallery,
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    sort = sort,
                                    onSortChanged = { sort ->
                                        viewModel.handleUiEvent(GalleryUiEvent.SortChanged(sort))
                                    }
                                )

                                IconButton(onClick = { showViewModeMenu = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_image),
                                        contentDescription = stringResource(R.string.view_mode_button),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showViewModeMenu,
                                    onDismissRequest = { showViewModeMenu = false },
                                ) {
                                    GalleryViewMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (mode == viewMode) "✓ ${stringResource(mode.labelRes)}"
                                                    else stringResource(mode.labelRes)
                                                )
                                            },
                                            onClick = {
                                                viewModel.handleUiEvent(GalleryUiEvent.ViewModeChanged(mode))
                                                showViewModeMenu = false
                                            },
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.openSmartCollection(SmartCollectionType.Trash) }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.collection_trash),
                                    )
                                }
                            }
                        }
                    )

                    if (uiState is GalleryUiState.Content) {
                        val contentState = uiState as GalleryUiState.Content
                        TabRow(
                            selectedTabIndex = contentState.selectedTab.ordinal,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            GalleryTab.entries.forEach { tab ->
                                Tab(
                                    selected = contentState.selectedTab == tab,
                                    onClick = { viewModel.handleUiEvent(GalleryUiEvent.TabChanged(tab)) },
                                    text = {
                                        Text(
                                            text = stringResource(
                                                if (tab == GalleryTab.All) R.string.gallery_tab_all
                                                else R.string.gallery_tab_classification
                                            ),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        )
{ contentPadding ->
            val modifier = Modifier.padding(top = contentPadding.calculateTopPadding())

            when (uiState) {
                is GalleryUiState.Empty -> GalleryPlaceholder(
                    handleUiEvent = { viewModel.handleUiEvent(it) },
                    modifier = modifier,
                )

                is GalleryUiState.Content -> {
                    val contentUiState = uiState as GalleryUiState.Content
                    val multiSelectionState = rememberMultiSelectionState(
                        items = lazyPhotos.itemSnapshotList.items.map { it?.uuid ?: "" }
                    )

                    GalleryContent(
                        uiState = contentUiState,
                        lazyPhotos = lazyPhotos,
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        multiSelectionState = multiSelectionState,
                        modifier = modifier,
                        viewMode = contentUiState.viewMode,
                        onAlbumClicked = { viewModel.openAlbum(it) },
                    )

                    AlbumPickerDialog(
                        visible = contentUiState.showAlbumSelectionDialog,
                        selectedItemIds = multiSelectionState.selectedItems.value.toList(),
                        onAlbumSelected = { multiSelectionState.cancelSelection() },
                        onDismissRequest = { viewModel.handleUiEvent(GalleryUiEvent.CancelAlbumSelection) }
                    )
                }
            }

            ImportSharedDialog()
        }

        NewFeaturesSheet()
    }
}
