package com.gtelots.maps.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gtelots.maps.data.model.models.StyleModel
import com.ots.myapplication.R
import com.ots.myapplication.databinding.ItemStyleBinding

class StyleAdapter(
) : RecyclerView.Adapter<StyleAdapter.ViewHolder>() {
    var onItemSelected: ((Int, StyleModel) -> Unit)? = null
    private var list = ArrayList<StyleModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStyleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            onItemSelected?.invoke(position,item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setData(listData: ArrayList<StyleModel>) {
        list.clear()
        list.addAll(listData)
        notifyDataSetChanged()
    }
    class ViewHolder(
        var binding: ItemStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StyleModel) {
            binding.tvStyleLaraGISStreets.text = item.name
            binding.imvCheck.background = ContextCompat.getDrawable(binding.root.context,item.backGround)
            if (item.isCheck){
                binding.llBgImage.background = ContextCompat.getDrawable(binding.root.context,R.drawable.bg_border_10_blue)
                binding.tvStyleLaraGISStreets.setTextColor(ContextCompat.getColor(binding.root.context, R.color.blueButton))
            }else{
                binding.llBgImage.background = ContextCompat.getDrawable(binding.root.context,R.color.transparent)
                binding.tvStyleLaraGISStreets.setTextColor(ContextCompat.getColor(binding.root.context, R.color.black))
            }
        }

    }


}

