# CONTINUIDADE — KRONOS

TAREFA ORIGINAL: escolher o modelo LLM titular do pipeline, e qual serve para CORRIGIR
                 as falas que o titular abandona.
OBJETIVO FINAL:  decisão com artefato, não com impressão.
CRITÉRIO DE ENCERRAMENTO: leitura da qualidade do português + teste do modelo de recuperação.
BRANCH / COMMIT BASE: main — `b7abf431`, árvore limpa, ahead de origin/main
SHA-256 DOS DOCUMENTOS-REGRA: ENGENHARIA f43d9c05… (1136) · REGRA-DO-DOCKER 98a5ad6a… (2402)

## ESTADO AO FIM DE 13/08 — mesa limpa, pronto para rodar do zero

Paulo apagou as saídas do Unicorn DE PROPÓSITO, para rodar do zero. Não houve perda acidental.
Sobrou `legendas_extraidas_ass` (22 ass, fontes ORIGINAIS, sem achatar) — que é exatamente a
entrada que se quer.

- **Tudo parado**: 8099 livre, nenhum Java do KRONOS, nenhum hunspell pendurado.
- **Suíte 1718 / 0 falhas / 26 pulados.** Os pulados são os harnesses de auditoria dizendo
  "acervo ausente, NÃO VERIFIQUEI" — comportamento novo e correto.
- **Backups intactos**: `unicorn-ass-aya-20260812` (a versão de 12/08) ·
  `troca_tipo_legenda_20260812_113344` (entrada original, hash conferido contra o MKV) ·
  `cache-unicorn-aya-20260813` (33 caches) · `cache-zeta-mistral-20260812` (82/82).

**O pipeline mudou desde a última tradução completa** — o próximo run é o primeiro com tudo:

| peça | efeito medido |
|---|---|
| regra `-ção` no normalizador | 440 → 231 ocorrências de erro no diálogo |
| dicionário do sistema (hunspell pt_BR) | 231 → **91** — 79% no total |
| memória do corretor | E01 em **73s**; antes da memória eram 3m05s/arquivo |
| deadlock do hunspell corrigido | leitura em thread; era travamento à espera de acontecer |

**Para rodar**: subir pelo `.cmd` e disparar pela UI, entrada `legendas_extraidas_ass`, contexto
`gundam_unicorn`. **Usar pasta de saída SEPARADA** se quiser comparar depois — foi assim que a
comparação das três versões existiu.

**Não medido ainda**: o tempo total do episódio COM a memória. O E01 saiu em 73s, o que sugere
voltar aos ~28 min do lote inteiro, mas isso é expectativa, não medição.

## 13/08 — ORTOGRAFIA SAIU DO LLM: o que está PRONTO e o que falta LIGAR

**A porta existe, está testada e NÃO está plugada no pipeline.** Ninguém chama
`DicionarioOrtograficoPort` ainda. É a próxima ação, e tem uma correção de desenho a aplicar
ANTES (ver o alerta abaixo).

| peça | onde | estado |
|---|---|---|
| regra `-ção`/`-ções` | `NormalizadorAcentosComuns` | ✅ ligada, 105 de 119 no Zeta |
| `DicionarioOrtograficoPort` | `core.texto` | ✅ testada, ❌ **não plugada** |
| `HunspellDicionarioAdapter` | `core.texto` | ✅ 5 testes, falha fechada com 3 estados |
| `instalar-pre-requisitos.ps1` | raiz | ✅ 9 OK / 0 FALTA nesta máquina |

