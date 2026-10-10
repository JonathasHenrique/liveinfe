package com.igrejalive.app.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
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

    private val prefs by lazy { getSharedPreferences("liveinfe_prefs", Context.MODE_PRIVATE) }
    private val PREF_STREAM_KEY = "saved_stream_key"
    private val PREF_AUTH_MODE = "saved_auth_mode"

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
            val statusCode = (e as? ApiException)?.statusCode
            if (statusCode == 10) {
                showError10Dialog()
            } else {
                Toast.makeText(this, "Falha ao entrar com Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        loadSavedSettings()
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

    private fun loadSavedSettings() {
        // Carrega modo salvo (Padrão: Chave de Transmissão)
        val authMode = prefs.getString(PREF_AUTH_MODE, "stream_key")
        if (authMode == "google") {
            binding.rbModeGoogleAccount.isChecked = true
            binding.layoutStreamKeyMode.visibility = View.GONE
            binding.layoutGoogleMode.visibility = View.VISIBLE
        } else {
            binding.rbModeStreamKey.isChecked = true
            binding.layoutStreamKeyMode.visibility = View.VISIBLE
            binding.layoutGoogleMode.visibility = View.GONE
        }

        // Carrega Chave salva
        val savedKey = prefs.getString(PREF_STREAM_KEY, "") ?: ""
        if (savedKey.isNotEmpty()) {
            binding.etStreamKey.setText(savedKey)
            binding.tvStreamKeyStatus.text = "✅ Chave salva e configurada! Pronto para transmitir."
            binding.tvStreamKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.tuned_green))
        }
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

        // Alternância do Modo de Conexão (Chave x Google)
        binding.rgAuthMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbModeStreamKey) {
                binding.layoutStreamKeyMode.visibility = View.VISIBLE
                binding.layoutGoogleMode.visibility = View.GONE
                prefs.edit().putString(PREF_AUTH_MODE, "stream_key").apply()
            } else {
                binding.layoutStreamKeyMode.visibility = View.GONE
                binding.layoutGoogleMode.visibility = View.VISIBLE
                prefs.edit().putString(PREF_AUTH_MODE, "google").apply()
            }
        }

        // Salva a chave automaticamente ao digitar
        binding.etStreamKey.doAfterTextChanged { text ->
            val key = text?.toString()?.trim() ?: ""
            prefs.edit().putString(PREF_STREAM_KEY, key).apply()
            if (key.isNotEmpty()) {
                binding.tvStreamKeyStatus.text = "✅ Chave salva e configurada! Pronto para transmitir."
                binding.tvStreamKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.tuned_green))
            } else {
                binding.tvStreamKeyStatus.text = "💡 Copie do YouTube Studio > Fazer Transmissão > Chave da Transmissão"
                binding.tvStreamKeyStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
            }
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

        // Botão Principal: ENTRAR AO VIVO NO YOUTUBE
        binding.btnStartLive.setOnClickListener {
            if (binding.rbModeStreamKey.isChecked) {
                startStreamWithKey()
            } else {
                startLiveWithGoogleFlow()
            }
        }

        // Modo manual RTMP (Fallback dialog)
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

    // Transmissão Direta com Chave (Fluxo Recomendado para Igrejas)
    private fun startStreamWithKey() {
        val key = binding.etStreamKey.text?.toString()?.trim() ?: ""
        if (key.isEmpty()) {
            binding.etStreamKey.error = "Cole a chave de transmissão do YouTube Studio"
            binding.etStreamKey.requestFocus()
            Toast.makeText(this, "Cole a chave de transmissão da igreja antes de iniciar", Toast.LENGTH_LONG).show()
            return
        }

        val fullTarget = if (key.startsWith("rtmp://") || key.startsWith("rtmps://")) {
            key
        } else {
            "rtmp://a.rtmp.youtube.com/live2/$key"
        }

        val is1080p = binding.rb1080p.isChecked
        val title = binding.etLiveTitle.text?.toString()?.trim() ?: "Culto ao Vivo"

        val intent = Intent(this, LiveStreamActivity::class.java).apply {
            putExtra(LiveStreamActivity.EXTRA_RTMP_URL, fullTarget)
            putExtra(LiveStreamActivity.EXTRA_IS_1080P, is1080p)
            putExtra(LiveStreamActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
    }

    // Transmissão via Conta Google OAuth (Criação Automática de Live na API)
    private fun startLiveWithGoogleFlow() {
        val account = authManager.getLastSignedInAccount()
        if (account == null) {
            Toast.makeText(this, "Por favor, conecte a conta do YouTube da igreja primeiro", Toast.LENGTH_LONG).show()
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

            val liveResult = youtubeApi.createAndBindLive(
                accessToken = token,
                title = title,
                description = "Culto ao vivo transmitido diretamente pelo app LiveinFé.",
                privacy = privacy,
                resolution = resolution
            )

            progress.dismiss()

            liveResult.onSuccess { result ->
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
                    .setMessage("O YouTube respondeu com erro:\n\n${err.message}\n\nVerifique se o canal da igreja está habilitado para transmissões ao vivo.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showError10Dialog() {
        AlertDialog.Builder(this)
            .setTitle("Configuração do Google Cloud Necessária")
            .setMessage(
                "O Google retornou o Erro 10 (DEVELOPER_ERROR).\n\n" +
                "Isso acontece porque a API do YouTube exige cadastrar o aplicativo no painel Google Cloud Console com a chave de assinatura (SHA-1) da igreja.\n\n" +
                "💡 DICA PRÁTICA: Você não precisa fazer isso para transmitir agora! Basta usar a opção '🔑 Chave de Transmissão', colar a chave da igreja uma única vez e iniciar a transmissão!"
            )
            .setPositiveButton("Usar Chave de Transmissão") { _, _ ->
                binding.rbModeStreamKey.isChecked = true
                binding.layoutStreamKeyMode.visibility = View.VISIBLE
                binding.layoutGoogleMode.visibility = View.GONE
                binding.etStreamKey.requestFocus()
            }
            .setNeutralButton("Ver SHA-1") { _, _ ->
                showSha1Dialog()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showSha1Dialog() {
        val sha1 = "9B:91:A6:1A:F5:23:A3:DC:AB:11:49:A3:2E:3F:2A:ED:71:F3:0F:A4"
        val sha256 = "A2:5B:59:22:42:33:CD:DF:F4:25:A3:7A:C2:DB:49:82:DD:61:B8:89:2D:88:90:2C:C6:61:5D:86:B0:69:92:7F"
        val pkg = "com.igrejalive.app"
        val message = "Pacote:\n$pkg\n\nSHA-1:\n$sha1\n\nSHA-256:\n$sha256"

        AlertDialog.Builder(this)
            .setTitle("Credenciais para Google Cloud Console")
            .setMessage(message)
            .setPositiveButton("Copiar SHA-1") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("SHA1", sha1)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "SHA-1 copiado!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showManualRtmpDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "rtmp://a.rtmp.youtube.com/live2/SUA-CHAVE"
            setText(prefs.getString(PREF_STREAM_KEY, ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Modo RTMP Manual")
            .setMessage("Cole a URL RTMP completa (com a chave):")
            .setView(input)
            .setPositiveButton("Transmitir") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    prefs.edit().putString(PREF_STREAM_KEY, url).apply()
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

