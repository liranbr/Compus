package com.afeka.compus.utility


import com.afeka.compus.objects.Site
import com.afeka.compus.objects.Waypoint
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("get_site")
    fun getSite(
        @Query("site_name") siteName: String,
    ): Call<Site>

    @GET("shortest_path")
    fun getShortestPath(
        @Query("site_name") siteName: String,
        @Query("poi_start") poiStart: String,
        @Query("poi_end") poiEnd: String,
        @Query("a11y") a11y: String = "WALK"
    ): Call<List<Waypoint>>

    @GET("get_site_images")
    fun getSiteImageURLs(
        @Query("site_name") siteName: String,
    ): Call<Map<String, String>>

    @Multipart
    @POST("upload_report")
    fun uploadReport(
        @Part("reporter_email") reporterEmail: String,
        @Part("description") description: String,
        @Part("wp_id") wpId: String,
        @Part("direction") direction: Int,
        @Part("site_name") siteName: String,
        @Part image: MultipartBody.Part
    ): Call<Void>
}