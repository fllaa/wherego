package app.wherego.core.database

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FxRateStore @Inject constructor(
    private val dao: FxRateDao,
    private val clock: Clock,
) {
    suspend fun rateToBase(currency: String, baseCurrency: String): String {
        if (currency == baseCurrency) return "1"
        return dao.get(currency)?.rateToBase ?: "1"
    }

    suspend fun put(currency: String, rateToBase: String) {
        dao.upsert(FxRateEntity(currency, rateToBase, clock.millis()))
    }
}
