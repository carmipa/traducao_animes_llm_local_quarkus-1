# 📋 API REST — Referência Completa

[← Mapa do Projeto](12-modulo-mapa-projeto.md) | [Configuração →](14-configuracao.md)

---

## Convenções

- **Base URL:** `http://127.0.0.1:8080`
- **Content-Type:** `application/json` em todas as requisições e respostas
- A maioria das operações do pipeline (`/analisar`, `/extrair`, `/traduzir`, `/corrigir-*`, `/revisar-*`, `/remuxar`) é **assíncrona**: o endpoint responde `200 OK` imediatamente com uma mensagem de confirmação, e o progresso/relatório real chega via **[SSE](#sse--logsstream)** no canal correspondente.
- Erros de validação de entrada retornam `400 Bad Request` com `{"mensagem": "..."}`.

---

## Pipeline — Operações Principais

### `GET /api/status`
Health check simples.

**Resposta:** `200 OK` `{"mensagem": "online"}`

---

### `POST /api/analisar`
Auditoria técnica de mídia. Ver [Análise de Mídia](03-modulo-analise-midia.md).

```json
{ "entrada": "C:/animes/DanMachi/Season 04", "saida": null }
```
**Canal SSE:** `analise` (progresso) + evento dedicado `analise-relatorio` com o **JSON do `ResultadoAnaliseLote`** (resultado estruturado, **não** gravado em disco). A exportação TXT é manual no navegador; só a telemetria técnica é persistida (em `logs/`).

---

### `POST /api/extrair`
Extração de faixas de legenda. Ver [Extração de Legendas](04-modulo-extracao-legendas.md).

```json
{ "entrada": "C:/animes/DanMachi/Season 04", "saida": "C:/.../legendas_extraidas", "formato": "ASS" }
```
`formato`: `ASS` | `SRT` | `PGS` (obrigatório — `400` se ausente/inválido). **Canal SSE:** `extracao`

---

### `POST /api/traduzir`
Tradução via LLM local com cache. Ver [Tradução Local](05-modulo-traducao-llm.md).

```json
{ "entrada": "C:/.../legendas_extraidas", "saida": "C:/.../legendas-ptbr", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `traducao`

---

### `POST /api/corrigir-cache`
Limpa entradas de fallback do cache (força retradução). Ver [Correção & Revisão](06-modulo-correcao-revisao.md#fluxo-1--limpeza-de-cache-traducaocorrige).

```json
{ "entrada": "cache", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `correcao`

---

### `POST /api/corrigir-scraping`
Correção de cache via Google Translate. Ver [Correção & Revisão](06-modulo-correcao-revisao.md#fluxo-2--correçãorevisão-via-google-translate-raspagemcorrecao).

```json
{ "entrada": "cache" }
```
**Canal SSE:** `correcao`

---

### `POST /api/revisar-cache`
Revisão de concordância PT-BR do cache via LLM. Ver [Correção & Revisão](06-modulo-correcao-revisao.md#fluxo-3--revisão-de-concordância-pt-br-via-llm-raspagemrevisao).

```json
{ "entrada": "cache", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `correcao`

---

### `POST /api/revisar-legendas`
Revisão de legendas `.ass` finais via Google Translate (modo `GOOGLE`).

```json
{ "entradaPt": "C:/.../legendas-ptbr", "entradaEn": "C:/.../legendas_extraidas", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `revisao`

---

### `POST /api/revisar-legendas-concordancia`
Revisão de legendas `.ass` finais via LLM local (modo `LLM_CONCORDANCIA`).

```json
{ "entradaPt": "C:/.../legendas-ptbr", "entradaEn": "C:/.../legendas_extraidas", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `revisao`

---

### `POST /api/revisar-concordancia`
Correção **determinística** de concordância de gênero (painel 8), direto na pasta PT-BR — sem LLM e sem o original. `aplicar: false` = dry-run (simula, não grava). Ver [Correção & Revisão](06-modulo-correcao-revisao.md#fluxo-4--concordância-de-gênero-determinística-revisaoconcordancia-painel-8).

```json
{ "diretorioTraduzido": "C:/.../legendas-ptbr", "aplicar": false }
```
**Canal SSE:** `revisao-concordancia`

---

### `POST /api/correcao-legendas`
Correção estrutural de legendas PT-BR usando a original como referência. Ver [Correção de Legendas](07-modulo-cura-tags.md).

### `POST /api/cura-tags`
Alias legado de compatibilidade para `/api/correcao-legendas`.

```json
{ "diretorioOriginal": "C:/.../legendas_extraidas", "diretorioTraduzido": "C:/.../legendas-ptbr", "contextoId": "danmachi-s4" }
```
**Canal SSE:** `cura`

---

### `POST /api/revisar-lore`
Corrige nomes, locais e termos de lore em legendas `.ass` já traduzidas, comparando com o original em inglês. Ver [Revisão de Lore](16-modulo-revisao-lore.md).

```json
{ "diretorioOriginal": "E:/.../legendas_eng", "diretorioTraduzido": "E:/.../traducao_ptbr", "contextoId": "gundam_0083", "revisarTodasFalas": false }
```
`contextoId` é **obrigatório** (`400` se ausente/desconhecido — usa o sistema de contextos próprio deste módulo, não o `/api/contextos` geral). **Canal SSE:** `revisao-lore`

---

### `POST /api/troca-legenda/escanear`
Audita os `.ass`/`.ssa` de uma pasta em busca de fontes legadas (TCVN3/VNI etc.) nos estilos. **Síncrono** (roda dentro da fila do pipeline e devolve o relatório na resposta). Ver [Troca Tipo Legenda](18-modulo-troca-tipo-legenda.md).

```json
{ "diretorioLegendas": "C:/animes/Serie/traducao-ptbr" }
```

### `POST /api/troca-legenda/aplicar`
Aplica em lote as substituições de fontes sugeridas, com backup automático. Assíncrono. **Canal SSE:** `troca-tipo-legenda`

---

### `POST /api/auditoria-conteudo`
Audita pares original ↔ traduzido com as 5 regras de anomalia (karaokê danificado, efeito vazado, quebra de linha alucinada, metadados, sincronia de estilos). **Síncrono** — devolve o `RelatorioAuditoriaConteudo` na resposta. Ver [Análise de Conteúdo](20-modulo-analise-conteudo.md).

```json
{ "caminhoOriginal": "C:/animes/Serie/legendas_originais", "caminhoTraduzido": "C:/animes/Serie/legendas-ptbr" }
```
**Canal SSE:** `auditor-conteudo`

---

### `POST /api/novo-karaoke/simular`
### `POST /api/novo-karaoke/aplicar`
Converte karaokê KFX (milhares de eventos por sílaba/frame) em legendas simples — uma linha limpa por frase, no tempo original. Simulação não grava nada. Ver [Karaokê Simples](21-modulo-karaoke-simples.md).

```json
{ "caminhoOrigem": "C:/animes/86/legendas-ptbr", "caminhoDestino": "C:/animes/86/legendas-karaoke-simples" }
```
**Canal SSE:** `novo-karaoke`

---

### `POST /api/traducao-karaoke/simular`
### `POST /api/traducao-karaoke/aplicar`
Traduz as letras de música preservando o romaji/japonês original: a simulação classifica linha a linha **sem LLM**; a aplicação entra na fila do pipeline e traduz via LLM apenas a camada em inglês, com cache editável em `cache/karaoke/`. Ver [Tradução de Karaokê](22-modulo-traducao-karaoke.md).

```json
{ "caminhoOrigem": "C:/animes/86/legendas-karaoke-simples", "contextoId": "eight_six" }
```
**Canal SSE:** `traducao-karaoke` — saída gravada na pasta irmã `<entrada>-karaoke-ptbr`

---

### `POST /api/renomear-arquivos/simular`
### `POST /api/renomear-arquivos/aplicar`
### `POST /api/renomear-arquivos/reverter`
Renomeação em lote de arquivos para o padrão `Nome - S01E01` (dry-run, aplicação com manifesto de undo e reversão). Ver [Renomear Arquivos](19-modulo-renomear-arquivos.md).

```json
{ "caminhoOrigem": "C:/animes/[SubsPlease] Nome Anime", "nomePadrao": "Nome Anime" }
```
**Canal SSE:** `renomear-arquivos` (`reverter` dispensa `nomePadrao`)

---

### `POST /api/remuxar`
Combina vídeo + legenda em MKV final. Ver [Remuxer](08-modulo-remuxer.md).

```json
{ "entrada": "C:/animes/Gundam-Narrative-NT", "saida": "C:/.../saida-mkv", "syncOffsetMs": 0, "preservarLegendasOriginais": true }
```
**Canal SSE:** `remuxer`

---

### `POST /api/pipeline/parar`
Solicita a **parada cooperativa** do trabalho em execução na fila única do pipeline: o job encerra no próximo ponto seguro e o progresso já salvo (cache, arquivos concluídos) é preservado. Os botões "Parar Execução" foram **removidos da UI em 2026-07-09** (a parada cooperativa não interrompia o job de forma perceptível); o endpoint permanece disponível via API e é usado internamente pelo fluxo de encerramento (`/api/sistema/sair`).

Sem payload. **Resposta:** `{"mensagem": "..."}`

---

### `POST /api/sistema/sair`
Encerra a aplicação de forma graciosa (menu "Sair" da UI): o trabalho em execução para no próximo ponto seguro e o servidor é desligado.

Sem payload. **Resposta:** `{"mensagem": "Encerrando a aplicação. ..."}`

---

### `POST /api/mapa`
Regenera `mapa_projeto.md`. Ver [Mapa do Projeto](12-modulo-mapa-projeto.md).

Sem payload.

---

## Dados de Apoio

### `GET /api/contextos`
Lista os contextos/lore disponíveis. Ver [Contextos & Lore](09-contextos-lore.md).

```json
[{ "id": "danmachi", "nome": "DanMachi (Geral)", "padrao": true }]
```

### `GET /api/revisao-lore/contextos`
Lista os contextos específicos do módulo de [Revisão de Lore](16-modulo-revisao-lore.md) — sistema separado do `/api/contextos` acima, com um subconjunto menor de obras calibradas.

```json
[{ "id": "gundam_0083", "nome": "Gundam 0083 - Revisao de Lore" }]
```

### `GET /api/metadata?caminho=<pasta_ou_nome>`
Metadados de anime (Jikan/TMDB). Ver [Metadados de Anime](11-modulo-metadados-anime.md).

### `GET /api/telemetria`
Resumo consolidado de telemetria + métricas de JVM. Ver [Telemetria](10-modulo-telemetria.md).

### `GET /api/telemetria/exportar`
Download do `logs/telemetria_compartilhada.json` bruto como `kronos_telemetria_segura.json`.

### `POST /api/telemetria/publicar-dataset`
Publica a telemetria **sanitizada** (só métricas — sem textos de legenda nem caminhos) como dataset público no repositório dedicado `kronos-anime-translation-telemetry-dataset`: snapshot em `metrics/`, commit e push. Ver [Telemetria](10-modulo-telemetria.md).

Sem payload. **Resposta:** `{ "repositorio", "commit", "pushOk", "mensagem" }`

---

## Diálogo Nativo (somente Windows)

### `GET /api/dialogo/selecionar-pasta`
Abre o seletor nativo de pasta do Windows (PowerShell + `System.Windows.Forms.OpenFileDialog` simulando `FolderBrowserDialog`). Timeout de 3 minutos.

```json
{ "caminho": "C:\\animes\\DanMachi\\Season 04" }
```

### `GET /api/dialogo/selecionar-arquivo?filtro=...`
Idem, para seleção de arquivo único.

---

## SSE — `/api/logs/stream`

Conexão `EventSource` única para **todos** os logs em tempo real. Cada operação em background publica sob um **canal nomeado** (ver `LogStreamService#definirCanalAtual`, escopado por `ThreadLocal` — operações concorrentes não se misturam):

| Canal | Origem |
|-------|--------|
| `analise` | `/api/analisar` |
| `extracao` | `/api/extrair` |
| `traducao` | `/api/traduzir` |
| `correcao` | `/api/corrigir-*`, `/api/revisar-cache` |
| `revisao` | `/api/revisar-legendas*` |
| `revisao-concordancia` | `/api/revisar-concordancia` |
| `cura` | `/api/cura-tags` |
| `revisao-lore` | `/api/revisar-lore` |
| `troca-tipo-legenda` | `/api/troca-legenda/aplicar` |
| `auditor-conteudo` | `/api/auditoria-conteudo` |
| `novo-karaoke` | `/api/novo-karaoke/*` |
| `traducao-karaoke` | `/api/traducao-karaoke/*` |
| `renomear-arquivos` | `/api/renomear-arquivos/*` |
| `remuxer` | `/api/remuxar` |
| `console` | Fallback genérico — roteado para a aba ativa no navegador |
| `sistema` | Mensagens de conexão/sistema |
| `analise-relatorio` | Evento único com o JSON do `ResultadoAnaliseLote` (resultado estruturado; não salvo em disco — export TXT é manual) |

```javascript
const es = new EventSource('/api/logs/stream');
es.addEventListener('traducao', (e) => console.log(e.data));
```

> 📋 **Relatório final padronizado**: toda operação, em qualquer canal, encerra com a linha
> `[RELATÓRIO FINAL] Operação: <nome> | Tempo total: <duração> | Término: <hh:mm:ss>` (hora local) —
> tanto em caso de sucesso quanto de falha.

Cada painel da SPA renderiza seu canal nesta mesma caixa "Logs do..." (exemplo abaixo: canal `analise`, painel Análise de Mídia):

![Console de logs em tempo real (SSE) — mesmo componente em todos os painéis](../src/main/resources/static/img/screenshots/analise-midia.webp)

---

## SSE — `/api/telemetria/stream`

JAX-RS puro (não Spring — evita colisão de rota). Publica o `TelemetriaResumo` serializado a cada 1 segundo.

---

## Exemplo com cURL

```bash
# Análise de mídia
curl -X POST http://127.0.0.1:8080/api/analisar \
  -H "Content-Type: application/json" \
  -d '{"entrada": "C:/animes/DanMachi/Season 04"}'

# Tradução
curl -X POST http://127.0.0.1:8080/api/traduzir \
  -H "Content-Type: application/json" \
  -d '{"entrada": "C:/.../legendas_extraidas", "contextoId": "danmachi"}'

# Acompanhar logs em tempo real
curl -N http://127.0.0.1:8080/api/logs/stream

# Telemetria
curl http://127.0.0.1:8080/api/telemetria
```

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Mapa do Projeto](12-modulo-mapa-projeto.md) | [Configuração →](14-configuracao.md) |
