package com.wavehouse.core.di

import android.content.Context
import androidx.room.Room
import com.wavehouse.data.local.db.WaveHouseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaveHouseDatabase =
        Room.databaseBuilder(
            context,
            WaveHouseDatabase::class.java,
            WaveHouseDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // Use proper migrations in production
            .build()

    @Provides
    fun provideProductDao(db: WaveHouseDatabase) = db.productDao()

    @Provides
    fun provideStockDao(db: WaveHouseDatabase) = db.stockDao()

    @Provides
    fun provideStockEntryDao(db: WaveHouseDatabase) = db.stockEntryDao()

    @Provides
    fun provideSupplierDao(db: WaveHouseDatabase) = db.supplierDao()
}
