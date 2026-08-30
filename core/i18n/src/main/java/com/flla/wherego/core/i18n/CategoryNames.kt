package com.flla.wherego.core.i18n

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.model.PresetCategories

@StringRes
private fun presetNameRes(id: String): Int? = when (id) {
    "cat_food_out" -> R.string.category_cat_food_out
    "cat_groceries" -> R.string.category_cat_groceries
    "cat_transport" -> R.string.category_cat_transport
    "cat_rent" -> R.string.category_cat_rent
    "cat_fun" -> R.string.category_cat_fun
    "cat_shopping" -> R.string.category_cat_shopping
    "cat_health" -> R.string.category_cat_health
    "cat_gifts" -> R.string.category_cat_gifts
    "cat_other" -> R.string.category_cat_other
    "cat_bills" -> R.string.category_cat_bills
    "cat_salary" -> R.string.category_cat_salary
    "cat_side" -> R.string.category_cat_side
    "cat_refund" -> R.string.category_cat_refund
    "cat_other_in" -> R.string.category_cat_other_in
    else -> null
}

/**
 * The stored name wins whenever the user has renamed the category. A preset the user has left
 * alone still holds its canonical English seed name, so an exact match against
 * [PresetCategories] means "not renamed" and the localized name is safe to show.
 */
@Composable
@ReadOnlyComposable
fun categoryDisplayName(id: String, storedName: String): String {
    val res = presetNameRes(id) ?: return storedName
    val canonical = PresetCategories.all.firstOrNull { it.id == id }?.name
    return if (storedName == canonical) stringResource(res) else storedName
}
