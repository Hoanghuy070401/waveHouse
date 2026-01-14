package com.gtelots.maps.data.model

import java.io.Serializable

/**
 * Metadata
 */
data class MetaDataModel(
    var currentPage: Int = 0,
    val pageSize: Int = 20,
    val total: Int = 0
) : Serializable {

    fun hasMoreData(): Boolean {
        if (currentPage == 0)
            return true
        return pageSize * currentPage < total
    }

    fun nextPage(): Int {
        currentPage++
        return currentPage

    }
}