Dicionários em `C:\Hunspell\`: `pt_BR` (4.373 KB) · `en_US` (539 KB) · `de_DE` (4.255 KB).
Instalados por `choco install hunspell.portable` + download do repositório do LibreOffice. O
alemão tem nome próprio: o pacote é `de_DE_frami`, e pedir `de_DE` devolve **404**.

**ALERTA DE DESENHO, medido e ainda NÃO corrigido:** `Resonância` (grafia errada; o certo é
*ressonância*) foi reprovada pelo `pt_BR` e **aceita** pelo `de_DE`. Cada dicionário novo é uma
chance de erro real passar. Antes de plugar: os secundários têm de **ROTULAR, não aprovar** — só
o `pt_BR` decide se está errado; `en_US`/`de_DE` dizem que TIPO de não-português é aquilo, o que
muda a AÇÃO e não o veredito.

**Medido no Zeta/aya (6.736 formas em minúscula, 66s):** 6.408 ok em PT · 22 resíduo de inglês
(`suit`, `suits`, `shuttle`, `cockpit`, `booster`) · **306 erro real** (`opiniao`, `antiaerea`,
`reuniao`, `serao`, `aereo`, `crianca`, `assembléia`, `necessario`). Limite: palavra que é forma
sem acento em PT e existe em inglês escapa (`area`, `video`, `radio`, `sera`).

**O alemão paga, e a percepção é do Paulo:** 155 formas capitalizadas do acervo que SÓ o alemão
reconhece — `Kamille` 950 (camomila), `Katz` 256, `Braun` 54, `Gier` 34, `Sieg` 23, `Nordlicht`
20, `Engel` 6. No 86 os Legion são nomeados em alemão direto.

**Japonês:** 6 falas com kana/kanji em 94.701 — cinco são notas do fansub entre chaves, uma é um
kanji solto (`那 nave...`). Não existe hunspell `ja` (japonês não separa palavras por espaço) e
`choco install mecab` não existe. Paulo instalando o MeCab por fora em 13/08; se entrar, encaixa
no MESMO padrão (ProcessBuilder atrás de porta, zero dependência no build) — o instalador oficial
põe em `C:\Program Files (x86)\MeCab\bin`, fora do PATH.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA — retraduzir o Zeta com a aya, para o PAR de comparação

Decisão do Paulo em 13/08: vale retraduzir o Zeta inteiro com a aya. Hoje só o Unicorn tem o par
mistral × aya lado a lado; com o Zeta a comparação passa a ter DUAS obras, e o Zeta é maior e tem
as 6 pendências conhecidas.

**A saída TEM de ir para pasta separada.** É o que tornou a auditoria do Unicorn possível.

```
C:\animes\Mobile Suit Zeta Gundam\        (atenção: NÃO é "Mobile Suit Gundam ZZ", outra obra)
  legendas_extraidas_ass   50 ass   <- entrada
  traducao_ptbr            56 ass   <- saída do MISTRAL. NÃO sobrescrever: é metade da comparação
  backup_traducao          68 ass
  → saída da aya: traducao_aya      (mesmo padrão do Unicorn)
