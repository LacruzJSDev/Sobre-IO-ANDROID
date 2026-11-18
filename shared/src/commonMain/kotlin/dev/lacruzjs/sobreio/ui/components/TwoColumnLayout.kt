package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Wide panes (tablet, open foldable) show a screen in two columns, each with ITS OWN scroll (decided 2026-11-18):
 * the left one holds what sums the screen up (hero, key figures, short cards), the right one what is gone through
 * (lists, history, charts per period). A short left column simply stays put while the right one scrolls. Below
 * [ContentWidth.TwoColumnsMinPane] (phones, a pane beside a detail) the same pieces stack in one column, left first.
 * The width that decides is the pane's own, never the window's.
 */

/** Horizontal padding of each column: the outer edge gets [PageGutter], the gap between them twice [ColumnGap]. */
val PageGutter: Dp = 16.dp
private val ColumnGap: Dp = 8.dp

/** Content padding of the left column of a [TwoColumnSplit] (outer gutter on the left, half the gap on the right). */
val LeftColumnPadding = PaddingValues(start = PageGutter, end = ColumnGap, top = 8.dp, bottom = 16.dp)

/** Content padding of the right column of a [TwoColumnSplit]. */
val RightColumnPadding = PaddingValues(start = ColumnGap, end = PageGutter, top = 8.dp, bottom = 16.dp)

/** Content padding of the single column used on narrow panes. */
val SingleColumnPadding = PaddingValues(horizontal = PageGutter, vertical = 8.dp)

/**
 * Chooses between [narrow] (the whole screen in one column, its own scroll) and [left] beside [right] (each filling the
 * height and scrolling by itself, for example a `LazyColumn` with [RightColumnPadding]). Use it when a column is a lazy
 * list; for plain columns [TwoColumnLayout] does the scrolling.
 */
@Composable
fun TwoColumnSplit(
    narrow: @Composable () -> Unit,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= ContentWidth.TwoColumnsMinPane) {
            Row(modifier = Modifier.fillMaxSize().contentMaxWidth(ContentWidth.TwoColumns)) {
                Box(Modifier.weight(leftWeight).fillMaxHeight()) { left() }
                Box(Modifier.weight(rightWeight).fillMaxHeight()) { right() }
            }
        } else {
            narrow()
        }
    }
}

/**
 * Two plain columns, each in its own vertical scroll on a wide pane; one scrolling column ([left] then [right]) on a
 * narrow one. [top] goes above both, across the width (an error line), inside the scroll on narrow panes.
 */
@Composable
fun TwoColumnLayout(
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
    spacing: Dp = 16.dp,
    top: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val arrangement = Arrangement.spacedBy(spacing)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= ContentWidth.TwoColumnsMinPane) {
            Column(Modifier.fillMaxSize().contentMaxWidth(ContentWidth.TwoColumns)) {
                if (top != null) Column(Modifier.padding(horizontal = PageGutter), content = top)
                Row(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.weight(leftWeight).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(LeftColumnPadding),
                        verticalArrangement = arrangement,
                        content = left,
                    )
                    Column(
                        modifier = Modifier.weight(rightWeight).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(RightColumnPadding),
                        verticalArrangement = arrangement,
                        content = right,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).contentMaxWidth(ContentWidth.List)
                    .padding(SingleColumnPadding),
                verticalArrangement = arrangement,
            ) {
                top?.invoke(this)
                left()
                right()
            }
        }
    }
}
