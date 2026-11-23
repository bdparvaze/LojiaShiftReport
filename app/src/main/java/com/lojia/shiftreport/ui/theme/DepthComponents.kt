package com.lojia.shiftreport.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private fun readAnimatorDurationScale(context: Context): Float {
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    } catch (_: Exception) {
        1f
    }
}

/**
 * Draws a Gaussian drop shadow outside a clipped shape using exact CSS-to-Gaussian
 * sigma mapping `sigma = B / 2` where B is the CSS blur radius in px.
 */
private fun DrawScope.drawSoftDropShadow(
    color: Color,
    offsetY: Dp,
    blurRadius: Dp,
    cornerRadius: Dp
) {
    val blurPx = blurRadius.toPx()
    if (blurPx <= 0.5f || color.alpha <= 0f) return
    val offsetPx = offsetY.toPx()
    val radiusPx = cornerRadius.toPx()
    val sigmaPx = (blurPx / 2f).coerceAtLeast(0.5f)
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color.toArgb()
            maskFilter = android.graphics.BlurMaskFilter(
                sigmaPx,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        val rect = android.graphics.RectF(
            0f,
            offsetPx,
            size.width,
            size.height + offsetPx
        )
        canvas.nativeCanvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
    }
}

/**
 * Draws a crisp perimeter stroke fully inside [shape] bounds so anti-aliased clipping
 * and inner fills/highlights never wash out or overwrite the border color.
 */
private fun DrawScope.drawInsetShapeBorder(
    shape: Shape,
    borderWidthPx: Float,
    color: Color
) {
    if (borderWidthPx <= 0f || color.alpha <= 0f) return
    val w = (size.width - borderWidthPx).coerceAtLeast(0f)
    val h = (size.height - borderWidthPx).coerceAtLeast(0f)
    if (w <= 0f || h <= 0f) return
    val half = borderWidthPx * 0.5f
    val outline = shape.createOutline(size, layoutDirection, this)
    if (outline is Outline.Rounded) {
        val rr = outline.roundRect
        val shrink = { cr: CornerRadius ->
            CornerRadius((cr.x - half).coerceAtLeast(0f), (cr.y - half).coerceAtLeast(0f))
        }
        val insetRr = RoundRect(
            left = rr.left + half,
            top = rr.top + half,
            right = rr.right - half,
            bottom = rr.bottom - half,
            topLeftCornerRadius = shrink(rr.topLeftCornerRadius),
            topRightCornerRadius = shrink(rr.topRightCornerRadius),
            bottomRightCornerRadius = shrink(rr.bottomRightCornerRadius),
            bottomLeftCornerRadius = shrink(rr.bottomLeftCornerRadius)
        )
        drawOutline(
            outline = Outline.Rounded(insetRr),
            color = color,
            style = Stroke(width = borderWidthPx)
        )
    } else {
        val insetOutline = shape.createOutline(Size(w, h), layoutDirection, this)
        translate(left = half, top = half) {
            drawOutline(
                outline = insetOutline,
                color = color,
                style = Stroke(width = borderWidthPx)
            )
        }
    }
}

private fun Modifier.trackButtonPress(
    enabled: Boolean,
    onPressedChange: (Boolean) -> Unit
): Modifier = pointerInput(enabled) {
    if (!enabled) {
        onPressedChange(false)
        return@pointerInput
    }
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            onPressedChange(event.changes.any { it.pressed })
        }
    }
}

/**
 * Raised soft-depth card container styling driven strictly by [LocalDepthStyle].
 */
