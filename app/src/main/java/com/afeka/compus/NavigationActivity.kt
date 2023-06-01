package com.afeka.compus

import android.animation.Animator
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.view.View
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.children
import com.afeka.compus.objects.Area
import com.afeka.compus.objects.Place
import com.afeka.compus.utility.UtilityMethods
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import java.util.Locale
import java.util.Stack


class NavigationActivity : AppCompatActivity() {

    private lateinit var oded: TextToSpeech
    private lateinit var carouselViews: Array<ImageView>
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var areaMapView: ImageView
    private lateinit var areaBitmap: Bitmap
    private lateinit var backBtn: ImageButton
    private lateinit var leftBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var rightBtn: ImageButton
    private lateinit var reportBTN: FloatingActionButton
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var lottie: LottieAnimationView

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
    private var inputEnabled = true
    private var onThePath = true // always start from starting point

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

            lottie.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    lottie.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animator) {
                    lottie.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })

        }
        // put an image in each carousel view, from wpImages from mainActivity
        carouselViews.forEachIndexed { i, imageView ->
            imageNameIntoView("${currWpId}-${directions[i]}", imageView)
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
        colorButtons(arrayOf(leftBtn, forwardBtn, rightBtn, backBtn), R.color.dark)
        updatePlace()
        updateStatus()
        drawCurrentWp()
        setListeners()
    }

    private fun updatePlace() {
        currPlace = placeFromWp(currWpId)
        currArea = areaFromWp(currWpId)
        updateArea()
    }

    private fun updateArea(){
        currArea = areaFromWp(currWpId)
        areaBitmap = MainActivity.imageBitmaps["area_map_${currArea.getAreaId()}"]!!
        val areaNames = currPlace.getAreas().map { it.getAreaId()}
        // Set up area map toggle buttons, currently invisible
        toggleGroup.removeAllViews()
        areaNames.forEach { toggleGroup.addView(MaterialButton(this).apply { text = it }) }
        for (i in areaNames.indices) {
            toggleGroup.getChildAt(i).setOnClickListener {
                if(isNavigation){
                    areaBitmap = drawPathOnAreaMap(areaNames[i])
                    areaMapView.setImageBitmap(areaBitmap)
                }
                else
                    imageNameIntoView("area_map_${areaNames[i]}", areaMapView)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun drawPathOnAreaMap(area_map_id: String) : Bitmap {
        val bitmap = MainActivity.imageBitmaps["area_map_$area_map_id"]
        val mCanvas = bitmap?.let { Canvas(it) }
        val inWidth = 30F
        val outWidth = 40F
        val inColor = 0xffff9532.toInt()
        val outColor = 0xffcb6200.toInt()

        val lineInner = Paint()
        lineInner.color = inColor
        lineInner.style = Paint.Style.STROKE
        lineInner.strokeWidth = inWidth
        lineInner.isAntiAlias = true
        val lineBorder = Paint(lineInner)
        lineBorder.color = outColor
        lineBorder.strokeWidth = outWidth

        val circleInner = Paint().apply { color = inColor }.apply { style = Paint.Style.FILL }
        val circleBorder = Paint().apply { color = outColor }.apply { style = Paint.Style.FILL }

        val positions = mutableListOf<Pair<Float, Float>>()

        val wpMap = MainActivity.graph?.getWps()
        for (i in 0 until (shortPathWpIds?.size!!)) {
            val wp = wpMap?.get(shortPathWpIds!![i])
            if(wp != null && wp.getAreaId() == currArea.getAreaId()) {
                val pos = wp.getPos()
                if(pos != null){
                    val pair = Pair(pos["x"]!!, pos["y"]!!)
                    positions.add(pair)
                }
            }
        }

        for (i in 0 until positions.size - 1) {
            val startX = positions[i].first
            val startY = positions[i].second
            val stopX = positions[i + 1].first
            val stopY = positions[i + 1].second
            mCanvas?.drawLine(startX, startY, stopX, stopY, lineBorder)
            mCanvas?.drawLine(startX, startY, stopX, stopY, lineInner)
            mCanvas?.drawCircle(startX, startY, outWidth/2 + 5, circleBorder)
            mCanvas?.drawCircle(startX, startY, inWidth/2 + 5, circleInner)
        }
        return bitmap!!
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawCurrentWp() {
//        if(!isNavigation) return

        val bitmap = areaBitmap.copy(areaBitmap.config,true)
        val mCanvas = Canvas(bitmap)
        val inWidth = 65F
        val outWidth = 70F
        val inColor = Color.GREEN
        val outColor = 0xffcb6200.toInt()

        val lineInner = Paint()
        lineInner.color = inColor
        lineInner.style = Paint.Style.STROKE
        lineInner.strokeWidth = inWidth
        lineInner.isAntiAlias = true
        val lineBorder = Paint(lineInner)
        lineBorder.color = outColor
        lineBorder.strokeWidth = outWidth

        val circleInner = Paint().apply { color = inColor }.apply { style = Paint.Style.FILL }
        val circleBorder = Paint().apply { color = outColor }.apply { style = Paint.Style.FILL }

        val pos = MainActivity.graph?.getWps()?.get(currWpId)?.getPos()

        if(pos != null){
            val pair = Pair(pos["x"]!!, pos["y"]!!)
            val pair2 = Pair(pos["x"]!! + 1, pos["y"]!! + 1)
            val startX = pair.first
            val startY = pair.second
            val stopX = pair2.first
            val stopY = pair2.second
            mCanvas.drawLine(startX, startY, stopX, stopY, lineBorder)
            mCanvas.drawLine(startX, startY, stopX, stopY, lineInner)
            mCanvas.drawCircle(startX, startY, outWidth/2 + 5, circleBorder)
            mCanvas.drawCircle(startX, startY, inWidth/2 + 5, circleInner)
        }

        areaMapView.setImageBitmap(bitmap)

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
        lottie = findViewById(R.id.Lottie)
    }

    private fun getCurrentStep() {
        if (!isNavigation) return
        if (forwardBtn.isEnabled)
            colorButton(forwardBtn, R.color.dark)
        colorButtons(arrayOf(leftBtn, rightBtn), R.color.dark) // TODO: Improve code
        val index = shortPathWpIds!!.indexOf(currWpId)
        if (index == -1) { // went off the path
            colorButton(backBtn, R.color.green_500)
            if (onThePath) {
                odedAmar("Go back.") // within an if to not spam it
                onThePath = false
            }
        }
        else { // on the path
            onThePath = true
            // reached destination
            if (index == shortPathWpIds!!.size - 1) {
                colorButtons(arrayOf(leftBtn, forwardBtn, rightBtn, backBtn), R.color.green_500)
                odedAmar("You have reached your destination.")
                lottie.animate().translationX(2000F).setDuration(2000).startDelay = 2900
                if (!reachedDest) // to only play once
                    lottie.playAnimation()
                reachedDest = true
            }
            // still en route
            else {
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
        else if (graph!!.getWps()[currWpId]!!.getAreaId() != currArea.getAreaId())
            updateArea()

        // Update the area map
        val currentAreaId = graph!!.getWps()[currWpId]!!.getAreaId()
        toggleGroup.children.filterIsInstance<MaterialButton>()
            .firstOrNull { it.text == currentAreaId && !it.isChecked }?.performClick()

        // Check if possible to move forward
        val canMoveForward = graph!!.getWpNeighs()[currWpId]!![currDirection] != ""
        var color = if (canMoveForward) R.color.dark else R.color.med_dark
        colorButton(forwardBtn, color)
        forwardBtn.isEnabled = canMoveForward

        // Check if possible to move back
        val canMoveBack = prevWps.isNotEmpty()
        color = if (canMoveBack) R.color.dark else R.color.med_dark
        colorButton(backBtn, color)
        backBtn.isEnabled = canMoveBack

        // If in Navigation mode, update the current step. Might further set colors, to green etc
        if (isNavigation) getCurrentStep()
    }

    private fun setListeners() {
        leftBtn.setOnClickListener {
            if (!inputEnabled) return@setOnClickListener
            rotateImageBy(-1) // rotate left
            updateStatus()
        }
        rightBtn.setOnClickListener {
            if (!inputEnabled) return@setOnClickListener
            rotateImageBy(1) // rotate right
            updateStatus()
        }
        forwardBtn.setOnClickListener {
            if (!inputEnabled) return@setOnClickListener
            // move forward, to the next vertex in the graph
            val nextVertex = graph!!.getWpNeighs()[currWpId]?.get(currDirection)
                ?: return@setOnClickListener
            for (i in directions.indices) {
                val imageName = nextVertex + "-" + directions[i]
                imageNameIntoView(imageName, carouselViews[i])
            }
            prevWps.add(currWpId)
            currWpId = nextVertex
            updateStatus()
            drawCurrentWp()
        }
        backBtn.setOnClickListener {
            if (!inputEnabled) return@setOnClickListener
            // Move to the previous waypoint according to the stack // TODO: grey-out if stack is empty
            if (prevWps.size <= 0) return@setOnClickListener
            val prevWpId = prevWps.peek()
            val dirToPrevWp = graph!!.getWpNeighs()[currWpId]!!.indexOf(prevWpId)
            if (currDirection != dirToPrevWp) {
                if (currDirection == (dirToPrevWp + 1) % 4)
                    rotateImageBy(-1)
                else rotateImageBy(1)
                // in case it's 180 degrees, just press again. One step at a time is more legible
            }
            else {
                currWpId = prevWps.pop()
                for (i in directions.indices)
                    imageNameIntoView(currWpId + "-" + directions[i], carouselViews[i])
            }
            updateStatus()
            drawCurrentWp()
        }
        reportBTN.setOnClickListener {
            if (!inputEnabled) return@setOnClickListener
            val currentImageURL = MainActivity.imageURLs?.get(currWpId + "-" + directions[currDirection])
            UtilityMethods.switchActivityWithData(this, ReportActivity::class.java,
                currentImageURL!!, currWpId, currDirection.toString())
        }
    }

    private fun imageNameIntoView(imageName: String, imageView: ImageView) {
        if (wpImages.containsKey(imageName))
            imageView.setImageBitmap(wpImages[imageName])
        else
            Glide.with(this).load(MainActivity.imageURLs!![imageName]).placeholder(R.drawable.load_icon).into(imageView)
    }

    private fun rotateImageBy(rotateAmount: Int) {
        currDirection += rotateAmount
        currDirection = (currDirection + 4) % 4
        when (rotateAmount) {
            1 -> { // rotate right
                viewFlipper.setInAnimation(this, R.anim.slide_in_right)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
                viewFlipper.showNext()
                delayInput(500)
            }
            -1 -> { // rotate left
                viewFlipper.setInAnimation(this, R.anim.slide_in_left)
                viewFlipper.setOutAnimation(this, R.anim.slide_out_right)
                viewFlipper.showPrevious()
                delayInput(500)
            }
            else -> {
                println("rotateImageBy: rotateAmount must be 1 or -1")
            }
        }
    }

    private fun delayInput(delayMillis: Long) {
        inputEnabled = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(delayMillis)  // Wait for 500 milliseconds
            inputEnabled = true  // Enable the button
        }
    }

    // oded amer learim yadaim
    private fun odedAmar(text: String) {
        if (!reachedDest)
            oded.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun colorButton(imageButton: ImageButton, @ColorRes id: Int) {
        if (imageButton.javaClass == AppCompatImageButton::class.java) {
            imageButton.setBackgroundColor(ContextCompat.getColor(this, id))
        }
        else {
            (imageButton as FloatingActionButton).backgroundTintList = ContextCompat.getColorStateList(this, id)
            imageButton.supportBackgroundTintList = ContextCompat.getColorStateList(this, id) // TODO: Figure out
        }
    }

    // color multiple buttons
    private fun colorButtons(imageButtons: Array<ImageButton>, @ColorRes id: Int) {
        for (imageButton in imageButtons) {
            colorButton(imageButton, id)
        }
    }

    private fun placeFromWp(wpId: String): Place {
        return graph!!.getPlaces().first { it.getPlaceName() == graph!!.getWps()[wpId]!!.getPlaceId() }
    }

    private fun areaFromWp(wpId: String) : Area{
        return currPlace!!.getAreas().first { it.getAreaId() == graph!!.getWps()[wpId]!!.getAreaId() }
    }
}