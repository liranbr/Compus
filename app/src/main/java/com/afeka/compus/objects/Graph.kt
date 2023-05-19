package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Graph(
    @SerializedName("graph_name") private val graphName: String,
    @SerializedName("places") private val places: List<Place>,
    @SerializedName("wps") private val wps: Map<String, Waypoint>,
    @SerializedName("wp_neighs") private val wpNeighs: Map<String, List<String>>,
    @SerializedName("paths") private val paths: Map<String, Path>,
    @SerializedName("poi_wps") private val poiWps: Map<String, String>) {


    fun getGraphName() = graphName
    fun getPlaces() = places
    fun getWps() = wps
    fun getWpNeighs() = wpNeighs
    fun getPaths() = paths
    fun getPoiWps() = poiWps

    override fun toString(): String {
        val placesStr = places.joinToString("\n") { it.toString() }
        val wpsStr = wps.map { "wp_id: ${it.key}, ${it.value}" }.joinToString("\n")
        val wpNeighsStr = wpNeighs.map { "wp_id: ${it.key}, ${it.value}" }.joinToString("\n")
        val pathsStr = paths.map { "path_id: ${it.key}, ${it.value}" }.joinToString("\n")
        val poiWpsStr = poiWps.map { "poi_id: ${it.key}, wp_id: ${it.value}" }.joinToString("\n")
        return "Graph: $graphName \n" +
                "Places: \n$placesStr\n" +
                "Waypoints: \n$wpsStr\n" +
                "Waypoints Neighbors: \n$wpNeighsStr\n" +
                "Paths: \n$pathsStr\n" +
                "POI Waypoints: \n$poiWpsStr"
    }

}