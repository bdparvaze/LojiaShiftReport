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
import com.lojia.shiftreport.ui.theme.*

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
                    
                    val boxShape = RoundedCornerShape(Dimens.RadiusMd)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .depthInset(
                                enabled = true,
                                isError = false,
                                isFocused = isActive,
                                shape = boxShape
                            )
                            .then(
                                if (isFilled) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = PosCashGreen,
                                        shape = boxShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = digit,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight
                            )
                        } else if (isActive) {
                            // Sleek vertical cursor indicator to show active focus position
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(18.dp)
                                    .background(SecondaryTealLight)
                            )
                        }
                    }
                }
            }
        }
    )
}
