# CONTINUIDADE — KRONOS

TAREFA ORIGINAL: escolher o modelo LLM titular do pipeline, e qual serve para CORRIGIR
                 as falas que o titular abandona.
OBJETIVO FINAL:  decisão com artefato, não com impressão.
CRITÉRIO DE ENCERRAMENTO: leitura da qualidade do português + teste do modelo de recuperação.
BRANCH / COMMIT BASE: main — em sincronia com origin/main
SHA-256 DOS DOCUMENTOS-REGRA: ENGENHARIA f43d9c05… (1136) · REGRA-DO-DOCKER 98a5ad6a… (2402)

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA

Paulo pediu (2026-08-11, ao encerrar): **testes complexos com a aya em animações mais
complexas**. Antes de disparar, nesta ordem:

1. Conferir que só a aya está carregada:
   `curl -s http://127.0.0.1:8099/api/llm/status`
   (com 2 modelos carregados o KRONOS pega o PRIMEIRO da lista e mede o errado)
2. Escolher obra que o mistral tenha fechado em **PASSADA ÚNICA** — senão repete a
   assimetria de 5 passadas × 1 que invalidou a comparação do Guilty Crown.
   Candidatas com telemetria de passada única a conferir:
   `Mobile Suit Gundam ZZ` (47 traduzidos), `Gundam Unicorn Season 1`,
   `[Joseki] 0083 Stardust Memory`, `[Joseki] 08th MS Team`
3. Saída em pasta NOVA (`traducao_ptbr_aya`), nunca sobrescrevendo o que existe.
4. Backup do cache da obra em `backups/` ANTES — a troca de modelo arquiva o cache
   do mistral (proveniência inclui `modeloLlm`).

## FECHADO COM ARTEFATO (2026-08-11)

- Portão único `checar-portao.ps1` — 3 estados, `--rerun-tasks`. Saída 0, 156 testes/32 classes.
- Compose falha fechada: `docker compose --env-file /dev/null config` sai **1** (saía 0).
- 7 rotas assíncronas recusam caminho impossível ANTES de enfileirar (`0f3724b4`).
- Segunda opinião entre modelos, desligada por padrão (`b1dcf791`, em origin/main).
- Hotfix do token de template: `<|END_OF_TURN_TOKEN|>` fazia o pipeline descartar tradução
  CORRETA — era 99% da pendência da aya. Suíte 1647 testes, 0 falhas.
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

## TESTES / GUARDAS

Executados: suíte completa 1647 testes, 0 falhas · portão 0.
Pendente: nenhum bloqueante.

## GAPS E BLOQUEIOS REAIS

- 🔴 `modelo-recuperacao` nunca foi exercitado com a aya. O alvo pronto são as 6 pendências
  do Zeta (E08×2, E15, E17, E33, E38), todas discurso citado com aspas — o tower recuperou
  3 delas. Config: `tradutor.llm.modelo-recuperacao: "aya-expanse-8b"`.
- 🟡 As 4.249 discordâncias entre mistral e aya no Guilty Crown esperam leitura humana.
- 🟡 `tradutor.fallback-online.ativo: true` com o comentário acima dizendo "desligado por
  padrão". O Google esteve no circuito de TODAS as rodadas de hoje. Decisão de Paulo:
  corrigir o valor ou o comentário.
- 🟡 Estilo de karaokê batizado com o nome da canção (`Hey World Romaji`, `Other songs`)
  escapa dos 3 detectores. Uma fala assim ABORTOU um episódio inteiro com o tower.
- 🟡 Contêiner `kronos` com `restart: unless-stopped` — volta sozinho ao ligar a máquina.

## NÃO REPETIR

- Vigia de rodada por JANELA DE TEMPO no log: errei duas vezes hoje, anunciando fim de
  rodada que nem tinha começado. Ancorar em CONTAGEM de conclusões e esperar o número subir.
- Comparar rodada com cache quente: a rodada de 1min58s parecia hotfix e era cache
  (`falasDoCache=343`). Conferir `falasDoCache` antes de interpretar qualquer resultado.
- Classificar estilo pela SAÍDA: o achatador colapsa `OP`/`ED`/`Other songs` em `Default`.
  A máscara certa é a lista `PRESERVADA_POR_REGRA` do dataset do pipeline.
- `glob.glob` em caminho com `[Sokudo]` / `[1080p]`: colchete é classe de caracteres.
