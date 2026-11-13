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

    // Catálogo base con esbirros y un Jefe Final (incluyendo sus avatares de drawable)
    private val catalogoMonstruos = listOf(
        MonstruoBase(id = "m1", nombre = "Goblin Explorador", vida = 12, armadura = 14, ataque = "+4", esJefe = false, avatarUrl = "avatar_goblin"),
        MonstruoBase(id = "m2", nombre = "Esqueleto Guerrero", vida = 15, armadura = 13, ataque = "+3", esJefe = false, avatarUrl = "avatar_esqueleto"),
        MonstruoBase(id = "m3", nombre = "Orco Saqueador", vida = 28, armadura = 15, ataque = "+5", esJefe = false, avatarUrl = "avatar_orco"),
        MonstruoBase(id = "boss1", nombre = "Liche Nigromante", vida = 60, armadura = 17, ataque = "+7", esJefe = true, avatarUrl = "avatar_liche")
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
        val nombrePartida = intent.getStringExtra("NOMBRE_PARTIDA") ?: "Sala de Juego"

        val listaCombatientes = ArrayList<HashMap<String, Any>>()

        catalogoMonstruos.filter { it.cantidadSeleccionada > 0 }.forEach { monstruo ->
            for (i in 1..monstruo.cantidadSeleccionada) {
                val nombreUnico = if (monstruo.cantidadSeleccionada > 1) {
                    "${monstruo.nombre} #$i"
                } else {
                    monstruo.nombre
                }

                val bonoInt = monstruo.ataque.replace("+", "").toIntOrNull() ?: 0

                val datosEnemigo = HashMap<String, Any>()
                datosEnemigo["id"] = "${monstruo.id}_$i"
                datosEnemigo["idUsuario"] = "MASTER"
                datosEnemigo["nombre"] = nombreUnico
                datosEnemigo["tipo"] = "ENEMIGO"
                datosEnemigo["claseOEstado"] = if (monstruo.esJefe) "Jefe Final" else "Esbirro"
                datosEnemigo["psActuales"] = monstruo.vida
                datosEnemigo["psMaximos"] = monstruo.vida
                datosEnemigo["ca"] = monstruo.armadura
                datosEnemigo["bonoAtaque"] = bonoInt
                datosEnemigo["ordenTurno"] = 0
                datosEnemigo["avatarUrl"] = monstruo.avatarUrl

                listaCombatientes.add(datosEnemigo)
            }
        }

        val actualizaciones = hashMapOf<String, Any>(
            "combatientes" to listaCombatientes,
            "estado" to "ESPERA",
            "turnoActualIndex" to 0,
            "ultimoLog" to "El Máster ha abierto la sala. Esperando jugadores..."
        )

        db.collection("partidas").document(idPartida)
            .update(actualizaciones)
            .addOnSuccessListener {
                val intent = Intent(this, CombatTableActivity::class.java).apply {
                    putExtra("ID_PARTIDA", idPartida)
                    putExtra("CODIGO_SALA", codigoSala)
                    putExtra("NOMBRE_PARTIDA", nombrePartida)
                    putExtra("ROL_USUARIO", "MASTER")
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnIniciarCombate.isEnabled = true
                Toast.makeText(this, "Error al guardar enemigos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}