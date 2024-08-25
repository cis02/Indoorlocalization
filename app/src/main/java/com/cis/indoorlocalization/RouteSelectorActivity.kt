package com.cis.indoorlocalization

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import kotlin.math.pow
import kotlin.math.sqrt

class RouteSelectorActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var frameLayout: FrameLayout
    private lateinit var tvTitle: TextView
    private val markers = mutableListOf<PointF>()
    private var currentLocationMarker: PointF? = null
    private var destinationMarkerView: View? = null
    private var pathOverlay: PathOverlayView? = null

    companion object {
        private const val MARKERS_FILE_NAME = "markers.csv"
        private const val IMAGE_FILE_NAME = "croppedImage.jpg"
        private const val TITLE_FILE_NAME = "title.txt"
        private const val MAP_DIRECTORY = "currentMap"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_selector)

        imageView = findViewById(R.id.imageView)
        frameLayout = findViewById(R.id.frameLayout)
        tvTitle = findViewById(R.id.tvTitle)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // Go back to the previous activity
        }

        loadTitle()
        loadImage()
        loadMarkersFromCSV()

        imageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val destinationMarker = findNearestMarker(event.x, event.y)
                if (destinationMarker != null) {
                    setDestinationMarker(destinationMarker)
                    if (currentLocationMarker != null) {
                        showNavigation(currentLocationMarker!!, destinationMarker)
                    }
                }
            }
            true
        }
    }

    private fun getMapDirectory(): File {
        return File(filesDir, MAP_DIRECTORY).apply {
            if (!exists()) mkdirs()
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
                        if (parts.size == 2) {
                            val x = parts[0].toFloat()
                            val y = parts[1].toFloat()
                            markers.add(PointF(x, y))
                            // Calculate actual positions and add markers
                            imageView.post {
                                val actualX = x * imageView.width
                                val actualY = y * imageView.height
                                addMarker(actualX.toInt(), actualY.toInt())
                            }
                        }
                    }
                }
                Log.d("RouteSelectorActivity", "Markers loaded successfully: ${file.absolutePath}")
                // Set a random marker as the current location marker for demonstration
                if (markers.isNotEmpty()) {
                    val randomMarker = markers.random()
                    currentLocationMarker = randomMarker
                    imageView.post {
                        val actualX = randomMarker.x * imageView.width
                        val actualY = randomMarker.y * imageView.height
                        addCurrentLocationMarker(actualX.toInt(), actualY.toInt())
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading markers: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Markers file does not exist: ${file.absolutePath}")
        }
    }

    private fun loadImage() {
        val file = File(getMapDirectory(), IMAGE_FILE_NAME)
        if (file.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Log.d("RouteSelectorActivity", "Image loaded successfully: ${file.absolutePath}")
                } else {
                    Log.e("RouteSelectorActivity", "Failed to decode image: ${file.absolutePath}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading image: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Image file does not exist: ${file.absolutePath}")
        }
    }

    private fun loadTitle() {
        val file = File(getMapDirectory(), TITLE_FILE_NAME)
        if (file.exists()) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    val title = reader.readLine()
                    tvTitle.text = title
                }
                Log.d("RouteSelectorActivity", "Title loaded successfully: ${file.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("RouteSelectorActivity", "Error loading title: ${e.message}")
            }
        } else {
            Log.e("RouteSelectorActivity", "Title file does not exist: ${file.absolutePath}")
        }
    }

    private fun addMarker(x: Int, y: Int) {
        val markerView = View.inflate(this, R.layout.marker_view, null)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.leftMargin = x - 10 // Adjust for marker size
        layoutParams.topMargin = y - 10 // Adjust for marker size
        frameLayout.addView(markerView, layoutParams)
    }

    private fun addCurrentLocationMarker(x: Int, y: Int) {
        val markerView = View.inflate(this, R.layout.marker_loc, null)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.leftMargin = x - 10 // Adjust for marker size
        layoutParams.topMargin = y - 10 // Adjust for marker size
        frameLayout.addView(markerView, layoutParams)
    }

    private fun setDestinationMarker(point: PointF) {
        destinationMarkerView?.let { frameLayout.removeView(it) }
        val markerView = View.inflate(this, R.layout.marker_dest, null)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.leftMargin = (point.x * imageView.width).toInt() - 10 // Adjust for marker size
        layoutParams.topMargin = (point.y * imageView.height).toInt() - 10 // Adjust for marker size
        frameLayout.addView(markerView, layoutParams)
        destinationMarkerView = markerView
    }

    private fun findNearestMarker(x: Float, y: Float): PointF? {
        val touchPoint = PointF(x / imageView.width, y / imageView.height)
        val threshold = 0.05f // 5% of the image size
        return markers.minByOrNull { calculateDistance(it, touchPoint) }?.takeIf { calculateDistance(it, touchPoint) < threshold }
    }

    private fun showNavigation(start: PointF, end: PointF) {
        val graph = Graph()

        // Create a map to assign a unique integer identifier to each marker
        val markerIndexMap = markers.mapIndexed { index, marker -> marker to index }.toMap()

        // Add vertices and edges based on your markers and possible paths
        markers.forEachIndexed { index, marker ->
            val adjacentVertices = markers.mapIndexedNotNull { neighborIndex, neighbor ->
                if (neighborIndex != index) {
                    Vertex(neighborIndex.toString()[0], calculateDistance(marker, neighbor).toInt())
                } else {
                    null
                }
            }
            graph.addVertex(index.toString()[0], adjacentVertices)
        }

        // Find the closest markers to the start and end points
        val startVertexIndex = markerIndexMap.entries.minByOrNull { calculateDistance(it.key, start) }?.value
        val endVertexIndex = markerIndexMap.entries.minByOrNull { calculateDistance(it.key, end) }?.value

        if (startVertexIndex == null || endVertexIndex == null) {
            Log.e("RouteSelectorActivity", "Could not find appropriate vertices for start or end points")
            return
        }

        val startVertex = startVertexIndex.toString()[0]
        val endVertex = endVertexIndex.toString()[0]

        // Log the start and end vertices
        Log.d("RouteSelectorActivity", "Start Vertex: $startVertex, End Vertex: $endVertex")

        val path = graph.getShortestPath(startVertex, endVertex)

        // Log the path for debugging
        Log.d("RouteSelectorActivity", "Calculated path: $path")

        // Translate the path characters back to the corresponding marker positions
        val pathPoints = path.map { markerId ->
            markers[markerId.toString().toInt()]
        }

        // Visualize the path on your map
        visualizePath(pathPoints)
    }






    private fun calculateDistance(point1: PointF, point2: PointF): Float {
        return sqrt((point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2))
    }

    private fun visualizePath(pathPoints: List<PointF>) {
        pathOverlay?.let { frameLayout.removeView(it) }
        pathOverlay = PathOverlayView(this).apply {
            setPathPoints(pathPoints)
        }
        frameLayout.addView(pathOverlay)
    }

    inner class PathOverlayView(context: android.content.Context) : View(context) {

        private val pathPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        private var pathPoints: List<PointF> = emptyList()

        fun setPathPoints(points: List<PointF>) {
            this.pathPoints = points
            invalidate() // Request to redraw the view with the new path
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (pathPoints.size < 2) return
            for (i in 0 until pathPoints.size - 1) {
                val start = pathPoints[i]
                val end = pathPoints[i + 1]
                // Log the points being drawn for debugging
                Log.d("PathOverlayView", "Drawing line from ($start.x, $start.y) to ($end.x, $end.y)")
                canvas.drawLine(
                    start.x * imageView.width,
                    start.y * imageView.height,
                    end.x * imageView.width,
                    end.y * imageView.height,
                    pathPaint
                )
            }
        }
    }




}
