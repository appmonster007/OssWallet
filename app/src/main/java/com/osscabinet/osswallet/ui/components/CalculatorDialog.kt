package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.osscabinet.ossWallet.ui.theme.AppColors

@Composable
fun CalculatorDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var expression by remember { mutableStateOf(if (initialValue == "0") "" else initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp,
            color = AppColors.panelSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val buttons = listOf(
                    "7", "8", "9", "/",
                    "4", "5", "6", "*",
                    "1", "2", "3", "-",
                    "C", "0", "=", "+"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(buttons) { button ->
                        val isOperator = button in listOf("/", "*", "-", "+", "=")
                        val isClear = button == "C"
                        
                        FilledTonalButton(
                            onClick = {
                                when (button) {
                                    "C" -> expression = ""
                                    "=" -> {
                                        try {
                                            expression = eval(expression).toString()
                                        } catch (e: Exception) {
                                            expression = "Error"
                                        }
                                    }
                                    else -> expression += button
                                }
                            },
                            modifier = Modifier.aspectRatio(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = if (isOperator) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AppColors.accentContainer,
                                    contentColor = AppColors.onAccentContainer
                                )
                            } else if (isClear) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AppColors.errorContainer,
                                    contentColor = AppColors.onErrorContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = button,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(expression.ifEmpty { "0" }) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("CONFIRM")
                    }
                }
            }
        }
    }
}

// Simple expression evaluator
fun eval(str: String): Double {
    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].toInt() else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.toInt()) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.toInt())) x += parseTerm()
                else if (eat('-'.toInt())) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.toInt())) x *= parseFactor()
                else if (eat('/'.toInt())) x /= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.toInt())) return parseFactor()
            if (eat('-'.toInt())) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.toInt())) {
                x = parseExpression()
                eat(')'.toInt())
            } else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) {
                while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }
            return x
        }
    }.parse()
}
