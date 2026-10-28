package com.example.approllmaster

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.approllmaster.databinding.ItemPersonajeBinding
import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.models.Personaje

class PersonajeAdapter(
    private var listaPersonajes: List<Personaje> = emptyList()
) : RecyclerView.Adapter<PersonajeAdapter.PersonajeViewHolder>() {

    inner class PersonajeViewHolder(val binding: ItemPersonajeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonajeViewHolder {
        val binding = ItemPersonajeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PersonajeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonajeViewHolder, position: Int) {
        val personaje = listaPersonajes[position]
        with(holder.binding) {
            tvItemNombre.text = personaje.nombre
            tvItemClase.text = personaje.clase
            tvItemSalud.text = "Salud: ${personaje.saludActual} / ${personaje.saludMax}"

            when (personaje) {
                is Guerrero -> tvItemRecurso.text = "Furia: ${personaje.furiaActual} / ${personaje.furiaMax}"
                is Mago -> tvItemRecurso.text = "Maná: ${personaje.manaActual} / ${personaje.manaMax}"
                else -> tvItemRecurso.text = "Recurso: 100 / 100"
            }
        }
    }

    override fun getItemCount(): Int = listaPersonajes.size

    fun actualizarLista(nuevaLista: List<Personaje>) {
        listaPersonajes = nuevaLista
        notifyDataSetChanged()
    }
}