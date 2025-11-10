package com.example.riegosostenible

// =======================================================
// ¡LOS IMPORTS!
// =======================================================
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.riegosostenible.databinding.ActivityMainBinding
import com.example.riegosostenible.network.ApiClient
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID
// =======================================================
// FIN DE LOS IMPORTS
// =======================================================


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val apiService = ApiClient.apiService

    // --- Variables de Bluetooth ---
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private var isConnected = false
    private var deviceAddress: String? = null

    // UUIDs (¡Deben coincidir EXACTAMENTE con tu ecoriego_esp32.ino!)
    companion object {
        private const val DEVICE_NAME = "EcoRiego_ESP32"
        private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        private val HUMEDAD_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        private val RIEGO_CHARACTERISTIC_UUID = UUID.fromString("c8209462-2561-42a1-a316-36f61091f04b")
        // Descriptor estándar para habilitar notificaciones
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    // Referencias a las Características (se obtienen al conectar)
    private var humedadCharacteristic: BluetoothGattCharacteristic? = null
    private var riegoCharacteristic: BluetoothGattCharacteristic? = null

    private val mainHandler = Handler(Looper.getMainLooper())


    // --- Lanzadores de Permisos y Bluetooth ---

    // Lanzador para encender el Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            showToast("Bluetooth encendido. ¡Listo para escanear!")
        } else {
            showToast("Bluetooth no fue encendido. No se puede conectar.")
        }
    }

    // Lanzador para pedir múltiples permisos de BLE
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            // Todos los permisos concedidos
            checkBluetoothEnabled()
        } else {
            // Al menos un permiso fue denegado
            showToast("Se necesitan permisos de Bluetooth para escanear.")
        }
    }


    // --- ON CREATE (Función Principal) ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            showToast("Este dispositivo no soporta Bluetooth")
            finish()
            return
        }

        // Configuración de Botones
        setupButtons()

        // Configuración Inicial del Gráfico (llama a la API)
        setupChart()
        loadChartData() // Esto sigue llamando a tu API para el historial
    }

    private fun setupButtons() {
        // Botón Conectar (¡AHORA ES REAL!)
        binding.connectButton.setOnClickListener {
            handleConnection()
        }

        // Botón Regar (¡AHORA ES REAL!)
        binding.regarButton.setOnClickListener {
            sendWaterCommand()
        }
    }

    // --- LÓGICA DE CONEXIÓN BLE ---

    private fun handleConnection() {
        if (isConnected) {
            // Si ya está conectado, desconectar
            disconnect()
        } else {
            // Si no está conectado, iniciar escaneo (que primero pide permisos)
            checkPermissions()
        }
    }

    // 1. Verificar Permisos (Android 12+)
    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val missingPermissions = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            // Todos los permisos están bien, verificar si el Bluetooth está encendido
            checkBluetoothEnabled()
        } else {
            // Pedir los permisos que faltan
            requestMultiplePermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // 2. Verificar si el Bluetooth está Encendido
    private fun checkBluetoothEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            // ¡Listo para escanear!
            startScan()
        }
    }

    // 3. Escanear dispositivos
    private fun startScan() {
        if (!bluetoothAdapter?.isEnabled!!) return
        if (isScanning) {
            stopScan()
            return
        }

        // Empezar a escanear
        isScanning = true
        updateUiState("Escaneando...", false)
        bluetoothLeScanner?.startScan(null, ScanSettings.Builder().build(), scanCallback)

        // Detener el escaneo después de 10 segundos
        mainHandler.postDelayed({ stopScan() }, 10000)
    }

    private fun stopScan() {
        if (isScanning) {
            isScanning = false
            bluetoothLeScanner?.stopScan(scanCallback)
            if (!isConnected) {
                updateUiState("Escaneo detenido.", false)
            }
        }
    }

    // 4. Callback de Escaneo (Cuando se encuentra un dispositivo)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // ¡¡IMPORTANTE!! Verificamos que el nombre coincida
            if (device.name == DEVICE_NAME) {
                Log.i("BLE", "¡Dispositivo EcoRiego encontrado! Conectando...")
                stopScan() // Detener el escaneo
                connectToDevice(device) // Conectar
            }
        }
    }

    // 5. Conectar al dispositivo
    private fun connectToDevice(device: BluetoothDevice) {
        updateUiState("Conectando...", false)
        deviceAddress = device.address
        // Conectar al GATT del dispositivo
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    // 6. Desconectar
    private fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    // 7. Callback del GATT (¡El cerebro de la conexión!)
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    isConnected = true
                    Log.i("BLE", "Conectado al GATT.")
                    // Descubrir los servicios que ofrece el ESP32
                    mainHandler.post { gatt.discoverServices() }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    isConnected = false
                    Log.i("BLE", "Desconectado del GATT.")
                    updateUiState("Desconectado", false)
                    bluetoothGatt?.close()
                }
            }
        }

        // 8. Servicios Descubiertos
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e("BLE", "Servicio EcoRiego no encontrado")
                    disconnect()
                    return
                }

                // Guardar las referencias a las características
                humedadCharacteristic = service.getCharacteristic(HUMEDAD_CHARACTERISTIC_UUID)
                riegoCharacteristic = service.getCharacteristic(RIEGO_CHARACTERISTIC_UUID)

                if (humedadCharacteristic == null || riegoCharacteristic == null) {
                    Log.e("BLE", "Características no encontradas")
                    disconnect()
                    return
                }

                Log.i("BLE", "¡Servicio y características encontradas!")

                // ¡Suscribirse a las notificaciones de humedad!
                subscribeToNotifications(gatt)
            }
        }

        // 9. Suscribirse a Notificaciones (para recibir la humedad)
        private fun subscribeToNotifications(gatt: BluetoothGatt) {
            gatt.setCharacteristicNotification(humedadCharacteristic, true)
            val descriptor = humedadCharacteristic?.getDescriptor(CCCD_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)

            // Una vez suscritos, actualizamos la UI
            updateUiState("Conectado", true)
        }

        // 10. ¡¡DATOS RECIBIDOS!! (Cuando la humedad cambia)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == HUMEDAD_CHARACTERISTIC_UUID) {
                // El ESP32 envía los datos como un String (ej: "85")
                val humedadString = characteristic.getStringValue(0)
                Log.i("BLE", "Humedad recibida: $humedadString%")

                // Actualizar la UI (¡en el hilo principal!)
                mainHandler.post {
                    binding.humidityTextView.text = "$humedadString%"
                }
            }
        }

        // 11. (Callback de cuando se escribió el comando de riego)
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == RIEGO_CHARACTERISTIC_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BLE", "Comando de riego enviado OK")
                } else {
                    Log.w("BLE", "Fallo al enviar comando de riego")
                }
            }
        }
    }

    // 12. Enviar Comando de Riego
    private fun sendWaterCommand() {
        if (bluetoothGatt == null || riegoCharacteristic == null || !isConnected) {
            showToast("No estás conectado al dispositivo EcoRiego")
            return
        }

        // Enviar "1" para regar
        val command = "1".toByteArray()
        riegoCharacteristic?.value = command
        riegoCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        bluetoothGatt?.writeCharacteristic(riegoCharacteristic)

        showToast("¡Regando el suelo!")
        updateUiState("Regando...", true)

        // Simular que el riego dura 3 segundos
        mainHandler.postDelayed({
            // Enviar "0" para apagar la bomba
            val stopCommand = "0".toByteArray()
            riegoCharacteristic?.value = stopCommand
            riegoCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(riegoCharacteristic)

            updateUiState("Conectado", true)
        }, 3000)
    }


    // --- Funciones de UI y Gráfico (¡EXISTENTES!) ---

    private fun updateUiState(status: String, isBleConnected: Boolean) {
        // Ejecutar en el hilo de UI
        mainHandler.post {
            binding.statusTextView.text = status
            binding.regarButton.isEnabled = isBleConnected

            if (isBleConnected) {
                binding.statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                binding.connectButton.text = "Desconectar"
            } else {
                binding.statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                binding.connectButton.text = "Conectar a EcoRiego"
                binding.humidityTextView.text = "--%"
            }
        }
    }

    private fun setupChart() {
        val chart = binding.humidityChart
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.BLACK
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}:00"
            }
        }
        val yAxisLeft = chart.axisLeft
        yAxisLeft.textColor = Color.BLACK
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.setDrawGridLines(true)
        chart.axisRight.isEnabled = false
    }

    private fun loadChartData() {
        lifecycleScope.launch {
            try {
                val response = apiService.getHistorialDemo()
                if (response.isSuccessful && response.body() != null) {
                    val historial = response.body()!!
                    val entries = ArrayList<Entry>()
                    historial.forEach { data ->
                        val timestamp = LocalDateTime.parse(data.timestamp)
                        entries.add(Entry(timestamp.hour.toFloat(), data.humedad.toFloat()))
                    }
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

    private fun updateChart(entries: List<Entry>) {
        if (entries.isEmpty()) {
            binding.humidityChart.clear()
            binding.humidityChart.invalidate()
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
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = ContextCompat.getDrawable(this, R.drawable.chart_fill)
        val lineData = LineData(dataSet)
        binding.humidityChart.data = lineData
        binding.humidityChart.invalidate()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Al destruir la app, nos desconectamos
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}