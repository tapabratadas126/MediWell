package com.mediwell.app

import android.app.Application
import android.util.Log

class MediwellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("Mediwell", "Mediwell Application initialized.")
    }
}
