# 🔴 LiveinFé — Transmissão Ministerial para o YouTube

[![Versão](https://img.shields.io/github/v/release/JonathasHenrique/liveinfe?style=flat-square&color=10b981&label=Versao)](https://github.com/JonathasHenrique/liveinfe/releases/latest)
[![Build](https://img.shields.io/badge/Build-passando-brightgreen?style=flat-square)](#-automa%C3%A7%C3%A3o-e-cicd)
[![Apoiar](https://img.shields.io/badge/Apoiar-Mercado_Pago-009ee3?style=flat-square)](https://link.mercadopago.com.br/johntechdicas)
[![YouTube](https://img.shields.io/badge/YouTube-John_TechDicas-red?style=flat-square)](https://www.youtube.com/@johntechdicas)
[![Android](https://img.shields.io/badge/Android-SDK_24+-3ddc84?style=flat-square)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-v1.9+-7f52ff?style=flat-square)](https://kotlinlang.org)
[![Pedro RootEncoder](https://img.shields.io/badge/Pedro_RootEncoder-v2.4.6-orange?style=flat-square)](https://github.com/pedroSG94/RootEncoder)
[![Java](https://img.shields.io/badge/Java-21_Temurin-blue?style=flat-square)](https://adoptium.net)

> **LiveinFé** (*trocadilho com "Viver em Fé"*) é um aplicativo nativo para Android desenvolvido para igrejas, ministérios e congregações realizarem transmissões ao vivo de cultos e eventos diretamente para o YouTube com máxima estabilidade, áudio de alta fidelidade e operação simplificada para voluntários.

### 📥 Download Rápido (Versão Mais Recente)

[![Baixar APK Android](https://img.shields.io/badge/Download-Android_APK_(Latest)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/JonathasHenrique/liveinfe/releases/latest)
[![Ver Todas as Releases](https://img.shields.io/badge/Ver_Todas-Releases_no_GitHub-24292e?style=for-the-badge&logo=github&logoColor=white)](https://github.com/JonathasHenrique/liveinfe/releases)
[![Manual com Prints](https://img.shields.io/badge/Manual-Guia_de_Transmissão_(Prints)-ff0000?style=for-the-badge&logo=youtube&logoColor=white)](docs/GUIA_TRANSMISSAO_YOUTUBE.md)

> 📖 **Guia Visual Passo a Passo:** Consulte o [**Manual Completo de Transmissão com Prints**](docs/GUIA_TRANSMISSAO_YOUTUBE.md) para aprender a usar a chave e transmitir cultos programados com títulos e capas.

---

## 📋 Sumário

- [Download e Instalação Rápida](#-download-e-instala%C3%A7%C3%A3o-r%C3%A1pida)
- [Guia Visual de Transmissão Passo a Passo (Manual com Prints)](docs/GUIA_TRANSMISSAO_YOUTUBE.md)
- [Visão Geral](#-vis%C3%A3o-geral)
- [Principais Funcionalidades da Versão v1.1.3](#-principais-funcionalidades-da-vers%C3%A3o-v113)
- [Arquitetura e Recursos Técnicos](#-arquitetura-e-recursos-t%C3%A9cnicos)
  - [1. Proporção 16:9 Nativa sem Distorções (OpenGL ES)](#1-propor%C3%A7%C3%A3o-169-nativa-sem-distor%C3%A7%C3%B5es-opengl-es)
  - [2. Captura de Áudio para Louvor e Mesas de Som (USB OTG)](#2-captura-de-%C3%A1udio-para-louvor-e-mesas-de-som-usb-otg)
  - [3. Serviço em Primeiro Plano (Foreground Service) Anti-Interrupções](#3-servi%C3%A7o-em-primeiro-plano-foreground-service-anti-interrup%C3%A7%C3%B5es)
  - [4. Bitrate Adaptativo (ABR) e Reconexão Contínua](#4-bitrate-adaptativo-abr-e-reconex%C3%A3o-cont%C3%ADnua)
  - [5. Suporte a Câmeras Ultra-Wide e Controle de Zoom](#5-suporte-a-c%C3%A2meras-ultra-wide-e-controle-de-zoom)
  - [6. Trava de Segurança contra Toques Acidentais (Touch Lock)](#6-trava-de-seguran%C3%A7a-contra-toques-acidentais-touch-lock)
  - [7. Link Permanente e Compartilhamento Rápido](#7-link-permanente-e-compartilhamento-r%C3%A1pido)
  - [8. Integração Direta com a YouTube Live Streaming API v3](#8-integra%C3%A7%C3%A3o-direta-com-a-youtube-live-streaming-api-v3)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Configuração de Credenciais no Google Cloud Console](#-configura%C3%A7%C3%A3o-de-credenciais-no-google-cloud-console)
- [Como Compilar o Aplicativo](#-como-compilar-o-aplicativo)
  - [Pelo Android Studio](#pelo-android-studio)
  - [Pela Linha de Comando (Gradle)](#pela-linha-de-comando-gradle)
- [Automação e CI/CD](#-automa%C3%A7%C3%A3o-e-cicd)
- [Simulador Web Preview](#-simulador-web-preview)
- [Guia Operacional para Voluntários](#-guia-operacional-para-volunt%C3%A1rios)
- [Apoie o Projeto](#-apoie-o-projeto)
- [Avisos Legais](#-avisos-legais)
- [Licença](#-licen%C3%A7a)

---

## 📥 Download e Instalação Rápida

| Plataforma | Pacote / Arquivo | Download Direto | Versão Oficial |
| :--- | :--- | :--- | :--- |
| **Android** (Celular da Transmissão) | `liveinfe-v1.1.3.apk` | [⬇️ **Baixar APK Direto**](https://github.com/JonathasHenrique/liveinfe/releases/latest/download/liveinfe-v1.1.3.apk) | [v1.1.3 (Latest)](https://github.com/JonathasHenrique/liveinfe/releases/latest) |

> 📌 **Como instalar no celular da equipe de transmissão:**  
> 1. Toque no link de download acima para baixar o arquivo `liveinfe-v1.1.3.apk`;  
> 2. Ao concluir o download, abra o arquivo no celular e autorize a instalação de fontes desconhecidas no navegador;  
> 3. Abra o aplicativo, selecione o canal da igreja com 1 clique e inicie a transmissão com total estabilidade.  
>  
> 💡 **Histórico e outras versões:** Acesse a página oficial de [**Releases no GitHub**](https://github.com/JonathasHenrique/liveinfe/releases).

---

## 🌟 Visão Geral

A transmissão ao vivo em congregações costuma ser operada por voluntários e equipes de comunicação que necessitam de ferramentas confiáveis, intuitivas e livres de complexidades técnicas excessivas (como digitação repetitiva de URLs de servidores e chaves de streaming em cada culto).

O **LiveinFé** foi projetado para atender especificamente a esse cenário:
- **Automação Completa**: Conexão segura via conta Google autorizada do canal e criação do evento de transmissão em apenas 1 toque;
- **Arquitetura Resiliente**: O envio de áudio e vídeo é sustentado por um serviço nativo desacoplado da interface, garantindo que a transmissão permaneça ativa mesmo caso o aplicativo saia de foco;
- **Interface Focada no Operador**: Visual limpo em tons escuros (*Midnight Navy*) com botões de ação bem contrastados e atalhos rápidos com títulos de cultos programáveis.

---

## 🚀 Principais Funcionalidades da Versão v1.1.3

| Funcionalidade | Descrição Técnica | Benefício Prático |
| :--- | :--- | :--- |
| **Interface Inteligente Contextual** | No Modo Chave, campos de título e presets são ocultados com aviso claro de gerenciamento no YouTube Studio. | Elimina dúvidas e confusões do operador durante a preparação do culto. |
| **Serviço Persistente em Primeiro Plano** | Transmissão gerenciada por `StreamService` com notificação ativa no sistema. | A live permanece no ar caso o aparelho receba chamadas, a tela seja desligada ou outros apps sejam acessados. |
| **Bitrate Adaptativo (ABR)** | Ajuste dinâmico de taxa de dados de acordo com a vazão de rede. | Previne congelamentos e quedas para os espectadores durante oscilações do sinal de internet. |
| **Reconexão Automática Contínua** | Mecanismo de retomada de conexão sem limite de tentativas manuais. | Restabelece a transmissão de forma transparente caso ocorram quedas temporárias de rede. |
| **Câmera Ultra-Wide (0.5x)** | Seleção de lentes com amplo campo de visão em aparelhos com múltiplos sensores. | Facilita o enquadramento de todo o altar, púlpito e congregação mesmo em espaços menores. |
| **Trava de Toques (Touch Lock)** | Bloqueio do visor com destravamento intencional por pressão de 2 segundos. | Evita interrupções acidentais provocadas por esbarrões no suporte ou manuseio do celular. |
| **Link Permanente do Canal** | Integração direta ao endereço permanente do canal (`/live`). | Permite à igreja divulgar um link único e contínuo para seus membros. |
| **Compartilhamento Rápido** | Atalhos para cópia do link e envio para aplicativos de mensagens. | Agilidade imediata na divulgação do culto no início da transmissão. |

---

## ⚙️ Arquitetura e Recursos Técnicos

### 1. Proporção 16:9 Nativa sem Distorções (OpenGL ES)
- **Ajuste de Sensor (Center Crop)**: Enquadramento com correção geométrica via renderizador OpenGL ES, assegurando que o vídeo seja transmitido exatamente na proporção widescreen 16:9 sem distorções horizontais ou verticais.
- **Resoluções Suportadas**:
  - **1080p Full HD**: 1920x1080 a 30 FPS, Bitrate de ~4.000 kbps, intervalo de quadros-chave (GOP) calibrado para 2 segundos;
  - **720p HD**: 1280x720 a 30 FPS, Bitrate de ~2.500 kbps (recomendado para conexões móveis mais restritas).
- **Codificação por Hardware**: Utilização de codificadores H.264/AVC nativos via `MediaCodec` do Android para baixo consumo de bateria e mínimo aquecimento térmico.

### 2. Captura de Áudio para Louvor e Mesas de Som (USB OTG)
- **Áudio de Alta Qualidade**: Codificação AAC Estéreo a 48.000 Hz e 128 kbps.
- **Perfil de Gravação Otimizado**: Configuração de entrada de áudio via fonte de captura que evita filtros agressivos de cancelamento de ruído vocal, preservando a dinâmica musical de instrumentos, vozes e ambientação do culto.
- **Compatibilidade com Interfaces e Mesas**: Suporte à entrada de som estéreo proveniente de mesas de áudio conectadas via cabo USB OTG ou adaptadores de áudio compatíveis.
- **VU Meter Graduado**: Monitoramento visual dos níveis de sinal:
  - 🟢 **Faixa Segura** (-36 a -12 dB);
  - 🟡 **Nível Ideal** (-12 a -3 dB);
  - 🔴 **Alerta de Saturação** (> -3 dB).

### 3. Serviço em Primeiro Plano (Foreground Service) Anti-Interrupções
A transmissão reside em uma instância de serviço do Android (`StreamService`):
- Declarado com permissões de primeiro plano para câmera e microfone (`camera|microphone`);
- A atividade da tela (`LiveStreamActivity`) conecta-se ao serviço apenas para projetar a prévia na tela;
- Permite minimizar o app, atender a eventos prioritários do sistema operacional ou apagar a tela sem derrubar o fluxo RTMP.

### 4. Bitrate Adaptativo (ABR) e Reconexão Contínua
- Monitoramento contínuo da fila de pacotes RTMP; se a largura de banda de upload diminuir, a taxa de bits é temporariamente reduzida para evitar perda de pacotes;
- Retorno automático ao bitrate configurado quando a conexão estabilizar;
- Verificação ativa do socket de conexão para identificar quedas silenciosas de sinal e reconectar de forma transparente.

### 5. Suporte a Câmeras Ultra-Wide e Controle de Zoom
- Identificação de câmeras grande-angulares disponíveis no hardware via `CameraManager`;
- Botões de seleção direta de ampliação (`0.5x`, `1x`, `2x`);
- Controle contínuo de zoom através de gesto de pinça na tela (pinch-to-zoom);
- Alternância dinâmica entre câmera frontal e traseira mantendo o fluxo ativo.

### 6. Trava de Segurança contra Toques Acidentais (Touch Lock)
- Ativação do modo de proteção por botão na barra de controle;
- Desativa todos os comandos de encerramento e ajuste no visor;
- Desbloqueio seguro por pressão mantida durante 2 segundos com confirmação háptica (vibração) e aviso visual.

### 7. Link Permanente e Compartilhamento Rápido
- Suporte a links canônicos do canal (`/@seucanal/live`);
- Cópia segura da URL do evento gerado com retorno visual;
- Acesso à folha de compartilhamento nativa do sistema operacional para envio direto aos canais de comunicação da congregação.

### 8. Integração Direta com a YouTube Live Streaming API v3
- Autenticação OAuth 2.0 segura utilizando Google Sign-In;
- Criação automatizada de transmissões ao vivo com definição de títulos, descrições e status de privacidade (*Público*, *Não Listado* para testes e *Privado*);
- Criação e associação instantânea de fluxo de transmissão RTMP sem requerer inserção manual de credenciais;
- Parâmetros configurados para evitar encerramento involuntário do evento por oscilações temporárias de conexão.

---

## 📁 Estrutura do Projeto

```
igreja-live/
├── android/                         # Projeto Android Nativo (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/igrejalive/app/
│   │   │   │   ├── auth/           # Gerenciamento de login e tokens OAuth
│   │   │   │   │   └── GoogleAuthManager.kt
│   │   │   │   ├── data/           # Comunicação com a YouTube Data API v3
│   │   │   │   │   ├── model/YouTubeModels.kt
│   │   │   │   │   └── api/YouTubeApiService.kt
│   │   │   │   ├── stream/         # Transmissão de vídeo, áudio e Service
│   │   │   │   │   ├── StreamManager.kt    # Parâmetros de codificação e ABR
│   │   │   │   │   └── StreamService.kt    # Foreground Service persistente
│   │   │   │   └── ui/             # Interfaces com o usuário
│   │   │   │       ├── MainActivity.kt       # Tela inicial e configuração
│   │   │   │       ├── LiveStreamActivity.kt # Interface de transmissão ativa
│   │   │   │       └── VuMeterView.kt        # Medidor visual de níveis de áudio
│   │   │   ├── res/
│   │   │   │   ├── drawable/       # Ícones e elementos visuais
│   │   │   │   ├── layout/         # Telas em XML (layouts)
│   │   │   │   └── values/         # Cores, textos e estilos
│   │   │   └── AndroidManifest.xml # Configuração de permissões e serviços
│   │   ├── build.gradle.kts        # Configurações do módulo do aplicativo
│   │   └── liveinfe-debug.keystore # Chave de assinatura para ambiente de desenvolvimento
│   ├── build.gradle.kts            # Configurações raiz do Gradle
│   ├── settings.gradle.kts         # Módulos do projeto
│   └── gradlew                     # Executável do Gradle Wrapper
│
├── .github/workflows/
│   └── build.yml                    # Pipeline de compilação contínua no GitHub Actions
│
└── web-preview/                     # Simulador de demonstração web
    ├── index.html                   # Estrutura do simulador
    ├── style.css                    # Estilos visuais da demonstração
    └── app.js                       # Comportamento interativo simulado
```

---

## 🔑 Configuração de Credenciais no Google Cloud Console

Para habilitar a comunicação direta com o canal da congregação:

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/);
2. Crie ou selecione o projeto correspondente à igreja;
3. No menu **APIs e Serviços > Biblioteca**, habilite a **YouTube Data API v3**;
4. Na seção **Tela de consentimento OAuth**, configure os dados institucionais e inclua o escopo:
   - `https://www.googleapis.com/auth/youtube`
5. Em **Credenciais**, gere um **ID do cliente OAuth 2.0**:
   - **Tipo**: Aplicativo Android
   - **Nome do pacote**: `com.igrejalive.app`
   - **Impressão digital SHA-1**: Obtenha a assinatura do keystore utilizado para compilar o APK:
     ```bash
     keytool -list -v -keystore android/app/liveinfe-debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

---

## 🔨 Como Compilar o Aplicativo

### Requisitos de Ambiente
- **Android Studio** (versão Ladybug 2024.2 ou superior recomendada)
- **JDK 17 ou 21**
- **Android SDK Platform 34** instalada no ambiente

### Pelo Android Studio
1. Abra o diretório `igreja-live/android` no **Android Studio**;
2. Aguarde o término da sincronização do Gradle;
3. Conecte o aparelho Android com a opção de depuração USB ativa;
4. Selecione o menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**;
5. O pacote compilado estará disponível na pasta:  
   `android/app/build/outputs/apk/debug/app-debug.apk`.

### Pela Linha de Comando (Gradle)
```bash
cd android
chmod +x gradlew
./gradlew assembleDebug --no-daemon
```

---

## 🤖 Automação e CI/CD

O repositório dispõe de pipeline no **GitHub Actions** (`.github/workflows/build.yml`):
- A cada alteração enviada ao branch principal, o ambiente compila e valida o projeto utilizando Java 21;
- Ao registrar tags de versão (por exemplo, `v1.1.2`), o artefato `liveinfe-v1.1.2.apk` é compilado automaticamente e publicado na área de **Releases** do repositório para distribuição imediata.

---

## 🌐 Simulador Web Preview

O projeto disponibiliza uma pasta `web-preview/` contendo um demonstrador para testes de interface em navegadores web:

```bash
cd web-preview
python3 -m http.server 4180
```
Abra `http://localhost:4180` no navegador para visualizar o protótipo.

---

## 📖 Guia Operacional para Voluntários

1. **Montagem**: Posicione o dispositivo móvel em suporte estável ou tripé, no formato horizontal (paisagem);
2. **Conexão de Áudio**: Caso utilize mesa de som, conecte o cabo à porta do smartphone antes de abrir o aplicativo;
3. **Identificação do Culto**: Selecione um dos botões rápidos com o título do culto desejado ou digite um título personalizado;
4. **Enquadramento**: Escolha a lente adequada (`0.5x` para templos compactos ou `1x` para visão padrão);
5. **Início da Transmissão**: Pressione o botão **`ENTRAR AO VIVO NO YOUTUBE`**;
6. **Proteção de Tela**: Toque no ícone de cadeado para bloquear toques acidentais durante a transmissão;
7. **Divulgação**: Utilize o atalho de compartilhamento para divulgar o link nos grupos da congregação.

---

## ❤️ Apoie o Projeto

O **LiveinFé** é um projeto de código aberto, gratuito e sem fins comerciais, desenvolvido para servir igrejas e congregações com ferramentas profissionais e acessíveis de transmissão ao vivo.

Se este aplicativo ajuda ou tem abençoado as transmissões da sua igreja, você pode apoiar voluntariamente o projeto com qualquer valor para ajudar nos custos contínuos de desenvolvimento e publicação:

[![Apoiar via Mercado Pago](https://img.shields.io/badge/Apoiar_via-Mercado_Pago-009ee3?style=for-the-badge&logo=mercadopago&logoColor=white)](https://link.mercadopago.com.br/johntechdicas)

👉 **Link de Apoio:** [link.mercadopago.com.br/johntechdicas](https://link.mercadopago.com.br/johntechdicas) *(Pix ou Cartão)*

📺 Acompanhe tutoriais, novidades e demonstrações no canal **[John TechDicas no YouTube](https://www.youtube.com/@johntechdicas)**!

---

## ⚖️ Avisos Legais

- **Marcas Registradas**: *YouTube* e *Android* são marcas registradas de titularidade de seus respectivos detentores de direitos. O uso destes termos neste projeto possui caráter meramente descritivo e de integração técnica com suas respectivas APIs públicas e plataformas.
- **Isenção de Vínculo**: Este aplicativo é um projeto independente de código aberto desenvolvido para fins ministeriais e comunitários, sem vínculo institucional ou endosso formal com serviços de terceiros.

---

## 📜 Licença

Distribuído sob a licença **MIT**. Consulte o arquivo de licença do projeto para mais detalhes.