```

- **Custo medido, não estimado no chute**: 17.036 falas no cache do Zeta, a 178 falas/min (ritmo
  real da última rodada do Unicorn) ≈ **96 min**.
- **NÃO ligar o reuso entre modelos nesta rodada.** Ele existe para exercitar pendência barato; se
  a aya herdar do mistral, não há o que comparar. As duas coisas se excluem.
- A app no ar (subiu 13/08 00:00) é ANTERIOR às features de ontem à noite — e para esta rodada
  isso não importa: retraduzir com aya funciona nela. Só o experimento de reuso + segunda opinião
  exige reiniciar pelo `.cmd` próprio.
- Baseline do cache do mistral protegido em `backups/cache-zeta-mistral-20260812` (82/82, com
  estrutura de pastas). O sistema também arquiva `.geracao_*.json` sozinho ao trocar de modelo.
- Telemetria: os 350 registros atuais estão com o campo `anime` VAZIO, então não há baseline do
  Zeta a congelar — e é um defeito de dado a olhar depois, porque a dedup por episódio depende
  desse campo.

## O QUE FICOU PARA TRÁS NO ACOMPANHAMENTO (erro meu, corrigido)

Reportei 19 concluídos / 2 parciais / 1 falha / 30m11s como se fosse da rodada de 13/08. É de
**12/08**: o `console-web.log` parou de ser escrito em 12/08 11:07 e a saída do Unicorn é de
12/08 11:35–12:04. A app de hoje subiu por outro caminho e não escreve nesse arquivo. **Instrumento
certo para estado vivo é a API (`/api/status`, `/api/llm/status`), não o log.** E a falha do E01
naquele log é o reload de 12/08 já registrado — não houve reload meu em 13/08 (zero commits e zero
`.class` recompilados entre 09h e 10h).

## PRÓXIMA AÇÃO EXECUTÁVEL (frentes anteriores)

**LM Studio no ar (12/08 noite) com 8 modelos. Medido agora:** sem especificar modelo responde
`aya-expanse-8b`; pedindo `towerinstruct-mistral-7b-v0.2` o servidor honra o override. É o arranjo
que a segunda opinião precisa. `model: "current"` no yml significa que o titular é quem o LM Studio
tiver carregado — não se troca no yml, troca-se na UI do LM Studio.

**A PROVENIÊNCIA DO CACHE DECIDE QUAL OBRA RODAR — conferido, não suposto:**

| obra | `modeloLlm` no cache | com a aya titular |
|---|---|---|
| Unicorn (33 caches) | `aya-expanse-8b` | **bate** → minutos |
| Zeta (82 caches) | `mistralai/mistral-nemo-instruct-2407` | **não bate** → retraduz 50 eps |

Logo: para exercitar a segunda opinião HOJE, o alvo é o **Unicorn** (2 pendências conhecidas, cache
quente). Para atacar as **6 do Zeta** sem retraduzir tudo, é preciso descarregar a aya no LM Studio
e deixar o mistral-nemo carregado — aí a proveniência bate e só as 6 vão ao LLM.

1. Subir com `iniciar-kronos-dev-com-recuperacao.cmd` (liga o tower como segunda opinião por
   variável de ambiente, SEM tocar no `application.yml`, que é falha fechada de propósito).
2. Conferir no console: `[SEGUNDA-OPINIAO] Ligada: "towerinstruct-mistral-7b-v0.2"`. Se disser
   DESLIGADA, a variável não chegou — e "nenhuma recuperação" NÃO pode ser lida como "o modelo não
   serviu" (`d1784665` existe exatamente para isso).
3. Aplicar a troca de fonte na entrada restaurada e traduzir.

**O gold set está pronto e é a frente de maior valor:**
`relatorios/gold-set-unicorn-mistral-x-aya.md` — 60 pares mistral × aya lado a lado, de 1.807
divergências em 4.497 falas, ordenados por suspeita. Marcar qual está melhor em cada par.
Nos 6 primeiros já apareceram 3 defeitos que nenhum instrumento via: espanhol da aya
("Reanudaremos"), inglês residual do mistral ("Hurry!") e o mistral trocando o pseudônimo
"Audrey Burne" pelo nome verdadeiro "Mineva Lao Zabi" — spoiler narrativo.

Depois, as duas frentes antigas:

1. **Ler o português lado a lado.** É o ÚNICO eixo que nenhuma medição resolve, e agora é o
   que sobrou. As três versões estão em disco na mesma obra:
   `C:\animes\Mobile Suit Gundam Unicorn Re0096 (2016) [Season 1] [BD 1080p HEVC OPUS] [Dual-Audio]\Gundam Unicorn Season 1\`
   → `traducao_mistral` (22) · `traducao_aya` (21) · `traducao_ptbr` (22, fonte+achatado)
2. **Exercitar o `modelo-recuperacao`** — 🔴 nunca foi feito. Alvo pronto: as 6 pendências do
   Zeta (E08×2, E15, E17, E33, E38), todas discurso citado com aspas; o tower recuperou 3.
   Config: `tradutor.llm.modelo-recuperacao: "aya-expanse-8b"`.

Para rodar qualquer coisa: **subir o KRONOS é com Paulo** (`.\iniciar-kronos-dev.cmd`), e eu
NÃO toco em `src/main` nem rodo Gradle enquanto houver job — foi um live-reload meu que matou
o E01 do Unicorn às 09:22.

## ESTADO DA BANCADA

- 8099 LIVRE (app desligada). Contêiner `kronos` PARADO de propósito — ele volta sozinho com o
  Docker Desktop e serve imagem velha; `docker compose start kronos` para religar.
- LM Studio no ar com **só a aya-expanse-8b** carregada.
- `legendas_extraidas_ass` foi RESTAURADA ao ORIGINAL em 12/08 à noite — 22/22 conferidos por
  hash, 9.480 eventos, fontes originais (Althea, Androgyne, Dash Horizon, Gandhi Sans). O estado
  anterior (achatado + Arial, 6.582 eventos) está em
  `backups/achatado-fonte-20260812_estado-anterior`. Falta só APLICAR A TROCA DE FONTE (precisa
  do KRONOS no ar) e traduzir.
- **O backup de 11:33 É o material cru**: `mkvextract` da faixa 4 do E01 devolveu hash IDÊNTICO
  (`481FCD71…`). Não há o que reextrair. E o Unicorn **não tem `\k` em faixa nenhuma** — é KFX
  pós-template, cada sílaba já é um evento com `\t`/`\move`. Ausência de `\k` aqui NÃO é sinal de
  achatamento; eu li errado uma vez.
- **Faixa 5 `Sign&Songs` nunca foi extraída nem traduzida** (186 eventos só no E01). Todo o acervo
  do Unicorn foi traduzido só da faixa 4 `Dialogue`. Pode ser decisão (música é da fatia de
  karaokê) — não tratado como defeito, fica como pista.
- **NENHUMA FALA FOI PERDIDA — medido no acervo inteiro, não por amostra.** Casando por INSTANTE
  (o texto muda ao traduzir, o tempo não), as três versões batem com o original:
  `traducao_mistral` 5.665/5.665 · `traducao_aya` 5.447/5.447 (21 eps; o E01 é o que meu
  live-reload matou) · `traducao_ptbr` 5.665/5.665. **Zero sem par, zero vazias nas três.**
  Por estilo, o achatamento removeu 0 de 5.665 `Default`, 0 de `ED`/`ED2`/`ED - EN`/`Sign`, e
  2.898 de 3.255 `OPL2` (89%) — só a abertura. E a letra sobrevive: o E01 sai de 155 eventos /
  111 textos para 17 / 17, e os 17 são as 17 linhas da letra. O que se perde é a animação
  sílaba a sílaba, não o texto.
- **Achatar DEPOIS da tradução é executável hoje**: `/api/troca-legenda/achatar-estilos` aceita
  qualquer diretório e usa só `conferirDiretorio` — a lista de "pasta de saída" do
  `GuardaCaminhoEntrada` vale só para a tradução, então apontar para `traducao_ptbr` NÃO é
  barrado. Grava in-place com backup.
- ARMADILHA para o próximo run: o cache do Unicorn já tem a aya. Traduzir de novo virá quase todo
  do cache (minutos, não 30). Ótimo se o objetivo é a legenda final limpa; inútil se o objetivo é
  medir o modelo — aí precisa de obra virgem ou `permitirRetraducao`.
- Baselines de telemetria congelados (a telemetria DEDUPLICA por episódio e o mais recente
  vence — sempre congelar antes de trocar de modelo):
  `backups/pre-aya-20260812/baseline-mistral-por-episodio.json` (307 eps) ·
  `backups/pre-achatado-20260812/baseline-aya-unicorn.json` (22 eps) · `.../cache-aya/` (22).

## O QUE A AUDITORIA DE 12/08 ESTABELECEU

| eixo | mistral | aya | aya+achatado |
|---|---|---|---|
| eco no artefato (5.455 falas) | 184 | 181 | 183 |
| resíduo em inglês | 0 | 0 | 0 |
| pergunta → afirmação | **34 (4,4%)** | **2 (0,3%)** | 4 (0,5%) |
| negação perdida | **11 (1,5%)** | **3 (0,4%)** | 4 (0,5%) |
| acentuação faltando | **23 (0,4%)** | **197 (3,6%)** | 193 (3,4%) |
| gênero explícito / implícito | 1 (falso pos.) / 0 | 0 / 0 | 0 / 0 |
| pendências · minutos | 5 · 104,1 | 2 · 30,2 | 2 · 30,4 |

**Os dois modelos erram em eixos OPOSTOS**: o mistral inverte o sentido, a aya erra ortografia.
E erro de acento é corrigível por máquina; erro de sentido não é — por isso o
`NormalizadorAcentosComuns` foi ampliado (o eixo em que a aya perdia deixa de existir para as
PRÓXIMAS traduções; os 197 já gravados só somem com reprocessamento).

**O achatamento não mudou a tradução** (183 × 181, 30,4 × 30,2 min, 2 × 2 pendências). Ele só
limpa o arquivo: 9.076 → 6.582 eventos, 0,90 → 0,67 MB, karaokê perde a animação e mantém a
letra. Vale nas obras da família DanMachi, onde a proteção nominal não cobre; no Unicorn e nos
Gundam é trabalho sem retorno.

## FECHADO COM ARTEFATO (2026-08-12)

- `b78a5cf6` recusa do LLM (4xx) deixa de abortar o episódio; disjuntor de 3 recusas seguidas.
- `84296493` a Tradução Local também dizia "iniciada" para pasta inexistente; catraca passou a
  exigir o COMPORTAMENTO (conferir ANTES do disparo) em vez da presença de uma classe.
- `43eeb8b0` + `f9070136` medição mistral × aya × achatado no artefato, 3 calibragens.
- `60d84e5b` onde o achatamento seria a única proteção, no acervo inteiro.
- `06645ed9` pergunta que virou afirmação — o 1º instrumento para erro FLUENTE.
- `5586ca40` negação, acentuação, gênero explícito + normalizador de acentos ampliado.
- `3230692b` gênero IMPLÍCITO lendo a ficha de personagem do contexto de produção.
- `47b47cb7` boa-fé: traduzir A PARTIR da pasta de saída deixa de ser possível.
- `b7abf431` retomada após interrupção: "salvas para retomar" vira fato provado.
- `74bc45d3` o corretor de concordância CORRIGE — provado até o byte, sem LLM. E a lente
  adversarial achou o furo: o detector normaliza `\N` e enxerga "Minha\Nmãe"; o CORRETOR recebia
  o texto cru e não alcançava. Ganho no acervo **ZERO** e declarado (2 corrigíveis, os mesmos 2).

Suíte **1688 testes, 0 falhas, 0 erros**. Portão do projeto **rc=0**.

## OS DOIS 🔴 FECHADOS NO MECANISMO (`dd87f53d`) — e o que continua aberto

Cada gap era duas perguntas coladas. Separá-las fechou metade sem depender da infraestrutura:

| gap | mecanismo funciona? | qual modelo se sai melhor? |
|---|---|---|
| segunda opinião | ✅ `SegundaOpiniaoModeloRecuperacaoTest` (3 casos) | 🔴 precisa do LM Studio |
| correção via LLM | ✅ `CorrecaoViaLlmChegaAoArquivoTest` (3 casos) | 🔴 precisa do LM Studio |

O caso-controle da segunda opinião é a fala REAL do Zeta (`I just said, "You want to meet Char,
don't you?"`). Os testes de "ligado" e "desligado" se calibram mutuamente: só muda a config e o
desfecho inverte — mecanismo morto daria o mesmo nos dois.

