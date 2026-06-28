package com.quangthe.photoqt.gallery.ui.menu

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.R
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessBottomSheetDialogFragment

@AndroidEntryPoint
class DeleteBottomSheetDialogFragment(
    items: List<String>
) : BaseProcessBottomSheetDialogFragment<String>(
    items,
    R.string.delete_deleting,
    true
) {

    override val viewModel: DeleteViewModel by viewModels()
}
