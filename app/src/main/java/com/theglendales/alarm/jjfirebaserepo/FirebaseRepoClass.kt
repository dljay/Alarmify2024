package com.theglendales.alarm.jjfirebaserepo

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

const val TAG ="FBRepoClass"
class FirebaseRepoClass
{
    private val firebaseFSInstance : FirebaseFirestore = FirebaseFirestore.getInstance()
    lateinit var dbCollectionReference: CollectionReference

    //1) Get all data from Firebase . 2) 코루틴 사용 안하고 callback 사용 3) google.
    fun getPostList(): Task<QuerySnapshot> { // return type: Task snapshot!
        dbCollectionReference = firebaseFSInstance.collection("ringtones")
        
        return dbCollectionReference.orderBy("id", Query.Direction.ASCENDING).get()

    }

    fun sortSingleOrMultipleTags(tagsList: MutableList<String>): Task<QuerySnapshot> {
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