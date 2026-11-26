package com.moneytracker.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.moneytracker.app.MoneyTrackerApp
import com.moneytracker.app.data.local.WidgetSnapshotStore

class WalletGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WalletGlanceWidget()
}

class TogglePrivacyAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = WidgetSnapshotStore(context)
        store.updateSnapshot { current ->
            current.copy(isPrivacyMasked = !current.isPrivacyMasked)
        }
        WalletGlanceWidget().updateAll(context)
    }
}

class CycleWalletAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = WidgetSnapshotStore(context)
        store.updateSnapshot { current ->
            val totalSlots = current.cycleCount
            val nextIdx = if (totalSlots <= 1) 0 else (current.selectedWalletIndex + 1) % totalSlots
            current.copy(selectedWalletIndex = nextIdx)
        }
        WalletGlanceWidget().updateAll(context)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val app = context.applicationContext as? MoneyTrackerApp
        if (app != null) {
            runCatching { app.container.repository.refreshWidgetSnapshot() }
        } else {
            WidgetSyncWorker.enqueueImmediateSync(context)
        }
        WalletGlanceWidget().updateAll(context)
    }
}
