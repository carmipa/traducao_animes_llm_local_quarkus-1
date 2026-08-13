# CONTINUIDADE — KRONOS

TAREFA ORIGINAL: escolher o modelo LLM titular do pipeline, e qual serve para CORRIGIR
                 as falas que o titular abandona.
OBJETIVO FINAL:  decisão com artefato, não com impressão.
CRITÉRIO DE ENCERRAMENTO: leitura da qualidade do português + teste do modelo de recuperação.
BRANCH / COMMIT BASE: main — `b7abf431`, árvore limpa, ahead de origin/main
SHA-256 DOS DOCUMENTOS-REGRA: ENGENHARIA f43d9c05… (1136) · REGRA-DO-DOCKER 98a5ad6a… (2402)

## PRÓXIMA IDEIA COM DANO MEDIDO — os dicionários no KARAOKÊ (Paulo, 13/08)

Fecha um prejuízo já pago: o `NormalizadorAcentosComuns` transformou `mae` em `mãe` **100 vezes
nos 50 episódios**, e `mae` é romaji (前), não português. Ver
[[karaoke-corretor-traduz-e-acento-ima-no-romaji]].

**O dicionário de romaji resolve** — medido em 13/08:

```
palavra   PT     EN     ROMAJI
mae       nao    nao    SIM     <- exatamente o caso do dano
kimi      nao    nao    SIM        kokoro, yume, hikari, tsubasa, kaze: idem
```

**MAS a mesma medição achou a armadilha, e ela inverte o dano:**

```
sora      SIM em PT   e SIM em ROMAJI
nada      SIM em PT   e SIM em ROMAJI
sera      SIM em PT   e SIM em ROMAJI
```

Ligar o romaji sem cuidado deixaria de corrigir `sera → será` em fala legítima em português.

**O desenho certo: o ESTILO decide, o dicionário confirma.**

```
linha de estilo musical/romaji  ->  romaji manda, NÃO acentuar
linha de diálogo                ->  português manda, como hoje
```

O projeto já classifica isso (`PadraoEstiloMusical`, `DetectorEfeitoKaraokeService`, pareamento
de camadas). O dicionário entra como confirmação — numa linha já reconhecida como romaji, ele
explica POR QUE `mae` não pode ser tocada. Nunca como decisor sozinho.

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

## ACHADO 13/08 17:33 — o corretor NAO alcanca o karaoke, com caso concreto

Rodada do Paulo: traducao_ptbr (17:20) + traducao_ptbr-karaoke-ptbr (17:33).

CONFIRMADO no dialogo: 440 -> 92 ocorrencias de erro ortografico (79% menos), com o
mesmo instrumento e o mesmo recorte (so estilo Default) da medicao de ontem.

CONFIRMADO no romaji: o estilo ED do Unicorn e romaji e saiu INTACTO —
"Furi dake no kotae to eeru tai de ensou o tsunagu". Nenhum acento foi imposto.
ATENCAO: procurei por "mae" e nao achei, mas isso e AUSENCIA DO CASO, nao prova de
protecao — o Unicorn nao tem essa palavra. O dano dos 100 "mae" foi em outra obra.

DEFEITO ACHADO, estilo "ED - EN" no E01:
    "Este sentimento e falso?"        <- deveria ser "é"
O corretor ortografico esta plugado SO no ProcessarArquivoUseCase; o karaoke tem use case
proprio e nao passa por ele. E a confirmacao pratica da ideia do Paulo, agora com endereco.

Tambem observado: a camada "ED - EN" (letra em ingles do encerramento) foi TRADUZIDA para
portugues. Se e intencional no menu de karaoke, ok; se nao, e a camada errada sendo
traduzida — conferir antes de mexer.


### CORRECAO 13/08 — o "ED - EN" traduzido e CORRETO (Paulo). Romaji NUNCA traduz; karaoke
### INGLES traduz. A duvida que levantei era minha, nao defeito.

