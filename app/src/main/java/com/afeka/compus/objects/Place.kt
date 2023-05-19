package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Place(
    @SerializedName("place_name") private val placeName: String,
    @SerializedName("areas") private val areas: List<Area>) {

    fun getPlaceName() = placeName
    fun getAreas() = areas

    override fun toString(): String {
        if (areas.isEmpty())
            return "Place: $placeName \nAreas: None"
        val areasStr = areas.joinToString("\n") { it.toString() }
        return "Place: $placeName \nAreas:\n$areasStr"
    }
}