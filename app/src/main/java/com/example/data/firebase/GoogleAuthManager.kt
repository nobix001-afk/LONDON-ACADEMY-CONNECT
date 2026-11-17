package com.example.data.firebase

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoSavedCredential : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

object GoogleAuthManager {

    private const val TAG = "GoogleAuthManager"

    @SuppressLint("DiscouragedApi")
    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return runCatching { context.getString(resId).trim() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun attemptSilentSignIn(activityContext: Context): GoogleSignInResult {
        val webClientId = resolveWebClientId(activityContext)
            ?: return GoogleSignInResult.NoSavedCredential
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredentialForFirebase(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            GoogleSignInResult.NoSavedCredential
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.d(TAG, "Silent Google sign-in skipped: ${e.message}")
            GoogleSignInResult.NoSavedCredential
        } catch (e: Exception) {
            Log.w(TAG, "Silent sign-in error", e)
            GoogleSignInResult.NoSavedCredential
        }
    }

    suspend fun signInInteractive(activityContext: Context): GoogleSignInResult {
        val webClientId = resolveWebClientId(activityContext)
            ?: return GoogleSignInResult.Error(
                "Google Sign-In is not configured for this build. Include app/google-services.json and rebuild."
            )
        val credentialManager = CredentialManager.create(activityContext)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredentialForFirebase(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device", e)
            GoogleSignInResult.Error("No Google account found on this device. Please add a Google account in device Settings and try again.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager sign-in failed", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }

    private suspend fun exchangeGoogleCredentialForFirebase(credential: Credential): GoogleSignInResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            return if (user != null) {
                GoogleSignInResult.Success(user)
            } else {
                GoogleSignInResult.Error("Firebase sign-in returned no user.")
            }
        }
        return GoogleSignInResult.Error("Unsupported credential type returned.")
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            Firebase.auth.signOut()
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state on sign-out", e)
            }
        }
    }
}
