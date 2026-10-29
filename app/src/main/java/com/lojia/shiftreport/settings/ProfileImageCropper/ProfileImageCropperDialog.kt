package com.lojia.shiftreport.settings.ProfileImageCropper

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.PrimaryIndigo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Material3 BottomSheet dialog providing interactive 1:1 circular cropping for
 * business profile and cashier avatars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileImageCropperDialog(
    imageUri: Uri,
    onCropSaved: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var cropDiameterPx by remember { mutableFloatStateOf(0f) }

    // Load and decode original bitmap asynchronously
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bmp = ImageCropUtils.decodeSampledBitmap(context, imageUri)
            originalBitmap = bmp
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Crop Profile Photo",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_cropper")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cropper Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryIndigo)
                } else if (originalBitmap != null) {
                    CropImageView(
                        bitmap = originalBitmap!!,
                        zoom = zoom,
                        onZoomChange = { zoom = it },
                        panOffset = panOffset,
                        onPanChange = { panOffset = it },
                        onDimensionsMeasured = { size, diameter ->
                            viewportSize = size
                            cropDiameterPx = diameter
                        }
                    )
                } else {
                    Text(
                        text = "Unable to load image",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Zoom Slider Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1.0f..4.0f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slider_cropper_zoom"),
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryIndigo,
                        activeTrackColor = PrimaryIndigo
                    )
                )
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Pinch to zoom • Drag to reposition",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Cancel / Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_cancel_crop"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val bitmap = originalBitmap
                        if (bitmap != null && viewportSize.width > 0 && cropDiameterPx > 0) {
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val croppedBitmap = withContext(Dispatchers.Default) {
                                        ImageCropUtils.cropToCircleBitmap(
                                            originalBitmap = bitmap,
                                            cropDiameterPx = cropDiameterPx,
                                            viewportWidth = viewportSize.width.toFloat(),
                                            viewportHeight = viewportSize.height.toFloat(),
                                            userZoom = zoom,
                                            panX = panOffset.x,
                                            panY = panOffset.y,
                                            targetSize = ImageCropUtils.TARGET_AVATAR_SIZE
                                        )
                                    }

                                    // Save to permanent app file
                                    val outFile = File(context.filesDir, "profile_cropped_${System.currentTimeMillis()}.jpg")
                                    val cacheCopy = File(context.cacheDir, "profile_cropped.jpg")

                                    val success = withContext(Dispatchers.IO) {
                                        val ok1 = ImageCropUtils.saveCroppedBitmap(croppedBitmap, outFile)
                                        val ok2 = ImageCropUtils.saveCroppedBitmap(croppedBitmap, cacheCopy)
                                        ok1 || ok2
                                    }

                                    isSaving = false
                                    if (success) {
                                        val finalUri = Uri.fromFile(if (outFile.exists()) outFile else cacheCopy)
                                        onCropSaved(finalUri)
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Failed to save cropped image", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isSaving = false
                                    Toast.makeText(context, "Crop error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isSaving && originalBitmap != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_crop"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