fun Modifier.depthCard(
    accent: Boolean = false,
    shape: Shape? = null,
    cornerRadius: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedRadius = cornerRadius ?: style.cardCornerRadius
    val resolvedShape = shape ?: RoundedCornerShape(resolvedRadius)
    val gradientColors = if (accent) {
        listOf(style.cardAccentGradientStart, style.cardAccentGradientEnd)
    } else {
        listOf(style.cardGradientStart, style.cardGradientEnd)
    }
    val borderColor = if (accent) {
        style.cardAccentBorderColor.copy(alpha = style.cardAccentBorderAlpha)
    } else {
        style.cardBorderColor.copy(alpha = style.cardBorderAlpha)
    }

    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = style.cardAmbientShadowAlpha),
                offsetY = style.cardShadowCloseOffsetY,
                blurRadius = style.cardShadowCloseBlur,
                cornerRadius = resolvedRadius
            )
            drawSoftDropShadow(
                color = style.cardShadowColor.copy(alpha = style.cardSpotShadowAlpha),
                offsetY = style.cardShadowFarOffsetY,
                blurRadius = style.cardShadowFarBlur,
                cornerRadius = resolvedRadius
            )
        }
        .clip(resolvedShape)
        .drawBehind {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            val borderPx = style.cardBorderWidth.toPx()
            val sheenY = borderPx + borderPx * 0.5f
            val insetX = resolvedRadius.toPx().coerceAtMost(size.width * 0.25f) * 0.25f + borderPx
            drawLine(
                color = PureWhite.copy(alpha = style.cardTopSheenAlpha),
                start = Offset(insetX, sheenY),
                end = Offset((size.width - insetX).coerceAtLeast(insetX), sheenY),
                strokeWidth = borderPx
            )
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = borderPx,
                color = borderColor
            )
        }
}

/**
 * Raised soft-depth semantic card preserving an explicit semantic tint and border color.
 */
fun Modifier.depthSemanticCard(
    backgroundColor: Color,
    borderColor: Color,
    shape: Shape? = null,
    cornerRadius: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedRadius = cornerRadius ?: style.cardCornerRadius
    val resolvedShape = shape ?: RoundedCornerShape(resolvedRadius)

    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = style.cardAmbientShadowAlpha),
                offsetY = style.cardShadowCloseOffsetY,
                blurRadius = style.cardShadowCloseBlur,
                cornerRadius = resolvedRadius
            )
            drawSoftDropShadow(
                color = style.cardShadowColor.copy(alpha = style.cardSpotShadowAlpha),
                offsetY = style.cardShadowFarOffsetY,
                blurRadius = style.cardShadowFarBlur,
                cornerRadius = resolvedRadius
            )
        }
        .clip(resolvedShape)
        .drawBehind {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, color = backgroundColor)
            val borderPx = style.cardBorderWidth.toPx()
            val sheenY = borderPx + borderPx * 0.5f
            val insetX = borderPx
            drawLine(
                color = PureWhite.copy(alpha = style.cardTopSheenAlpha * 0.65f),
                start = Offset(insetX, sheenY),
                end = Offset((size.width - insetX).coerceAtLeast(insetX), sheenY),
                strokeWidth = borderPx
            )
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = borderPx,
                color = borderColor
            )
        }
}

/**
 * Pressed-in inset surface styling for input fields and recessed wells.
 * Draws gradient fill -> inner shadows -> bottom white highlight -> crisp perimeter border on top.
 */
