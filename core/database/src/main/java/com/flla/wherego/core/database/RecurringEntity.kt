package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flla.wherego.core.model.RecurringRule

@Entity(
    tableName = "recurring_rules",
    indices = [Index("nextOn")],
)
data class RecurringEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val note: String,
    val freq: String,
    val interval: Int,
    val dayOfMonth: Int?,
    val weekday: Int?,
    val startOn: String,
    val endOn: String?,
    val nextOn: String,
    val remindDaysBefore: Int,
    val autoPost: Boolean,
    val updatedAt: Long,
) {
    fun toModel(): RecurringRule = RecurringRule(
        id, kind, amountMinor, currency, categoryId, note, freq, interval,
        dayOfMonth, weekday, startOn, endOn, nextOn, remindDaysBefore, autoPost, updatedAt,
    )

    companion object {
        fun from(model: RecurringRule): RecurringEntity = RecurringEntity(
            id = model.id,
            kind = model.kind,
            amountMinor = model.amountMinor,
            currency = model.currency,
            categoryId = model.categoryId,
            note = model.note,
            freq = model.freq,
            interval = model.interval,
            dayOfMonth = model.dayOfMonth,
            weekday = model.weekday,
            startOn = model.startOn,
            endOn = model.endOn,
            nextOn = model.nextOn,
            remindDaysBefore = model.remindDaysBefore,
            autoPost = model.autoPost,
            updatedAt = model.updatedAt,
        )
    }
}
