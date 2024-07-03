package com.cis.indoorlocalization

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.*

class MapBuilderActivity : AppCompatActivity() {

    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView
    private lateinit var frameLayout: FrameLayout
    private val markers = mutableListOf<PointF>()

    companion object {
        private const val REQUEST_CROP_IMAGE = 1001
        private const val MARKERS_FILE_NAME = "markers.csv"
        private const val IMAGE_FILE_NAME = "croppedImage.jpg"
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

        imageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                addMarker(event.x, event.y)
                saveMarkersToCSV()
            }
            true
        }

        setupActivityResultLaunchers()
        loadImage() // Load the image first
        loadMarkersFromCSV() // Load the markers after the image is loaded
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
        val destinationUri = Uri.fromFile(File(filesDir, IMAGE_FILE_NAME))
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
                saveImage(uri)
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
        val markerX = x / imageView.width
        val markerY = y / imageView.height
        markers.add(PointF(markerX, markerY))
    }

    private fun saveMarkersToCSV() {
        val file = File(filesDir, MARKERS_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                markers.forEach { point ->
                    writer.append("${point.x},${point.y}\n")
                }
            }
            Log.d("MapBuilderActivity", "Markers saved successfully: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("MapBuilderActivity", "Error saving markers: ${e.message}")
        }
    }

    private fun loadMarkersFromCSV() {
        val file = File(filesDir, MARKERS_FILE_NAME)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val parts = line!!.split(",")
                        if (parts.size == 2) {
                            val x = parts[0].toFloat()
                            val y = parts[1].toFloat()
                            markers.add(PointF(x, y))
                            // Calculate actual positions and add markers
                            imageView.post {
                                val actualX = x * imageView.width
                                val actualY = y * imageView.height
                                addMarker(actualX, actualY)
                            }
                        }
                    }
                }
                Log.d("MapBuilderActivity", "Markers loaded successfully: ${file.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("MapBuilderActivity", "Error loading markers: ${e.message}")
            }
        } else {
            Log.e("MapBuilderActivity", "Markers file does not exist: ${file.absolutePath}")
        }
    }

    private fun saveImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val file = File(filesDir, IMAGE_FILE_NAME)
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
                Log.d("MapBuilderActivity", "Image saved successfully: ${file.absolutePath}")
            } else {
                Log.e("MapBuilderActivity", "Failed to decode input stream to bitmap")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("MapBuilderActivity", "Error saving image: ${e.message}")
        }
    }

    private fun loadImage() {
        val file = File(filesDir, IMAGE_FILE_NAME)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d("MapBuilderActivity", "Image loaded successfully: ${file.absolutePath}")
                } else {
                    Log.e("MapBuilderActivity", "Failed to decode image: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("MapBuilderActivity", "Error loading image: ${e.message}")
            }
        } else {
            Log.e("MapBuilderActivity", "Image file does not exist: ${file.absolutePath}")
        }
    }
}
