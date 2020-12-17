package com.klitsie.progressbutton.ui


import android.annotation.SuppressLint
import androidx.compose.animation.animatedFloat
import androidx.compose.animation.core.*
import androidx.compose.animation.transition
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val IterationProp = IntPropKey()

private val BaseRotationProp = FloatPropKey()

// How far forward (degrees) both the head and tail should be from the base point
private val HeadRotationProp = FloatPropKey()
private val TailRotationProp = FloatPropKey()
private const val RotationsPerCycle = 5
private const val BaseRotationAngle = 286f
private const val JumpRotationAngle = 290f
private const val RotationDuration = 2000
private const val StartAngleOffset = -90f

private const val RotationAngleOffset = (BaseRotationAngle + JumpRotationAngle) % 360f
private const val HeadAndTailAnimationDuration = (RotationDuration * 0.5).toInt()
private const val HeadAndTailDelayDuration = HeadAndTailAnimationDuration

private val CircularEasing = CubicBezierEasing(0.2f, 0f, 0.8f, 1f)

@SuppressLint("Range")
private val CircularIndeterminateTransition = transitionDefinition<Int> {
    state(0) {
        this[IterationProp] = 0
        this[BaseRotationProp] = 0f
        this[HeadRotationProp] = 0f
        this[TailRotationProp] = 0f
    }

    state(1) {
        this[IterationProp] = RotationsPerCycle
        this[BaseRotationProp] = BaseRotationAngle
        this[HeadRotationProp] = JumpRotationAngle
        this[TailRotationProp] = JumpRotationAngle
    }

    transition(fromState = 0, toState = 1) {
        IterationProp using infiniteRepeatable(
            animation = tween(
                durationMillis = RotationDuration * RotationsPerCycle,
                easing = LinearEasing
            )
        )
        BaseRotationProp using infiniteRepeatable(
            animation = tween(
                durationMillis = RotationDuration,
                easing = LinearEasing
            )
        )
        HeadRotationProp using infiniteRepeatable(
            animation = keyframes {
                durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at 0 with CircularEasing
                JumpRotationAngle at HeadAndTailAnimationDuration
            }
        )
        TailRotationProp using infiniteRepeatable(
            animation = keyframes {
                durationMillis = HeadAndTailAnimationDuration + HeadAndTailDelayDuration
                0f at HeadAndTailDelayDuration with CircularEasing
                JumpRotationAngle at durationMillis
            }
        )
    }
}

