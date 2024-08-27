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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yalantis.ucrop.UCrop
import java.io.*
import java.util.Objects


class MapBuilderActivity : AppCompatActivity() {

    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var markerEditLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView
    private lateinit var frameLayout: FrameLayout
    private lateinit var etTitle: EditText
    private val markers = mutableListOf<MarkerData>()

    companion object {
        private const val REQUEST_CROP_IMAGE = 1001
        private const val MARKERS_FILE_NAME = "markers.csv"
        private const val IMAGE_FILE_NAME = "croppedImage.jpg"
        private const val TITLE_FILE_NAME = "title.txt"
        private const val MAP_DIRECTORY = "currentMap"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_builder)

        imageView = findViewById(R.id.imageView)
        frameLayout = findViewById(R.id.frameLayout)
        etTitle = findViewById(R.id.etTitle)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            saveTitle()
            finish() // Go back to the main activity
        }

        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            selectImage()
        }

        imageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("MapBuilderActivity", "Touch detected at: (${event.x}, ${event.y})")
                addMarker(event.x, event.y, "", "")
                //saveMarkersToCSV()
            }
            true
        }


        setupActivityResultLaunchers()
        loadTitle() // Load the title first
        loadImage() // Load the image next
        loadMarkersFromCSV() // Load the markers after the image is loaded
    }

    private fun getMapDirectory(): File {
        return File(filesDir, MAP_DIRECTORY).apply {
            if (!exists()) mkdirs()
        }
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

        markerEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val markerIndex = result.data?.getIntExtra("markerIndex", -1) ?: -1
                val markerData = result.data?.getParcelableExtra<MarkerData>("markerData")

                if (markerIndex != -1 && markerData != null) {
                    markers[markerIndex] = markerData
                    Log.d("MapBuilderActivity", "Marker updated: ${markerData.name} at position ${markerData.position}")
                    saveMarkersToCSV()
                    updateMarkersOnMap() // Refresh the UI to reflect the changes
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
        val destinationUri = Uri.fromFile(File(getMapDirectory(), IMAGE_FILE_NAME))
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

    private fun addMarker(x: Float, y: Float, name: String, wifiData: String) {
        val position = PointF(x / imageView.width, y / imageView.height)
        Log.d("MapBuilderActivity", "Adding marker at: $position")
        val markerData = MarkerData(position, name, wifiData)
        markers.add(markerData)
        saveMarkersToCSV()
        updateMarkersOnMap()
    }


    private fun updateMarkersOnMap() {
        //frameLayout.remove // Clear all previous markers and names

        markers.forEachIndexed { index, markerData ->
            // Inflate marker view
            val markerView = View.inflate(this, R.layout.marker_view, null)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            // Calculate the actual position of the marker
            val actualX = markerData.position.x * imageView.width
            val actualY = markerData.position.y * imageView.height

            layoutParams.leftMargin = actualX.toInt() - 10 // Adjust for marker size
            layoutParams.topMargin = actualY.toInt() - 10 // Adjust for marker size
            Log.d("MapBui/lderActivity", "Displaying marker at: (${layoutParams.leftMargin}, ${layoutParams.topMargin})")

            // Set up the click listener to edit the marker
            markerView.setOnClickListener {
                Log.d("MapBuilderActivity", "Marker clicked: ${markerData.name}")
                val intent = Intent(this, MarkerEditActivity::class.java).apply {
                    putExtra("markerIndex", index)
                    putExtra("markerData", markerData)
                }
                markerEditLauncher.launch(intent)
            }

            // Add the marker to the layout
            frameLayout.addView(markerView, layoutParams)

            // Display the marker's name above it
            if (markerData.name.isNotEmpty()) {
                val nameView = TextView(this).apply {
                    text = markerData.name
                    setBackgroundResource(android.R.color.transparent)
                }
                val nameLayoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                nameLayoutParams.leftMargin = actualX.toInt() - 10 // Adjust for marker size
                nameLayoutParams.topMargin = actualY.toInt() - 30 // Above the marker
                frameLayout.addView(nameView, nameLayoutParams)
            }
        }
    }




    private fun saveMarkersToCSV() {
        val file = File(getMapDirectory(), MARKERS_FILE_NAME)

        try {
            FileWriter(file, false).use { writer ->
                markers.forEach { marker ->
                    val  str: String  = "${marker.position.x},${marker.position.y},${marker.name},${marker.wifiData}\n"
                    writer.write(str)
                    Log.d("MapBuilderActivity", "Saved marker: ${marker.name} at position ${marker.position}")
                }
            }
            Log.d("MapBuilderActivity", "Markers saved successfully: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("MapBuilderActivity", "Error saving markers: ${e.message}")
        }
    }


    private fun loadMarkersFromCSV() {
        val file = File(getMapDirectory(), MARKERS_FILE_NAME)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val parts = line!!.split(",")
                        if (parts.size >= 4) {
                            val x = parts[0].toFloat()
                            val y = parts[1].toFloat()
                            //val position = PointF(parts[0].toFloat(), parts[1].toFloat())
                            val name = parts[2]
                            val wifiData = parts[3]

                          //  markers.add(MarkerData(PointF(x,y), name, wifiData))

                            imageView.post {
                                val actualX = x * imageView.width
                                val actualY = y * imageView.height
                                addMarker(actualX, actualY, name, wifiData)
                            }
                        }
                    }
                }
                Log.d("MapBuilderActivity", "Markers loaded successfully: ${file.absolutePath}")
                updateMarkersOnMap()
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
                val file = File(getMapDirectory(), IMAGE_FILE_NAME)
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
        val file = File(getMapDirectory(), IMAGE_FILE_NAME)
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




    private fun saveTitle() {
        val title = etTitle.text.toString()
        val file = File(getMapDirectory(), TITLE_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                writer.write(title)
            }
            Log.d("MapBuilderActivity", "Title saved successfully: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("MapBuilderActivity", "Error saving title: ${e.message}")
        }
    }

    private fun loadTitle() {
        val file = File(getMapDirectory(), TITLE_FILE_NAME)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    val title = reader.readLine()
                    etTitle.setText(title)
                }
                Log.d("MapBuilderActivity", "Title loaded successfully: ${file.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("MapBuilderActivity", "Error loading title: ${e.message}")
            }
        } else {
            Log.e("MapBuilderActivity", "Title file does not exist: ${file.absolutePath}")
        }
    }
}
