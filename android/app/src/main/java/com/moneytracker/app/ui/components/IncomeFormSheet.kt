package com.moneytracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.Income
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

private fun formatIncomeEditableDecimal(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", amount).trimEnd('0').trimEnd('.')
    }
}

private fun sanitizeIncomeAmountInput(raw: String): String {
    val cleaned = buildString {
        var dotSeen = false
        for (ch in raw) {
            if (ch.isDigit()) {
                append(ch)
            } else if (ch == '.' && !dotSeen) {
                dotSeen = true
                append(ch)
            }
        }
    }
    val dotIdx = cleaned.indexOf('.')
    return if (dotIdx >= 0 && cleaned.length > dotIdx + 3) {
        cleaned.substring(0, dotIdx + 3)
    } else {
        cleaned
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeFormSheet(
    editingIncome: Income? = null,
    wallets: List<Wallet>,
    defaultWalletId: String? = null,
    defaultDate: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        walletId: String,
        amount: Double,
        description: String,
        incomeDate: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingIncome != null

    var amountText by remember(editingIncome?.id) {
        mutableStateOf(
            editingIncome?.amount?.let { formatIncomeEditableDecimal(it) }.orEmpty()
        )
    }
    var description by remember(editingIncome?.id) {
        mutableStateOf(editingIncome?.description.orEmpty())
    }
    var selectedWalletId by remember(editingIncome?.id, defaultWalletId) {
        val matchedDefault = defaultWalletId
            ?.takeIf { id -> id != "all" && wallets.any { it.id == id } }
        mutableStateOf(
            editingIncome?.walletId
                ?: matchedDefault
                ?: wallets.firstOrNull()?.id.orEmpty()
        )
    }
    var dateStr by remember(editingIncome?.id, defaultDate) {
        val initial = editingIncome?.incomeDate
            ?.let { FinanceMath.parseLocalDate(it)?.format(FinanceMath.ISO_DATE) }
            ?: defaultDate?.let { FinanceMath.parseLocalDate(it)?.format(FinanceMath.ISO_DATE) }
            ?: LocalDate.now().format(FinanceMath.ISO_DATE)
        mutableStateOf(initial)
    }
    var validationError by remember(editingIncome?.id) { mutableStateOf<String?>(null) }

    var showCalculator by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(wallets) {
        if (selectedWalletId.isBlank() && wallets.isNotEmpty()) {
            val matched = defaultWalletId?.takeIf { id -> id != "all" && wallets.any { it.id == id } }
            selectedWalletId = matched ?: wallets.first().id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianColors.JadeElevated,
        contentColor = ObsidianColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Income" else "Add Income",
                    style = MaterialTheme.typography.titleLarge,
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
                        contentDescription = "Close",
                        tint = ObsidianColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Peso Amount Input + Calculator Trigger
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianColors.ObsidianCanvas,
                border = BorderStroke(1.dp, ObsidianColors.EmeraldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₱",
                        style = MonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                        color = ObsidianColors.EmeraldPrimary
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { next ->
                            validationError = null
                            amountText = sanitizeIncomeAmountInput(next)
                        },
                        placeholder = {
                            Text(
                                text = "0.00",
                                style = MonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                                color = ObsidianColors.TextMuted
                            )
                        },
                        textStyle = MonoStyle.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianColors.EmeraldBright
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ObsidianColors.ObsidianCanvas.copy(alpha = 0f),
                            unfocusedBorderColor = ObsidianColors.ObsidianCanvas.copy(alpha = 0f),
                            cursorColor = ObsidianColors.EmeraldPrimary,
                            focusedContainerColor = ObsidianColors.ObsidianCanvas.copy(alpha = 0f),
                            unfocusedContainerColor = ObsidianColors.ObsidianCanvas.copy(alpha = 0f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showCalculator = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = "Open calculator",
                            tint = ObsidianColors.EmeraldPrimary
                        )
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.Danger
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Salary, Freelance, Bonus") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ObsidianColors.EmeraldPrimary,
                    unfocusedBorderColor = ObsidianColors.Hairline,
                    focusedTextColor = ObsidianColors.TextPrimary,
                    unfocusedTextColor = ObsidianColors.TextPrimary,
                    focusedLabelColor = ObsidianColors.EmeraldPrimary,
                    unfocusedLabelColor = ObsidianColors.TextSecondary,
                    cursorColor = ObsidianColors.EmeraldPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Date Selector
            Text(
                text = "Date",
                style = MaterialTheme.typography.labelLarge,
                color = ObsidianColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianColors.JadeMuted,
                border = BorderStroke(1.dp, ObsidianColors.Hairline),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = ObsidianColors.EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = FinanceMath.formatDateDisplay(dateStr, "MMM dd, yyyy")
                                .ifBlank { dateStr },
                            style = MonoStyle.copy(fontSize = 15.sp),
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = ObsidianColors.EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet Selector
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.labelLarge,
                color = ObsidianColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (wallets.isEmpty()) {
                Text(
                    text = "No wallets available",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.TextMuted
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(wallets, key = { it.id }) { wallet ->
                        val selected = wallet.id == selectedWalletId
                        FilterChip(
                            selected = selected,
                            onClick = {
                                validationError = null
                                selectedWalletId = wallet.id
                            },
                            label = {
                                Text(
                                    text = wallet.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(wallet.color))
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ObsidianColors.JadeMuted,
                                labelColor = ObsidianColors.TextSecondary,
                                selectedContainerColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = ObsidianColors.EmeraldBright
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = ObsidianColors.Hairline,
                                selectedBorderColor = ObsidianColors.EmeraldPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianColors.Hairline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ObsidianColors.TextSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                EmeraldPrimaryButton(
                    text = if (isEditing) "Save Changes" else "Save Income",
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        when {
                            parsedAmount == null || parsedAmount <= 0.0 -> {
                                validationError = "Enter an amount greater than 0"
                            }
                            selectedWalletId.isBlank() -> {
                                validationError = "Select a wallet"
                            }
                            else -> {
                                onSave(
                                    selectedWalletId,
                                    parsedAmount,
                                    description.trim(),
                                    dateStr
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }

    if (showCalculator) {
        InlineCalculatorDialog(
            initialExpression = amountText,
            onApply = { result ->
                validationError = null
                amountText = formatIncomeEditableDecimal(result)
            },
            onDismiss = { showCalculator = false }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(dateStr) {
            (FinanceMath.parseLocalDate(dateStr) ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = ObsidianColors.JadeElevated),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            dateStr = picked.format(FinanceMath.ISO_DATE)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = ObsidianColors.EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = ObsidianColors.TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ObsidianColors.JadeElevated,
                    titleContentColor = ObsidianColors.TextPrimary,
                    headlineContentColor = ObsidianColors.TextPrimary,
                    weekdayContentColor = ObsidianColors.TextSecondary,
                    subheadContentColor = ObsidianColors.TextSecondary,
                    dayContentColor = ObsidianColors.TextPrimary,
                    selectedDayContainerColor = ObsidianColors.EmeraldPrimary,
                    selectedDayContentColor = ObsidianColors.TextOnAccent,
                    todayDateBorderColor = ObsidianColors.EmeraldPrimary,
                    todayContentColor = ObsidianColors.EmeraldBright
                )
            )
        }
    }
}
