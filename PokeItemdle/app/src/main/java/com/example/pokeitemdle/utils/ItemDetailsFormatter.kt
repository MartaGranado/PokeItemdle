package com.example.pokeitemdle.utils

import android.content.Context
import com.example.pokeitemdle.R
import org.json.JSONObject

class ItemDetailsFormatter {
    companion object {
        fun format(context: Context, details: JSONObject): String {
            val resources = context.resources
            val name = details.optString("name", resources.getString(R.string.unknown))
            val cost = details.optInt("cost", 0)
            val category = details.optJSONObject("category")?.optString("name", resources.getString(R.string.unknown))
            val flingPower =
                if (details.has("fling_power")) details.optInt("fling_power") else "N/A"
            val attributes = details.optJSONArray("attributes")?.let { attrs ->
                (0 until attrs.length()).joinToString(", ") { index ->
                    attrs.getJSONObject(index).optString("name", resources.getString(R.string.unknown))
                }
            } ?: resources.getString(R.string.no_attributes)

            val generationMap = mapOf(
                "generation-i" to 1,
                "generation-ii" to 2,
                "generation-iii" to 3,
                "generation-iv" to 4,
                "generation-v" to 5,
                "generation-vi" to 6,
                "generation-vii" to 7,
                "generation-viii" to 8,
                "generation-ix" to 9
            )

            val firstGeneration = details.optJSONArray("game_indices")?.let { indices ->
                if (indices.length() > 0) {
                    val genName = indices.getJSONObject(0)
                        .optJSONObject("generation")?.optString("name", resources.getString(R.string.unknown))
                    generationMap[genName] ?: resources.getString(R.string.unknown)
                } else {
                    resources.getString(R.string.unknown)
                }
            } ?: resources.getString(R.string.unknown)

            val customGenerations = mapOf(
                "master-ball" to 1,
                "ultra-ball" to 1,
                "great-ball" to 1
            )
            val effectiveGeneration = customGenerations[name.lowercase()] ?: firstGeneration

            val effectEntries = details.optJSONArray("effect_entries")?.let { effects ->
                (0 until effects.length()).joinToString("\n") { index ->
                    effects.getJSONObject(index).optString("effect", "")
                }
            } ?: resources.getString(R.string.no_effects)

            return """
                ${resources.getString(R.string.name)}: $name
                ${resources.getString(R.string.category)}: $category
                ${resources.getString(R.string.cost)}: $cost
                ${resources.getString(R.string.fling_power)}: $flingPower
                ${resources.getString(R.string.generation)}: $effectiveGeneration
                ${resources.getString(R.string.attributes)}: $attributes
                ${resources.getString(R.string.effects)}:
                $effectEntries
            """.trimIndent()
        }
    }
}
