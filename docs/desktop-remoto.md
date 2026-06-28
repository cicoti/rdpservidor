# Desktop Remoto ("RDP") — Funcionamento Técnico

> Este documento descreve o **sistema completo de desktop remoto** da S4E: o
> **cliente**, embarcado no **viewer** (`viewer-vpu`), e o **servidor**
> (`RDPServidor`). Os dois repositórios mantêm uma cópia idêntica deste arquivo.

---

## 1. Visão geral

O recurso permite que o **viewer exiba e controle a tela de uma máquina remota**.
São dois programas:

- **Cliente** — dentro do viewer (`com.s4etech.integration.*`): recebe e exibe a
  tela do servidor e captura mouse/teclado do operador.
- **Servidor** — `RDPServidor` (`com.s4etech.desktop.*`): captura a própria tela,
  transmite, e reproduz o input recebido. Roda como aplicativo de **bandeja
  (system tray)** no Windows.

A comunicação é **toda em UDP**, dividida em **3 canais** com portas distintas.

> ⚠️ **"RDP" é apenas o nome.** Não é o protocolo RDP da Microsoft (porta 3389) e
> não usa nenhuma biblioteca de RDP (não há FreeRDP nem similar). É um protocolo
> **próprio** da S4E, montado sobre **UDP + GStreamer**. A opção por uma
> implementação própria — em vez do protocolo **RDP da Microsoft** — se deve ao
> **suporte limitado e desatualizado** da API Java para esse protocolo.

---

## 2. Arquitetura

```
        CLIENTE (viewer-vpu)                          SERVIDOR (RDPServidor)
  ┌──────────────────────────┐                 ┌────────────────────────────────┐
  │ ClientHandshakeSender     │── HELLO ─UDP──▶ │ ServerHandshakeListener         │
  │                           │◀─ ACK   ─UDP─── │  (handshakePort)                │
  ├──────────────────────────┤                 ├────────────────────────────────┤
  │ VideoPanel (GStreamer)    │◀═ H264/RTP ═UDP═│ ScreenStreamServer              │
  │  (videoPort)              │                 │  (d3d11 capture → x264 → udpsink)│
  ├──────────────────────────┤                 ├────────────────────────────────┤
  │ MouseControlClient        │── input ─UDP──▶ │ MouseControlServer              │
  │  (mouse/teclado)          │                 │  (Robot + JNA SendInput)        │
  └──────────────────────────┘                 └────────────────────────────────┘
```

---

## 3. Os 3 canais (UDP)

| Canal | Porta | Sentido | Conteúdo |
|---|---|---|---|
| **Handshake** | `handshakePort` | cliente ⇄ servidor | abertura de sessão (HELLO/ACK) |
| **Vídeo** | `videoPort` | servidor → cliente | tela em **H264/RTP** |
| **Controle** | `controlePort` | cliente → servidor | eventos de mouse/teclado |

---

## 4. Configuração

**Cliente** — arquivo `rdp.cfg` (lido por `RDPConfigurationManager`): uma linha
com 5 campos separados por `|`, validados (portas distintas, IP válido):

```
servidor(IP) | ativar(true/false) | handshakePort | videoPort | controlePort
```

**Servidor** — `ServerConfig` (via `ServerConfigManager`, editável pela bandeja):
define `handshakePort`, `controlPort` e o **perfil de conexão**
(`ConnectionProfile`), que controla a transmissão:

- resolução, `fps`, `bitrate` (kbps)
- `tune` e `speed-preset` do x264, `key-int-max`, fila *leaky* (sim/não)

> O `videoPort` **não** é fixo no servidor: o cliente o escolhe e o informa no
> handshake. O servidor envia o vídeo para `clientIp:videoPort`.

---

## 5. Fluxo de uma sessão

1. **Config** — o viewer lê o `rdp.cfg` (IP do servidor + 3 portas).
2. **Handshake** — `ClientHandshakeSender` envia **HELLO** para `handshakePort`
   a cada 1 s, informando seu IP + videoPort + controlPort. O
   `ServerHandshakeListener` recebe, responde **ACK** e cria uma sessão
   (`sessionId` incremental).
