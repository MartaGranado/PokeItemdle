package com.example.pokeitemdle.networking

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RemoteAPI {

    private val BASE_URL = "https://pokeapi.co/api/v2/item" // Pokémon API endpoint
    private val MOVE_URL = "https://pokeapi.co/api/v2/move"
    private val TAG = "API"

    fun getAllItems(onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val allItems = mutableListOf<String>()
                var nextUrl: String? = BASE_URL
                var index = 1 // Índice inicial

                // Definir los rangos de interés
                val validRanges = listOf(1..304, 449..457, 580..614)

                // Loop through all pages of results
                while (nextUrl != null && allItems.size < validRanges.sumOf { it.count() }) {
                    try {
                        val connection = URL(nextUrl).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Accept", "application/json")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val response = StringBuilder()
                        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            reader.forEachLine { response.append(it.trim()) }
                        }

                        Log.d(TAG, "Fetching data from: $nextUrl")

                        val jsonObject = JSONObject(response.toString())
                        val results = jsonObject.getJSONArray("results")
                        nextUrl = jsonObject.optString("next") // Get the next URL, if any

                        // Add items to the list if they fall within the specified ranges
                        for (i in 0 until results.length()) {
                            if (validRanges.any { index in it }) {
                                val item = results.getJSONObject(i).getString("name")
                                allItems.add(item)
                            }
                            index++ // Increment index for each item

                            // Stop processing if we have fetched all required items
                            if (allItems.size >= validRanges.sumOf { it.count() }) {
                                nextUrl = null // Force exit from the loop
                                break
                            }
                        }

                        // Log progress
                        Log.d(TAG, "Fetched ${allItems.size} items so far")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching data from $nextUrl: ${e.localizedMessage}")
                        nextUrl = null // Stop pagination on error
                    }
                }

                // Return filtered items after fetching
                onSuccess(allItems)
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.localizedMessage}")
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }.start()
    }


    fun getItemDetails(itemName: String, onSuccess: (JSONObject) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val connection = URL("$BASE_URL/$itemName").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.forEachLine { response.append(it.trim()) }
                }

                val jsonObject = JSONObject(response.toString())
                onSuccess(jsonObject)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching item details: ${e.localizedMessage}")
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }.start()
    }
    fun getAllMoves(onSuccess: (List<String>) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val allMoves = mutableListOf<String>()
                var nextUrl: String? = MOVE_URL
                var index = 1 // Índice inicial

                // Definir los rangos de interés
                val validRanges = listOf(1..919)

                // Loop through all pages of results
                while (nextUrl != null && allMoves.size < validRanges.sumOf { it.count() }) {
                    try {
                        val connection = URL(nextUrl).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Accept", "application/json")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000

                        val response = StringBuilder()
                        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            reader.forEachLine { response.append(it.trim()) }
                        }

                        Log.d(TAG, "Fetching data from: $nextUrl")

                        val jsonObject = JSONObject(response.toString())
                        val results = jsonObject.getJSONArray("results")
                        nextUrl = jsonObject.optString("next") // Get the next URL, if any

                        // Add items to the list if they fall within the specified ranges
                        for (i in 0 until results.length()) {
                            if (validRanges.any { index in it }) {
                                val move = results.getJSONObject(i).getString("name")
                                allMoves.add(move)
                            }
                            index++ // Increment index for each item

                            // Stop processing if we have fetched all required items
                            if (allMoves.size >= validRanges.sumOf { it.count() }) {
                                nextUrl = null // Force exit from the loop
                                break
                            }
                        }

                        // Log progress
                        Log.d(TAG, "Fetched ${allMoves.size} moves so far")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching data from $nextUrl: ${e.localizedMessage}")
                        nextUrl = null // Stop pagination on error
                    }
                }

                // Return filtered items after fetching
                onSuccess(allMoves)
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.localizedMessage}")
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }.start()
    }
    fun getMoveDetails(moveName: String, onSuccess: (JSONObject) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val connection = URL("$MOVE_URL/$moveName").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.forEachLine { response.append(it.trim()) }
                }

                val jsonObject = JSONObject(response.toString())
                onSuccess(jsonObject)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching item details: ${e.localizedMessage}")
                onError(e.localizedMessage ?: "Unknown Error")
            }
        }.start()
    }

}
