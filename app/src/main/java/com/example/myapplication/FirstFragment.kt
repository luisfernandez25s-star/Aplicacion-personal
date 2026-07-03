package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentFirstBinding
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.MongoDBManager
import com.example.myapplication.data.SensorReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.wearable.DataClient

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), DataClient.OnDataChangedListener {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var lastReading: SensorReading? = null

    override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
        Log.d("FirstFragment", "onDataChanged local disparado: ${dataEvents.count}")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sensor_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val hr = dataMap.getFloat("hr")
                
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.titleText.text = "Reloj: HR $hr recibida"
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(requireActivity()).addListener(this)
        } catch (e: Exception) {
            Log.e("FirstFragment", "Error al añadir listener: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(requireActivity()).removeListener(this)
        } catch (e: Exception) {
            Log.e("FirstFragment", "Error al quitar listener: ${e.message}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Verificar Google Play Services
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())
        if (availability != ConnectionResult.SUCCESS) {
            binding.titleText.text = "Google Play Services: ERROR ($availability)"
            binding.titleText.setTextColor(android.graphics.Color.RED)
        }
        
        binding.textviewReadings.text = "Buscando datos en la base de datos..."

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.buttonSimulate.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val simReading = SensorReading(
                    sensorName = "SIMULADO",
                    valueX = (60..100).random().toFloat(),
                    valueY = 0f,
                    valueZ = 0f,
                    timestamp = System.currentTimeMillis()
                )
                AppDatabase.getDatabase(requireContext().applicationContext).sensorDao().insert(simReading)
                Toast.makeText(requireContext(), "Dato simulado guardado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonClearDb.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext().applicationContext).sensorDao().deleteAll()
                Toast.makeText(requireContext(), "Vista limpiada", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonTestMongo.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    binding.buttonTestMongo.isEnabled = false
                    binding.buttonTestMongo.text = "ENVIANDO..."
                    
                    val db = AppDatabase.getDatabase(requireContext().applicationContext)
                    // Obtenemos solo las lecturas NO sincronizadas
                    val readings = withContext(Dispatchers.IO) {
                        db.sensorDao().getUnsyncedReadings()
                    }

                    if (readings.isEmpty()) {
                        Toast.makeText(requireContext(), "Todos los datos ya están en Atlas.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("Atlas", "Iniciando envío masivo de ${readings.size} registros...")
                        
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val manager = MongoDBManager.getInstance()
                                val isOk = manager.saveReadingsBulk(readings)
                                if (isOk) {
                                    readings.forEach { db.sensorDao().markAsSynced(it.id) }
                                    "OK"
                                } else {
                                    "Error desconocido"
                                }
                            } catch (e: Exception) {
                                Log.e("Atlas", "Excepción: ${e.message}")
                                e.localizedMessage ?: "Error de red"
                            }
                        }
                        
                        if (result == "OK") {
                            Toast.makeText(requireContext(), "✅ ¡Sincronizado con Atlas!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "❌ Error: $result", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("Atlas", "Error: ${t.message}")
                    Toast.makeText(requireContext(), "Error al conectar con Atlas", Toast.LENGTH_LONG).show()
                } finally {
                    binding.buttonTestMongo.isEnabled = true
                    binding.buttonTestMongo.text = "GUARDAR EN MONGO ATLAS"
                }
            }
        }

        // Observar la base de datos para mostrar los datos agrupados por sensor
        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            database.sensorDao().getAllReadings().collect { readings ->
                if (readings.isNotEmpty()) {
                    // Agrupamos por nombre de sensor para que se vea ordenado
                    val grouped = readings.groupBy { it.sensorName }
                    
                    val summaryText = StringBuilder()
                    grouped.forEach { (name, sensorReadings) ->
                        val last = sensorReadings.first()
                        val date = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(last.timestamp))
                        
                        summaryText.append("🔴 $name (Último: $date)\n")
                        if (name == "Ritmo Cardiaco" || name == "SIMULADO") {
                            summaryText.append("   VALOR: ${last.valueX} BPM\n")
                        } else {
                            summaryText.append("   X: ${last.valueX}, Y: ${last.valueY}, Z: ${last.valueZ}\n")
                        }
                        summaryText.append("----------------------------\n")
                    }
                    
                    binding.textviewReadings.text = summaryText.toString()
                } else {
                    binding.textviewReadings.text = getString(R.string.no_readings)
                }
            }
        }
        
        // Verificar conexión con el reloj periódicamente (cada 3 segundos)
        startConnectionCheckLoop()
    }

    private fun startConnectionCheckLoop() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                try {
                    val nodes = withContext(Dispatchers.IO) {
                        com.google.android.gms.tasks.Tasks.await(Wearable.getNodeClient(requireActivity()).connectedNodes)
                    }
                    if (nodes.isEmpty()) {
                        binding.titleText.text = "Estado: Reloj Desconectado ❌"
                        binding.titleText.setTextColor(android.graphics.Color.RED)
                    } else {
                        binding.titleText.text = "Estado: Reloj Conectado ✅"
                        binding.titleText.setTextColor(android.graphics.Color.parseColor("#006400")) // Verde oscuro
                    }
                } catch (e: Exception) {
                    Log.e("FirstFragment", "Error al verificar nodos", e)
                }
                kotlinx.coroutines.delay(3000) // Esperar 3 segundos
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}