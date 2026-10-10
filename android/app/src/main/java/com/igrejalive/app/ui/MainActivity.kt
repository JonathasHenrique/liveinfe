package com.igrejalive.app.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.igrejalive.app.R
import com.igrejalive.app.auth.GoogleAuthManager
import com.igrejalive.app.data.api.YouTubeApiService
import com.igrejalive.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: GoogleAuthManager
    private val youtubeApi = YouTubeApiService()

    // Permissões de Câmera e Microfone
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (!cameraGranted || !audioGranted) {
            Toast.makeText(this, "Permissões de câmera e microfone são obrigatórias para transmitir", Toast.LENGTH_LONG).show()
        }
    }

    // Retorno do Google Sign-In
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            updateUiWithAccount(account?.displayName, account?.email)
            Toast.makeText(this, "Conectado como: ${account?.displayName}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Falha ao entrar com Google: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = GoogleAuthManager(this)

        checkPermissions()
        setupDefaultLiveTitle()
        setupListeners()
        checkExistingLogin()
    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun setupDefaultLiveTitle() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateToday = sdf.format(Date())
        binding.etLiveTitle.setText("Culto de Celebração - $dateToday")
    }

    private fun setupListeners() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateToday = sdf.format(Date())

        // Preset Chips de Culto
        binding.chipCelebracao.setOnClickListener {
            binding.etLiveTitle.setText("Culto de Celebração • $dateToday")
        }
        binding.chipOracao.setOnClickListener {
            binding.etLiveTitle.setText("Culto de Oração e Intercessão • $dateToday")
        }
        binding.chipJovens.setOnClickListener {
            binding.etLiveTitle.setText("Encontro de Jovens • Ao Vivo")
        }
        binding.chipCeia.setOnClickListener {
            binding.etLiveTitle.setText("Santa Ceia do Senhor • $dateToday")
        }

        // Botão Login Google
        binding.btnLoginGoogle.setOnClickListener {
            val signInIntent = authManager.client.signInIntent
            signInLauncher.launch(signInIntent)
        }

        // Botão Desconectar / Trocar conta
        binding.btnLogout.setOnClickListener {
            authManager.signOut {
                binding.layoutChannelInfo.visibility = View.GONE
                binding.btnLoginGoogle.visibility = View.VISIBLE
            }
        }

        // Botão Principal: INICIAR CULTO AO VIVO
        binding.btnStartLive.setOnClickListener {
            startLiveFlow()
        }

        // Modo manual RTMP (Fallback)
        binding.btnManualRtmp.setOnClickListener {
            showManualRtmpDialog()
        }
    }

    private fun checkExistingLogin() {
        val account = authManager.getLastSignedInAccount()
        if (account != null) {
            updateUiWithAccount(account.displayName, account.email)
        } else {
            binding.layoutChannelInfo.visibility = View.GONE
            binding.btnLoginGoogle.visibility = View.VISIBLE
        }
    }

    private fun updateUiWithAccount(name: String?, email: String?) {
        binding.layoutChannelInfo.visibility = View.VISIBLE
        binding.btnLoginGoogle.visibility = View.GONE
        binding.tvChannelName.text = name ?: "Canal da Igreja"
        binding.tvChannelStatus.text = email ?: "Pronto para transmitir"
    }

    private fun startLiveFlow() {
        val account = authManager.getLastSignedInAccount()
        if (account == null) {
            Toast.makeText(this, "Por favor, entre com a conta do YouTube da igreja primeiro", Toast.LENGTH_LONG).show()
            binding.btnLoginGoogle.performClick()
            return
        }

        val title = binding.etLiveTitle.text?.toString()?.trim()
        if (title.isNullOrEmpty()) {
            binding.etLiveTitle.error = "Digite o título do culto"
            return
        }

        val privacy = if (binding.rbPublic.isChecked) "public" else "unlisted"
        val is1080p = binding.rb1080p.isChecked
        val resolution = if (is1080p) "1080p" else "720p"

        @Suppress("DEPRECATION")
        val progress = ProgressDialog(this).apply {
            setMessage("Criando transmissão no YouTube e configurando 16:9...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            // 1. Obtém o token OAuth do Google
            val tokenResult = authManager.getAccessToken(account)
            if (tokenResult.isFailure) {
                progress.dismiss()
                Toast.makeText(
                    this@MainActivity,
                    "Erro ao obter permissão do YouTube: ${tokenResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val token = tokenResult.getOrThrow()

            // 2. Cria o Broadcast, Stream e vincula automaticamente no YouTube
            val liveResult = youtubeApi.createAndBindLive(
                accessToken = token,
                title = title,
                description = "Culto ao vivo transmitido diretamente pelo app Igreja Live.",
                privacy = privacy,
                resolution = resolution
            )

            progress.dismiss()

            liveResult.onSuccess { result ->
                // Abre a tela de transmissão já com tudo conectado!
                val intent = Intent(this@MainActivity, LiveStreamActivity::class.java).apply {
                    putExtra(LiveStreamActivity.EXTRA_RTMP_URL, result.fullRtmpTarget)
                    putExtra(LiveStreamActivity.EXTRA_BROADCAST_ID, result.broadcastId)
                    putExtra(LiveStreamActivity.EXTRA_WATCH_URL, result.watchUrl)
                    putExtra(LiveStreamActivity.EXTRA_ACCESS_TOKEN, token)
                    putExtra(LiveStreamActivity.EXTRA_IS_1080P, is1080p)
                    putExtra(LiveStreamActivity.EXTRA_TITLE, title)
                }
                startActivity(intent)
            }.onFailure { err ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Erro ao criar live no YouTube")
                    .setMessage("O YouTube respondeu com erro:\n\n${err.message}\n\nVerifique se o canal da sua igreja já está habilitado para transmissões ao vivo no YouTube Studio.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showManualRtmpDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "rtmp://a.rtmp.youtube.com/live2/SUA-CHAVE"
        }
        AlertDialog.Builder(this)
            .setTitle("Modo RTMP Manual")
            .setMessage("Cole a URL RTMP completa (com a chave):")
            .setView(input)
            .setPositiveButton("Transmitir") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    val intent = Intent(this, LiveStreamActivity::class.java).apply {
                        putExtra(LiveStreamActivity.EXTRA_RTMP_URL, url)
                        putExtra(LiveStreamActivity.EXTRA_IS_1080P, binding.rb1080p.isChecked)
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
