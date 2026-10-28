package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivityCharacterListBinding
import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.models.Personaje
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore

class CharacterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCharacterListBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()
    private val adapter = PersonajeAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar RecyclerView
        binding.rvPersonajes.layoutManager = LinearLayoutManager(this)
        binding.rvPersonajes.adapter = adapter

        // Botón inferior para crear nuevo personaje
        binding.btnCrearPersonajeDesdeLista.setOnClickListener {
            val intent = Intent(this, CreateCharacterActivity::class.java)
            startActivity(intent)
        }

        // Botón inferior para volver al menú principal
        binding.btnVolverMenu.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refrescar la lista al volver a esta pantalla
        cargarPersonajes()
    }

    private fun cargarPersonajes() {
        val userId = authRepository.currentUser?.uid ?: return

        binding.progressBarLista.visibility = View.VISIBLE
        binding.tvSinPersonajes.visibility = View.GONE

        db.collection("personajes")
            .whereEqualTo("idUsuario", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBarLista.visibility = View.GONE
                val personajes = mutableListOf<Personaje>()

                for (doc in snapshot.documents) {
                    val clase = doc.getString("clase") ?: ""
                    val p = if (clase == "GUERRERO") {
                        doc.toObject(Guerrero::class.java)
                    } else {
                        doc.toObject(Mago::class.java)
                    }
                    if (p != null) {
                        personajes.add(p)
                    }
                }

                if (personajes.isEmpty()) {
                    binding.tvSinPersonajes.visibility = View.VISIBLE
                } else {
                    binding.tvSinPersonajes.visibility = View.GONE
                    adapter.actualizarLista(personajes)
                }
            }
            .addOnFailureListener { e ->
                binding.progressBarLista.visibility = View.GONE
                Toast.makeText(this, "Error al cargar personajes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}