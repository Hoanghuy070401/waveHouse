package com.gtelots.maps.utils

sealed class State<out T : Any> {
    data object Loading : State<Nothing>()
    data class Success<out T : Any>(val data: T) : State<T>()
    data class Error(val throwable: Throwable) : State<Nothing>()
}
