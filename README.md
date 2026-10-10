# 🔴 LiveinFé - Transmissão Ministerial para o YouTube

**LiveinFé** (*trocadilho com "Viver em Fé"*) é um aplicativo nativo para Android desenvolvido sob medida para igrejas e ministérios realizarem transmissões ao vivo de cultos para o YouTube.

O aplicativo segue o padrão visual do afinador da **Bíblia do Instrumentista**, trazendo como cor primária de ação o **Verde Afinador Vibrante (`#10B981`)**, a **Bíblia Sagrada Aberta** com ondas de transmissão (em total consonância com a tradição da Assembleia de Deus, sem símbolos de cruz), fundo Midnight Navy e máxima simplicidade operacional para os voluntários.

---

## ✨ Principais Diferenciais do LiveinFé

1. **Botão de Início Verde Afinador:**
   - Botão **`ENTRAR AO VIVO NO YOUTUBE`** com o gradiente esmeralda vibrante idêntico ao botão "Ativar Afinador" (`#10B981` -> `#059669`).
2. **Logo Bíblia da Palavra & Transmissão:**
   - Squircle combinando a **Bíblia Sagrada Aberta** e ondas de propagação da live para o mundo.
3. **Vídeo 16:9 Nativo Travado (Nunca Estica):**
   - Resolve o problema do app oficial do YouTube (que estica o sensor 4:3 para 16:9).
   - Usa **OpenGL ES** e o motor **Pedro RootEncoder** com *Center Crop* inteligente em 1080p Full HD (1920x1080) e 720p HD.
4. **1 Toque para Entrar Ao Vivo (Sem Tokens Manuais):**
   - Automatiza a **YouTube Live Streaming API v3** (OAuth 2.0). Cria o evento de transmissão e configura o stream RTMP com 1 clique, eliminando as falhas do IRL Pro.
5. **VU Meter de Áudio com a Paleta do Afinador:**
   - Medidor de áudio de alta precisão na tela: **Verde** (som afinado e seguro), **Dourado** (ótimo volume) e **Vermelho** (alerta de clipping/distorção).
6. **Atalhos Rápidos de Cultos:**
   - Chips para preencher o título em 1 toque: *Culto de Celebração*, *Culto de Oração*, *Encontro de Jovens*, *Santa Ceia*.
7. **Troca Rápida de Câmera:**
   - Alterna instantaneamente entre o púlpito e a nave da igreja sem derrubar o sinal da transmissão.
8. **Suporte para Mesa de Som via USB OTG:**
   - Entrada estéreo a 48kHz limpa e sem filtros agressivos de voz, garantindo dinâmica total para instrumentos e louvor.

---

## 📁 Estrutura de Arquivos

```
igreja-live/
├── android/                         # Projeto Android Nativo (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/igrejalive/app/
│   │   │   │   ├── auth/           # Login Google & Token OAuth 2.0
│   │   │   │   │   └── GoogleAuthManager.kt
│   │   │   │   ├── data/           # YouTube Data API v3 (Live Streaming)
│   │   │   │   │   ├── model/YouTubeModels.kt
│   │   │   │   │   └── api/YouTubeApiService.kt
│   │   │   │   ├── stream/         # Pedro RootEncoder & OpenGL 16:9
│   │   │   │   │   └── StreamManager.kt
│   │   │   │   └── ui/             # Telas do Voluntário & VU Meter
│   │   │   │       ├── MainActivity.kt       # Configuração & Chips de Culto
│   │   │   │       ├── LiveStreamActivity.kt # HUD da Live em Tempo Real
│   │   │   │       └── VuMeterView.kt        # Medidor de Áudio Estilo Afinador
│   │   │   ├── res/
│   │   │   │   ├── drawable/       # ic_liveinfe_logo, bg_gold_gradient, etc.
│   │   │   │   ├── layout/         # activity_main.xml & activity_live_stream.xml
│   │   │   │   └── values/         # colors.xml, strings.xml, themes.xml
│   │   │   └── AndroidManifest.xml # Permissões e Configuração de Tela
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
└── web-preview/                     # Simulador Interativo Web (porta 4180)
    ├── index.html                   # Mockup do Android com Status Bar e HUD
    ├── style.css                    # Identidade Visual Bíblia do Instrumentista
    └── app.js                       # Lógica de Transmissão e Medidor de Som
```

---

## 🚀 Como Compilar o APK no Android Studio

1. Abra a pasta `igreja-live/android` no **Android Studio**;
2. Aguarde a sincronização automática do Gradle;
3. Vá em **Build > Build Bundle(s) / APK(s) > Build APK(s)**;
4. Instale o arquivo `app-debug.apk` gerado no celular da equipe de transmissão.
