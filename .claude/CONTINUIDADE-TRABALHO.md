# CONTINUIDADE — KRONOS

TAREFA ORIGINAL: escolher o modelo LLM titular do pipeline, e qual serve para CORRIGIR
                 as falas que o titular abandona.
OBJETIVO FINAL:  decisão com artefato, não com impressão.
CRITÉRIO DE ENCERRAMENTO: leitura da qualidade do português + teste do modelo de recuperação.
BRANCH / COMMIT BASE: main — `b78a5cf6` local, ahead 1 de origin/main
SHA-256 DOS DOCUMENTOS-REGRA: ENGENHARIA f43d9c05… (1136) · REGRA-DO-DOCKER 98a5ad6a… (2402)

## ESTADO AGORA (2026-08-12, 09:40) — Paulo assumiu a rodada comparativa

- **A 8099 é o quarkusDev com o código de hoje.** O contêiner `kronos` foi PARADO
  (`docker compose stop kronos`): ele tinha voltado sozinho com o Docker Desktop, tomado a
  porta e servido a imagem de ontem. Como é dev no host, os caminhos são `C:\animes\...`;
  se voltar ao contêiner (`docker compose start kronos`), passam a ser `/acervo\...`.
- **Só a `aya-expanse-8b` está carregada** (`/api/v0/models` → `state: loaded`).
- **A telemetria DEDUPLICA por `nomeEpisodio` e o mais recente vence**: rodar outro modelo
  APAGA o registro do anterior. O baseline do mistral foi congelado em
  `backups/pre-aya-20260812/baseline-mistral-por-episodio.json` (307 episódios). Perdi E01 e
  E02 do ZZ antes de perceber — congelar ANTES de cada troca de modelo, sempre.
- Piloto já rodado no ZZ (aya): `_piloto_aya_eng` / `_piloto_aya_saida`, 2 eps.
  **O cache do ZZ E01/E02 agora é da aya**, não do mistral.
- Pareado no MESMO episódio (ZZ E02): mistral 285 falas / 0 pend / **4,25 min** ×
  aya 285 falas / 0 pend / **1,26 min** — 3,37× mais rápida, desfecho idêntico.
- Onde o tempo vai: **165,8s de 172s (96,4%) é espera pelo LM Studio**. Otimização de JVM/JIT
  atua sobre os 3,6% restantes — decisão de Paulo de não perseguir isso está medida e correta.

## EXPERIMENTO PREPARADO — Paulo executa (2026-08-12, 12:00)

**A aplicação está FORA DO AR de propósito.** Paulo sobe e roda; eu não toco em `src/main`
enquanto rodar — foi um live-reload meu que matou o E01 do Unicorn às 09:22.

Tudo já preparado e conferido:

| item | estado |
|---|---|
| `legendas_eng_achatado` (cópia de trabalho, 22 arquivos) | criada, conferida, **ainda SEM troca de fonte e SEM achatador** |
| `legendas_extraidas_ass` (entrada original) | INTACTA, 22 arquivos |
| `traducao_mistral` (backup do Paulo) | intocado |
| baseline mistral por episódio | `backups/pre-aya-20260812/baseline-mistral-por-episodio.json` (307 eps) |
| baseline **aya** por episódio | `backups/pre-achatado-20260812/baseline-aya-unicorn.json` (22 eps) |
| cache da aya | `backups/pre-achatado-20260812/cache-aya/` (22, conferido) |

**Sequência para rodar** (pasta sempre `...\Gundam Unicorn Season 1\legendas_eng_achatado`):
1. `POST /api/troca-legenda/aplicar` com `forcarArial: true`
2. `POST /api/troca-legenda/achatar-estilos`
3. `POST /api/traduzir` com `contextoId: "gundam_zz"`… **NÃO** — é `gundam_uc` ou o do Unicorn:
   conferir em `GET /api/contextos` antes. Saída: pasta NOVA `traducao_ptbr_achatado`.
