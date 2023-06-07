package com.afeka.compus

import androidx.appcompat.app.AppCompatActivity
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.afeka.compus.objects.Site
import com.afeka.compus.utility.UtilityMethods

class GPSArrivalActivity: AppCompatActivity() {
    private lateinit var place_text: TextView
    private lateinit var next_button: Button
    private lateinit var waze_IMG: ImageView
    private lateinit var google_Maps_IMG: ImageView
    private lateinit var moovit_IMG: ImageView
    private var fullDestination = ""
    private var destination = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gps_arrival_menu)
        findViews()
        val extras = intent.extras
        if (extras != null) {
            fullDestination = extras.getString("key0", "")
            destination = fullDestination
            place_text.text = destination
        }
        setListeners()
    }

    private fun setListeners() {
        next_button.setOnClickListener {
            UtilityMethods.switchActivityWithData(this, NavigationActivity::class.java,
                "Ficus, Gate", fullDestination) // TODO: update whole function after implementing multiple entrances UI
            finish()
        }

        val site: Site = MainActivity.site!!
        val latLng = site.getEntrances().entries.first().value
        val lat = latLng["latitude"]!!
        val lon = latLng["longitude"]!!

        waze_IMG.setOnClickListener { searchWaze(destination, lat, lon) }
        google_Maps_IMG.setOnClickListener { searchGoogleMaps(destination, lat, lon) }
        moovit_IMG.setOnClickListener { searchMoovit(destination, lat, lon) }
    }

    // Deep link to Waze
    private fun searchWaze(dst: String, lat: Double, lon: Double) {
        try {
            val url = "https://waze.com/ul?ll=$lat,$lon&navigate=yes"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            // If Waze is not installed, open it in Google Play:
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"))
            startActivity(intent)
        }
    }

    // Deep link to Moovit
    private fun searchMoovit(dst: String, lat: Double, lon: Double) {
        val url = "moovit://directions?dest_lat=$lat&dest_lon=$lon&dest_name=$dst" // if bug, might not be installed, check
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            // If Moovit is not installed, open it in Google Play:
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.tranzmate"))
            startActivity(intent)
        }
    }

    // Deep link to Google Maps
    private fun searchGoogleMaps(dst: String, lat: Double, lon: Double) {
        val url = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lon"
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    private fun findViews() {
        place_text = findViewById(R.id.Arrival_BOX_place)
        next_button = findViewById(R.id.Arrival_BTN_next)
        waze_IMG = findViewById(R.id.Arrival_IMG_waze)
        google_Maps_IMG = findViewById(R.id.Arrival_IMG_google_maps)
        moovit_IMG = findViewById(R.id.Arrival_IMG_moovit)
    }
}