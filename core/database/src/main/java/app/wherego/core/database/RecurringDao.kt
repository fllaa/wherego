package app.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_rules ORDER BY nextOn ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_rules WHERE nextOn <= :today ORDER BY nextOn ASC")
    fun observeDue(today: String): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun listAll(): List<RecurringEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun get(id: String): RecurringEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: RecurringEntity)

    @Update
    suspend fun update(row: RecurringEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: String)
}
