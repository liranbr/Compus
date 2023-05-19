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
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.view.children
import com.afeka.compus.utility.UtilityMethods
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale
import java.util.Stack


class NavigationActivity : AppCompatActivity() {

    private lateinit var oded: TextToSpeech
    private lateinit var panoramaCarouselViews: Array<ImageView>
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var areaMapView: ImageView
    private lateinit var backBtn: ImageButton
    private lateinit var leftBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var rightBtn: ImageButton
    private lateinit var reportBTN: FloatingActionButton
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    private var isNavigation = false
    private var currentDirection = 0 // 0 = up, 1 = right, 2 = down, 3 = left
    private var fromWherePOI = ""
    private var currentWaypointId = ""
    private var destinationPOI = ""
    private var destWpId = ""
    private val directions = arrayOf("up", "right", "down", "left") // TODO: Need?
    private var shortestPathDirections = IntArray(0)
    private var shortestPath :List<String> ?= null
    private val previousWaypoints = Stack<String>()
    private val waypointImages = HashMap<String, Bitmap>()
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
            fromWherePOI = extras.getString("key0")?.split(", ")?.get(1) ?: ""
            if (extras.containsKey("key1")) {
                destinationPOI = extras.getString("key1")?.split(", ")?.get(1) ?: ""
                if (destinationPOI.isNotEmpty()) {
                    isNavigation = true
                }
            }
            currentWaypointId = graph!!.getPoiWps()[fromWherePOI] ?: ""
            destWpId = graph!!.getPoiWps()[destinationPOI] ?: ""
        }

        // Download panorama images for all wps, load relevant ones into the image carousel TODO: implement download in MainActivity
        MainActivity.imageURLs!!.forEach { entry ->
            Glide.with(this).asBitmap().load(entry.value).into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    waypointImages[entry.key] = resource
                    val innerImageNameParts = entry.key.split("-".toRegex()).toTypedArray()
                    val imageDirection = innerImageNameParts[innerImageNameParts.size - 1]
                    if (entry.key.contains(currentWaypointId)) {
                        panoramaCarouselViews[directions.indexOf(imageDirection)].setImageBitmap(resource)
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
        }

        val place = graph!!.getPlaces().first { it.getPlaceName() == graph!!.getWps()[currentWaypointId]!!.getPlaceId() } // TODO: BUG IS HERE NULL
        //TODO: implement moving between Places
        val areaNames = place.getAreas().map { it.getAreaId()}

        // Setup Toggle Group, a button per area
        for (i in areaNames.indices) {
            val mb = MaterialButton(this)
            mb.text = areaNames[i]
            mb.setPadding(0, 0, 0, 0)
            mb.setTextColor(ContextCompat.getColorStateList(this, R.color.selectable_floor_button_text))
            mb.backgroundTintList = ContextCompat.getColorStateList(this, R.color.selectable_floor_button_background)
            toggleGroup.addView(mb, i)
        }

        for (i in areaNames.indices) {
            val areaName = areaNames[i]
            val url = MainActivity.imageURLs!!["area_map_$areaName"] // TODO: Use area.areaMap
            toggleGroup.getChildAt(i).setOnClickListener {
                Glide.with(this).load(url).placeholder(R.drawable.compus_logo).into(areaMapView)
            }
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
        updateStatus()
        setListeners()
    }

    private fun makeShortestPath() {
        shortestPath = MainActivity.sm.getShortestPath( site!!.getSiteName(),
            fromWherePOI, destinationPOI, MainActivity.a11yMode)?.map { it.getId() } ?: return
        val neighbours = MainActivity.graph!!.getWpNeighs()
        shortestPathDirections = IntArray(shortestPath!!.size - 1)
        for (i in 0 until shortestPath!!.size - 1) {
            shortestPathDirections[i] = neighbours[shortestPath!![i]]!!.indexOf(shortestPath!![i + 1])
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
        panoramaCarouselViews = arrayOf(
            findViewById(R.id.Navigation_IMG_Indoor_up),
            findViewById(R.id.Navigation_IMG_Indoor_right),
            findViewById(R.id.Navigation_IMG_Indoor_down),
            findViewById(R.id.Navigation_IMG_Indoor_left))
        viewFlipper = findViewById(R.id.Navigation_VF_Indoor)
    }

    private fun getCurrentStep() {
        if (!isNavigation) return
        if (forwardBtn.isEnabled)
            colorButton(forwardBtn, R.color.dark)
        colorButtons(arrayOf(leftBtn, rightBtn, backBtn), R.color.dark)
        val index = shortestPath!!.indexOf(currentWaypointId)
        when (index) {
            -1 -> {
                colorButton(backBtn, R.color.green_500)
                odedAmar("Turn back.")
            }
            shortestPath!!.size - 1 -> {
                // TODO: Improve clarity of having reached destination
                colorButtons(arrayOf(leftBtn, forwardBtn, rightBtn, backBtn), R.color.green_500)
                odedAmar("You have reached your destination.")
            }
            else -> { // still en route
                when (shortestPathDirections[index]) {
                    currentDirection -> {
                        colorButton(forwardBtn, R.color.green_500)
                    }
                    (currentDirection + 1) % 4 -> {
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
        // Update the area map
        val currentAreaId = graph!!.getWps()[currentWaypointId]!!.getAreaId()
        toggleGroup.children.filterIsInstance<MaterialButton>()
            .firstOrNull { it.text == currentAreaId && !it.isChecked }?.performClick()

        // Check if possible to move forward
        val canMoveForward = graph!!.getWpNeighs()[currentWaypointId]!![currentDirection] != ""
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
            val nextVertex = graph!!.getWpNeighs()[currentWaypointId]?.get(currentDirection)
                ?: return@setOnClickListener
            for (i in directions.indices) {
                val imageName = nextVertex + "-" + directions[i]
                if (waypointImages.containsKey(imageName))
                    panoramaCarouselViews[i].setImageBitmap(waypointImages[imageName])
            }
            previousWaypoints.add(currentWaypointId)
            currentWaypointId = nextVertex
            updateStatus()
        }
        backBtn.setOnClickListener {
            // Move to the previous waypoint according to the stack // TODO: grey-out if stack is empty
            if (previousWaypoints.size <= 0) return@setOnClickListener
            val previousVertex = previousWaypoints.peek()
            for (i in directions.indices) {
                val imageName = previousVertex + "-" + directions[i]
                if (waypointImages.containsKey(imageName))
                    panoramaCarouselViews[i].setImageBitmap(waypointImages[imageName])
            }
            currentWaypointId = previousWaypoints.pop()
            updateStatus()
        }
        reportBTN.setOnClickListener {
            val currentImageURL = MainActivity.imageURLs?.get(currentWaypointId + "-" + directions[currentDirection])
            UtilityMethods.switchActivityWithData(this, ReportActivity::class.java,
                currentImageURL!!, currentWaypointId, currentDirection.toString())
        }
    }

    private fun rotateImage(direction: Int) {
        currentDirection += direction
        currentDirection = (currentDirection + 4) % 4
        val imageName = currentWaypointId + "-" + directions[currentDirection]
        if (!waypointImages.containsKey(imageName)) {
            println("Image not found: $imageName")
            return
        }
        if (direction == 1) { // rotate right
            viewFlipper.setInAnimation(this, R.anim.slide_in_right)
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
            viewFlipper.showNext()
        } else if (direction == -1) { // rotate left
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
            viewFlipper.showPrevious()
        }
        else if (direction == 2 || direction == -2) { // rotate right twice, to turn 180
            viewFlipper.setInAnimation(this, R.anim.slide_in_right)
            viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
            viewFlipper.showNext()
            Thread.sleep(500)
            viewFlipper.showNext()
        }
    }

    // oded amer learim yadaim
    private fun odedAmar(text: String) = oded.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)

    private fun colorButton(imageButton: ImageButton, @ColorRes id: Int) = imageButton.setBackgroundColor(ContextCompat.getColor(this, id))

    // color multiple buttons
    private fun colorButtons(imageButtons: Array<ImageButton>, @ColorRes id: Int) {
        for (imageButton in imageButtons)
            imageButton.setBackgroundColor(ContextCompat.getColor(this, id))
    }

}