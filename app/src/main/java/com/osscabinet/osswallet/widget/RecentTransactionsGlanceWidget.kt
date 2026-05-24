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
import androidx.glance.GlanceTheme
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
        val IS_LOADED = stringPreferencesKey("rt_is_loaded")
        val TRANSACTIONS_JSON = stringPreferencesKey("rt_transactions_json")
        val PERIOD_LABEL = stringPreferencesKey("rt_period_label")
        val PRIMARY_DISPLAY = stringPreferencesKey("rt_primary_display")
    }
}

@Composable
internal fun RecentTransactionsWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    val scale = when {
        widgetSize.height < 100.dp -> 0.85f
        widgetSize.height < 180.dp -> 1.0f
        else -> 1.15f
    }
    
    val isLoaded = prefs[RecentTransactionsGlanceWidget.Keys.IS_LOADED] == "true"
    
    if (!isLoaded) {
        RecentTransactionsPlaceholder(scale)
    } else {
        RecentTransactionsBody(context, prefs, scale)
    }
}

@Composable
private fun RecentTransactionsPlaceholder(scale: Float) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Text(
            text = "RECENT ACTIVITIES",
            style = TextStyle(
                fontSize = TextUnit(10f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading...",
                style = TextStyle(
                    fontSize = TextUnit(12f * scale, TextUnitType.Sp),
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
internal fun RecentTransactionsBody(context: Context, prefs: Preferences, scale: Float) {
    val json = prefs[RecentTransactionsGlanceWidget.Keys.TRANSACTIONS_JSON] ?: "[]"
    val periodLabel = prefs[RecentTransactionsGlanceWidget.Keys.PERIOD_LABEL]
    val primaryDisplay = prefs[RecentTransactionsGlanceWidget.Keys.PRIMARY_DISPLAY] ?: "CATEGORY_FIRST"
    
    val transactions = try {
        Json.decodeFromString<List<WidgetTransaction>>(json)
    } catch (e: Exception) {
        emptyList()
    }

    val headerSp = 10f * scale

    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Match NetWorth header style
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT ACTIVITIES",
                style = TextStyle(
                    fontSize = TextUnit(headerSp, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            if (periodLabel != null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = periodLabel.uppercase(),
                    style = TextStyle(
                        fontSize = TextUnit(headerSp, TextUnitType.Sp),
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = context.getString(R.string.no_recent_activities),
                    style = TextStyle(
                        fontSize = TextUnit(11f * scale, TextUnitType.Sp),
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(transactions) { tx ->
                    TransactionItem(tx, scale, primaryDisplay)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(tx: WidgetTransaction, scale: Float, primaryDisplay: String) {
    val isIncome = tx.type == "income"
    val amountColor = if (isIncome) {
        ColorProvider(R.color.widget_income_color)
    } else {
        ColorProvider(R.color.widget_expense_color)
    }

    val categoryColor = tx.categoryColor?.let { Color(it) } ?: NeutralStatusColor
    val iconBgColor = categoryColor.copy(alpha = 0.15f)
    val iconTextColor = categoryColor

    val showNoteFirst = primaryDisplay == "NOTE_FIRST"
    
    val titleText = if (showNoteFirst && tx.note.isNotBlank()) tx.note else tx.category
    val subTextMain = if (showNoteFirst || tx.note.isBlank()) tx.category else tx.note
    val subtitle = "$subTextMain • ${tx.accountName}"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provide LocalDensity for ImageVector rendering
        val context = LocalContext.current
        val density = Density(context.resources.displayMetrics.density)
        
        CompositionLocalProvider(LocalDensity provides density) {
            Box(
                modifier = GlanceModifier
                    .size((36f * scale).dp)
                    .background(ColorProvider(iconBgColor))
                    .cornerRadius((18f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = getIconByName(tx.iconName)
                val painter = rememberVectorPainter(icon)
                
                val bitmap = remember(tx.iconName, iconTextColor, density, scale) {
                    val sizePx = (20 * scale * density.density).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
                    val drawScope = CanvasDrawScope()
                    drawScope.draw(
                        density = density,
                        layoutDirection = LayoutDirection.Ltr,
                        canvas = composeCanvas,
                        size = Size(sizePx.toFloat(), sizePx.toFloat())
                    ) {
                        with(painter) {
                            draw(size = Size(sizePx.toFloat(), sizePx.toFloat()), colorFilter = ComposeColorFilter.tint(iconTextColor))
                        }
                    }
                    bitmap
                }

                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.size((20f * scale).dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = titleText,
                style = TextStyle(
                    fontSize = TextUnit(11f * scale, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
            
            Text(
                text = subtitle,
                style = TextStyle(
                    fontSize = TextUnit(8f * scale, TextUnitType.Sp),
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.width(4.dp))

        Column(horizontalAlignment = Alignment.End) {
            val prefix = if (isIncome) "+" else "-"
            Text(
                text = prefix + tx.amount,
                style = TextStyle(
                    fontSize = TextUnit(11f * scale, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            )
            Text(
                text = tx.timestamp,
                style = TextStyle(
                    fontSize = TextUnit(8f * scale, TextUnitType.Sp),
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}