3. **Ativação** — `ScreenStreamServer` consome a sessão e inicia o pipeline de
   captura, apontando o `udpsink` para o **IP + videoPort** do cliente.
4. **Vídeo** — o servidor captura a tela, codifica em H264 e envia via RTP/UDP.
   O `VideoPanel` recebe com GStreamer, decodifica e exibe.
5. **Controle** — o `MouseControlClient` captura mouse/teclado do operador sobre o
   painel de vídeo, normaliza as coordenadas e envia ao servidor. O
   `MouseControlServer` reproduz os eventos na máquina remota.

---

## 6. Protocolo de pacotes

Pacotes binários (`DataOutputStream`/`DataInputStream`, big-endian).

### 6.1 Handshake (`handshakePort`)

**HELLO** — cliente → servidor:

| Campo | Tipo | Valor |
|---|---|---|
| tipo | `byte` | `100` (TYPE_HELLO) |
| IP local | `UTF` | IP do cliente |
| videoPort | `int` | porta onde receberá o vídeo |
| controlPort | `int` | porta de onde enviará o controle |

**ACK** — servidor → cliente: um único `byte` = `101` (TYPE_ACK).

### 6.2 Controle (`controlePort`) — cliente → servidor

| Evento | Código | Payload |
|---|---|---|
| MOVE | `1` | `int xNorm`, `int yNorm` (0–10000) |
| LEFT_DOWN | `2` | — |
| LEFT_UP | `3` | — |
| RIGHT_DOWN | `4` | — |
| RIGHT_UP | `5` | — |
| KEY_DOWN | `10` | `int keyCode` (AWT `KeyEvent`) |
| KEY_UP | `11` | `int keyCode` (AWT `KeyEvent`) |
| KEY_TYPED | `12` | `char` (Unicode, 2 bytes) |

### 6.3 Vídeo (`videoPort`)

Não é protocolo próprio: é **H264 empacotado em RTP** (payload type `96`,
clock-rate `90000`) sobre UDP — gerado e consumido pelo GStreamer.

---

## 7. Pipelines GStreamer

**Servidor — captura e envio** (`ScreenStreamServer.buildPipeline`):

```
d3d11screencapturesrc monitor-index=0 show-cursor=false
  ! queue [leaky=downstream max-size-buffers=2]
  ! videoconvert ! videoscale
  ! video/x-raw,format=I420,width=W,height=H,framerate=FPS/1
  ! x264enc tune=<tune> speed-preset=<preset> bitrate=<kbps>
            key-int-max=<n> bframes=0 byte-stream=true aud=true
  ! h264parse ! rtph264pay pt=96 config-interval=1 mtu=1200
  ! udpsink host=<clientIp> port=<videoPort> sync=false async=false
```

**Cliente — recepção e exibição** (`VideoPanel.buildPipelineDescription`):

```
udpsrc port=<videoPort> timeout=<ns>
       caps="application/x-rtp,media=video,encoding-name=H264,payload=96,clock-rate=90000"
  ! ... ! rtph264depay ! h264parse ! avdec_h264 ! <sink no painel Swing>
```

Notas:
- Captura via **Direct3D 11** (`d3d11screencapturesrc`) — exclusivo do Windows.
- `bframes=0` + `tune`/`speed-preset` priorizam **baixa latência**.
- `key-int-max` controla a frequência de keyframes — importante porque **UDP não
  garante entrega** (após perda, o próximo keyframe recupera a imagem).

---

## 8. Reprodução de input no servidor

`MouseControlServer` recebe os pacotes de controle e reproduz:

- **Mouse move:** desnormaliza — `logical = (norm/10000) * tela`, depois
  `real = logical * dpiScale` — e chama `robot.mouseMove(realX, realY)`.
- **Botões:** `robot.mousePress/Release` com `BUTTON1`/`BUTTON3`.
- **Teclas (down/up):** `robot.keyPress/keyRelease(keyCode)`.
- **Caractere (typed):** `User32.SendInput` (**JNA**) com `KEYEVENTF_UNICODE` —
  necessário para acentuados/Unicode, que o `Robot` não digita.
