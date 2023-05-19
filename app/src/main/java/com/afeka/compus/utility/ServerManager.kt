package com.afeka.compus.utility
import com.afeka.compus.objects.A11y
import com.afeka.compus.objects.Report
import com.afeka.compus.objects.Site
import com.afeka.compus.objects.Waypoint
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

fun main() {
    val sm = ServerManager()
    sm.testGetters()
}

class ServerManager {
    private val BASE = "http://85.250.108.91:5000/"
    private val gson: Gson = GsonBuilder().registerTypeAdapter(A11y::class.java, A11y.Deserializer()).create()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    private val service: ApiService = retrofit.create(ApiService::class.java)

    private fun errorMessage(code: Int, response: ResponseBody?): String {
        if (response == null)
            return "Error $code: \nNo error body"
        var errorBody = response.string() // reading flask's abort(code, message) format
        val messageStart = errorBody.indexOf("<p>")
        val messageEnd = errorBody.indexOf("</p>")
        if (messageStart != -1 && messageEnd != -1)
            errorBody = errorBody.substring(messageStart + 3, messageEnd)
        return "Error $code: \n$errorBody"
    }

    private fun execute(call: Call<*>, retries: Int = 3): Response<*> {
        return try {
            call.execute()
        } catch (e: Exception) {
            if (retries > 0) {
                Thread.sleep(500)
                println("Retrying the call, retries: $retries")
                execute(call.clone(), retries - 1)
            } else {
                throw e
            }
        }
    }

    fun getSite(siteName: String): Site? {
        val call = service.getSite(siteName)
        println(call.request().url())
        val response = execute(call)
        return if (response.isSuccessful) (response as Response<Site>).body() else null
    }

    fun getShortestPath(siteName: String, wpIdSrc: String, wpIdDst: String, a11y: String = "WALK"): List<Waypoint>? {
        val call = service.getShortestPath(siteName, wpIdSrc, wpIdDst, a11y)
        println(call.request().url())
        val response = execute(call)
        return if (response.isSuccessful) (response as Response<List<Waypoint>>).body() else null
    }

    fun getSiteImageURLs(siteName: String): Map<String, String>? {
        val call = service.getSiteImageURLs(siteName)
        println(call.request().url())
        val response = execute(call)
        return if (response.isSuccessful) (response as Response<Map<String, String>>).body() else null
    }

    fun uploadReport(report: Report, file : File): Boolean {
        val imageRB = RequestBody.create(
            MediaType.parse("image/jpeg"), file)
        val image = MultipartBody.Part.createFormData("image", file.name, imageRB)
        val response = service.uploadReport(report.getReporterEmail(), report.getText(),
            report.getWpId(), report.getDirection(), report.getSiteName(), image).execute()
        return response.isSuccessful
    }

    fun testGetters() {
        val call1 = service.getSite("Afeka")
        val call2 = service.getSiteImageURLs("Afeka")
        val call3 = service.getShortestPath("Afeka", "location-entrance", "crossroad", "WALK")
        for (call in listOf(call1, call2, call3)) {
            val response = call.execute()
            println(
                if (response.isSuccessful) response.body()
                else errorMessage(response.code(), response.errorBody())
            )
        }
    }

    fun testUploadReport() {
        // example of Upload Report request
        val imageRB = RequestBody.create(
            MediaType.parse("image/jpeg"),
            File("D:\\Folders\\Development\\Repositories\\Compus versions\\test.jpg")
        )
        val image = MultipartBody.Part.createFormData("image", "test.jpg", imageRB)
        val call = service.uploadReport("tester@gmail.com", "description", "304", 0, "Afeka", image)
        println(call.request().url())
        val response = call.execute()
        println(if (response.isSuccessful) response.body()
                else errorMessage(response.code(), response.errorBody())
        )
    }

}