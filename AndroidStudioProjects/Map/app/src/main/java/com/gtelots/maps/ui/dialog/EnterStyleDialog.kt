package com.gtelots.maps.ui.dialog

import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.ots.myapplication.databinding.DialogEnterStyleMapBinding

class EnterStyleDialog(context: android.content.Context) :
    Dialog(context) {
    var onAction: ((styleName: String,styleLink: String) -> Unit)? = null
    private var binding: DialogEnterStyleMapBinding = DialogEnterStyleMapBinding.inflate(LayoutInflater.from(context))

    init {
        val window = window
        window?.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        window?.setGravity(Gravity.CENTER)
        setContentView(binding.root)

        binding.btnConfirm.setOnClickListener {
            onAction?.invoke(binding.edtStyleName.text.toString(),binding.edtStyle.text.toString())
        }
    }
}