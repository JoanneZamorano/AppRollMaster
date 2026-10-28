package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityLoginBinding
import com.example.approllmaster.repository.AuthRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //si el usuario ya inicio sesion antes -> pasa directo a la app principal
        if (authRepository.currentUser != null) {
            irAlMenuPrincipal()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ir a la pantalla de crear cuenta
        binding.btnIrARegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        //pulsar en entrar
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Introduce correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mostrarCarga(true)

            authRepository.iniciarSesion(
                email = email,
                pass = pass,
                onSuccess = {
                    mostrarCarga(false)
                    irAlMenuPrincipal()
                },
                onError = { error ->
                    mostrarCarga(false)
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun irAlMenuPrincipal() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Cerramos el login para que al dar atras no vuelva aqui
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progressBarLogin.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !cargando
    }
}