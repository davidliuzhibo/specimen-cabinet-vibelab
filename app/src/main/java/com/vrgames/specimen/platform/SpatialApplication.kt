package com.vrgames.specimen.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.vrgames.specimen.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        launch(::mainApp)
    }
}
