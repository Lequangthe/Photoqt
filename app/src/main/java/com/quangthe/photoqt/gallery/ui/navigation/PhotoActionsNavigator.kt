package com.quangthe.photoqt.gallery.ui.navigation

import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import com.quangthe.photoqt.gallery.ui.menu.DeleteBottomSheetDialogFragment
import com.quangthe.photoqt.gallery.ui.menu.ExportBottomSheetDialogFragment
import com.quangthe.photoqt.imageviewer.ui.ImageViewerFragmentDirections
import com.quangthe.photoqt.model.database.entity.Photo
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.other.extensions.show
import javax.inject.Inject

class PhotoActionsNavigator @Inject constructor(
    private val photoRepository: PhotoRepository,
) {
    fun navigate(action: PhotoAction, navController: NavController, fragment: Fragment) {
        when (action) {
            is PhotoAction.DeletePhotos -> confirmAndDelete(
                action.selectedItems,
                fragment.childFragmentManager,
            )

            is PhotoAction.ExportPhotos -> confirmAndExport(
                action.selectedItems,
                action.target,
                fragment.childFragmentManager,
            )

            is PhotoAction.OpenPhoto -> navigateOpenPhoto(action.photoUUID, action.albumUUID, navController)
        }
    }

    private fun confirmAndExport(
        selectedItems: List<String>,
        target: Uri,
        fragmentManager: FragmentManager,
    ) {
        ExportBottomSheetDialogFragment(selectedItems, target).show(fragmentManager)
    }

    private fun confirmAndDelete(
        selectedItems: List<String>,
        fragmentManager: FragmentManager,
    ) {
        DeleteBottomSheetDialogFragment(selectedItems).show(fragmentManager)
    }

    private fun navigateOpenPhoto(photoUUID: String, albumUUID: String, navController: NavController) {
        val direction = ImageViewerFragmentDirections.actionGlobalImageViewerFragment(photoUuid = photoUUID, albumUuid = albumUUID)
        navController.navigate(direction)
    }
}

sealed interface PhotoAction {
    data class OpenPhoto(val photoUUID: String, val albumUUID: String = "") : PhotoAction
    data class DeletePhotos(val selectedItems: List<String>) : PhotoAction
    data class ExportPhotos(val selectedItems: List<String>, val target: Uri) : PhotoAction
}
