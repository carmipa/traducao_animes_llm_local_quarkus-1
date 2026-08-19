# 🔤 Módulo: Troca Tipo Legenda (Fontes Legadas)

[← Revisão de Lore](etapa-3.2-revisao-lore.md) | [Remuxer →](etapa-5.1-remuxer.md)

---

## Para que serve

Painel **"9. Troca Tipo Legenda"** da SPA (grupo **Qualidade**). Audita arquivos `.ass`/`.ssa` em busca de **fontes legadas de 8 bits** nos estilos (`Fontname` em `[V4+ Styles]`) e as substitui em lote por fontes Unicode seguras — com backup automático antes de gravar. Mesmo quando a auditoria automática não encontra defeito obrigatório, o operador pode escolher **normalizar manualmente os `Fontname` para Arial** por legibilidade em TV; essa ação não achata estilos, não remove efeitos e não altera textos/tempos.

![Painel de Troca Tipo Legenda](../src/main/resources/static/img/screenshots/troca-tipo-legenda.png)

### O problema que motivou o módulo

Fansubs antigos usam fontes com codificações pré-Unicode — em especial as vietnamitas **TCVN3/VNI** (prefixo `.Vn`, ex.: `.VnBook-Antiqua`) — que colocam **glifos vietnamitas nas posições dos caracteres acentuados do Latin-1**. O texto no arquivo fica correto, mas qualquer player que honre fontes embutidas (VLC, mpv/libass) renderiza `"Não, não é"` como `"Nóo, nóo ộ"`. Legendas em inglês (ASCII puro) nunca revelam o defeito — ele só aparece quando o pipeline traduz para PT-BR e os acentos entram em cena.

> 💡 Caso real: a temporada de *Gundam 08th MS Team* (release [Joseki]) usava `.VnBook-Antiqua` no estilo `Dialogue`, com o `Vnantiqb.ttf` embutido nos MKVs. O defeito é 100% de **renderização** — o `.ass` está íntegro — e por isso não aparece em nenhum grep/diff do texto.

---

## Pacote e classes principais

| Classe | Papel |
|--------|-------|
| `TrocaTipoLegendaUseCase` (`application`) | Orquestra o lote: escaneia, cria backup, grava substituições, persiste relatório e telemetria |
| `AuditoriaFontesService` (`application`) | Detecta fontes legadas/problemáticas nos estilos e sugere a substituta Unicode |
| `TrocaTipoLegendaAuditoriaCache` (`infrastructure`) | Cache/auditoria append-only de cada substituição realizada |
| `AuditoriaFonteInfo`, `AuditoriaLegendaResultado`, `ResultadoGeralAuditoria`, `ResultadoTrocaFonte` (`domain`) | Records imutáveis dos resultados de auditoria e da aplicação |
| `TrocaTipoLegendaController` (`presentation`) | Endpoints REST — escanear (síncrono via fila) e aplicar (assíncrono via fila) |

---

## Fluxo de execução

```mermaid
sequenceDiagram
    actor Op as Operador
    participant UI as Painel Troca Tipo Legenda
    participant API as TrocaTipoLegendaController
    participant Fila as FilaExecucaoPipeline
    participant UC as TrocaTipoLegendaUseCase
    participant Aud as AuditoriaFontesService

    Op->>UI: Pasta com as legendas .ass/.ssa
    UI->>API: POST /api/troca-legenda/escanear
    API->>Fila: executarEAguardar(escanear)
    Fila->>UC: escanear(diretorio)
    UC->>Aud: auditar estilos de cada arquivo
    Aud-->>UC: fontes legadas + sugestões Unicode
    UC-->>UI: relatório de auditoria (JSON síncrono)

    Op->>UI: Confirma "Aplicar Substituições"
    UI->>API: POST /api/troca-legenda/aplicar
    API->>Fila: submeter(aplicar) — assíncrono
    Fila->>UC: aplicar(diretorio)
    UC->>UC: backup da pasta → substitui Fontname → grava
    UC-->>UI: progresso via SSE (canal troca-tipo-legenda)
```

