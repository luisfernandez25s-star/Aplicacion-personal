package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.local.SensorDao
import com.example.myapplication.databinding.FragmentSecondBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var sensorDao: SensorDao

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

        viewLifecycleOwner.lifecycleScope.launch {
            sensorDao.getAll().collect { entities ->
                val history = StringBuilder("Cola de Sincronización Local:\n\n")
                entities.take(50).forEach { entity ->
                    val date = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(entity.timestamp))
                    history.append("⏳ [$date]\n")
                    history.append("   HR: ${entity.heartRate} BPM\n")
                    history.append("   Accel: ${entity.accelX}, ${entity.accelY}, ${entity.accelZ}\n")
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
