package com.rimagwinya.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** An item with its groups and options, as Room assembles it. */
data class CachedMenuItem(
    @androidx.room.Embedded val item: MenuItemEntity,
    @androidx.room.Relation(entity = OptionGroupEntity::class, parentColumn = "id", entityColumn = "itemId")
    val groups: List<CachedOptionGroup>,
)

data class CachedOptionGroup(
    @androidx.room.Embedded val group: OptionGroupEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "groupId")
    val options: List<OptionEntity>,
)

data class CachedOrder(
    @androidx.room.Embedded val order: OrderEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "orderId")
    val lines: List<OrderLineEntity>,
)

@Dao
interface MenuDao {
    @Transaction
    @Query("SELECT * FROM menu_items ORDER BY sortOrder, name")
    fun observe(): Flow<List<CachedMenuItem>>

    @Transaction
    @Query("SELECT * FROM menu_items ORDER BY sortOrder, name")
    suspend fun load(): List<CachedMenuItem>

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun count(): Int

    /**
     * Replaces the cached menu wholesale, in one transaction, so a failure
     * halfway cannot leave half a menu on screen.
     */
    @Transaction
    suspend fun replace(
        items: List<MenuItemEntity>,
        groups: List<OptionGroupEntity>,
        options: List<OptionEntity>,
    ) {
        clearOptions()
        clearGroups()
        clearItems()
        insertItems(items)
        insertGroups(groups)
        insertOptions(options)
    }

    @Query("UPDATE menu_items SET stockQuantity = :stock, isAvailable = :available WHERE id = :id")
    suspend fun updateStock(id: String, stock: Int, available: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertItems(items: List<MenuItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGroups(groups: List<OptionGroupEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOptions(options: List<OptionEntity>)
    @Query("DELETE FROM menu_items") suspend fun clearItems()
    @Query("DELETE FROM option_groups") suspend fun clearGroups()
    @Query("DELETE FROM options") suspend fun clearOptions()
}

@Dao
interface SlotDao {
    @Query("SELECT * FROM slots WHERE serviceDate = :date ORDER BY startsAt")
    suspend fun forDate(date: String): List<SlotEntity>

    @Transaction
    suspend fun replace(date: String, slots: List<SlotEntity>) {
        clear(date)
        insert(slots)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(slots: List<SlotEntity>)
    @Query("DELETE FROM slots WHERE serviceDate = :date") suspend fun clear(date: String)
}

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY placedAt DESC")
    fun observe(): Flow<List<CachedOrder>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeOne(id: String): Flow<CachedOrder?>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY placedAt DESC")
    suspend fun load(): List<CachedOrder>

    @Transaction
    suspend fun upsert(orders: List<OrderEntity>, lines: List<OrderLineEntity>) {
        insertOrders(orders)
        orders.forEach { clearLines(it.id) }
        insertLines(lines)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertOrders(orders: List<OrderEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLines(lines: List<OrderLineEntity>)
    @Query("DELETE FROM order_lines WHERE orderId = :orderId") suspend fun clearLines(orderId: String)
    @Query("DELETE FROM orders") suspend fun clearAll()
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_lines ORDER BY addedAt")
    fun observe(): Flow<List<CartLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(line: CartLineEntity)
    @Query("DELETE FROM cart_lines WHERE `key` = :key") suspend fun remove(key: String)
    @Query("DELETE FROM cart_lines") suspend fun clear()
}

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt")
    fun observe(): Flow<List<PendingActionEntity>>

    /** Oldest first: an order placed before a stock change is sent first. */
    @Query("SELECT * FROM pending_actions WHERE failureCode IS NULL ORDER BY createdAt")
    suspend fun due(): List<PendingActionEntity>

    @Insert suspend fun insert(action: PendingActionEntity): Long
    @Update suspend fun update(action: PendingActionEntity)
    @Delete suspend fun delete(action: PendingActionEntity)
    @Query("DELETE FROM pending_actions WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM pending_actions") suspend fun clear()
}

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather WHERE id = 1")
    suspend fun get(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(weather: WeatherEntity)
}
