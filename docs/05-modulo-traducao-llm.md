# 🌐 Módulo: Tradução Local (LLM)

[← Extração de Legendas](04-modulo-extracao-legendas.md) | [Correção & Revisão →](06-modulo-correcao-revisao.md)

---

## Para que serve

O núcleo do pipeline: traduz cada fala de uma legenda `.ass`/`.ssa` do inglês para PT-BR usando um **LLM rodando 100% localmente** via [LM Studio](https://lmstudio.ai/), com **cache persistente** (evita retraduzir falas já processadas) e **contexto/lore por anime** (nomes próprios, gênero de personagens, terminologia).

![Painel de Tradução Local](../src/main/resources/static/img/screenshots/traducao-local.webp)

---

## Pacote e classes principais

| Classe | Papel |
|--------|-------|
| `ProcessarArquivoUseCase` (fatia `traducao`, `application`) | Orquestra: lê o `.ass`, separa falas por lote, consulta o cache, envia pendências ao LLM, valida e escreve o `.ass` traduzido |
| `LlmClientAdapter` (`traducao/infrastructure/adapters`) | Implementa `LlmPort` (peer **`llm`**) — cliente HTTP OpenAI-compatible para o LM Studio; é o **ponto de composição** do peer `llm` |
| `CacheTraducaoService` / `EntradaCache` / `ProvenienciaCache` (peer **`cachetraducao`**) | **Dono único** do cache: persiste o par original↔traduzido por arquivo e carimba a **proveniência** (ver abaixo) |
| `GerenciadorContexto` / `ProvedorContexto` (peer **`contexto`**) | Sistema de lore por anime — ver [Contextos & Lore](09-contextos-lore.md) |
| `LeitorLegendaAss` / `EscritorLegendaAss` (peer **`legenda`**) | Parser/escritor do `.ass` — preserva timestamps e formatação **byte a byte**, só troca o campo `Text` |
| `MascaradorTags` / `ValidadorTraducaoService` / `DetectorTraducaoIdenticaService` (peer **`qualidadeTraducao`**) | Máscara de tags ASS, detecção de resíduo/alucinação e de tradução idêntica ao original (falha silenciosa) |

> As classes de cache, legenda, contexto, qualidade e LLM vivem em **fatias peer próprias** (`cachetraducao`, `legenda`, `contexto`, `qualidadeTraducao`, `llm`), importadas pela Tradução Local por uma fronteira **congelada por ArchUnit** — ver [Arquitetura](01-arquitetura.md#fatias-verticais-peers-e-fronteiras-congeladas-archunit).

---

## Por que o parser de `.ass` nunca interpreta timestamps

Uma decisão de design deliberada: `LeitorLegendaAss` separa cada linha `Dialogue:`/`Comment:` em **`prefixo`** (tudo até o penúltimo campo do `Format:` — inclui `Start`, `End`, `Style`, posicionamento) e **`texto`** (só o último campo, que vai para tradução). O parser **nunca decompõe `H:MM:SS.cc`** — o prefixo é tratado como string opaca e devolvido bit a bit idêntico na escrita. Isso garante que **a tradução nunca pode introduzir dessincronização de tempo** — se uma legenda sai dessincronizada depois de traduzida, a causa está em outro lugar (ex.: legenda de origem que já veio de um release diferente do vídeo — ver [Solução de Problemas](15-solucao-problemas.md)).

---

## Fluxo de execução (cache-aware)

```mermaid
sequenceDiagram
    actor Op as Operador
    participant API as ApiController
    participant UC as ProcessarArquivoUseCase
    participant Leitor as LeitorLegendaAss
    participant Cache as CacheTraducaoService
    participant Mask as MascaradorTags
    participant LLM as LlmClientAdapter
    participant Escritor as EscritorLegendaAss

    Op->>API: POST /api/traduzir {entrada, contextoId}
    API->>UC: processar(arquivo.ass)
    UC->>Leitor: ler(arquivo.ass)
    Leitor-->>UC: lista de EventoLegenda (prefixo + texto, timestamps intactos)
    UC->>Cache: carregar(arquivo.cache.json)
    Cache-->>UC: mapa original → traduzido já processados

    loop Para cada fala sem tradução em cache
        UC->>Mask: mascarar(texto) — {\pos(...)} → [[TAG0]]
        UC->>LLM: traduzir(lote, promptSistema=lore do contexto)
        LLM-->>UC: texto traduzido (ainda mascarado)
        UC->>Mask: desmascarar(texto) — [[TAG0]] → {\pos(...)}
        UC->>Cache: salvar par (original → traduzido)
    end

    UC->>Escritor: escrever(eventos com texto final, prefixo original intacto)
    Escritor-->>UC: arquivo_PT-BR.ass
    UC-->>API: relatório do lote (SSE canal "traducao")
```

---

## Cache de tradução

- **Formato:** JSON, lista de `EntradaCache(indice, estilo, original, traduzido, idiomaOriginal, idiomaTraduzido)`.
- **Localização:** `cache/<espelha a pasta de entrada>/<nomeLegenda>.cache.json` — ex. `cache/86/86 Part1/[DB]86_-_01_..._ENG.cache.json`.
- **Chave de lookup:** o **texto original**, não o índice — se a mesma frase aparecer em falas diferentes, a mesma tradução é reaproveitada (cada evento mantém seu próprio timestamp, então isso nunca afeta sincronismo).
- **Editável manualmente:** o operador pode abrir o `.cache.json` e corrigir uma tradução na mão; na próxima execução, o valor corrigido é respeitado (não é sobrescrito, a menos que o texto original mude).
- **Entradas de falha:** quando o LLM devolve o mesmo texto (não traduziu), a entrada é salva com `original == traduzido` — esse é o "fallback de falha" que os 3 fluxos de [Correção & Revisão](06-modulo-correcao-revisao.md) tratam de formas diferentes.
- **Proveniência (`ProvenienciaCache`):** cada arquivo de cache carrega um hash de proveniência (`contextoHash` = SHA-256 do prompt de sistema/lore, mais modelo e idiomas). Se a **lore ou o modelo mudam**, o cache anterior é **arquivado e não reusado** — a fala é retraduzida sob o contexto atual, em vez de servir uma tradução feita sob outra lore. É também o mecanismo que separa os braços A/B do [contexto de cena](#correção-de-gênero-por-contexto-de-cena-piloto-d).

---

## Proteção de tags ASS (`MascaradorTags`)

Falas de karaokê/efeitos têm prefixos de formatação complexos:

```
{\fad(100,100)\blur2\c&HE8E8E8&\1a&HFF&}Kitto Soba de Hohoendeitai
```

Antes de enviar ao LLM, o `MascaradorTags` substitui blocos `{...}` por marcadores neutros (`[[TAG0]]`, `[[TAG1]]`...), pedindo ao LLM para preservá-los literalmente. Isso evita dois problemas comuns de LLMs com texto estruturado: (1) tentar "traduzir" o conteúdo da tag, e (2) alucinar/corromper a sintaxe da tag. Falas de estilos listados em `tradutor.estilos-ignorados` (karaokê romaji, títulos de abertura/encerramento) **nem chegam a ser enviadas ao LLM** — são copiadas como estão.

---

## Prevenção de alucinação: lote de 1 linha

`tradutor.tamanho-lote: 1` — cada requisição ao LLM traduz **uma única fala por vez**, não um lote de N falas. Isso é mais lento (mais chamadas HTTP), mas elimina uma classe inteira de erro: LLMs locais menores frequentemente perdem a contagem de linhas em lotes maiores (retornam menos ou mais linhas que o pedido), desalinhando toda a tradução subsequente do arquivo.

---

## Modelo "coringa": `tradutor.llm.model: "current"`

O valor de `tradutor.llm.model` em `application.yml` é **sempre** o literal `"current"`, nunca o id fixo de um modelo (ex. `"mistralai/mistral-nemo-instruct-2407"`). Ao iniciar cada operação, `LlmClientAdapter.verificarDisponibilidade()` consulta o LM Studio para descobrir **qual modelo está de fato carregado em memória** (via a API estendida `/api/v0/models`, que expõe o campo `state: "loaded"`) e adapta o valor em runtime. Isso permite trocar o modelo ativo direto na UI/CLI do LM Studio (`lms load`) sem tocar no `application.yml` nem recompilar — e evita que o app dispare um **auto-load de uma segunda instância de modelo** ao mandar uma requisição para um id que não bate com o que está carregado (ver detalhes técnicos em [Solução de Problemas](15-solucao-problemas.md#lm-studio-carregando-dois-modelos-simultaneamente)).

---

## Correção de gênero por contexto de cena (piloto D)

**O problema.** Em PT-BR, adjetivos e particípios concordam em gênero com quem fala ("Estou cert**o**" vs "Estou cert**a**"). Só que a fonte `.ass` de fansub **não diz quem fala** — a coluna `Name`/Actor vem vazia (no Gundam 08th MS Team, em 325/325 falas). Como o pipeline traduz **linha a linha** (`lote=1`) e dedup/cacheia por texto original puro, o modelo não tem como inferir o gênero e erra: `Thank you, Norris.` — dito pela **Aina** — virava "Obrigad**o**".

**A correção (piloto D).** Um motor **OPT-IN** em subpacotes próprios da fatia — `traducao/{domain,application,infrastructure}/contextocena` — que, para cada fala de **diálogo**, manda ao modelo uma **janela de falas vizinhas** como referência (rotuladas "NÃO traduzir, só contexto") para ele inferir o falante, mantendo `lote=1` no alvo. Não-diálogo (música/letreiro/romaji) continua pela via de hoje. Desligado por padrão: com `tradutor.contexto-cena.ativo=false`, o pipeline é **byte-idêntico** ao atual (provado por 16 testes de caracterização).

```mermaid
flowchart TD
    A["🗣️ Fala elegível"] --> B{"Categoria?"}
    B -->|"não-diálogo<br/>(música / letreiro / romaji)"| V["Via de hoje<br/>dedup + lote + validação"]
    B -->|"diálogo"| J["Monta JANELA de contexto<br/>N falas vizinhas (tamanho-janela)"]
    J --> M["Mascara as tags do alvo<br/>tag ASS vira marcador seguro"]
    M --> P["1 chamada ao LLM<br/>contexto (não traduzir) + alvo mascarado"]
    P --> D["Desmascara as tags<br/>fallback anti-alucinação:<br/>mantém o original se o marcador corromper"]
    D --> W["Valida + reconstrói o .ass por ÍNDICE<br/>(nunca por texto → não recolapsa cenas)"]
    V --> W
    W --> OUT["📄 .ass PT-BR"]

    classDef dlg fill:#312e81,stroke:#818CF8,color:#F9FAFB
    classDef via fill:#14532d,stroke:#4ADE80,color:#F9FAFB
    classDef io fill:#1e293b,stroke:#3B82F6,color:#F9FAFB
    class A,J,M,P,D,W dlg
    class V via
    class OUT io
```

Detalhes que preservam a segurança do cache e da legenda:

- **Chave de cache só-fonte:** a assinatura da entrada contextual é derivada só da FONTE (índice + original + estilo + hash das vizinhas + versão da política), nunca do palpite do modelo — evita circularidade. `EntradaCache` ganhou o campo aditivo `assinaturaContexto` (o cache legado continua válido).
- **Proveniência separa os braços:** ligar a flag muda o `contextoHash`, então uma tradução feita **com** contexto de cena nunca reusa (nem é reusada por) uma feita **sem** — o braço B arquiva o cache do braço A automaticamente.
- **Relatório A/B append-only:** com `tradutor.contexto-cena.relatorio-ab=true`, cada execução acrescenta uma linha a `logs/contexto_cena_ab.jsonl` (marcada `A_BASELINE` ou `B_CONTEXTO`), com atividade e custo — a telemetria canônica (que deduplica por episódio) não conseguiria guardar os dois braços do mesmo episódio. Ver [Telemetria](10-modulo-telemetria.md).

> Como ativar e comparar (protocolo A/B com variáveis congeladas e critério de decisão) está registrado no cérebro do projeto (Plano-Mestre D). O acerto de gênero em si é medido contra um conjunto-ouro manual; o relatório mede **atividade e custo**, não acerto.

---

## Endpoint REST

### `POST /api/traduzir`

```json
{
  "entrada": "C:/animes/[Sokudo] DanMachi/Season 04/legendas_extraidas",
  "saida": "C:/animes/[Sokudo] DanMachi/Season 04/legendas-ptbr",
  "contextoId": "danmachi-s4"
}
```

| Campo | Obrigatório | Descrição |
|-------|:-----------:|-----------|
| `entrada` | ✅ | Pasta com legendas `.ass`/`.ssa` em inglês |
| `saida` | ⚪ | Pasta de saída para os arquivos `_PT-BR.ass` |
| `contextoId` | ⚪ | Id de um dos contextos/lore cadastrados (ver [Contextos & Lore](09-contextos-lore.md)); padrão `"danmachi"` |

A resposta imediata é `200 OK` (job assíncrono); o progresso e relatório de cada lote chegam via **SSE** no canal `traducao`.

---

## Navegação

| Anterior | Próximo |
|----------|---------|
| [← Extração de Legendas](04-modulo-extracao-legendas.md) | [Correção & Revisão →](06-modulo-correcao-revisao.md) |
