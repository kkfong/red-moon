/*
 * Copyright (c) 2016  Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jmstudios.redmoon.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.jmstudios.redmoon.event.secureSuspendChanged

import com.jmstudios.redmoon.model.Config
import com.jmstudios.redmoon.receiver.ScreenStateReceiver
import com.jmstudios.redmoon.thread.CurrentAppMonitoringThread
import com.jmstudios.redmoon.helper.Logger
import com.jmstudios.redmoon.util.atLeastAPI
import org.greenrobot.eventbus.Subscribe

class CurrentAppMonitor(private val mContext: Context) : ScreenStateReceiver.ScreenStateListener {
    companion object : Logger()
    private val screenStateReceiver = ScreenStateReceiver(this)
    private var mCamThread: CurrentAppMonitoringThread? = null

    private val powerManager
        get() = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val screenOn: Boolean
        get() = powerManager.run {
            if (atLeastAPI(20)) isInteractive else @Suppress("DEPRECATION") isScreenOn
        }

    private var isMonitoring = false

    override fun onScreenTurnedOn() {
        Log.i("Screen turn on received")
        startCamThread()
    }

    override fun onScreenTurnedOff() {
        Log.i("Screen turn off received")
        stopCamThread()
    }

    @Subscribe fun onSecureSuspendChanged(event: secureSuspendChanged) {
        if (Config.secureSuspend) start() else stop()
    }

    fun start() = when {
        !Config.secureSuspend -> Log.i("Can't start; monitoring is disabled.")
        isMonitoring -> Log.i("Monitoring is already started")
        else -> {
            Log.i("Starting app monitoring")
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON )
            }
            mContext.registerReceiver(screenStateReceiver, filter)
            isMonitoring = true
            startCamThread()
        }
    }

    fun stop() = if (!isMonitoring) {
        Log.i("Monitoring is already stopped")
    } else {
        Log.i("Stopping app monitoring")
        stopCamThread()
        try {
            mContext.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Catch errors when receiver is unregistered more than once.
            // It is not a problem, so we just ignore it.
        }
        isMonitoring = false
    }

    private fun startCamThread() {
        if (mCamThread == null && screenOn) {
            mCamThread = CurrentAppMonitoringThread(mContext).apply { start() }
        }
    }

    private fun stopCamThread() = mCamThread?.run{
        if (!isInterrupted) { interrupt() }
        mCamThread = null
    }
}