**Para exercitar com modelo real, falta uma DECISÃO sua**: qual segundo modelo carregar. O
`application.yml` traz `modelo-recuperacao: ""` (desligado, falha fechada). O tower recuperou 3
das 6 do Zeta em 11/08; a aya nunca foi testada nesse papel. Com dois modelos carregados o KRONOS
pega o primeiro — ver [[confronto-modelos-danmachi-virgem]].

## DUAS LACUNAS DE DETECÇÃO REAIS, AMBAS COM INCIDÊNCIA ZERO NO ACERVO

Padrão que apareceu duas vezes hoje: o mecanismo tem furo provado por controle positivo, e o
acervo não tem o caso. As duas ficam declaradas e NÃO corrigidas — alargar padrão de concordância
sem prejuízo medido é como os 3 falsos positivos consertados hoje de manhã (regra 14).

- **quebra `\N` colada** — corrigida no CORRETOR (`74bc45d3`) porque o custo era zero; cegueira
  residual medida = 0 de 18.940 falas com `\N`.
- **advérbio entre verbo e particípio** — `"Ela está muito cansado"` NÃO é detectada, porque
  `ELA_COM_PREDICADO_MASC` exige o particípio colado ao verbo. Medido: **0 defeitos escondidos**
  em 1.324 falas com verbo+advérbio. `MedicaoAdverbioEntreVerboEParticipioIT` guarda o número.
  Descoberta por um teste REPROVANDO, não por leitura de código.

