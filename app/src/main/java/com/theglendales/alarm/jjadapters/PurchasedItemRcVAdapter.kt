package com.theglendales.alarm.jjadapters

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
class PurchasedItemRcVAdapter(private val receivedActivity: Activity,
                              private var purchaseBoolTrueList: List<RtInTheCloud>) : RecyclerView.Adapter<PurchasedItemRcVAdapter.PurchaseVHolder>() {

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


    inner class PurchaseVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView) {
        val tvPurchaseDate: TextView = xmlToView.findViewById(R.id.tv_purchased_date)
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_purchased_rtTitle)
        val tvOrderId: TextView = xmlToView.findViewById(R.id.tv_purchased_orderId)
        val tvPrice: TextView = xmlToView.findViewById(R.id.tv_purchased_price_paid)
        val ivAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_purchased_albumArt)
    }

}