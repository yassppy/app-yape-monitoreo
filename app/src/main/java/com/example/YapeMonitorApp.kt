package com.example

import android.app.Application
import com.example.alert.AlertManager
import com.example.data.AppPreferences
import com.example.data.db.AppDatabase
import com.example.data.repository.PaymentRepository
import com.example.network.FastApiWebhookClient

class YapeMonitorApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var webhookClient: FastApiWebhookClient
        private set

    lateinit var alertManager: AlertManager
        private set

    lateinit var repository: PaymentRepository
        private set

    companion object {
        lateinit var instance: YapeMonitorApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = AppPreferences(this)
        database = AppDatabase.getInstance(this)
        webhookClient = FastApiWebhookClient()
        alertManager = AlertManager(this, preferences)
        repository = PaymentRepository(
            paymentDao = database.paymentDao(),
            webhookClient = webhookClient,
            preferences = preferences,
            alertManager = alertManager
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        alertManager.release()
    }
}
