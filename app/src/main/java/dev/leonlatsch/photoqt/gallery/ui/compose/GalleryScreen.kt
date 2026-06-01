package dev.leonlatsch.photoqt.gallery.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
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
import dev.leonlatsch.photoqt.R
import dev.leonlatsch.photoqt.gallery.albums.domain.model.SmartCollectionType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.leonlatsch.photoqt.gallery.components.AlbumPickerDialog
import dev.leonlatsch.photoqt.gallery.components.ImportSharedDialog
import dev.leonlatsch.photoqt.gallery.components.rememberMultiSelectionState
import dev.leonlatsch.photoqt.gallery.ui.GalleryUiEvent
import dev.leonlatsch.photoqt.gallery.ui.GalleryUiState
import dev.leonlatsch.photoqt.gallery.ui.GalleryViewModel
import dev.leonlatsch.photoqt.news.newfeatures.ui.NewFeaturesSheet
import dev.leonlatsch.photoqt.sort.domain.SortConfig
import dev.leonlatsch.photoqt.sort.ui.SortingMenu
import dev.leonlatsch.photoqt.sort.ui.SortingMenuIconButton
import dev.leonlatsch.photoqt.ui.components.AppName
import dev.leonlatsch.photoqt.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    AppTheme {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { AppName() },
                    windowInsets = WindowInsets.statusBars,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (uiState is GalleryUiState.Content) {
                            val contentState = uiState as GalleryUiState.Content
                            val sort = contentState.sort

                            var showSortMenu by remember { mutableStateOf(false) }

                            FilterChip(
                                selected = contentState.showUnassignedOnly,
                                onClick = { viewModel.handleUiEvent(GalleryUiEvent.ToggleUnassignedFilter) },
                                label = {
                                    Text(
                                        if (contentState.showUnassignedOnly) stringResource(R.string.gallery_filter_unassigned)
                                        else stringResource(R.string.gallery_filter_all)
                                    )
                                },
                            )

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
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { contentPadding ->
            val modifier = Modifier.padding(top = contentPadding.calculateTopPadding())

            when (uiState) {
                is GalleryUiState.Empty -> GalleryPlaceholder(
                    handleUiEvent = { viewModel.handleUiEvent(it) },
                    modifier = modifier,
                )

                is GalleryUiState.Content -> {
                    val contentUiState = uiState as GalleryUiState.Content
                    val multiSelectionState = rememberMultiSelectionState(
                        items = contentUiState.photos.map { it.uuid }
                    )

                    GalleryContent(
                        uiState = contentUiState,
                        handleUiEvent = { viewModel.handleUiEvent(it) },
                        multiSelectionState = multiSelectionState,
                        modifier = modifier,
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
