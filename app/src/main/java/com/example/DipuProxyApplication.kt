package com.example

import android.app.Application
import com.example.data.repository.TransactionRepository

class DipuProxyApplication : Application() {
    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TransactionRepository(this)
    }
}
