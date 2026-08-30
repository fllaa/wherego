package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType

@Composable
fun ParkItButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        border = if (enabled) BorderStroke(2.5.dp, colors.ink) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.teal,
            contentColor = Color.White,
            disabledContainerColor = colors.tealSoft.copy(alpha = 0.6f),
            disabledContentColor = colors.muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text("Park it", style = WheregoType.cta)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun WheregoPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Outlined.ArrowForward,
) {
    val colors = WheregoTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .then(if (enabled) Modifier.wheregoHardShadow(cornerRadius = 22.dp, offsetY = 4.dp) else Modifier),
        shape = RoundedCornerShape(22.dp),
        border = if (enabled) BorderStroke(2.5.dp, colors.ink) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.teal,
            contentColor = Color.White,
            disabledContainerColor = colors.tealSoft.copy(alpha = 0.6f),
            disabledContentColor = colors.muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(label, style = WheregoType.buttonLabel)
        if (icon != null) {
            Spacer(Modifier.width(9.dp))
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
