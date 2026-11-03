package com.example.varsivo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.R
import com.example.varsivo.data.LoginOutcome
import com.example.varsivo.data.UserStore
import com.example.varsivo.databinding.FragmentLoginBinding
import com.example.varsivo.util.AppLanguage
import com.example.varsivo.util.Validators
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Sign in with email + password against the Varsivo API (Azure). Onboarding choices (role and
 * language) are picked here by tapping the two pills at the bottom of the screen.
 */
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLoginBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Onboarding: role and language (tap a pill to change it).
        binding.gradeValue.text = userStore.role
        binding.languageValue.text = userStore.language
        binding.gradeValue.setOnClickListener { pickRole() }
        binding.languageValue.setOnClickListener { pickLanguage() }

        // Clear the error as soon as the student starts typing again.
        binding.studentIdInput.onTextChanged { clearError() }
        binding.passwordInput.onTextChanged { clearError() }

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.passwordInput.onImeAction(EditorInfo.IME_ACTION_DONE) { attemptLogin() }
        binding.signUpText.setOnClickListener { findNavController().navigate(R.id.registerFragment) }
        binding.forgotPasswordText.setOnClickListener { showForgotPasswordDialog() }
        binding.googleButton.setOnClickListener { showGoogleDialog() }
    }

    private fun attemptLogin() {
        val email = binding.studentIdInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()

        // Input validation: never crash on empty input, tell the student what is missing.
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_enter_id_and_password))
            return
        }
        if (!Validators.isValidEmail(email)) {
            showError(getString(R.string.error_email_invalid))
            return
        }

        binding.root.hideKeyboard()
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = authRepository.login(email, password)
            if (!hasView) return@launch
            setLoading(false)
            when (outcome) {
                LoginOutcome.SUCCESS -> {
                    Log.i(TAG, "Login OK, opening Home")
                    findNavController().navigate(R.id.action_login_to_home)
                    AppLanguage.apply(userStore.language) // the profile may hold another language
                }
                LoginOutcome.INVALID_CREDENTIALS -> showError(getString(R.string.error_incorrect_login))
                LoginOutcome.NO_CONNECTION -> showError(getString(R.string.error_no_connection))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loginProgress.showIf(loading)
        binding.loginButton.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.show()
    }

    private fun clearError() {
        if (hasView) binding.errorText.hide()
    }

    /** Sends a real password-reset email through Firebase Authentication. */
    private fun showForgotPasswordDialog() {
        val input = EditText(requireContext()).apply {
            setText(binding.studentIdInput.text)
            hint = getString(R.string.student_id)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        val padding = resources.getDimensionPixelSize(R.dimen.dialog_field_padding)
        val container = FrameLayout(requireContext()).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_password_title)
            .setMessage(R.string.reset_password_message)
            .setView(container)
            .setPositiveButton(R.string.send_reset_link) { _, _ ->
                val email = input.text?.toString().orEmpty().trim()
                if (!Validators.isValidEmail(email)) {
                    showError(getString(R.string.error_email_invalid))
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    authRepository.sendPasswordReset(email)
                    // Same message either way, so the app never reveals which emails are registered.
                    if (hasView) Snackbar.make(binding.root, R.string.reset_requested, Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Single sign-on with Google through Android Credential Manager. The Google ID token is then
     * exchanged for a Firebase account ([com.example.varsivo.data.AuthRepository.loginWithGoogle]).
     *
     * Reference: Android Developers (2025) *Authenticate users with Sign in with Google*
     * https://developer.android.com/identity/sign-in/credential-manager-siwg
     */
    private fun showGoogleDialog() {
        val webClientId = getString(R.string.default_web_client_id)
        if (!webClientId.endsWith(".apps.googleusercontent.com")) {
            // Firebase project not configured for Google yet: never let the button skip the login.
            Log.w(TAG, "Google sign-in not configured (default_web_client_id missing)")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.continue_with_google)
                .setMessage(R.string.google_coming_soon)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = try {
                val result = CredentialManager.create(requireContext()).getCredential(requireActivity(), request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    authRepository.loginWithGoogle(google.idToken, google.displayName)
                } else {
                    Log.w(TAG, "Unexpected credential type: ${credential.type}")
                    LoginOutcome.INVALID_CREDENTIALS
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i(TAG, "Google sign-in cancelled by the user")
                null
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google sign-in failed: ${e.type} ${e.message}")
                LoginOutcome.NO_CONNECTION
            } catch (e: GoogleIdTokenParsingException) {
                Log.w(TAG, "Could not read the Google ID token", e)
                LoginOutcome.INVALID_CREDENTIALS
            }
            if (!hasView) return@launch
            setLoading(false)
            when (outcome) {
                null -> Unit // cancelled
                LoginOutcome.SUCCESS -> {
                    Log.i(TAG, "Google sign-in OK, opening Home")
                    findNavController().navigate(R.id.action_login_to_home)
                    AppLanguage.apply(userStore.language)
                }
                LoginOutcome.INVALID_CREDENTIALS -> showError(getString(R.string.error_google_sign_in))
                LoginOutcome.NO_CONNECTION -> showError(getString(R.string.error_google_sign_in))
            }
        }
    }

    private fun pickRole() {
        val roles = UserStore.SUPPORTED_ROLES.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_role)
            .setSingleChoiceItems(roles, roles.indexOf(userStore.role)) { dialog, which ->
                userStore.role = roles[which]
                binding.gradeValue.text = roles[which]
                Log.i(TAG, "Role chosen: ${roles[which]}")
                dialog.dismiss()
            }
            .show()
    }

    private fun pickLanguage() {
        val languages = UserStore.SUPPORTED_LANGUAGES.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_language)
            .setSingleChoiceItems(languages, languages.indexOf(userStore.language)) { dialog, which ->
                userStore.language = languages[which]
                binding.languageValue.text = languages[which]
                Log.i(TAG, "Language chosen: ${languages[which]}")
                dialog.dismiss()
                AppLanguage.apply(languages[which]) // redraws the screen in the new language
            }
            .show()
    }

    private companion object {
        const val TAG = "LoginFragment"
    }
}
