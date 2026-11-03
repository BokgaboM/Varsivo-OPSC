package com.example.varsivo.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentHomeBinding
import com.example.varsivo.notifications.PushNotifications
import com.example.varsivo.util.PartOfDay
import java.util.Calendar

/** Dashboard: greeting, search, "most urgent" banner and the four main sections. */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    /** Android 13+ asks the student before the app may show push notifications. */
    private val askNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "Notification permission granted=$granted")
        }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (userStore.notificationsEnabled && !PushNotifications.hasPermission(requireContext()) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            askNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val firstName = userStore.name.trim().substringBefore(" ").ifBlank { getString(R.string.student) }
        binding.greetingText.text = getString(R.string.greeting_hi, firstName)

        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        binding.timeOfDayText.setText(
            when (PartOfDay.fromHour(hour)) {
                PartOfDay.MORNING -> R.string.good_morning
                PartOfDay.AFTERNOON -> R.string.good_afternoon
                PartOfDay.EVENING -> R.string.good_evening
            }
        )

        // Pressing the keyboard's Search key opens the bursaries list with the typed query.
        binding.searchBar.setup(
            hint = getString(R.string.search_bursaries_hint),
            onSearchKey = {
                val query = binding.searchBar.searchInput.text?.toString().orEmpty().trim()
                val args = Bundle().apply { putString(BursariesFragment.ARG_QUERY, query) }
                findNavController().navigate(R.id.bursariesFragment, args)
            },
        )

        binding.cardApplications.setOnClickListener {
            findNavController().navigate(R.id.applicationsTrackerFragment)
        }
        binding.cardBursaries.setOnClickListener { findNavController().navigate(R.id.bursariesFragment) }
        binding.cardExplore.setOnClickListener { findNavController().navigate(R.id.exploreFragment) }
        binding.cardUniAssist.setOnClickListener { findNavController().navigate(R.id.firstYearSupportFragment) }
    }

    private companion object {
        const val TAG = "HomeFragment"
    }
}
