package com.theglendales.alarm.jjfirebaserepo

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

private const val TAG ="FBRepoClass"
private const val FB_COLLECTION_NAME="ringtones"
class FirebaseRepoClass(val context: Context)
{
    private val firebaseFSInstance : FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var dbCollectionReference: CollectionReference

    //1) Get all data from Firebase . 2) 코루틴 사용 안하고 callback 사용 3) google.
    fun getPostList(): Task<QuerySnapshot> { // return type: Task snapshot!
        // A) APP CHECK todo: '22 5.1 일단은 Debug Build 와 기타 빌드 Variant 안쓰게 설정하고 넣어보기.
        // App check Debug: https://firebase.google.com/docs/app-check/web/debug-provider
     /*   try {
            FirebaseApp.initializeApp(context) // Init FB
            val fbAppCheck = FirebaseAppCheck.getInstance()
            fbAppCheck.installAppCheckProviderFactory (SafetyNetAppCheckProviderFactory.getInstance())
        }catch (e: Exception) {
            Log.d(TAG, "getPostList: Unable to app Check! ㅆㅂ! e=$e")
        }*/

        // B) 자료 받기.
        dbCollectionReference = firebaseFSInstance.collection(FB_COLLECTION_NAME)
//        dbCollectionReference.whereArrayContains(badgeStrArray, "A, B")
        
        return dbCollectionReference.orderBy("id", Query.Direction.ASCENDING).get()

    }
    // 이건 whereArrayContains 는 두번 이상 못 써서 ..망침.. 의미가 없음..


    fun sortSingleOrMultipleTags(tagsList: MutableList<String>): Task<QuerySnapshot> {
        Log.d(TAG, "sortSingleOrMultipleTags: tagsList = $tagsList")
        // 약간 멍청해보이지만 this is the only way..?
        when (tagsList.size) {
            1 -> { // 1개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).get() // 이거 그냥 whereArrayContains (1개 조건 only..)로 바꿔도 됨.//containsAny = 10개 항목..
            }
            2 -> { // 2개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).whereEqualTo(tagsList[1], true).get()
            }
            3 -> { // 3개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).whereEqualTo(tagsList[1], true).whereEqualTo(tagsList[2], true).get()
            }
            4 -> { // 4개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).whereEqualTo(tagsList[1], true)
                    .whereEqualTo(tagsList[2], true).whereEqualTo(tagsList[3], true).get()
            }
            5 -> { // 5개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).whereEqualTo(tagsList[1], true)
                    .whereEqualTo(tagsList[2], true).whereEqualTo(tagsList[3], true).whereEqualTo(tagsList[4], true).get()
            }
            6 -> { // 6개 chip 선택
                return dbCollectionReference.whereEqualTo(tagsList[0], true).whereEqualTo(tagsList[1], true)
                    .whereEqualTo(tagsList[2], true).whereEqualTo(tagsList[3], true).whereEqualTo(tagsList[4], true).whereEqualTo(tagsList[5], true).get()
            }
            else-> return dbCollectionReference.whereEqualTo(tagsList[0], true).get() // 이건 그냥 혹시몰라서 when(1) 그대로 복붙!
        }

    }




}