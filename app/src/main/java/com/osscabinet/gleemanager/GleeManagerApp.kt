package com.osscabinet.gleemanager

import android.app.Application
import net.sqlcipher.database.SQLiteDatabase

class GleeManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
    }
}