4. Medir com `.\gradlew.bat test --tests "*MedicaoUnicornMistralXAyaIT*" --rerun-tasks`
   (apontando o harness para a pasta nova).

**O que esperar, medido antes de rodar** — estilos na cópia:
`Default 5665 · OPL2 3255 · ED2 207 · ED-EN 165 · ED 165 · Sign 23`.
O achatador decide preservação por `podeSerCamadaMusical`, que **não conhece OPL2** (só a
lista nominal do yml conhece). Se ele colapsar OPL2 em `Default`, o veto nominal deixa de
casar na tradução e **3.255 linhas de letra do OP vão ao LLM** — o próprio yml registra o que
acontece: "sa" virou "Meu nome é Mineva Lao Zabi". É exatamente o ganho/perda que o
experimento quer medir; a rodada fica bem mais longa por causa dessas linhas.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA

**Assim que o LM Studio estiver no ar** (ver BLOQUEIO abaixo), nesta ordem:

1. Conferir que só a aya está carregada:
   `curl -s http://127.0.0.1:8099/api/llm/status`
   (com 2 modelos carregados o KRONOS pega o PRIMEIRO da lista e mede o errado)
2. Subir o KRONOS: `.\iniciar-kronos-dev.cmd` — depois conferir a porta 8099, porque
   `TaskStop` mata só o wrapper do Gradle e a JVM velha continua servindo código antigo.
3. Escolher obra que o mistral tenha fechado em **PASSADA ÚNICA** — senão repete a
   assimetria de 5 passadas × 1 que invalidou a comparação do Guilty Crown.
   Candidatas com telemetria de passada única a conferir:
   `Mobile Suit Gundam ZZ` (47 traduzidos), `Gundam Unicorn Season 1`,
   `[Joseki] 0083 Stardust Memory`, `[Joseki] 08th MS Team`
4. Saída em pasta NOVA (`traducao_ptbr_aya`), nunca sobrescrevendo o que existe.
5. Backup do cache da obra em `backups/` ANTES — a troca de modelo arquiva o cache
   do mistral (proveniência inclui `modeloLlm`).

## FECHADO COM ARTEFATO (2026-08-12)

- **Recusa do LLM ≠ servidor fora do ar** (`b78a5cf6`). HTTP 4xx permanente vira
  `RequisicaoRecusadaPeloLlmException` e segue o caminho da pendência; timeout/5xx continua
  abortando; disjuntor de 3 recusas consecutivas devolve o aborto quando é global.
  Caso-controle REPROVOU antes do conserto (`TraducaoParcialException` por 1 fala);
  4 casos novos verdes, com contraprova. **Suíte 1655 testes, 0 falhas, 0 erros, 268 classes.
  Portão do projeto rc=0, 156 testes em 32 classes de guarda.**
- **Inventário de estilos do acervo** (`C:\animes`): 1.022 `.ass`, 2.346.132 linhas
  `Dialogue`, 132 estilos distintos, e só **2 com `\k` no corpo** — `Paradise` (864) e
  `Dungeons` (2), ambos cobertos pelo CONTEÚDO. Congelado em `EstiloMusicalDoAcervoTest`.
- **Anotação de 11/08 corrigida:** `Hey World Romaji` NÃO escapa (o padrão casa a forma
  curta `roma`), e `PadraoEstiloMusical` tem 10 substrings, não só `(op|ed)`.

## FECHADO COM ARTEFATO (2026-08-11)

- Portão único `checar-portao.ps1` — 3 estados, `--rerun-tasks`.
- Compose falha fechada: `docker compose --env-file /dev/null config` sai **1** (saía 0).
- 7 rotas assíncronas recusam caminho impossível ANTES de enfileirar (`0f3724b4`).
- Segunda opinião entre modelos, desligada por padrão (`b1dcf791`, em origin/main).
- Hotfix do token de template: `<|END_OF_TURN_TOKEN|>` fazia o pipeline descartar tradução
  CORRETA — era 99% da pendência da aya.
