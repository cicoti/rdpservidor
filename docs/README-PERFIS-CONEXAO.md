# Perfis de conexão

Os perfis de conexão definem como o servidor transmite a imagem da área de trabalho para o cliente.

A aplicação trabalha com dois tipos de perfil:

- **Perfis protegidos do sistema**: `LAN` e `WIFI`
- **Perfis customizados**: criados e mantidos pelo usuário

---

## Estrutura geral

O perfil ativo é definido por:

```properties
connection.profile=LAN
```

Os perfis customizados gravados no arquivo são definidos assim:

```properties
connection.profile.<ID>=id,displayName,width,height,fps,bitrateKbps,keyIntMax,encoderPreset,encoderTune,leakyQueue
```

Exemplo de perfil customizado:

```properties
connection.profile.SATELLITE_1=SATELLITE_1,Satélite 1,1024,576,10,1000,20,veryfast,zerolatency,true
```

---

## Perfis protegidos do sistema

A aplicação sempre possui dois perfis protegidos:

- `LAN`
- `WIFI`

### Regras desses perfis

- são definidos no código
- sempre existem
- não são lidos do arquivo de propriedades
- não são gravados no arquivo de propriedades
- não podem ser excluídos
- não podem ser sobrescritos por perfis customizados

### Observação importante

O valor de `connection.profile` pode apontar para um perfil protegido do sistema ou para um perfil customizado.

Exemplos válidos:

```properties
connection.profile=LAN
```

```properties
connection.profile=WIFI
```

```properties
connection.profile=SATELLITE_1
```

---

## Perfis customizados

Perfis customizados são criados pela interface da aplicação.

### Regras dos perfis customizados

- podem ser criados a partir da duplicação de um perfil existente
- podem ser editados
- podem ser excluídos
- são gravados no arquivo `remote-desktop-server.properties`
- o ID é gerado automaticamente a partir do nome informado na interface

### Regras do ID

- o usuário informa apenas o nome do perfil
- o sistema gera o ID automaticamente
- o ID é convertido para um formato interno sem acentos e em maiúsculas
- os IDs `LAN` e `WIFI` são reservados e não podem ser usados por perfis customizados
- se o ID gerado já existir, o sistema cria um sufixo incremental automaticamente

Exemplos:

- `Rede Escritório` → `REDE_ESCRITORIO`
- `Wi Fi Filial 1` → `WI_FI_FILIAL_1`
- `Satélite 1` → `SATELITE_1`

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

**Impacto:**
- quanto maior, melhor definição horizontal
- quanto maior, maior consumo de banda
- quanto maior, maior uso de CPU para captura e codificação

---

### 4. `height`
Altura da imagem transmitida.

**Impacto:**
- quanto maior, melhor definição vertical
- quanto maior, maior consumo de banda
- quanto maior, maior uso de CPU

---

### 5. `fps`
Frames por segundo.

Exemplos comuns:
- `10`
- `12`
- `15`
- `20`
- `24`
- `30`

**Impacto:**
- quanto maior, mais fluidez
- quanto maior, maior uso de banda
- quanto maior, maior uso de CPU
- valores baixos deixam a navegação mais travada, mas economizam rede

---

### 6. `bitrateKbps`
Taxa de bits do vídeo em kbps.

Exemplos comuns:
- `600`
- `800`
- `1000`
- `1200`
- `1400`
- `1800`
- `2500`
- `3000`
- `4000`
- `6000`

**Impacto:**
- bitrate baixo reduz consumo de banda, mas piora a qualidade visual
- bitrate alto melhora a qualidade, mas aumenta o consumo de banda

---

### 7. `keyIntMax`
Intervalo máximo entre quadros-chave do encoder.

Exemplos comuns:
- `10`
- `15`
- `20`
- `24`
- `30`
- `60`

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

## Perfis padrão do sistema

### LAN
Perfil protegido para rede cabeada local.

Configuração de referência:

```properties
LAN,Rede local,1920,1080,30,6000,30,ultrafast,zerolatency,false
```

### WIFI
Perfil protegido para uso em rede Wi-Fi.

Configuração de referência:

```properties
WIFI,Wi-Fi,1280,720,20,2500,30,veryfast,zerolatency,true
```

---

## Perfis customizados de exemplo

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

## Estrutura esperada do arquivo

O arquivo `remote-desktop-server.properties` deve conter apenas:

- `connection.profile`
- perfis customizados
- `control.port`
- `handshake.port`

Exemplo:

```properties
#formato:
#id,displayName,width,height,fps,bitrateKbps,keyIntMax,encoderPreset,encoderTune,leakyQueue
#
#Remote Desktop Server configuration
#Mon Apr 20 13:44:15 BRT 2026
connection.profile=LAN
connection.profile.SATELLITE_1=SATELLITE_1,Sat\u00E9lite 1,1024,576,10,1000,20,veryfast,zerolatency,true
control.port=5000
handshake.port=7000
```

### Observações

- `LAN` e `WIFI` não devem ser gravados no arquivo
- o arquivo armazena apenas perfis customizados
- ao salvar alterações, o arquivo deve ser regenerado por completo

---

## Regras importantes

- o perfil ativo definido em `connection.profile` deve existir
- o perfil ativo pode ser protegido do sistema ou customizado
- todos os campos do perfil devem ser preenchidos
- os campos numéricos devem ser positivos
- `leakyQueue` deve ser `true` ou `false`
- `LAN` e `WIFI` são IDs reservados
- qualquer definição de `connection.profile.LAN` ou `connection.profile.WIFI` no arquivo deve ser ignorada

---

## Uso pela interface

Na interface de configuração, o usuário pode:

- selecionar um perfil existente
- duplicar um perfil para criar um novo perfil customizado
- editar o nome e os parâmetros de um perfil customizado
- excluir perfis customizados
- salvar e ativar o perfil selecionado

### Comportamento dos perfis protegidos

Quando o perfil selecionado for `LAN` ou `WIFI`:

- os campos aparecem preenchidos
- os campos ficam bloqueados para edição
- o perfil não pode ser excluído
- o perfil pode ser duplicado
- ao salvar, ele apenas se torna o perfil ativo

### Comportamento dos perfis customizados

Quando o perfil selecionado for customizado:

- os campos ficam editáveis
- o perfil pode ser excluído
- ao salvar, o perfil é gravado no arquivo e se torna o perfil ativo

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
