package com.quangthe.photoqt.gallery.ui.menu

import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import com.quangthe.photoqt.model.repositories.PhotoRepository
import com.quangthe.photoqt.uicomponnets.base.processdialogs.BaseProcessViewModel
import javax.inject.Inject

@HiltViewModel
class DeleteViewModel @Inject constructor(
    app: Application,
    private val photoRepository: PhotoRepository
) : BaseProcessViewModel<String>(app) {

    override suspend fun processItem(item: String) {
        if (item.isEmpty()) {
            failuresOccurred = true
            return
        }

        val photo = photoRepository.get(item)
        val success = photoRepository.safeDeletePhoto(photo)
        if (!success) {
            failuresOccurred = true
        }
    }
}
