package com.afeka.compus

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import com.bumptech.glide.request.transition.Transition
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.afeka.compus.objects.Report
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import java.io.File


class ReportActivity : AppCompatActivity() {

    private lateinit var chooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var currentPhoto: ImageView
    private lateinit var currentImageURL: String
    private lateinit var wpId: String
    private lateinit var file: File
    private lateinit var newPhoto: ImageView
    private lateinit var description: EditText
    private lateinit var submit: Button
    private var direction: Int = 0
    private var uploadedImgUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report_menu)
        findViews()
        val extras = intent.extras
        if (extras != null) {
            currentImageURL = extras.getString("key0").toString()
            wpId = extras.getString("key1").toString()
            direction = extras.getString("key2")!!.toInt()
        }

        Glide.with(this@ReportActivity).load(currentImageURL)
            .placeholder(R.drawable.compus_logo).into(currentPhoto)

        setupImagePicker(newPhoto)
    }

    override fun onResume() {
        super.onResume()
        setSubmitListener()
    }

    private fun findViews() {
        currentPhoto = findViewById(R.id.report_menu_IMG_cur)
        newPhoto = findViewById(R.id.report_menu_IMG_new)
        submit = findViewById(R.id.report_menu_BTN_submit)
        description = findViewById(R.id.report_menu_ET_reason)
    }

    private fun setSubmitListener() {
        val submitListener = View.OnClickListener {
            val rep = Report(
                reportId = "", // server generates uuid when uploading
                text = description.text.toString(),
                wpId = wpId,
                direction = direction,
                siteName = MainActivity.site!!.getSiteName()
            )
            // Check that none of these are empty
           if(!rep.isCompleteReport()){
                Toast.makeText(
                    this,
                    "Please fill out all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }
            val result = MainActivity.sm.uploadReport(rep, file)
            val text = if (result) "Report uploaded successfully" else "Error uploading report"
            Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
        submit.setOnClickListener(submitListener)
    }

    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap? {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }


    private fun setupImagePicker(img: ImageView) {
        val chooser = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        chooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Error picking image", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val data = result.data
            if (data != null) {
                val uri = data.data
                uri?.let { it ->
                    Glide.with(this)
                        .asBitmap()
                        .load(it)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {val scaledBitmap = scaleToFitWidth(resource, Resources.getSystem().displayMetrics.widthPixels)
                                img.setImageBitmap(scaledBitmap)
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                    uploadedImgUri = it

                    // Convert uploadedImgUri to File
                    val contentResolver = contentResolver
                    contentResolver.takeIf { it != null }?.let { resolver ->
                        val inputStream = resolver.openInputStream(uploadedImgUri!!)
                        val file = File.createTempFile("prefix", ".jpg")
                        file.outputStream().use { output ->
                            inputStream?.copyTo(output)
                        }
                        this.file = file
                        inputStream?.close()
                    }
                }
            }
        }
        img.setOnClickListener { chooserLauncher.launch(chooser) }
    }
}