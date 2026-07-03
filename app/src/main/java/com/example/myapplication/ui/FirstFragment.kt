package com.example.myapplication.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.repository.MongoRepository
import com.example.myapplication.databinding.FragmentFirstBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import timber.log.Timber

import android.widget.Toast

@AndroidEntryPoint
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.importConfigFromFile(requireContext(), uri)
                Toast.makeText(requireContext(), "Configuración importada", Toast.LENGTH_SHORT).show()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.btnImportUri.setOnClickListener {
            showImportOptions()
        }

        binding.buttonTestMongo.setOnClickListener {
            viewModel.forceSync()
            Toast.makeText(requireContext(), "Sincronizando con el servidor en Render...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportOptions() {
        val options = arrayOf("Importar archivo .env / .json", "Ingresar URL de API manualmente")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Configurar API Render")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        filePickerLauncher.launch(intent)
                    }
                    1 -> showManualUriDialog()
                }
            }
            .show()
    }

    private fun showManualUriDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            hint = "https://tu-app.onrender.com/"
            setPadding(40, 40, 40, 40)
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Ingresar URL de API")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val uri = editText.text.toString().trim()
                if (uri.isNotEmpty()) {
                    viewModel.updateApiUri(uri)
                    Toast.makeText(requireContext(), "URL guardada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.latestSensorData.collectLatest { data ->
                        if (data != null) {
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))
                            binding.tvRealtimeData.text = """
                                ❤️ ${data.heartRate.toInt()} BPM
                                📍 XYZ: ${String.format(Locale.getDefault(), "%.1f, %.1f, %.1f", data.accelerometer.x, data.accelerometer.y, data.accelerometer.z)}
                                🔄 G: ${String.format(Locale.getDefault(), "%.1f, %.1f, %.1f", data.gyroscope.x, data.gyroscope.y, data.gyroscope.z)}
                                🕒 $time
                            """.trimIndent()
                        } else {
                            binding.tvRealtimeData.text = """
                                ❤️ -- BPM
                                📍 XYZ: 0.0, 0.0, 0.0
                                🔄 G: 0.0, 0.0, 0.0
                                🕒 --:--:--
                            """.trimIndent()
                        }
                    }
                }

                launch {
                    viewModel.connectionError.collectLatest { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                launch {
                    viewModel.connectionStatus.collectLatest { status ->
                        binding.tvConnectionStatus.text = "API Status: ${status.name}"
                        when (status) {
                            MongoRepository.ConnectionStatus.CONNECTED -> {
                                binding.tvConnectionStatus.text = "API Status: ONLINE"
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.GREEN)
                                binding.buttonTestMongo.isEnabled = true
                                // Si veníamos de un estado de carga, mostrar éxito
                                if (binding.buttonTestMongo.text == "SINCRONIZANDO...") {
                                    Toast.makeText(requireContext(), "✅ Datos subidos a MongoDB Atlas", Toast.LENGTH_LONG).show()
                                }
                                binding.buttonTestMongo.text = "SUBIR A MONGO ATLAS"
                            }
                            MongoRepository.ConnectionStatus.ERROR -> {
                                binding.tvConnectionStatus.text = "API Status: ERROR"
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.RED)
                                binding.buttonTestMongo.isEnabled = true
                                binding.buttonTestMongo.text = "REINTENTAR SUBIDA"
                            }
                            MongoRepository.ConnectionStatus.CONNECTING -> {
                                binding.tvConnectionStatus.text = "API Status: ENVIANDO..."
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.YELLOW)
                                binding.buttonTestMongo.isEnabled = false
                                binding.buttonTestMongo.text = "SINCRONIZANDO..."
                            }
                            else -> {
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.WHITE)
                                binding.buttonTestMongo.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
