package com.theglendales.alarm.jjmvvm

import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass

class JjFbRepository {

    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()

    fun getRtFromFb() {

    }
}