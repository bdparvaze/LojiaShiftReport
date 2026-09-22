package com.lojia.shiftreport.ui.common



import androidx.compose.foundation.background


import androidx.compose.foundation.border


import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.foundation.text.BasicTextField


import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.material3.Text


import androidx.compose.runtime.Composable


import androidx.compose.ui.Alignment


import androidx.compose.ui.Modifier


import androidx.compose.ui.draw.clip


import androidx.compose.ui.graphics.Color


import androidx.compose.ui.text.font.FontWeight


import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.ui.unit.dp


import androidx.compose.ui.unit.sp

@Composable
fun OtpInputField(
    pin: String,
    onPinChange: (String) -> Unit,
    length: Int = 6,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = pin,
        onValueChange = { input ->
            if (input.length <= length && input.all { char -> char.isDigit() }) {
                onPinChange(input)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until length) {
                    val isFilled = i < pin.length
                    val isActive = i == pin.length
                    val digit = if (isFilled) pin[i].toString() else ""
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f) // Maintains perfectly balanced square shapes
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isActive -> Color(0xFFCCFBF1) // Light teal for active state
                                    isFilled -> Color(0xFFECFDF5) // Light mint for filled states
                                    else -> Color(0xFFF1F5F9)     // Soft neutral grey for empty state
                                }
                            )
                            .border(
                                width = if (isActive) 2.dp else 1.dp,
                                color = when {
                                    isActive -> Color(0xFF00796B) // High contrast teal border for active focus
                                    isFilled -> Color(0xFF059669) // Emerald border for filled inputs
                                    else -> Color(0xFFCBD5E1)     // Slate-300 border for empty slots
                                },
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = digit,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A) // Rich deep charcoal
                            )
                        } else if (isActive) {
                            // Sleek vertical cursor indicator to show active focus position
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF00796B))
                            )
                        }
                    }
                }
            }
        }
    )
}
