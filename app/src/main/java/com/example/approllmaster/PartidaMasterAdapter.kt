package com.example.approllmaster

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.approllmaster.databinding.ItemPartidaMasterBinding
import com.example.approllmaster.models.Partida

class PartidaMasterAdapter(
    private val listaPartidas: List<Partida>,
    private val onPartidaClick: (Partida) -> Unit,
    private val onEliminarClick: (Partida) -> Unit
) : RecyclerView.Adapter<PartidaMasterAdapter.PartidaViewHolder>() {

    inner class PartidaViewHolder(val binding: ItemPartidaMasterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidaViewHolder {
        val binding = ItemPartidaMasterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PartidaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PartidaViewHolder, position: Int) {
        val partida = listaPartidas[position]
        with(holder.binding) {
            tvNombrePartida.text = partida.nombrePartida
            chipCodigo.text = partida.codigoSala
            tvEstadoPartida.text = "Estado: ${partida.estado}"

            root.setOnClickListener {
                onPartidaClick(partida)
            }

            btnEliminarPartida.setOnClickListener {
                onEliminarClick(partida)
            }
        }
    }

    override fun getItemCount(): Int = listaPartidas.size
}