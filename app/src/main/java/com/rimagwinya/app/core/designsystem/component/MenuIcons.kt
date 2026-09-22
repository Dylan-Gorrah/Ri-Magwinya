package com.rimagwinya.app.core.designsystem.component

import androidx.annotation.DrawableRes
import com.rimagwinya.app.R

/**
 * Maps `menu_items.icon_key` to a drawable.
 *
 * The key is data on the row, so staff can add an item from the stock screen
 * and pick its icon without an app release. Anything unrecognised falls back
 * to the box rather than crashing or showing nothing.
 */
object MenuIcons {

    private val byKey: Map<String, Int> = mapOf(
        "wors" to R.drawable.ic_food_wors,
        "vetkoek" to R.drawable.ic_food_vetkoek,
        "chips" to R.drawable.ic_food_chips,
        "packet" to R.drawable.ic_food_packet,
        "triangle" to R.drawable.ic_food_triangle,
        "sweet" to R.drawable.ic_food_sweet,
        "can" to R.drawable.ic_food_can,
        "bottle" to R.drawable.ic_food_bottle,
        "energy" to R.drawable.ic_food_energy,
        "cup" to R.drawable.ic_food_cup,
    )

    /** Every icon a staff member can choose when adding an item. */
    val keys: List<String> get() = byKey.keys.toList()

    @DrawableRes
    fun drawableFor(key: String?): Int = byKey[key?.lowercase()] ?: R.drawable.ic_box
}
