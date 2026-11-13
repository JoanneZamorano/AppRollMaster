package com.example.approllmaster

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
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
        holder.binding.ivAvatarCombatiente.alpha = if (estaMuerto) 0.35f else 1.0f

        // Paleta oscura sin bordes excepto en el turno activo
        when {
            estaMuerto -> {
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#1E1E1E"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#111113"))
                holder.binding.cardCombatiente.strokeWidth = 0
                holder.binding.tvNombreCombatiente.setTextColor(Color.parseColor("#757575"))
                holder.binding.tvStatsCombatiente.setTextColor(Color.parseColor("#555555"))
            }
            esSuTurno -> {
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#B71C1C"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#3A1216"))
                holder.binding.cardCombatiente.strokeColor = Color.parseColor("#E53935")
                holder.binding.cardCombatiente.strokeWidth = 5
                holder.binding.tvNombreCombatiente.setTextColor(Color.WHITE)
                holder.binding.tvStatsCombatiente.setTextColor(Color.parseColor("#FFCDD2"))
            }
            combatiente.tipo == "ENEMIGO" -> {
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#5C1920"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#1F1517"))
                holder.binding.cardCombatiente.strokeWidth = 0
                holder.binding.tvNombreCombatiente.setTextColor(Color.WHITE)
                holder.binding.tvStatsCombatiente.setTextColor(Color.parseColor("#B0B0B0"))
            }
            else -> {
                // Héroes / Jugadores
                holder.binding.cardAvatar.setCardBackgroundColor(Color.parseColor("#232938"))
                holder.binding.cardCombatiente.setCardBackgroundColor(Color.parseColor("#181A20"))
                holder.binding.cardCombatiente.strokeWidth = 0
                holder.binding.tvNombreCombatiente.setTextColor(Color.WHITE)
                holder.binding.tvStatsCombatiente.setTextColor(Color.parseColor("#B0B0B0"))
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