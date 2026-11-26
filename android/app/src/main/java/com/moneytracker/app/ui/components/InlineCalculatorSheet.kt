package com.moneytracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.round

private enum class CalcKeyKind {
    DIGIT,
    FUNCTION,
    OPERATOR,
    EQUALS,
    APPLY
}

private data class CalcKey(
    val id: String,
    val label: String,
    val contentDesc: String,
    val kind: CalcKeyKind
)

private val CALC_KEYPAD_ROWS: List<List<CalcKey>> = listOf(
    listOf(
        CalcKey("C", "C", "Clear", CalcKeyKind.FUNCTION),
        CalcKey("BACK", "⌫", "Backspace", CalcKeyKind.FUNCTION),
        CalcKey("%", "%", "Percent", CalcKeyKind.FUNCTION),
        CalcKey("÷", "÷", "Divide", CalcKeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("7", "7", "7", CalcKeyKind.DIGIT),
        CalcKey("8", "8", "8", CalcKeyKind.DIGIT),
        CalcKey("9", "9", "9", CalcKeyKind.DIGIT),
        CalcKey("×", "×", "Multiply", CalcKeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("4", "4", "4", CalcKeyKind.DIGIT),
        CalcKey("5", "5", "5", CalcKeyKind.DIGIT),
        CalcKey("6", "6", "6", CalcKeyKind.DIGIT),
        CalcKey("−", "−", "Subtract", CalcKeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("1", "1", "1", CalcKeyKind.DIGIT),
        CalcKey("2", "2", "2", CalcKeyKind.DIGIT),
        CalcKey("3", "3", "3", CalcKeyKind.DIGIT),
        CalcKey("+", "+", "Add", CalcKeyKind.OPERATOR)
    ),
    listOf(
        CalcKey("0", "0", "0", CalcKeyKind.DIGIT),
        CalcKey(".", ".", "Decimal point", CalcKeyKind.DIGIT),
        CalcKey("=", "=", "Equals", CalcKeyKind.EQUALS),
        CalcKey("APPLY", "✓", "Apply result", CalcKeyKind.APPLY)
    )
)

private val OPERATOR_CHARS = setOf('+', '-', '−', '*', '×', '/', '÷')

private fun formatCalcNumber(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
}

private fun formatDisplayPreview(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    val fmt = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    return fmt.format(rounded)
}

@Composable
fun InlineCalculatorDialog(
    initialExpression: String = "",
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember(initialExpression) {
        mutableStateOf(initialExpression.trim())
    }
    var evaluated by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val liveValue = remember(expression) {
        if (expression.isBlank()) null else FinanceMath.evaluateCalcExpression(expression)
    }

    val mainDisplayText = when {
        isError -> "Error"
        expression.isBlank() -> "0"
        liveValue != null -> "₱${formatDisplayPreview(liveValue)}"
        else -> {
            val tail = expression.split('+', '-', '−', '*', '×', '/', '÷').lastOrNull().orEmpty()
            tail.ifBlank { expression }
        }
    }

    fun handleDigit(digit: String) {
        isError = false
        if (evaluated) {
            expression = digit
            evaluated = false
        } else {
            expression += digit
        }
    }

    fun handleDecimal() {
        isError = false
        if (evaluated) {
            expression = "0."
            evaluated = false
            return
        }
        val currentSegment = expression.split('+', '-', '−', '*', '×', '/', '÷').lastOrNull().orEmpty()
        if (currentSegment.contains('.') || currentSegment.endsWith('%')) return
        expression += if (currentSegment.isEmpty()) "0." else "."
    }

    fun handleOperator(op: String) {
        isError = false
        evaluated = false
        if (expression.isEmpty()) {
            if (op == "−") {
                expression = "-"
            }
            return
        }
        val last = expression.last()
        expression = if (last in OPERATOR_CHARS) {
            if (expression.length == 1 && last == '-') {
                if (op == "−") expression else ""
            } else {
                expression.dropLast(1) + op
            }
        } else {
            expression + op
        }
    }

    fun handlePercent() {
        isError = false
        if (expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == ')' || last == '%') {
            expression += "%"
            evaluated = false
        }
    }

    fun handleBackspace() {
        isError = false
        evaluated = false
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
        }
    }

    fun handleClear() {
        expression = ""
        evaluated = false
        isError = false
    }

    fun handleEquals() {
        if (expression.isBlank()) return
        val result = FinanceMath.evaluateCalcExpression(expression)
        if (result == null || !result.isFinite()) {
            isError = true
            evaluated = false
            return
        }
        expression = formatCalcNumber(result)
        evaluated = true
        isError = false
    }

    fun handleApply() {
        if (expression.isBlank()) {
            isError = true
            return
        }
        val result = FinanceMath.evaluateCalcExpression(expression)
        if (result == null || !result.isFinite() || result <= 0.0) {
            isError = true
            return
        }
        val rounded = round(result * 100.0) / 100.0
        onApply(rounded)
        onDismiss()
    }

    fun onKeyPress(keyId: String) {
        when (keyId) {
            "C" -> handleClear()
            "BACK" -> handleBackspace()
            "%" -> handlePercent()
            "÷", "×", "−", "+" -> handleOperator(keyId)
            "." -> handleDecimal()
            "=" -> handleEquals()
            "APPLY" -> handleApply()
            else -> handleDigit(keyId)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianColors.JadeElevated,
            border = BorderStroke(1.dp, ObsidianColors.Hairline),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        color = ObsidianColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close calculator",
                            tint = ObsidianColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ObsidianColors.ObsidianCanvas,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expression.ifBlank { "0" },
                            style = MonoStyle.copy(fontSize = 13.sp),
                            color = ObsidianColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mainDisplayText,
                            style = MonoStyle.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isError) ObsidianColors.Danger else ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CALC_KEYPAD_ROWS.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                CalcKeypadButton(
                                    key = key,
                                    onClick = { onKeyPress(key.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKeypadButton(
    key: CalcKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor: Color
    val contentColor: Color
    val borderStroke: BorderStroke?

    when (key.kind) {
        CalcKeyKind.DIGIT -> {
            containerColor = ObsidianColors.JadeMuted
            contentColor = ObsidianColors.TextPrimary
            borderStroke = BorderStroke(1.dp, ObsidianColors.Hairline)
        }
        CalcKeyKind.FUNCTION -> {
            containerColor = ObsidianColors.JadeSurface
            contentColor = if (key.id == "C") ObsidianColors.Danger else ObsidianColors.TextSecondary
            borderStroke = BorderStroke(1.dp, ObsidianColors.Hairline)
        }
        CalcKeyKind.OPERATOR -> {
            containerColor = ObsidianColors.ElectricViolet.copy(alpha = 0.16f)
            contentColor = ObsidianColors.ElectricViolet
            borderStroke = BorderStroke(1.dp, ObsidianColors.ElectricViolet.copy(alpha = 0.35f))
        }
        CalcKeyKind.EQUALS -> {
            containerColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.18f)
            contentColor = ObsidianColors.EmeraldBright
            borderStroke = BorderStroke(1.dp, ObsidianColors.EmeraldPrimary.copy(alpha = 0.45f))
        }
        CalcKeyKind.APPLY -> {
            containerColor = ObsidianColors.EmeraldPrimary
            contentColor = ObsidianColors.TextOnAccent
            borderStroke = null
        }
    }

    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = borderStroke,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 52.dp)
            .height(54.dp)
            .semantics { contentDescription = key.contentDesc }
    ) {
        when (key.id) {
            "BACK" -> Icon(
                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            "APPLY" -> Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            else -> Text(
                text = key.label,
                style = MonoStyle.copy(
                    fontSize = if (key.kind == CalcKeyKind.OPERATOR || key.kind == CalcKeyKind.EQUALS) 20.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}
