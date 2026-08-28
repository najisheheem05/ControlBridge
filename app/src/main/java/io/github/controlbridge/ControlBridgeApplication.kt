/*
 * Copyright (C) 2026 Ishan
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 3 only.
 *
 * This program is distributed without any warranty. See the GNU General Public License for more details.
 */


package io.github.controlbridge

import android.app.Application
import android.content.Context
import android.content.Intent
import io.github.controlbridge.utils.LoggerService
import io.github.controlbridge.utils.settings.GlobalConfig

class ControlBridgeApplication : Application() {
    init {
        instance = this
    }

    companion object {
        lateinit var instance : ControlBridgeApplication
            private set

        val context : Context get() = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        GlobalConfig.init(this)
        startService(Intent(this, LoggerService::class.java))
    }
}
