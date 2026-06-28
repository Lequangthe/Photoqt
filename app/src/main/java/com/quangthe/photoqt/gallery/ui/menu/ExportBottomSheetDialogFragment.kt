package com.quangthe.photoqt.gallery.ui.menu

import android.net.Uri
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.R
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessBottomSheetDialogFragment

@AndroidEntryPoint
class ExportBottomSheetDialogFragment(
    items: List<String>,
    private val target: Uri,
) : BaseProcessBottomSheetDialogFragment<String>(
    items,
    R.string.export_exporting,
    true
) {

    override val viewModel: ExportViewModel by viewModels()

    override fun prepareViewModel(items: List<String>?) {
        super.prepareViewModel(items)
        viewModel.target = target
    }
}
