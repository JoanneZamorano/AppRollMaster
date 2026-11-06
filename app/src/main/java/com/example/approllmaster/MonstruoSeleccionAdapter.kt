package com.example.approllmaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.approllmaster.databinding.ItemMonstruoSeleccionBinding
import com.example.approllmaster.models.MonstruoBase

class MonstruoSeleccionAdapter(
    private val listaMonstruos: List<MonstruoBase>,
    private val onCantidadCambiada: () -> Unit
) : RecyclerView.Adapter<MonstruoSeleccionAdapter.MonstruoViewHolder>() {

    inner class MonstruoViewHolder(val binding: ItemMonstruoSeleccionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonstruoViewHolder {
        val binding = ItemMonstruoSeleccionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonstruoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonstruoViewHolder, position: Int) {
        val monstruo = listaMonstruos[position]

        with(holder.binding) {
            tvNombreMonstruo.text = monstruo.nombre
            tvStatsMonstruo.text = "❤️ PS: ${monstruo.vida} | 🛡️ CA: ${monstruo.armadura} | ⚔️ Atq: ${monstruo.ataque}"
            tvCantidad.text = monstruo.cantidadSeleccionada.toString()

            chipJefe.visibility = if (monstruo.esJefe) View.VISIBLE else View.GONE

            btnMas.setOnClickListener {
                monstruo.cantidadSeleccionada++
                tvCantidad.text = monstruo.cantidadSeleccionada.toString()
                onCantidadCambiada()
            }

            btnMenos.setOnClickListener {
                if (monstruo.cantidadSeleccionada > 0) {
                    monstruo.cantidadSeleccionada--
                    tvCantidad.text = monstruo.cantidadSeleccionada.toString()
                    onCantidadCambiada()
                }
            }
        }
    }

    override fun getItemCount(): Int = listaMonstruos.size
}