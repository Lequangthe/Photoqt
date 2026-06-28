package com.quangthe.photoqt.gallery.ui.menu

import android.app.Application
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessViewModel
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    app: Application,
    private val photoRepository: PhotoRepository
) : BaseProcessViewModel<String>(app) {

    lateinit var target: Uri

    override suspend fun processItem(item: String) {
        val photo = photoRepository.get(item)
        val result = photoRepository.exportPhoto(photo, target)
        if (!result) {
            failuresOccurred = true
        }
    }
}
