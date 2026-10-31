package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPrimaryButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.LeovaneyLogoBadge
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.GlassSurfaceWhite8
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite

@Composable
fun OnboardingScreen(
    initialName: String = "van",
    isSaving: Boolean = false,
    onCompleteOnboarding: (String) -> Unit
) {
    var nameInput by rememberSaveable { mutableStateOf(initialName.ifBlank { "van" }) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ambient glass halo around the LV logo
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GlassBorderWhite28, ObsidianBlack)
                        )
                    )
                    .border(1.dp, GlassBorderWhite15, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LeovaneyLogoBadge(
                    size = 88.dp,
                    modifier = Modifier.testTag("onboarding_logo")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(id = R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(id = R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedSilver,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_subtitle")
            )

            Spacer(modifier = Modifier.height(28.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorderWhite28
            ) {
                Text(
                    text = "PROFIL PENGGUNA TUNGGAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
                Spacer(modifier = Modifier.height(12.dp))

                GlassTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (localError != null && it.trim().isNotEmpty()) {
                            localError = null
                        }
                    },
                    label = "Nama Panggilan",
                    placeholder = "Masukkan nama Anda",
                    isError = localError != null,
                    supportingText = localError ?: "Nama ini akan ditampilkan pada sapaan dashboard pribadi Anda.",
                    testTag = "onboarding_name_input"
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassPrimaryButton(
                    text = stringResource(id = R.string.start_manage_finance),
                    isLoading = isSaving,
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isEmpty()) {
                            localError = "Nama tidak boleh kosong."
                        } else {
                            localError = null
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCompleteOnboarding(trimmed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_start_button"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OnboardingHighlightPill(icon = Icons.Default.OfflineBolt, label = "100% Offline")
                OnboardingHighlightPill(icon = Icons.Default.Lock, label = "Tanpa Login")
                OnboardingHighlightPill(icon = Icons.Default.Shield, label = "Database Lokal")
            }
        }
    }
}

@Composable
private fun OnboardingHighlightPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(GlassSurfaceWhite8)
            .border(1.dp, GlassBorderWhite15, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedSilver,
            fontWeight = FontWeight.Medium
        )
    }
}