fun Modifier.depthInset(
    enabled: Boolean = true,
    isError: Boolean = false,
    isFocused: Boolean = false,
    shape: Shape? = null,
    cornerRadius: Dp? = null,
    borderWidth: Dp? = null,
    focusedBorderWidth: Dp? = null,
    gradientColors: List<Color>? = null,
    borderColorOverride: Color? = null,
    topShadowAlphaOverride: Float? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedRadius = cornerRadius ?: style.insetCornerRadius
    val resolvedShape = shape ?: RoundedCornerShape(resolvedRadius)
    val resolvedTopShadowAlpha = topShadowAlphaOverride ?: style.insetTopShadowAlpha
    val resolvedGradient = when {
        !enabled -> listOf(style.insetDisabledBackground, style.insetDisabledBackground)
        isError -> listOf(style.insetErrorBackground, style.insetErrorBackground)
        gradientColors != null -> gradientColors
        isFocused -> listOf(style.insetGradientStart, style.insetFocusedBackground)
        else -> listOf(style.insetGradientStart, style.insetBackground)
    }
    val resolvedBorder = when {
        borderColorOverride != null -> borderColorOverride
        !enabled -> style.insetDisabledBorderColor
        isError -> style.insetErrorBorderColor
        isFocused -> style.insetFocusedBorderColor
        else -> style.insetBorderColor.copy(alpha = style.insetBorderAlpha)
    }
    val baseBorder = borderWidth ?: style.insetBorderWidth
    val focusBorder = focusedBorderWidth ?: style.insetFocusedBorderWidth
    val resolvedBorderWidth = if (isFocused || isError) focusBorder else baseBorder

    this
        .clip(resolvedShape)
        .drawBehind {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            // 1) Recessed vertical gradient fill
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(colors = resolvedGradient)
            )

            // 2) Top inner shadow (inset 0 2dp 4dp rgba(16,24,40,0.07))
            val shadowHeightPx = (style.insetShadowOffsetY + style.insetShadowBlur).toPx()
                .coerceAtMost(size.height * 0.45f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        style.insetShadowColor.copy(alpha = resolvedTopShadowAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = shadowHeightPx
                ),
                size = Size(size.width, shadowHeightPx)
            )

            // 3) Bottom 1dp white bevel highlight inside the perimeter stroke
            val borderPx = resolvedBorderWidth.toPx()
            val highlightPx = 1.dp.toPx()
            val bottomY = (size.height - borderPx - highlightPx * 0.5f).coerceAtLeast(0f)
            drawLine(
                color = style.insetBottomHighlightColor.copy(alpha = style.insetBottomHighlightAlpha),
                start = Offset(borderPx, bottomY),
                end = Offset((size.width - borderPx).coerceAtLeast(borderPx), bottomY),
                strokeWidth = highlightPx
            )

            // 4) Crisp perimeter border drawn LAST above fill, inner shadows, and highlights
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = borderPx,
                color = resolvedBorder
            )
        }
}

/**
 * Raised 3D button styling with brand gradient face and crisp unblurred lower lip,
 * preserving minimum touch targets and tactile press compression honoring reduced motion.
 */
fun Modifier.depthRaisedButton(
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    cornerRadius: Dp? = null,
    minHeight: Dp = Dimens.ButtonHeightPrimary
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = cornerRadius ?: style.buttonCornerRadius
    val shape = RoundedCornerShape(resolvedCorner)
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }
    val animateMotion = !isInspection && shouldAnimate(animatorScale)
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (animateMotion && enabled && isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (animateMotion) style.pressDurationMs else 0),
        label = "raised_btn_press"
    )

    val shadowColor = if (isDestructive) style.destructiveButtonShadowColor else style.buttonShadowColor

    this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = minHeight.coerceAtLeast(Dimens.MinTouchTarget)
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
        .graphicsLayer {
            if (animateMotion && enabled) {
                translationY = pressProgress * style.buttonPressedTranslationY.toPx()
                val s = 1f - pressProgress * (1f - style.buttonPressedScale)
                scaleX = s
                scaleY = s
            }
        }
        .drawBehind {
            if (enabled) {
                drawSoftDropShadow(
                    color = shadowColor.copy(alpha = style.buttonShadowAlpha),
                    offsetY = style.buttonShadowOffsetY,
                    blurRadius = style.buttonShadowBlur,
                    cornerRadius = resolvedCorner
                )
            }
        }
        .clip(shape)
        .drawBehind {
            val lipPx = style.buttonLipHeight.toPx().coerceAtMost(size.height * 0.25f)
            val cornerPx = resolvedCorner.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)

            val lipColor = when {
                !enabled -> style.buttonLipDisabledColor.copy(alpha = style.buttonLipDisabledAlpha)
                isDestructive -> style.destructiveButtonLipColor.copy(alpha = style.buttonLipAlpha)
                else -> style.buttonLipColor.copy(alpha = style.buttonLipAlpha)
            }
            // 1) Unblurred solid 4dp bottom lip
            drawRoundRect(
                color = lipColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cr
            )

            // 2) Raised gradient face above the lip
            val faceHeight = (size.height - lipPx).coerceAtLeast(1f)
            val faceColors = when {
                !enabled -> listOf(style.buttonDisabledStart, style.buttonDisabledEnd)
                isDestructive -> listOf(style.destructiveButtonGradientStart, style.destructiveButtonGradientEnd)
                else -> listOf(style.buttonGradientStart, style.buttonGradientEnd)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = faceColors,
                    startY = 0f,
                    endY = faceHeight
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, faceHeight),
                cornerRadius = cr
            )

            // 3) Top 1dp white specular highlight
            if (enabled) {
                val highlightPx = 1.dp.toPx()
                val insetX = (cornerPx * 0.35f).coerceAtMost(size.width * 0.25f)
                drawLine(
                    color = PureWhite.copy(alpha = style.buttonTopHighlightAlpha),
                    start = Offset(insetX, highlightPx * 0.5f),
                    end = Offset((size.width - insetX).coerceAtLeast(insetX), highlightPx * 0.5f),
                    strokeWidth = highlightPx
                )
            }
        }
        .padding(bottom = style.buttonLipHeight)
}

