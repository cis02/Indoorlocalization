package com.cis.indoorlocalization

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selector)

        val recyclerViewMaps = findViewById<RecyclerView>(R.id.recyclerViewMaps)
        recyclerViewMaps.layoutManager = LinearLayoutManager(this)

        val otherMapsPath = File(filesDir, "otherMaps")
        val mapFiles = otherMapsPath.listFiles()?.filter { it.extension == "zip" } ?: emptyList()

        val adapter = MapAdapter(mapFiles)
        recyclerViewMaps.adapter = adapter

        recyclerViewMaps.addOnItemTouchListener(
            RecyclerItemClickListener(this, recyclerViewMaps, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View, position: Int) {
                    selectedMapFile = mapFiles[position]
                }

                override fun onLongItemClick(view: View, position: Int) {}
            })
        )

        val buttonBack = findViewById<Button>(R.id.buttonBack)
        val buttonSave = findViewById<Button>(R.id.buttonSave)
        val buttonLoad = findViewById<Button>(R.id.buttonLoad)
        val buttonExport = findViewById<Button>(R.id.buttonExport)
        val buttonImport = findViewById<Button>(R.id.buttonImport)

        buttonBack.setOnClickListener {
            finish()
        }

        buttonSave.setOnClickListener {
            FileUtils.archiveMap(this, "My First Map")
            Toast.makeText(this, "Map saved successfully", Toast.LENGTH_SHORT).show()
            adapter.notifyDataSetChanged()
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
            if (FileUtils.exportMapToExternalStorage(this, "My First Map")) {
                Toast.makeText(this, "Map exported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to export map", Toast.LENGTH_SHORT).show()
            }
        }

        buttonImport.setOnClickListener {
            // Assuming the map file is in the Downloads directory
            val imported = FileUtils.importMapFromExternalStorage(this, "/sdcard/Download/My First Map.zip")
            if (imported) {
                Toast.makeText(this, "Map imported successfully", Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Failed to import map", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
