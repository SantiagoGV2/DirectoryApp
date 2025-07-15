package com.example.directoriodigital.ui.folders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.directoriodigital.data.model.Carpeta// Asegúrate que el import sea correcto
import com.example.directoriodigital.databinding.ItemFolderBinding // Importa el ViewBinding de tu item

// 1. La clase extiende ListAdapter para eficiencia máxima
class FolderAdapter : ListAdapter<Carpeta, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    // 2. ViewHolder: Mantiene las referencias a las vistas de un solo item (item_folder.xml)
    class FolderViewHolder(private val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        // ✅ El método bind recibe un objeto Carpeta
        fun bind(carpeta: Carpeta) {
            binding.folderName.text = carpeta.nombre

            try {
                // Usamos el campo 'color' de la clase Carpeta
                val color = Color.parseColor(carpeta.color)
                DrawableCompat.setTint(binding.folderIcon.drawable, color)
            } catch (e: Exception) {
                // Manejo de error si el color es nulo o inválido
            }
        }
    }

    // 3. onCreateViewHolder: Crea una nueva vista para un item cuando es necesario
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// 5. DiffUtil.ItemCallback: Ayuda al ListAdapter a saber qué elementos cambiaron.
// Esto hace que las actualizaciones de la lista sean súper rápidas.
class FolderDiffCallback : DiffUtil.ItemCallback<Carpeta>() {
    override fun areItemsTheSame(oldItem: Carpeta, newItem: Carpeta): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Carpeta, newItem: Carpeta): Boolean {
        return oldItem == newItem
    }
}