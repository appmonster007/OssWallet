package com.osscabinet.ossWallet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.osscabinet.ossWallet.R
import org.json.JSONArray
import org.json.JSONException

/**
 * Cash Flow Glance widget — table layout showing income + expense per currency.
 */
class CashFlowGlanceWidget : BaseOssWalletWidget() {

    override val widgetNavRoute: String = "analytics"

    @Composable
    override fun RenderContent(context: Context, prefs: Preferences, size: DpSize) {
        CashFlowWidgetContent(context, prefs, size)
    }

    object Keys {
        val IS_LOADED = stringPreferencesKey("cf_is_loaded")
        val INCOME_TOTAL = stringPreferencesKey("cf_income_total")
        val EXPENSE_TOTAL = stringPreferencesKey("cf_expense_total")
        val TRANSFER_TOTAL = stringPreferencesKey("cf_transfer_total")
        val COMPARISON = stringPreferencesKey("cf_comparison")
        val PERIOD_LABEL = stringPreferencesKey("cf_period_label")
        val BASE_CURRENCY = stringPreferencesKey("cf_base_currency")
        val SHOW_BASE_CONVERSION = stringPreferencesKey("cf_show_base_conversion")
        val SHOW_TOTALS = stringPreferencesKey("cf_show_totals")
        val CURRENCY_LIST = stringPreferencesKey("cf_currency_list")
    }
}

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

internal data class CurrencyRow(
    val code: String,
    val income: String,
    val incomeBase: String,
    val expense: String,
    val expenseBase: String,
    val transfer: String = "",
    val transferBase: String = ""
)

internal fun parseCurrencyList(json: String?): List<CurrencyRow> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val code = obj.optString("code", "")
            if (code.isBlank()) return@mapNotNull null
            CurrencyRow(
                code = code,
                income = obj.optString("income", ""),
                incomeBase = obj.optString("incomeBase", ""),
                expense = obj.optString("expense", ""),
                expenseBase = obj.optString("expenseBase", ""),
                transfer = obj.optString("transfer", ""),
                transferBase = obj.optString("transferBase", "")
            )
        }
    } catch (_: JSONException) {
        emptyList()
    }
}

// ---------------------------------------------------------------------------
// Composables
// ---------------------------------------------------------------------------

@Composable
internal fun CashFlowWidgetContent(context: Context, prefs: Preferences, widgetSize: DpSize) {
    val scale = when {
        widgetSize.height < 100.dp -> 0.85f
        widgetSize.height < 180.dp -> 1.0f
        else -> 1.15f
    }
    val isCompact = widgetSize.height < 100.dp

    val isLoaded = prefs[CashFlowGlanceWidget.Keys.IS_LOADED] == "true"
    if (!isLoaded) {
        CashFlowPlaceholder(scale)
    } else {
        CashFlowBody(prefs, scale, isCompact)
    }
}