/**
 * Lighter raised 3D secondary/outlined button styling with subtle surface gradient, unblurred lower lip,
 * perimeter stroke, soft shadow, and >= 48.dp touch target preservation.
 */
fun Modifier.depthSecondaryButton(
    enabled: Boolean = true,
    borderColor: Color? = null,
    cornerRadius: Dp? = null,
    borderWidth: Dp? = null,
    lipHeight: Dp? = null,
    lipColor: Color? = null,
    gradientColors: List<Color>? = null,
    shadowColor: Color? = null,
    shadowAlpha: Float? = null,
    shadowOffsetY: Dp? = null,
    shadowBlur: Dp? = null,
    minHeight: Dp = Dimens.ButtonHeightPrimary
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = cornerRadius ?: style.buttonCornerRadius
    val shape = RoundedCornerShape(resolvedCorner)
    val resolvedBorderColor = borderColor ?: style.secondaryButtonBorderColor
    val resolvedBorderWidth = borderWidth ?: style.secondaryButtonBorderWidth
    val resolvedLipHeight = lipHeight ?: style.secondaryButtonLipHeight
    val resolvedLipColor = (lipColor ?: style.secondaryButtonLipColor).copy(
        alpha = if (enabled) style.secondaryButtonLipAlpha else style.secondaryButtonLipDisabledAlpha
    )
    val resolvedShadowColor = (shadowColor ?: style.secondaryButtonShadowColor).copy(
        alpha = shadowAlpha ?: style.secondaryButtonShadowAlpha
    )
    val resolvedShadowOffsetY = shadowOffsetY ?: style.buttonShadowOffsetY
    val resolvedShadowBlur = shadowBlur ?: style.buttonShadowBlur

    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }
    val animateMotion = !isInspection && shouldAnimate(animatorScale)
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (animateMotion && enabled && isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (animateMotion) style.pressDurationMs else 0),
        label = "secondary_btn_press"
    )

    this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = minHeight.coerceAtLeast(Dimens.MinTouchTarget)
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
        .graphicsLayer {
            if (animateMotion && enabled) {
                translationY = pressProgress * style.buttonPressedTranslationY.toPx()
            }
        }
        .drawBehind {
            if (enabled) {
                drawSoftDropShadow(
                    color = resolvedShadowColor,
                    offsetY = resolvedShadowOffsetY,
                    blurRadius = resolvedShadowBlur,
                    cornerRadius = resolvedCorner
                )
            }
        }
        .clip(shape)
        .drawBehind {
            val lipPx = resolvedLipHeight.toPx().coerceAtMost(size.height * 0.25f)
            val cornerPx = resolvedCorner.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)

            drawRoundRect(
                color = resolvedLipColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cr
            )

            val faceHeight = (size.height - lipPx).coerceAtLeast(1f)
            val faceColors = if (enabled) {
                gradientColors ?: listOf(style.secondaryButtonGradientStart, style.secondaryButtonGradientEnd)
            } else {
                listOf(style.secondaryButtonDisabledStart, style.secondaryButtonDisabledEnd)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = faceColors,
                    startY = 0f,
                    endY = faceHeight
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, faceHeight),
                cornerRadius = cr
            )

            if (resolvedBorderWidth > 0.dp) {
                drawInsetShapeBorder(
                    shape = shape,
                    borderWidthPx = resolvedBorderWidth.toPx(),
                    color = resolvedBorderColor
                )
            }
        }
        .padding(bottom = resolvedLipHeight)
}

