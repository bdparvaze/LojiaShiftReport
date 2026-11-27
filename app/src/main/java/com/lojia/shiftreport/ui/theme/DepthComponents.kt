package com.lojia.shiftreport.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
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
 * and inner fills never wash out or overwrite the border color.
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
        val shrink = { cr: androidx.compose.ui.geometry.CornerRadius ->
            androidx.compose.ui.geometry.CornerRadius(
                (cr.x - half).coerceAtLeast(0f),
                (cr.y - half).coerceAtLeast(0f)
            )
        }
        val insetRr = androidx.compose.ui.geometry.RoundRect(
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
            outline = androidx.compose.ui.graphics.Outline.Rounded(insetRr),
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
 * Clean flat card container styling driven strictly by [LocalDepthStyle]:
 * solid surface fill, 1dp border, and single soft shadow (0/1dp blur 4dp black @ 6%).
 */
fun Modifier.depthCard(
    accent: Boolean = false,
    shape: Shape? = null,
    cornerRadius: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedRadius = cornerRadius ?: style.cardCornerRadius
    val resolvedShape = shape ?: RoundedCornerShape(resolvedRadius)
    val fillColor = if (accent) {
        style.cardAccentFillColor
    } else {
        style.cardFillColor
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
        }
        .clip(resolvedShape)
        .drawBehind {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = fillColor
            )
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * Flat semantic card preserving an explicit semantic solid tint, 1dp border, and soft card shadow.
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
        }
        .clip(resolvedShape)
        .drawBehind {
            val outline = resolvedShape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, color = backgroundColor)
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * Flat input field container styling with crisp 1dp / 1.5dp border and solid surface fill.
 */
@Suppress("UNUSED_PARAMETER")
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
    topShadowAlphaOverride: Float? = null,
    disabledBackgroundOverride: Color? = null,
    insetShadowOffsetYOverride: Dp? = null,
    insetShadowBlurOverride: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedRadius = cornerRadius ?: style.insetCornerRadius
    val resolvedShape = shape ?: RoundedCornerShape(resolvedRadius)
    val resolvedDisabledBg = disabledBackgroundOverride ?: style.insetDisabledBackground
    val resolvedFill = when {
        !enabled -> resolvedDisabledBg
        isError -> style.insetErrorBackground
        gradientColors != null -> gradientColors.lastOrNull() ?: style.insetBackground
        isFocused -> style.insetFocusedBackground
        else -> style.insetBackground
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
            drawOutline(
                outline = outline,
                color = resolvedFill
            )
            drawInsetShapeBorder(
                shape = resolvedShape,
                borderWidthPx = resolvedBorderWidth.toPx(),
                color = resolvedBorder
            )
        }
}

/**
 * Flat primary action button styling with solid fill, 8% darkened pressed fill,
 * soft card-matching shadow, and >= 48dp minimum touch targets.
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
    var isPressed by remember { mutableStateOf(false) }

    val baseFill = when {
        !enabled -> style.buttonDisabledEnd
        isDestructive -> style.destructiveButtonFillColor
        else -> style.buttonFillColor
    }
    val pressedFill = when {
        !enabled -> style.buttonDisabledEnd
        isDestructive -> style.destructiveButtonPressedFillColor
        else -> style.buttonPressedFillColor
    }
    val resolvedFill = if (enabled && isPressed) pressedFill else baseFill

    this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = minHeight.coerceAtLeast(Dimens.MinTouchTarget)
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
        .drawBehind {
            if (enabled) {
                drawSoftDropShadow(
                    color = style.buttonShadowColor.copy(alpha = style.buttonShadowAlpha),
                    offsetY = style.buttonShadowOffsetY,
                    blurRadius = style.buttonShadowBlur,
                    cornerRadius = resolvedCorner
                )
            }
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = resolvedFill
            )
        }
}

/**
 * Flat secondary/outlined button styling with white fill, 1dp border, 6% pressed tint fill,
 * no drop shadow, and >= 48dp touch target preservation.
 */
