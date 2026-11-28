package com.moneytracker.app.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.remote.SupabaseClient
import com.moneytracker.app.ui.components.EmeraldPrimaryButton
import com.moneytracker.app.ui.components.MoneyTrackerLogoMark
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val ownerEmail = SupabaseClient.OWNER_EMAIL

    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        focusManager.clearFocus()
        viewModel.signIn(ownerEmail, password)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        ObsidianSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            cornerRadius = 24.dp,
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MoneyTrackerLogoMark(size = 52.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Money Tracker",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = ObsidianColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Personal wallet — stays signed in automatically on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (!state.isSupabaseConfigured) {
                Spacer(modifier = Modifier.height(16.dp))
                StatusBanner(
                    icon = Icons.Rounded.WarningAmber,
                    message = "Supabase configuration missing. Add credentials to local.properties or .env and rebuild.",
                    accent = ObsidianColors.Warning
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val lockedEmailColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianColors.ObsidianCanvas,
                unfocusedContainerColor = ObsidianColors.ObsidianCanvas,
                focusedBorderColor = ObsidianColors.HairlineHover,
                unfocusedBorderColor = ObsidianColors.HairlineHover,
                focusedLabelColor = ObsidianColors.TextSecondary,
                unfocusedLabelColor = ObsidianColors.TextSecondary,
                focusedTextColor = ObsidianColors.TextSecondary,
                unfocusedTextColor = ObsidianColors.TextSecondary,
                cursorColor = Color.Transparent
            )

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ObsidianColors.ObsidianCanvas,
                unfocusedContainerColor = ObsidianColors.ObsidianCanvas,
                focusedBorderColor = ObsidianColors.EmeraldPrimary,
                unfocusedBorderColor = ObsidianColors.HairlineHover,
                focusedLabelColor = ObsidianColors.EmeraldPrimary,
                unfocusedLabelColor = ObsidianColors.TextSecondary,
                focusedTextColor = ObsidianColors.TextPrimary,
                unfocusedTextColor = ObsidianColors.TextPrimary,
                cursorColor = ObsidianColors.EmeraldPrimary
            )

            OutlinedTextField(
                value = ownerEmail,
                onValueChange = {},
                readOnly = true,
                label = { Text("Account") },
                singleLine = true,
                enabled = !state.isLoading,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked account",
                        tint = ObsidianColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = lockedEmailColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (state.error != null) viewModel.clearMessages()
                },
                label = { Text("Password") },
                singleLine = true,
                enabled = !state.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = ObsidianColors.TextSecondary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            if (!state.error.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                StatusBanner(
                    icon = Icons.Rounded.ErrorOutline,
                    message = state.error.orEmpty(),
                    accent = ObsidianColors.Danger
                )
            }

            if (!state.infoMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                StatusBanner(
                    icon = Icons.Rounded.CheckCircle,
                    message = state.infoMessage.orEmpty(),
                    accent = ObsidianColors.EmeraldPrimary
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            color = ObsidianColors.JadeMuted,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = ObsidianColors.EmeraldPrimary
                    )
                }
            } else {
                EmeraldPrimaryButton(
                    text = "Sign In",
                    onClick = { submit() },
                    enabled = password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ObsidianColors.TextPrimary
            )
        }
    }
}
