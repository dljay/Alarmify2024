package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.util.Log
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

private const val TAG="MyCacher"
class MyCacher(private val receivedContext: Context, private val cacheDir: File, private val mpInstanceReceived: ExoForUrl ) {

    companion object {
        var simpleCache: SimpleCache? = null
        var leastRecentlyUsedCacheEvictor: LeastRecentlyUsedCacheEvictor? = null
        var exoDatabaseProvider: ExoDatabaseProvider? = null
        var exoPlayerCacheSize: Long = 90 * 1024 * 1024 // 94.3MB 갤S20 에서 사용했을때 램이 100~104 정도로 뜸.
    }

    fun initCacheVariables() { //MainActivity 에서 onCreate 에서 바로 부름.
        Log.d(TAG, "initCacheVariables: starts.. ")


        if (leastRecentlyUsedCacheEvictor == null) {
            leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)
            Log.d(TAG, "initCacheVariables: inside leastRecentlyUsed....")
        }

        if (exoDatabaseProvider == null) {
            exoDatabaseProvider = ExoDatabaseProvider(receivedContext)
            Log.d(TAG, "initCacheVariables: inside exoDatabaseProvider ... ")
        }

        if (simpleCache == null) {
            simpleCache = SimpleCache(File(cacheDir,"exoCache"), leastRecentlyUsedCacheEvictor!!, exoDatabaseProvider!!)
            Log.d(TAG, "initCacheVariables: inside simpleCache..")
        }
        // 이게 다 끝나면 더 이상 null 이 없을테니 ExoForLocal Instance 로 넘김!
        mpInstanceReceived.initExoPlayer(true)
        Log.d(TAG, "initCacheVariables: Ends.. ")

    }

}