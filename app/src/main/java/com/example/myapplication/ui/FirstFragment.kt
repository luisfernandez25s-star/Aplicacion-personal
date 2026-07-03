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
                viewModel.importMongoUriFromFile(requireContext(), uri)
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
            Toast.makeText(requireContext(), "Sincronizando con MongoDB Atlas...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImportOptions() {
        val options = arrayOf("Importar archivo .env / .json", "Ingresar URI manualmente")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Configurar MongoDB Atlas")
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
            hint = "mongodb+srv://user:pass@cluster..."
            setPadding(40, 40, 40, 40)
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Ingresar MongoDB URI")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val uri = editText.text.toString().trim()
                if (uri.isNotEmpty()) {
                    viewModel.updateMongoUri(uri)
                    Toast.makeText(requireContext(), "URI guardada", Toast.LENGTH_SHORT).show()
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
                        Timber.d("DEBUG_UI: Received data flow update: $data")
                        data?.let {
                            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))
                            binding.tvRealtimeData.text = """
                                ❤️ FC: ${it.heartRate.toInt()} BPM
                                📍 Accel: X=${String.format(Locale.getDefault(), "%.2f", it.accelerometer.x)}, Y=${String.format(Locale.getDefault(), "%.2f", it.accelerometer.y)}, Z=${String.format(Locale.getDefault(), "%.2f", it.accelerometer.z)}
                                🔄 Gyro: X=${String.format(Locale.getDefault(), "%.2f", it.gyroscope.x)}, Y=${String.format(Locale.getDefault(), "%.2f", it.gyroscope.y)}, Z=${String.format(Locale.getDefault(), "%.2f", it.gyroscope.z)}
                                🕒 Recibido: $time
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
                        binding.tvConnectionStatus.text = "MongoDB: ${status.name}"
                        when (status) {
                            MongoRepository.ConnectionStatus.CONNECTED -> {
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.GREEN)
                                binding.buttonTestMongo.isEnabled = true
                            }
                            MongoRepository.ConnectionStatus.ERROR -> {
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.RED)
                                binding.buttonTestMongo.isEnabled = true
                            }
                            MongoRepository.ConnectionStatus.CONNECTING -> {
                                binding.tvConnectionStatus.setTextColor(android.graphics.Color.YELLOW)
                                binding.buttonTestMongo.isEnabled = false
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
