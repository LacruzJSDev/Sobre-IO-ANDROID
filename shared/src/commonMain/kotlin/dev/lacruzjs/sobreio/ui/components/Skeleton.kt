package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * First load with nothing to show yet: soft shapes where the content will be, gently pulsing, instead of a spinner on a
 * blank page. Later loads never show these: the screen keeps its figures and the [RefreshBar] says it is updating.
 */

private val LocalSkeletonAlpha = compositionLocalOf { 1f }

/** One placeholder shape in the soft container colour. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    Box(
        modifier
            .alpha(LocalSkeletonAlpha.current)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape),
    )
}

/** Hides [content] from accessibility (it is not real content) and makes every [SkeletonBlock] in it pulse together. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(modifier.clearAndSetSemantics {}) {
        CompositionLocalProvider(LocalSkeletonAlpha provides alpha) { content() }
    }
}

/** A list row: avatar, two lines and an amount on the right. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier, avatarSize: Dp = 40.dp) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(Modifier.size(avatarSize), MaterialTheme.shapes.medium)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.55f).height(14.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.35f).height(10.dp))
        }
        SkeletonBlock(Modifier.width(64.dp).height(14.dp))
    }
}

/** The usual tab or list page: an optional hero card and [rows] rows. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, withHero: Boolean = true, rows: Int = 6) {
    Skeleton(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (withHero) {
                SkeletonBlock(Modifier.fillMaxWidth().height(HeroCardHeight), MaterialTheme.shapes.large)
                Spacer(Modifier.height(24.dp))
            }
            SkeletonBlock(Modifier.width(120.dp).height(12.dp))
            Spacer(Modifier.height(8.dp))
            repeat(rows) { SkeletonRow() }
        }
    }
}

/** A detail page: the main figure, a chart and a few rows. */
@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    Skeleton(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.width(96.dp).height(12.dp))
            SkeletonBlock(Modifier.width(180.dp).height(32.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.5f).height(12.dp))
            Spacer(Modifier.height(4.dp))
            SkeletonBlock(Modifier.fillMaxWidth().height(160.dp), MaterialTheme.shapes.large)
            Spacer(Modifier.height(4.dp))
            repeat(4) { SkeletonRow() }
        }
    }
}

/** A form: the avatar and name, then tiles. */
@Composable
fun FormSkeleton(modifier: Modifier = Modifier) {
    Skeleton(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(Modifier.size(56.dp), MaterialTheme.shapes.medium)
                SkeletonBlock(Modifier.weight(1f).height(48.dp), MaterialTheme.shapes.medium)
            }
            Spacer(Modifier.height(8.dp))
            repeat(5) { SkeletonBlock(Modifier.fillMaxWidth().height(56.dp), MaterialTheme.shapes.medium) }
        }
    }
}
