package com.afeka.compus.objects

import com.google.gson.annotations.SerializedName

class Site(
    @SerializedName("site_name") private val siteName: String,
    @SerializedName("graphs") private val graphs: List<Graph>,
    @SerializedName("entrances") private val entrances: Map<String, Map<String, Double>>) {

    fun getSiteName() = siteName
    fun getGraphs() = graphs
    fun getEntrances() = entrances

    override fun toString(): String {
        val entrancesStr = entrances.map { "wp_id: ${it.key}, ${it.value}" }.joinToString("\n")
        val graphsStr = graphs.joinToString("\n") { it.toString() }
        return "Site: $siteName \n" +
                "Entrances: \n$entrancesStr\n" +
                "Graphs: \n$graphsStr"
    }
}