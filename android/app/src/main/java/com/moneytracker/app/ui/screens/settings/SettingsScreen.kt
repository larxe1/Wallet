package com.moneytracker.app.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneytracker.app.data.model.Category
import com.moneytracker.app.data.model.PaymentMethod
import com.moneytracker.app.data.model.Wallet
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.components.ConfirmDeleteDialog
import com.moneytracker.app.ui.components.EmeraldPrimaryButton
import com.moneytracker.app.ui.components.ObsidianSurfaceCard
import com.moneytracker.app.ui.components.parseHexColor
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.SettingsViewModel
import com.moneytracker.app.widget.WalletGlanceWidgetReceiver
import java.util.Locale

private val CATEGORY_EMOJIS = listOf(
    "🏠", "🛒", "🍔", "🚗", "⚡", "🎮",
    "🎬", "🏋️", "💰", "💳", "📦", "❤️",
    "🎁", "✈️", "📚", "👔", "🐾", "🏥"
)

private val CARD_NETWORKS = listOf("Visa", "Mastercard", "Amex")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var newWalletName by remember { mutableStateOf("") }
    var newWalletColor by remember { mutableStateOf(FinanceMath.WALLET_COLOR_PRESETS.first().first) }

    var pmType by remember { mutableStateOf("credit") }
    var pmBank by remember { mutableStateOf("") }
    var pmNetwork by remember { mutableStateOf("Visa") }
    var pmDetails by remember { mutableStateOf("") }

    var showCategorySheet by remember { mutableStateOf(false) }

    var pendingDeleteWallet by remember { mutableStateOf<Wallet?>(null) }
    var pendingDeletePm by remember { mutableStateOf<PaymentMethod?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(state.message, state.error) {
        val msg = state.message ?: state.error
        if (msg != null) {
            onSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ObsidianColors.EmeraldPrimary,
        unfocusedBorderColor = ObsidianColors.Hairline,
        focusedTextColor = ObsidianColors.TextPrimary,
        unfocusedTextColor = ObsidianColors.TextPrimary,
        focusedLabelColor = ObsidianColors.EmeraldPrimary,
        unfocusedLabelColor = ObsidianColors.TextSecondary,
        cursorColor = ObsidianColors.EmeraldPrimary
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.ObsidianCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = ObsidianColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (state.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ObsidianColors.EmeraldPrimary,
                    trackColor = ObsidianColors.JadeElevated
                )
            }
        }

        // 1. Home Screen Widget Card
        item {
            ObsidianSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionIconBadge(
                        icon = Icons.Rounded.Widgets,
                        tint = ObsidianColors.EmeraldPrimary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home Screen Widget",
                            style = MaterialTheme.typography.titleMedium,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Balance overview & 1-tap quick log",
                            style = MaterialTheme.typography.bodySmall,
                            color = ObsidianColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                EmeraldPrimaryButton(
                    text = "Add Home Widget",
                    icon = Icons.Rounded.AddToHomeScreen,
                    onClick = {
                        val manager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, WalletGlanceWidgetReceiver::class.java)
                        if (manager.isRequestPinAppWidgetSupported) {
                            val accepted = manager.requestPinAppWidget(provider, null, null)
                            if (!accepted) {
                                onSnackbar("Launcher declined the widget request. Add it from your home screen.")
                            }
                        } else {
                            onSnackbar("Launcher does not support 1-tap pinning. Add it from your widget picker.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Wallets
        item {
            SectionHeader(
                icon = Icons.Rounded.AccountBalanceWallet,
                tint = ObsidianColors.EmeraldPrimary,
                title = "Wallets",
                count = state.wallets.size
            )
        }

        item {
            ObsidianSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                if (state.wallets.isEmpty()) {
                    Text(
                        text = "No wallets yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianColors.TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.wallets.forEach { wallet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianColors.JadeElevated)
                                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(wallet.color))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = wallet.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = ObsidianColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (wallet.isDefault) {
                                    Surface(
                                        color = ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ObsidianColors.EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            maxLines = 1
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { pendingDeleteWallet = wallet },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete wallet ${wallet.name}",
                                            tint = ObsidianColors.Danger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ObsidianColors.Hairline)
                Spacer(modifier = Modifier.height(12.dp))

                // 7 Wallet Color Swatches
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceMath.WALLET_COLOR_PRESETS.forEach { (hex, label) ->
                        val isSelected = newWalletColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { newWalletColor = hex }
                                .semantics { contentDescription = "Select $label color" },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) ObsidianColors.TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = ObsidianColors.TextOnAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newWalletName,
                        onValueChange = { newWalletName = it },
                        placeholder = { Text("New wallet name", color = ObsidianColors.TextMuted) },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    EmeraldPrimaryButton(
                        text = "Add",
                        icon = Icons.Rounded.Add,
                        onClick = {
                            val trimmed = newWalletName.trim()
                            viewModel.addWallet(trimmed, newWalletColor)
                            if (trimmed.isNotEmpty()) {
                                newWalletName = ""
                            }
                        }
                    )
                }
            }
        }

        // 3. Payment Methods
        item {
            SectionHeader(
                icon = Icons.Rounded.CreditCard,
                tint = ObsidianColors.ElectricViolet,
                title = "Payment Methods",
                count = state.paymentMethods.size
            )
        }

        item {
            ObsidianSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                if (state.paymentMethods.isEmpty()) {
                    Text(
                        text = "No payment methods configured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianColors.TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.paymentMethods.forEachIndexed { idx, pm ->
                            PaymentMethodRow(
                                pm = pm,
                                canMoveUp = idx > 0,
                                canMoveDown = idx < state.paymentMethods.lastIndex,
                                onMoveUp = { viewModel.movePaymentMethod(idx, idx - 1) },
                                onMoveDown = { viewModel.movePaymentMethod(idx, idx + 1) },
                                onDelete = { pendingDeletePm = pm }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ObsidianColors.Hairline)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "credit" to "Credit",
                            "debit" to "Debit",
                            "cash" to "Cash"
                        ).forEach { (typeKey, label) ->
                            val selected = pmType == typeKey
                            SelectPillButton(
                                label = label,
                                selected = selected,
                                accentColor = ObsidianColors.ElectricViolet,
                                onClick = { pmType = typeKey },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (pmType == "credit") {
                        OutlinedTextField(
                            value = pmBank,
                            onValueChange = { pmBank = it },
                            label = { Text("Bank (e.g. BPI)") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CARD_NETWORKS.forEach { net ->
                                SelectPillButton(
                                    label = net,
                                    selected = pmNetwork == net,
                                    accentColor = ObsidianColors.EmeraldPrimary,
                                    onClick = { pmNetwork = net },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = pmDetails,
                            onValueChange = { next ->
                                if (next.length <= 4) {
                                    pmDetails = next.filter { it.isDigit() }
                                }
                            },
                            label = { Text("Last 4 digits") },
                            textStyle = MonoStyle.copy(color = ObsidianColors.TextPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = pmDetails,
                            onValueChange = { pmDetails = it },
                            label = { Text("Bank or App Name") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    EmeraldPrimaryButton(
                        text = "Add Payment Method",
                        icon = Icons.Rounded.Add,
                        onClick = {
                            val trimmedDetails = pmDetails.trim()
                            viewModel.addPaymentMethod(
                                type = pmType,
                                details = trimmedDetails,
                                bank = pmBank.trim(),
                                network = pmNetwork
                            )
                            if (trimmedDetails.isNotEmpty()) {
                                pmDetails = ""
                                pmBank = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Categories
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    icon = Icons.Rounded.Tag,
                    tint = ObsidianColors.EmeraldPrimary,
                    title = "Categories",
                    count = state.categories.size,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ObsidianSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmeraldPrimaryButton(
                        text = "Category",
                        icon = Icons.Rounded.Add,
                        onClick = { showCategorySheet = true },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { viewModel.seedDefaultCategories() },
                        enabled = !state.isSeeding,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianColors.Hairline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ObsidianColors.TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = ObsidianColors.ElectricViolet,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isSeeding) "Syncing..." else "Seed Defaults",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.categories.isEmpty()) {
                    Text(
                        text = "No categories yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianColors.TextMuted
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.categories.forEach { cat ->
                            CategoryChipItem(
                                category = cat,
                                onDelete = { pendingDeleteCategory = cat }
                            )
                        }
                    }
                }
            }
        }

        // 5. Account
        item {
            SectionHeader(
                icon = Icons.Rounded.Person,
                tint = ObsidianColors.TextSecondary,
                title = "Account",
                count = null
            )
        }

        item {
            ObsidianSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Signed in as",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianColors.TextMuted
                        )
                        Text(
                            text = state.userEmail ?: "Authenticated session",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianColors.Danger.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ObsidianColors.Danger.copy(alpha = 0.1f),
                            contentColor = ObsidianColors.Danger
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", maxLines = 1)
                    }
                }
            }
        }
    }

    if (showCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { showCategorySheet = false },
            onSave = { name, icon ->
                viewModel.addCategory(name, icon)
                showCategorySheet = false
            }
        )
    }

    pendingDeleteWallet?.let { w ->
        ConfirmDeleteDialog(
            title = "Delete Wallet",
            message = "Remove wallet \"${w.name}\"?",
            onConfirm = {
                viewModel.deleteWallet(w)
                pendingDeleteWallet = null
            },
            onDismiss = { pendingDeleteWallet = null }
        )
    }

    pendingDeletePm?.let { pm ->
        ConfirmDeleteDialog(
            title = "Delete Payment Method",
            message = "Remove \"${pm.label}\"?",
            onConfirm = {
                viewModel.deletePaymentMethod(pm)
                pendingDeletePm = null
            },
            onDismiss = { pendingDeletePm = null }
        )
    }

    pendingDeleteCategory?.let { cat ->
        ConfirmDeleteDialog(
            title = "Delete Category",
            message = "Remove category \"${cat.name}\"?",
            onConfirm = {
                viewModel.deleteCategory(cat)
                pendingDeleteCategory = null
            },
            onDismiss = { pendingDeleteCategory = null }
        )
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    tint: Color,
    title: String,
    count: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionIconBadge(icon = icon, tint = tint)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = ObsidianColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (count != null) {
            Surface(
                color = ObsidianColors.JadeElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ObsidianColors.Hairline)
            ) {
                Text(
                    text = count.toString(),
                    style = MonoStyle.copy(fontSize = 11.sp),
                    color = ObsidianColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SectionIconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SelectPillButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.18f) else ObsidianColors.JadeElevated)
            .border(
                width = 1.dp,
                color = if (selected) accentColor.copy(alpha = 0.65f) else ObsidianColors.Hairline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accentColor else ObsidianColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PaymentMethodRow(
    pm: PaymentMethod,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val typeLower = pm.type.lowercase(Locale.US)
    val badgeColor = when (typeLower) {
        "credit" -> ObsidianColors.ElectricViolet
        "debit" -> ObsidianColors.EmeraldPrimary
        else -> ObsidianColors.Warning
    }
    val typeIcon = when (typeLower) {
        "credit" -> Icons.Rounded.CreditCard
        "debit" -> Icons.Rounded.AccountBalance
        else -> Icons.Rounded.Payments
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianColors.JadeElevated)
            .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = typeLower,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = pm.label,
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = if (canMoveUp) ObsidianColors.TextSecondary else ObsidianColors.TextMuted.copy(alpha = 0.35f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = if (canMoveDown) ObsidianColors.TextSecondary else ObsidianColors.TextMuted.copy(alpha = 0.35f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${pm.label}",
                    tint = ObsidianColors.Danger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    category: Category,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianColors.JadeElevated)
            .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(12.dp))
            .padding(start = 10.dp, end = if (category.isDefault) 10.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = category.icon,
            fontSize = 15.sp
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = ObsidianColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (category.isDefault) {
            Surface(
                color = ObsidianColors.EmeraldPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianColors.EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        } else {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete category ${category.name}",
                    tint = ObsidianColors.Danger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(CATEGORY_EMOJIS.first()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

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
                    text = "Add Category",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianColors.TextPrimary
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

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianColors.JadeSurface)
                    .border(1.dp, ObsidianColors.Hairline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon.ifBlank { "🏷️" }, fontSize = 18.sp)
                Text(
                    text = name.trim().ifEmpty { "Category Preview" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMsg = null
                },
                label = { Text("Name") },
                placeholder = { Text("e.g. Groceries", color = ObsidianColors.TextMuted) },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = icon,
                onValueChange = { next ->
                    if (next.length <= 4) icon = next
                },
                label = { Text("Emoji Icon") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORY_EMOJIS.forEach { emoji ->
                    val selected = emoji == icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) {
                                    ObsidianColors.EmeraldPrimary.copy(alpha = 0.22f)
                                } else {
                                    ObsidianColors.JadeSurface
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) ObsidianColors.EmeraldPrimary else ObsidianColors.Hairline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { icon = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg.orEmpty(),
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
                    text = "Save Category",
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            errorMsg = "Enter a category name."
                        } else {
                            onSave(trimmed, icon.trim().ifEmpty { "🏷️" })
                        }
                    },
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}
