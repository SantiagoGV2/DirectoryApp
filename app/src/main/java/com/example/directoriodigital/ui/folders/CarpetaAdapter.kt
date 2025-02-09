package com.example.directoriodigital.ui.folders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.directoriodigital.R
import com.example.directoriodigital.ui.home.Carpeta


class CarpetaAdapter(private var carpetas: List<Carpeta>) : RecyclerView.Adapter<CarpetaAdapter.CarpetaViewHolder>() {

    class CarpetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombreCarpeta: TextView = itemView.findViewById(R.id.textNombreCarpeta)
        val colorCarpeta: View = itemView.findViewById(R.id.colorCarpeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarpetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carpeta, parent, false)
        return CarpetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarpetaViewHolder, position: Int) {
        val carpeta = carpetas[position]
        holder.nombreCarpeta.text = carpeta.nombre
        holder.colorCarpeta.setBackgroundColor(Color.parseColor(carpeta.hexcolor))
    }

    override fun getItemCount(): Int = carpetas.size

    fun updateData(newCarpetas: List<Carpeta>) {
        carpetas = newCarpetas
        notifyDataSetChanged()
    }
}