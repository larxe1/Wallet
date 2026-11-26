package com.moneytracker.app

import android.app.Application
import com.moneytracker.app.viewmodel.AppContainer
import com.moneytracker.app.widget.WidgetSyncWorker

class MoneyTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetSyncWorker.schedulePeriodicSync(this)
    }
}
