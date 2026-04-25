package com.mzwprojects.mytwin

import android.app.Application
import com.mzwprojects.mytwin.di.ServiceLocator

class MyTwinApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}