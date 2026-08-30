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
import androidx.compose.ui.res.stringResource
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
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.model.AppLanguage
import com.flla.wherego.feature.auth.AuthViewModel
import com.flla.wherego.feature.auth.SignInResult

private val TimeZones = listOf(
    "Asia/Jakarta" to R.string.tz_jakarta,
    "Asia/Makassar" to R.string.tz_makassar,
    "Asia/Jayapura" to R.string.tz_jayapura,
    "Asia/Singapore" to R.string.tz_singapore,
    "Asia/Kuala_Lumpur" to R.string.tz_kuala_lumpur,
    "America/New_York" to R.string.tz_new_york,
    "Europe/Berlin" to R.string.tz_berlin,
)

private val Languages = listOf(
    AppLanguage.SYSTEM to R.string.lang_system,
    AppLanguage.ID to R.string.lang_id,
    AppLanguage.EN to R.string.lang_en,
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
    var signedOutBanner by remember { mutableStateOf(false) }
    var authResult by remember { mutableStateOf<SignInResult?>(null) }
    val heading = state.displayName.ifBlank { stringResource(R.string.me_greeting_fallback) }
    val parsedLanguage = AppLanguage.parse(state.localeTag)
    val zoneLabel = TimeZones.firstOrNull { it.first == state.timeZoneId }
        ?.let { stringResource(it.second) }
        ?: state.timeZoneId
    val localeLabel = Languages.firstOrNull { it.first == parsedLanguage }
        ?.let { stringResource(it.second) }
        ?: stringResource(R.string.lang_system)

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
                    contentDescription = stringResource(R.string.ds_cd_back),
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
                    if (state.signedIn) {
                        state.email?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.profile_sub_backup_on)
                    } else {
                        stringResource(R.string.profile_sub_local)
                    },
                    style = WheregoType.onboardSub,
                    color = colors.muted,
                )
            }
            NameField(value = state.displayName, onValueChange = settings::onDisplayName)
            WheregoSettingsCard {
                WheregoSettingRow(
                    icon = Icons.Outlined.Schedule,
                    badgeFill = colors.tealSoft,
                    label = stringResource(R.string.profile_row_timezone),
                    onClick = { sheet = ProfileSheet.TIMEZONE },
                    value = zoneLabel,
                )
                WheregoSettingDivider()
                WheregoSettingRow(
                    icon = Icons.Outlined.Language,
                    badgeFill = colors.tealSoft,
                    label = stringResource(R.string.profile_row_language),
                    onClick = { sheet = ProfileSheet.LOCALE },
                    value = localeLabel,
                )
            }
            WheregoSectionLabel(stringResource(R.string.profile_section_backup))
            WheregoCard(cornerRadius = 22.dp, padding = 16.dp, gap = 10.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.profile_backup_google),
                        style = WheregoType.txTitle,
                        color = colors.ink,
                    )
                    BackupChip(signedIn = state.signedIn)
                }
                Text(
                    state.email ?: stringResource(R.string.profile_backup_not_signed_in),
                    style = WheregoType.helper,
                    color = colors.muted,
                )
                Text(
                    stringResource(R.string.profile_backup_body),
                    style = WheregoType.helper,
                    color = colors.muted,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.signedIn) {
                        stringResource(R.string.auth_sign_out)
                    } else {
                        stringResource(R.string.auth_sign_in_google)
                    },
                    style = WheregoType.chip,
                    color = if (state.signedIn) colors.coral else colors.tealDeep,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .clickable {
                            if (state.signedIn) {
                                auth.signOut()
                                signedOutBanner = true
                                authResult = null
                            } else {
                                signedOutBanner = false
                                auth.signIn(context.findActivity()) { authResult = it }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            if (signedOutBanner) {
                Text(
                    stringResource(R.string.auth_sign_out_done),
                    style = WheregoType.meta,
                    color = colors.coral,
                )
            } else {
                authResult?.let { result ->
                    Text(
                        authBanner(result),
                        style = WheregoType.meta,
                        color = colors.coral,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    when (sheet) {
        ProfileSheet.NONE -> Unit
        ProfileSheet.TIMEZONE -> WheregoBottomSheet(
            stringResource(R.string.profile_sheet_timezone),
            onDismiss = { sheet = ProfileSheet.NONE },
        ) {
            TimeZones.forEach { (id, labelRes) ->
                ProfileChoiceRow(
                    label = stringResource(labelRes),
                    selected = state.timeZoneId == id,
                    onClick = {
                        settings.onTimeZone(id)
                        sheet = ProfileSheet.NONE
                    },
                )
            }
        }
        ProfileSheet.LOCALE -> WheregoBottomSheet(
            stringResource(R.string.profile_sheet_language),
            onDismiss = { sheet = ProfileSheet.NONE },
        ) {
            Languages.forEach { (tag, labelRes) ->
                ProfileChoiceRow(
                    label = stringResource(labelRes),
                    selected = parsedLanguage == tag,
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
private fun authBanner(result: SignInResult): String = when (result) {
    SignInResult.Ok -> stringResource(R.string.auth_ok_backup_on)
    is SignInResult.Failed -> stringResource(result.messageRes)
    is SignInResult.FailedRaw -> result.message.ifBlank {
        stringResource(R.string.auth_err_sign_in_failed)
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
        Text(stringResource(R.string.me_field_name), style = WheregoType.settingLabel, color = colors.ink)
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
        if (signedIn) stringResource(R.string.me_pill_synced) else stringResource(R.string.profile_pill_local_only),
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