@Suppress("UNUSED_PARAMETER")
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
    minHeight: Dp = Dimens.ButtonHeightPrimary,
    enforceMinTouchTarget: Boolean = true
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = cornerRadius ?: style.buttonCornerRadius
    val shape = RoundedCornerShape(resolvedCorner)
    val resolvedBorderColor = borderColor ?: style.secondaryButtonBorderColor
    val resolvedBorderWidth = borderWidth ?: style.secondaryButtonBorderWidth
    var isPressed by remember { mutableStateOf(false) }

    val baseFill = if (enabled) {
        gradientColors?.firstOrNull() ?: style.secondaryButtonGradientStart
    } else {
        style.secondaryButtonDisabledStart
    }
    val pressedTintSource = if (borderColor != null && borderColor.alpha > 0f) {
        PrimaryBlueLight
    } else {
        style.secondaryButtonBorderColor
    }
    val resolvedFill = if (enabled && isPressed) {
        lerp(baseFill, pressedTintSource, DepthTokens.FLAT_SECONDARY_PRESSED_ALPHA)
    } else {
        baseFill
    }

    val resolvedMinWidth = if (enforceMinTouchTarget) Dimens.MinTouchTarget else Dp.Unspecified
    val resolvedMinHeight = if (enforceMinTouchTarget) minHeight.coerceAtLeast(Dimens.MinTouchTarget) else minHeight

    this
        .defaultMinSize(
            minWidth = resolvedMinWidth,
            minHeight = resolvedMinHeight
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = resolvedFill
            )
            if (resolvedBorderWidth > 0.dp) {
                drawInsetShapeBorder(
                    shape = shape,
                    borderWidthPx = resolvedBorderWidth.toPx(),
                    color = resolvedBorderColor
                )
            }
        }
}

/**
 * Flat keypad key styling for the Quick PIN screen with solid white fill, 1dp border,
 * and subtle card shadow.
 */
fun Modifier.depthKeypadKey(
    enabled: Boolean = true,
    cornerRadius: Dp? = null
): Modifier = composed {
    val style = LocalDepthStyle.current
    val resolvedCorner = cornerRadius ?: style.keypadCornerRadius
    val shape = RoundedCornerShape(resolvedCorner)
    var isPressed by remember { mutableStateOf(false) }

    val baseFill = if (enabled) style.keypadGradientStart else style.secondaryButtonDisabledStart
    val resolvedFill = if (enabled && isPressed) {
        lerp(baseFill, PrimaryBlueLight, DepthTokens.FLAT_SECONDARY_PRESSED_ALPHA)
    } else {
        baseFill
    }

    this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = style.keypadHeight
        )
        .trackButtonPress(enabled = enabled) { isPressed = it }
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
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = resolvedFill
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = style.cardBorderColor
            )
        }
}

/**
 * Standalone 48dp flat app logo tile with solid fill, 1dp border, and subtle card shadow.
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
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = style.logoTileGradientStart
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = style.cardBorderColor
            )
        }
}

/**
 * Top solid primary-blue header container with RTL-safe padding and bottom-start/end corners.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LojiaGradientHeader(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomStart = Dimens.RadiusXl, bottomEnd = Dimens.RadiusXl),
    cornerRadiusForShadow: Dp = Dimens.SpacingNone,
    showDropShadow: Boolean = false,
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
            .clip(shape)
            .background(color = style.headerFillColor)
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
 * One-time subtle alpha fade-in entrance staggered by [index], honoring reduced motion.
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
        alpha = progress.value
    }
}

/**
 * Static flat LIVE indicator modifier (glow pulse animation removed in Stage 29A).
 */
fun Modifier.livePulse(): Modifier = this

/**
 * Compact flat circular LIVE indicator dot using [DepthStyle.liveDotColor].
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
 * Flat tab bar container modifier (shadow removed in Stage 29A).
 */
fun Modifier.depthTabBarShadow(): Modifier = this

/**
 * Plain solid tab indicator modifier (glow removed in Stage 29A).
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.depthTabIndicatorGlow(cornerRadius: Dp = Dimens.TabIndicatorHeight): Modifier = this

/**
 * Flat tinted summary/KPI card with solid background, 1dp border, and subtle soft shadow.
 */
fun Modifier.depthGradientCard(
    gradientColors: List<Color>,
    borderColor: Color,
    cornerRadius: Dp = Dimens.RadiusKpiCard
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = gradientColors.lastOrNull() ?: style.cardFillColor
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = style.cardAmbientShadowAlpha),
                offsetY = style.cardShadowCloseOffsetY,
                blurRadius = style.cardShadowCloseBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = fillColor
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * Flat section accordion banner with solid fill, 1dp border, and subtle soft shadow.
 */
