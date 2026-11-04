package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.varsivo.R
import com.example.varsivo.databinding.FragmentExploreBinding
import com.example.varsivo.databinding.ItemCareerBinding
import com.example.varsivo.databinding.ItemInstitutionBinding
import com.example.varsivo.databinding.ItemSubjectMarkBinding
import com.example.varsivo.util.ApsCalculator
import com.example.varsivo.util.CareerAdvisor
import com.example.varsivo.util.ExploreData
import com.example.varsivo.util.Validators
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText

/**
 * Explore: an APS (Admission Point Score) calculator that tells the student which institutions
 * they currently qualify for and which careers they can aim for with that score.
 * Before a score is calculated, a short list of popular careers is shown instead.
 * The maths lives in [ApsCalculator] and [CareerAdvisor] so it can be unit tested.
 */
class ExploreFragment : BaseFragment<FragmentExploreBinding>() {

    private val subjectInputs = mutableListOf<TextInputEditText>()
    private var apsScore: Int? = null
    private var subjectsUsedForScore = ApsCalculator.MAX_SUBJECTS
    private var showAllInstitutions = false
    private var showAllCareers = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentExploreBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        repeat(INITIAL_SUBJECTS) { addSubjectField() }

        binding.toggleCalculatorButton.setOnClickListener {
            val show = binding.calculatorCard.visibility != View.VISIBLE
            binding.calculatorCard.showIf(show)
            binding.toggleCalculatorButton.setText(if (show) R.string.hide_calculator else R.string.calculate_aps)
        }
        binding.addSubjectButton.setOnClickListener { addSubjectField() }
        binding.calculateButton.setOnClickListener { calculate() }

        binding.viewMoreInstitutionsButton.setOnClickListener {
            showAllInstitutions = !showAllInstitutions
            renderInstitutions()
        }
        binding.viewMoreCareersButton.setOnClickListener {
            showAllCareers = !showAllCareers
            renderCareers()
        }

