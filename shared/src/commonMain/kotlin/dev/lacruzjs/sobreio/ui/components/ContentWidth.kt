package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Widths that content is held to on wide panes. They are decided by the pane's own width (the constraints the modifier
 * receives), not by the window, so the same screen behaves the same alone or beside another one.
 */
object ContentWidth {
    /** Forms, pickers and the sign-in family. */
    val Form: Dp = 640.dp

    /** Lists and single-column detail content. */
    val List: Dp = 720.dp

    /** Two-column layouts (Group tab, account and envelope details). */
    val TwoColumns: Dp = 1120.dp

    /**
     * A pane at least this wide lays its content out in two columns: each column then gets about a phone's content width
     * (the hero card and charts are designed for that). An open foldable with the list alone qualifies; a pane beside a
     * detail does not.
     */
    val TwoColumnsMinPane: Dp = 720.dp
}

/**
 * Holds the content to at most [max] and centres it horizontally; the background behind it (the screen's `Surface`)
 * keeps the full width. Below [max] it simply fills the width. Apply it once on a screen's root column, not per item.
 */
fun Modifier.contentMaxWidth(max: Dp = ContentWidth.List): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = max)
        .fillMaxWidth()