/**
 * Square raised 3D keypad key styling for the Quick PIN screen matching Stage 26D boards.
 * Draws outer drop shadow -> unblurred solid 4dp #D5DCEB bottom lip -> #FFFFFF..#F1F4FB face.
 */
fun Modifier.depthKeypadKey(
    enabled: Boolean = true,
    cornerRadius: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = cornerRadius ?: style.keypadCornerRadius
    val shape = RoundedCornerShape(resolvedCorner)
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }
    val animateMotion = !isInspection && shouldAnimate(animatorScale)
    var isPressed by remember { mutableStateOf(false) }
    val pressProgress by animateFloatAsState(
        targetValue = if (animateMotion && enabled && isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (animateMotion) style.pressDurationMs else 0),
        label = "keypad_key_press"
    )

    this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = style.keypadHeight
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
        .graphicsLayer {
            if (animateMotion && enabled) {
                translationY = pressProgress * style.buttonPressedTranslationY.toPx()
            }
        }
        .drawBehind {
            if (enabled) {
                drawSoftDropShadow(
                    color = style.keypadShadowColor.copy(alpha = style.keypadShadowAlpha),
                    offsetY = style.keypadShadowOffsetY,
                    blurRadius = style.keypadShadowBlur,
                    cornerRadius = resolvedCorner
                )
            }
        }
        .clip(shape)
        .drawBehind {
            val lipPx = style.buttonLipHeight.toPx().coerceAtMost(size.height * 0.25f)
            val cornerPx = resolvedCorner.toPx()
            val cr = CornerRadius(cornerPx, cornerPx)

            // 1) Crisp unblurred 4dp solid #D5DCEB lip (no maskFilter)
            drawRoundRect(
                color = if (enabled) style.keypadLipColor else style.buttonLipDisabledColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cr
            )

            // 2) Full vertical gradient #FFFFFF -> #F1F4FB face above the 4dp lip
            val faceHeight = (size.height - lipPx).coerceAtLeast(1f)
            val faceColors = if (enabled) {
                listOf(style.keypadGradientStart, style.keypadGradientEnd)
            } else {
                listOf(style.secondaryButtonDisabledStart, style.secondaryButtonDisabledEnd)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = faceColors,
                    startY = 0f,
                    endY = faceHeight
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, faceHeight),
                cornerRadius = cr
            )

            if (enabled) {
                val highlightPx = 1.dp.toPx()
                val insetX = (cornerPx * 0.35f).coerceAtMost(size.width * 0.25f)
                drawLine(
                    color = PureWhite,
                    start = Offset(insetX, highlightPx * 0.5f),
                    end = Offset((size.width - insetX).coerceAtLeast(insetX), highlightPx * 0.5f),
                    strokeWidth = highlightPx
                )
            }
        }
        .padding(bottom = style.buttonLipHeight)
}

/**
 * Standalone 48dp diagonal-gradient app logo tile with soft drop shadow and 1dp inner bottom edge.
 */
