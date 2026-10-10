// Lógica do Simulador Web do LiveinFé (Inspirado na Bíblia do Instrumentista)
document.addEventListener('DOMContentLoaded', () => {
  // 1. Navegação de Abas
  const tabBtns = document.querySelectorAll('.nav-tab');
  const tabContents = document.querySelectorAll('.tab-content');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      const targetId = `tab-${btn.dataset.tab}`;
      const target = document.getElementById(targetId);
      if (target) target.classList.add('active');
    });
  });

  // 2. Relógio do Android Mockup
  const androidClock = document.getElementById('android-clock');
  function updateClock() {
    const now = new Date();
    const h = String(now.getHours()).padStart(2, '0');
    const m = String(now.getMinutes()).padStart(2, '0');
    if (androidClock) androidClock.textContent = `${h}:${m}`;
  }
  updateClock();
  setInterval(updateClock, 30000);

  // 3. Preset Chips de Culto
  const chipBtns = document.querySelectorAll('.chip-btn');
  const inputLiveTitle = document.getElementById('input-live-title');

  const today = new Date().toLocaleDateString('pt-BR');
  chipBtns.forEach(chip => {
    chip.addEventListener('click', () => {
      const preset = chip.dataset.preset;
      inputLiveTitle.value = `${preset} • ${today}`;
      // Efeito de feedback
      chip.style.transform = 'scale(0.95)';
      setTimeout(() => { chip.style.transform = ''; }, 150);
    });
  });

  // 4. Estado da Transmissão
  let isGoogleLoggedIn = false;
  let isStreaming = false;
  let streamTimer = null;
  let streamSeconds = 0;
  let currentCameraFacing = 'user'; // 'user' ou 'environment'
  let isMuted = false;
  let isFlashOn = false;
  let mediaStream = null;
  let useRealWebcam = false;

  // Elementos do App
  const screenSetup = document.getElementById('screen-setup');
  const screenLive = document.getElementById('screen-live');
  const authSignedOut = document.getElementById('auth-signed-out');
  const authSignedIn = document.getElementById('auth-signed-in');
  const btnLoginSim = document.getElementById('btn-login-sim');
  const btnSwitchAccount = document.getElementById('btn-switch-account');
  const btnStartStream = document.getElementById('btn-start-stream');
  const btnStopLive = document.getElementById('btn-stop-live');
  const liveTimerEl = document.getElementById('live-timer');
  const liveBitrateEl = document.getElementById('live-bitrate');
  const liveResLabel = document.getElementById('live-res-label');
  const liveVideo = document.getElementById('live-camera-video');
  const simulatedFeed = document.getElementById('simulated-church-feed');
  const btnFlipCam = document.getElementById('btn-flip-cam');
  const btnMuteMic = document.getElementById('btn-mute-mic');
  const btnToggleLight = document.getElementById('btn-toggle-light');
  const btnCopyLink = document.getElementById('btn-copy-link');

  // Elementos do painel de teste
  const btnUseWebcam = document.getElementById('btn-use-webcam');
  const btnUseAltar = document.getElementById('btn-use-altar');
  const inputAudioVolume = document.getElementById('input-audio-volume');
  const selectNetworkHealth = document.getElementById('select-network-health');
  const vuMeterDisplay = document.getElementById('vu-meter-display');
  const vuDbText = document.getElementById('vu-db-text');

  // 5. Montar VU Meter Estilo Afinador (16 segmentos)
  const TOTAL_VU_BARS = 16;
  vuMeterDisplay.innerHTML = '';
  for (let i = 0; i < TOTAL_VU_BARS; i++) {
    const bar = document.createElement('div');
    bar.className = 'vu-bar';
    bar.dataset.index = i;
    vuMeterDisplay.appendChild(bar);
  }

  function updateVuMeter(fraction) {
    if (isMuted) fraction = 0;
    const bars = vuMeterDisplay.querySelectorAll('.vu-bar');
    const activeCount = Math.round(fraction * TOTAL_VU_BARS);

    bars.forEach((bar, idx) => {
      bar.className = 'vu-bar';
      if (idx < activeCount) {
        const ratio = idx / TOTAL_VU_BARS;
        if (ratio > 0.85) bar.classList.add('active-red');
        else if (ratio > 0.65) bar.classList.add('active-yellow');
        else bar.classList.add('active-green');
      }
    });

    const db = Math.round(-36 + fraction * 36);
    vuDbText.textContent = isMuted ? 'MUTE' : `${db > 0 ? '+' : ''}${db}dB`;
  }

  // Animação de Áudio Contínua
  setInterval(() => {
    if (!isStreaming) {
      updateVuMeter(0.2);
      return;
    }
    const baseGain = parseInt(inputAudioVolume.value, 10) / 100;
    const variance = (Math.random() - 0.5) * 0.25;
    const currentFraction = Math.max(0, Math.min(1, baseGain + variance));
    updateVuMeter(currentFraction);
  }, 100);

  // 6. Login Simulado
  btnLoginSim.addEventListener('click', () => {
    isGoogleLoggedIn = true;
    authSignedOut.style.display = 'none';
    authSignedIn.style.display = 'flex';
  });

  btnSwitchAccount.addEventListener('click', () => {
    isGoogleLoggedIn = false;
    authSignedIn.style.display = 'none';
    authSignedOut.style.display = 'block';
  });

  // 7. Iniciar Live no YouTube
  btnStartStream.addEventListener('click', async () => {
    if (!isGoogleLoggedIn) {
      isGoogleLoggedIn = true;
      authSignedOut.style.display = 'none';
      authSignedIn.style.display = 'flex';
    }

    const resPill = document.querySelector('input[name="sim-res"]:checked').value;
    liveResLabel.textContent = resPill === '1080p' ? '1080p Full HD • 16:9 Nativo' : '720p HD • 16:9 Nativo';

    screenSetup.classList.remove('active');
    screenLive.classList.add('active');
    isStreaming = true;
    streamSeconds = 0;

    startTimer();
    updateBitrateDisplay();

    if (useRealWebcam) {
      await startCameraStream();
    }
  });

  // 8. Encerrar Live
  btnStopLive.addEventListener('click', () => {
    const confirmStop = confirm('Deseja realmente encerrar a transmissão ao vivo do culto no YouTube?');
    if (confirmStop) {
      isStreaming = false;
      stopTimer();
      stopCameraStream();
      screenLive.classList.remove('active');
      screenSetup.classList.add('active');
    }
  });

  // 9. Cronômetro da Live
  function startTimer() {
    stopTimer();
    streamTimer = setInterval(() => {
      streamSeconds++;
      const h = String(Math.floor(streamSeconds / 3600)).padStart(2, '0');
      const m = String(Math.floor((streamSeconds % 3600) / 60)).padStart(2, '0');
      const s = String(streamSeconds % 60).padStart(2, '0');
      liveTimerEl.textContent = `${h}:${m}:${s}`;
    }, 1000);
  }

  function stopTimer() {
    if (streamTimer) clearInterval(streamTimer);
    streamTimer = null;
  }

  // 10. Troca de Câmera
  btnFlipCam.addEventListener('click', async () => {
    currentCameraFacing = currentCameraFacing === 'user' ? 'environment' : 'user';
    btnFlipCam.style.transform = 'rotate(180deg)';
    setTimeout(() => { btnFlipCam.style.transform = ''; }, 300);

    if (useRealWebcam) {
      await startCameraStream();
    } else {
      const altar = document.querySelector('.pulpit-podium');
      if (currentCameraFacing === 'user') {
        altar.textContent = 'Câmera Frontal (Púlpito)';
      } else {
        altar.textContent = 'Câmera Traseira (Altar / Congregação)';
      }
    }
  });

  // 11. Mudo
  btnMuteMic.addEventListener('click', () => {
    isMuted = !isMuted;
    if (isMuted) {
      btnMuteMic.textContent = '🔇';
      btnMuteMic.style.background = 'rgba(239, 68, 68, 0.4)';
    } else {
      btnMuteMic.textContent = '🎤';
      btnMuteMic.style.background = '';
    }
  });

  // 12. Lanterna
  btnToggleLight.addEventListener('click', () => {
    isFlashOn = !isFlashOn;
    btnToggleLight.style.color = isFlashOn ? '#fbbf24' : '#ffffff';
    btnToggleLight.style.borderColor = isFlashOn ? '#f59e0b' : '';
  });

  // 13. Compartilhar
  btnCopyLink.addEventListener('click', () => {
    navigator.clipboard?.writeText('https://youtube.com/live/culto-liveinfe');
    btnCopyLink.textContent = '✔ Copiado!';
    setTimeout(() => { btnCopyLink.textContent = '🔗 Link'; }, 2000);
  });

  // 14. Gestão de Câmera Real vs Simulada
  btnUseWebcam.addEventListener('click', async () => {
    btnUseWebcam.classList.add('active');
    btnUseAltar.classList.remove('active');
    useRealWebcam = true;
    if (isStreaming) {
      await startCameraStream();
    }
  });

  btnUseAltar.addEventListener('click', () => {
    btnUseAltar.classList.add('active');
    btnUseWebcam.classList.remove('active');
    useRealWebcam = false;
    stopCameraStream();
  });

  async function startCameraStream() {
    try {
      stopCameraStream();
      mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          aspectRatio: { ideal: 16 / 9 },
          facingMode: currentCameraFacing
        },
        audio: false
      });
      liveVideo.srcObject = mediaStream;
      liveVideo.style.display = 'block';
      simulatedFeed.style.display = 'none';
    } catch (e) {
      console.warn('Webcam não disponível, usando simulação:', e);
      stopCameraStream();
    }
  }

  function stopCameraStream() {
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop());
      mediaStream = null;
    }
    liveVideo.style.display = 'none';
    simulatedFeed.style.display = 'flex';
  }

  // 15. Monitor de Rede e Bitrate
  selectNetworkHealth.addEventListener('change', updateBitrateDisplay);

  function updateBitrateDisplay() {
    const health = selectNetworkHealth.value;
    const statusDot = document.querySelector('.status-indicator-green');
    if (health === 'good') {
      liveBitrateEl.textContent = '4000 kbps';
      statusDot.style.background = '#10b981';
      statusDot.style.boxShadow = '0 0 6px #10b981';
    } else if (health === 'medium') {
      liveBitrateEl.textContent = '2480 kbps';
      statusDot.style.background = '#f59e0b';
      statusDot.style.boxShadow = '0 0 6px #f59e0b';
    } else {
      liveBitrateEl.textContent = '1150 kbps';
      statusDot.style.background = '#ef4444';
      statusDot.style.boxShadow = '0 0 6px #ef4444';
    }
  }
});
