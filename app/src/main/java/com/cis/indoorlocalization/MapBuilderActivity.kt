package com.cis.indoorlocalization

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MapBuilderActivity : AppCompatActivity() {

    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView
    private lateinit var frameLayout: FrameLayout
    private val markers = mutableListOf<PointF>()

    companion object {
        private const val REQUEST_CROP_IMAGE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_builder)

        imageView = findViewById(R.id.imageView)
        frameLayout = findViewById(R.id.frameLayout)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // Go back to the main activity
        }

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            selectImage()
        }

        imageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                addMarker(event.x, event.y)
                saveMarkersToCSV()
            }
            true
        }

        setupActivityResultLaunchers()
    }

    private fun setupActivityResultLaunchers() {
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                selectedImageUri?.let { uri ->
                    cropImage(uri)
                }
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    private fun cropImage(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val options = UCrop.Options()

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(3f, 4f)
            .withOptions(options)
            .start(this, REQUEST_CROP_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CROP_IMAGE && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let { uri ->
                imageView.setImageURI(uri)
            }
        }
    }

    private fun addMarker(x: Float, y: Float) {
        val markerView = View.inflate(this, R.layout.marker_view, null)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.leftMargin = x.toInt() - 10 // Adjust for marker size
        layoutParams.topMargin = y.toInt() - 10 // Adjust for marker size
        frameLayout.addView(markerView, layoutParams)

        // Save the marker position
        val imageViewLocation = IntArray(2)
        imageView.getLocationOnScreen(imageViewLocation)
        val markerX = (x - imageViewLocation[0]) / imageView.width
        val markerY = (y - imageViewLocation[1]) / imageView.height
        markers.add(PointF(markerX, markerY))
    }

    private fun saveMarkersToCSV() {
        val file = File(getExternalFilesDir(null), "markers.csv")
        try {
            FileWriter(file).use { writer ->
                markers.forEach { point ->
                    writer.append("${point.x},${point.y}\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
