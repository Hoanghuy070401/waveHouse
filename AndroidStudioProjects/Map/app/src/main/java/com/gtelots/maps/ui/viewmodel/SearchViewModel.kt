package com.gtelots.maps.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gtelots.maps.data.SearchAddressApiHelper
import com.gtelots.maps.data.model.models.ResponseDetailModel
import com.gtelots.maps.data.model.models.SuggestAddressModel
import com.gtelots.maps.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import javax.inject.Inject


@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiHelper: SearchAddressApiHelper

) : ViewModel() {
    private val _addressSuggestions = MutableLiveData<State<SuggestAddressModel>>()
    val addressSuggestions: LiveData<State<SuggestAddressModel>> = _addressSuggestions
    private val _addressDetails = MutableLiveData<State<ResponseDetailModel>>()
    val addressDetails: LiveData<State<ResponseDetailModel>> = _addressDetails
    private val _addressDetailsMarker = MutableLiveData<State<SuggestAddressModel>>()
    val addressDetailsMarker: LiveData<State<SuggestAddressModel>> = _addressDetailsMarker


    fun getAddressSuggestList(q: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _addressSuggestions.postValue(State.Loading)
            if (q.isNotEmpty()){
                val result =
                    apiHelper.getAddressSuggest(q)
                Log.d("result.data", Gson().toJson(result))
                _addressSuggestions.postValue(State.Success(result))
            }

        }
    }

    fun getAddressDetailsList(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _addressDetails.postValue(State.Loading)
            val result =
                apiHelper.getAddressDetails(id)

            Log.d("result.data", Gson().toJson(result))
            _addressDetails.postValue(State.Success(result))
        }
    }
    fun getAddressDetailsMarkers(location: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            _addressDetailsMarker.postValue(State.Loading)
            val result =
                apiHelper.getAddressDetails(location )

            Log.d("result.data", Gson().toJson(result))
            _addressDetailsMarker.postValue(State.Success(result))
        }
    }

}