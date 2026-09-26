package com.lojia.shiftreport.auth

import com.lojia.shiftreport.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuickPinScreen(
    isBn: Boolean,
    preferencesRepository: PreferencesRepository,
    onAuthenticated: () -> Unit,
    onFallbackToLogin: () -> Unit,
    onTriggerBiometric: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            val isVerified = preferencesRepository.verifyPin(enteredPin)
            if (isVerified) {
                pinError = false
                onAuthenticated()
            } else {
                pinError = true
                delay(400)
                enteredPin = ""
            }
        } else {
            pinError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
            .padding(horizontal = 32.dp)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.quick_pin_enter_pin),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C20)
            )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.quick_pin_subtitle),
            fontSize = 14.sp,
            color = Color(0xFF44474E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val isFilled = i < enteredPin.length
                val color = if (pinError) Color(0xFFBA1A1A) else if (isFilled) Color(0xFF415F91) else Color(0xFFCAC4D0)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }

        if (pinError) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.quick_pin_incorrect),
                color = Color(0xFFBA1A1A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.height(36.dp)) // Maintain spacing
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Numpad
        NumpadView(
            onNumberClick = { num ->
                if (enteredPin.length < 4) {
                    enteredPin += num
                }
            },
            onDeleteClick = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            },
            showBiometric = preferencesRepository.isBiometricEnabled(),
            onBiometricClick = onTriggerBiometric
        )

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onFallbackToLogin) {
            Text(
                text = stringResource(R.string.quick_pin_use_password),
                color = Color(0xFF415F91),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
}

@Composable
fun NumpadView(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    showBiometric: Boolean,
    onBiometricClick: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (num in row) {
                    NumpadButton(text = num, onClick = { onNumberClick(num) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometric) {
                Box(
                    modifier = Modifier.weight(1f).aspectRatio(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBiometricClick) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric",
                            tint = Color(0xFF415F91),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            NumpadButton(text = "0", onClick = { onNumberClick("0") }, modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier.weight(1f).aspectRatio(1.2f),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDeleteClick) {
                    Text(
                        text = "DEL",
                        color = Color(0xFF44474E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.2f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF191C20)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