MEDIDO por estilo no Unicorn (traducao_ptbr-karaoke-ptbr):

  estilo    linhas  fora-do-PT  natureza                        acao
  ED           165        396   ROMAJI (kieta, tsunagu, daita)  NAO tocar — outro idioma
  ED - EN      165         11   portugues traduzido (nao)       CORRIGIR
  ED2          207         77   portugues traduzido (tras,      CORRIGIR
                                mascara, nao, voce)
  OPL2        3255       3381   KFX silaba a silaba (feel,      NAO tocar — nem e frase
                                lone, hear) — nao e traduzivel
  Sign          23          0   ok

DEFEITO REAL DIMENSIONADO: 88 ocorrencias de acento faltando em 372 linhas de karaoke JA
traduzidas para portugues. O corretor esta plugado so no ProcessarArquivoUseCase; o
karaoke tem use case proprio.

DETALHE QUE IMPORTA NA IMPLEMENTACAO: ED e OPL2 ficam de fora por motivos DIFERENTES — um
e outro idioma, o outro nem e texto completo (fragmento de silaba). Mesmo criterio para os
dois nao serve.


### AMPLIACAO (Paulo, 13/08): no karaoke sao os QUATRO dicionarios, nao so o pt_BR

Musica de anime mistura idioma na MESMA linha. Evidencia no proprio Unicorn: o ED2 tem
"gonna" e "one" convivendo com portugues correto. E o projeto ja registrou code-switch real
no Gundam 08th ("Song JP"). Ver [[karaoke-86-sem-codeswitching]].

Consultar so o portugues no karaoke produziria o dano inverso do atual: "corrigir" a letra
da musica. O classificador completo ja existe e resolve —
core.texto.dicionarioOrtografia.ClassificadorQuatroIdiomas.

  palavra valida em PT        -> ok
  sem acento, existe com      -> CORRIGIR
  valida em EN                -> preservar (e a letra)
  valida em DE                -> preservar (anime usa alemao a beca)
  valida em ROMAJI            -> preservar (mae, kimi, kokoro)
  nenhum idioma reconhece     -> nao tocar, so reportar

A DIFERENCA ENTRE AS FATIAS, e ela e o ponto: no dialogo, palavra em ingles provavelmente e
RESIDUO de traducao; no karaoke, e A LETRA. Mesmo dicionario, decisao oposta. Quem decide e
a fatia; o dicionario so informa — que e o motivo de ele viver no core sem acoplar ninguem.


### O PRINCIPIO QUE REGE A IMPLEMENTACAO NO KARAOKE (Paulo, 13/08)

"A mesma regra da traducao tem de ser mantida, mas aqui em PRESERVACAO do karaoke."

As garantias sao as mesmas; o DEFAULT se inverte. Na traducao o pipeline AGE e as guardas
seguram o exagero. No karaoke o default e NAO TOCAR, e so se mexe no inequivoco — porque
erro ali nao e palavra torta, e animacao quebrada: timing por silaba, camadas pareadas, KFX.

  garantia                      karaoke
  falha fechada, 3 estados      igual — sem dicionario nao corrige e DECLARA
  nao perder fala               igual — contar linhas antes e depois
  backup antes de sobrescrever  igual
  tag e \N byte a byte          igual, e vale dobrado: \k e timing sao intocaveis
  NA DUVIDA                     traducao tenta; KARAOKE PRESERVA

O criterio do CorretorAcentoPorDicionario ja e conservador do jeito certo: so aceita a
sugestao que e A MESMA PALAVRA ACENTUADA. "mascara -> mascara-com-acento" entra;
"mascara -> mascarada" nunca. Aplicado ao karaoke, mexe nas 88 ocorrencias medidas e passa
ao largo de kieta, gonna, Nordlicht e das silabas do OPL2.


#### CORRECAO na regra do alemao (Paulo, 13/08): depende da FAIXA

Eu havia registrado "DE -> preservar" para o karaoke. ERRADO. Na faixa TRADUZIDA o alemao
remanescente e traducao que faltou, nao termo a proteger.

  dialogo             DE -> PRESERVAR   e lore (Nerv, Seele, Nordlicht, Kamille)
  karaoke EN/ED2      DE -> SINALIZAR   a faixa existe para virar portugues; alemao ali e
                                        pendencia. Excecao: nome de lore, que a protecao de
                                        termos ja cobre antes.

