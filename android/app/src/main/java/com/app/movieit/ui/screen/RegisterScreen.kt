package com.app.movieit.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.movieit.ui.viewmodel.RegisterViewModel
import com.app.movieit.ui.theme.*
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.registered) {
        if (state.registered) {
            onRegisterSuccess()
            viewModel.consumeRegistered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Logo ──────────────────────────────────────────────────────────────
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 2.sp)) {
                    append("MOVIE")
                }
                withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Black, letterSpacing = 2.sp)) {
                    append("IT")
                }
            },
            fontSize = 42.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        // ── Email ─────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = TextStyle(color = TextPrimary),
            colors = authStyledTextFieldColors(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── Username ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = TextStyle(color = TextPrimary),
            colors = authStyledTextFieldColors(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(12.dp))

        // ── Password ──────────────────────────────────────────────────────────
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = !state.loading
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ascunde parola" else "Arată parola",
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            singleLine = true,
            enabled = !state.loading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(color = TextPrimary),
            colors = authStyledTextFieldColors(),
            shape = RoundedCornerShape(14.dp)
        )

        // ── Password checklist ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.password.isNotBlank(),
            enter = fadeIn() + slideInVertically()
        ) {
            PasswordChecklist(
                hasMinLength = state.hasMinLength,
                hasUppercase = state.hasUppercase,
                hasLowercase = state.hasLowercase,
                hasDigit = state.hasDigit,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ── Error ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.error != null,
            enter = fadeIn() + slideInVertically()
        ) {
            state.error?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRed.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("⚠ $err", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Register button ──────────────────────────────────────────────────
        Button(
            onClick = viewModel::register,
            enabled = !state.loading && (state.password.isBlank() || state.passwordValid),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple,
                disabledContainerColor = AccentPurple.copy(alpha = 0.5f)
            )
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextPrimary, strokeWidth = 2.dp)
            } else {
                Text("Create account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Divider ───────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Divider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
            Text("SAU", color = TextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
            Divider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
        }

        Spacer(Modifier.height(20.dp))

        // ── Login link ─────────────────────────────────────────────────────────
        Button(
            onClick = onNavigateToLogin,
            enabled = !state.loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(AccentPurple, GlowPurple))
            )
        ) {
            Text(
                "Already have an account? Login",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SoftLavender
            )
        }
    }
}

@Composable
private fun PasswordChecklist(
    hasMinLength: Boolean,
    hasUppercase: Boolean,
    hasLowercase: Boolean,
    hasDigit: Boolean,
    modifier: Modifier = Modifier
) {
    val metColor = Color(0xFF4CAF50)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PasswordRule(met = hasMinLength, label = "At least 8 characters", metColor = metColor)
        PasswordRule(met = hasUppercase, label = "One uppercase letter", metColor = metColor)
        PasswordRule(met = hasLowercase, label = "One lowercase letter", metColor = metColor)
        PasswordRule(met = hasDigit, label = "One number", metColor = metColor)
    }
}

@Composable
private fun PasswordRule(met: Boolean, label: String, metColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (met) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (met) metColor else TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (met) metColor else TextSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}