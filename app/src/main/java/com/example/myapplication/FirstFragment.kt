package com.example.myapplication

import android.os.Bundle
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

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.buttonTestMongo.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val reading = SensorReading(
                    sensorName = "Prueba Manual UI",
                    valueX = 0f,
                    valueY = 0f,
                    valueZ = 0f,
                    timestamp = System.currentTimeMillis()
                )
                try {
                    withContext(Dispatchers.IO) {
                        MongoDBManager.getInstance().saveReading(reading)
                    }
                    Toast.makeText(requireContext(), "Comando de envío enviado a Atlas", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}