package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.UniAssistRepository
import com.example.varsivo.databinding.FragmentFirstYearSupportBinding
import com.example.varsivo.databinding.ItemDeadlineBinding
import com.example.varsivo.databinding.ItemTimetableEntryBinding
import com.example.varsivo.util.Validators
import kotlinx.coroutines.launch

/**
 * UniAssist (premium tier): today's timetable and upcoming deadlines for first-year students.
 * Locked until the student verifies a university or college email (.ac.za, .edu.za or an allowed college domain).
 * The timetable and deadlines are sample content for the prototype.
 */
class FirstYearSupportFragment : BaseFragment<FragmentFirstYearSupportBinding>() {

    private val uniAssist by lazy { UniAssistRepository(userStore) }

    private data class TimetableEntry(val time: String, val subject: String)
    private data class Deadline(val title: String, val daysLeft: Int)

    private val timetable = listOf(
        TimetableEntry("09:00", "Data Structures"),
        TimetableEntry("14:00", "Software Eng")
    )

    private val deadlines = listOf(
        Deadline("Assignment 2", 2),
        Deadline("Maths test", 5),
        Deadline("Assignment 3", 10)
    )

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFirstYearSupportBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Locked until a university or college email is verified (see Validators.isStudentEmail).
        showLocked(!userStore.uniAssistVerified)
        binding.verifyButton.setOnClickListener { verify() }
        binding.studentEmailInput.onTextChanged { binding.studentEmailLayout.error = null }
        viewLifecycleOwner.lifecycleScope.launch {
            val verified = uniAssist.isVerified() // also checks Firestore (e.g. on a new phone)
            if (hasView) showLocked(!verified)
        }

        timetable.forEach { entry ->
            val row = ItemTimetableEntryBinding.inflate(layoutInflater, binding.timetableContainer, false)
            row.root.text = getString(R.string.timetable_entry, entry.time, entry.subject)
            binding.timetableContainer.addView(row.root)
        }

        deadlines.forEachIndexed { index, deadline ->
            val row = ItemDeadlineBinding.inflate(layoutInflater, binding.deadlineContainer, false)
            row.deadlineTitle.text = deadline.title
            row.deadlineDays.text = getString(R.string.days_left, deadline.daysLeft)
            row.deadlineDivider.showIf(index != deadlines.lastIndex)
            binding.deadlineContainer.addView(row.root)
        }
    }

    private fun showLocked(locked: Boolean) {
        binding.lockedContainer.showIf(locked)
        binding.contentScroll.showIf(!locked)
        binding.verifiedBadge.showIf(!locked)
    }

    private fun verify() {
        val email = binding.studentEmailInput.text?.toString().orEmpty().trim()
        if (!Validators.isStudentEmail(email)) {
            binding.studentEmailLayout.error = getString(R.string.error_student_email)
            return
        }
        binding.root.hideKeyboard()
        binding.verifyButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val synced = uniAssist.saveVerified(email)
            Log.i(TAG, "UniAssist verified with $email, synced=$synced")
            if (!hasView) return@launch
            binding.verifyButton.isEnabled = true
            Toast.makeText(requireContext(), R.string.uniassist_unlocked, Toast.LENGTH_SHORT).show()
            showLocked(false)
        }
    }

    private companion object {
        const val TAG = "UniAssist"
    }
}
