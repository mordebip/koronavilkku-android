package fi.thl.koronahaavi.home

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.*
import fi.thl.koronahaavi.databinding.FragmentHomeBinding
import fi.thl.koronahaavi.device.SystemState
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    private val viewModel by viewModels<HomeViewModel>()
    private val requestResolutionViewModel by activityViewModels<RequestResolutionViewModel>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            cardHomeSymptom.setOnClickListener {
                findNavController().navigateSafe(HomeFragmentDirections.toSymptoms())
            }

            cardHomePrevention.cardContainer.setOnClickListener {
                openLink(getString(R.string.home_prevention_url))
            }

            cardHomeStatistics.cardContainer.setOnClickListener {
                openLink(getString(R.string.home_statistics_url))
            }

            imageHomeThl.setOnClickListener {
                openLink(getString(R.string.home_thl_url))
            }
            buttonHomeTest.setOnClickListener {
                findNavController().navigateSafe(HomeFragmentDirections.toTest())
            }

            cardHomeExposure.setOnClickListener {
                navigateToExposureDetail()
            }
            buttonHomeExposureInstructions.setOnClickListener {
                navigateToExposureDetail()
            }

            buttonHomeAppEnable.setOnClickListener {
                enableSystem()
            }

            buttonHomeAppGuide.setOnClickListener {
                activity?.openGuide()
            }
        }

        viewModel.enableResolutionRequired().observe(viewLifecycleOwner, Observer {
            Timber.d("Got enableResolutionRequired event, starting request")
            it.getContentIfNotHandled()?.startResolutionForResult(
                requireActivity(), RequestResolutionViewModel.REQUEST_CODE_ENABLE
            )
        })

        requestResolutionViewModel.enableResolvedEvent().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { accepted ->
                if (accepted) {
                    Timber.d("Enable request accepted, trying again")
                    this.enableSystem()
                }
            }
        })

        viewModel.enableErrorEvent().observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { reason ->
                context?.showEnableFailureReasonDialog(reason)
            }
        })

        viewModel.systemState().observe(viewLifecycleOwner, Observer {
            it?.let { state ->
                updateStatusIcon(state)
                updateStatusText(state)
                updateEnableButton(state)
            }
        })

        viewModel.hasExposures.observe(viewLifecycleOwner, Observer {
            it?.let { state ->
                updateExposureLabel(state)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        (binding.imageHomeAppStatus.drawable as? AnimatedVectorDrawable)?.start()
    }

    override fun onStop() {
        super.onStop()

        // done in onStop instead of onPause, because stopping in onPause shows
        // a flicker in vector while its stopped/reset
        (binding.imageHomeAppStatus.drawable as? AnimatedVectorDrawable)?.stop()
    }

    private fun navigateToExposureDetail() = findNavController().navigateSafe(HomeFragmentDirections.toExposureDetail())

    private fun updateExposureLabel(hasExposures: Boolean) {
        binding.textHomeExposureLabel.setTextColor(requireContext().themeColor(
            if (hasExposures) R.attr.colorError else android.R.attr.textColorPrimary)
        )
    }

    private fun updateStatusText(state: SystemState) {
        binding.textHomeAppStatus.text = requireContext().getString(
            when (state) {
                SystemState.On -> R.string.home_status_system_enabled
                SystemState.Off -> R.string.home_status_system_disabled
                SystemState.Locked -> R.string.home_status_system_locked
            }
        )

        binding.textHomeAppStatus.setTextColor(requireContext().getColor(
            when (state) {
                SystemState.On -> R.color.mainBlue
                SystemState.Off -> R.color.mainRed
                SystemState.Locked -> R.color.textDarkGrey
            }
        ))

        binding.textHomeAppStatusExplained.text = requireContext().getText(
            when (state) {
                SystemState.On -> R.string.home_status_enabled_explained
                SystemState.Off -> R.string.home_status_disabled_explained
                SystemState.Locked -> R.string.home_status_locked_explained
            }
        )
    }

    private fun updateStatusIcon(state: SystemState) {
        getDrawable(when (state) {
            SystemState.On -> R.drawable.anim_radar
            else -> R.drawable.ic_radar_off
        }).let {
            binding.imageHomeAppStatus.setImageDrawable(it) // v24+ has an animated vector drawable
            (it as? AnimatedVectorDrawable)?.start()
        }
    }

    private fun updateEnableButton(state: SystemState) {
        binding.buttonHomeAppEnable.visibility = when (state) {
            SystemState.Off -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun enableSystem() {
        viewModel.enableSystem()
    }
}