fun Modifier.depthLogoTile(): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = Dimens.RadiusLogoTile
    val shape = RoundedCornerShape(resolvedCorner)

    this
        .size(Dimens.LogoTileSize)
        .drawBehind {
            drawSoftDropShadow(
                color = style.logoTileShadowColor.copy(alpha = style.logoTileShadowAlpha),
                offsetY = Dimens.LogoTileShadowOffsetY,
                blurRadius = Dimens.LogoTileShadowBlur,
                cornerRadius = resolvedCorner
            )
        }
        .clip(shape)
        .drawBehind {
            // 145-degree CSS linear gradient vector
            val dx = size.width * 0.287f
            val dy = size.height * 0.410f
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(style.logoTileGradientStart, style.logoTileGradientEnd),
                    start = Offset(cx - dx, cy - dy),
                    end = Offset(cx + dx, cy + dy)
                ),
                size = size
            )
            val edgePx = 1.dp.toPx()
            drawLine(
                color = style.cardShadowCloseColor.copy(alpha = style.logoTileBottomEdgeAlpha),
                start = Offset(0f, size.height - edgePx * 0.5f),
                end = Offset(size.width, size.height - edgePx * 0.5f),
                strokeWidth = edgePx
            )
        }
}

/**
 * Top brand gradient container with RTL-safe padding and bottom-start/end corners.
 */
@Composable
fun LojiaGradientHeader(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomStart = Dimens.RadiusXl, bottomEnd = Dimens.RadiusXl),
    cornerRadiusForShadow: Dp = 0.dp,
    showDropShadow: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Dimens.SpacingLg,
        vertical = Dimens.SpacingLg
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalDepthStyle.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showDropShadow) {
                    Modifier.drawBehind {
                        drawSoftDropShadow(
                            color = style.headerShadowColor.copy(alpha = style.headerShadowAlpha),
                            offsetY = Dimens.HeaderShadowOffsetY,
                            blurRadius = Dimens.HeaderShadowBlur,
                            cornerRadius = cornerRadiusForShadow
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawBehind {
                // 135-deg linear gradient matching board GR6 (#2B5DE8 -> #1238A8)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(style.headerGradientStart, style.headerGradientEnd),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    size = size
                )
                val sheenHeight = size.height * 0.45f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PureWhite.copy(alpha = style.headerSheenAlpha),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = sheenHeight
                    ),
                    size = Size(size.width, sheenHeight)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * One-time entrance fade and upward rise animation staggered by [index].
 * Automatically skipped when system animator duration scale is 0 (reduced motion) or in preview inspection.
 */
fun Modifier.cardRise(index: Int): Modifier = composed {
    val style = LocalDepthStyle.current
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }

    if (isInspection || !shouldAnimate(animatorScale)) {
        return@composed this
    }

    val progress = remember { Animatable(0f) }
    val delayMs = remember(index, style.cardRiseStaggerStepMs, style.cardRiseMaxDelayMs) {
        cardRiseDelayMillis(
            index = index,
            stepMs = style.cardRiseStaggerStepMs,
            maxDelayMs = style.cardRiseMaxDelayMs
        )
    }

    LaunchedEffect(Unit) {
        if (delayMs > 0) {
            delay(delayMs.toLong())
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = style.cardRiseDurationMs,
                easing = DepthMotionTokens.CardRiseEasing
            )
        )
    }

    this.graphicsLayer {
        val value = progress.value
        alpha = value
        translationY = (1f - value) * style.cardRiseTranslationY.toPx()
    }
}

/**
 * Pulsing outer halo modifier for small LIVE status dots, honoring reduced-motion settings.
 */
