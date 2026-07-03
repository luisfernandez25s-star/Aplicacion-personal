package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        val db = AppDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.sensorDao().getAllReadings().collect { readings ->
                val history = StringBuilder("Historial de Lecturas (Últimas 50):\n\n")
                readings.take(50).forEach { reading ->
                    val date = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(reading.timestamp))
                    val status = if (reading.isSynced) "✅" else "⏳"
                    history.append("$status [$date] ${reading.sensorName}\n")
                    history.append("   Valores: ${reading.valueX}, ${reading.valueY}, ${reading.valueZ}\n")
                    history.append("----------------------------\n")
                }
                binding.textviewSecond.text = history.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}