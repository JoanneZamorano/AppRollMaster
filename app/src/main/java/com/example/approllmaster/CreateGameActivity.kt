package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivityCreateGameBinding
import com.example.approllmaster.models.Partida
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class CreateGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGameBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val listaMisPartidas = mutableListOf<Partida>()
    private lateinit var adapter: PartidaMasterAdapter

    private val escenariosDisponibles = listOf(
        "Mazmorra tenebrosa",
        "Bosque encantado",
        "Montaña",
        "Taberna"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarDesplegableEscenarios()
        configurarRecyclerView()
        cargarMisPartidas()

        binding.btnGenerarPartida.setOnClickListener {
            val nombre = binding.etNombrePartida.text.toString().trim()
            if (nombre.isEmpty()) {
                binding.tilNombrePartida.error = "Escribe un nombre para la partida"
                return@setOnClickListener
            }
            binding.tilNombrePartida.error = null

            val escenarioSeleccionado = binding.actvEscenario.text.toString().ifEmpty { "Mazmorra tenebrosa" }
            crearSalaMaster(nombre, escenarioSeleccionado)
        }

        binding.btnVolverMenu.setOnClickListener {
            finish()
        }
    }

    private fun configurarDesplegableEscenarios() {
        val adapterEscenarios = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            escenariosDisponibles
        )
        binding.actvEscenario.setAdapter(adapterEscenarios)
        binding.actvEscenario.setText(escenariosDisponibles[0], false)
    }

    private fun configurarRecyclerView() {
        adapter = PartidaMasterAdapter(
            listaMisPartidas,
            onPartidaClick = { partidaSeleccionada ->
                val intent = Intent(this, SelectMonstersActivity::class.java).apply {
                    putExtra("ID_PARTIDA", partidaSeleccionada.idPartida)
                    putExtra("CODIGO_SALA", partidaSeleccionada.codigoSala)
                    putExtra("NOMBRE_PARTIDA", partidaSeleccionada.nombrePartida)
                    putExtra("ESCENARIO", partidaSeleccionada.escenario)
                }
                startActivity(intent)
            },
            onEliminarClick = { partidaAEliminar ->
                confirmarYEliminarPartida(partidaAEliminar)
            }
        )
        binding.rvMisPartidas.layoutManager = LinearLayoutManager(this)
        binding.rvMisPartidas.adapter = adapter
    }

    private fun confirmarYEliminarPartida(partida: Partida) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar sala")
            .setMessage("¿Seguro que deseas eliminar la sala \"${partida.nombrePartida}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarPartidaDeFirestore(partida.idPartida)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPartidaDeFirestore(idPartida: String) {
        mostrarCarga(true)
        db.collection("partidas").document(idPartida)
            .delete()
            .addOnSuccessListener {
                mostrarCarga(false)
                Toast.makeText(this, "Sala eliminada con éxito", Toast.LENGTH_SHORT).show()
                cargarMisPartidas()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarMisPartidas() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("partidas")
            .whereEqualTo("idMaster", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                listaMisPartidas.clear()
                for (doc in snapshot.documents) {
                    val partida = doc.toObject(Partida::class.java)
                    if (partida != null) {
                        listaMisPartidas.add(partida)
                    }
                }
                adapter.notifyDataSetChanged()

                if (listaMisPartidas.isEmpty()) {
                    binding.tvSinPartidas.visibility = View.VISIBLE
                    binding.rvMisPartidas.visibility = View.GONE
                } else {
                    binding.tvSinPartidas.visibility = View.GONE
                    binding.rvMisPartidas.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar tus partidas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun crearSalaMaster(nombre: String, escenario: String) {
        val userId = auth.currentUser?.uid ?: return
        mostrarCarga(true)

        val pinAleatorio = "ROLL${Random.nextInt(10, 99)}"
        val docRef = db.collection("partidas").document()

        val nuevaPartida = Partida(
            idPartida = docRef.id,
            nombrePartida = nombre,
            codigoSala = pinAleatorio,
            idMaster = userId,
            estado = "ESPERA",
            escenario = escenario
        )

        docRef.set(nuevaPartida)
            .addOnSuccessListener {
                mostrarCarga(false)
                Toast.makeText(this, "Sala $pinAleatorio creada", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, SelectMonstersActivity::class.java).apply {
                    putExtra("ID_PARTIDA", docRef.id)
                    putExtra("CODIGO_SALA", pinAleatorio)
                    putExtra("NOMBRE_PARTIDA", nombre)
                    putExtra("ESCENARIO", escenario)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                Toast.makeText(this, "Error al crear sala: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGenerarPartida.isEnabled = !cargando
    }
}