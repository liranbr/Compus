package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Area(
    @SerializedName("area_id") private val areaName: String,
    @SerializedName("area_map") private val areaMap: String,
    @SerializedName("wp_ids") private val wpIds: MutableSet<String> = mutableSetOf()){

    fun getAreaId() = areaName
    fun getAreaMap() = areaMap
    fun getWpIds() = wpIds

    override fun toString(): String {
        return "Area: area_id=$areaName, area_map=$areaMap, wp_ids=$wpIds"
    }

}