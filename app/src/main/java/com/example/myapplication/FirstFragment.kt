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

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), DataClient.OnDataChangedListener {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var lastReading: SensorReading? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Wearable.getDataClient(requireActivity()).addListener(this)

        binding.buttonTestMongo.setOnClickListener {
            val readingToSave = lastReading
            if (readingToSave == null) {
                Toast.makeText(requireContext(), "No hay datos del reloj para guardar aún", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                binding.buttonTestMongo.isEnabled = false
                binding.buttonTestMongo.text = "GUARDANDO..."
                try {
                    withContext(Dispatchers.IO) {
                        MongoDBManager.getInstance().saveReading(readingToSave)
                    }
                    Toast.makeText(requireContext(), "¡Guardado en Atlas exitosamente!", Toast.LENGTH_SHORT).show()
                } catch (e: Throwable) {
                    Log.e("FirstFragment", "Error Atlas: ${e.message}")
                    Toast.makeText(requireContext(), "Error al conectar con Atlas", Toast.LENGTH_LONG).show()
                } finally {
                    binding.buttonTestMongo.isEnabled = true
                    binding.buttonTestMongo.text = getString(R.string.test_atlas)
                }
            }
        }

        val database = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            database.sensorDao().getAllReadings().collect { readings ->
                val text = readings.take(20).joinToString("\n") { 
                    "${it.sensorName}: [${it.valueX}, ${it.valueY}, ${it.valueZ}] (${it.timestamp})"
                }
                binding.textviewReadings.text = if (text.isEmpty()) getString(R.string.no_readings) else text
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/sensor_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val sensorName = dataMap.getString("sensor_name") ?: "Sensor"
                val x = dataMap.getFloat("value_x")
                val y = dataMap.getFloat("value_y")
                val z = dataMap.getFloat("value_z")
                val time = dataMap.getLong("timestamp")
                
                lastReading = SensorReading(sensorName = sensorName, valueX = x, valueY = y, valueZ = z, timestamp = time)
                
                activity?.runOnUiThread {
                    binding.textviewReadings.text = "RECIBIDO: $sensorName\nX: $x, Y: $y, Z: $z\n(Listo para guardar)"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Wearable.getDataClient(requireActivity()).removeListener(this)
        _binding = null
    }
}