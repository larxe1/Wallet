package com.moneytracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.RecurringTransaction
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormSheet(
    editingRecurring: RecurringTransaction? = null,
    wallets: List<Wallet>,
    categories: List<Category>,
    paymentMethods: List<PaymentMethod>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Double,
        description: String?,
        walletId: String,
        categoryId: String?,
        paymentMethodId: String?,
        frequencyType: String,
        dayOfMonth: Int?,
        startDate: String?,
        intervalDays: Int?,
        isActive: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.type?.lowercase(Locale.US) ?: "expense")
    }
    var amountText by remember(editingRecurring) {
        mutableStateOf(
            editingRecurring?.amount?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } ?: ""
        )
    }
    var description by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.description.orEmpty())
    }
    var walletId by remember(editingRecurring, wallets) {
        mutableStateOf(
            editingRecurring?.walletId
                ?.takeIf { id -> wallets.any { it.id == id } }
                ?: wallets.firstOrNull()?.id.orEmpty()
        )
    }
    var categoryId by remember(editingRecurring, categories) {
        mutableStateOf(
            editingRecurring?.categoryId
                ?.takeIf { id -> categories.any { it.id == id } }
                ?: categories.firstOrNull()?.id.orEmpty()
        )
    }
    var paymentMethodId by remember(editingRecurring, paymentMethods) {
        mutableStateOf(
            editingRecurring?.paymentMethodId
                ?.takeIf { id -> paymentMethods.any { it.id == id } }
                ?: paymentMethods.firstOrNull()?.id.orEmpty()
        )
    }
    var isActive by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.isActive ?: true)
    }
    var frequencyType by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.frequencyType?.lowercase(Locale.US) ?: "monthly")
    }
    var dayOfMonth by remember(editingRecurring) {
        mutableIntStateOf((editingRecurring?.dayOfMonth ?: 1).coerceIn(1, 31))
    }
    var startDate by remember(editingRecurring) {
        mutableStateOf(
            editingRecurring?.startDate?.take(10)
                ?: LocalDate.now().format(FinanceMath.ISO_DATE)
        )
    }
    var intervalDaysText by remember(editingRecurring) {
        mutableStateOf((editingRecurring?.intervalDays ?: 30).toString())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    var showCalculator by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ObsidianColors.EmeraldPrimary,
        unfocusedBorderColor = ObsidianColors.Hairline,
        focusedTextColor = ObsidianColors.TextPrimary,
        unfocusedTextColor = ObsidianColors.TextPrimary,
        focusedLabelColor = ObsidianColors.EmeraldPrimary,
        unfocusedLabelColor = ObsidianColors.TextSecondary,
        cursorColor = ObsidianColors.EmeraldPrimary
    )

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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRecurring != null) {
                        "Edit Recurring Rule"
                    } else {
                        "Add Recurring Rule"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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

            // Expense / Income segmented pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianColors.JadeSurface)
                    .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SegmentTabButton(
                    label = "Expense",
                    selected = type == "expense",
                    activeColor = ObsidianColors.Danger,
                    onClick = { type = "expense" },
                    modifier = Modifier.weight(1f)
                )
                SegmentTabButton(
                    label = "Income",
                    selected = type == "income",
                    activeColor = ObsidianColors.EmeraldPrimary,
                    onClick = { type = "income" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { next ->
                    amountText = next
                    validationError = null
                },
                label = { Text("Amount (PHP)") },
                prefix = {
                    Text(
                        text = "₱",
                        style = MonoStyle,
                        color = if (type == "income") ObsidianColors.EmeraldPrimary else ObsidianColors.Danger
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showCalculator = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = "Calculator",
                            tint = ObsidianColors.EmeraldPrimary
                        )
                    }
                },
                textStyle = MonoStyle.copy(color = ObsidianColors.TextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    validationError = null
                },
                label = { Text("Description") },
                placeholder = { Text("e.g. Netflix, Monthly Rent", color = ObsidianColors.TextMuted) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Wallet dropdown
            WalletDropdownField(
                wallets = wallets,
                selectedWalletId = walletId,
                onSelect = { walletId = it },
                fieldColors = fieldColors
            )

            if (type == "expense") {
                CategoryDropdownField(
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onSelect = { categoryId = it },
                    fieldColors = fieldColors
                )
                PaymentMethodDropdownField(
                    paymentMethods = paymentMethods,
                    selectedId = paymentMethodId,
                    onSelect = { paymentMethodId = it },
                    fieldColors = fieldColors
                )
            }

            // Active status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianColors.JadeSurface)
                    .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(12.dp))
                    .clickable { isActive = !isActive }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rule Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = ObsidianColors.TextPrimary
                    )
                    Text(
                        text = if (isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) ObsidianColors.EmeraldPrimary else ObsidianColors.TextMuted
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianColors.TextOnAccent,
                        checkedTrackColor = ObsidianColors.EmeraldPrimary,
                        uncheckedThumbColor = ObsidianColors.TextSecondary,
                        uncheckedTrackColor = ObsidianColors.JadeMuted
                    )
                )
            }

            // Schedule frequency
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Schedule Frequency",
                    style = MaterialTheme.typography.labelLarge,
                    color = ObsidianColors.TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianColors.JadeSurface)
                        .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SegmentTabButton(
                        label = "Monthly",
                        icon = Icons.Rounded.CalendarMonth,
                        selected = frequencyType == "monthly",
                        activeColor = ObsidianColors.ElectricViolet,
                        onClick = { frequencyType = "monthly" },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentTabButton(
                        label = "Custom Interval",
                        icon = Icons.Rounded.Repeat,
                        selected = frequencyType == "interval",
                        activeColor = ObsidianColors.ElectricViolet,
                        onClick = {
                            frequencyType = "interval"
                            if (startDate.isBlank()) {
                                startDate = LocalDate.now().format(FinanceMath.ISO_DATE)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (frequencyType == "monthly") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Day of Month (${FinanceMath.getOrdinalDay(dayOfMonth)})",
                        style = MaterialTheme.typography.labelLarge,
                        color = ObsidianColors.TextSecondary
                    )
                    DayOfMonthGrid(
                        selectedDay = dayOfMonth,
                        onSelectDay = { dayOfMonth = it }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Start Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianColors.TextSecondary
                        )
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianColors.Hairline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ObsidianColors.JadeSurface,
                                contentColor = ObsidianColors.TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = ObsidianColors.EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = FinanceMath.formatDateDisplay(startDate, "MMM d, yyyy").ifEmpty { startDate },
                                style = MonoStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = intervalDaysText,
                        onValueChange = {
                            intervalDaysText = it.filter { ch -> ch.isDigit() }
                            validationError = null
                        },
                        label = { Text("Every X Days") },
                        textStyle = MonoStyle.copy(color = ObsidianColors.TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianColors.Danger
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianColors.Hairline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ObsidianColors.TextSecondary
                    )
                ) {
                    Text("Cancel", maxLines = 1)
                }

                EmeraldPrimaryButton(
                    text = if (editingRecurring != null) "Save Changes" else "Save Rule",
                    onClick = {
                        val parsedAmount = amountText.trim().toDoubleOrNull()
                        val parsedInterval = intervalDaysText.trim().toIntOrNull()
                        when {
                            parsedAmount == null || parsedAmount <= 0.0 -> {
                                validationError = "Enter an amount greater than 0."
                            }
                            description.isBlank() -> {
                                validationError = "Enter a description."
                            }
                            walletId.isBlank() -> {
                                validationError = "Select a wallet."
                            }
                            frequencyType == "interval" && (parsedInterval == null || parsedInterval < 1) -> {
                                validationError = "Interval days must be at least 1."
                            }
                            else -> {
                                onSave(
                                    type,
                                    parsedAmount,
                                    description.trim(),
                                    walletId,
                                    if (type == "income") null else categoryId.takeIf { it.isNotBlank() },
                                    if (type == "income") null else paymentMethodId.takeIf { it.isNotBlank() },
                                    frequencyType,
                                    if (frequencyType == "monthly") dayOfMonth else null,
                                    if (frequencyType == "interval") startDate else null,
                                    if (frequencyType == "interval") parsedInterval else null,
                                    isActive
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }

    if (showCalculator) {
        InlineCalculatorDialog(
            initialExpression = amountText,
            onApply = { evaluated ->
                amountText = if (evaluated % 1.0 == 0.0) {
                    evaluated.toLong().toString()
                } else {
                    String.format(Locale.US, "%.2f", evaluated)
                }
                showCalculator = false
            },
            onDismiss = { showCalculator = false }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(startDate) {
            val ld = FinanceMath.parseLocalDate(startDate) ?: LocalDate.now()
            ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            startDate = picked.format(FinanceMath.ISO_DATE)
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
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SegmentTabButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) activeColor else ObsidianColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) activeColor else ObsidianColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayOfMonthGrid(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    val rows = remember { (1..31).toList().chunked(7) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianColors.JadeSurface)
            .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { rowDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowDays.forEach { day ->
                    val isSelected = day == selectedDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) ObsidianColors.EmeraldPrimary else ObsidianColors.JadeMuted
                            )
                            .clickable { onSelectDay(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            style = MonoStyle.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) ObsidianColors.TextOnAccent else ObsidianColors.TextPrimary,
                            maxLines = 1
                        )
                    }
                }
                repeat(7 - rowDays.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletDropdownField(
    wallets: List<Wallet>,
    selectedWalletId: String,
    onSelect: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = wallets.find { it.id == selectedWalletId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Select Wallet",
            onValueChange = {},
            readOnly = true,
            label = { Text("Wallet") },
            leadingIcon = selected?.let {
                {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(it.color))
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ObsidianColors.JadeElevated
        ) {
            wallets.forEach { w ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(w.color))
                            )
                            Text(
                                text = w.name,
                                color = ObsidianColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        onSelect(w.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownField(
    categories: List<Category>,
    selectedCategoryId: String,
    onSelect: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "Select Category",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = selected?.let { cat ->
                { CategoryIcon(categoryName = cat.name, storedIcon = cat.icon, size = 18.dp) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ObsidianColors.JadeElevated
        ) {
            categories.forEach { c ->
                DropdownMenuItem(
                    leadingIcon = {
                        CategoryIcon(categoryName = c.name, storedIcon = c.icon, size = 18.dp)
                    },
                    text = {
                        Text(
                            text = c.name,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(c.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodDropdownField(
    paymentMethods: List<PaymentMethod>,
    selectedId: String,
    onSelect: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = paymentMethods.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.label ?: "Select Payment Method",
            onValueChange = {},
            readOnly = true,
            label = { Text("Payment Method") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ObsidianColors.JadeElevated
        ) {
            paymentMethods.forEach { pm ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = pm.label,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(pm.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
