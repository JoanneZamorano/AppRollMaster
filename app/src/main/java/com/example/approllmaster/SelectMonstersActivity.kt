package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivitySelectMonstersBinding
import com.example.approllmaster.models.MonstruoBase
import com.google.firebase.firestore.FirebaseFirestore

class SelectMonstersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectMonstersBinding
    private val db = FirebaseFirestore.getInstance()

    private var idPartida: String = ""
    private var codigoSala: String = ""

    // Catálogo base con esbirros y un Jefe Final
    private val catalogoMonstruos = listOf(
        MonstruoBase(id = "m1", nombre = "Goblin Explorador", vida = 12, armadura = 14, ataque = "+4", esJefe = false),
        MonstruoBase(id = "m2", nombre = "Esqueleto Guerrero", vida = 15, armadura = 13, ataque = "+3", esJefe = false),
        MonstruoBase(id = "m3", nombre = "Orco Saqueador", vida = 28, armadura = 15, ataque = "+5", esJefe = false),
        MonstruoBase(id = "boss1", nombre = "Liche Nigromante", vida = 60, armadura = 17, ataque = "+7", esJefe = true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectMonstersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPartida = intent.getStringExtra("ID_PARTIDA") ?: ""
        codigoSala = intent.getStringExtra("CODIGO_SALA") ?: ""
        val nombrePartida = intent.getStringExtra("NOMBRE_PARTIDA") ?: "Partida sin nombre"


        binding.chipCodigoSala.text = codigoSala
        binding.tvNombreSalaCabecera.text = nombrePartida

        configurarRecyclerView()

        binding.btnIniciarCombate.setOnClickListener {
            confirmarEnemigosYEntrar()
        }
        binding.btnVolverSalas.setOnClickListener {
            finish()
        }
    }

    private fun configurarRecyclerView() {
        val adapter = MonstruoSeleccionAdapter(catalogoMonstruos) {
            actualizarTextoBoton()
        }
        binding.rvMonstruos.layoutManager = LinearLayoutManager(this)
        binding.rvMonstruos.adapter = adapter
        actualizarTextoBoton()
    }

    private fun actualizarTextoBoton() {
        val totalSeleccionados = catalogoMonstruos.sumOf { it.cantidadSeleccionada }
        binding.btnIniciarCombate.text = if (totalSeleccionados == 0) {
            "Iniciar Combate (Sin enemigos)"
        } else {
            "Iniciar Combate ($totalSeleccionados enemigos seleccionados)"
        }
    }

    private fun confirmarEnemigosYEntrar() {
        if (idPartida.isEmpty()) {
            Toast.makeText(this, "Error: Partida no identificada", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnIniciarCombate.isEnabled = false

        // Lista tipada explícitamente como HashMap
        val listaEnemigos = ArrayList<HashMap<String, Any>>()

        catalogoMonstruos.filter { it.cantidadSeleccionada > 0 }.forEach { monstruo ->
            for (i in 1..monstruo.cantidadSeleccionada) {
                val nombreUnico = if (monstruo.cantidadSeleccionada > 1) {
                    "${monstruo.nombre} #$i"
                } else {
                    monstruo.nombre
                }

                val datosEnemigo = HashMap<String, Any>()
                datosEnemigo["nombre"] = nombreUnico
                datosEnemigo["vidaMax"] = monstruo.vida
                datosEnemigo["vidaActual"] = monstruo.vida
                datosEnemigo["armadura"] = monstruo.armadura
                datosEnemigo["ataque"] = monstruo.ataque
                datosEnemigo["esJefe"] = monstruo.esJefe

                listaEnemigos.add(datosEnemigo)
            }
        }

        // Guardamos los enemigos en el documento de la partida
        db.collection("partidas").document(idPartida)
            .update("enemigos", listaEnemigos)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Encuentro configurado con éxito!", Toast.LENGTH_SHORT).show()

                // Aquí enlazaremos la apertura de la Mesa de Combate
            }
            .addOnFailureListener { e ->
                binding.btnIniciarCombate.isEnabled = true
                Toast.makeText(this, "Error al guardar enemigos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}