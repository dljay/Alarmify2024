package com.theglendales.alarm.jjadapters

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.jjdata.RtInTheCloud
import java.text.SimpleDateFormat
import java.util.*

private const val TAG="PurchasedItemRcVAdapter"
class PurchasedItemRcVAdapter(private val receivedActivity: Activity) : RecyclerView.Adapter<PurchasedItemRcVAdapter.PurchaseVHolder>() {

    private var purchaseBoolTrueList= listOf<RtInTheCloud>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchasedItemRcVAdapter.PurchaseVHolder {
        val xmlToView = LayoutInflater.from(receivedActivity).inflate(R.layout.jj_purchased_item_singleslot, parent, false)
        return PurchaseVHolder(xmlToView)
    }

    override fun onBindViewHolder(holder: PurchasedItemRcVAdapter.PurchaseVHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: ")
        val purchasedRt = purchaseBoolTrueList[position]
        // A) Purchased Date
        val purchasedTimeInMiliSecs = purchasedRt.purchaseTime
        val purchasedDateHumanReadable = getReadableDateTime(purchasedTimeInMiliSecs)
        holder.tvPurchaseDate.text = purchasedDateHumanReadable

        // B) Title & OrderId & Price
        holder.tvRtTitle.text = purchasedRt.title
        holder.tvOrderId.text = purchasedRt.orderID
        holder.tvPrice.text = purchasedRt.itemPrice

        // C) Album Art Loading
        GlideApp.with(receivedActivity).load(purchasedRt.imageURL).centerCrop().error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object  : RequestListener<Drawable>{
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: [Purchased Item] Glide Load Failed xx.... ")
                    return false}

                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?,dataSource: DataSource?,isFirstResource: Boolean): Boolean {
                    return false}

            }).into(holder.ivAlbumArt)
    }

    override fun getItemCount(): Int {
        return purchaseBoolTrueList.size
    }
    // my Methods & Refresh(), DiffUtil
    fun getReadableDateTime(timeInMilSeconds: Long): String {
        // Long(1573847839...) 을 받아서 -> Nov.27, 2021 이런식으로 최종 출력(O). 추후 아래 highlight 된 warning 좀 수정할것.
        return try{
            val sdf =  SimpleDateFormat("MMM.dd, yyyy") // new SimpleDateFormat 으로 바꾸고 싶은데 API 24 이상부터?
            val netDate = Date(timeInMilSeconds)
            sdf.format(netDate)
        }catch (e:Exception) {
            e.toString()
        }
    }
    fun refreshRecyclerView(newList: List<RtInTheCloud>) {
        //Log.d(TAG, "refreshRecyclerView: @@@@@@@@ currentRtList.size (BEFORE): ${currentRtList.size}")

        val oldList = purchaseBoolTrueList //.map { it.copy() }.toList() // 현재 메모리에 떠있던 rtList 내용물을 받아서 새로운 리스트로 만들어줌.

        //Log.d(TAG, "refreshRecyclerView: oldList.hashcode= ${oldList.hashCode()}, newlist.hashcode=${newList.hashCode()}")
        Log.d(TAG, "refreshRecyclerView: oldList=$oldList, \n\n newList=$newList") //어찌하여 둘이 같은가?!?!
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(PurchasedItemDiffUtilClass(oldList,newList))
        purchaseBoolTrueList = newList
        Log.d(TAG, "refreshRecyclerView: @@@@@@@@ purchaseBoolTrueList.size (AFTER): ${purchaseBoolTrueList.size}")

        diffResult.dispatchUpdatesTo(this)

    }

    // DiffUtil Class
    class PurchasedItemDiffUtilClass(var oldPurchasedTrueList: List<RtInTheCloud>, var newPurchasedTrueList: List<RtInTheCloud>) : DiffUtil.Callback() { // Extend by DiffUtil
        override fun getOldListSize(): Int {
            return oldPurchasedTrueList.size
        }

        override fun getNewListSize(): Int {
            return newPurchasedTrueList.size
        }

        // 1차로 여기서 id 로 판별. (기존 리스트 item 과 새로 받은 리스트 item)
        override fun areItemsTheSame(oldItemPosition: Int,newItemPosition: Int): Boolean { // check if two items represent the same item. 흠.. 다 똑같은지말고 id 만 같아도 true 라는듯..
            //Log.d(TAG, "areItemsTheSame: oldItemPos: $oldItemPosition, newItemPos: $newItemPosition, bool result: ${oldRingToneList[oldItemPosition].id == newRingToneList[newItemPosition].id}")
            return (oldPurchasedTrueList[oldItemPosition].id == newPurchasedTrueList[newItemPosition].id) // id 의존 왜냐면 id is unique and unchangeable.
        }

        // 1차 결과가 true 일때만 불림-> 1차 선발된 놈들을 2차로 여기서 아예 동일한 놈인지(data 로 파악) 판명.
        override fun areContentsTheSame(oldItemPosition: Int,newItemPosition: Int): Boolean { // 모든 field 가 아예 똑같은건지 확인! (id/url/image 등등)

            return (oldPurchasedTrueList[oldItemPosition] == newPurchasedTrueList[newItemPosition])
        }

    }
    // ViewHolder
    inner class PurchaseVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView) {
        val tvPurchaseDate: TextView = xmlToView.findViewById(R.id.tv_purchased_date)
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_purchased_rtTitle)
        val tvOrderId: TextView = xmlToView.findViewById(R.id.tv_purchased_orderId)
        val tvPrice: TextView = xmlToView.findViewById(R.id.tv_purchased_price_paid)
        val ivAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_purchased_albumArt)
    }

}