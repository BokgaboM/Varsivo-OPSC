package com.example.varsivo

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.varsivo.databinding.ActivityMainBinding

/**
 * The app's only Activity. Every screen is a Fragment shown inside the NavHostFragment
 * (see res/navigation/nav_graph.xml). The navy bottom navigation bar lives here once and is
 * shown or hidden depending on which screen is open.
 *
 * Reference: Android Developers (2024) *Navigation component* <https://developer.android.com/guide/navigation>
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    /** Screens that are shown full-screen, without the bottom navigation bar. */
    private val screensWithoutBottomNav = setOf(
        R.id.splashFragment,
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.universityMapFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind the system bars (required on Android 15+) and pad the root layout by their size.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        setUpBottomNav()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showNav = destination.id !in screensWithoutBottomNav
            binding.bottomNav.visibility = if (showNav) View.VISIBLE else View.GONE
            highlightTab(destination.id)
            Log.d(TAG, "Destination changed: ${destination.label ?: destination.id}, bottomNav=$showNav")
        }
    }

    /** Bottom bar: Home | Explore | Bursaries | Tracker | Profile. */
    private fun setUpBottomNav() {
        binding.tabHome.setOnClickListener { openTab(R.id.homeFragment) }
        binding.tabExplore.setOnClickListener { openTab(R.id.exploreFragment) }
        binding.tabBursaries.setOnClickListener { openTab(R.id.bursariesFragment) }
        binding.tabTracker.setOnClickListener { openTab(R.id.applicationsTrackerFragment) }
        binding.tabProfile.setOnClickListener { openTab(R.id.settingsFragment) }
    }

    /**
     * Opens a bottom-bar destination. Home stays at the bottom of the back stack, so Back from any
     * tab returns to Home, and tapping the tab you are already on does nothing.
     */
    private fun openTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        if (destinationId == R.id.homeFragment) {
            // Go back to the existing Home instead of stacking a second copy.
            if (!navController.popBackStack(R.id.homeFragment, inclusive = false)) navController.navigate(destinationId)
            return
        }
        navController.navigate(
            destinationId,
            null,
            NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).setLaunchSingleTop(true).build()
        )
    }

    /** Gold icon and label for the tab that matches the current screen, white for the others. */
    private fun highlightTab(destinationId: Int) {
        val gold = getColor(R.color.varsivo_gold)
        val white = getColor(R.color.white)
        fun tint(icon: ImageView, label: TextView, selected: Boolean) {
            val color = if (selected) gold else white
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color))
            label.setTextColor(color)
        }

        tint(binding.navHome, binding.labelHome, destinationId == R.id.homeFragment)
        tint(binding.navExplore, binding.labelExplore, destinationId == R.id.exploreFragment)
        tint(
            binding.navBursaries, binding.labelBursaries,
            destinationId == R.id.bursariesFragment || destinationId == R.id.bursaryDetailsFragment
        )
        tint(
            binding.navTracker, binding.labelTracker,
            destinationId == R.id.applicationsTrackerFragment || destinationId == R.id.applicationFormFragment ||
                destinationId == R.id.universitiesFragment
        )
        tint(binding.navProfile, binding.labelProfile, destinationId == R.id.settingsFragment)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
