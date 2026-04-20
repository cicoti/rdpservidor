# Perfis de conexão

Os perfis de conexão definem como o servidor transmite a imagem da área de trabalho para o cliente.

Cada perfil é configurado no arquivo `remote-desktop-server.properties`.

---

## Estrutura

O perfil ativo é definido por:

```properties
connection.profile=LAN
```

Os perfis disponíveis são definidos assim:

```properties
connection.profile.<ID>=id,displayName,width,height,fps,bitrateKbps,keyIntMax,encoderPreset,encoderTune,leakyQueue
```

Exemplo:

```properties
connection.profile.LAN=LAN,Rede local,1920,1080,30,6000,30,ultrafast,zerolatency,false
```

---

## Campos do perfil

Formato:

```properties
id,displayName,width,height,fps,bitrateKbps,keyIntMax,encoderPreset,encoderTune,leakyQueue
```

### 1. `id`
Identificador interno do perfil.

Exemplos:
- `LAN`
- `SATELLITE_1`
- `WIFI`

**Impacto:**
- não altera qualidade nem desempenho
- serve apenas como chave de identificação

---

### 2. `displayName`
Nome exibido na interface.

Exemplos:
- `Rede local`
- `Satélite 1`
- `Wi-Fi`

**Impacto:**
- não altera transmissão
- afeta apenas o texto mostrado ao usuário

---

### 3. `width`
Largura da imagem transmitida.

Exemplos:
- `1024`
- `1280`
- `1920`

**Impacto:**
- quanto maior, melhor definição horizontal
- quanto maior, maior consumo de banda
- quanto maior, maior uso de CPU para captura e codificação

---

### 4. `height`
Altura da imagem transmitida.

Exemplos:
- `576`
- `720`
- `1080`

**Impacto:**
- quanto maior, melhor definição vertical
- quanto maior, maior consumo de banda
- quanto maior, maior uso de CPU

---

### 5. `fps`
Frames por segundo.

Exemplos:
- `10`
- `12`
- `15`
- `20`
- `30`

**Impacto:**
- quanto maior, mais fluidez
- quanto maior, maior uso de banda
- quanto maior, maior uso de CPU
- valores baixos deixam a navegação mais travada, mas economizam rede

---

### 6. `bitrateKbps`
Taxa de bits do vídeo em kbps.

Exemplos:
- `1000`
- `1400`
- `1800`
- `2500`
- `6000`

**Impacto:**
- bitrate baixo reduz consumo de banda, mas piora a qualidade visual
- bitrate alto melhora a qualidade, mas aumenta o consumo de banda

---

### 7. `keyIntMax`
Intervalo máximo entre quadros-chave do encoder.

Exemplos:
- `20`
- `24`
- `30`

**Impacto:**
- valores menores recuperam a imagem mais rápido em redes instáveis
- valores maiores melhoram a compressão em redes estáveis

---

### 8. `encoderPreset`
Preset do encoder H.264.

Exemplos comuns:
- `ultrafast`
- `superfast`
- `veryfast`
- `faster`
- `fast`
- `medium`
- `slow`

**Impacto:**
- presets mais rápidos usam menos CPU, mas comprimem pior
- presets mais lentos usam mais CPU, mas comprimem melhor

**Recomendação:**
- para desktop remoto, normalmente usar `ultrafast` ou `veryfast`

---

### 9. `encoderTune`
Ajuste fino do encoder.

Exemplos comuns:
- `zerolatency`
- `film`
- `animation`
- `grain`
- `stillimage`
- `fastdecode`

**Impacto:**
- para acesso remoto interativo, o recomendado é `zerolatency`

---

### 10. `leakyQueue`
Controla o comportamento da fila quando há acúmulo de frames.

Valores:
- `true`
- `false`

**Impacto:**
- `true`: descarta frames antigos para reduzir atraso
- `false`: mantém mais frames, podendo aumentar latência

**Recomendação:**
- rede instável ou lenta: `true`
- rede local ou estável: `false`

---

## Perfis de exemplo

### Rede local
```properties
connection.profile.LAN=LAN,Rede local,1920,1080,30,6000,30,ultrafast,zerolatency,false
```

### Wi-Fi
```properties
connection.profile.WIFI=WIFI,Wi-Fi,1280,720,20,2500,30,veryfast,zerolatency,true
```

### Satélite 1
```properties
connection.profile.SATELLITE_1=SATELLITE_1,Satélite 1,1024,576,10,1000,20,veryfast,zerolatency,true
```

### Satélite 2
```properties
connection.profile.SATELLITE_2=SATELLITE_2,Satélite 2,1280,720,12,1400,24,veryfast,zerolatency,true
```

### Satélite 3
```properties
connection.profile.SATELLITE_3=SATELLITE_3,Satélite 3,1280,720,15,1800,30,veryfast,zerolatency,true
```

---

## Regras importantes

- o perfil ativo definido em `connection.profile` deve existir
- todos os campos devem ser preenchidos
- os campos numéricos devem ser positivos
- `leakyQueue` deve ser `true` ou `false`

---

## Recomendações rápidas

### Menor atraso
Use algo próximo de:

```properties
1280,720,12~20,1400~2500,20~30,veryfast,zerolatency,true
```

### Melhor imagem
Use algo próximo de:

```properties
1920,1080,30,6000+,30,ultrafast ou fast,zerolatency,false
```

### Rede muito limitada
Use algo próximo de:

```properties
1024,576,10,800~1200,20,veryfast,zerolatency,true
```