@Composable
private fun CashFlowPlaceholder(scale: Float) {
    Text(
        text = "Cash Flow",
        style = TextStyle(
            fontSize = TextUnit(12f * scale, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            color = GlanceTheme.colors.onSurface
        )
    )
    Spacer(modifier = GlanceModifier.height((4f * scale).dp))
    Text(
        text = "Tap to open app",
        style = TextStyle(
            fontSize = TextUnit(10f * scale, TextUnitType.Sp),
            color = GlanceTheme.colors.onSurfaceVariant
        )
    )
}

@Composable
internal fun CashFlowBody(prefs: Preferences, scale: Float, isCompact: Boolean = false) {
    val keys = CashFlowGlanceWidget.Keys

    val incomeTotal = prefs[keys.INCOME_TOTAL] ?: ""
    val expenseTotal = prefs[keys.EXPENSE_TOTAL] ?: ""
    val transferTotal = prefs[keys.TRANSFER_TOTAL] ?: ""
    val periodLabel = prefs[keys.PERIOD_LABEL] ?: ""
    val comparison = prefs[keys.COMPARISON]?.takeIf { it.isNotEmpty() }
    val subText = comparison ?: periodLabel.takeIf { it.isNotBlank() }
    val baseCurrency = prefs[keys.BASE_CURRENCY] ?: ""
    val showBaseConversion = prefs[keys.SHOW_BASE_CONVERSION] != "false"
    val showTotals = prefs[keys.SHOW_TOTALS] != "false"
    val rows = parseCurrencyList(prefs[keys.CURRENCY_LIST])

    val headerSp = 10f * scale
    val colHeaderSp = 8f * scale
    val codeSp = 8f * scale
    val amountSp = 10f * scale
    val baseSp = 8f * scale
    val totalSp = 10f * scale

    val codeColWidth = 36.dp

    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "CASH FLOW",
                    style = TextStyle(
                        fontSize = TextUnit(headerSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        style = TextStyle(
                            fontSize = TextUnit(headerSp * 0.85f, TextUnitType.Sp),
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
            if (showTotals) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = incomeTotal,
                        style = TextStyle(
                            fontSize = TextUnit(totalSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_income_color)
                        )
                    )
                    Text(
                        text = expenseTotal,
                        style = TextStyle(
                            fontSize = TextUnit(totalSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_expense_color)
                        )
                    )
                    Text(
                        text = transferTotal,
                        style = TextStyle(
                            fontSize = TextUnit(totalSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_transfer_color)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(5.dp))

        if (isCompact) {
            CompactTotalsRow(incomeTotal, expenseTotal, transferTotal, scale)
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = GlanceModifier.width(codeColWidth))
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "INCOME",
                        style = TextStyle(
                            fontSize = TextUnit(colHeaderSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_income_label)
                        )
                    )
                }
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "EXPENSE",
                        style = TextStyle(
                            fontSize = TextUnit(colHeaderSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_expense_label)
                        )
                    )
                }
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "TRANSFER",
                        style = TextStyle(
                            fontSize = TextUnit(colHeaderSp, TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(R.color.widget_transfer_label)
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(3.dp))
            TableDivider()
            Spacer(modifier = GlanceModifier.height(2.dp))

            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(rows) { row ->
                    CurrencyTableRow(
                        row = row,
                        baseCurrency = baseCurrency,
                        showBaseConversion = showBaseConversion,
                        codeColWidth = codeColWidth,
                        codeSp = codeSp,
                        amountSp = amountSp,
                        baseSp = baseSp,
                        scale = scale
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyTableRow(
    row: CurrencyRow,
    baseCurrency: String,
    showBaseConversion: Boolean,
    codeColWidth: androidx.compose.ui.unit.Dp,
    codeSp: Float,
    amountSp: Float,
    baseSp: Float,
    scale: Float
) {
    val isForeign = row.code != baseCurrency
    val showConversion = showBaseConversion && isForeign

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.code,
                style = TextStyle(
                    fontSize = TextUnit(codeSp, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                modifier = GlanceModifier.width(codeColWidth)
            )
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = row.income,
                    style = TextStyle(
                        fontSize = TextUnit(amountSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_income_color)
                    )
                )
                if (showConversion) {
                    Text(
                        text = "≈ ${row.incomeBase}",
                        style = TextStyle(
                            fontSize = TextUnit(baseSp, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_income_comparison)
                        )
                    )
                }
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = row.expense,
                    style = TextStyle(
                        fontSize = TextUnit(amountSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_expense_color)
                    )
                )
                if (showConversion) {
                    Text(
                        text = "≈ ${row.expenseBase}",
                        style = TextStyle(
                            fontSize = TextUnit(baseSp, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_expense_comparison)
                        )
                    )
                }
            }
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = row.transfer,
                    style = TextStyle(
                        fontSize = TextUnit(amountSp, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(R.color.widget_transfer_color)
                    )
                )
                if (showConversion) {
                    Text(
                        text = "≈ ${row.transferBase}",
                        style = TextStyle(
                            fontSize = TextUnit(baseSp, TextUnitType.Sp),
                            color = ColorProvider(R.color.widget_transfer_comparison)
                        )
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.height((3f * scale).dp))
    }
}

@Composable
private fun CompactTotalsRow(incomeTotal: String, expenseTotal: String, transferTotal: String, scale: Float) {
    val sp = 9f * scale
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "IN",
            style = TextStyle(
                fontSize = TextUnit(7f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_income_label)
            )
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = incomeTotal,
            style = TextStyle(
                fontSize = TextUnit(sp, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_income_color)
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "OUT",
            style = TextStyle(
                fontSize = TextUnit(7f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_expense_label)
            )
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = expenseTotal,
            style = TextStyle(
                fontSize = TextUnit(sp, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_expense_color)
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "TR",
            style = TextStyle(
                fontSize = TextUnit(7f * scale, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_transfer_label)
            )
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = transferTotal,
            style = TextStyle(
                fontSize = TextUnit(sp, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                color = ColorProvider(R.color.widget_transfer_color)
            )
        )
    }
}

@Composable
private fun TableDivider() {
    Spacer(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(GlanceTheme.colors.onSurfaceVariant)
    )
}
