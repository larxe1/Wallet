package com.moneytracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.data.model.WalletDashboardData
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors

/**
 * Calm private-banking wallet balance card with swipe and chevron switching across wallets.
 */
@Composable
fun WalletCardStack(
    walletsData: List<WalletDashboardData>,
    selectedWalletId: String,
    onSelectWallet: (String) -> Unit,
    isPrivacyMode: Boolean,
    onQuickAdd: (type: String, walletId: String?) -> Unit,
    onManageWallets: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (walletsData.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth()) {
            EmptyStateCard(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "No wallets configured",
                subtitle = "Create a wallet to track balances and log expenses.",
                actionLabel = "Manage Wallets",
                onAction = onManageWallets
            )
        }
        return
    }

    val activeIndex = remember(walletsData, selectedWalletId) {
        walletsData.indexOfFirst { it.wallet.id == selectedWalletId }.takeIf { it >= 0 } ?: 0
    }
    val current = walletsData[activeIndex]
    val walletColor = remember(current.wallet.color) { parseHexColor(current.wallet.color) }
    val hasMultiple = walletsData.size > 1
    val swipeThresholdPx = with(LocalDensity.current) { 52.dp.toPx() }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasMultiple) {
                        Modifier.pointerInput(walletsData, activeIndex) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onDragCancel = { dragAccumulator = 0f },
                                onDragEnd = {
                                    when {
                                        dragAccumulator <= -swipeThresholdPx -> {
                                            val nextIdx = (activeIndex + 1) % walletsData.size
                                            onSelectWallet(walletsData[nextIdx].wallet.id)
                                        }
                                        dragAccumulator >= swipeThresholdPx -> {
                                            val prevIdx = if (activeIndex == 0) walletsData.lastIndex else activeIndex - 1
                                            onSelectWallet(walletsData[prevIdx].wallet.id)
                                        }
                                    }
                                    dragAccumulator = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAccumulator += dragAmount
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            color = ObsidianColors.JadeElevated,
            border = BorderStroke(1.dp, ObsidianColors.Hairline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Crisp 2dp wallet-color top edge accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(walletColor.copy(alpha = 0.85f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row: Wallet dot + Name + pager / Manage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(walletColor)
                            )
                            Text(
                                text = current.wallet.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = ObsidianColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasMultiple) {
                                Text(
                                    text = "${activeIndex + 1}/${walletsData.size}",
                                    style = MonoStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = ObsidianColors.TextMuted,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasMultiple) {
                                IconButton(
                                    onClick = {
                                        val prevIdx = if (activeIndex == 0) walletsData.lastIndex else activeIndex - 1
                                        onSelectWallet(walletsData[prevIdx].wallet.id)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronLeft,
                                        contentDescription = "Previous wallet",
                                        tint = ObsidianColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val nextIdx = (activeIndex + 1) % walletsData.size
                                        onSelectWallet(walletsData[nextIdx].wallet.id)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Next wallet",
                                        tint = ObsidianColors.TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = onManageWallets,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = ObsidianColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Wallets",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ObsidianColors.TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Primary balance figure
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "AVAILABLE BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = ObsidianColors.TextMuted,
                            maxLines = 1
                        )
                        Text(
                            text = if (isPrivacyMode) {
                                "********"
                            } else {
                                FinanceMath.formatPhpCurrency(current.balance)
                            },
                            style = MonoStyle.copy(
                                fontSize = 31.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.8).sp
                            ),
                            color = when {
                                isPrivacyMode -> ObsidianColors.TextSecondary
                                current.balance < 0 -> ObsidianColors.Danger
                                else -> ObsidianColors.TextPrimary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = ObsidianColors.Hairline, thickness = 1.dp)

                    // Calm 2-column Monthly Inflow / Outflow ledger summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Period Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextMuted,
                                maxLines = 1
                            )
                            Text(
                                text = if (isPrivacyMode) {
                                    "****"
                                } else {
                                    "+${FinanceMath.formatPhpCurrency(current.totalIncome)}"
                                },
                                style = MonoStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isPrivacyMode) ObsidianColors.TextSecondary else ObsidianColors.EmeraldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Period Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianColors.TextMuted,
                                maxLines = 1
                            )
                            Text(
                                text = if (isPrivacyMode) {
                                    "****"
                                } else {
                                    "-${FinanceMath.formatPhpCurrency(current.totalExpenses)}"
                                },
                                style = MonoStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isPrivacyMode) ObsidianColors.TextSecondary else ObsidianColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action row: Expense & Income quick add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onQuickAdd("expense", current.wallet.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ObsidianColors.Danger.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ObsidianColors.Danger.copy(alpha = 0.10f),
                                contentColor = ObsidianColors.Danger
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }

                        EmeraldPrimaryButton(
                            text = "Income",
                            icon = Icons.Rounded.Add,
                            onClick = { onQuickAdd("income", current.wallet.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (hasMultiple) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                walletsData.forEachIndexed { index, item ->
                    val isSelected = index == activeIndex
                    val dotColor = if (isSelected) walletColor else ObsidianColors.JadeMuted
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (isSelected) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable { onSelectWallet(item.wallet.id) }
                    )
                }
            }
        }
    }
}