fun Modifier.depthSectionBanner(
    gradientColors: List<Color>,
    borderColor: Color,
    cornerRadius: Dp = Dimens.RadiusReportInput
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = gradientColors.lastOrNull() ?: style.cardFillColor
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
                color = fillColor
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * Flat compact icon tile inside section headers & KPI cards with solid fill, 1dp border, and soft shadow.
 */
fun Modifier.depthRaisedTile(
    gradientColors: List<Color>,
    cornerRadius: Dp = Dimens.RadiusSectionIconTile
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = gradientColors.lastOrNull() ?: style.cardFillColor
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
                color = fillColor
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = style.cardBorderColor
            )
        }
}

/**
 * Flat active thumb for segmented period selector (solid primary blue, no glow).
 */
fun Modifier.depthSegmentActiveThumb(
    cornerRadius: Dp = Dimens.RadiusSegmentThumb
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .clip(shape)
        .background(color = style.buttonFillColor, shape = shape)
}

/**
 * Flat icon tile for Settings, Quick Actions, and List rows:
 * solid surface fill, 1dp border, and subtle soft shadow.
 */
fun Modifier.depthRaisedIconTile(
    gradientColors: List<Color>,
    cornerRadius: Dp = Dimens.RadiusSettingsIconTile
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillColor = gradientColors.lastOrNull() ?: style.cardFillColor
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
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = fillColor
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = style.cardBorderWidth.toPx(),
                color = style.cardBorderColor
            )
        }
}

/**
 * Flat note/privacy block with solid fill and 1dp border.
 */
fun Modifier.depthNoteBox(
    cornerRadius: Dp = Dimens.RadiusInputRegister,
    fillColor: Color = DepthTokens.RegisterNoteBackground,
    borderColor: Color = DepthTokens.RegisterNoteBorderColor
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, color = fillColor)
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = Dimens.CardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * Flat tinted info block with solid fill and 1dp border.
 */
fun Modifier.depthInfoBlock(
    cornerRadius: Dp = Dimens.RadiusInputRegister,
    fillColor: Color = DepthTokens.RecoveryInfoBackground,
    borderColor: Color = DepthTokens.RecoveryInfoBorderColor
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline = outline, color = fillColor)
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = Dimens.CardBorderWidth.toPx(),
                color = borderColor
            )
        }
}

/**
 * 46x46dp flat icon tile for password recovery info block.
 */
fun Modifier.depthRecoveryTile(): Modifier = this
    .size(Dimens.RecoveryTileSize)
    .depthRaisedIconTile(
        gradientColors = listOf(DepthTokens.RecoveryTileGradientStart, DepthTokens.RecoveryTileGradientEnd),
        cornerRadius = Dimens.RadiusRecoveryTile
    )

/**
 * Composable helper returning true when animations are allowed (not in inspection and system animator scale > 0).
 */
@Composable
fun rememberShouldAnimate(): Boolean {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val animatorScale = remember(context) { readAnimatorDurationScale(context) }
    return !isInspection && shouldAnimate(animatorScale)
}

/**
 * Flat emerald status badge pill with solid fill and 1dp border (no glow shadow).
 */
fun Modifier.depthLiveBadge(): Modifier = composed {
    val shape = RoundedCornerShape(Dimens.RadiusFull)
    this
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = DepthTokens.LiveBadgeBgStart
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = Dimens.CardBorderWidth.toPx(),
                color = DepthTokens.LiveBadgeBorder
            )
        }
}

/**
 * Flat amber draft/review alert banner with solid fill, 1dp border, and subtle soft shadow.
 */
fun Modifier.depthDraftBanner(
    cornerRadius: Dp = Dimens.RadiusKpiCard
): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(cornerRadius)
    this
        .drawBehind {
            drawSoftDropShadow(
                color = style.cardShadowCloseColor.copy(alpha = DepthTokens.DRAFT_BANNER_SHADOW_ALPHA),
                offsetY = Dimens.HomeDraftBannerShadowOffsetY,
                blurRadius = Dimens.HomeDraftBannerShadowBlur,
                cornerRadius = cornerRadius
            )
        }
        .clip(shape)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(
                outline = outline,
                color = SectionAmberGradientEnd
            )
            drawInsetShapeBorder(
                shape = shape,
                borderWidthPx = Dimens.CardBorderWidth.toPx(),
                color = SectionAmberBorder
            )
        }
}

/**
 * Flat chart today-bar modifier (glow removed in Stage 29A).
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.depthChartTodayBarGlow(
    cornerRadius: Dp = Dimens.HomeChartBarRadius
): Modifier = this
