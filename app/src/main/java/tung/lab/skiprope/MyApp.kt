package tung.lab.skiprope

import android.app.Application
import tung.lab.skiprope.ble.BleManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.init(this)
    }
}