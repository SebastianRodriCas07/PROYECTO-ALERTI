package com.example.alerti

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.telephony.SmsManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alerti.data.AppDatabase
import com.example.alerti.data.Contacto
import com.example.alerti.data.EventoAlerta
import com.example.alerti.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var giroscopio: Sensor? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ================= BASE DE DATOS =================
    private lateinit var database: AppDatabase
    private var contactosGuardados: List<Contacto> = emptyList()

    // ================= PARÁMETROS DEL ALGORITMO =================
    private val UMBRAL_CAIDA_LIBRE = 5.9f
    private val UMBRAL_IMPACTO = 21.0f
    private val JERK_MINIMO_IMPACTO = 150f
    private val VENTANA_IMPACTO_MS = 800L
    private val VENTANA_INMOVILIDAD_MS = 2000L
    private val TOLERANCIA_INMOVILIDAD = 1.5f
    private val UMBRAL_GIROSCOPIO = 3.5f
    private val ALPHA_FILTRO = 0.3f

    private enum class EstadoDeteccion {
        NORMAL,
        CAIDA_LIBRE,
        IMPACTO_DETECTADO,
        VERIFICANDO_INMOVILIDAD
    }

    private var estado = EstadoDeteccion.NORMAL
    private var tiempoInicioCaidaLibre = 0L
    private var tiempoInicioVerificacion = 0L
    private var giroscopioActivoDuranteEvento = false
    private var magnitudFiltrada = 9.81f
    private var magnitudAnterior = 9.81f
    private var tiempoAnteriorNs = 0L
    private val bufferMagnitudes = mutableListOf<Float>()

    private var contadorNotificacion: CountDownTimer? = null
    private var contadorAlarma: CountDownTimer? = null
    private var mediaPlayerAlarma: MediaPlayer? = null
    private var protocoloActivo = false

    private val CODIGO_PERMISOS = 100

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ================= SENSORES =================

        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        acelerometro =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        giroscopio =
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // ================= UBICACIÓN =================

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // ================= BASE DE DATOS =================

        database = AppDatabase.obtenerInstancia(this)

        cargarContactosDesdeBaseDeDatos()

        insertarContactoDePruebaSiNoExiste()

        // ================= BOTÓN ESTOY BIEN =================

        binding.botonEstoyBien.setOnClickListener {
            cancelarProtocolo()
        }

        // ================= CONTACTOS DE EMERGENCIA =================

        binding.botonConfigurarContactos.setOnClickListener {
            mostrarDialogoAgregarContacto()
        }

        // ================= PERMISOS =================

        pedirPermisosSiHacenFalta()
    }

    // =========================================================
    // AGREGAR CONTACTO
    // =========================================================

    private fun mostrarDialogoAgregarContacto() {

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 10, 50, 10)
        }

        val campoNombre = EditText(this).apply {
            hint = "Nombre del contacto"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val campoNumero = EditText(this).apply {
            hint = "Número de teléfono"
            inputType = InputType.TYPE_CLASS_PHONE
        }

        val campoPrioridad = EditText(this).apply {
            hint = "Prioridad (1, 2 o 3)"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        contenedor.addView(campoNombre)
        contenedor.addView(campoNumero)
        contenedor.addView(campoPrioridad)

        AlertDialog.Builder(this)
            .setTitle("Contacto de emergencia")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->

                val nombre =
                    campoNombre.text.toString().trim()

                val numero =
                    campoNumero.text.toString().trim()

                val prioridadTexto =
                    campoPrioridad.text.toString().trim()

                // ================= VALIDACIONES =================

                if (nombre.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Escribe el nombre del contacto",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (numero.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Escribe el número de teléfono",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val prioridad =
                    prioridadTexto.toIntOrNull()

                if (prioridad == null || prioridad !in 1..3) {
                    Toast.makeText(
                        this,
                        "La prioridad debe ser 1, 2 o 3",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                // ================= GUARDAR =================

                CoroutineScope(Dispatchers.IO).launch {

                    database.contactoDao().insertar(
                        Contacto(
                            nombre = nombre,
                            numero = numero,
                            prioridad = prioridad
                        )
                    )

                    contactosGuardados =
                        database.contactoDao().obtenerTodosUnaVez()

                    runOnUiThread {

                        Toast.makeText(
                            this@MainActivity,
                            "Contacto guardado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    // =========================================================
    // CONTACTO DE PRUEBA
    // =========================================================

    private fun insertarContactoDePruebaSiNoExiste() {

        CoroutineScope(Dispatchers.IO).launch {

            val actuales =
                database.contactoDao().obtenerTodosUnaVez()

            if (actuales.isEmpty()) {

                database.contactoDao().insertar(
                    Contacto(
                        nombre = "Contacto de prueba",
                        numero = "3103375555",
                        prioridad = 1
                    )
                )

                contactosGuardados =
                    database.contactoDao().obtenerTodosUnaVez()
            }
        }
    }

    // =========================================================
    // CARGAR CONTACTOS
    // =========================================================

    private fun cargarContactosDesdeBaseDeDatos() {

        CoroutineScope(Dispatchers.IO).launch {

            contactosGuardados =
                database.contactoDao().obtenerTodosUnaVez()
        }
    }

    // =========================================================
    // PERMISOS
    // =========================================================

    private fun pedirPermisosSiHacenFalta() {

        val permisosNecesarios = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val faltantes =
            permisosNecesarios.filter {

                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (faltantes.isNotEmpty()) {

            ActivityCompat.requestPermissions(
                this,
                faltantes.toTypedArray(),
                CODIGO_PERMISOS
            )
        }
    }

    // =========================================================
    // RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        acelerometro?.also {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        giroscopio?.also {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    override fun onPause() {

        super.onPause()

        sensorManager.unregisterListener(this)
    }

    // =========================================================
    // SENSOR
    // =========================================================

    override fun onSensorChanged(event: SensorEvent?) {

        event ?: return

        if (protocoloActivo) return

        when (event.sensor.type) {

            Sensor.TYPE_ACCELEROMETER ->
                procesarAcelerometro(event)

            Sensor.TYPE_GYROSCOPE ->
                procesarGiroscopio(event)
        }
    }

    // =========================================================
    // GIROSCOPIO
    // =========================================================

    private fun procesarGiroscopio(event: SensorEvent) {

        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]

        val velocidadAngular =
            sqrt(gx * gx + gy * gy + gz * gz)

        if (
            estado != EstadoDeteccion.NORMAL &&
            velocidadAngular > UMBRAL_GIROSCOPIO
        ) {

            giroscopioActivoDuranteEvento = true
        }
    }

    // =========================================================
    // ACELERÓMETRO
    // =========================================================

    private fun procesarAcelerometro(event: SensorEvent) {

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitudCruda =
            sqrt(x * x + y * y + z * z)

        magnitudFiltrada =
            ALPHA_FILTRO * magnitudCruda +
                    (1 - ALPHA_FILTRO) * magnitudFiltrada

        val ahoraNs = event.timestamp

        val jerk =
            if (tiempoAnteriorNs != 0L) {

                val deltaTs =
                    (ahoraNs - tiempoAnteriorNs) /
                            1_000_000_000f

                if (deltaTs > 0) {

                    abs(
                        magnitudFiltrada -
                                magnitudAnterior
                    ) / deltaTs

                } else {
                    0f
                }

            } else {
                0f
            }

        magnitudAnterior = magnitudFiltrada
        tiempoAnteriorNs = ahoraNs

        val ahoraMs =
            SystemClock.elapsedRealtime()

        binding.textoValoresSensor.text =
            "Magnitud: %.2f | Jerk: %.1f"
                .format(magnitudFiltrada, jerk)

        when (estado) {

            // ================= NORMAL =================

            EstadoDeteccion.NORMAL -> {

                if (
                    magnitudFiltrada <
                    UMBRAL_CAIDA_LIBRE
                ) {

                    estado =
                        EstadoDeteccion.CAIDA_LIBRE

                    tiempoInicioCaidaLibre =
                        ahoraMs

                    giroscopioActivoDuranteEvento =
                        false
                }
            }

            // ================= CAÍDA LIBRE =================

            EstadoDeteccion.CAIDA_LIBRE -> {

                val transcurrido =
                    ahoraMs - tiempoInicioCaidaLibre

                val esImpactoValido =
                    magnitudFiltrada >
                            UMBRAL_IMPACTO &&
                            jerk >
                            JERK_MINIMO_IMPACTO

                if (
                    esImpactoValido &&
                    transcurrido <=
                    VENTANA_IMPACTO_MS
                ) {

                    estado =
                        EstadoDeteccion.IMPACTO_DETECTADO

                } else if (
                    transcurrido >
                    VENTANA_IMPACTO_MS
                ) {

                    estado =
                        EstadoDeteccion.NORMAL
                }
            }

            // ================= IMPACTO =================

            EstadoDeteccion.IMPACTO_DETECTADO -> {

                estado =
                    EstadoDeteccion.VERIFICANDO_INMOVILIDAD

                tiempoInicioVerificacion =
                    ahoraMs

                bufferMagnitudes.clear()

                bufferMagnitudes.add(
                    magnitudFiltrada
                )
            }

            // ================= INMOVILIDAD =================

            EstadoDeteccion.VERIFICANDO_INMOVILIDAD -> {

                bufferMagnitudes.add(
                    magnitudFiltrada
                )

                val transcurrido =
                    ahoraMs - tiempoInicioVerificacion

                if (
                    transcurrido >=
                    VENTANA_INMOVILIDAD_MS
                ) {

                    val promedio =
                        bufferMagnitudes
                            .average()
                            .toFloat()

                    val variacionMaxima =
                        bufferMagnitudes.maxOf {
                            abs(it - promedio)
                        }

                    val huboInmovilidad =
                        variacionMaxima <
                                TOLERANCIA_INMOVILIDAD

                    if (huboInmovilidad) {

                        onCaidaConfirmada()
                    }

                    estado =
                        EstadoDeteccion.NORMAL

                    bufferMagnitudes.clear()
                }
            }
        }
    }

    // =========================================================
    // CAÍDA CONFIRMADA
    // =========================================================

    private fun onCaidaConfirmada() {

        protocoloActivo = true

        iniciarEtapaNotificacion()
    }

    // =========================================================
    // NOTIFICACIÓN
    // =========================================================

    private fun iniciarEtapaNotificacion() {

        binding.botonEstoyBien.visibility =
            View.VISIBLE

        binding.iconoEstado.text = "⚠️"

        contadorNotificacion =
            object : CountDownTimer(
                30_000,
                1000
            ) {

                override fun onTick(
                    msRestante: Long
                ) {

                    binding.textoEstado.text =
                        "¿Estás bien?"

                    binding.textoSubestado.text =
                        "Respondiendo en ${msRestante / 1000}s"

                    binding.textoEstado.setTextColor(
                        resources.getColor(
                            R.color.alerti_naranja,
                            theme
                        )
                    )
                }

                override fun onFinish() {

                    iniciarEtapaAlarma()
                }
            }.start()
    }

    // =========================================================
    // ALARMA
    // =========================================================

    private fun iniciarEtapaAlarma() {

        binding.iconoEstado.text = "🚨"

        binding.textoEstado.text =
            "Alarma activada"

        binding.textoEstado.setTextColor(
            resources.getColor(
                R.color.alerti_rojo,
                theme
            )
        )

        val uriAlarma =
            RingtoneManager.getActualDefaultRingtoneUri(
                this,
                RingtoneManager.TYPE_ALARM
            )

        mediaPlayerAlarma =
            MediaPlayer().apply {

                setDataSource(
                    this@MainActivity,
                    uriAlarma
                )

                isLooping = true

                prepare()

                start()
            }

        contadorAlarma =
            object : CountDownTimer(
                30_000,
                1000
            ) {

                override fun onTick(
                    msRestante: Long
                ) {

                    binding.textoSubestado.text =
                        "Enviando SMS en ${msRestante / 1000}s"
                }

                override fun onFinish() {

                    detenerAlarma()

                    enviarSmsDeEmergencia()
                }
            }.start()
    }

    // =========================================================
    // CANCELAR PROTOCOLO
    // =========================================================

    private fun cancelarProtocolo() {

        contadorNotificacion?.cancel()

        contadorAlarma?.cancel()

        detenerAlarma()

        binding.botonEstoyBien.visibility =
            View.GONE

        binding.iconoEstado.text = "✅"

        binding.textoEstado.text =
            "Todo en orden"

        binding.textoEstado.setTextColor(
            resources.getColor(
                R.color.alerti_texto,
                theme
            )
        )

        binding.textoSubestado.text =
            "Monitoreando sensores..."

        protocoloActivo = false

        // Registrar evento cancelado

        CoroutineScope(Dispatchers.IO).launch {

            database.eventoAlertaDao().insertar(

                EventoAlerta(

                    fechaHoraMillis =
                        System.currentTimeMillis(),

                    latitud = null,

                    longitud = null,

                    magnitudPico =
                        magnitudFiltrada,

                    estado = "CANCELADO",

                    contactosNotificados = 0
                )
            )
        }
    }

    // =========================================================
    // DETENER ALARMA
    // =========================================================

    private fun detenerAlarma() {

        mediaPlayerAlarma?.apply {

            if (isPlaying) {
                stop()
            }

            release()
        }

        mediaPlayerAlarma = null
    }

    // =========================================================
    // ENVIAR SMS
    // =========================================================

    private fun enviarSmsDeEmergencia() {

        binding.botonEstoyBien.visibility =
            View.GONE

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            binding.textoEstado.text =
                "❌ Falta permiso de ubicación"

            protocoloActivo = false

            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                val mensaje =

                    if (location != null) {

                        "🚨 ALERTI: posible accidente detectado. " +
                                "Ubicación: " +
                                "https://maps.google.com/?q=" +
                                "${location.latitude}," +
                                "${location.longitude}"

                    } else {

                        "🚨 ALERTI: posible accidente detectado. " +
                                "No se pudo obtener la ubicación exacta."
                    }

                enviarSmsATodosLosContactos(

                    mensaje,

                    location?.latitude,

                    location?.longitude,

                    magnitudFiltrada
                )
            }
            .addOnFailureListener {

                enviarSmsATodosLosContactos(

                    "🚨 ALERTI: posible accidente detectado. " +
                            "No se pudo obtener la ubicación.",

                    null,

                    null,

                    magnitudFiltrada
                )
            }
    }

    // =========================================================
    // ENVIAR SMS A TODOS LOS CONTACTOS
    // =========================================================

    private fun enviarSmsATodosLosContactos(
        mensaje: String,
        latitud: Double?,
        longitud: Double?,
        magnitudPico: Float
    ) {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            binding.textoEstado.text =
                "❌ Falta permiso de SMS"

            protocoloActivo = false

            return
        }

        val smsManager =
            getSystemService(SmsManager::class.java)

        var enviados = 0

        for (contacto in contactosGuardados) {

            try {

                smsManager.sendTextMessage(
                    contacto.numero,
                    null,
                    mensaje,
                    null,
                    null
                )

                enviados++

                Log.d(
                    "ALERTI_SMS",
                    "SMS enviado a ${contacto.nombre} " +
                            "(${contacto.numero})"
                )

            } catch (e: Exception) {

                Log.e(
                    "ALERTI_SMS",
                    "Error enviando SMS a " +
                            "${contacto.numero}: ${e.message}"
                )
            }
        }

        // ================= GUARDAR EVENTO =================

        CoroutineScope(Dispatchers.IO).launch {

            database.eventoAlertaDao().insertar(

                EventoAlerta(

                    fechaHoraMillis =
                        System.currentTimeMillis(),

                    latitud = latitud,

                    longitud = longitud,

                    magnitudPico =
                        magnitudPico,

                    estado = "CONFIRMADO",

                    contactosNotificados =
                        enviados
                )
            )
        }

        // ================= ACTUALIZAR INTERFAZ =================

        binding.iconoEstado.text = "✅"

        binding.textoEstado.text =
            "SMS enviado"

        binding.textoEstado.setTextColor(
            resources.getColor(
                R.color.alerti_verde,
                theme
            )
        )

        binding.textoSubestado.text =
            "Contactos notificados: $enviados"

        protocoloActivo = false
    }

    // =========================================================
    // ACCURACY
    // =========================================================

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}