## O QUE AINDA NÃO FOI PROVADO NO CORRETOR (a pergunta do Paulo, respondida pela metade)

`CorrecaoChegaAoArquivoTest` prova o elo **determinístico**. O que segue sem prova é a correção
via **LLM** — que é a única rota para a maioria das classes de defeito, porque a regra local só
cobre parentesco, "graças a Deus", insulto forte e o artigo de mobile suit. Concordância nominal
("Ela está cansado") é DETECTADA e não tem conserto local: depende do modelo.

Alvo pronto para fechar isso: as 6 pendências do Zeta + o `modelo-recuperacao`, que segue 🔴.

## DÍVIDA CONHECIDA E DATADA — extrair os pronomes cruzados (decidido em 12/08 NÃO fazer agora)

A quebra do `DetectorConcordanciaService` parou com 4 das 5 famílias extraídas para
`raspagemRevisao/application/concordancia/`. Falta o grupo de **pronomes cruzados**.

**Por que paramos, com número:** o que motivou a quebra era manutenção, e isso já foi
atendido — o maior método saiu de **124 para 41 linhas**, e o grupo já está em 6 métodos
nomeados com a cicatriz de cada um no Javadoc. O que sobra é arrumação.

Os três cortes feitos eram blocos soltos. Este é ENTRELAÇADO: `PREPOSICOES_OBJETO`,
`VERBOS_TRANSITIVOS_DIRETOS` e `VERBOS_SUJEITO` alimentam 3 pontos cada;
`removerPredicadoDePrimeiraSegundaPessoa` tem 4 chamadas; `adicionarSeEncontrado`, 15. E é
exatamente o código das TRÊS correções de 12/08 — recém-afinado, sem defeito aberto, que
levou a varredura do Unicorn de 3 falsos positivos a 0.

