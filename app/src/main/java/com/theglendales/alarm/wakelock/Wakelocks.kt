package com.theglendales.alarm.wakelock

interface Wakelocks {
    fun acquireServiceLock()

    fun releaseServiceLock()
}