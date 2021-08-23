package com.theglendales.alarm.jjmvp

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers

import io.reactivex.schedulers.Schedulers


private const val TAG="JJ_Presenter"
class JJ_Presenter(val secondFragmentView: JJ_ITF.ViewITF) : JJ_ITF.PresenterITF {
//    private val liveRtList = MutableLiveData<MutableList<RingtoneClass>>() // live data!
//    private val disposables = CompositeDisposable()
//    private val ringtonesSubject: BehaviorSubject<MutableList<RingtoneClass>> = BehaviorSubject.createDefault(mutableListOf())

    val jjModel = JJ_Model(this)

//    init {
//        ringtonesSubject.subscribe { ringtones -> liveRtList.value = ringtones }.addTo(disposables)
//    }
    override fun loadFromFb() {
        Log.d(TAG, "loadFromFb: 2) starts.")
        jjModel.loadFromFbModel()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { result -> secondFragmentView.showResult(result) }
    }
//    fun getLiveRtList(): LiveData<MutableList<RingtoneClass>> {
//        return liveRtList
//    }

}