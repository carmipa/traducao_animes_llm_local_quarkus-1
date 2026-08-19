# 🎤 Módulo: Tradução de Karaokê (Romaji + PT-BR juntos)

[← Karaokê Simples](etapa-4.3-karaoke-simples.md) | [Correção de Karaoke →](etapa-4.2-cura-tags.md)

---

## Para que serve

Painel **"11. Tradução de Karaokê"** da SPA (grupo **Karaokê**). Traduz as **letras de música** das legendas mantendo o japonês original junto na tela: a camada **romaji/japonesa é preservada intacta** e apenas a camada de **tradução em inglês** vai ao LLM, virando PT-BR nos mesmos tempos — resultado: romaji em cima, PT-BR embaixo, como fansub clássico.

O problema central que o módulo resolve: **cantores japoneses misturam inglês no meio da letra** (*"kimi no heart ni fly away"*). Uma detecção ingênua de "linha em inglês" mandaria a letra original para o LLM e a destruiria. Aqui a classificação é **por evidência**, com viés de preservação.

![Painel de Tradução de Karaokê](../src/main/resources/static/img/screenshots/traducao-karaoke.png)

---

## Pacote e classes principais

| Classe | Papel |
|--------|-------|
| `ClassificadorLetraKaraokeService` (`application`) | O coração do módulo: decide por linha se é letra original (preservar), tradução em inglês (traduzir), já PT-BR ou efeito KFX |
| `TraduzirKaraokeUseCase` (`application`) | Orquestra: classifica, consulta cache, chama o LLM linha a linha, grava a saída |
| `ClasseLinhaKaraoke` (`domain`) | Enum das 5 classes de linha |
| `ResultadoTraducaoKaraoke` (`domain`) | Métricas por arquivo (preservadas, traduzidas, cache, sem tradução) |
| `TraducaoKaraokePersistencia` (`infrastructure`) | Manifesto de auditoria em `logs/traducao-karaoke/manifestos/` |
| `TraducaoKaraokeController` (`presentation`) | Endpoints REST — simulação assíncrona; aplicação via fila do pipeline |

---

## Como uma linha de música é classificada

```mermaid
flowchart TB
    A["🎼 Evento de música<br/>(estilo Song/OP/ED ou tags de karaokê)"] --> B{"Efeito KFX?<br/>(sílaba/frame, alta densidade de tags)"}
    B -->|Sim| KFX["🛡️ EFEITO_KFX<br/>preservado intacto"]
    B -->|Não| C{"Kana/Kanji no texto?"}
    C -->|Sim| JP["🇯🇵 ORIGINAL_JAPONES<br/>nunca traduz"]
    C -->|Não| D{"Nome do estilo?"}
    D -->|"Romaji / JP / ROM"| JP
    D -->|"English / EN"| E{"Já está em PT-BR?"}
    D -->|ambíguo| F["⚖️ Votação por evidência"]
    E -->|Sim| PT["🇧🇷 JA_PORTUGUES<br/>nada a fazer"]
    E -->|Não| EN["🔁 TRADUZIVEL_INGLES<br/>vai ao LLM"]
    F --> G{"Partículas japonesas<br/>(wa, ga, ni, kimi, boku...)<br/>vs gramática inglesa<br/>(the, you, of, with...)"}
    G -->|"romaji vence<br/>('kimi no heart ni fly away')"| JP
    G -->|"inglês vence<br/>('You are the light of my world')"| EN
    G -->|"empate: fração silábica Hepburn<br/>('One more time, one more chance')"| JP

    classDef entrada fill:#831843,stroke:#F472B6,color:#F9FAFB
    classDef decisao fill:#312e81,stroke:#818CF8,color:#F9FAFB
    classDef preserva fill:#14532d,stroke:#4ADE80,color:#F9FAFB
    classDef traduz fill:#0c4a6e,stroke:#38BDF8,color:#F9FAFB
    class A entrada
    class B,C,D,E,F,G decisao
    class KFX,JP,PT preserva
    class EN traduz
```

> ⚖️ **Na dúvida, o viés é PRESERVAR** — deixar uma linha de música sem traduzir custa menos que destruir a letra original. O **dry-run mostra a classificação linha a linha** antes de gastar qualquer chamada de LLM.

---

## Fluxo de execução

```mermaid
sequenceDiagram
    actor Op as Operador
    participant UI as Painel Tradução de Karaokê
    participant API as TraducaoKaraokeController
    participant Fila as FilaExecucaoPipeline
    participant UC as TraduzirKaraokeUseCase
    participant LLM as LlmClientAdapter

    Op->>UI: Obra (lore) + pasta das legendas
    UI->>API: POST /api/traducao-karaoke/simular
    API->>UC: simular (assíncrono, sem LLM)
    UC-->>UI: classificação linha a linha via SSE

    Op->>UI: Confere e clica "Traduzir Letras"
    UI->>API: POST /api/traducao-karaoke/aplicar
    API->>Fila: submete o job de LLM
    Fila->>UC: aplicar(pasta, contextoId)
    UC->>UC: congela a lore escolhida em snapshot imutável
    loop Cada linha TRADUZIVEL_INGLES sem cache
        UC->>LLM: traduzir (1 linha, tags mascaradas)
        LLM-->>UC: PT-BR (validado contra alucinação)
    end
    UC-->>UI: [RELATÓRIO FINAL] com contagens e tempo total
```

