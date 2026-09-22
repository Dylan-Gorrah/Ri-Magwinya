package com.rimagwinya.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        MenuItemEntity::class,
        OptionGroupEntity::class,
        OptionEntity::class,
        SlotEntity::class,
        OrderEntity::class,
        OrderLineEntity::class,
        CartLineEntity::class,
        PendingActionEntity::class,
        WeatherEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class RimagwinyaDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun slotDao(): SlotDao
    abstract fun orderDao(): OrderDao
    abstract fun cartDao(): CartDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun weatherDao(): WeatherDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RimagwinyaDatabase =
        Room.databaseBuilder(context, RimagwinyaDatabase::class.java, "rimagwinya.db")
            // Everything here is a cache or a queue the app can rebuild, so
            // a schema change throws it away rather than shipping migrations
            // for data the server already has. The queue is the one thing
            // that could be lost, which is why a release must not change
            // this schema without thinking about it.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun menuDao(db: RimagwinyaDatabase) = db.menuDao()
    @Provides fun slotDao(db: RimagwinyaDatabase) = db.slotDao()
    @Provides fun orderDao(db: RimagwinyaDatabase) = db.orderDao()
    @Provides fun cartDao(db: RimagwinyaDatabase) = db.cartDao()
    @Provides fun pendingActionDao(db: RimagwinyaDatabase) = db.pendingActionDao()
    @Provides fun weatherDao(db: RimagwinyaDatabase) = db.weatherDao()
}