fun Modifier.livePulse(): Modifier = composed {
    val style = LocalDepthStyle.current
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }

    if (isInspection || !shouldAnimate(animatorScale)) {
        return@composed this.drawBehind {
            val baseRadius = size.minDimension / 2f
            drawCircle(
                color = style.liveDotColor.copy(alpha = style.liveHaloSteadyAlpha),
                radius = baseRadius * 1.45f,
                center = center
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "live_pulse")
    val pulseFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = style.livePulsePeriodMs,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_pulse_fraction"
    )

    this.drawBehind {
        val baseRadius = size.minDimension / 2f
        val haloRadius = baseRadius * (1f + (style.liveHaloMaxScale - 1f) * pulseFraction)
        val haloAlpha = style.liveHaloStartAlpha +
            (style.liveHaloEndAlpha - style.liveHaloStartAlpha) * pulseFraction
        drawCircle(
            color = style.liveDotColor.copy(alpha = haloAlpha.coerceIn(0f, 1f)),
            radius = haloRadius,
            center = center
        )
    }
}

/**
 * Compact circular LIVE indicator dot combining [livePulse] and [DepthStyle.liveDotColor].
 */
@Composable
fun LojiaLiveDot(modifier: Modifier = Modifier) {
    val style = LocalDepthStyle.current
    Box(
        modifier = modifier
            .size(style.liveDotSize)
            .livePulse()
            .background(color = style.liveDotColor, shape = CircleShape)
    )
}

/**
 * Tab bar container shadow matching board SH8 (0 3dp 8dp rgba(16,24,40,0.08)).
 */
fun Modifier.depthTabBarShadow(): Modifier = composed {
    val style = LocalDepthStyle.current
    this.drawBehind {
        drawSoftDropShadow(
            color = style.tabBarShadowColor.copy(alpha = style.tabBarShadowAlpha),
            offsetY = Dimens.TabBarShadowOffsetY,
            blurRadius = Dimens.TabBarShadowBlur,
            cornerRadius = 0.dp
        )
    }
}

/**
 * Active tab bottom pill glow matching board SH9 (0 -2dp 8dp rgba(29,79,216,0.35)).
 */
fun Modifier.depthTabIndicatorGlow(cornerRadius: Dp = Dimens.TabIndicatorHeight): Modifier = composed {
    val style = LocalDepthStyle.current
    this.drawBehind {
        drawSoftDropShadow(
            color = style.tabIndicatorGlowColor.copy(alpha = style.tabIndicatorGlowAlpha),
            offsetY = Dimens.TabIndicatorGlowOffsetY,
            blurRadius = Dimens.TabIndicatorGlowBlur,
            cornerRadius = cornerRadius
        )
    }
}

/**
 * Raised tinted gradient card for Due History / Ledger KPI & summary cards (SH2 + gradient + 1dp border).
 */
fun Modifier.depthGradientCard(
    gradientColors: List<Color>,
    borderColor: Color,
    cornerRadius: Dp = Dimens.RadiusKpiCard
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = style.cardAmbientShadowAlpha),
                offsetY = style.cardShadowCloseOffsetY,
                blurRadius = style.cardShadowCloseBlur,
                cornerRadius = cornerRadius
            )
            drawSoftDropShadow(
                color = style.cardShadowColor.copy(alpha = style.cardSpotShadowAlpha),
                offsetY = style.cardShadowFarOffsetY,
                blurRadius = style.cardShadowFarBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            val borderPx = style.cardBorderWidth.toPx()
            val sheenY = borderPx + borderPx * 0.5f
            val insetX = cornerRadius.toPx().coerceAtMost(size.width * 0.25f) * 0.25f + borderPx
            drawLine(
                color = PureWhite.copy(alpha = style.cardTopSheenAlpha),
                start = Offset(insetX, sheenY),
                end = Offset((size.width - insetX).coerceAtLeast(insetX), sheenY),
                strokeWidth = borderPx
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = borderPx,
                color = borderColor
            )
        }
}

/**
 * Accordion / section header banner matching board SH16 + gradient + 1dp border + top white highlight.
 */