E RARO, e por isso a acao e SINALIZAR e nao construir mecanismo: o corretor ortografico
repoe acento, nao traduz. Quem traduz e o LLM; palavra alema perdida numa letra e caso de
RELATORIO, nao de automacao nova. Mecanismo para caso raro e cerimonia (regra 22).


## DEFEITO ABERTO 13/08 — a TROCA DE FONTE nao ajusta o TAMANHO, e o texto estoura

Paulo mostrou a tela: linhas sobrepostas e texto gigante no encerramento do Unicorn.

MEDIDO em traducao_ptbr-karaoke-ptbr, E01 (PlayResX/Y = 1920x1080):

  estilo      fonte   tamanho   margens L/R/V
  Default     Arial      70        2/0/0      <- V=0 joga na borda
  Sign        Arial     150
  OPL2        Arial     130
  ED          Arial      75
  ED - EN     Arial      60

Em 1920x1080 o normal e 40-60. A troca de fonte COPIOU o tamanho numerico da fonte
original (Althea, Androgyne, Dash Horizon, Gandhi Sans) para Arial — e Arial tem metrica
MUITO maior. O numero foi mantido, a proporcao nao.

Efeito somado: ED (75) e ED - EN (60) desenham quase no mesmo lugar e as duas linhas se
atropelam. E o "Andlmt" embolado da captura.

O QUE INVESTIGAR ANTES DE MEXER:
  1. A troca de fonte deve ajustar o tamanho proporcionalmente a metrica da fonte nova, ou
     manter o tamanho e sofrer isso? Decisao de produto.
  2. Margem V=0 no Default e intencional ou efeito colateral da troca?
  3. Conferir de qual EPISODIO e a captura: no E01 medido o "ED - EN" esta em PORTUGUES
     ("Faça uma pequena pausa"), mas a tela mostra ingles ("And Im calling calling") — pode
     ser outro episodio com camada nao traduzida.

NAO MEXER sem responder as tres. Trocar tamanho de fonte no acervo inteiro e irreversivel
sem backup, e o efeito so aparece assistindo.


### CORRECAO ao que registrei antes: o OPL2 NAO e intraduzivel — 17 das 155 sao frases

Eu havia escrito "OPL2 -> nao tocar, nem e frase". ERRADO, e o Paulo mostrou na tela: a
abertura do E01 aparece em INGLES por cima dos creditos.

MEDIDO no E01 (entrada):
  OPL2: 155 linhas
     17 com espaco  = FRASES COMPLETAS ("And Im calling calling out your name again")
    138 palavra unica = fragmentos de KFX ("Do", "you", "feel", "lone")

As 17 sao a letra e SAO traduziveis — sao exatamente as que sobram quando a musica e
achatada. Os 138 e que devem ser pulados. Hoje as 155 ficam em ingles.

DIAGNOSTICO DA TELA DO PAULO — dois defeitos SOMADOS:
  1. fonte: Arial 130 no OPL2 onde cabia ~50 (a troca copiou o tamanho da fonte original)
  2. as 17 frases NAO traduzidas desenhando junto com os 138 fragmentos, todos gigantes
     -> "And Im calling calling" sobreposto a "And out your name again"

O E01 do Unicorn tem karaoke TOTALMENTE em ingles na abertura (Paulo), entao nao ha camada
romaji ali para preservar — tudo o que e frase deveria virar portugues.

O resto do E01 esta CERTO, conferido entrada x saida:
  ED        romaji -> romaji            preservado, correto
  ED - EN   ingles -> portugues         traduzido, correto
  Sign      DEPARTURE 0096 -> Partida 0096

CRITERIO SUGERIDO (nao implementado): dentro de estilo musical, linha COM espaco e frase e
vai ao LLM; linha de palavra unica e fragmento de KFX e fica. Simples, mensuravel, e
separa 17 de 138 no E01 sem depender de nome de estilo.


### CONFIRMADO NA TELA 13/08 — o ENCERRAMENTO do E01 ficou perfeito

