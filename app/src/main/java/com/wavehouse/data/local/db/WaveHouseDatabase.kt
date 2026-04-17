package com.wavehouse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wavehouse.data.local.dao.ProductDao
import com.wavehouse.data.local.dao.StockDao
import com.wavehouse.data.local.dao.StockEntryDao
import com.wavehouse.data.local.dao.SupplierDao
import com.wavehouse.data.local.entity.ProductEntity
import com.wavehouse.data.local.entity.StockEntryEntity
import com.wavehouse.data.local.entity.StockItemEntity
import com.wavehouse.data.local.entity.SupplierEntity

@Database(
    entities = [
        ProductEntity::class,
        StockItemEntity::class,
        StockEntryEntity::class,
        SupplierEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WaveHouseDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun stockEntryDao(): StockEntryDao
    abstract fun supplierDao(): SupplierDao

    companion object {
        const val DATABASE_NAME = "wavehouse_db"
    }
}
