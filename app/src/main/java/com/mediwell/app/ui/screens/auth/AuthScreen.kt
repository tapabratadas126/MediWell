package com.mediwell.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediwell.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState  by viewModel.uiState.collectAsState()
    val email    by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isSignUp by viewModel.isSignUp.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager   = LocalFocusManager.current
    val scrollState    = rememberScrollState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) onAuthSuccess()
    }

    Scaffold(containerColor = Ivory) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo + wordmark ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ForestGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("M+", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "MEDCHCK",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = ForestGreen
                )
            )
            Text(
                text = "Medication Safety & Intelligence",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // ── Tab row ──────────────────────────────────────────────────────
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = SageGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(4.dp)) {
                    listOf(Pair(false, "Sign In"), Pair(true, "Create Account")).forEach { (signUp, label) ->
                        val selected = isSignUp == signUp
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp)),
                            color    = if (selected) White else androidx.compose.ui.graphics.Color.Transparent,
                            onClick  = { viewModel.setAuthMode(signUp) }
                        ) {
                            Text(
                                text        = label,
                                modifier    = Modifier.padding(vertical = 10.dp),
                                textAlign   = TextAlign.Center,
                                fontWeight  = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color       = if (selected) ForestGreen else TextTertiary,
                                fontSize    = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Input card ───────────────────────────────────────────────────
            Surface(
                shape          = RoundedCornerShape(16.dp),
                color          = White,
                tonalElevation = 0.dp,
                modifier       = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    // Error banner
                    if (uiState is AuthUiState.Error) {
                        Surface(shape = RoundedCornerShape(8.dp), color = CriticalRedBg) {
                            Text(
                                text     = (uiState as AuthUiState.Error).message,
                                modifier = Modifier.padding(12.dp),
                                color    = CriticalRed,
                                style    = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Email field
                    OutlinedTextField(
                        value         = email,
                        onValueChange = viewModel::updateEmail,
                        label         = { Text("Email Address") },
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = TextTertiary) },
                        modifier      = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        singleLine = true,
                        shape      = RoundedCornerShape(10.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            focusedLabelColor  = ForestGreen
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password field
                    OutlinedTextField(
                        value         = password,
                        onValueChange = viewModel::updatePassword,
                        label         = { Text("Password") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null, tint = TextTertiary) },
                        trailingIcon  = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                                  else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide" else "Show",
                                    tint = TextTertiary
                                )
                            }
                        },
                        modifier            = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions     = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions     = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine          = true,
                        shape               = RoundedCornerShape(10.dp),
                        colors              = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            focusedLabelColor  = ForestGreen
                        )
                    )

                    if (isSignUp) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Password must be at least 6 characters",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = TextTertiary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Primary action button
                    Button(
                        onClick  = {
                            focusManager.clearFocus()
                            viewModel.submitEmailAuth()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled  = uiState !is AuthUiState.Loading,
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text       = if (isSignUp) "Create Account" else "Sign In",
                                color      = White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── RLS security note ────────────────────────────────────────────
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = SageGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("🔒", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Your data is protected",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 12.sp,
                            color      = ForestGreen
                        )
                        Text(
                            "Prescriptions and reports are secured by Supabase Row-Level Security, scoped to your authenticated session.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
