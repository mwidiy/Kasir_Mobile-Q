package com.example.kasir.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedLogo(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {}
) {
    // --- ANIMATION STATES ---
    // 1. Line Drawing Progress (0f -> 1f)
    val strokeProgress = remember { Animatable(0f) }
    
    // 2. Fill Opacity (Fade In after lines are mostly done)
    val fillAlpha = remember { Animatable(0f) }
    
    // 3. Gold Accents Scale (Pop at the end)
    val accentScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: Draw the Blueprint (Stroke)
        strokeProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        
        // Phase 2: Solidify (Fill) - Starts slightly before stroke finishes
        // We handle this concurrently for visual smoothness, but sequentially logically
    }

    LaunchedEffect(Unit) {
        delay(1200) // Start filling when stroke is ~80% done
        fillAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    LaunchedEffect(Unit) {
        delay(1800) // Pop accents after fill is solid
        accentScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(500) // Wait a bit after animation finishes
        onAnimationFinished()
    }

    // --- PATH DATA ---
    val bodyPathString = "M267.1 402.5 l-23.3 -23.5 -115.6 0 -115.7 0 0 -182 0 -182 122.3 0 122.3 0 -0.1 58.3 -0.1 58.2 34.1 0.5 34.1 0.5 -0.1 94.9 c0 52.3 -0.2 95.1 -0.3 95.3 -0.2 0.1 -19.8 -19.1 -43.5 -42.7 l-43.2 -43.1 0 -67.9 0 -68 -69.5 0.2 -69.6 0.3 0.1 96.5 c0 53 0.4 96.8 0.9 97.3 0.4 0.4 5.7 0.6 11.7 0.5 l10.9 -0.3 0.3 -32.7 0.2 -32.7 40 0.3 40 0.3 2.6 2.4 c1.5 1.3 4.4 4.2 6.6 6.4 2.2 2.2 28.8 28.9 59.1 59.4 l55.1 55.4 22.2 -0.6 22.2 -0.6 0 36.5 0 36.4 -40.2 0 -40.3 0 -23.2 -23.5z"
    val accentPaths = listOf(
        "M370.4 411.5 c0 -8.2 0.2 -11.5 0.3 -7.2 0.2 4.3 0.2 11 0 15 -0.1 4 -0.3 0.5 -0.3 -7.8z",
        "M370.4 369 c0 -9.1 0.2 -12.8 0.3 -8.2 0.2 4.5 0.2 11.9 0 16.5 -0.1 4.5 -0.3 0.8 -0.3 -8.3z",
        "M69.8 378.3 c32.1 -0.2 84.3 -0.2 116 0 31.7 0.1 5.5 0.2 -58.3 0.2 -63.8 0 -89.8 -0.1 -57.7 -0.2z",
        "M98.4 122 c0 -9.1 0.2 -12.8 0.3 -8.2 0.2 4.5 0.2 11.9 0 16.5 -0.1 4.5 -0.3 0.8 -0.3 -8.3z",
        "M277.7 110.3 c-0.3 -0.5 -0.6 -20.6 -0.7 -44.8 -0.1 -24.2 -0.3 -45.3 -0.4 -47 l-0.1 -3 47.3 -0.3 47.2 -0.2 -0.2 47.7 -0.3 47.8 -46.2 0.3 c-25.4 0.1 -46.4 -0.1 -46.6 -0.5z"
    )

    // Pre-calculate paths
    val bodyPath = remember { PathParser().parsePathString(bodyPathString).toPath() }
    val pathMeasure = remember { PathMeasure() }
    
    // Remember parsed accent paths
    val goldPaths = remember { accentPaths.map { PathParser().parsePathString(it).toPath() } }

    Canvas(modifier = modifier) {
        val originalWidth = 382f
        val originalHeight = 436f
        
        val scaleX = size.width / originalWidth
        val scaleY = size.height / originalHeight
        val scale = minOf(scaleX, scaleY)

        val translateX = (size.width - (originalWidth * scale)) / 2
        val translateY = (size.height - (originalHeight * scale)) / 2

        withTransform({
            translate(left = translateX, top = translateY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // --- DRAW BODY (BLUE) ---
            pathMeasure.setPath(bodyPath, false)
            val totalLength = pathMeasure.length
            
            // 1. Draw Stroke (Blueprint Effect)
            if (strokeProgress.value < 1f || fillAlpha.value < 1f) {
                val segmentPath = Path()
                pathMeasure.getSegment(0f, totalLength * strokeProgress.value, segmentPath, true)
                drawPath(
                    path = segmentPath,
                    color = Color(0xFF1C355A),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 2. Draw Fill (Fade In)
            if (fillAlpha.value > 0f) {
                drawPath(
                    path = bodyPath,
                    color = Color(0xFF1C355A),
                    alpha = fillAlpha.value,
                    style = Fill
                )
            }

            // --- DRAW ACCENTS (GOLD) ---
            // These pop with a scale animation from the approximate center
            if (accentScale.value > 0f) {
                withTransform({
                    scale(
                        scaleX = accentScale.value,
                        scaleY = accentScale.value,
                        pivot = Offset(originalWidth / 2, originalHeight / 2)
                    )
                }) {
                    goldPaths.forEach { p ->
                        drawPath(p, color = Color(0xFFF0DB88), style = Fill)
                    }
                }
            }
        }
    }
}
