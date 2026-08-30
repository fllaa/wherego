package com.flla.wherego.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.R
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.model.MoneyFormatter
import java.io.File

@Composable
fun ReceiptAttachDialog(
    transactionId: String,
    onFinished: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(transactionId) { viewModel.reset(transactionId) }

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.ingest(uri) else onFinished()
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) viewModel.ingest(uri) else onFinished()
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = cameraCaptureUri(context)
            cameraUri = uri
            takePicture.launch(uri)
        } else {
            onFinished()
        }
    }

    when {
        state.busy -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.receipt_reading_title)) },
                text = { Text(stringResource(R.string.receipt_reading_body)) },
                confirmButton = {},
            )
        }
        state.proposedAmount != null -> {
            val label = MoneyFormatter.format(state.proposedAmount!!, state.currency)
            AlertDialog(
                onDismissRequest = { viewModel.keepAmount() },
                title = { Text(stringResource(R.string.receipt_confirm_title)) },
                text = { Text(stringResource(R.string.receipt_confirm_body, label)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmAmount() }) { Text(stringResource(R.string.receipt_use_it)) }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.keepAmount() }) { Text(stringResource(R.string.receipt_keep_mine)) }
                },
            )
        }
        state.savedLocal -> {
            AlertDialog(
                onDismissRequest = onFinished,
                title = { Text(stringResource(R.string.receipt_parked_title)) },
                text = {
                    Text(state.error?.let { stringResource(it) } ?: stringResource(R.string.receipt_parked_body))
                },
                confirmButton = {
                    TextButton(onClick = onFinished) { Text(stringResource(R.string.dialog_ok)) }
                },
            )
        }
        else -> {
            AlertDialog(
                onDismissRequest = onFinished,
                title = { Text(stringResource(R.string.receipt_attach_title)) },
                text = { Text(stringResource(R.string.receipt_attach_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                val uri = cameraCaptureUri(context)
                                cameraUri = uri
                                takePicture.launch(uri)
                            } else {
                                requestCamera.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ) { Text(stringResource(R.string.receipt_camera)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pickGallery.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) { Text(stringResource(R.string.receipt_gallery)) }
                },
            )
        }
    }
}

private fun cameraCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "shot.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
