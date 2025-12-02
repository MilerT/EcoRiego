package com.example.riegosostenible

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.riegosostenible.databinding.ActivityMainBinding
import com.example.riegosostenible.network.ApiClient
import com.example.riegosostenible.network.HistorialData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import android.Manifest
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val apiService = ApiClient.apiService
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // --- Lógica movida a su lugar correcto ---
        pedirPermisosBluetooth() // Pedimos permisos al crear la actividad
        // --- Configuración de Botones ---
        setupButtons()

        // --- Configuración Inicial del Gráfico ---
        setupChart()

        // --- Cargar Datos del Gráfico desde la API ---
        loadChartData()
    }





    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private var dispositivoEncontrado: BluetoothDevice? = null
    private fun pedirPermisosBluetooth() {
        // Primero, definimos qué permisos necesitamos según la versión de Android
        val permisosNecesarios = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Verificamos si ya tenemos los permisos
        val permisosFaltantes = permisosNecesarios.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Si faltan permisos, los pedimos al usuario
        if (permisosFaltantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permisosFaltantes.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        } else {
            // Si ya tenemos los permisos, podríamos iniciar la búsqueda aquí si quisiéramos
            Log.d("Bluetooth", "Los permisos de Bluetooth ya están concedidos.")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verificamos que la respuesta sea para nuestra solicitud de Bluetooth
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Comprobamos si el usuario concedió todos los permisos solicitados
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // El usuario aceptó. ¡Ahora sí tienes los permisos!
                showToast("Permisos de Bluetooth concedidos.")
                // Aquí es un buen lugar para iniciar automáticamente el escaneo si quieres
                // Ejemplo: iniciarEscaneoBluetooth()
            } else {
                // El usuario rechazó los permisos.
                showToast("Los permisos de Bluetooth son necesarios para conectar dispositivos.")
                // Puedes deshabilitar los botones o mostrar un mensaje más persistente.
            }
        }
    }


    private fun setupButtons() {
        // Botón Conectar
        binding.connectButton.setOnClickListener {
            showToast("Buscando dispositivos EcoRiego...")
            binding.statusTextView.text = "Conectado"
            binding.statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.regarButton.isEnabled = true
        }

        // Botón Regar
        binding.regarButton.setOnClickListener {
            showToast("¡Regando el suelo!")
            binding.statusTextView.text = "Regando..."
            Handler(Looper.getMainLooper()).postDelayed({
                binding.statusTextView.text = "Conectado"
                // Después de regar, volvemos a cargar el gráfico (en un futuro)
                // loadChartData()
            }, 3000)
        }
    }

    // --- Configuración del Gráfico (Estilo) ---
    private fun setupChart() {
        val chart = binding.humidityChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)

        // Eje X (Tiempo)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.BLACK
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        // Formateador para mostrar la hora
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Asumimos que 'value' es la hora (ej. 10.0f -> "10:00")
                return "${value.toInt()}:00"
            }
        }

        // Eje Y (Izquierdo - Humedad)
        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.BLACK
        yAxisLeft.axisMaximum = 100f // %
        yAxisLeft.axisMinimum = 0f   // %
        yAxisLeft.setDrawGridLines(true)

        // Eje Y (Derecho - Deshabilitado)
        chart.axisRight.isEnabled = false
    }

    // --- Cargar Datos de la API y Dibujar ---
    private fun loadChartData() {
        lifecycleScope.launch {
            try {
                val response = apiService.getHistorialDemo()
                if (response.isSuccessful && response.body() != null) {
                    val historial = response.body()!!
                    // Convertir los datos de la API en "Entradas" para el gráfico
                    val entries = ArrayList<Entry>()

                    historial.forEach { data ->
                        // Convertir el String "timestamp" a un objeto LocalDateTime
                        val timestamp = LocalDateTime.parse(data.timestamp)
                        // Usamos la HORA como el valor X
                        // y la HUMEDAD como el valor Y
                        entries.add(Entry(timestamp.hour.toFloat(), data.humedad.toFloat()))
                    }

                    // Actualizar el gráfico
                    updateChart(entries)

                } else {
                    showToast("Error al cargar el historial.")
                }
            } catch (e: IOException) {
                showToast("Error de red. ¿Tu API está encendida?")
                Log.e("MainActivity", "Error de red", e)
            }
        }
    }

    // --- Dibujar los datos en el Gráfico ---
    private fun updateChart(entries: List<Entry>) {
        if (entries.isEmpty()) {
            binding.humidityChart.clear()
            binding.humidityChart.invalidate() // Refrescar gráfico vacío
            return
        }

        val dataSet = LineDataSet(entries, "Humedad")
        dataSet.color = ContextCompat.getColor(this, R.color.blue_water_ecoriego)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.green_dark_ecoriego)
        dataSet.setCircleColor(R.color.blue_water_ecoriego)
        dataSet.setCircleHoleColor(R.color.blue_water_ecoriego)
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Curva suave
        dataSet.setDrawFilled(true) // Relleno debajo de la línea
        dataSet.fillDrawable = ContextCompat.getDrawable(this, R.drawable.chart_fill)


        val lineData = LineData(dataSet)
        binding.humidityChart.data = lineData
        binding.humidityChart.invalidate() // Refrescar el gráfico
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // --- Archivo de Relleno del Gráfico ---
    // ¡Necesitas crear este archivo!
    // Ruta: app/res/drawable/chart_fill.xml
}