- **Escanear** roda **síncrono dentro da fila** (`executarEAguardar`): garante que nenhum job pesado roda em paralelo e devolve o relatório na própria resposta HTTP. Se a fila estiver ocupada com um job longo, a requisição espera.
- **Aplicar** roda **assíncrono na fila** (`submeter`): cria o backup, grava os arquivos, loga cada substituição no console via SSE e registra cada troca no cache de auditoria.
- A execução respeita **parada cooperativa** (interrupção via encerramento/`/api/pipeline/parar`) — arquivos já gravados são preservados.

---

## O estilo base é o de MAIOR TEMPO DE TELA — nunca o mais numeroso

O achatamento joga os estilos decorativos no estilo de diálogo principal. Escolher **qual** é o
principal decidia tudo — e o critério antigo (*"o estilo mais frequente"*) elegia a **decoração**.

```
Zeta Gundam — o logo de abertura e animado quadro a quadro:
  297 eventos do estilo "Zeta Episode Title", de 0,04 s cada  =  6 s de tela

episodios 1, 8 e 14:  297 quadros  >  205 / 291 / 275 falas de dialogo
  -> a decoracao ganhava a votacao e o achatamento rodava AO CONTRARIO:
     o dialogo inteiro ia para o estilo do letreiro (corpo 100, contorno 0,
     sombra 0, cinza claro, margem 10) e ficava ilegivel sobre cena clara

episodio 2:  298 falas contra 297 quadros  ->  UMA linha de diferenca
  -> nos outros 47 episodios o defeito nao apareceu POR UM FIO
```

Hoje a eleição prefere `Default` quando ele existe no cabeçalho; na ausência dele, vence o estilo
com **maior tempo de tela** entre as falas `Dialogue` com fonte declarada. Por tempo, a separação é
de **duas ordens de grandeza** (6 s de logo contra ~10 min de diálogo) e não depende de sorte.

> Contagem de eventos ainda decide, mas só como **último recurso** — quando nenhuma duração pôde
> ser lida (legenda sem colunas `Start`/`End`, ou com tempos ilegíveis). Critério que só funciona
> por margem de uma linha não é critério: é sorte com aparência de regra.

**O achatamento em si é feature, não defeito** — quem confundiu os dois fui eu, ao ler o resultado
antes do código. O bug era só o voto por contagem.

---

## Endpoints REST

| Endpoint | Payload | Canal SSE |
|----------|---------|-----------|
| `POST /api/troca-legenda/escanear` | `{diretorioLegendas}` | — (resposta síncrona) |
| `POST /api/troca-legenda/aplicar` | `{diretorioLegendas, forcarArial?}` | `troca-tipo-legenda` |

```json
{ "diretorioLegendas": "C:/animes/[Joseki] Gundam 08th MS Team/traducao-ptbr" }
```

Para decisão manual de legibilidade:

```json
{ "diretorioLegendas": "C:/animes/Mobile Suit Zeta Gundam/traducao_ptbr", "forcarArial": true }
```

`diretorioLegendas` é **obrigatório** (`400` se ausente).

---

## Pontos de atenção

- A troca altera apenas o cabeçalho `[V4+ Styles]`; tags `\fn` inline nos eventos (raras) precisam de conferência manual — o relatório de auditoria as aponta.
- Depois da troca, os MKVs finais precisam ser **re-remuxados** ([Remuxer](etapa-5.1-remuxer.md)) para embutir a legenda corrigida.
- Fontes anexadas no MKV original que nenhum estilo referencia mais (ex.: o `Vnantiqb.ttf` órfão) são inofensivas, mas continuam dentro do vídeo até um remux que as descarte.
- Regra prática ao iniciar **qualquer série nova**: rodar o escaneamento na pasta das legendas extraídas antes de traduzir — fontes `.Vn*`, `VNI-*` e similares quebram a acentuação PT-BR silenciosamente.
- A normalização manual para Arial mexe somente no campo `Fontname` dos estilos. O **Achatador de Estilos Decorativos** continua sendo uma aba independente na mesma página e não é acionado por essa troca de fonte.

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Revisão de Lore](etapa-3.2-revisao-lore.md) | [Remuxer →](etapa-5.1-remuxer.md) |
