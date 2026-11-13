package com.example.approllmaster

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivityCombatTableBinding
import com.example.approllmaster.models.Combatiente
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.random.Random

class CombatTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCombatTableBinding
    private val db = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository()

    private var idPartida: String = ""
    private var codigoSala: String = ""
    private var nombrePartida: String = ""
    private var rolUsuario: String = "JUGADOR" // "MASTER" o "JUGADOR"
    private var uidActual: String = ""

    private lateinit var adapter: CombatienteAdapter
    private var listenerPartida: ListenerRegistration? = null

    private var listaActualCombatientes: List<Combatiente> = emptyList()
    private var estadoActual: String = "ESPERA"
    private var turnoActualIndex: Int = 0
    private var ultimoTimestampTiradaVisto: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCombatTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idPartida = intent.getStringExtra("ID_PARTIDA") ?: ""
        codigoSala = intent.getStringExtra("CODIGO_SALA") ?: "----"
        nombrePartida = intent.getStringExtra("NOMBRE_PARTIDA") ?: "Mesa de Juego"
        rolUsuario = intent.getStringExtra("ROL_USUARIO") ?: "JUGADOR"
        uidActual = authRepository.currentUser?.uid ?: ""

        binding.tvNombreSalaMesa.text = nombrePartida

        // La cuadrícula 2x2 de sonidos solo es visible para el Máster
        if (rolUsuario == "MASTER") {
            binding.layoutSonidosMaster.visibility = View.VISIBLE
            binding.btnSonidoEspadas.setOnClickListener {
                reproducirSonidoEfecto(ToneGenerator.TONE_PROP_BEEP2, "Choque de Espadas")
            }
            binding.btnSonidoHechizo.setOnClickListener {
                reproducirSonidoEfecto(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, "Bola de Fuego / Hechizo")
            }
            binding.btnSonidoRugido.setOnClickListener {
                reproducirSonidoEfecto(ToneGenerator.TONE_CDMA_LOW_L, "Rugido de Monstruo")
            }
            binding.btnSonidoVictoria.setOnClickListener {
                reproducirSonidoEfecto(ToneGenerator.TONE_PROP_ACK, "Fanfarria de Victoria")
            }
        }

        configurarRecyclerView()
        configurarBotones()
        escucharSalaEnTiempoReal()
    }

    private fun configurarRecyclerView() {
        adapter = CombatienteAdapter(emptyList(), "", false)
        binding.rvCombatientes.layoutManager = LinearLayoutManager(this)
        binding.rvCombatientes.adapter = adapter
    }

    private fun configurarBotones() {
        // Máster inicia combate y sortea orden aleatorio
        binding.btnSortearEIniciar.setOnClickListener {
            sortearTurnosEIniciarCombate()
        }

        // Jugador (o Máster con monstruo) realiza tirada en su turno
        binding.btnTirarDadosTurno.setOnClickListener {
            abrirSelectorObjetivoYTirarDados()
        }

        binding.btnSalirMesa.setOnClickListener {
            finish()
        }
    }

    private fun escucharSalaEnTiempoReal() {
        if (idPartida.isEmpty()) return

        listenerPartida = db.collection("partidas").document(idPartida)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                estadoActual = snapshot.getString("estado") ?: "ESPERA"
                turnoActualIndex = (snapshot.getLong("turnoActualIndex") ?: 0L).toInt()
                val ultimoLog = snapshot.getString("ultimoLog") ?: "Esperando acciones..."

                binding.tvUltimoLog.text = ultimoLog
                val textoFase = if (estadoActual == "COMBATE") "EN COMBATE" else "ANTESALA"
                binding.tvEstadoYSala.text = "Código: $codigoSala  •  $textoFase"

                // Leemos la lista de combatientes desde Firestore
                val parseados = mutableListOf<Combatiente>()
                val rawList = snapshot.get("combatientes") as? List<*>
                if (rawList != null) {
                    for (item in rawList) {
                        val map = item as? Map<*, *> ?: continue
                        parseados.add(
                            Combatiente(
                                id = map["id"] as? String ?: "",
                                idUsuario = map["idUsuario"] as? String ?: "",
                                nombre = map["nombre"] as? String ?: "",
                                tipo = map["tipo"] as? String ?: "JUGADOR",
                                claseOEstado = map["claseOEstado"] as? String ?: "",
                                psActuales = (map["psActuales"] as? Number)?.toInt() ?: 0,
                                psMaximos = (map["psMaximos"] as? Number)?.toInt() ?: 0,
                                ca = (map["ca"] as? Number)?.toInt() ?: 10,
                                bonoAtaque = (map["bonoAtaque"] as? Number)?.toInt() ?: 0,
                                ordenTurno = (map["ordenTurno"] as? Number)?.toInt() ?: 0,
                                avatarUrl = map["avatarUrl"] as? String ?: ""
                            )
                        )
                    }
                }

                // Si ya estamos en combate, ordenamos por ordenTurno
                listaActualCombatientes = if (estadoActual == "COMBATE") {
                    parseados.sortedBy { it.ordenTurno }
                } else {
                    parseados
                }

                // Comprobamos si hay una nueva tirada de dado para animarla en pantalla
                val tiradaTimestamp = snapshot.getLong("ultimaTiradaTimestamp") ?: 0L
                if (tiradaTimestamp > ultimoTimestampTiradaVisto) {
                    ultimoTimestampTiradaVisto = tiradaTimestamp
                    val atacanteDado = snapshot.getString("ultimaTiradaAtacante") ?: ""
                    val valorDado = (snapshot.getLong("ultimaTiradaValor") ?: 10L).toInt()
                    val resumenDado = snapshot.getString("ultimaTiradaResumen") ?: ""
                    mostrarAnimacionDadoD20(atacanteDado, valorDado, resumenDado)
                }

                actualizarInterfazSegunEstadoYTurno()
            }
    }

    private fun actualizarInterfazSegunEstadoYTurno() {
        val enCombate = estadoActual == "COMBATE"
        val combatienteActivo = if (enCombate && listaActualCombatientes.isNotEmpty()) {
            listaActualCombatientes[turnoActualIndex % listaActualCombatientes.size]
        } else {
            null
        }

        // Actualizamos tarjetas iluminando al personaje activo
        adapter.actualizarDatos(
            listaActualCombatientes,
            combatienteActivo?.id ?: "",
            enCombate
        )

        // Control de botones inferiores
        if (!enCombate) {
            // FASE ANTESALA
            binding.btnTirarDadosTurno.visibility = View.GONE
            if (rolUsuario == "MASTER") {
                binding.btnSortearEIniciar.visibility = View.VISIBLE
                binding.tvEsperandoMaster.visibility = View.GONE
            } else {
                binding.btnSortearEIniciar.visibility = View.GONE
                binding.tvEsperandoMaster.visibility = View.VISIBLE
            }
        } else {
            // FASE COMBATE ACTIVO
            binding.btnSortearEIniciar.visibility = View.GONE
            binding.tvEsperandoMaster.visibility = View.GONE

            val esMiTurno = if (combatienteActivo == null) {
                false
            } else if (rolUsuario == "MASTER") {
                // El Máster controla el turno cuando le toca a un ENEMIGO
                combatienteActivo.tipo == "ENEMIGO"
            } else {
                // El Jugador controla el turno cuando le toca a su héroe
                combatienteActivo.idUsuario == uidActual
            }

            if (esMiTurno && combatienteActivo != null) {
                binding.btnTirarDadosTurno.visibility = View.VISIBLE
                binding.btnTirarDadosTurno.text = "Tirar dado D20"
            } else {
                binding.btnTirarDadosTurno.visibility = View.GONE
            }
        }
    }

    private fun sortearTurnosEIniciarCombate() {
        if (listaActualCombatientes.isEmpty()) {
            Toast.makeText(this, "No hay combatientes en la sala", Toast.LENGTH_SHORT).show()
            return
        }

        // Mezclamos aleatoriamente a todos (héroes y monstruos) y asignamos orden 1..N
        val mezclados = listaActualCombatientes.shuffled().mapIndexed { index, combatiente ->
            hashMapOf<String, Any>(
                "id" to combatiente.id,
                "idUsuario" to combatiente.idUsuario,
                "nombre" to combatiente.nombre,
                "tipo" to combatiente.tipo,
                "claseOEstado" to combatiente.claseOEstado,
                "psActuales" to combatiente.psActuales,
                "psMaximos" to combatiente.psMaximos,
                "ca" to combatiente.ca,
                "bonoAtaque" to combatiente.bonoAtaque,
                "ordenTurno" to (index + 1),
                "avatarUrl" to combatiente.avatarUrl
            )
        }

        val primero = mezclados.first()["nombre"] as? String ?: "Alguien"

        db.collection("partidas").document(idPartida)
            .update(
                "combatientes", mezclados,
                "estado", "COMBATE",
                "turnoActualIndex", 0,
                "ultimoLog", "¡Turnos sorteados! Comienza el combate. Empieza: $primero."
            )
    }

    private fun abrirSelectorObjetivoYTirarDados() {
        if (listaActualCombatientes.isEmpty()) return
        val atacante = listaActualCombatientes[turnoActualIndex % listaActualCombatientes.size]

        val posiblesObjetivos = listaActualCombatientes.filter {
            it.psActuales > 0 && it.tipo != atacante.tipo
        }

        if (posiblesObjetivos.isEmpty()) {
            Toast.makeText(this, "¡No quedan enemigos en pie!", Toast.LENGTH_SHORT).show()
            return
        }

        val density = resources.displayMetrics.density
        fun dp(valor: Int): Int = (valor * density).toInt()

        // Tarjeta principal del diálogo sin borde
        val cardDialogo = com.google.android.material.card.MaterialCardView(this).apply {
            setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.superficie_tarjeta))
            radius = 18 * density
            cardElevation = 8 * density
            strokeWidth = 0
        }

        val contenedorPrincipal = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(14))
        }

        // Título centrado
        val tvTitulo = android.widget.TextView(this).apply {
            text = "Elige objetivo"
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_blanco))
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, dp(14))
        }
        contenedorPrincipal.addView(tvTitulo)

        // ScrollView por si hay muchos enemigos en sala
        val scrollObjetivos = android.widget.ScrollView(this)
        val listaBotones = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        val dialog = AlertDialog.Builder(this)
            .setView(cardDialogo)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        for (objetivo in posiblesObjetivos) {
            val cardOpcion = com.google.android.material.card.MaterialCardView(this).apply {
                setCardBackgroundColor(android.graphics.Color.parseColor("#24242A"))
                radius = 14 * density
                cardElevation = 0f
                strokeWidth = 0
                isClickable = true
                isFocusable = true
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }

            val fila = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(14), dp(10))
            }

            // Avatar circular del objetivo
            val cardAvatar = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(44), dp(44))
                radius = 22 * density
                cardElevation = 0f
                strokeWidth = 0
                setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.fondo_negro))
            }

            val ivAvatar = android.widget.ImageView(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageResource(R.mipmap.ic_launcher)

                val url = objetivo.avatarUrl.trim()
                if (url.isNotEmpty()) {
                    val resDrawable = resources.getIdentifier(url, "drawable", packageName)
                    val resMipmap = resources.getIdentifier(url, "mipmap", packageName)
                    if (resDrawable != 0) setImageResource(resDrawable)
                    else if (resMipmap != 0) setImageResource(resMipmap)
                }
            }
            cardAvatar.addView(ivAvatar)
            fila.addView(cardAvatar)

            // Columna central: Nombre + PS/CA
            val colTexto = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    marginStart = dp(12)
                    marginEnd = dp(8)
                }
            }

            val tvNombre = android.widget.TextView(this).apply {
                text = objetivo.nombre
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_blanco))
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvStats = android.widget.TextView(this).apply {
                text = "PS: ${objetivo.psActuales}/${objetivo.psMaximos}  •  CA: ${objetivo.ca}"
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_gris_claro))
                textSize = 12f
                setPadding(0, dp(2), 0, 0)
            }

            colTexto.addView(tvNombre)
            colTexto.addView(tvStats)
            fila.addView(colTexto)

            // Indicador de acción a la derecha
            val tvAccion = android.widget.TextView(this).apply {
                text = "›"
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_claro))
                textSize = 22f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            fila.addView(tvAccion)

            cardOpcion.addView(fila)
            cardOpcion.setOnClickListener {
                dialog.dismiss()
                ejecutarTiradaYAvanzarTurno(atacante, objetivo)
            }

            listaBotones.addView(cardOpcion)
        }

        scrollObjetivos.addView(listaBotones)
        contenedorPrincipal.addView(scrollObjetivos)

        // Botón Cancelar estilizado abajo
        val btnCancelar = com.google.android.material.button.MaterialButton(
            this,
            null,
            android.R.attr.borderlessButtonStyle
        ).apply {
            text = "CANCELAR"
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.rojo_claro))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
            setOnClickListener { dialog.dismiss() }
        }
        contenedorPrincipal.addView(btnCancelar)

        cardDialogo.addView(contenedorPrincipal)
        dialog.show()
    }

    private fun ejecutarTiradaYAvanzarTurno(atacante: Combatiente, objetivo: Combatiente) {
        val dadoD20 = Random.nextInt(1, 21) // Tirada de ataque (1 al 20)
        val totalAtaque = dadoD20 + atacante.bonoAtaque

        var danio = 0
        val mensajeLog: String

        when {
            dadoD20 == 20 -> {
                val dadoDanio = Random.nextInt(1, 9)
                danio = (dadoDanio + 2) * 2
                mensajeLog = "¡CRÍTICO! ${atacante.nombre} ataca a ${objetivo.nombre}\n" +
                        "Ataque: $totalAtaque (D20: 20 + ${atacante.bonoAtaque}) vs CA ${objetivo.ca}\n" +
                        "Daño crítico: -$danio PS [(D8: $dadoDanio + 2) x 2]"
            }
            dadoD20 == 1 -> {
                mensajeLog = "¡PIFIA! ${atacante.nombre} ataca a ${objetivo.nombre}\n" +
                        "Ataque: $totalAtaque (D20: 1 + ${atacante.bonoAtaque}) vs CA ${objetivo.ca}\n" +
                        "Resultado: Fallo automático por pifia"
            }
            totalAtaque >= objetivo.ca -> {
                val dadoDanio = Random.nextInt(1, 9)
                danio = dadoDanio + 2
                mensajeLog = "${atacante.nombre} ataca a ${objetivo.nombre}\n" +
                        "Ataque: $totalAtaque (D20: $dadoD20 + ${atacante.bonoAtaque}) vs CA ${objetivo.ca} -> ¡Impacta!\n" +
                        "Daño: -$danio PS (D8: $dadoDanio + 2)"
            }
            else -> {
                mensajeLog = "${atacante.nombre} ataca a ${objetivo.nombre}\n" +
                        "Ataque: $totalAtaque (D20: $dadoD20 + ${atacante.bonoAtaque}) vs CA ${objetivo.ca}\n" +
                        "Resultado: El objetivo esquiva el golpe"
            }
        }

        // Actualizamos la vida del objetivo en la lista
        val nuevaLista = listaActualCombatientes.map { c ->
            val nuevaVida = if (c.id == objetivo.id) {
                (c.psActuales - danio).coerceAtLeast(0)
            } else {
                c.psActuales
            }
            hashMapOf<String, Any>(
                "id" to c.id,
                "idUsuario" to c.idUsuario,
                "nombre" to c.nombre,
                "tipo" to c.tipo,
                "claseOEstado" to c.claseOEstado,
                "psActuales" to nuevaVida,
                "psMaximos" to c.psMaximos,
                "ca" to c.ca,
                "bonoAtaque" to c.bonoAtaque,
                "ordenTurno" to c.ordenTurno,
                "avatarUrl" to c.avatarUrl
            )
        }

        // Calculamos el siguiente turno saltando a los que tengan 0 PS
        val siguienteIndice = calcularSiguienteTurnoVivo(nuevaLista, turnoActualIndex)

        val resumenDado = when {
            dadoD20 == 20 -> "¡CRÍTICO! Ataque: $totalAtaque | Daño: $danio PS"
            dadoD20 == 1 -> "¡PIFIA! (D20: 1)"
            totalAtaque >= objetivo.ca -> "¡IMPACTO! ($dadoD20+${atacante.bonoAtaque}=$totalAtaque vs CA ${objetivo.ca}) • Daño: $danio PS"
            else -> "FALLO ($dadoD20+${atacante.bonoAtaque}=$totalAtaque vs CA ${objetivo.ca})"
        }

        db.collection("partidas").document(idPartida)
            .update(
                "combatientes", nuevaLista,
                "turnoActualIndex", siguienteIndice,
                "ultimoLog", mensajeLog,
                "ultimaTiradaTimestamp", System.currentTimeMillis(),
                "ultimaTiradaAtacante", atacante.nombre,
                "ultimaTiradaValor", dadoD20,
                "ultimaTiradaResumen", resumenDado
            )
    }

    private fun calcularSiguienteTurnoVivo(lista: List<HashMap<String, Any>>, indiceActual: Int): Int {
        val total = lista.size
        if (total == 0) return 0
        for (salto in 1..total) {
            val candidato = (indiceActual + salto) % total
            val vida = (lista[candidato]["psActuales"] as? Number)?.toInt() ?: 0
            if (vida > 0) return candidato
        }
        return (indiceActual + 1) % total
    }

    private fun reproducirSonidoEfecto(tipoTono: Int, nombreEfecto: String) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            toneGen.startTone(tipoTono, 450)
            Toast.makeText(this, "Reproduciendo: $nombreEfecto", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Sonido: $nombreEfecto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarAnimacionDadoD20(atacante: String, resultadoFinal: Int, resumen: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tirada_d20, null)
        val tvTitulo = dialogView.findViewById<android.widget.TextView>(R.id.tvTituloTirada)
        val ivDado = dialogView.findViewById<android.widget.ImageView>(R.id.ivDadoD20)
        val tvNumero = dialogView.findViewById<android.widget.TextView>(R.id.tvNumeroDado)
        val tvDetalle = dialogView.findViewById<android.widget.TextView>(R.id.tvDetalleResultadoDado)

        tvTitulo.text = "Tirada de $atacante"
        tvDetalle.text = "Rodando el dado..."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Giramos el dado 3 vueltas (1080 grados) durante 1.6 segundos
        ivDado.animate()
            .rotationBy(1080f)
            .setDuration(1600L)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val toneGen = try { ToneGenerator(AudioManager.STREAM_MUSIC, 85) } catch (e: Exception) { null }

        // Efecto de números cambiando y sonido de dado rodando (12 pasos de 120ms = 1.44s)
        for (paso in 0..11) {
            handler.postDelayed({
                if (dialog.isShowing) {
                    tvNumero.text = Random.nextInt(1, 21).toString()
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                }
            }, paso * 120L)
        }

        // A los 1.6 segundos clavamos el número real y sonamos impacto/crítico
        handler.postDelayed({
            if (dialog.isShowing) {
                tvNumero.text = resultadoFinal.toString()
                tvDetalle.text = resumen

                when (resultadoFinal) {
                    20 -> {
                        tvNumero.setTextColor(android.graphics.Color.parseColor("#FFD700")) // Oro
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
                    }
                    1 -> {
                        tvNumero.setTextColor(android.graphics.Color.parseColor("#FF5252")) // Rojo pifia
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 400)
                    }
                    else -> {
                        tvNumero.setTextColor(android.graphics.Color.WHITE)
                        toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
                    }
                }
            }
        }, 1600L)

        // A los 3.3 segundos cerramos automáticamente el dado para seguir jugando
        handler.postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            toneGen?.release()
        }, 3300L)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerPartida?.remove()
    }
}