package com.flla.wherego.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.flla.wherego.core.designsystem.component.WheregoBottomSheet
import com.flla.wherego.core.designsystem.component.WheregoCard
import com.flla.wherego.core.designsystem.component.WheregoSectionLabel
import com.flla.wherego.core.designsystem.component.WheregoSettingDivider
import com.flla.wherego.core.designsystem.component.WheregoSettingRow
import com.flla.wherego.core.designsystem.component.WheregoSettingsCard
import com.flla.wherego.core.designsystem.component.wheregoHardShadow
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.feature.auth.AuthViewModel

private val TimeZones = listOf(
    "Asia/Jakarta" to "Jakarta (WIB)",
    "Asia/Makassar" to "Makassar (WITA)",
    "Asia/Jayapura" to "Jayapura (WIT)",
    "Asia/Singapore" to "Singapore",
    "Asia/Kuala_Lumpur" to "Kuala Lumpur",
    "America/New_York" to "New York",
    "Europe/Berlin" to "Berlin",
)

private val Locales = listOf(
    "id-ID" to "Indonesian",
    "en-US" to "English",
)

private enum class ProfileSheet { NONE, TIMEZONE, LOCALE }

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    settings: SettingsViewModel = hiltViewModel(),
    auth: AuthViewModel = hiltViewModel(),
) {
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val state by settings.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(ProfileSheet.NONE) }
    var message by remember { mutableStateOf<String?>(null) }
    val heading = state.displayName.ifBlank { "Hey you" }
    val zoneLabel = TimeZones.firstOrNull { it.first == state.timeZoneId }?.second ?: state.timeZoneId
    val localeLabel = Locales.firstOrNull { it.first == state.localeTag }?.second ?: state.localeTag

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.sheet)
                    .border(BorderStroke(2.5.dp, colors.ink), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = colors.ink,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(photoUrl = state.photoUrl, signedIn = state.signedIn)
                Text(heading, style = WheregoType.onboardTitle, color = colors.ink)
                Text(
                    if (state.signedIn) state.email ?: "Backup is on"
                    else "Local notebook",
                    style = WheregoType.onboardSub,
                    color = colors.muted,
                )
            }
            NameField(value = state.displayName, onValueChange = settings::onDisplayName)
            WheregoSettingsCard {
                WheregoSettingRow(
                    icon = Icons.Outlined.Schedule,
                    badgeFill = colors.tealSoft,
                    label = "Timezone",
                    onClick = { sheet = ProfileSheet.TIMEZONE },
                    value = zoneLabel,
                )
                WheregoSettingDivider()
                WheregoSettingRow(
                    icon = Icons.Outlined.Language,
                    badgeFill = colors.tealSoft,
                    label = "Locale",
                    onClick = { sheet = ProfileSheet.LOCALE },
                    value = localeLabel,
                )
            }
            WheregoSectionLabel("BACKUP")
            WheregoCard(cornerRadius = 22.dp, padding = 16.dp, gap = 10.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Google", style = WheregoType.txTitle, color = colors.ink)
                    BackupChip(signedIn = state.signedIn)
                }
                Text(
                    state.email ?: "Not signed in",
                    style = WheregoType.helper,
                    color = colors.muted,
                )
                Text(
                    "Capture never waits on this.",
                    style = WheregoType.helper,
                    color = colors.muted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.signedIn) "Sign out" else "Sign in with Google",
                    style = WheregoType.chip,
                    color = if (state.signedIn) colors.coral else colors.tealDeep,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable {
                            if (state.signedIn) {
                                auth.signOut()
                                message = "Local stays. Cloud paused."
                            } else {
                                val activity = context.findActivity()
                                if (activity == null) {
                                    message = "Need an Activity to sign in."
                                } else {
                                    auth.signIn(activity) { message = it }
                                }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            message?.let {
                Text(it, style = WheregoType.meta, color = colors.coral)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    when (sheet) {
        ProfileSheet.NONE -> Unit
        ProfileSheet.TIMEZONE -> WheregoBottomSheet("Timezone", onDismiss = { sheet = ProfileSheet.NONE }) {
            TimeZones.forEach { (id, label) ->
                ProfileChoiceRow(
                    label = label,
                    selected = state.timeZoneId == id,
                    onClick = {
                        settings.onTimeZone(id)
                        sheet = ProfileSheet.NONE
                    },
                )
            }
        }
        ProfileSheet.LOCALE -> WheregoBottomSheet("Locale", onDismiss = { sheet = ProfileSheet.NONE }) {
            Locales.forEach { (tag, label) ->
                ProfileChoiceRow(
                    label = label,
                    selected = state.localeTag == tag,
                    onClick = {
                        settings.onLocale(tag)
                        sheet = ProfileSheet.NONE
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(photoUrl: String?, signedIn: Boolean) {
    val colors = WheregoTheme.colors
    Box(
        Modifier
            .size(88.dp)
            .wheregoHardShadow(shape = CircleShape, color = colors.shadow, offsetY = 5.dp)
            .clip(CircleShape)
            .background(colors.mascotFill)
            .border(BorderStroke(2.5.dp, colors.ink), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("🪙", fontSize = 40.sp)
        if (signedIn && !photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Name", style = WheregoType.settingLabel, color = colors.ink)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(40)) },
            singleLine = true,
            textStyle = WheregoType.settingLabel.copy(color = colors.ink),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.chipIdle)
                .border(BorderStroke(2.dp, colors.track), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun BackupChip(signedIn: Boolean) {
    val colors = WheregoTheme.colors
    val pill = RoundedCornerShape(99.dp)
    Text(
        if (signedIn) "Synced" else "Local only",
        style = WheregoType.leftPill,
        color = if (signedIn) colors.tealDeep else colors.muted,
        modifier = Modifier
            .clip(pill)
            .background(if (signedIn) colors.tealSoft else colors.track)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun ProfileChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = WheregoTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.tealSoft else colors.chipIdle)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = WheregoType.settingLabel, color = colors.ink)
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.tealDeep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
