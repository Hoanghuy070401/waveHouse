package com.gtelots.maps.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.gtelots.maps.data.model.models.StyleModel
import com.gtelots.maps.ui.adapter.StyleAdapter
import com.ots.myapplication.databinding.DialogRejectionBottomSheetBinding

class StyleBottomSheetDialog(val list: ArrayList<StyleModel>):BottomSheetDialogFragment() {
    var onAction: ((Int, StyleModel) -> Unit)? = null
    var onActionAdd: (() -> Unit)? = null
    private lateinit var adapter: StyleAdapter
    private lateinit var binding: DialogRejectionBottomSheetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding= DialogRejectionBottomSheetBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setData()
        setEvent()
    }

    private fun setEvent() {
//        binding.imvClose.setOnClickListener {
//            dismiss()
//        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setData() {
        adapter = StyleAdapter()
        binding.rcv.layoutManager = GridLayoutManager(requireContext(),3)
        binding.rcv.adapter = adapter
        adapter.setData(list)
        Log.d("Listabc",Gson().toJson(list))
        adapter.onItemSelected = { position, model ->
            onAction?.invoke(position, model)
        }
        binding.imvAdd.setOnClickListener {
            onActionAdd?.invoke()
        }

    }

}