package com.example.foodexpiryapp.presentation.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodexpiryapp.databinding.FragmentProfileAccountBinding
import com.example.foodexpiryapp.presentation.viewmodel.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileAccountFragment : Fragment() {

    private var _binding: FragmentProfileAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupGoogleSignIn()
        setupListeners()
        observeState()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            viewModel.updateGoogleSignInState(
                isSignedIn = true,
                displayName = account.displayName,
                email = account.email,
                photoUrl = account.photoUrl?.toString()
            )
        }
    }

    private fun setupListeners() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGoogleSignOut.setOnClickListener {
            signOutFromGoogle()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update Google Sign-In UI
                    if (state.isGoogleSignIn) {
                        binding.btnGoogleSignIn.visibility = View.GONE
                        binding.btnGoogleSignOut.visibility = View.VISIBLE
                        binding.textGoogleUserName.visibility = View.VISIBLE
                        binding.textGoogleUserEmail.visibility = View.VISIBLE
                        binding.textGoogleUserName.text = state.googleUserName
                        binding.textGoogleUserEmail.text = state.googleUserEmail
                        
                        if (state.googleUserPhotoUrl != null) {
                            binding.imgGooglePhoto.visibility = View.VISIBLE
                            Glide.with(this@ProfileAccountFragment)
                                .load(state.googleUserPhotoUrl)
                                .circleCrop()
                                .into(binding.imgGooglePhoto)
                        }
                    } else {
                        binding.btnGoogleSignIn.visibility = View.VISIBLE
                        binding.btnGoogleSignOut.visibility = View.GONE
                        binding.textGoogleUserName.visibility = View.GONE
                        binding.textGoogleUserEmail.visibility = View.GONE
                        binding.imgGooglePhoto.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    private fun signOutFromGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            viewModel.signOutGoogle()
            Snackbar.make(binding.root, "Signed out successfully", Snackbar.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.updateGoogleSignInState(
                    isSignedIn = true,
                    displayName = account.displayName,
                    email = account.email,
                    photoUrl = account.photoUrl?.toString()
                )
                Snackbar.make(binding.root, "Signed in as ${account.email}", Snackbar.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Snackbar.make(binding.root, "Google Sign-In failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
