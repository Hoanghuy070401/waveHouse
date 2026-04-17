package com.wavehouse.core.di

import com.wavehouse.data.remote.firebase.AuthRepositoryImpl
import com.wavehouse.data.remote.firebase.OrderRepositoryImpl
import com.wavehouse.data.remote.firebase.ProductRepositoryImpl
import com.wavehouse.data.remote.firebase.StockRepositoryImpl
import com.wavehouse.data.remote.firebase.SupplierRepositoryImpl
import com.wavehouse.data.remote.firebase.WarehouseRepositoryImpl
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.repository.ProductRepository
import com.wavehouse.domain.repository.StockRepository
import com.wavehouse.domain.repository.SupplierRepository
import com.wavehouse.domain.repository.WarehouseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(impl: SupplierRepositoryImpl): SupplierRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindWarehouseRepository(impl: WarehouseRepositoryImpl): WarehouseRepository
}