- **DPI awareness:** `SetProcessDPIAware` (via JNA) para o mapeamento de
  coordenadas bater em telas com escala.

### Coordenadas normalizadas (0–10000)

O cliente envia a posição **relativa à área do vídeo** (corrigindo o DPI do
cliente); o servidor converte para a **resolução real** dele (corrigindo o DPI do
servidor). Assim, clientes e servidores com **resoluções/DPIs diferentes**
funcionam sem ajuste manual.

---

## 9. Robustez e sessões

- **Sessão:** cada HELLO gera um `sessionId`. Uma nova sessão de outro cliente
  **assume** a transmissão; sessão repetida do mesmo destino é **reaproveitada**.
- **Filtro de origem:** o servidor só processa controle vindo do **IP da sessão
  ativa** (`ActiveSessionContext`, compartilhado entre tela e mouse).
- **Retry de pipeline:** até 3 tentativas (1 s entre elas) ao iniciar.
- **Auto-recuperação:** o *bus* do GStreamer é monitorado; em `ERROR`/`EOS` o
  pipeline é reiniciado automaticamente, sem ação do cliente.
- **Handshake resiliente:** o cliente reenvia HELLO a cada 1 s até o ACK.

---

## 10. O servidor como aplicação

- **Entry point:** `com.s4etech.Main` → `RemoteDesktopServer.main()`.
- **Instância única:** *file lock* em `remote_desktop_server.lock` (impede abrir
  duas vezes).
- **System tray:** ícone na bandeja com menu **Status / Configuração / Ajuda /
  Sair** (Swing/AWT). A configuração abre o `ServerConfigDialog`.
- **Ciclo de vida:** `RemoteDesktopServer` cria e inicia o `ScreenStreamServer` e
  o `MouseControlServer` (compartilhando o `ActiveSessionContext`) e cuida do
  *shutdown* limpo (para servidores, libera GStreamer e o lock).

---

## 11. Mapa de classes

| Função | Cliente (`viewer-vpu` · `com.s4etech.integration`) | Servidor (`RDPServidor` · `com.s4etech.desktop`) |
|---|---|---|
| Handshake | `ClientHandshakeSender` | `listener.ServerHandshakeListener` |
| Vídeo | `VideoPanel` | `server.ScreenStreamServer` |
| Controle | `MouseControlClient` | `server.MouseControlServer` |
| Orquestração | `ui.screens.Viewer` | `server.RemoteDesktopServer` (+ `Main`) |
| Configuração | `config.manager.RDPConfigurationManager`, `dto.RDPDTO` | `config.ServerConfig`, `config.ConnectionProfile`, `config.ServerConfigManager` |
| Sessão | — | `session.ActiveSessionContext`, `ServerHandshakeListener.HandshakeSession` |

---

## 12. Tecnologias

| Tecnologia | Papel |
|---|---|
| **Java 22** | Linguagem (cliente e servidor) |
| **GStreamer** (`gst1-java-core`) | Captura (`d3d11`), codificação (`x264enc`) e transporte (RTP/UDP) do vídeo |
| **UDP** (`java.net.DatagramSocket`) | Os 3 canais: handshake, vídeo e controle |
| **java.awt.Robot** | Reprodução de mouse e teclas no servidor |
| **JNA** (`User32.SendInput`, `SetProcessDPIAware`) | Digitação Unicode e DPI awareness no servidor |
| **Swing / AWT** | Exibição do vídeo (cliente) e system tray/diálogos (servidor) |
| **SLF4J + Logback** | Logging |

---

> **Resumo:** **solução própria** de desktop remoto sobre UDP — o servidor captura a tela com
> GStreamer (`d3d11 → x264 → RTP/UDP`) e o cliente recebe e decodifica; o
> mouse/teclado vai no sentido inverso em pacotes UDP próprios, reproduzidos com
> `Robot` + `JNA`. De "RDP" (protocolo Microsoft) não tem nada — é só o nome.
