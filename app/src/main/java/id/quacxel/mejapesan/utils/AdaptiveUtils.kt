package id.quacxel.mejapesan.utils

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the type of window based on screen width.
 * - Phone: < 600dp (standard phones)
 * - Tablet7: 600-719dp (7-inch tablets in portrait)
 * - Tablet10: >= 720dp (10-inch tablets in portrait)
 */
enum class WindowType {
    Phone, Tablet7, Tablet10
}

/**
 * Holds all adaptive/responsive values used throughout the app.
 * Each screen reads from this to make responsive decisions.
 */
data class AdaptiveValues(
    val windowType: WindowType = WindowType.Phone,

    /** Max width for main content areas (order lists, forms, etc.) */
    val contentMaxWidth: Dp = Dp.Unspecified,

    /** Number of columns for order/item grids */
    val gridColumns: Int = 1,

    /** Horizontal padding for screen-level containers */
    val horizontalPadding: Dp = 16.dp,

    /** Max width for standalone cards (Balance, Summary, etc.) */
    val cardMaxWidth: Dp = Dp.Unspecified,

    /** Max width for dialogs and modals */
    val dialogMaxWidth: Dp = Dp.Unspecified,

    /** Max width for the bottom navigation bar content */
    val bottomNavMaxWidth: Dp = Dp.Unspecified,

    /** Size of the QR scanner area */
    val scanAreaSize: Dp = 280.dp,

    /** Number of columns for the Table (Meja) grid */
    val tableGridColumns: Int = 2,

    /** Whether the current device is a tablet */
    val isTablet: Boolean = false
)

/**
 * CompositionLocal to provide [WindowType] down the tree.
 */
val LocalWindowType = compositionLocalOf { WindowType.Phone }

/**
 * CompositionLocal to provide [AdaptiveValues] down the tree.
 */
val LocalAdaptiveValues = compositionLocalOf { AdaptiveValues() }

/**
 * Determines the [WindowType] based on current screen configuration.
 */
@Composable
fun rememberWindowType(): WindowType {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp) {
        when {
            screenWidthDp >= 720 -> WindowType.Tablet10
            screenWidthDp >= 600 -> WindowType.Tablet7
            else -> WindowType.Phone
        }
    }
}

/**
 * Creates [AdaptiveValues] based on the given [WindowType].
 * All responsive breakpoints are centralized here.
 */
@Composable
fun rememberAdaptiveValues(windowType: WindowType): AdaptiveValues {
    return remember(windowType) {
        when (windowType) {
            WindowType.Phone -> AdaptiveValues(
                windowType = WindowType.Phone,
                contentMaxWidth = Dp.Unspecified,
                gridColumns = 1,
                horizontalPadding = 16.dp,
                cardMaxWidth = Dp.Unspecified,
                dialogMaxWidth = Dp.Unspecified,
                bottomNavMaxWidth = Dp.Unspecified,
                scanAreaSize = 280.dp,
                tableGridColumns = 2,
                isTablet = false
            )

            WindowType.Tablet7 -> AdaptiveValues(
                windowType = WindowType.Tablet7,
                contentMaxWidth = 520.dp,
                gridColumns = 2,
                horizontalPadding = 24.dp,
                cardMaxWidth = 480.dp,
                dialogMaxWidth = 440.dp,
                bottomNavMaxWidth = 480.dp,
                scanAreaSize = 320.dp,
                tableGridColumns = 3,
                isTablet = true
            )

            WindowType.Tablet10 -> AdaptiveValues(
                windowType = WindowType.Tablet10,
                contentMaxWidth = 600.dp,
                gridColumns = 2,
                horizontalPadding = 32.dp,
                cardMaxWidth = 520.dp,
                dialogMaxWidth = 480.dp,
                bottomNavMaxWidth = 520.dp,
                scanAreaSize = 360.dp,
                tableGridColumns = 4,
                isTablet = true
            )
        }
    }
}

/**
 * Modifier extension that applies adaptive max-width constraint.
 * On phones, no constraint is applied (full width).
 * On tablets, content is capped at [AdaptiveValues.contentMaxWidth].
 */
fun Modifier.adaptiveContentWidth(adaptive: AdaptiveValues): Modifier {
    return if (adaptive.contentMaxWidth != Dp.Unspecified) {
        this.widthIn(max = adaptive.contentMaxWidth)
    } else {
        this
    }
}

/**
 * Modifier extension for card-level max width.
 */
fun Modifier.adaptiveCardWidth(adaptive: AdaptiveValues): Modifier {
    return if (adaptive.cardMaxWidth != Dp.Unspecified) {
        this.widthIn(max = adaptive.cardMaxWidth)
    } else {
        this
    }
}

/**
 * Modifier extension for dialog max width.
 */
fun Modifier.adaptiveDialogWidth(adaptive: AdaptiveValues): Modifier {
    return if (adaptive.dialogMaxWidth != Dp.Unspecified) {
        this.widthIn(max = adaptive.dialogMaxWidth)
    } else {
        this
    }
}
