package com.afeka.compus

import android.os.Bundle
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.afeka.compus.objects.Graph
import com.afeka.compus.objects.Site
import com.afeka.compus.utility.AutoSuggestAdapter
import com.afeka.compus.utility.UtilityMethods
import com.bumptech.glide.Glide


class WhereFromActivity : AppCompatActivity(){

    private lateinit var startNavBtn: Button
    private lateinit var whereFromPlaceQuestion: TextView
    private lateinit var whereFromDestination: TextView
    private lateinit var floorPlan: ImageView
    private lateinit var whereFromACTV: AutoCompleteTextView
    private lateinit var site: Site
    private lateinit var graph: Graph
    private var destination: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_from_menu)
        findViews()

        val extras = intent.extras
        site = MainActivity.site!!
        graph = MainActivity.graph!!
        if (extras != null) {
            destination = extras.getString("key0")!!
            val fromWhereQuestion = "From where in ${site.getSiteName()}?"
            whereFromPlaceQuestion.text = fromWhereQuestion
            whereFromDestination.text = destination
        }

        whereFromACTV.setAdapter(AutoSuggestAdapter(
            this, android.R.layout.simple_list_item_1, MainActivity.POIsWPs!!.keys.toMutableList()))
        whereFromACTV.threshold = 1

        setListeners()
    }

    private fun setListeners() {
        whereFromACTV.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            UtilityMethods.closeKeyboard(this@WhereFromActivity, whereFromACTV)
            val selectedPOI = parent.getItemAtPosition(position) as String
            whereFromACTV.setText(selectedPOI)
            // POI -> WP_ID-> WP -> AreaId -> URL
            val wpId = MainActivity.POIsWPs!![selectedPOI]
            val areaMapName = "area_map_" + graph.getWps()[wpId]?.getAreaId()
            val url = MainActivity.imageURLs!![areaMapName]

            Glide.with(this@WhereFromActivity).load(url).placeholder(R.drawable.compus_logo).into(floorPlan)
        }

        startNavBtn.setOnClickListener {
            val fromWhere = whereFromACTV.text.toString()
            if (fromWhere.isNotEmpty() && fromWhere in MainActivity.POIsWPs!!.keys) {
                UtilityMethods.switchActivityWithData(this@WhereFromActivity, NavigationActivity::class.java, fromWhere, destination)
            } else {
                whereFromACTV.error = "Please select a starting point"
            }
        }
    }

    private fun findViews() {
        whereFromPlaceQuestion = findViewById(R.id.WhereFrom_TXT_current_location)
        whereFromDestination = findViewById(R.id.WhereFrom_BOX_destination)
        whereFromACTV = findViewById(R.id.WhereFrom_ACTV_current_location)
        floorPlan = findViewById(R.id.WhereFrom_IMG_map)
        startNavBtn = findViewById(R.id.WhereFrom_BTN_startNav)
    }
}