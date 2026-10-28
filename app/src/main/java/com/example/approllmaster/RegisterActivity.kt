package com.example.approllmaster

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityRegisterBinding
import com.example.approllmaster.repository.AuthRepository

class RegisterActivity : AppCompatActivity() {

    // ViewBinding para acceder a las vistas del XML sin findViewById
    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //volver atras si ya tiene cuenta
        binding.btnVolverLogin.setOnClickListener {
            finish()
        }

        //pulsar el boton de crear cuenta
        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombreUsuario.text.toString().trim()
            val email = binding.etEmailRegistro.text.toString().trim()
            val pass = binding.etPasswordRegistro.text.toString().trim()

            // comprobacio basica antes de llamar a Firebase
            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // muestra la barrita de carga y apagamos el boton para no darle dos veces
            mostrarCarga(true)

            authRepository.registrarUsuario(
                email = email,
                pass = pass,
                nombreUsuario = nombre,
                onSuccess = {
                    mostrarCarga(false)
                    Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show()
                    finish() // Cerramos y volvemos al login
                },
                onError = { mensajeError ->
                    mostrarCarga(false)
                    Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progressBarRegistro.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnRegistrar.isEnabled = !cargando
    }
}