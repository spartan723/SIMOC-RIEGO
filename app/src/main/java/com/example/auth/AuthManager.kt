package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthManager"

class AuthManager(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            val authResult = auth.signInWithCredential(authCredential).await()
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Google Sign-In successful for uid: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Usuario no disponible tras autenticación"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Google Sign-In", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