fun Modifier.depthSectionBanner(
    gradientColors: List<Color>,
    borderColor: Color,
    cornerRadius: Dp = Dimens.RadiusReportInput
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = DepthTokens.SECTION_PILL_SHADOW_ALPHA),
                offsetY = Dimens.SectionPillShadowOffsetY,
                blurRadius = Dimens.SectionPillShadowBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            val borderPx = style.cardBorderWidth.toPx()
            val sheenY = borderPx + borderPx * 0.5f
            val insetX = borderPx
            drawLine(
                color = PureWhite.copy(alpha = DepthTokens.SECTION_PILL_HIGHLIGHT_ALPHA),
                start = Offset(insetX, sheenY),
                end = Offset((size.width - insetX).coerceAtLeast(insetX), sheenY),
                strokeWidth = borderPx
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = borderPx,
                color = borderColor
            )
        }
}

/**
 * Small raised 32dp / 38dp icon tile inside section headers & KPI cards matching board SH17.
 */
fun Modifier.depthRaisedTile(
    gradientColors: List<Color>,
    cornerRadius: Dp = Dimens.RadiusSectionIconTile
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = DepthTokens.SECTION_ICON_SHADOW_ALPHA),
                offsetY = Dimens.SectionTileShadowOffsetY,
                blurRadius = Dimens.SectionTileShadowBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(colors = gradientColors)
            )
            val highlightPx = 1.dp.toPx()
            drawLine(
                color = PureWhite,
                start = Offset(0f, highlightPx * 0.5f),
                end = Offset(size.width, highlightPx * 0.5f),
                strokeWidth = highlightPx
            )
        }
}

/**
 * Active thumb for segmented period selector matching board GR3 + SH19 (0 3dp 8dp #1D4FD8@0.35).
 */
fun Modifier.depthSegmentActiveThumb(
    cornerRadius: Dp = Dimens.RadiusSegmentThumb
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.buttonShadowColor.copy(alpha = DepthTokens.SEGMENT_ACTIVE_SHADOW_ALPHA),
                offsetY = Dimens.SegmentActiveShadowOffsetY,
                blurRadius = Dimens.SegmentActiveShadowBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                brush = Brush.verticalGradient(
                    colors = listOf(style.buttonGradientStart, style.buttonGradientEnd)
                )
            )
            val highlightPx = 1.dp.toPx()
            drawLine(
                color = PureWhite.copy(alpha = style.buttonTopHighlightAlpha),
                start = Offset(0f, highlightPx * 0.5f),
                end = Offset(size.width, highlightPx * 0.5f),
                strokeWidth = highlightPx
            )
        }
}

/**
 * Raised 145-degree diagonal gradient icon tile matching board SH1:
 * 0 4dp 8dp rgba(16, 24, 40, 0.14) drop shadow, 145-deg diagonal gradient fill,
 * 1dp inner top white highlight (0.90), and 2dp inner bottom bevel edge (0.07).
 */
fun Modifier.depthRaisedIconTile(
    gradientColors: List<Color>,
    cornerRadius: Dp = Dimens.RadiusSettingsIconTile
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = DepthTokens.ICON_TILE_SHADOW_ALPHA),
                offsetY = Dimens.IconTileShadowOffsetY,
                blurRadius = Dimens.IconTileShadowBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            // 145-degree CSS linear gradient vector
            val dx = size.width * 0.287f
            val dy = size.height * 0.410f
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            drawRect(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(cx - dx, cy - dy),
                    end = Offset(cx + dx, cy + dy)
                ),
                size = size
            )
            val topPx = 1.dp.toPx()
            drawLine(
                color = PureWhite.copy(alpha = DepthTokens.ICON_TILE_TOP_HIGHLIGHT_ALPHA),
                start = Offset(0f, topPx * 0.5f),
                end = Offset(size.width, topPx * 0.5f),
                strokeWidth = topPx
            )
            val bevelPx = Dimens.IconTileBottomBevelHeight.toPx()
            drawLine(
                color = style.cardShadowCloseColor.copy(alpha = DepthTokens.ICON_TILE_BOTTOM_BEVEL_ALPHA),
                start = Offset(0f, size.height - bevelPx * 0.5f),
                end = Offset(size.width, size.height - bevelPx * 0.5f),
                strokeWidth = bevelPx
            )
        }
}


