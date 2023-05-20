package com.afeka.compus

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.afeka.compus.objects.Graph
import com.afeka.compus.objects.Site
import com.bumptech.glide.Glide
import androidx.annotation.ColorRes
import androidx.core.view.children
import com.afeka.compus.objects.Area
import com.afeka.compus.objects.Place
import com.afeka.compus.utility.UtilityMethods
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale
import java.util.Stack


class NavigationActivity : AppCompatActivity() {

    private lateinit var oded: TextToSpeech
    private lateinit var carouselViews: Array<ImageView>
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var areaMapView: ImageView
    private lateinit var backBtn: ImageButton
    private lateinit var leftBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var rightBtn: ImageButton
    private lateinit var reportBTN: FloatingActionButton
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    private var isNavigation = false
    private var currDirection = 0 // 0 = up, 1 = right, 2 = down, 3 = left
    private var startPointPOI = ""
    private var currWpId = ""
    private lateinit var currArea: Area
    private lateinit var currPlace: Place
    private var destPOI = ""
    private var startWpId = ""
    private var destWpId = ""
    private var wpImages: HashMap<String, Bitmap> = MainActivity.imageBitmaps!!
    private val directions = arrayOf("up", "right", "down", "left") // TODO: Need?
    private var shortPathDirections = IntArray(0)
    private var shortPathWpIds: List<String> ?= null
    private val prevWps = Stack<String>()
    private var reachedDest = false
    private var site: Site? = null
    private var graph: Graph? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        site = MainActivity.site
        graph = MainActivity.graph
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigation_screen)
        findViews()
        val extras = intent.extras
        if (extras != null) {
            startPointPOI = extras.getString("key0")?.split(", ")?.get(1) ?: ""
            if (extras.containsKey("key1")) {
                destPOI = extras.getString("key1")?.split(", ")?.get(1) ?: ""
                if (destPOI.isNotEmpty()) {
                    isNavigation = true
                }
            }
            startWpId = graph!!.getPoiWps()[startPointPOI] ?: ""
            currWpId = startWpId
            destWpId = graph!!.getPoiWps()[destPOI] ?: ""
        }
        // put an image in each carousel view, from wpImages from mainActivity
        carouselViews.forEachIndexed { i, imageView ->
            imageIntoView("${currWpId}-${directions[i]}", imageView)
        }

        if (isNavigation) {
            makeShortestPath()
            oded = TextToSpeech(applicationContext) { status ->
                if (status != TextToSpeech.ERROR) {
                    oded.language = Locale.US
                    odedAmar("Lets start navigating")
                }
            }
        }
        updatePlace()
        updateStatus()
        setListeners()
    }

    private fun updatePlace() {
        currPlace = placeFromWp(currWpId)
        val areaNames = currPlace.getAreas().map { it.getAreaId()}
        // Set up area map toggle buttons, currently invisible
        toggleGroup.removeAllViews()
        areaNames.forEach { toggleGroup.addView(MaterialButton(this).apply { text = it }) }
        for (i in areaNames.indices) {
            toggleGroup.getChildAt(i).setOnClickListener {
                imageIntoView("area_map_${areaNames[i]}", areaMapView)
            }
        }
    }
    private fun makeShortestPath() {
        shortPathWpIds = MainActivity.sm.getShortestPath( site!!.getSiteName(),
            startWpId, destWpId, MainActivity.a11yMode)?.map { it.getId() } ?: return
        val neighbours = MainActivity.graph!!.getWpNeighs()
        shortPathDirections = IntArray(shortPathWpIds!!.size - 1)
        for (i in 0 until shortPathWpIds!!.size - 1) {
            shortPathDirections[i] = neighbours[shortPathWpIds!![i]]!!.indexOf(shortPathWpIds!![i + 1])
        }
    }

    private fun findViews() {
        areaMapView = findViewById(R.id.Navigation_IMG_Floor)
        backBtn = findViewById(R.id.Navigation_IMGBTN_back)
        leftBtn = findViewById(R.id.Navigation_IMGBTN_left)
        forwardBtn = findViewById(R.id.Navigation_IMGBTN_up)
        rightBtn = findViewById(R.id.Navigation_IMGBTN_right)
        reportBTN = findViewById(R.id.Navigation_FAB_report)
        toggleGroup = findViewById(R.id.Navigation_MBTG_navigation)
        carouselViews = arrayOf(
            findViewById(R.id.Navigation_IMG_Indoor_up),
            findViewById(R.id.Navigation_IMG_Indoor_right),
            findViewById(R.id.Navigation_IMG_Indoor_down),
            findViewById(R.id.Navigation_IMG_Indoor_left))
        viewFlipper = findViewById(R.id.Navigation_VF_Indoor)
    }

    private fun getCurrentStep() {
        if (!isNavigation) return
        if (reachedDest) return
        if (forwardBtn.isEnabled)
            colorButton(forwardBtn, R.color.dark)
        colorButtons(arrayOf(leftBtn, rightBtn, backBtn), R.color.dark)
        when (val index = shortPathWpIds!!.indexOf(currWpId)) {
            -1 -> {
                colorButton(backBtn, R.color.green_500)
                odedAmar("Turn back.")
            }
            shortPathWpIds!!.size - 1 -> {
                // TODO: Improve clarity of having reached destination
                colorButtons(arrayOf(leftBtn, forwardBtn, rightBtn, backBtn), R.color.green_500)
                odedAmar("You have reached your destination.")
                reachedDest = true
            }
            else -> { // still en route
                when (shortPathDirections[index]) {
                    currDirection -> {
                        colorButton(forwardBtn, R.color.green_500)
                    }
                    (currDirection + 1) % 4 -> {
                        colorButton(rightBtn, R.color.green_500)
                        odedAmar("Turn right.")
                    }
                    else -> {
                        colorButton(leftBtn, R.color.green_500)
                        odedAmar("Turn left.")
                    }
                }
            }
        }
    }

    private fun updateStatus() {
        // Update the place, and area buttons
        if (graph!!.getWps()[currWpId]!!.getPlaceId() != currPlace.getPlaceName())
            updatePlace()
        // Update the area map
        val currentAreaId = graph!!.getWps()[currWpId]!!.getAreaId()
        toggleGroup.children.filterIsInstance<MaterialButton>()
            .firstOrNull { it.text == currentAreaId && !it.isChecked }?.performClick()

        // Check if possible to move forward
        val canMoveForward = graph!!.getWpNeighs()[currWpId]!![currDirection] != ""
        val color = if (canMoveForward) R.color.dark else R.color.med_dark
        forwardBtn.setBackgroundColor(ContextCompat.getColor(this, color))
        forwardBtn.isEnabled = canMoveForward

        // If in Navigation mode, update the current step
        if (isNavigation) getCurrentStep()
    }

    private fun setListeners() {
        leftBtn.setOnClickListener {
            rotateImage(-1) // rotate left
            updateStatus()
        }
        rightBtn.setOnClickListener {
            rotateImage(1) // rotate right
            updateStatus()
        }
        forwardBtn.setOnClickListener {
            // move forward, to the next vertex in the graph
            val nextVertex = graph!!.getWpNeighs()[currWpId]?.get(currDirection)
                ?: return@setOnClickListener
            for (i in directions.indices) {
                val imageName = nextVertex + "-" + directions[i]
                imageIntoView(imageName, carouselViews[i])
            }
            prevWps.add(currWpId)
            currWpId = nextVertex
            updateStatus()
        }
        backBtn.setOnClickListener {
            // Move to the previous waypoint according to the stack // TODO: grey-out if stack is empty
            if (prevWps.size <= 0) return@setOnClickListener
            val previousVertex = prevWps.peek()
            for (i in directions.indices)
                imageIntoView(previousVertex + "-" + directions[i], carouselViews[i])
            currWpId = prevWps.pop()
            updateStatus()
        }
        reportBTN.setOnClickListener {
            val currentImageURL = MainActivity.imageURLs?.get(currWpId + "-" + directions[currDirection])
            UtilityMethods.switchActivityWithData(this, ReportActivity::class.java,
                currentImageURL!!, currWpId, currDirection.toString())
        }
    }

    private fun imageIntoView(imageName: String, imageView: ImageView) {
        if (wpImages.containsKey(imageName))
            imageView.setImageBitmap(wpImages[imageName])
        else
            Glide.with(this).load(MainActivity.imageURLs!![imageName]).placeholder(R.drawable.compus_background).into(imageView)
        // TODO: replace Placeholder with a loading animation?
    }

    private fun rotateImage(direction: Int) {
        currDirection += direction
        currDirection = (currDirection + 4) % 4
        val imageName = currWpId + "-" + directions[currDirection]
        if (!wpImages.containsKey(imageName)) {
            println("Image not found: $imageName")
            return
        }
        when (direction) {
            1 -> { // rotate right
                viewFlipper.setInAnimation(this, R.anim.slide_in_right)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
                viewFlipper.showNext()
            }
            -1 -> { // rotate left
                viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
                viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
                viewFlipper.showPrevious()
            }
            2, -2 -> { // rotate right twice, to turn 180
                viewFlipper.setInAnimation(this, R.anim.slide_in_right)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
                viewFlipper.showNext()
                Thread.sleep(500)
                viewFlipper.showNext()
            }
        }
    }

    // oded amer learim yadaim
    private fun odedAmar(text: String) {
        if (!reachedDest)
            oded.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun colorButton(imageButton: ImageButton, @ColorRes id: Int) = imageButton.setBackgroundColor(ContextCompat.getColor(this, id))

    // color multiple buttons
    private fun colorButtons(imageButtons: Array<ImageButton>, @ColorRes id: Int) {
        for (imageButton in imageButtons)
            imageButton.setBackgroundColor(ContextCompat.getColor(this, id))
    }

    private fun placeFromWp(wpId: String): Place {
        return graph!!.getPlaces().first { it.getPlaceName() == graph!!.getWps()[wpId]!!.getPlaceId() }
    }


}