**Gatilho para fazer:** quando alguém precisar mexer numa regra de pronomes (nova cicatriz,
novo falso positivo). Aí já se está lendo aquele código com atenção e a extração sai junto.

**Mapa pronto:** mover `PREPOSICOES_OBJETO`, `VERBOS_TRANSITIVOS_DIRETOS`, `VERBOS_SUJEITO`,
os 4 padrões de objeto/imperativo/regência, os 4 de abertura, os 2 de sujeito,
`ELE_ISOLADO`/`ELA_ISOLADA` e os dois `remover*`. A fachada cai de 518 para ~250 linhas.

## GAPS E BLOQUEIOS REAIS

- 🔴 `modelo-recuperacao` nunca exercitado com a aya (ver PRÓXIMA AÇÃO).
- 🟡 Sem teste: **idempotência da retomada** (rodar 2× produz o mesmo?) e **clique duplo na
  rota de traduzir** (a fila recusa com 409, caracterizado só para o renomeador). Nenhum dos
  dois tem dano registrado — por isso ficaram atrás dos três que tinham cicatriz.
- 🟡 `tradutor.fallback-online.ativo: true` com o comentário acima dizendo "desligado por
  padrão". Decisão de produto: corrigir o valor ou o comentário.
- 🟡 Lacuna declarada: estilo com nome próprio de canção e SEM `\k` (`RISE LIGHT RISE English`,
  `Logo`) segue invisível aos detectores. Fechar por nome exigiria lista por obra.
- 🟡 O piso dos instrumentos novos: nenhum julga se o português está BOM, só se está errado de
  forma mecânica. `"Audrey é o piloto"` não é acusado (substantivo tem gênero próprio).

## NÃO REPETIR

- **Editar `src/main` ou rodar Gradle com job em andamento.** O quarkusDev observa
  `build/classes`; o reload matou o E01 do Unicorn e criou um "defeito da aya" que era meu.
- Medir versão achatada contra a entrada ORIGINAL: 2.898 eventos a menos desalinham o
  pareamento por índice e produzem 77,7% onde o real é 0,5%. Cada saída contra A SUA entrada.
- Aceitar número de instrumento novo sem controle positivo E negativo. Nesta sessão os
  controles pegaram: `can't` (a alternância `can` come o `n`), tag question exigindo `?` no
  fim, `ja[a-z]` casando `jaz`, `[ée]` casando a conjunção "e", janela de 40 chars ligando
  particípio a sujeito distante, e um teste de retomada que concluía sem interromper.
- Reimplementar em script critério que a produção já tem. O "é musical?" veio de
  `PadraoEstiloMusical` + lista nominal do yml, via teste JUnit; o PowerShell só colheu fato bruto.
- `Get-Content` em caminho com `[Sokudo]`: colchete é classe de caracteres. Usar `-LiteralPath`.
