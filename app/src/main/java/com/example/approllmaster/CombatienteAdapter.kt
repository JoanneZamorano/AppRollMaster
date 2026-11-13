package com.example.approllmaster

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.approllmaster.databinding.ItemCombatienteBinding
import com.example.approllmaster.models.Combatiente
import java.net.URL
import java.util.concurrent.Executors

class CombatienteAdapter(
    private var listaCombatientes: List<Combatiente>,
    private var idTurnoActivo: String = "",
    private var enCombate: Boolean = false
) : RecyclerView.Adapter<CombatienteAdapter.CombatienteViewHolder>() {

    private val executor = Executors.newCachedThreadPool()

    inner class CombatienteViewHolder(val binding: ItemCombatienteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CombatienteViewHolder {
        val binding = ItemCombatienteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CombatienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CombatienteViewHolder, position: Int) {
        val combatiente = listaCombatientes[position]
        val estaMuerto = combatiente.psActuales <= 0
        val esSuTurno = enCombate && !estaMuerto && combatiente.id == idTurnoActivo

        // Nombre
        val estadoSufijo = if (combatiente.claseOEstado.isNotBlank()) {
            " (${combatiente.claseOEstado})"
        } else {
            ""
        }
        holder.binding.tvNombreCombatiente.text = "${combatiente.nombre}$estadoSufijo"

        // Stats
        holder.binding.tvStatsCombatiente.text =
            "PS: ${combatiente.psActuales} / ${combatiente.psMaximos}   |   CA: ${combatiente.ca}"

        val porcentaje = if (combatiente.psMaximos > 0) {
            (combatiente.psActuales * 100) / combatiente.psMaximos
        } else 0
        holder.binding.pbVidaCombatiente.progress = porcentaje.coerceIn(0, 100)

        // Cargar avatar del personaje si tiene avatarUrl (recurso drawable o URL web)
        // Imagen por defecto: icono de Android (ic_launcher), y si tiene avatarUrl se sustituye
        holder.binding.ivAvatarCombatiente.setImageResource(R.mipmap.ic_launcher)

        val url = combatiente.avatarUrl.trim()
        if (url.isNotEmpty()) {
            val context = holder.itemView.context
            val resDrawable = context.resources.getIdentifier(url, "drawable", context.packageName)
            val resMipmap = context.resources.getIdentifier(url, "mipmap", context.packageName)

            when {
                resDrawable != 0 -> holder.binding.ivAvatarCombatiente.setImageResource(resDrawable)
                resMipmap != 0 -> holder.binding.ivAvatarCombatiente.setImageResource(resMipmap)
                url.startsWith("http") -> {
                    holder.binding.ivAvatarCombatiente.tag = url
                    executor.execute {
                        try {
                            val stream = URL(url).openStream()
                            val bitmap = BitmapFactory.decodeStream(stream)
                            holder.itemView.post {
                                if (holder.binding.ivAvatarCombatiente.tag == url && bitmap != null) {
                                    holder.binding.ivAvatarCombatiente.setImageBitmap(bitmap)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        }

        // Atenuar imagen si está caído
        holder.binding.ivAvatarCombatiente.alpha = if (estaMuerto) 0.4f else 1.0f

        // Colores del círculo y de la tarjeta
        when {
            estaMuerto -> {
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#757575"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
                holder.binding.cardCombatiente.strokeColor = Color.parseColor("#BDBDBD")
                holder.binding.cardCombatiente.strokeWidth = 2
                holder.binding.tvNombreCombatiente.setTextColor(Color.parseColor("#757575"))
            }
            esSuTurno -> {
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#FF6D00"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.binding.cardCombatiente.strokeColor = Color.parseColor("#FF6D00")
                holder.binding.cardCombatiente.strokeWidth = 6
                holder.binding.tvNombreCombatiente.setTextColor(Color.parseColor("#1C1B1F"))
            }
            else -> {
                holder.binding.cardAvatar.setCardBackgroundColor(
                    if (combatiente.tipo == "ENEMIGO") Color.parseColor("#8E0000") else Color.parseColor("#4A148C")
                )
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.WHITE)
                holder.binding.cardCombatiente.strokeColor =
                    if (combatiente.tipo == "ENEMIGO") Color.parseColor("#FFCDD2") else Color.parseColor("#D1C4E9")
                holder.binding.cardCombatiente.strokeWidth = 3
                holder.binding.tvNombreCombatiente.setTextColor(Color.parseColor("#1C1B1F"))
            }
        }
    }

    override fun getItemCount(): Int = listaCombatientes.size

    fun actualizarDatos(nuevaLista: List<Combatiente>, nuevoIdTurno: String, combateActivo: Boolean) {
        listaCombatientes = nuevaLista
        idTurnoActivo = nuevoIdTurno
        enCombate = combateActivo
        notifyDataSetChanged()
    }
}