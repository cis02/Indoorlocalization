package com.cis.indoorlocalization

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MapSelectorActivity : AppCompatActivity() {

    private lateinit var selectedMapFile: File
    private val PICK_EXPORT_LOCATION_REQUEST_CODE = 1
    private val PICK_IMPORT_FILE_REQUEST_CODE = 2
    private lateinit var adapter: MapAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selector)

        val recyclerViewMaps = findViewById<RecyclerView>(R.id.recyclerViewMaps)
        recyclerViewMaps.layoutManager = LinearLayoutManager(this)

        val buttonBack = findViewById<Button>(R.id.buttonBack)
        val buttonSave = findViewById<Button>(R.id.buttonSave)
        val buttonLoad = findViewById<Button>(R.id.buttonLoad)
        val buttonExport = findViewById<Button>(R.id.buttonExport)
        val buttonImport = findViewById<Button>(R.id.buttonImport)
        val buttonDelete = findViewById<Button>(R.id.buttonDelete)

        buttonBack.setOnClickListener {
            finish()
        }

        buttonSave.setOnClickListener {
            FileUtils.archiveMap(this)
            Toast.makeText(this, "Map saved successfully", Toast.LENGTH_SHORT).show()
            refreshMapList()
        }

        buttonLoad.setOnClickListener {
            if (::selectedMapFile.isInitialized) {
                FileUtils.unzipMap(this, selectedMapFile, "currentMap")
                Toast.makeText(this, "Map loaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No map selected", Toast.LENGTH_SHORT).show()
            }
        }

        buttonExport.setOnClickListener {
            if (::selectedMapFile.isInitialized) {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, selectedMapFile.name)
                }
                startActivityForResult(intent, PICK_EXPORT_LOCATION_REQUEST_CODE)
            } else {
                Toast.makeText(this, "No map selected", Toast.LENGTH_SHORT).show()
            }
        }

        buttonImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            }
            startActivityForResult(intent, PICK_IMPORT_FILE_REQUEST_CODE)
        }

        buttonDelete.setOnClickListener {
            if (::selectedMapFile.isInitialized) {
                if (selectedMapFile.delete()) {
                    Toast.makeText(this, "Map deleted successfully", Toast.LENGTH_SHORT).show()
                    refreshMapList()
                } else {
                    Toast.makeText(this, "Failed to delete map", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No map selected", Toast.LENGTH_SHORT).show()
            }
        }

        setupRecyclerView(recyclerViewMaps)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val otherMapsPath = File(filesDir, "otherMaps")
        val mapFiles = otherMapsPath.listFiles()?.filter { it.extension == "zip" } ?: emptyList()
        adapter = MapAdapter(mapFiles)
        recyclerView.adapter = adapter

        recyclerView.addOnItemTouchListener(
            RecyclerItemClickListener(this, recyclerView, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    selectedMapFile = mapFiles[position]
                    adapter.notifyDataSetChanged()
                }

                override fun onLongItemClick(view: View, position: Int) {}
            })
        )
    }

    private fun refreshMapList() {
        val otherMapsPath = File(filesDir, "otherMaps")
        val mapFiles = otherMapsPath.listFiles()?.filter { it.extension == "zip" } ?: emptyList()
        adapter.updateMapFiles(mapFiles)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_EXPORT_LOCATION_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        exportSelectedMap(uri)
                    }
                }
                PICK_IMPORT_FILE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val imported = FileUtils.importMapFromExternalStorage(this, uri)
                        if (imported) {
                            Toast.makeText(this, "Map imported successfully", Toast.LENGTH_SHORT).show()
                            refreshMapList()
                        } else {
                            Toast.makeText(this, "Failed to import map", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun exportSelectedMap(uri: Uri) {
        try {
            val outputStream = contentResolver.openOutputStream(uri)
            val inputStream = selectedMapFile.inputStream()
            inputStream.use { input ->
                outputStream?.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Map exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export map: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
