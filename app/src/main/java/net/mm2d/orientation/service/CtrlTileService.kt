package net.mm2d.orientation.service

import android.os.Build.VERSION_CODES
import android.service.quicksettings.TileService;
import androidx.annotation.RequiresApi
import kotlin.system.exitProcess

@RequiresApi(VERSION_CODES.N)
class CtrlTileService:  TileService() {
    override fun onStartListening() {
        super.onStartListening()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        if (MainService.isStarted) {
            MainService.stop(context)
            exitProcess(0)
        } else {
            MainService.start(context)
        }
    }
}
