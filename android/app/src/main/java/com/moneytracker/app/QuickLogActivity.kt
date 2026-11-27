package com.moneytracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.CategoryIcon
import com.moneytracker.app.ui.components.EmeraldPrimaryButton
import com.moneytracker.app.ui.components.ObsidianSegmentedPill
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.QuickLogViewModel
import java.time.Instant
import java.time.ZoneOffset

class QuickLogActivity : ComponentActivity() {

    private lateinit var quickLogViewModel: QuickLogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val container = (application as MoneyTrackerApp).container
        quickLogViewModel = ViewModelProvider(this, container.viewModelFactory)[QuickLogViewModel::class.java]
        applyIntent(intent)

        setContent {
            MoneyTrackerTheme {
                QuickLogOverlaySheet(
                    viewModel = quickLogViewModel,
                    onDismiss = { finish() },
                    onOpenMainApp = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        val typeFromExtra = intent?.getStringExtra(EXTRA_TYPE)
        val typeFromUri = intent?.data?.getQueryParameter("type")
        val initialType = (typeFromExtra ?: typeFromUri ?: "expense")

        val walletFromExtra = intent?.getStringExtra(EXTRA_WALLET_ID)
        val walletFromUri = intent?.data?.getQueryParameter("wallet")?.takeIf { it.isNotBlank() }
        val walletId = walletFromExtra ?: walletFromUri

        quickLogViewModel.initialize(
            initialType = initialType,
            preselectedWalletId = walletId
        )
    }

    companion object {
        const val EXTRA_TYPE = "extra_quick_log_type"
        const val EXTRA_WALLET_ID = "extra_quick_log_wallet_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickLogOverlaySheet(
    viewModel: QuickLogViewModel,
    onDismiss: () -> Unit,
    onOpenMainApp: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCalculator by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isExpense = state.type != "income"
    val accentColor = if (isExpense) ObsidianColors.Danger else ObsidianColors.EmeraldPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = ObsidianColors.JadeSurface,
            border = BorderStroke(1.dp, ObsidianColors.HairlineHover),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Drag indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ObsidianColors.HairlineHover)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpense) "Quick Expense" else "Quick Income",
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

                if (!state.isAuthenticated) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sign in to Money Tracker to log transactions from the widget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    EmeraldPrimaryButton(
                        text = "Open App",
                        onClick = onOpenMainApp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    return@Column
                }

                // Expense / Income Toggle
                ObsidianSegmentedPill(
                    options = listOf(
                        "expense" to "Expense",
                        "income" to "Income"
                    ),
                    selected = state.type,
                    onSelect = { viewModel.setType(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Large ₱ Amount Display + Calculator Keypad Toggle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ObsidianColors.ObsidianCanvas,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₱",
                                style = MonoStyle.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = state.expression,
                                onValueChange = { viewModel.setExpression(it) },
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        style = MonoStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                                        color = ObsidianColors.TextMuted
                                    )
                                },
                                singleLine = true,
                                textStyle = MonoStyle.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ObsidianColors.TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    cursorColor = ObsidianColors.EmeraldPrimary
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showCalculator = !showCalculator },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (showCalculator) ObsidianColors.EmeraldPrimary.copy(alpha = 0.18f)
                                        else ObsidianColors.JadeElevated
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = "Toggle calculator",
                                    tint = if (showCalculator) ObsidianColors.EmeraldPrimary else ObsidianColors.TextSecondary
                                )
                            }
                        }

                        val hasOperator = state.expression.any { it in setOf('+', '-', '*', '/', '×', '÷', '%') }
                        if (hasOperator && state.evaluatedAmount != null) {
                            Text(
                                text = "= ${FinanceMath.formatPhpCurrency(state.evaluatedAmount ?: 0.0)}",
                                style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = ObsidianColors.EmeraldPrimary,
                                modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                // Inline Calculator Keypad
                AnimatedVisibility(visible = showCalculator) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rows = listOf(
                            listOf("7", "8", "9", "÷"),
                            listOf("4", "5", "6", "×"),
                            listOf("1", "2", "3", "-"),
                            listOf(".", "0", "BACK", "+")
                        )
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    val isOp = key in setOf("÷", "×", "-", "+")
                                    QuickCalcKey(
                                        label = key,
                                        isAccent = isOp,
                                        isBackspace = key == "BACK",
                                        onClick = {
                                            when (key) {
                                                "BACK" -> viewModel.backspaceCalc()
                                                else -> viewModel.appendCalcToken(key)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickCalcKey(
                                label = "C",
                                isAccent = false,
                                isBackspace = false,
                                onClick = { viewModel.clearCalc() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickCalcKey(
                                label = "%",
                                isAccent = true,
                                isBackspace = false,
                                onClick = { viewModel.appendCalcToken("%") },
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(2f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button) { viewModel.evaluateAndApplyCalc() },
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianColors.EmeraldPrimary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "= Apply",
                                        style = MonoStyle.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        color = ObsidianColors.TextOnAccent
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.setDescription(it) },
                    label = { Text("Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianColors.ObsidianCanvas,
                        unfocusedContainerColor = ObsidianColors.ObsidianCanvas,
                        focusedBorderColor = ObsidianColors.EmeraldPrimary,
                        unfocusedBorderColor = ObsidianColors.HairlineHover,
                        focusedLabelColor = ObsidianColors.EmeraldPrimary,
                        unfocusedLabelColor = ObsidianColors.TextSecondary,
                        focusedTextColor = ObsidianColors.TextPrimary,
                        unfocusedTextColor = ObsidianColors.TextPrimary,
                        cursorColor = ObsidianColors.EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = ObsidianColors.EmeraldPrimary
                        )
                    }
                } else {
                    // Wallet Chips
                    ChipSectionLabel("Wallet")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.wallets.forEach { wallet ->
                            val selected = state.selectedWalletId == wallet.id
                            QuickSelectChip(
                                label = wallet.name,
                                dotColor = parseHexColor(wallet.color),
                                selected = selected,
                                onClick = { viewModel.setWallet(wallet.id) }
                            )
                        }
                    }

                    if (isExpense) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ChipSectionLabel("Category")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.categories.forEach { cat ->
                                val selected = state.selectedCategoryId == cat.id
                                QuickSelectChip(
                                    label = cat.name,
                                    dotColor = null,
                                    selected = selected,
                                    onClick = { viewModel.setCategory(cat.id) },
                                    leadingIcon = {
                                        CategoryIcon(
                                            categoryName = cat.name,
                                            storedIcon = cat.icon,
                                            tint = if (selected) ObsidianColors.TextOnAccent else ObsidianColors.TextSecondary,
                                            size = 15.dp
                                        )
                                    }
                                )
                            }
                        }

                        if (state.paymentMethods.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ChipSectionLabel("Payment Method")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.paymentMethods.forEach { pm ->
                                    val selected = state.selectedPaymentMethodId == pm.id
                                    QuickSelectChip(
                                        label = pm.label,
                                        dotColor = null,
                                        selected = selected,
                                        onClick = { viewModel.setPaymentMethod(pm.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker Trigger + Optional Installment Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianColors.JadeElevated,
                        border = BorderStroke(1.dp, ObsidianColors.Hairline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = "Pick date",
                                tint = ObsidianColors.EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = FinanceMath.formatDateDisplay(state.dateStr, "MMM dd, yyyy")
                                    .ifBlank { state.dateStr },
                                style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = ObsidianColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    if (isExpense) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Installment",
                                style = MaterialTheme.typography.labelLarge,
                                color = ObsidianColors.TextSecondary
                            )
                            Switch(
                                checked = state.isInstallment,
                                onCheckedChange = { viewModel.setInstallment(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianColors.TextOnAccent,
                                    checkedTrackColor = ObsidianColors.EmeraldPrimary,
                                    uncheckedThumbColor = ObsidianColors.TextSecondary,
                                    uncheckedTrackColor = ObsidianColors.JadeMuted
                                )
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = isExpense && state.isInstallment) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 6, 12, 24).forEach { months ->
                            val selected = state.installmentMonths == months
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setInstallment(true, months) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ObsidianColors.ElectricViolet else ObsidianColors.JadeElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ObsidianColors.ElectricViolet else ObsidianColors.Hairline
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${months}m",
                                        style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        color = if (selected) Color.White else ObsidianColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                if (!state.error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = ObsidianColors.Danger
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianColors.JadeMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = ObsidianColors.EmeraldPrimary
                        )
                    }
                } else {
                    EmeraldPrimaryButton(
                        text = if (isExpense) "Save Expense" else "Save Income",
                        onClick = { viewModel.submit(onSaved = onDismiss) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = FinanceMath.parseLocalDate(state.dateStr)
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = ObsidianColors.JadeElevated),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocal = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.setDate(selectedLocal.format(FinanceMath.ISO_DATE))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("OK", color = ObsidianColors.EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Cancel", color = ObsidianColors.TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ChipSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = ObsidianColors.TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun QuickSelectChip(
    label: String,
    dotColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (selected) ObsidianColors.EmeraldPrimary else ObsidianColors.JadeElevated,
            border = BorderStroke(
                1.dp,
                if (selected) ObsidianColors.EmeraldBright else ObsidianColors.Hairline
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                } else if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (selected) ObsidianColors.TextOnAccent else dotColor)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) ObsidianColors.TextOnAccent else ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickCalcKey(
    label: String,
    isAccent: Boolean,
    isBackspace: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isAccent) ObsidianColors.ElectricViolet.copy(alpha = 0.2f) else ObsidianColors.JadeElevated,
        border = BorderStroke(1.dp, ObsidianColors.Hairline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Backspace",
                    tint = ObsidianColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = label,
                    style = MonoStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    color = if (isAccent) ObsidianColors.ElectricViolet else ObsidianColors.TextPrimary
                )
            }
        }
    }
}
