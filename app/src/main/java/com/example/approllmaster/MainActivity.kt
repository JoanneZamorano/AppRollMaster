package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityMainBinding
import com.example.approllmaster.repository.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private var nombreUsuarioActual: String = "Aventurero"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarDatosUsuario()

        // Menú desplegable de usuario (Mi perfil / Cerrar sesión)
        binding.btnMenuUsuario.setOnClickListener { vista ->
            mostrarMenuUsuario(vista)
        }

        // 1 - Mis personajes
        binding.btnMisPersonajes.setOnClickListener {
            startActivity(Intent(this, CharacterListActivity::class.java))
        }

        // 2 - Crear NUEVO personaje
        binding.btnCrearPersonajeMenu.setOnClickListener {
            startActivity(Intent(this, CreateCharacterActivity::class.java))
        }

        // 3 - Crear partida (Master)
        binding.btnCrearPartida.setOnClickListener {
            startActivity(Intent(this, CreateGameActivity::class.java))
        }

        // 4 - Unirse a Partida (Jugador)
        binding.btnUnirsePartida.setOnClickListener {
            startActivity(Intent(this, JoinGameActivity::class.java))
        }
    }

    private fun cargarDatosUsuario() {
        val usuarioActual = authRepository.currentUser ?: return
        db.collection("usuarios").document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombreUsuario")
                if (!nombre.isNullOrBlank()) {
                    nombreUsuarioActual = nombre
                    binding.tvBienvenida.text = "¡Saludos, $nombre!"
                }
            }
    }

    private fun mostrarMenuUsuario(anchor: View) {
        val density = resources.displayMetrics.density
        fun dp(valor: Int): Int = (valor * density).toInt()

        val cardMenu = com.google.android.material.card.MaterialCardView(this).apply {
            setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.superficie_tarjeta))
            radius = 14 * density
            cardElevation = 10 * density
            strokeWidth = 0
        }

        val contenedor = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val popupWindow = android.widget.PopupWindow(
            cardMenu,
            dp(190),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10 * density
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        fun crearOpcion(texto: String, colorTexto: Int, onClick: () -> Unit): android.widget.TextView {
            return android.widget.TextView(this).apply {
                text = texto
                setTextColor(colorTexto)
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(dp(18), dp(13), dp(18), dp(13))
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener {
                    popupWindow.dismiss()
                    onClick()
                }
            }
        }

        val blanco = androidx.core.content.ContextCompat.getColor(this, R.color.texto_blanco)
        val rojo = androidx.core.content.ContextCompat.getColor(this, R.color.rojo_claro)

        contenedor.addView(crearOpcion("Mi perfil", blanco) {
            abrirDialogoMiPerfil()
        })

        val separador = View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                marginStart = dp(14)
                marginEnd = dp(14)
            }
            setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.borde_tarjeta))
        }
        contenedor.addView(separador)

        contenedor.addView(crearOpcion("Cerrar sesión", rojo) {
            confirmarCerrarSesion()
        })

        cardMenu.addView(contenedor)
        popupWindow.showAsDropDown(anchor, -dp(8), dp(4))
    }

    private fun abrirDialogoMiPerfil() {
        val usuarioActual = authRepository.currentUser ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_mi_perfil, null)
        val etCorreo = dialogView.findViewById<TextInputEditText>(R.id.etPerfilCorreo)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilPerfilNombre)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etPerfilNombre)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPerfilPassword)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.etPerfilPassword)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarPerfil)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarPerfil)

        etCorreo.setText(usuarioActual.email ?: "Sin correo")
        etNombre.setText(nombreUsuarioActual)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            val nuevaPassword = etPassword.text.toString().trim()

            if (nuevoNombre.isEmpty()) {
                tilNombre.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            tilNombre.error = null

            if (nuevaPassword.isNotEmpty() && nuevaPassword.length < 6) {
                tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }
            tilPassword.error = null

            btnGuardar.isEnabled = false

            // 1. Actualizamos el nombre en Firestore
            db.collection("usuarios").document(usuarioActual.uid)
                .update("nombreUsuario", nuevoNombre)
                .addOnSuccessListener {
                    nombreUsuarioActual = nuevoNombre
                    binding.tvBienvenida.text = "¡Saludos, $nuevoNombre!"

                    // 2. Si el usuario escribió una contraseña nueva, la actualizamos en Firebase Auth
                    if (nuevaPassword.isNotEmpty()) {
                        usuarioActual.updatePassword(nuevaPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Perfil y contraseña actualizados", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                btnGuardar.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Nombre guardado, pero para cambiar la contraseña debes haber iniciado sesión recientemente (${e.message})",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    } else {
                        Toast.makeText(this, "Nombre actualizado con éxito", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .addOnFailureListener { e ->
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al actualizar perfil: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres cerrar tu sesión en RollMaster?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                authRepository.cerrarSesion()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}