package com.example.pokeitemdle.utils

import org.json.JSONObject

class ItemDetailsFormatter {
    companion object {
        fun format(details: JSONObject): String {
            val name = details.optString("name", "Unknown")
            val cost = details.optInt("cost", 0)
            val category = details.optJSONObject("category")?.optString("name", "Unknown")
            val flingPower =
                if (details.has("fling_power")) details.optInt("fling_power") else "N/A"
            val attributes = details.optJSONArray("attributes")?.let { attrs ->
                (0 until attrs.length()).joinToString(", ") { index ->
                    attrs.getJSONObject(index).optString("name", "Unknown")
                }
            } ?: "No attributes available."

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
                        .optJSONObject("generation")?.optString("name", "Unknown")
                    generationMap[genName] ?: "Unknown"
                } else {
                    "Unknown"
                }
            } ?: "Unknown"

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
            } ?: "No effects available."

            return """
                Nombre: $name
                Categoría: $category
                Coste: $cost
                Fling-power: $flingPower
                Generación: $effectiveGeneration
                Atributos: $attributes
                Effects:
                $effectEntries
            """.trimIndent()
        }
    }
}
