package com.lojia.pos.auth

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
import com.lojia.pos.data.PreferencesRepository
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isBn) "পিন লিখুন" else "Enter PIN",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isBn) "অ্যাপটি আনলক করতে আপনার 4-সংখ্যার পিন লিখুন" else "Enter your 4-digit PIN to unlock the app",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
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
                val color = if (pinError) Color(0xFFEF4444) else if (isFilled) Color(0xFF3858F6) else Color(0xFFE2E8F0)
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
                text = if (isBn) "ভুল পিন" else "Incorrect PIN",
                color = Color(0xFFEF4444),
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
                text = if (isBn) "পাসওয়ার্ড ব্যবহার করুন" else "Use Password",
                color = Color(0xFF3858F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
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
                            tint = Color(0xFF3858F6),
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
                        color = Color(0xFF64748B),
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
            containerColor = Color(0xFFF1F5F9),
            contentColor = Color(0xFF1E293B)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
