package com.example.foodexpiryapp.presentation.ui.vision

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.remote.ProviderConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioServerConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioVisionClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaServerConfig
import com.example.foodexpiryapp.data.remote.ollama.OllamaVisionClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OllamaSettingsDialog : DialogFragment() {

    @Inject lateinit var ollamaServerConfig: OllamaServerConfig
    @Inject lateinit var ollamaClient: OllamaVisionClient
    @Inject lateinit var lmStudioServerConfig: LmStudioServerConfig
    @Inject lateinit var lmStudioClient: LmStudioVisionClient
    @Inject lateinit var providerConfig: ProviderConfig

    private lateinit var editTextUrl: TextInputEditText
    private lateinit var editTextModel: TextInputEditText
    private lateinit var editTextToken: TextInputEditText
    private lateinit var layoutApiToken: TextInputLayout
    private lateinit var textHint: android.widget.TextView
    private lateinit var btnOllama: MaterialButton
    private lateinit var btnLmStudio: MaterialButton
    private lateinit var toggleGroup: com.google.android.material.button.MaterialButtonToggleGroup

    private var currentProvider = ProviderConfig.PROVIDER_OLLAMA

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_ollama_settings, null)

        editTextUrl = view.findViewById(R.id.editTextServerUrl)
        editTextModel = view.findViewById(R.id.editTextModelName)
        editTextToken = view.findViewById(R.id.editTextApiToken)
        layoutApiToken = view.findViewById(R.id.layoutApiToken)
        textHint = view.findViewById(R.id.textHint)
        btnOllama = view.findViewById(R.id.btnOllama)
        btnLmStudio = view.findViewById(R.id.btnLmStudio)
        toggleGroup = view.findViewById(R.id.toggleGroupProvider)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentProvider = if (checkedId == R.id.btnLmStudio) {
                    ProviderConfig.PROVIDER_LMSTUDIO
                } else {
                    ProviderConfig.PROVIDER_OLLAMA
                }
                loadProviderSettings()
            }
        }

        lifecycleScope.launch {
            currentProvider = providerConfig.getProvider()
            if (currentProvider == ProviderConfig.PROVIDER_LMSTUDIO) {
                toggleGroup.check(R.id.btnLmStudio)
            } else {
                toggleGroup.check(R.id.btnOllama)
            }
            loadProviderSettings()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("推理服務器設置")
            .setView(view)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .setNeutralButton("測試連接", null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog ?: return
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            saveSettingsAndDismiss(dialog)
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            testConnection()
        }
    }

    private fun loadProviderSettings() {
        lifecycleScope.launch {
            if (currentProvider == ProviderConfig.PROVIDER_LMSTUDIO) {
                val config = lmStudioServerConfig.getConfig()
                editTextUrl.setText(config.baseUrl)
                editTextModel.setText(config.modelName)
                editTextToken.setText("")
                layoutApiToken.visibility = View.GONE
                textHint.text = "LM Studio 預設端口: http://localhost:1234"
            } else {
                val config = ollamaServerConfig.getConfig()
                editTextUrl.setText(config.baseUrl)
                editTextModel.setText(config.modelName)
                config.apiToken?.let { editTextToken.setText(it) }
                layoutApiToken.visibility = View.VISIBLE
                textHint.text = "例如：http://192.168.1.100:11434 或你的 Cloudflare Tunnel 域名"
            }
        }
    }

    private fun saveSettingsAndDismiss(dialog: AlertDialog) {
        val url = editTextUrl.text?.toString()?.trim()
        val model = editTextModel.text?.toString()?.trim()
        val token = editTextToken.text?.toString()?.trim()

        if (url.isNullOrBlank()) {
            Toast.makeText(context, "請輸入服務器 URL", Toast.LENGTH_SHORT).show()
            return
        }
        if (model.isNullOrBlank()) {
            Toast.makeText(context, "請輸入模型名稱", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                providerConfig.setProvider(currentProvider)
                if (currentProvider == ProviderConfig.PROVIDER_LMSTUDIO) {
                    lmStudioServerConfig.setBaseUrl(url)
                    lmStudioServerConfig.setModelName(model)
                } else {
                    ollamaServerConfig.setBaseUrl(url)
                    ollamaServerConfig.setModelName(model)
                    ollamaServerConfig.setApiToken(token?.takeIf { it.isNotBlank() })
                }
                Toast.makeText(context, "設置已保存 (${if (currentProvider == ProviderConfig.PROVIDER_LMSTUDIO) "LM Studio" else "Ollama"})", Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult("settings_saved", Bundle.EMPTY)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "保存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testConnection() {
        val url = editTextUrl.text?.toString()?.trim()
        val model = editTextModel.text?.toString()?.trim()
        val token = editTextToken.text?.toString()?.trim()

        if (url.isNullOrBlank()) {
            Toast.makeText(context, "請輸入服務器 URL", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                if (currentProvider == ProviderConfig.PROVIDER_LMSTUDIO) {
                    lmStudioServerConfig.setBaseUrl(url)
                    lmStudioServerConfig.setModelName(model ?: "qwen3.5-9b")
                    val isConnected = lmStudioClient.testConnection()
                    Toast.makeText(context, if (isConnected) "LM Studio 連接成功!" else "LM Studio 連接失敗", Toast.LENGTH_SHORT).show()
                } else {
                    ollamaServerConfig.setBaseUrl(url)
                    ollamaServerConfig.setModelName(model ?: "qwen3.5:9b")
                    ollamaServerConfig.setApiToken(token?.takeIf { it.isNotBlank() })
                    val isConnected = ollamaClient.testConnection()
                    Toast.makeText(context, if (isConnected) "Ollama 連接成功!" else "Ollama 連接失敗", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "連接失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
