package com.kalotracker.app.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.kalotracker.app.core.network.SupabaseModule
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class AuthUserState(
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val userId: String? = null,
    val isGuestMode: Boolean = false
)

class AuthRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kalo_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(checkInitialAuthState())
    val authState: StateFlow<AuthUserState> = _authState.asStateFlow()

    private fun checkInitialAuthState(): AuthUserState {
        val isGuest = prefs.getBoolean("is_guest_mode", false)
        if (isGuest) {
            return AuthUserState(isAuthenticated = false, isGuestMode = true)
        }

        if (SupabaseModule.isConfigured) {
            val user = SupabaseModule.client.auth.currentUserOrNull()
            if (user != null) {
                return AuthUserState(
                    isAuthenticated = true,
                    email = user.email,
                    userId = user.id,
                    isGuestMode = false
                )
            }
        }

        // Default to guest mode enabled for first launch so user can immediately track
        return AuthUserState(isAuthenticated = false, isGuestMode = true)
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseModule.isConfigured) {
            // Emulate successful sign in for demo
            setGuestMode(false)
            _authState.value = AuthUserState(
                isAuthenticated = true,
                email = email,
                userId = "demo-user-id",
                isGuestMode = false
            )
            return@withContext Result.success(Unit)
        }

        try {
            SupabaseModule.client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            val user = SupabaseModule.client.auth.currentUserOrNull()
            setGuestMode(false)
            _authState.value = AuthUserState(
                isAuthenticated = true,
                email = user?.email ?: email,
                userId = user?.id,
                isGuestMode = false
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseModule.isConfigured) {
            setGuestMode(false)
            _authState.value = AuthUserState(
                isAuthenticated = true,
                email = email,
                userId = "demo-user-id",
                isGuestMode = false
            )
            return@withContext Result.success(Unit)
        }

        try {
            SupabaseModule.client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }
            val user = SupabaseModule.client.auth.currentUserOrNull()
            setGuestMode(false)
            _authState.value = AuthUserState(
                isAuthenticated = true,
                email = user?.email ?: email,
                userId = user?.id,
                isGuestMode = false
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean("is_guest_mode", isGuest).apply()
        _authState.value = _authState.value.copy(isGuestMode = isGuest)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            if (SupabaseModule.isConfigured) {
                SupabaseModule.client.auth.signOut()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setGuestMode(true)
        _authState.value = AuthUserState(isAuthenticated = false, isGuestMode = true)
    }
}
