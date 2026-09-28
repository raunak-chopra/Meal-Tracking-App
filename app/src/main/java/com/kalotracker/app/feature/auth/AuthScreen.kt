package com.kalotracker.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding
            Text(
                text = "KALO",
                style = KaloTypography.displayLarge,
                color = KaloTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Minimalist AI Nutrition & Movement",
                style = KaloTypography.bodyMedium,
                color = KaloTextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = if (state.isSignUp) 1 else 0,
                containerColor = KaloSurface,
                contentColor = KaloTextPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = !state.isSignUp,
                    onClick = { viewModel.toggleMode(false) },
                    text = { Text("Sign In", style = KaloTypography.titleMedium) }
                )
                Tab(
                    selected = state.isSignUp,
                    onClick = { viewModel.toggleMode(true) },
                    text = { Text("Create Account", style = KaloTypography.titleMedium) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Email input
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email", color = KaloTextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                textStyle = KaloTypography.bodyLarge.copy(color = KaloTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KaloProtein,
                    unfocusedBorderColor = KaloBorder,
                    focusedContainerColor = KaloSurface,
                    unfocusedContainerColor = KaloSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password", color = KaloTextSecondary) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = KaloTypography.bodyLarge.copy(color = KaloTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KaloProtein,
                    unfocusedBorderColor = KaloBorder,
                    focusedContainerColor = KaloSurface,
                    unfocusedContainerColor = KaloSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!state.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = state.errorMessage ?: "",
                    style = KaloTypography.bodyMedium,
                    color = KaloFat
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary action button
            KaloButton(
                text = if (state.isSignUp) "Create Account" else "Sign In",
                onClick = { viewModel.submit(onAuthSuccess) },
                loading = state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Continue as guest
            TextButton(
                onClick = { viewModel.continueAsGuest(onAuthSuccess) }
            ) {
                Text(
                    text = "Continue as Guest (Offline Mode)",
                    style = KaloTypography.bodyLarge,
                    color = KaloTextSecondary
                )
            }
        }
    }
}
