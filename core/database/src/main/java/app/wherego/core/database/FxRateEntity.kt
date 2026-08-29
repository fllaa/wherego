package app.wherego.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "fx_rates")
data class FxRateEntity(
    @PrimaryKey val currency: String,
    val rateToBase: String,
    val fetchedAt: Long,
)

@Dao
interface FxRateDao {
    @Query("SELECT * FROM fx_rates WHERE currency = :currency LIMIT 1")
    suspend fun get(currency: String): FxRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: FxRateEntity)
}
