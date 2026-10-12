# 🎥 Guia Completo de Transmissão: LiveinFé + YouTube Studio

> **Manual Prático e Visual para Operadores, Voluntários e Equipes de Mídia da Igreja**  
> *Aprenda como transmitir cultos ao vivo pelo celular com qualidade profissional, áudio direto da mesa e proporção 16:9 nativa sem distorções.*

---

## 📌 Sumário
1. [Comparativo: Qual Modo Usar na Sua Igreja?](#1-comparativo-qual-modo-usar)
2. [Método 1: Transmissão Rápida com Chave Padrão (Sem Agendamento)](#2-método-1-transmissão-rápida-com-chave-padrão)
3. [Método 2: Transmissão Profissional Agendada (Com Título, Capa e Horário Marcado)](#3-método-2-transmissão-profissional-agendada)
4. [Método 3: Transmissão com 1 Toque via Conta Google (OAuth Automático)](#4-método-3-transmissão-com-1-toque-via-conta-google)
5. [Controles do Operador Durante a Live (Áudio, Zoom e Trava)](#5-controles-do-operador-durante-a-live)
6. [Configurações Importantes no YouTube Studio](#6-configurações-importantes-no-youtube-studio)
7. [Checklist do Operador no Dia do Culto](#7-checklist-do-operador-no-dia-do-culto)
8. [Dúvidas Frequentes e Solução de Problemas](#8-dúvidas-frequentes-e-solução-de-problemas)

---

## 1. Comparativo: Qual Modo Usar?

O YouTube oferece duas formas principais de receber a transmissão do seu aplicativo:

```mermaid
graph TD
    A[Como a Igreja quer transmitir?] --> B[Culto Imediato / Rápido]
    A --> C[Culto Divulgado com Antecedência]
    B --> D[Método 1: Chave Padrão / Transmitir Agora]
    C --> E[Método 2: Live Agendada com Capa e Título]
    D --> F[Cola a Chave 1 vez no celular e aperta Iniciar]
    E --> G[Agenda no YouTube Studio e transmite na hora marcada]
```

| Recurso | Método 1: Chave Padrão (`((•))`) | Método 2: Transmissão Agendada (`📅`) |
| :--- | :--- | :--- |
| **Ideal para** | Cultos diários, orações, vigílias ou quando não deu tempo de programar. | Cultos principais de domingo, conferências e eventos especiais. |
| **Título e Capa** | Usa o título padrão ou editado previamente no Studio. | Título personalizado para o dia (ex: *"Culto da Família • Pr. João"*). |
| **Link Antecipado** | Usa o link permanente do canal: `youtube.com/@seucanal/live`. | Gera link específico do evento com contagem regressiva para lembrete. |
| **Esforço no Dia** | **Zero esforço**: abre o app e aperta Iniciar. | Abre o app, seleciona a chave e aperta Iniciar. |

---

## 2. Método 1: Transmissão Rápida com Chave Padrão

Neste método, você usa a **Chave de Transmissão Padrão (Default Stream Key)** do canal.

### Passo 1: Localizar a Chave no YouTube Studio
1. Acesse o **[YouTube Studio](https://studio.youtube.com)** no navegador do computador ou celular.
2. No canto superior direito, clique em **Criar** ➔ **Fazer transmissão ao vivo**.
3. No menu lateral esquerdo, clique no primeiro ícone: **`((•))` Transmitir**.

![YouTube Studio - Conectar Codificador e Chave Padrão](images/01_youtube_studio_chave.png)

4. No painel central, localize o campo **Chave da transmissão**:
   * Clique no botão **Copiar** (não precisa revelar a chave).

### Passo 2: Colar no Aplicativo LiveinFé
1. Abra o **LiveinFé** no celular.
2. No modo de conexão, selecione **🔑 Chave de Transmissão**.
3. Cole a chave copiada no campo correspondente.
4. O app exibirá: `✅ Chave salva e configurada! Pronto para transmitir.`

![Aplicativo LiveinFé - Modo Chave com Aviso Contextual](images/04_liveinfe_modo_chave.png)

> [!TIP]
> **Você só precisa colar essa chave UMA ÚNICA VEZ.**  
> O aplicativo memoriza a chave na memória segura do aparelho. Nos próximos cultos, é só abrir o app e tocar em **Entrar ao Vivo no YouTube**.

### Passo 3: Iniciar a Transmissão
* Toque no botão verde **ENTRAR AO VIVO NO YOUTUBE**.
* Em cerca de 3 a 5 segundos, o sinal entra no ar no canal!

![Transmissão ao Vivo em Tempo Real no YouTube](images/03_youtube_live_ao_vivo.png)

---

## 3. Método 2: Transmissão Profissional Agendada

Se a igreja deseja divulgar a transmissão dias antes no WhatsApp com a arte oficial (capa/miniatura), o tema da mensagem e o nome do pregador:

### Passo 1: Agendar o Culto no YouTube Studio
1. No YouTube Studio, clique no ícone de calendário: **`📅` Gerenciar** (menu esquerdo).
2. Clique no botão azul **Programar transmissão**.
3. Preencha os detalhes do culto:
   * **Título:** Ex: *"Culto de Celebração e Santa Ceia • 18/10/2026"*
   * **Descrição:** Versículos, links para dízimos/ofertas e redes sociais da igreja.
   * **Miniatura:** Envie a imagem da capa do culto (1280x720).
   * **Data e Horário:** Defina o dia e o horário do início do culto.
   * **Visibilidade:** Marque como **Público**.
4. Clique em **Criar transmissão**.

### Passo 2: Como Vincular ao LiveinFé sem Redigitar Chaves
* Na tela da live agendada, em **Configurações da transmissão**, selecione a chave:  
  👉 **`Default stream key (RTMP)`**.
* **Como o LiveinFé já está configurado com essa mesma chave padrão**, você **não precisa alterar nada no celular**!
* Na hora do culto, basta tocar em iniciar no app e o YouTube direcionará o sinal para o evento agendado do dia.

---

## 4. Método 3: Transmissão com 1 Toque via Conta Google

O LiveinFé também conta com integração nativa com a **YouTube Live Streaming API v3**.  
Neste modo, o operador pode definir o título e a privacidade da live diretamente pelo celular:

![Aplicativo LiveinFé - Modo Conta Google](images/05_liveinfe_modo_google.png)

1. Marque a opção **🌐 Conta Google (1 Clique)**.
2. Toque no canal da igreja conectado.
3. Escolha um chip de atalho rápido (ex: *🔥 Culto de Celebração*, *🙏 Culto de Oração*) ou digite o título personalizado.
4. Escolha a privacidade (**Público** para o culto oficial ou **Não Listado** para testes da equipe).
5. Ao tocar em **ENTRAR AO VIVO**, o app se comunica via API com o YouTube, cria o evento, configura 1080p e inicia a transmissão automaticamente!

![Criando Transmissão Automatizada na API do YouTube](images/06_liveinfe_criando_live.png)

---

## 5. Controles do Operador Durante a Live

Durante a transmissão, a tela do LiveinFé oferece todos os recursos profissionais para o operador:

![Controles da Câmera, Áudio e Trava no LiveinFé](images/07_liveinfe_camera_ao_vivo.png)

* 🔴 **Indicador AO VIVO:** Mostra o tempo decorrido do culto em tempo real.
* 🟢 **Monitor de Conexão e Bitrate:** Exibe a taxa de transferência real (ex: `3861 kbps • 30 FPS`) com indicador de integridade de rede.
* 🎚️ **VU Meter de Áudio em Tempo Real:** Medição precisa em decibéis (`-12dB IGREJA`) para garantir que o som da mesa não esteja distorcendo nem baixo demais.
* 🔍 **Controle de Lentes e Zoom:** Alternância rápida entre `.5x` (Ultra-Wide para enquadrar todo o altar e a congregação), `1x` e `2x`, além de controle suave por gesto de pinça.
* 🔒 **Trava de Toques (Touch Lock):** Bloqueia a tela contra esbarrões no tripé. Para destravar, segure por 2 segundos.
* 🛑 **Encerramento Seguro:** Botão com confirmação por pressão contínua (**SEGURE P/ ENCERRAR**) para impedir que o voluntário encerre o culto por acidente.

---

## 6. Configurações Importantes no YouTube Studio

Na tela de configurações da transmissão, verifique os seguintes ajustes:

![Configurações da Transmissão e Integridade do Stream](images/02_youtube_studio_configuracoes.png)

1. **Ativar DVR (Recomendado: LIGADO):**
   * Permite que os espectadores que chegarem atrasados voltem o vídeo para assistir ao louvor ou ao início da pregação.
2. **Latência da transmissão:**
   * **Latência normal:** Melhor estabilidade para conexões de internet móveis.
   * **Baixa latência (Recomendado):** Permite interação rápida no chat (atraso de cerca de 4 a 6 segundos).
3. **Definir transmissão como não listada ao acabar (Opcional):**
   * Mantenha desligado se quiser que o culto continue público no canal após o encerramento.

---

## 7. Checklist do Operador no Dia do Culto

Siga este passo a passo 15 minutos antes do início do culto:

- [ ] **1. Fixação:** Posicione o celular no tripé na altura dos olhos, sem obstruir as lentes.
- [ ] **2. Cabo de Áudio (OTG):** Conecte o adaptador USB OTG no celular com o cabo vindo da saída auxiliar da mesa de som.
- [ ] **3. Bateria:** Conecte o carregador ao celular ou use um hub OTG com alimentação para evitar descarregar durante o culto.
- [ ] **4. Modo Não Perturbe:** Ative o modo "Não Perturbe" no celular para não receber notificações na tela.
- [ ] **5. Fonte de Áudio no App:** No LiveinFé, selecione **Som da Mesa (OTG / Louvor)**.
- [ ] **6. Iniciar Transmissão:** Toque em **ENTRAR AO VIVO NO YOUTUBE** 2 minutos antes do prelúdio para verificar o sinal.
- [ ] **7. Trava de Segurança:** Toque no ícone de cadeado (**Travar toques**) para evitar interrupções acidentais.

---

## 8. Dúvidas Frequentes e Solução de Problemas

### ❓ Preciso ter um computador ligado durante a live?
> **Não.** O celular envia o sinal diretamente aos servidores do Google via RTMP. O computador só é necessário se alguém quiser moderar comentários no chat.

### ❓ Como compartilho o link da transmissão com os membros?
> No LiveinFé, toque no botão **Compartilhar Link do Culto**.  
> Você pode divulgar o link permanente do canal:  
> `https://www.youtube.com/@seucanal/live`  
> Quem abrir esse link enquanto a live estiver rolando já entra direto na transmissão!

### ❓ Se a internet oscilar durante o culto, a transmissão cai?
> **Não.** O LiveinFé possui **Reconexão Automática Contínua** e **Bitrate Adaptativo**. Se o sinal oscilar, ele reduz momentaneamente a taxa de dados sem congelar o áudio e restabelece a qualidade máxima assim que a internet estabilizar.

### ❓ Como destravar a tela se o Touch Lock estiver ativo?
> Pressione e segure o dedo na tela por **2 segundos**. O app vibrará e liberará os controles da câmera e zoom.
