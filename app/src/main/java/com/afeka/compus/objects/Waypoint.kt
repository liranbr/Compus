package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Waypoint(
    @SerializedName("id") private val id: String,
    @SerializedName("place_id") private val placeId: String,
    @SerializedName("area_id") private val areaId: String) {

    fun getId() = id
    fun getPlaceId() = placeId
    fun getAreaId() = areaId

    override fun toString(): String {
        return "Waypoint: id=$id, place=$placeId, area=$areaId"
    }

}