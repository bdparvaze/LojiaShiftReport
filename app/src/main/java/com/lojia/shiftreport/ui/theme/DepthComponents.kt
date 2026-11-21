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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
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
 * Raised soft-depth card container styling driven strictly by [LocalDepthStyle].
 */
fun Modifier.depthCard(accent: Boolean = false): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(style.cardCornerRadius)
    val elevation = if (accent) style.cardAccentElevation else style.cardElevation
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
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = style.cardShadowColor.copy(alpha = style.cardAmbientShadowAlpha),
            spotColor = style.cardShadowColor.copy(alpha = style.cardSpotShadowAlpha)
        )
        .clip(shape)
        .background(brush = Brush.verticalGradient(colors = gradientColors))
        .border(width = style.cardBorderWidth, color = borderColor, shape = shape)
        .drawWithContent {
            drawContent()
            drawLine(
                color = PureWhite.copy(alpha = style.cardTopSheenAlpha),
                start = Offset(0f, 0.5f),
                end = Offset(size.width, 0.5f),
                strokeWidth = style.cardBorderWidth.toPx()
            )
        }
}

/**
 * Pressed-in inset surface styling for input fields and recessed wells.
 */
fun Modifier.depthInset(): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(style.insetCornerRadius)
    val borderColor = style.insetBorderColor.copy(alpha = style.insetBorderAlpha)

    this
        .clip(shape)
        .background(color = style.insetBackground)
        .drawWithContent {
            drawContent()
            val shadowPx = style.insetShadowHeight.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        style.insetShadowColor.copy(alpha = style.insetTopShadowAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = shadowPx
                ),
                size = Size(size.width, shadowPx)
            )
            val strokePx = style.insetBorderWidth.toPx()
            drawLine(
                color = style.insetShadowColor.copy(alpha = style.insetSideShadowAlpha),
                start = Offset(0f, strokePx),
                end = Offset(size.width, strokePx),
                strokeWidth = strokePx
            )
            drawLine(
                color = style.insetBottomHighlightColor.copy(alpha = style.insetBottomHighlightAlpha),
                start = Offset(0f, size.height - strokePx),
                end = Offset(size.width, size.height - strokePx),
                strokeWidth = strokePx
            )
        }
        .border(width = style.insetBorderWidth, color = borderColor, shape = shape)
}

/**
 * Raised 3D button styling with brand gradient face and lower lip, preserving minimum touch targets.
 */
fun Modifier.depthRaisedButton(enabled: Boolean = true): Modifier = composed {
    val style = LocalDepthStyle.current
    val shape = RoundedCornerShape(style.buttonCornerRadius)
    val base = this
        .defaultMinSize(
            minWidth = Dimens.MinTouchTarget,
            minHeight = Dimens.MinTouchTarget
        )

    val elevated = if (enabled) {
        base.shadow(
            elevation = style.buttonElevation,
            shape = shape,
            clip = false,
            ambientColor = style.buttonShadowColor.copy(alpha = style.buttonShadowAlpha),
            spotColor = style.buttonShadowColor.copy(alpha = style.buttonShadowAlpha)
        )
    } else {
        base
    }

    elevated
        .clip(shape)
        .drawBehind {
            val lipPx = style.buttonLipHeight.toPx().coerceAtMost(size.height * 0.25f)
            val cornerPx = style.buttonCornerRadius.toPx()
            val cornerRadius = CornerRadius(cornerPx, cornerPx)

            val lipColor = if (enabled) {
                style.buttonLipColor.copy(alpha = style.buttonLipAlpha)
            } else {
                style.buttonLipDisabledColor.copy(alpha = style.buttonLipDisabledAlpha)
            }
            drawRoundRect(
                color = lipColor,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = cornerRadius
            )

            val faceHeight = (size.height - lipPx).coerceAtLeast(1f)
            val faceColors = if (enabled) {
                listOf(style.buttonGradientStart, style.buttonGradientEnd)
            } else {
                listOf(style.buttonDisabledStart, style.buttonDisabledEnd)
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = faceColors,
                    startY = 0f,
                    endY = faceHeight
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, faceHeight),
                cornerRadius = cornerRadius
            )
        }
        .padding(bottom = style.buttonLipHeight)
}

/**
 * Top brand gradient container with RTL-safe padding and bottom-start/end corners.
 */
@Composable
fun LojiaGradientHeader(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomStart = Dimens.RadiusXl, bottomEnd = Dimens.RadiusXl),
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
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(style.headerGradientStart, style.headerGradientEnd)
                )
            )
            .drawBehind {
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
                easing = FastOutSlowInEasing
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