Paulo assistiu o MKV remuxado. O ED do E01 mostra as duas camadas nos lugares certos:

    "Faça uma pequena pausa."                       <- ED - EN, traduzido
    "Furi dake no kotae to eeru tai de
     ensou o tsunagu"                               <- ED, romaji PRESERVADO

Sem sobreposicao, sem estouro de fonte, romaji intacto. E a prova em video de que o
desenho funciona — traduz a camada inglesa e nao toca na japonesa —, e de que o dano dos
100 "mae" nao se repetiu.

DELIMITA O DEFEITO: o problema NAO e o karaoke em geral, e so a ABERTURA (OPL2), onde as
17 frases ficaram em ingles e desenham junto com os 138 fragmentos em Arial 130. O ED, o
ED - EN e o Sign estao corretos.


## DEFEITO GRAVE 13/08 — o ROMAJI foi traduzido numa linha, com o texto do ED - EN

Paulo viu na tela: a mesma frase em portugues em CIMA e EMBAIXO, ao mesmo tempo.

EVIDENCIA no E01 (traducao_ptbr-karaoke-ptbr):

    ED         0:23:09.95   Tire minha roupa e coroa, entao posso dormir profundamente.
    ED - EN    0:23:09.95   Tire minha roupa e coroa, entao posso dormir profundamente.

MESMO texto, MESMO instante, nas DUAS camadas. E aos 0:22:16 o mesmo estilo ED esta
CORRETO, em romaji ("Furi dake no kotae to eeru tai de\Nensou o tsunagu").

Logo: NAO e o estilo inteiro que vaza — e uma LINHA. Isso aponta para o reaproveitamento
por texto visivel do TraduzirKaraokeUseCase ("Mesma letra, moldura diferente: reaproveita a
traducao ja obtida", traducaoPorTextoVisivel). Se a linha do romaji e a do ingles colidem
nessa chave, a traducao do ingles e aplicada NAS DUAS.

E o que explica a contradicao aparente das duas capturas: aos 22:16 o encerramento esta
perfeito (romaji preservado), aos 23:09 esta duplicado.

INVESTIGAR (nao feito): por que as duas linhas colidem na chave de texto visivel. Hipotese
a testar primeiro — TextoSemTags.decompor devolvendo vazio ou igual para as duas, fazendo o
putIfAbsent casar linhas que nao sao a mesma letra.

RISCO: e o dano de sobrescrever camada original, da mesma familia dos 100 "mae". Aqui e
pior, porque perde a letra em romaji inteira naquela linha.


## O DEFEITO CENTRAL (Paulo, 13/08): nao preserva a faixa ORIGINAL quando ha UMA camada so

"ele nao ta preservando a faixa de karaoke original, esta traduzindo tudo ou nao traduzindo"

E13, encerramento — MEDIDO entrada x saida:

    ED2    "Behind your mask"   ->  "Por tras da sua mascara"   traduziu e a ORIGINAL SUMIU
    OPL2   "Do you feel alone"  ->  "Do you feel alone"         nao traduziu nada
    (o E13 NAO tem camada ED em romaji)

A DIFERENCA ENTRE OS EPISODIOS EXPLICA TUDO:
  E01: DUAS camadas (ED romaji + ED - EN ingles) -> traduz uma, preserva a outra. Funciona.
  E13: UMA camada so (ED2 ingles)                -> traduziu e a original desapareceu.

Com uma camada so, o correto e DUPLICAR — manter a original e acrescentar a traducao
embaixo, que e a promessa do modo "romaji em cima, PT-BR embaixo". Substituir apaga o
karaoke da tela.

OS TRES DEFEITOS SAO O MESMO ERRO POR LADOS OPOSTOS:
  ED2 do E13        substituiu a original      -> devia preservar E acrescentar
  ED do E01 23:09   substituiu o romaji        -> devia preservar
  OPL2              nao traduziu nada          -> devia traduzir as 17 frases

DETALHE QUE DATA A SAIDA: "Por tras da sua mascara" esta sem acento em "tras" — e a linha
que virou caso de teste. Confirma que esta saida e ANTERIOR a correcao movida para o ponto
certo (eventosFinais.add), e que a app no ar ainda e a de 17:52.

