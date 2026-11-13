package com.example.approllmaster

import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
            tvStatsMonstruo.text = "PS: ${monstruo.vida} | CA: ${monstruo.armadura} | Atq: ${monstruo.ataque}"
            tvCantidad.text = monstruo.cantidadSeleccionada.toString()

            chipJefe.visibility = if (monstruo.esJefe) View.VISIBLE else View.GONE

            // Cargar avatar local desde res/drawable
            val url = monstruo.avatarUrl.trim()
            val context = holder.itemView.context
            val resDrawable = if (url.isNotEmpty()) {
                context.resources.getIdentifier(url, "drawable", context.packageName)
            } else 0

            if (resDrawable != 0) {
                ivAvatarMonstruo.setImageResource(resDrawable)
            } else {
                ivAvatarMonstruo.setImageResource(R.mipmap.ic_launcher)
            }

            // Recorte superior (Top-Crop) para que siempre se vea la cabeza de la criatura
            ivAvatarMonstruo.scaleType = ImageView.ScaleType.MATRIX
            ivAvatarMonstruo.post {
                val drawable = ivAvatarMonstruo.drawable ?: return@post
                val viewWidth = ivAvatarMonstruo.width.toFloat()
                val viewHeight = ivAvatarMonstruo.height.toFloat()
                val imgWidth = drawable.intrinsicWidth.toFloat()
                val imgHeight = drawable.intrinsicHeight.toFloat()

                if (imgWidth > 0f && imgHeight > 0f) {
                    val scale = maxOf(viewWidth / imgWidth, viewHeight / imgHeight)
                    val dx = (viewWidth - imgWidth * scale) / 2f
                    val matrix = Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(dx, 0f)
                    }
                    ivAvatarMonstruo.imageMatrix = matrix
                }
            }

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