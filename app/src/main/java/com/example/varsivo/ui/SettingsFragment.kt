package com.example.varsivo.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.varsivo.BuildConfig
import com.example.varsivo.R
import com.example.varsivo.data.AzureBlobStorage
import com.example.varsivo.data.ChangePasswordOutcome
import com.example.varsivo.data.ProfilePhotoRepository
import com.example.varsivo.data.UserStore
import com.example.varsivo.databinding.FragmentSettingsBinding
import com.example.varsivo.notifications.PushNotifications
import com.example.varsivo.notifications.RealtimeAlerts
import com.example.varsivo.util.AppLanguage
import com.example.varsivo.util.Validators
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Settings: edit the profile, choose a language, switch notifications on/off, change the password
 * and log out. Values are saved on the phone (SharedPreferences) and language + notifications are
 * also saved in Cloud Firestore (synced automatically when the phone is back online).
 */
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val photos by lazy {
        ProfilePhotoRepository(
            requireContext().applicationContext,
            AzureBlobStorage(BuildConfig.AZURE_PHOTOS_SAS_URL),
            userStore
        )
    }

    /** Android photo picker (no storage permission needed). */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uploadPhoto(uri) else Log.d(TAG, "No photo picked")
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSettingsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Show what is currently saved.
        binding.nameInput.setText(userStore.name)
        binding.schoolInput.setText(userStore.schoolName)
        binding.studentIdInput.setText(userStore.studentId)
        binding.notificationsSwitch.isChecked = userStore.notificationsEnabled
        binding.versionText.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)

        // Language drop-down (the choice is stored; translations are a later milestone).
        binding.languageDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, UserStore.SUPPORTED_LANGUAGES)
        )
        binding.languageDropdown.setText(userStore.language, false) // false = don't filter the list

        binding.nameInput.onTextChanged { binding.nameLayout.error = null }
        binding.currentPasswordInput.onTextChanged { binding.currentPasswordLayout.error = null }
        binding.newPasswordInput.onTextChanged { binding.newPasswordLayout.error = null }
        binding.confirmNewPasswordInput.onTextChanged { binding.confirmNewPasswordLayout.error = null }

        binding.changePhotoButton.setOnClickListener {
            if (!photos.isConfigured) {
                showMessage(getString(R.string.photo_not_configured), isError = true)
            } else {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
        loadPhoto()

        binding.saveButton.setOnClickListener { saveSettings() }
        binding.changePasswordButton.setOnClickListener { changePassword() }
        binding.logoutButton.setOnClickListener { confirmLogout() }
    }

    private fun saveSettings() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val school = binding.schoolInput.text?.toString().orEmpty().trim()
        val language = binding.languageDropdown.text?.toString().orEmpty()

        // Input validation: a blank or one-letter name is refused with a message, not saved.
        if (!Validators.isValidName(name)) {
            binding.nameLayout.error = getString(R.string.error_name_invalid)
            showMessage(getString(R.string.error_name_invalid), isError = true)
            return
        }

        binding.root.hideKeyboard()
        val chosenLanguage = language.takeIf { it in UserStore.SUPPORTED_LANGUAGES } ?: UserStore.DEFAULT_LANGUAGE
        val notifications = binding.notificationsSwitch.isChecked

        binding.saveButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            // Saved on the phone first, then in Cloud Firestore (users/{uid}).
            val syncedOnline = authRepository.updateSettings(name, school, chosenLanguage, notifications)
            if (!hasView) return@launch
            binding.saveButton.isEnabled = true
            Log.i(TAG, "Settings saved, syncedOnline=$syncedOnline")
            showMessage(
                getString(if (syncedOnline) R.string.settings_saved_online else R.string.settings_saved_offline),
                isError = false
            )
            PushNotifications.applyPreference(notifications) // join/leave the alerts topic
            AppLanguage.apply(chosenLanguage) // redraws every screen if the language changed
        }
    }

    private fun loadPhoto() {
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = photos.load()
            if (hasView && bitmap != null) showPhoto(bitmap)
        }
    }

    private fun showPhoto(bitmap: Bitmap) {
        binding.profilePhoto.imageTintList = null
        binding.profilePhoto.setPadding(0, 0, 0, 0)
        binding.profilePhoto.clipToOutline = true
        binding.profilePhoto.setImageBitmap(bitmap)
    }

    /** Uploads the picked photo to Azure Blob Storage. */
    private fun uploadPhoto(uri: Uri) {
        binding.photoProgress.show()
        binding.changePhotoButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = try {
                photos.upload(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Photo upload failed: ${e.message}")
                false
            }
            if (!hasView) return@launch
            binding.photoProgress.hide()
            binding.changePhotoButton.isEnabled = true
            showMessage(getString(if (ok) R.string.photo_saved else R.string.photo_failed), isError = !ok)
            loadPhoto()
        }
    }

    private fun changePassword() {
        val current = binding.currentPasswordInput.text?.toString().orEmpty()
        val new = binding.newPasswordInput.text?.toString().orEmpty()
        val confirm = binding.confirmNewPasswordInput.text?.toString().orEmpty()

        var valid = true
        if (current.isEmpty()) {
            binding.currentPasswordLayout.error = getString(R.string.error_current_password_required)
            valid = false
        }
        if (!Validators.isStrongPassword(new)) {
            binding.newPasswordLayout.error = getString(R.string.error_password_weak)
            valid = false
        }
        if (new != confirm) {
            binding.confirmNewPasswordLayout.error = getString(R.string.error_passwords_dont_match)
            valid = false
        }
        if (!valid) return

        binding.root.hideKeyboard()
        binding.changePasswordButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = authRepository.changePassword(current, new)
            if (!hasView) return@launch
            binding.changePasswordButton.isEnabled = true
            when (outcome) {
                ChangePasswordOutcome.SUCCESS -> {
                    binding.currentPasswordInput.setText("")
                    binding.newPasswordInput.setText("")
                    binding.confirmNewPasswordInput.setText("")
                    showMessage(getString(R.string.password_changed), isError = false)
                }
                ChangePasswordOutcome.WRONG_CURRENT_PASSWORD ->
                    binding.currentPasswordLayout.error = getString(R.string.error_current_password_wrong)
                ChangePasswordOutcome.NO_ACCOUNT ->
                    showMessage(getString(R.string.error_no_account), isError = true)
                ChangePasswordOutcome.NO_CONNECTION ->
                    showMessage(getString(R.string.error_no_connection), isError = true)
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.log_out_title)
            .setMessage(R.string.log_out_message)
            .setPositiveButton(R.string.log_out) { _, _ ->
                authRepository.logout()
                RealtimeAlerts.stop() // no alerts for an account that has logged out
                findNavController().navigate(R.id.action_settings_to_login)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMessage(message: String, isError: Boolean) {
        binding.savedMessage.text = message
        binding.savedMessage.setTextColor(
            ContextCompat.getColor(requireContext(), if (isError) R.color.varsivo_error else R.color.varsivo_success)
        )
        binding.savedMessage.show()
    }

    private companion object {
        const val TAG = "SettingsFragment"
    }
}