---

## Garantias de segurança

- **Entrada intocada**: a saída vai para a pasta irmã `<entrada>-karaoke-ptbr`, com o **mesmo nome de arquivo** (o pareamento do [Remuxer](etapa-5.1-remuxer.md) continua funcionando).
- **Cache editável por arquivo** em `cache/karaoke/*.cache.json` — mesmo fluxo de correção manual da [Tradução Local](etapa-2.1-traducao-llm.md); refrão repetido gasta **uma** chamada de LLM.
- Falha ou alucinação do LLM numa linha **mantém a linha original** com aviso — nunca derruba o arquivo.
- Diálogos, placas e efeitos KFX passam **byte a byte** — o módulo só toca música.
- A aplicação roda **na fila única do pipeline**, mas não relê a lore global: o `contextoId` escolhido vira um snapshot imutável e seu prompt é passado explicitamente em todas as chamadas ao LLM.
- Cache, manifesto e telemetria registram o ID e a proveniência do contexto usado; cache legado sem carimbo não é reaproveitado automaticamente.

---

## O que mudou entre 07 e 15/08/2026 — quatro consertos, todos medidos

### 1. O gradiente de cor sobrevive à tradução (`GradienteKaraoke`)

Linha pintada **letra a letra** — o gradiente que os fansubs aplicam no OP/ED — tem **uma tag por
letra**. O mascarador produzia 10 a 30 marcadores intercalados, e nenhum LLM os devolve na ordem.

```
Guilty Crown, 07/08/2026: das 31 linhas recusadas numa execucao,
28 cairam por "Marcadores de formatacao ([[TAGn]])"

a unica que passou pelo validador saiu assim:
  So, everything that makes me whole
  -> So, eu e evidentementereithyingthathingthatmakes mea whole wholed
```

Agora **só o texto vai ao LLM** e as **mesmas cores voltam distribuídas** sobre a tradução. Nenhuma
tag viaja.

### 2. A tradução vinha certa e era jogada fora por falta de um marcador

O veto de `\t(` era aplicado ao **texto inteiro**, não ao trecho que importava — e derrubava
proposta correta. Medido numa execução do **86**: **2.987 recusas**, que caíram para **4** depois
do conserto.

### 3. O corretor ortográfico saiu da ORIGEM e foi para a SAÍDA

Ele estava antes do cache; passou para depois, que é por onde a legenda realmente sai. Nasce com
**preservação como padrão** — na dúvida, não mexe.

### 4. O acento tem lista PRÓPRIA, e a razão é o romaji

`AcentosLetraKaraoke` **não** reusa a lista de 162 entradas do diálogo (`NormalizadorAcentosComuns`).
Medido em 14/08/2026 contra o dicionário `ja_ROMAJI` do próprio projeto (129.745 formas): **quatro
daquelas entradas também são romaji válido** —

```
ate    mae    nao    sao
```

No diálogo elas nunca fizeram mal, porque ali não existe camada japonesa. Aqui, `mae` é 前
("antes") e virou **`mãe` 100 vezes** nos 50 episódios do Unicorn.

> **É duplicação declarada, e a medição é o motivo.** A regra que Paulo enunciou em 14/08: a
> camada resolve o problema **dela** e não empurra o próprio dano para as vizinhas. A lista daqui
> é montada do que foi medido **na saída do karaokê** — na tradução do 86, a camada portuguesa
> saiu com `nao` sem acento em 5 falas distintas, presentes em 918 linhas do arquivo (o gradiente
> de `\clip` repete a mesma fala centenas de vezes).

---

## Endpoints REST

| Endpoint | Payload | Canal SSE |
|----------|---------|-----------|
| `POST /api/traducao-karaoke/simular` | `{caminhoOrigem, contextoId}` | `traducao-karaoke` |
| `POST /api/traducao-karaoke/aplicar` | `{caminhoOrigem, contextoId}` | `traducao-karaoke` |

```json
{ "caminhoOrigem": "C:/animes/86/legendas-karaoke-simples", "contextoId": "eight_six" }
```

---

## Pontos de atenção

- O lugar natural do módulo é **depois do [Karaokê Simples](etapa-4.3-karaoke-simples.md)** (converte o KFX primeiro, traduz a letra depois) e **antes da [Correção de Karaoke](etapa-4.2-cura-tags.md)**.
- Estilos rotulados decidem primeiro (`OP - Romaji` preserva, `OP - English` traduz) — a votação por evidência só entra em estilos ambíguos (`Song`, `Insert`).
- Letra 100% em inglês **cantada no original** (ex.: *"One more time, one more chance"*) é preservada pelo desempate silábico — se ela estiver na camada de tradução com estilo rotulado `English`, é traduzida normalmente.

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Karaokê Simples](etapa-4.3-karaoke-simples.md) | [Correção de Karaoke →](etapa-4.2-cura-tags.md) |
