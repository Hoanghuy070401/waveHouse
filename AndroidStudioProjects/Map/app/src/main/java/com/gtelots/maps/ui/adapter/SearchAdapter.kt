package com.gtelots.maps.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gtelots.maps.data.model.models.Feature
import com.ots.myapplication.databinding.ItemSearchBinding

class SearchAdapter( // Pass the listener in the constructor
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var list = ArrayList<Feature>()
    var onItemSelected: ((Feature) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemSelected?.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setData(listData: List<Feature>) {
        list.clear()
        list.addAll(listData)
        notifyDataSetChanged()
    }
    class ViewHolder(private var binding: ItemSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Feature) {
            binding.tvTile.text = item.properties?.name?:""
            binding.tvAddress.text = item.properties?.address?:""
//            binding.tvRange.text = AppUtils.convertMetersToKilometers(item.distance?.toDouble()?:0.0)
        }
    }

}

