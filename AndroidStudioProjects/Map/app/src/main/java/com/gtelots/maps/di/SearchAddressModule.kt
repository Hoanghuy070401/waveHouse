package com.gtelots.maps.di

import com.gtelots.maps.data.SearchAddressService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SearchAddressModule {

    @Provides
    @Singleton
    fun searchAddressService(retrofit: Retrofit): SearchAddressService {
        return retrofit.create(SearchAddressService::class.java)
    }
}