package com.afeka.compus

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afeka.compus.objects.Graph
import com.afeka.compus.objects.Site
import com.afeka.compus.utility.AutoSuggestAdapter
import com.afeka.compus.utility.ServerManager
import com.afeka.compus.utility.UtilityMethods.Companion.switchActivityWithData
import com.afeka.compus.utility.UtilityMethods.Companion.closeKeyboard
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {

    enum class Modes {
        NAVIGATION,
        LOOKAROUND
    }
    lateinit var destInputHintText: TextView
    lateinit var startPointChoiceText: TextView
    lateinit var destInput: TextInputLayout
    lateinit var amNotAtSiteBTN: Button
    lateinit var amAtSiteBTN: Button
    lateinit var modeChoiceTG: MaterialButtonToggleGroup
    lateinit var autoCompleteTV: AutoCompleteTextView
    lateinit var a11yCheckBox: CheckBox

    private var currentMode = Modes.LOOKAROUND // default mode
    private var destWpId = ""
    companion object {
        var site : Site? = null
        var graph: Graph? = null
        var POIsWPs: Map<String, String>? = null
        var imageURLs : Map<String, String>? = null // TODO: remove ? and initialize
        var imageBitmaps = HashMap<String, Bitmap>()
        var sm = ServerManager()
        var a11yMode = "WALK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // disable dark mode
        var policy = ThreadPolicy.Builder().permitAll().build() // synchronous network calls
        StrictMode.setThreadPolicy(policy) // makes the app wait until site is loaded, which is a short duration

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)
        FirebaseApp.initializeApp(this)
        findViews()
        setListeners()
        setPromptState(false)

        val siteName = "Afeka" // TODO: replace these two with user input
        val graphName = "Campus"
        site = sm.getSite(siteName)
        graph = site!!.getGraphs().find { it.getGraphName() == graphName }

        POIsWPs = graph!!.getPoiWps().map { graph!!.getWps()[it.value]!!.getPlaceId() +
                ", " + it.key to it.value }.toMap() // 'Class 304' -> 'Ficus, Class 304'
        autoCompleteTV.setAdapter(AutoSuggestAdapter(this, android.R.layout.simple_list_item_1, POIsWPs!!.keys.toList()))
        autoCompleteTV.threshold = 1
        modeChoiceTG.check(R.id.directionsMode)
        imageURLs = sm.getSiteImageURLs(siteName)
        downloadImages()
    }

    // function to show/hide and enable/disable buttons
    private fun setPromptState(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        amNotAtSiteBTN.isEnabled = enabled
        amNotAtSiteBTN.visibility = visibility
        amAtSiteBTN.isEnabled = enabled
        amAtSiteBTN.visibility = visibility
        startPointChoiceText.text = if (enabled) "Are you currently at ${site!!.getSiteName()}?" else ""
    }

    private fun downloadImages() {
        imageURLs!!.forEach { entry ->
            Glide.with(this).asBitmap().load(entry.value).placeholder(R.drawable.compus_logo).into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageBitmaps!![entry.key] = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) {} // TODO: Remove?
            })
        }
    }

    private fun setListeners() {
        modeChoiceTG.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) { // only activate the listener of the currently checked button
                if (autoCompleteTV.text.isNotEmpty()) {
                    // if there is text but not the currently selected item, or LookAround mode, show drop down
                    if (autoCompleteTV.text.toString() != destWpId || checkedId == R.id.lookaroundMode)
                        autoCompleteTV.showDropDown()
                    else
                        setPromptState(true)
                }
                if (checkedId == R.id.directionsMode) {
                    currentMode = Modes.NAVIGATION
                    destInputHintText.text = "Where to in ${site!!.getSiteName()}?"
                    a11yCheckBox.visibility = View.VISIBLE
                }
                else if (checkedId == R.id.lookaroundMode) {
                    currentMode = Modes.LOOKAROUND
                    destInputHintText.text = "Look where in ${site!!.getSiteName()}?"
                    a11yCheckBox.visibility = View.INVISIBLE
                    setPromptState(false)
                }
            }
        }

        autoCompleteTV.setOnItemClickListener { parent, view, position, id ->
            closeKeyboard(this, autoCompleteTV)
            val selectedItem = parent.getItemAtPosition(position) as String
            autoCompleteTV.setText(selectedItem)
            destWpId = selectedItem
            if (currentMode == Modes.NAVIGATION) {
                setPromptState(true)
            }
            else if (currentMode == Modes.LOOKAROUND) {
                println("Destination: $destWpId") // TODO: Rename destination
                switchActivityWithData(this, NavigationActivity::class.java, destWpId)
            }
        }
        destInput.setEndIconOnClickListener {
            autoCompleteTV.setText("")
            setPromptState(false)
        }
        a11yCheckBox.setOnClickListener {
            a11yMode = if (a11yCheckBox.isChecked) "WHEELCHAIR" else "WALK"
        }
        amNotAtSiteBTN.setOnClickListener {
            switchActivityWithData(this, GPSArrivalActivity::class.java, destWpId)
        }
        amAtSiteBTN.setOnClickListener {
            switchActivityWithData(this, WhereFromActivity::class.java, destWpId)
        }
    }

    private fun findViews() {
        destInputHintText = findViewById(R.id.textView2)
        modeChoiceTG = findViewById(R.id.toggleButton)
        amNotAtSiteBTN = findViewById(R.id.reach_place)
        amAtSiteBTN = findViewById(R.id.im_at_place)
        autoCompleteTV = findViewById(R.id.TXT_ACV_Search)
        destInput = findViewById(R.id.textInputLayout)
        startPointChoiceText = findViewById(R.id.textView3)
        a11yCheckBox = findViewById(R.id.a11y_checkbox)
    }

}