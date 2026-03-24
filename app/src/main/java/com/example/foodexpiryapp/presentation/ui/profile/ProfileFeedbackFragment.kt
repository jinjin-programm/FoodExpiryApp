package com.example.foodexpiryapp.presentation.ui.profile

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.foodexpiryapp.BuildConfig
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentProfileFeedbackBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFeedbackFragment : Fragment() {

    private var _binding: FragmentProfileFeedbackBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val FEEDBACK_EMAIL = "123qwerty100@gmail.com"
        private const val EMAIL_SUBJECT = "FoodExpiryApp Feedback"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSendFeedbackButton()
    }

    private fun setupSendFeedbackButton() {
        binding.sendFeedbackButton.setOnClickListener {
            val feedbackMessage = binding.feedbackEditText.text.toString().trim()
            
            if (feedbackMessage.isEmpty()) {
                binding.feedbackInputLayout.error = getString(R.string.feedback_error_empty)
                return@setOnClickListener
            }
            
            binding.feedbackInputLayout.error = null
            sendFeedbackEmail(feedbackMessage)
        }
        
        binding.feedbackEditText.doAfterTextChanged {
            binding.feedbackInputLayout.error = null
        }
    }

    private fun sendFeedbackEmail(message: String) {
        val emailBody = buildEmailBody(message)
        
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }
        
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.feedback_chooser_title)))
        } catch (e: ActivityNotFoundException) {
            showNoEmailAppError()
        }
    }

    private fun buildEmailBody(userMessage: String): String {
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        
        return buildString {
            appendLine(userMessage)
            appendLine()
            appendLine("---")
            appendLine("App Version: $appVersion")
            appendLine("Device: $deviceInfo")
            appendLine("OS: $androidVersion")
        }
    }

    private fun showNoEmailAppError() {
        Snackbar.make(
            binding.root,
            getString(R.string.feedback_no_email_app),
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
