package com.cis.indoorlocalization

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MapAdapter(private val mapFiles: List<File>) : RecyclerView.Adapter<MapAdapter.MapViewHolder>() {

    class MapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mapTitle: TextView = itemView.findViewById(R.id.mapTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_map, parent, false)
        return MapViewHolder(view)
    }

    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val mapFile = mapFiles[position]
        holder.mapTitle.text = mapFile.nameWithoutExtension
    }

    override fun getItemCount(): Int = mapFiles.size
}
