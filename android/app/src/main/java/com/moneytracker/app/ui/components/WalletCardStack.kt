package com.moneytracker.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.moneytracker.app.data.model.WalletDashboardData
import com.moneytracker.app.domain.FinanceMath
import com.moneytracker.app.ui.theme.MonoStyle
import com.moneytracker.app.ui.theme.ObsidianColors

private data class DeckWalletCard(
    val id: String,
    val name: String,
    val colorHex: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val isCombined: Boolean,
    val walletCount: Int
) {
    val balance: Double get() = totalIncome - totalExpenses
}

/**
 * Interactive 3D fanned deck of wallet cards featuring an emerald mesh front card,
 * light-wave Canvas paths, quick-add trigger, and spring-animated card cycling.
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
    val cards = remember(walletsData) {
        val totalInc = walletsData.sumOf { it.totalIncome }
        val totalExp = walletsData.sumOf { it.totalExpenses }
        buildList {
            add(
                DeckWalletCard(
                    id = "all",
                    name = "All Wallets",
                    colorHex = "#28C991",
                    totalIncome = totalInc,
                    totalExpenses = totalExp,
                    isCombined = true,
                    walletCount = walletsData.size
                )
            )
            walletsData.forEach { w ->
                add(
                    DeckWalletCard(
                        id = w.wallet.id,
                        name = w.wallet.name,
                        colorHex = w.wallet.color,
                        totalIncome = w.totalIncome,
                        totalExpenses = w.totalExpenses,
                        isCombined = false,
                        walletCount = 0
                    )
                )
            }
        }
    }

    val activeIndex = cards.indexOfFirst { it.id == selectedWalletId }.takeIf { it >= 0 } ?: 0
    val visibleBehind = (cards.size - 1).coerceIn(0, 2)
    val frontOffsetDp = (visibleBehind * 18).dp
    val cardHeight = 216.dp
    val deckHeight = cardHeight + frontOffsetDp

    fun cycle(delta: Int) {
        if (cards.size <= 1) return
        val nextIdx = (activeIndex + delta + cards.size) % cards.size
        onSelectWallet(cards[nextIdx].id)
    }

    var dragAccum by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My Wallets",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cards.size > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { cycle(-1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous wallet",
                            tint = ObsidianColors.TextSecondary
                        )
                    }
                    Text(
                        text = "${activeIndex + 1}/${cards.size}",
                        style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = ObsidianColors.TextSecondary,
                        maxLines = 1
                    )
                    IconButton(
                        onClick = { cycle(1) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next wallet",
                            tint = ObsidianColors.TextSecondary
                        )
                    }
                }
            }

            TextButton(
                onClick = onManageWallets,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.labelLarge,
                    color = ObsidianColors.EmeraldPrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = ObsidianColors.EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(deckHeight)
                .pointerInput(cards, activeIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum < -64f) {
                                cycle(1)
                            } else if (dragAccum > 64f) {
                                cycle(-1)
                            }
                            dragAccum = 0f
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            cards.forEachIndexed { index, card ->
                val depth = (index - activeIndex + cards.size) % cards.size
                val isFront = depth == 0
                val isVisible = depth <= 2

                val targetOffsetY = when (depth) {
                    0 -> frontOffsetDp
                    1 -> (frontOffsetDp - 18.dp).coerceAtLeast(0.dp)
                    else -> 0.dp
                }
                val targetScale = when (depth) {
                    0 -> 1f
                    1 -> 0.94f
                    2 -> 0.88f
                    else -> 0.84f
                }
                val targetAlpha = when (depth) {
                    0 -> 1f
                    1 -> 0.92f
                    2 -> 0.72f
                    else -> 0f
                }
                val zOrder = when (depth) {
                    0 -> 30f
                    1 -> 20f
                    2 -> 10f
                    else -> 1f
                }

                val animatedY by animateDpAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "deckOffsetY"
                )
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "deckScale"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "deckAlpha"
                )

                if (isVisible || animatedAlpha > 0.02f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardHeight)
                            .zIndex(zOrder)
                            .offset(y = animatedY)
                            .scale(animatedScale)
                            .alpha(animatedAlpha)
                    ) {
                        WalletDeckCardSurface(
                            card = card,
                            isFront = isFront,
                            isPrivacyMode = isPrivacyMode,
                            onSelect = if (!isFront) {
                                { onSelectWallet(card.id) }
                            } else {
                                null
                            },
                            onQuickAdd = { type ->
                                val targetWalletId = if (card.isCombined) null else card.id
                                onQuickAdd(type, targetWalletId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletDeckCardSurface(
    card: DeckWalletCard,
    isFront: Boolean,
    isPrivacyMode: Boolean,
    onSelect: (() -> Unit)?,
    onQuickAdd: (String) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val walletAccent = parseHexColor(card.colorHex, ObsidianColors.EmeraldPrimary)
    var showAddMenu by remember { mutableStateOf(false) }

    val cardModifier = Modifier
        .fillMaxSize()
        .clip(shape)
        .then(
            if (onSelect != null) {
                Modifier.clickable(onClick = onSelect)
            } else {
                Modifier
            }
        )

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = if (isFront) ObsidianColors.EmeraldDeep else ObsidianColors.JadeElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (isFront) Color.White.copy(alpha = 0.22f) else ObsidianColors.HairlineHover
        ),
        shadowElevation = if (isFront) 12.dp else 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Emerald mesh gradient + flowing light-wave paths for front card; sculpted dark jade for stacked cards
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (isFront) {
                    val primaryTop = if (card.isCombined) {
                        ObsidianColors.EmeraldBright
                    } else {
                        lerp(ObsidianColors.EmeraldPrimary, walletAccent, 0.38f)
                    }
                    val midTone = if (card.isCombined) {
                        ObsidianColors.EmeraldMid
                    } else {
                        lerp(ObsidianColors.EmeraldMid, walletAccent, 0.22f)
                    }
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryTop, midTone, ObsidianColors.EmeraldDeep),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                    // Ambient radial highlights
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ObsidianColors.ChartreuseLime.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.14f, h * 0.12f),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.14f, h * 0.12f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ObsidianColors.ObsidianCanvas.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.92f, h * 0.95f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * 0.92f, h * 0.95f)
                    )

                    // Light-wave Canvas ribbons
                    val wave1 = Path().apply {
                        moveTo(-w * 0.05f, h * 0.72f)
                        cubicTo(w * 0.28f, h * 0.44f, w * 0.58f, h * 0.92f, w * 1.05f, h * 0.56f)
                    }
                    val wave2 = Path().apply {
                        moveTo(-w * 0.05f, h * 0.88f)
                        cubicTo(w * 0.35f, h * 0.62f, w * 0.66f, h * 1.02f, w * 1.06f, h * 0.74f)
                    }
                    drawPath(
                        path = wave1,
                        color = Color.White.copy(alpha = 0.14f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = wave2,
                        color = ObsidianColors.ChartreuseLime.copy(alpha = 0.16f),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lerp(ObsidianColors.JadeElevated, walletAccent, 0.16f),
                                ObsidianColors.JadeSurface
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
            }

            if (isFront) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Wallet name + circular '+' quick-add trigger
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (card.isCombined) ObsidianColors.ChartreuseLime else walletAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ObsidianColors.TextOnAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (card.isCombined && card.walletCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(ObsidianColors.TextOnAccent.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${card.walletCount}",
                                        style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        color = ObsidianColors.TextOnAccent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { showAddMenu = true },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.92f),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Quick add transaction",
                                        tint = ObsidianColors.TextOnAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false },
                                containerColor = ObsidianColors.JadeElevated
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Add Expense",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ObsidianColors.TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = ObsidianColors.Danger
                                        )
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onQuickAdd("expense")
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Add Income",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ObsidianColors.TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            tint = ObsidianColors.EmeraldPrimary
                                        )
                                    },
                                    onClick = {
                                        showAddMenu = false
                                        onQuickAdd("income")
                                    }
                                )
                            }
                        }
                    }

                    // Net balance display
                    Column {
                        Text(
                            text = "Net Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = ObsidianColors.TextOnAccent.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceMath.formatPhpCurrency(
                                amount = card.balance,
                                masked = isPrivacyMode
                            ),
                            style = MonoStyle.copy(
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.6).sp
                            ),
                            color = ObsidianColors.TextOnAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bottom compact In/Out summary pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FrontCardMiniPill(
                            label = "In",
                            amount = FinanceMath.formatPhpCurrency(
                                amount = card.totalIncome,
                                compact = true,
                                masked = isPrivacyMode
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FrontCardMiniPill(
                            label = "Out",
                            amount = FinanceMath.formatPhpCurrency(
                                amount = card.totalExpenses,
                                compact = true,
                                masked = isPrivacyMode
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Stacked card header peeking above front card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(walletAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = ObsidianColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FinanceMath.formatPhpCurrency(
                            amount = card.balance,
                            compact = true,
                            masked = isPrivacyMode
                        ),
                        style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = ObsidianColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FrontCardMiniPill(
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianColors.TextOnAccent.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = ObsidianColors.TextOnAccent.copy(alpha = 0.78f),
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = amount,
            style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = ObsidianColors.TextOnAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
