package com.moneytracker.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.moneytracker.app.MainActivity
import com.moneytracker.app.QuickLogActivity
import com.moneytracker.app.R
import com.moneytracker.app.data.local.WidgetSnapshotStore
import com.moneytracker.app.data.model.WidgetSnapshot
import com.moneytracker.app.domain.FinanceMath

private val WIDGET_BG = Color(0xFF101715)
private val WIDGET_ELEVATED = Color(0xFF18221F)
private val WIDGET_EXPENSE_BG = Color(0xFF281A1D)
private val WIDGET_EMERALD = Color(0xFF26B883)
private val WIDGET_DANGER = Color(0xFFE36475)
private val WIDGET_TEXT_PRIMARY = Color(0xFFF0F4F2)
private val WIDGET_TEXT_SECONDARY = Color(0xFF96ABA3)
private val WIDGET_TEXT_ON_ACCENT = Color(0xFF061912)

class WalletGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_MODE = DpSize(180.dp, 110.dp)
        private val MEDIUM_MODE = DpSize(250.dp, 130.dp)
        private val LARGE_MODE = DpSize(300.dp, 180.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_MODE, MEDIUM_MODE, LARGE_MODE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = WidgetSnapshotStore(context)
        val initialSnapshot = store.readSnapshot()

        provideContent {
            val snapshot by store.snapshotFlow.collectAsState(initial = initialSnapshot)
            WalletWidgetContent(snapshot = snapshot)
        }
    }
}

@Composable
private fun WalletWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val isCompactHeight = size.height < 125.dp

    if (!snapshot.isAuthenticated) {
        SignedOutWidgetContent(context = context)
        return
    }

    val walletAccent = parseWidgetColor(snapshot.displayedWalletColor)
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val expenseIntent = buildQuickLogIntent(
        context = context,
        type = "expense",
        walletId = snapshot.displayedWalletId
    )
    val incomeIntent = buildQuickLogIntent(
        context = context,
        type = "income",
        walletId = snapshot.displayedWalletId
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BG)
            .cornerRadius(22.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header Row: Wallet switcher pill + Privacy toggle + Refresh
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .background(WIDGET_ELEVATED)
                    .cornerRadius(99.dp)
                    .clickable(actionRunCallback<CycleWalletAction>())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(walletAccent)
                        .cornerRadius(4.dp)
                ) {}
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = snapshot.displayedWalletName,
                    style = TextStyle(
                        color = ColorProvider(WIDGET_TEXT_PRIMARY),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier
                    .background(WIDGET_ELEVATED)
                    .cornerRadius(99.dp)
                    .clickable(actionRunCallback<TogglePrivacyAction>())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        if (snapshot.isPrivacyMasked) {
                            R.drawable.ic_widget_visibility
                        } else {
                            R.drawable.ic_widget_visibility_off
                        }
                    ),
                    contentDescription = if (snapshot.isPrivacyMasked) "Show balance" else "Hide balance",
                    modifier = GlanceModifier.size(16.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(ColorProvider(WIDGET_TEXT_SECONDARY))
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Box(
                modifier = GlanceModifier
                    .background(WIDGET_ELEVATED)
                    .cornerRadius(99.dp)
                    .clickable(actionRunCallback<RefreshWidgetAction>())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "Refresh balance",
                    modifier = GlanceModifier.size(16.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(ColorProvider(WIDGET_EMERALD))
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(if (isCompactHeight) 6.dp else 10.dp))

        // Balance readout + Month spend caption (tapping opens MainActivity)
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(openAppIntent))
        ) {
            Text(
                text = FinanceMath.formatPhpCurrency(
                    amount = snapshot.displayedBalance,
                    masked = snapshot.isPrivacyMasked
                ),
                style = TextStyle(
                    color = ColorProvider(WIDGET_TEXT_PRIMARY),
                    fontSize = if (isCompactHeight) 22.sp else 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = "Month spend: ${
                    FinanceMath.formatPhpCurrency(
                        amount = snapshot.thisMonthSpent,
                        compact = true,
                        masked = snapshot.isPrivacyMasked
                    )
                }",
                style = TextStyle(
                    color = ColorProvider(WIDGET_TEXT_SECONDARY),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(if (isCompactHeight) 8.dp else 12.dp))

        // 1-Tap Quick-Log Action Pills
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(WIDGET_EXPENSE_BG)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(expenseIntent))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Expense",
                    style = TextStyle(
                        color = ColorProvider(WIDGET_DANGER),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(WIDGET_EMERALD)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(incomeIntent))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Income",
                    style = TextStyle(
                        color = ColorProvider(WIDGET_TEXT_ON_ACCENT),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SignedOutWidgetContent(context: Context) {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WIDGET_BG)
            .cornerRadius(22.dp)
            .clickable(actionStartActivity(openAppIntent))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Money Tracker",
            style = TextStyle(
                color = ColorProvider(WIDGET_EMERALD),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Sign in to sync balances",
            style = TextStyle(
                color = ColorProvider(WIDGET_TEXT_SECONDARY),
                fontSize = 12.sp
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Box(
            modifier = GlanceModifier
                .background(WIDGET_EMERALD)
                .cornerRadius(14.dp)
                .clickable(actionStartActivity(openAppIntent))
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Open App",
                style = TextStyle(
                    color = ColorProvider(WIDGET_TEXT_ON_ACCENT),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Builds distinct Intents with unique action and data Uri so Android PendingIntents
 * for Expense vs Income never collide.
 */
private fun buildQuickLogIntent(
    context: Context,
    type: String,
    walletId: String?
): Intent {
    val normalized = if (type.equals("income", ignoreCase = true)) "income" else "expense"
    val actionName = if (normalized == "income") {
        "com.moneytracker.app.action.QUICK_LOG_INCOME"
    } else {
        "com.moneytracker.app.action.QUICK_LOG_EXPENSE"
    }
    val uriBuilder = Uri.Builder()
        .scheme("moneytracker")
        .authority("quicklog")
        .appendQueryParameter("type", normalized)
    if (!walletId.isNullOrBlank()) {
        uriBuilder.appendQueryParameter("wallet", walletId)
    }

    return Intent(context, QuickLogActivity::class.java).apply {
        action = actionName
        data = uriBuilder.build()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(QuickLogActivity.EXTRA_TYPE, normalized)
        if (!walletId.isNullOrBlank()) {
            putExtra(QuickLogActivity.EXTRA_WALLET_ID, walletId)
        }
    }
}

private fun parseWidgetColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return WIDGET_EMERALD
    val cleaned = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
    return runCatching { Color(android.graphics.Color.parseColor(cleaned)) }.getOrDefault(WIDGET_EMERALD)
}