- Zeta: 22 → 6 pendências, 45/50 concluídos, em 59 s.
- Confronto de 4 modelos em conteúdo virgem + Guilty Crown inteiro com a aya.

## O QUE A MEDIÇÃO ESTABELECEU

| | |
|---|---|
| passada única, mesmos 23 eps | **aya 19 pendências / 12 eps limpos** × mistral 141 / 0 |
| artefato final (aya 1 passada × mistral 5) | **empate**: 266 × 264 falas em inglês de 5.679 |
| subconjunto objetivo | **empate**: aya venceu 51, mistral venceu 53 |
| qualidade do português | **NÃO LIDO** — é o que falta |

Três medições convergiram para empate ou vantagem de ESFORÇO. Nenhuma estabeleceu
vantagem de QUALIDADE. Promover a aya se sustenta em custo e confiabilidade do pipeline,
**não** em "traduz melhor".

## GAPS E BLOQUEIOS REAIS

- 🔴 **BLOQUEIO — LM Studio não sobe por lançamento programado.** O executável sai com
  código 0 em segundos, sem stdout/stderr, e nenhum processo permanece. Descartados por
  teste: lock órfão (`llmster-pid.lock` apontava para o PID 20244, morto — movido para
  `.internal\llmster-pid.lock.orfao-2026-08-12`) e atualização pendente (`pending`
  desviada e restaurada, sem efeito). Quatro caminhos tentados: `Start-Process` com e sem
  sandbox, execução direta com redirecionamento, e `cmd /c start`. **Ação de Paulo: abrir o
  LM Studio pelo ícone e carregar SÓ a `aya-expanse-8b`.** Sem isso, nada do teste de hoje roda.
- 🔴 `modelo-recuperacao` nunca foi exercitado com a aya. O alvo pronto são as 6 pendências
  do Zeta (E08×2, E15, E17, E33, E38), todas discurso citado com aspas — o tower recuperou
  3 delas. Config: `tradutor.llm.modelo-recuperacao: "aya-expanse-8b"`.
- 🟡 As 4.249 discordâncias entre mistral e aya no Guilty Crown esperam leitura humana.
- 🟡 `tradutor.fallback-online.ativo: true` com o comentário acima dizendo "desligado por
  padrão". O Google esteve no circuito de TODAS as rodadas de 11/08. **Decisão de Paulo
  (é produto): corrigir o valor ou o comentário.** Para o experimento de hoje, o Google
  precisa estar FORA do circuito, senão mede-se aya+Google.
- 🟡 Lacuna declarada: estilo com nome próprio de canção e SEM `\k` no corpo
  (`RISE LIGHT RISE English`, `Logo`) segue invisível aos detectores. Fechar por nome
  exigiria lista nominal por obra — remendo, não mecanismo. O dano que ela causava
  (derrubar o episódio) está fechado por `b78a5cf6`.
- 🟡 Contêiner `kronos` com `restart: unless-stopped` — volta sozinho ao ligar a máquina.

## NÃO REPETIR

- Vigia de rodada por JANELA DE TEMPO no log: errei duas vezes em 11/08, anunciando fim de
  rodada que nem tinha começado. Ancorar em CONTAGEM de conclusões e esperar o número subir.
- Comparar rodada com cache quente: a rodada de 1min58s parecia hotfix e era cache
  (`falasDoCache=343`). Conferir `falasDoCache` antes de interpretar qualquer resultado.
- Classificar estilo pela SAÍDA: o achatador colapsa `OP`/`ED`/`Other songs` em `Default`.
  A máscara certa é a lista `PRESERVADA_POR_REGRA` do dataset do pipeline.
- `glob.glob` / `Get-Content` em caminho com `[Sokudo]` / `[1080p]`: colchete é classe de
  caracteres. Em PowerShell, usar `-LiteralPath` ou `[System.IO.File]::ReadAllText`.
- Reimplementar em script o critério que o código de produção já tem. O "é musical?" desta
  sessão veio de `PadraoEstiloMusical` via teste JUnit; o PowerShell só colheu fato bruto.
