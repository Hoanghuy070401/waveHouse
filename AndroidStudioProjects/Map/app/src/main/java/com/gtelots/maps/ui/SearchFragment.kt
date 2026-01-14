package com.gtelots.maps.ui

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.gtelots.maps.data.model.models.Feature
import com.gtelots.maps.ui.adapter.SearchAdapter
import com.gtelots.maps.ui.viewmodel.SearchViewModel
import com.gtelots.maps.utils.AppUtils
import com.gtelots.maps.utils.State
import com.ots.myapplication.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var binding: FragmentSearchBinding
    private var list = ArrayList<Feature>()
    private lateinit var adapter: SearchAdapter
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    var onItemSelected: ((Feature, Boolean) -> Unit)? = null
    private var location = Location("").apply {
        latitude = 10.8231
        longitude = 106.6297
    }

    companion object {
        const val LOCATION_KEY = "location_key"
        fun newInstance(myLocation: Location): SearchFragment {
            return SearchFragment().apply {
                val args = Bundle()
                args.putParcelable(LOCATION_KEY, myLocation)
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<Location>(LOCATION_KEY)?.let {
            location = it
        }

    }

    private fun setInitView() {
        adapter = SearchAdapter()
        binding.rcvData.layoutManager = LinearLayoutManager(requireContext())
        binding.rcvData.adapter = adapter
        adapter.setData(list)
        adapter.onItemSelected = { model ->
            onItemSelected?.invoke(model, true)
        }
       AppUtils.showKeyboard(requireContext(),binding.edtSearch)
    }

    private fun observer() {
        viewModel.addressSuggestions.observe(viewLifecycleOwner) {
            when (it) {
                is State.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Error occurred: ${it.throwable.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("fjjf1", it.throwable.message.toString())
                }

                State.Loading -> {}
                is State.Success -> {
                    adapter.setData(it.data.features ?: arrayListOf())
                    Log.d("sdhfh", Gson().toJson(it.data.features))
                    if (it.data.features?.isEmpty()==true){
                        binding.rcvData.visibility=View.GONE
                        binding.imvEmpty.visibility=View.VISIBLE
                    }else{
                        binding.rcvData.visibility=View.VISIBLE
                        binding.imvEmpty.visibility=View.GONE
                    }
                }
            }
        }
    }

    private fun setEvent() {
        binding.edtSearch.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                if (text.isNotEmpty()) {
                        Log.d("fjjf", text.length.toString())
                    runnable = Runnable {
                        viewModel.getAddressSuggestList(text.toString())
                    }
                    handler.postDelayed(runnable!!, 1000)
                }
            }
        }
        binding.imvBack.setOnClickListener {
            onItemSelected?.invoke(Feature(), false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInitView()
        setEvent()
        observer()
    }

    fun newInstance(lastLocation: Location?) {
        location = lastLocation ?: location
    }
}

