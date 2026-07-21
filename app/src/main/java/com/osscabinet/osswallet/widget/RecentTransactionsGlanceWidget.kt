@file:Suppress("RestrictedApi") // Glance's resource-based ColorProvider(@ColorRes) is public but flagged across library groups

package com.osscabinet.ossWallet.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter as ComposeColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.ui.components.getIconByName
import kotlinx.serialization.json.Json

class RecentTransactionsGlanceWidget : BaseOssWalletWidget() {

    override val widgetNavRoute: String = "records"

    @Composable
    override fun RenderContent(context: Context, prefs: Preferences, size: DpSize) {
        RecentTransactionsWidgetContent(context, prefs, size)
    }

    object Keys {
        val IS_LOADED         = stringPreferencesKey("rt_is_loaded")
        val TRANSACTIONS_JSON = stringPreferencesKey("rt_transactions_json")
        val PERIOD_LABEL      = stringPreferencesKey("rt_period_label")
        val PRIMARY_DISPLAY   = stringPreferencesKey("rt_primary_display")
    }
}

// ---------------------------------------------------------------------------
// Size thresholds
// ---------------------------------------------------------------------------

private val RT_TALL_HEIGHT = 200.dp
private val RT_WIDE4_WIDTH = 300.dp

// ---------------------------------------------------------------------------
// Composables
// ---------------------------------------------------------------------------

@Composable
internal fun RecentTransactionsWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    val isLoaded = prefs[RecentTransactionsGlanceWidget.Keys.IS_LOADED] == "true"

    if (!isLoaded) {
        RecentTransactionsPlaceholder()
        return
    }

    val showTimestamp = widgetSize.width  >= RT_WIDE4_WIDTH
    val showCategory  = widgetSize.height >= RT_TALL_HEIGHT
    val showAccount   = showTimestamp && showCategory
    val maxRows       = if (showCategory) 5 else 3
    val iconSizeDp    = when {
        showTimestamp && showCategory  -> 34.dp
        !showTimestamp && showCategory -> 30.dp
        showTimestamp                  -> 28.dp
        else                           -> 26.dp
    }

    RecentTransactionsBody(context, prefs, showTimestamp, showCategory, showAccount, maxRows, iconSizeDp)
}

@Composable
private fun RecentTransactionsPlaceholder() {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "RECENT ACTIVITIES",
            style = TextStyle(
                fontSize = TextUnit(10f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_on_surface_variant)
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading…",
                style = TextStyle(
                    fontSize = TextUnit(12f, TextUnitType.Sp),
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
internal fun RecentTransactionsBody(
    context: Context,
    prefs: Preferences,
    showTimestamp: Boolean,
    showCategory: Boolean,
    showAccount: Boolean,
    maxRows: Int,
    iconSizeDp: androidx.compose.ui.unit.Dp
) {
    val json           = prefs[RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON] ?: "[]"
    val periodLabel    = prefs[RecentTransactionsGlanceWidget.Keys.PERIOD_LABEL]
    val primaryDisplay = prefs[RecentTransactionsGlanceWidget.Keys.PRIMARY_DISPLAY] ?: "CATEGORY_FIRST"

    val transactions = try {
        Json.decodeFromString<List<WidgetTransaction>>(json)
    } catch (e: Exception) {
        emptyList()
    }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT ACTIVITIES",
                style = TextStyle(
                    fontSize = TextUnit(10f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(R.color.widget_on_surface_variant)
                )
            )
            if (periodLabel != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = periodLabel.uppercase(),
                    style = TextStyle(
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getString(R.string.no_recent_activities),
                    style = TextStyle(
                        fontSize = TextUnit(11f, TextUnitType.Sp),
                        color = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(transactions.take(maxRows)) { tx ->
                    TransactionItem(
                        tx             = tx,
                        showTimestamp  = showTimestamp,
                        showCategory   = showCategory,
                        showAccount    = showAccount,
                        iconSizeDp     = iconSizeDp,
                        primaryDisplay = primaryDisplay
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    tx: WidgetTransaction,
    showTimestamp: Boolean,
    showCategory: Boolean,
    showAccount: Boolean,
    iconSizeDp: androidx.compose.ui.unit.Dp,
    primaryDisplay: String
) {
    val isIncome   = tx.type == "income"
    val amountColor = if (isIncome) ColorProvider(R.color.widget_positive) else ColorProvider(R.color.widget_negative)
    val amountPrefix = if (isIncome) "+" else "-"

    val categoryColor = tx.categoryColor?.let { Color(it) } ?: NeutralStatusColor
    val iconBgColor   = categoryColor.copy(alpha = 0.15f)

    val showNoteFirst  = primaryDisplay == "NOTE_FIRST"
    val titleText      = if (showNoteFirst && tx.note.isNotBlank()) tx.note else tx.category
    val subTextMain    = if (showNoteFirst || tx.note.isBlank()) tx.category else tx.note
    val subtitle = when {
        showAccount   -> "$subTextMain • ${tx.accountName}"
        showCategory  -> subTextMain
        else          -> null
    }
    val iconPxSize = (iconSizeDp.value * 0.75f)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val glanceContext = LocalContext.current
        val density = Density(glanceContext.resources.displayMetrics.density)

        CompositionLocalProvider(LocalDensity provides density) {
            Box(
                modifier = GlanceModifier
                    .size(iconSizeDp)
                    .background(ColorProvider(iconBgColor))
                    .cornerRadius((iconSizeDp.value / 2).dp),
                contentAlignment = Alignment.Center
            ) {
                val icon    = getIconByName(tx.iconName)
                val painter = rememberVectorPainter(icon)

                val bitmap = remember(tx.iconName, categoryColor, density, iconSizeDp) {
                    val sizePx = (iconPxSize * density.density).toInt().coerceAtLeast(1)
                    val bmp    = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    val drawScope = CanvasDrawScope()
                    drawScope.draw(
                        density         = density,
                        layoutDirection = LayoutDirection.Ltr,
                        canvas          = androidx.compose.ui.graphics.Canvas(canvas),
                        size            = Size(sizePx.toFloat(), sizePx.toFloat())
                    ) {
                        with(painter) {
                            draw(
                                size        = Size(sizePx.toFloat(), sizePx.toFloat()),
                                colorFilter = ComposeColorFilter.tint(categoryColor)
                            )
                        }
                    }
                    bmp
                }

                Image(
                    provider           = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier           = GlanceModifier.size((iconPxSize / density.density).dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text     = titleText,
                style    = TextStyle(
                    fontSize   = TextUnit(11f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color      = ColorProvider(R.color.widget_on_surface)
                ),
                maxLines = 1
            )
            if (subtitle != null) {
                Text(
                    text     = subtitle,
                    style    = TextStyle(
                        fontSize   = TextUnit(8f, TextUnitType.Sp),
                        fontWeight = FontWeight.Medium,
                        color      = ColorProvider(R.color.widget_on_surface_variant)
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(4.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = amountPrefix + tx.amount,
                style = TextStyle(
                    fontSize   = TextUnit(11f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color      = amountColor
                )
            )
            if (showTimestamp) {
                Text(
                    text  = tx.timestamp,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        color    = ColorProvider(R.color.widget_on_surface_variant)
                    )
                )
            }
        }
    }
}
