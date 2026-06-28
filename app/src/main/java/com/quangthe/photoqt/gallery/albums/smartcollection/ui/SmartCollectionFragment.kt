package com.quangthe.photoqt.gallery.albums.smartcollection.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import com.quangthe.photoqt.gallery.albums.domain.model.SmartCollectionType
import com.quangthe.photoqt.gallery.albums.smartcollection.ui.compose.SmartCollectionScreen
import com.quangthe.photoqt.gallery.ui.navigation.PhotoActionsNavigator
import com.quangthe.photoqt.other.extensions.launchLifecycleAwareJob
import com.quangthe.photoqt.settings.data.Config
import com.quangthe.photoqt.settings.ui.compose.LocalConfig
import com.quangthe.photoqt.transcoding.compose.LocalEncryptedImageLoader
import com.quangthe.photoqt.transcoding.di.EncryptedImageLoader
import com.quangthe.photoqt.ui.theme.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class SmartCollectionFragment : Fragment() {

    private val args: SmartCollectionFragmentArgs by navArgs()

    private val viewModel: SmartCollectionViewModel by viewModels()

    @Inject
    lateinit var photoActionsNavigator: PhotoActionsNavigator

    @Inject
    lateinit var config: Config

    @EncryptedImageLoader
    @Inject
    lateinit var encryptedImageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = SmartCollectionType.entries.find { it.id == args.collectionType }
            ?: SmartCollectionType.AllPhotos
        viewModel.init(type)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    CompositionLocalProvider(
                        LocalEncryptedImageLoader provides encryptedImageLoader,
                        LocalConfig provides config,
                    ) {
                        SmartCollectionScreen(viewModel, findNavController())
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchLifecycleAwareJob {
            viewModel.photoActions.collect { action ->
                photoActionsNavigator.navigate(action, findNavController(), this)
            }
        }
    }
}