        renderInstitutions()
        renderCareers()
    }

    private fun addSubjectField() {
        if (subjectInputs.size >= ApsCalculator.MAX_SUBJECTS) return
        val row = ItemSubjectMarkBinding.inflate(layoutInflater, binding.subjectContainer, false)
        // Each field is labelled with the subject it stands for (Home Language, FAL, Maths, electives).
        val subjectNames = resources.getStringArray(R.array.aps_subjects)
        row.subjectLayout.hint = subjectNames.getOrElse(subjectInputs.size) {
            getString(R.string.subject_mark_hint, subjectInputs.size + 1)
        }
        row.subjectInput.onTextChanged { row.subjectLayout.error = null }
        subjectInputs.add(row.subjectInput)
        binding.subjectContainer.addView(row.root)
        binding.addSubjectButton.showIf(subjectInputs.size < ApsCalculator.MAX_SUBJECTS)
        binding.subjectCountText.text = getString(R.string.aps_rule_note)
    }

    private fun calculate() {
        binding.calculatorMessage.hide()
        val marks = mutableListOf<Int>()
        var valid = true

        subjectInputs.forEachIndexed { index, input ->
            val layout = binding.subjectContainer.getChildAt(index) as com.google.android.material.textfield.TextInputLayout
            val mark = input.text?.toString()?.toIntOrNull()
            when {
                mark == null -> {
                    layout.error = getString(R.string.error_enter_mark)
                    valid = false
                }
                !Validators.isValidMark(mark) -> {
                    layout.error = getString(R.string.error_mark_range)
                    valid = false
                }
                else -> marks.add(mark)
            }
        }
        if (!valid) {
            binding.calculatorMessage.text = getString(R.string.error_fix_marks)
            binding.calculatorMessage.show()
            return
        }

        val score = ApsCalculator.calculate(marks) ?: return
        Log.i(TAG, "APS calculated: $score from ${marks.size} subject(s)")
        apsScore = score
        subjectsUsedForScore = marks.size

        binding.apsScoreText.text = getString(R.string.your_score, score)
        binding.apsNoteText.hide() // always six subjects now, so the score is never an estimate
        binding.calculatorCard.hide()
        binding.toggleCalculatorButton.setText(R.string.calculate_aps)
        renderInstitutions()
        showAllCareers = false
        renderCareers()
    }

    private fun renderInstitutions() {
        val pool = ExploreData.institutions
        val shown = if (showAllInstitutions) pool else pool.take(INITIAL_INSTITUTIONS)
        binding.institutionContainer.removeAllViews()
        shown.forEach { institution ->
            val row = ItemInstitutionBinding.inflate(layoutInflater, binding.institutionContainer, false)
            row.institutionNameText.text = institution.name
            row.minApsText.text = getString(R.string.min_aps, institution.minAps)
            val score = apsScore
            if (score != null) {
                val eligible = ApsCalculator.isEligible(score, institution.minAps)
                row.matchChip.setText(if (eligible) R.string.match else R.string.below_min)
                row.matchChip.setBackgroundResource(if (eligible) R.drawable.bg_chip_match else R.drawable.bg_chip_below)
                row.matchChip.show()
            }
            binding.institutionContainer.addView(row.root)
        }
        binding.viewMoreInstitutionsButton.showIf(pool.size > INITIAL_INSTITUTIONS)
        binding.viewMoreInstitutionsButton.setText(
            if (showAllInstitutions) R.string.view_less else R.string.view_more_institutions
        )
    }

    private fun renderCareers() {
        val score = apsScore
        if (score != null) {
            renderRecommendedCareers(score)
            return
        }
        binding.careerTitle.setText(R.string.popular_careers)
        binding.careerHint.setText(R.string.careers_hint_before)
        val pool = ExploreData.careers
        val shown = if (showAllCareers) pool else pool.take(INITIAL_CAREERS)
        binding.careerContainer.removeAllViews()
        shown.forEach { career ->
            val row = ItemCareerBinding.inflate(layoutInflater, binding.careerContainer, false)
            row.careerText.text = career
            binding.careerContainer.addView(row.root)
        }
        binding.viewMoreCareersButton.showIf(pool.size > INITIAL_CAREERS)
        binding.viewMoreCareersButton.setText(if (showAllCareers) R.string.view_less else R.string.view_more_careers)
    }

    /**
     * After the APS is calculated: careers the learner qualifies for (hardest first), then the
     * ones that are only a few points away, so the learner knows what to aim for.
     */
    private fun renderRecommendedCareers(score: Int) {
        val names = resources.getStringArray(R.array.career_names)
        val result = CareerAdvisor.recommend(score)
        Log.d(TAG, "Careers for APS $score: ${result.qualifies.size} match, ${result.almost.size} almost")

        binding.careerTitle.text = getString(R.string.careers_for_aps, score)
        binding.careerHint.setText(R.string.careers_hint_after)

        val rows = result.qualifies.map {
            getString(R.string.career_row_qualifies, names[it.index], it.minAps)
        } + result.almost.map {
            getString(R.string.career_row_almost, names[it.index], it.minAps, CareerAdvisor.pointsShort(score, it))
        }

        binding.careerContainer.removeAllViews()
        if (rows.isEmpty()) {
            val row = ItemCareerBinding.inflate(layoutInflater, binding.careerContainer, false)
            row.careerText.setText(R.string.careers_none)
            binding.careerContainer.addView(row.root)
        }
        val shown = if (showAllCareers) rows else rows.take(INITIAL_RECOMMENDED)
        shown.forEach { text ->
            val row = ItemCareerBinding.inflate(layoutInflater, binding.careerContainer, false)
            row.careerText.text = text
            binding.careerContainer.addView(row.root)
        }
        binding.viewMoreCareersButton.showIf(rows.size > INITIAL_RECOMMENDED)
        binding.viewMoreCareersButton.setText(if (showAllCareers) R.string.view_less else R.string.view_more_careers)
    }

    private companion object {
        const val TAG = "ExploreFragment"
        const val INITIAL_RECOMMENDED = 4
        const val INITIAL_SUBJECTS = ApsCalculator.MAX_SUBJECTS // all six subjects are needed
        const val INITIAL_INSTITUTIONS = 2
        const val INITIAL_CAREERS = 3
    }
}
