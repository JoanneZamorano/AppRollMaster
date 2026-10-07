package com.example.approllmaster.repository

import com.example.approllmaster.models.Combatiente
import com.example.approllmaster.models.Partida
import com.example.approllmaster.models.TiradaDado
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.random.Random

class PartidaRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val coleccionPartidas = db.collection("partidas")

    // crea una sala con un codigo tipo ROLL84 para que se unan los demas
    fun crearPartida(
        idMaster: String,
        onSuccess: (Partida) -> Unit,
        onError: (String) -> Unit
    ) {
        val codigoGenerado = "ROLL" + Random.nextInt(10, 99)
        val docRef = coleccionPartidas.document()

        val nuevaPartida = Partida(
            idPartida = docRef.id,
            codigoSala = codigoGenerado,
            idMaster = idMaster,
            estado = "ESPERA"
        )

        docRef.set(nuevaPartida)
            .addOnSuccessListener {
                onSuccess(nuevaPartida)
            }
            .addOnFailureListener { error ->
                onError("Error al crear la partida: ${error.message}")
            }
    }

    // busca la sala que tenga el PIN que ha escrito el jugador
    fun unirsePorCodigo(
        codigo: String,
        onSuccess: (Partida) -> Unit,
        onError: (String) -> Unit
    ) {
        coleccionPartidas.whereEqualTo("codigoSala", codigo)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val partida = doc.toObject(Partida::class.java)
                    if (partida != null) {
                        onSuccess(partida)
                    } else {
                        onError("No se pudo leer la info de la partida")
                    }
                } else {
                    onError("No existe ninguna sala con ese codigo: $codigo")
                }
            }
            .addOnFailureListener { error ->
                onError("Error al buscar sala: ${error.message}")
            }
    }

    // guarda una tirada en la partida (y marca si ha salido 20)
    fun registrarTirada(
        idPartida: String,
        remitente: String,
        valorDado: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val tirada = TiradaDado(
            idTirada = db.collection("partidas").document().id,
            remitente = remitente,
            valor = valorDado,
            esCritico = (valorDado == 20)
        )

        coleccionPartidas.document(idPartida)
            .collection("tiradas")
            .add(tirada)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error -> onError("Error al guardar tirada: ${error.message}") }
    }

    //escucha en directo si entra gente a la sala o si les cambia la vida
    fun escucharCombatientes(
        idPartida: String,
        onActualizacion: (List<Combatiente>) -> Unit
    ): ListenerRegistration {
        return coleccionPartidas.document(idPartida)
            .collection("combatientes")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val combatientes = snapshot.toObjects(Combatiente::class.java)
                    onActualizacion(combatientes)
                }
            }
    }
}