fun Modifier.countDownBorder(
    duration: Int,
    shape: Shape,
    isVisible: Boolean,
    forwards: Boolean = true,
    onEnd: () -> Unit
): Modifier = composed {
    val progress = animatedFloat(initVal = if (forwards) 0f else 1f)
    val alpha = animatedFloat(initVal = if (isVisible) 1f else 0f)
    onCommit(isVisible) {
        alpha.animateTo(if (isVisible) 1f else 0f)
        if (isVisible) {
            progress.snapTo(if (forwards) 0f else 1f)
            progress.animateTo(
                if (forwards) 1f else 0f,
                TweenSpec(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            ) { reason, _ ->
                if (reason == AnimationEndReason.TargetReached) {
                    onEnd()
                }
            }
        } else {
            progress.stop()
        }
    }
    ProgressBorderModifier(
        alpha.value,
        0f,
        progress.value,
        remember { DrawBorderCache() },
        shape,
        2.dp,
        SolidColor(
            MaterialTheme.colors.primary
        )
    )
}

fun Modifier.progressBorder(isVisible: Boolean, shape: Shape): Modifier = composed {
    val state = transition(
        definition = CircularIndeterminateTransition,
        initState = 0,
        toState = 1
    )
    val currentRotation = state[IterationProp]
    val baseRotation = state[BaseRotationProp]
    val currentRotationAngleOffset = (currentRotation * RotationAngleOffset) % 360f
    var startAngle = state[TailRotationProp]
    val endAngle = state[HeadRotationProp]

    val sweep = abs(endAngle - startAngle)
    val adjustedSweep = kotlin.math.max(sweep, 10f) / 360f
    // Offset by the constant offset and the per rotation offset
    startAngle += StartAngleOffset + currentRotationAngleOffset
    startAngle += baseRotation

    val fade = animatedProgress(
        targetValue = 1f.takeIf { isVisible } ?: 0f,
        animation = TweenSpec()
    )

    val adjustment = (startAngle.toInt() / 360).let {
        if (startAngle < 0) {
            it - 1
        } else {
            it
        }
    }
    val adjustedStartAngle = (startAngle / 360f) - adjustment
    val adjustedEndAngle = adjustedStartAngle + adjustedSweep

    ProgressBorderModifier(
        fade.value,
        adjustedStartAngle,
        adjustedEndAngle,
        remember { DrawBorderCache() },
        shape,
        2.dp,
        SolidColor(
            MaterialTheme.colors.primary
        )
    )
}

private class ProgressBorderModifier(
    private val alpha: Float = 1f,
    startAngle: Float,
    endAngle: Float,
    private val outlineCache: DrawBorderCache,
    shape: Shape,
    private val borderWidth: Dp,
    private val brush: Brush
) : DrawModifier {

    init {
        outlineCache.sectionProgress = startAngle to endAngle
        outlineCache.lastShape = shape
        outlineCache.borderSize = borderWidth
    }

    override fun ContentDrawScope.draw() {
        val density = this
        with(outlineCache) {
            drawContent()
            if (alpha == 0f) {
                return
            }
            modifierSize = size
            val borderSize =
                if (borderWidth == Dp.Hairline) 1f else borderWidth.value * density.density
            if (borderSize <= 0 || size.minDimension <= 0.0f) {
                return
            }
            drawPath(
                borderPath(density, borderSize),
                brush,
                alpha = alpha,
                style = Stroke(borderWidth.toPx())
            )
        }
    }
}

private class DrawBorderCache {
    private val shapePath = Path()
    private val sectionPath = Path()
    private var dirtyShape = true
    private var dirtySection = true
    private var dirtyOutline = true
    private var outline: Outline? = null

    private val shapeMeasure = PathMeasure()

    init {
        shapeMeasure.setPath(shapePath, false)
    }

    var sectionProgress: Pair<Float, Float> = 0f to 0f
        set(value) {
            if (value != field) {
                field = value
                dirtySection = true
            }
        }

    var lastShape: Shape? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyShape = true
                dirtySection = true
                dirtyOutline = true
            }
        }

    var borderSize: Dp = Dp.Unspecified
        set(value) {
            if (value != field) {
                field = value
                dirtySection = true
                dirtyShape = true
            }
        }

    var modifierSize: Size? = null
        set(value) {
            if (value != field) {
                field = value
                dirtyShape = true
                dirtySection = true
                dirtyOutline = true
            }
        }

    private fun modifierSizeOutline(density: Density): Outline {
        if (dirtyOutline) {
            outline = lastShape?.createOutline(modifierSize!!, density)
            dirtyOutline = false
        }
        return outline!!
    }

    fun borderPath(density: Density, borderPixelSize: Float): Path {
        if (dirtyShape) {
            val size = modifierSize!!
            shapePath.reset()
            if (borderPixelSize * 2 >= size.minDimension) {
                shapePath.addOutline(modifierSizeOutline(density))
            } else {
                val sizeMinusHalfBorder =
                    Size(
                        size.width - borderPixelSize,
                        size.height - borderPixelSize
                    )
                val halfBorderSize = borderPixelSize / 2f
                shapePath.addOutline(lastShape!!.createOutline(sizeMinusHalfBorder, density))
                shapePath.translate(Offset(halfBorderSize, halfBorderSize))
                shapeMeasure.setPath(shapePath, false)
            }
            dirtyShape = false
        }
        if (dirtySection) {
            sectionPath.reset()
            val shapeLength = shapeMeasure.length
            val start = sectionProgress.first * shapeLength
            val end = sectionProgress.second * shapeLength
            shapeMeasure.getSegment(start, end, sectionPath, true)
            if (end > shapeLength) {
                shapeMeasure.getSegment(
                    start - shapeLength,
                    end - shapeLength,
                    sectionPath,
                    true
                )
            }
            dirtySection = false
        }
        return sectionPath
    }
}

@Composable
fun animatedProgress(
    targetValue: Float,
    animation: AnimationSpec<Float>,
    onAnimationFinish: () -> Unit = {}
): AnimatedFloat {
    val animatedFloat = animatedFloat(initVal = targetValue)
    onCommit(targetValue) {
        animatedFloat.animateTo(
            targetValue,
            anim = animation,
            onEnd = { reason, _ ->
                if (reason == AnimationEndReason.TargetReached) {
                    onAnimationFinish()
                }
            })
    }
    return animatedFloat
}
