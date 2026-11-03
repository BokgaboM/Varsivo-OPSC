package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.varsivo.R
import com.example.varsivo.api.Bursary
import com.example.varsivo.data.BursaryRepository
import com.example.varsivo.databinding.FragmentBursariesBinding
import com.example.varsivo.ui.adapter.BursaryAdapter
import com.example.varsivo.util.BursaryFilter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Lists bursaries from Cloud Firestore, with search (name or provider) and filters for study field
 * and province. Three are shown at first; "View more", a search or a filter shows every match.
 */
class BursariesFragment : BaseFragment<FragmentBursariesBinding>() {

    private val adapter = BursaryAdapter(onBursaryClick = ::openDetails)

    private val repository = BursaryRepository()
    private var allBursaries: List<Bursary> = emptyList()
    private var field = BursaryFilter.ALL_FIELDS
    private var province = BursaryFilter.ALL_PROVINCES
    private var isLoading = true
    private var errorMessage: String? = null
    private var showAll = false
    private var query = ""

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentBursariesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.bursaryList.layoutManager = LinearLayoutManager(requireContext())
        binding.bursaryList.adapter = adapter

        binding.searchBar.setup(hint = getString(R.string.search_bursaries_hint)) { text ->
            query = text
            render()
        }
        arguments?.getString(ARG_QUERY)?.takeIf { it.isNotBlank() }?.let {
            binding.searchBar.searchInput.setText(it) // triggers the watcher, which renders
        }

        setUpDropdown(binding.fieldDropdown, BursaryFilter.fields, field) { field = it }
        setUpDropdown(binding.provinceDropdown, BursaryFilter.provinces, province) { province = it }

        binding.viewMoreButton.setOnClickListener {
            showAll = !showAll
            render()
        }
        binding.retryButton.setOnClickListener { loadBursaries() }

        loadBursaries()
        render()
    }

    private fun setUpDropdown(
        dropdown: MaterialAutoCompleteTextView,
        options: List<String>,
        current: String,
        onChosen: (String) -> Unit
    ) {
        dropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options))
        dropdown.setText(current, false) // false = don't filter the list
        dropdown.setOnItemClickListener { _, _, position, _ ->
            onChosen(options[position])
            Log.d(TAG, "Filter changed: field=$field, province=$province")
            render()
        }
    }

    /** Reads the "bursaries" collection from Cloud Firestore. */
    private fun loadBursaries() {
        isLoading = true
        errorMessage = null
        render()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = repository.loadBursaries()
                Log.i(TAG, "Loaded ${result.size} bursaries")
                allBursaries = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not load bursaries", e)
                errorMessage = getString(R.string.error_load_bursaries)
            }
            isLoading = false
            if (hasView) render()
        }
    }

    private fun render() {
        if (!hasView) return
        val filtering = query.isNotBlank() || field != BursaryFilter.ALL_FIELDS || province != BursaryFilter.ALL_PROVINCES
        val visible = BursaryFilter.visibleBursaries(allBursaries, showAll, query, field, province)
        adapter.submit(visible)

        val showProgress = isLoading
        val error = errorMessage
        val showError = error != null
        val showEmpty = !showProgress && !showError && visible.isEmpty()

        binding.progress.showIf(showProgress)
        binding.stateContainer.showIf(showError || showEmpty)
        binding.retryButton.showIf(showError)
        binding.stateText.text = when {
            showError -> error
            showEmpty -> getString(R.string.no_bursaries_match)
            else -> ""
        }
        binding.bursaryList.showIf(!showProgress && !showError && visible.isNotEmpty())

        binding.viewMoreButton.showIf(
            !filtering && !isLoading && errorMessage == null && allBursaries.size > BursaryFilter.FIRST_PAGE
        )
        binding.viewMoreButton.setText(if (showAll) R.string.view_less else R.string.view_more_bursaries)
    }

    private fun openDetails(bursary: Bursary) {
        Log.d(TAG, "Opening details for ${bursary.name}")
        val args = Bundle().apply {
            putString(BursaryDetailsFragment.ARG_NAME, bursary.name)
            putString(BursaryDetailsFragment.ARG_DESCRIPTION, bursary.description)
            putString(BursaryDetailsFragment.ARG_PROVIDER, bursary.provider)
            putString(BursaryDetailsFragment.ARG_ELIGIBILITY, bursary.eligibility)
            putString(BursaryDetailsFragment.ARG_CLOSING_DATE, bursary.closingDate)
        }
        findNavController().navigate(R.id.bursaryDetailsFragment, args)
    }

    companion object {
        private const val TAG = "BursariesFragment"
        const val ARG_QUERY = "query"
    }
}
