# CONTINUIDADE — KRONOS

# ✅ EXECUTADO (2026-08-19) — F1 a F5 do Plano Mestre do critério "isto é música?"

`gradlew test --rerun-tasks` = **BUILD SUCCESSFUL** (2.015 testes na última contagem, 35 pulados).

## Efeito MEDIDO no acervo, com o código consertado e o instrumento fiel à produção

```
                     ANTES        DEPOIS
FORA_DE_MUSICA     344.174       501.204
EFEITO_KFX         761.338     1.281.361
ORIGINAL_JAPONES   868.288       319.426
JA_PORTUGUES         2.637         1.141
TRADUZIVEL_INGLES  165.827        39.132     <- -76,4%
Char's Counterattack 107.384           84     <- o dialogo do filme saiu
```

## O que cada fase entregou

- **F1** `CriterioDeMusicaCaracterizacaoTest` — congelou o defeito com linhas REAIS do acervo.
- **F2** régua de evidência positiva em `ClassificadorLetraKaraokeService.classificar(estilo,
  texto, SinaisDeKaraoke)`. `eEfeitoKaraoke` **saiu** da pergunta "é música" e **continua** na
  pergunta "é sílaba ou frase". Novo record `SinaisDeKaraoke` (campo `Effect` + romaji no mesmo
  instante). `TraduzirKaraokeUseCase` passa os sinais; o pré-passe usa SÓ o `Effect`, para a
  regra não se alimentar da própria conclusão.
- **F3** casos-controle, com as 3 asserções invertidas e o motivo escrito ao lado. Também
  **fortalecida** uma asserção que passou a ser cega: `assertNotEquals(EFEITO_KFX, ...)` ficava
  verde quando a resposta virava `FORA_DE_MUSICA`, que é igualmente errado ali.
- **F4** evidência (e): estilo que declara PAPEL DE CAMADA (`English`/`Romaji`/`Kanji`/`Lyrics`).
  Recupera `Hey World English` 1.150 e `RISE LIGHT RISE English` 927. **Junto** veio a guarda de
  comando de desenho — sem ela a F4 traria de volta 10 eventos de traçado vetorial.
- **F5** `entradasCacheDescartadas` no manifesto e no console. A limpeza do cache já era
  automática (o ramo `TRADUZIVEL` é o único que consulta cache, e `salvarCache` regrava só o
  aplicado); o que faltava era o NÚMERO, senão cache que encolhe parece perda de dado.

## Três medições que corrigiram o próprio plano

1. **A perda declarada de ~2.400 era ~272.** `NipSlip`, `Paradise`, `HestiaFamilia` e `EG` não
   são música — são cartaz de piada, nome de lugar e traçado vetorial. Só `Hey World English`
   era letra. Superestimei em 9×.
2. **550.022 linhas do DanMachi mudaram de `ORIGINAL_JAPONES` para `EFEITO_KFX`** — e é
   CORREÇÃO, não regressão: são comandos de desenho (`m 165 -450 l 416 -450`, `b` de Bézier)
   que o código antigo contava como "romaji a preservar". O arquivo saía igual; os contadores
   mentiam em meio milhão de linhas.
3. **O primeiro instrumento de medição estava errado**: chamava a forma de 2 argumentos, que não
   recebe os sinais, e portanto NÃO media o que a produção faz. Refeito em `MapaKaraoke2`.

## Aberto, declarado

- Abertura da Part 2 do 86: letra a letra, sem camada de frase, bilíngue na mesma janela.
- Quebra do use case em classes menores (`classificar` ainda roda 2× por evento, linhas 388/404).
- Efeito do acento nunca conferido em `.ass` (verde em teste, sem execução).
- Nenhuma execução real de karaokê foi feita depois do conserto — o efeito acima é do
  classificador, não do arquivo final.

---

# ▶▶ PLANO MESTRE (2026-08-19) — o registro de como se chegou aqui

AUTORIZADO por Paulo. Escopo FECHADO. Ordem combinada: **conserto primeiro, quebra em classes
menores depois.**

## Problema, medido sobre o acervo inteiro (726 `.ass`, 0 erro de leitura)

O classificador de produção, rodado sobre todo o acervo, devolve **165.827 TRADUZIVEL_INGLES**.
Destes, **133.951 (80,8%) entram SÓ pela assinatura de efeito** — nem o estilo diz música, nem
existe tag `\k`. A causa é `DetectorEfeitoKaraokeService.eSaidaDeTemplateKaraoke`, que trata
"posicionamento complexo + alta densidade de tags" como prova de karaokê. Isso é assinatura de
TIPOGRAFIA, não de música.

O que entra por engano, por estilo:
`Char's Counterattack` 106.692 (estilo de DIÁLOGO do filme) · `Signs` 9.213 ·
`Zeta Episode Title` 6.923 · `Main Title` 4.129 · `Logo` 1.961 · `Mobile Suit Gundam` 644.

## INVARIANTE (regra 16)

```
ID:      INV-KARAOKE-001
Nome:    so vai ao LLM de karaoke o que tem evidencia POSITIVA de musica
Dano:    dialogo traduzido com prompt de karaoke e empilhado ingles\Nportugues na tela
         Medido: 106.692 eventos so no Char's Counterattack
Camadas: classificador (decisao) · caso-controle (guarda) · manifesto (telemetria)
Testes:  CASO DOENTE = CCA com clip retangular · CASO SAO = 86 Opening
```

## A RÉGUA — QUATRO evidências (a quarta é contribuição de Paulo, 19/08)

É karaokê se — e só se — **(a)** tem tag `\k`, **(b)** o estilo declara música,
**(c)** o campo **`Effect` do ASS** marca karaokê (`fx`, `Effector [fx]`, `karaoke`, `template`),
**ou (d)** existe camada `ORIGINAL_JAPONES` no MESMO instante do arquivo.

E só DEPOIS vem a segunda pergunta, que o classificador já responde razoavelmente:
**sendo karaokê, é japonês/romaji (preserva) ou inglês (traduz)?** Separar as duas perguntas é
o que estava faltando — hoje elas estão fundidas num único `indicaMusica`.

### A pista de Paulo, medida

O campo `Effect` é o 9º da linha `Dialogue:` e o Kara Templater do Aegisub o preenche nas linhas
que gera. **O classificador nunca olhou esse campo.** Cruzamento no acervo:

```
EFFECT            traduzivel    romaji/JP         KFX
fx                     1.344      838.021      566.479
(vazio)              164.065       30.237      148.432
karaoke                  378            0            0

Os 133.951 FALSOS POSITIVOS, por campo Effect:
   133.691  (vazio)   <- 99,8%
       258  fx        <- OPL2, musica de verdade
```

### As duas réguas comparadas — e por que ficam as DUAS

```
REGUA POR EFFECT (Paulo):   fica 32.134  |  sai 133.693
REGUA POR INSTANTE (minha): fica 32.159  |  sai 133.668
so a de Paulo salva: 258 (OPL2)   |   so a minha salva: 283
```

Removem o mesmo lixo e **salvam conjuntos diferentes**. Juntas salvam 541 linhas que qualquer
uma sozinha perderia. Por isso a régua final tem quatro evidências, não três.

**PERDA RESIDUAL DECLARADA ~2.400**, em estilos que são NOME DE MÚSICA com `Effect` vazio e sem
camada romaji: `NipSlip` 762, `Paradise` 648 (outras 864 ficam), `HestiaFamilia` 408, `EG` 348,
`Hey World English` 272. É o gap já registrado ("estilo de karaokê com nome da música é
invisível") e é o alvo da F4.

## Duas hipóteses MINHAS que a medição DERRUBOU — não reabrir

1. **"O CCA é comando de desenho vetorial."** FALSO: 0% de `\p1..9`, 0% sem letra. São falas
   reais, 1.786 textos distintos repetidos 31× em camadas.
2. **"Clip vetorial separa tipografia de karaokê."** FALSO: só 5,6% dos falsos positivos têm
   clip vetorial; **82,7% usam clip RETANGULAR — a mesma forma do karaokê do 86.**

## Refino importante do desenho

A régua precisa de contexto de ARQUIVO (o conjunto de instantes com romaji), e o use case **já
calcula esse conjunto** — `instantesComOriginalPreservada`, linha 388. Ele só o usa para
empilhar, não para decidir. O conserto reaproveita o pré-passe existente e **não depende** da
quebra em classes.

### Por que as QUATRO evidências, e não a melhor delas (medido em 19/08)

O campo `Effect` **não existe em toda obra**: no 86 ele está vazio nas 159.398 linhas, e no
Guilty Crown também. O `fx` vem de DanMachi, Zeta e ZZ. Ou seja: a evidência de Paulo cobre
umas obras, a de instante cobre outras. **Elas se cobrem em obras diferentes, não só em linhas
diferentes** — é isso que torna as quatro necessárias, e não redundantes.

Contexto do achatador (Paulo, 19/08): ele nasceu por causa das animações, com casos em que os
vetores fizeram uma tradução durar **9 horas**. O achatamento normaliza estilo e fonte — e o
efeito colateral é apagar a distinção `OP - Romaji` × `OP - English`, que no 86 vira `Opening`
para as duas camadas. É por isso que ali a decisão romaji×inglês cai na heurística de texto.

### CUSTO — e a honestidade sobre ele

```
HOJE   165.827 eventos -> 1.267 chamadas ao LLM  (repeticao 130,9x)
REGUA   32.417 eventos ->   721 chamadas         (repeticao  45,0x)
```

As 9 horas já foram resolvidas pelo **dedup por texto visível**, que absorve 130× de repetição.
A régua corta mais 43,1% das chamadas — bom, mas **o ganho dela é CORREÇÃO, não tempo**:
133.410 eventos que deixam de ser corrompidos. Não vender velocidade como motivo.

## FASES

```
F1  congelar o comportamento de HOJE num teste de caracterizacao (baseline)
F2  a regua no classificador, alimentada pelo pre-passe que ja existe
F3  casos-controle: CCA reprova · 86 Opening passa · adulteracao nega
F4  recuperar as perdas: nome de musica no estilo (gap ja registrado)
F5  cache: marcar como suspeito o decidido pelo classificador velho
```

## FORA DE ESCOPO (declarado, não esquecido)

- Quebra do use case em classes menores — **próxima frente, já combinada com Paulo**. O
  argumento do JIT foi MEDIDO e **não se sustenta**: todo método do caminho quente já está
  abaixo de 325 bytecodes (`classificar` 219, `classificarPorEvidenciaDeTexto` 207,
  `extrairTextoVisivel` 43). O 1064 que eu tinha visto era o `static {};`, que roda uma vez.
  O motivo que sustenta a quebra é testabilidade — e o desperdício real medido:
  **`classificar` é chamado DUAS vezes por evento** (linhas 388 e 395).
- Abertura da Part 2 do 86 (letra a letra, sem camada de frase, bilíngue na mesma janela).
- Ligar `protecao-romaji-pareamento` (`application.yml:163` = `false`), que pertence a `traducao`.

## RISCOS PELAS TRÊS LENTES

- **Boa-fé:** rodar karaokê sobre pasta de SAÍDA faz o pré-passe ver romaji já empilhado, e a
  régua passaria a proteger o que não deve. Vira caso-controle na F3.
- **Falha operacional:** arquivo sem nenhuma camada romaji (Part 2 do 86) perde a evidência (c);
  sobra (a) e (b). É o caso já aberto — lacuna conhecida, não silenciosa.
- **Adversarial:** estilo renomeado contendo "op"/"ed" vira música. Já é verdade hoje; a régua
  não piora.

## ESTADO DA FATIA HOJE (medido em 19/08)

1.976 linhas de produção em 13 classes · 1.253 de teste · **104 testes de karaokê, 0 falhas**,
4 pulados. Manifesto grava 27 campos (`statusFinal`, `estadoDicionario` de 3 estados,
`arquivosComFalha[]`, `acentosRepostos`). Cobertura real: **2 obras de 21** (86 e 08th MS Team).
Cache: 8.817 entradas, e **184 dos 230 arquivos são anteriores ao último conserto do
classificador (13/08)** — carregam decisões do classificador velho e voltam na reexecução.

---

## ▶ PRÓXIMA AÇÃO EXECUTÁVEL EXATA (2026-08-19, noite — documentação)

**Nenhuma pendente na documentação.** A tarefa de 19/08 à noite ("atualize totalmente a
documentação") foi entregue e provada. Se a sessão retomar, a fila real é a do acervo, mais
abaixo neste arquivo (acento: 699 falas; termo de lore: 127 — as duas com ferramenta pronta e
trava dupla, aguardando decisão de Paulo).

---

# ▶ ADENDO — SUBMENU "ESQUELETO DO PROJETO" (pedido de Paulo, mesma noite)

Página nova no submenu **Fundamentos** da Documentação: a árvore completa do código em diagrama
de linhas — todo pacote, toda pasta e o nome de **todas as 839 classes** —, mais um mapa mermaid
colorido dos grupos → peers → infra.

**A página é GERADA do disco, e o gerador é a própria catraca**
(`CatracaEsqueletoDoProjetoAtualizadoTest`, 2 testes):

```
regravar:  gradlew test --tests "*CatracaEsqueletoDoProjetoAtualizadoTest*" -Dkronos.esqueleto.regravar=true
conferir:  roda junto com a suite; divergiu, reprova apontando a primeira linha
```

A chave `kronos.esqueleto.regravar` entrou na lista de propagação do `build.gradle` — sem ela o
`-D` fica na JVM do Gradle e o "regravei" teria a mesma cara de "não mudou nada", cicatriz que a
própria lista já documentava.

**Calibração:** troquei uma classe por um nome inventado na página → 1 de 2 testes reprovou,
apontando `linha 511: na pagina ResultadoConcordanciaINVENTADA.java / no disco
ResultadoConcordancia.java`. Restaurada, volta verde.

**Três defeitos do meu próprio gerador, achados antes de virar entrega:**

1. **id de nó por `hash()`** (versão Python): `hash` é randomizado por processo, e o arquivo
   mudaria sozinho a cada regeração — churn com cara de alteração. Virou índice do grupo.
2. **4 classes descartadas em silêncio**: as que moram na raiz de `org.traducao.projeto`
   (`WebInterfaceTest`, `ApiControllerTest`, `ApiEndpointsTest`, `SseConsoleDinamicoTest`) caíam
   fora da varredura. Consertado com nó `(raiz do pacote)` — e entrou um **segundo teste** que
   compara duas contagens independentes do mesmo alvo, para "perder classe pelo caminho" não
   voltar a ser silencioso.
3. **Diagrama ilegível**: com os rótulos numa linha só, o mermaid espremeu o SVG em 796×**67 px**,
   caixas de 13 px. Um pacote por linha: 796×**273 px**, caixas de 64 px.

**Provado no navegador:** o item aparece em Fundamentos, a página abre, 1 SVG mermaid, 58 blocos
de árvore e **839 ocorrências de `.java`** — exatamente a contagem do disco (471 + 368).

## 🔴 Achado declarado, NÃO consertado (fora do escopo desta tarefa)

`mapa_projeto.md` na raiz é **regenerado pela aplicação no boot** e diverge por máquina: o
commitado veio do desktop em 25/07 (raiz `...quarkus`, 3.109 pastas, **10.049 `.java`** — ele
indexava `build/`), e aqui saiu com 346 pastas e **838 `.java`**. Restaurei o commitado em vez de
empurrar 142 mil linhas de diferença junto de uma tarefa de documentação.

**Decisão de Paulo:** versionar ou não um artefato que cada máquina reescreve. O precedente é a
decisão de 25/07 — *"nada de runtime é versionado"* — que tirou `cache/` e `relatorios/` do índice.

---

# ▶ ENTREGUE EM 19/08/2026 (noite) — DOCUMENTAÇÃO ATUALIZADA

**TAREFA ORIGINAL (palavras de Paulo):** *"Agora queciso que voce atualize totalemtne a
documentação do kronos, pois estes ultimos dias mexemos em muitas coisas e ele deve estar todo
desatualizado, colocque diagramas de arquitetura coloridos icones cores e imagens novas,
screesn das telas , pois elas mudaram, Documente muito bem tudo!"*

## O desvio, medido antes de escrever

```
219 commits desde a ultima atualizacao dos docs (06/08)
screenshots de 19/07 — um mes atras, com 3.1/3.2/3.3 inteiras mudadas
3.3 Revisao de Concordancia: existia no menu, SEM pagina nenhuma
3.1 Revisao de Legendas:     sem pagina propria, dobrada dentro da 2.3
docs/ref-docker.md:          no disco e FORA do indice (invisivel no app)
```

## ✅ O DEFEITO QUE A TAREFA DESENTERROU — a documentação estava MORTA no app

`GET /api/docs/etapa-1.1-analise-midia` → **HTTP 400**. As **14 páginas numeradas** — o pipeline
inteiro — não abriam no painel "Documentação" desde a renomeação de 06/08/2026. Só abriam as
páginas sem ponto no nome (`arquitetura`, `ref-*`).

Causa: `DocumentacaoController.NOME_SEGURO` era `^[a-zA-Z0-9_\-]+$`, **sem o ponto**, e as páginas
passaram a se chamar `etapa-G.N-nome.md`. A `CatracaOrdemDocumentacaoTest` conferia numeração,
nome e ordem — e **nenhuma guarda perguntava se a página abre**.

```
CONFIRMADO NO NAVEGADOR (antes):  "Nao foi possivel carregar etapa-1.1-analise-midia. HTTP 400"
DEPOIS: as 26 paginas de docs/ respondem 200, conferidas uma a uma por curl
```

**Guarda nova:** `CatracaPaginaDeDocumentacaoAbreTest` (3 testes) — toda página de `docs/` abre;
travessia (`..`, separador, vazio) continua recusada; inexistente responde 404.

```
CALIBRACAO (replantei o padrao antigo, sem ponto) .... 3 tests completed, 2 failed
COM O CONSERTO ...................................... 3/3 verdes
CatracaOrdemDocumentacaoTest ........................ 4/4 verdes
```

## ✅ SEGUNDO DEFEITO — todo bloco de código media 525px

Medido na 3.3: `pre` de 2, 3 e 4 linhas, **todos com 525px**. A regra global do `base.css`
(`pre { height: 50vh; min-height: 150px; resize: vertical }`) existe para os VIEWERS (console de
log, telemetria) e alcançava o markdown da documentação. Corrigido **só no painel de
documentação** (`documentacao.css`), sem tocar nos consoles.

```
ANTES:  525 · 525 · 525 · 525 px   (2 a 4 linhas de conteudo)
DEPOIS: 104 ·  98 ·  62 ·  77 · 202 px  (proporcional ao conteudo)
```

## O que foi escrito

| arquivo | o que mudou |
|---|---|
| `docs/etapa-3.3-revisao-concordancia.md` | **NOVO** — a tela que não tinha página |
| `docs/etapa-3.1-revisao-legendas.md` | **NOVO** — extraída da 2.3, com as duas passadas e o cartão do alvo |
| `docs/etapa-3.2-revisao-lore.md` | **reescrito** — a tela passou a CORRIGIR; lore.yaml como produto; as cicatrizes (token de template, Bosnia, patente, Kelley) |
| `docs/etapa-2.3-correcao-revisao.md` | recortado para o **cache** apenas; fluxos 3 e 4 apontam para as páginas novas |
| `docs/modulo-contextos-lore.md` | **reescrito** — a lore é DADO desde 15/08; instruções de "adicionar obra" estavam mandando criar classe Java |
| `docs/arquitetura.md` | números remedidos; peer `contexto` → `lore`; 34 guardas |
| `docs/catracas-e-fronteiras.md` | inventário de **13 → 34 guardas**, agrupado, com testes por classe |
| `docs/ref-api-endpoints.md` | âncoras mortas para os fluxos que mudaram de página |
| `README.md` | números remedidos, 3.1/3.3 na tabela, 69 obras |
| `index.html` | índice da documentação: 3.1, 3.3 e `ref-docker` (que estava invisível) |
| `AlcanceRevisaoLore.java` | `{@link}` para `RevisarLorePtOnlyUseCase`, classe removida em 17/08 |

**21 screenshots** refeitos com o servidor reiniciado (18 atualizados + 3 novos:
`revisao-concordancia`, `traducao-sem-lore`, `sobre`).

## Números remedidos (o que a documentação afirmava × o que a produção diz)

| afirmava | mede hoje |
|---|---|
| 72 lores | **69** obras no `lore.yaml`, **68** na lista da UI (1 com `apareceNaLista: false`) |
| 577 classes / 63.942 linhas | **471** classes / **63.049** linhas em `src/main` |
| 1.440 testes | **2.001** testes em **360** classes |
| 13 guardas | **34** guardas (24 catracas + 10 fronteiras), 133 testes |
| 19 controllers | **21** |
| peer `contexto`, 93 classes | peer `lore`, **24** classes + `lore.yaml` de 15.101 linhas |

## Provas coladas

```
gradlew test --rerun-tasks .......... 2.001 testes | 360 classes | 0 falhas | 35 pulados
34 guardas ......................... 133 testes | 0 falhas   (chaveado pelo FQN do arquivo)
26 paginas de docs/ ................ todas HTTP 200
navegador (playwright) ............. 3.3 renderiza: 1 mermaid SVG, imagem carregada
                                     (naturalWidth>0), 8 tabelas, sem erro no painel
arquitetura .......................  5 diagramas mermaid renderizados, coloridos
```

**Erro meu, pego pelo proprio instrumento:** meu primeiro parser dos XMLs de teste descartava em
silêncio o arquivo cuja ordem de atributos não casava, e reportei "18 catracas" onde eram 24. A
chave certa é o **FQN no nome do arquivo**, não o atributo `name` — que vira o `@DisplayName`
quando a classe tem um. Mesma família do "descarte silencioso" que a regra 23 proíbe.

---


## ▶ PRÓXIMA AÇÃO EXECUTÁVEL EXATA (2026-08-19, fim do dia)

**Nenhuma pendente.** A 3.3 está fechada (Paulo: *"boa então fecho concordância?"* → sim, com as
ressalvas registradas no vault) e a migração do cartão do alvo, autorizada logo depois
(*"pode fazer"*), foi entregue em `57f3298e`.

**Se a sessão retomar, a primeira coisa a fazer é a única que ficou 🟡:**

```
1. abrir o KRONOS (http://localhost:8099) DEPOIS de reiniciar o servidor — o Quarkus dev
   serve estático do BOOT, e o servidor de hoje ainda tem o JS antigo
2. conferir o cartão do alvo nas TRÊS telas migradas, lado a lado:
     3.1 Revisão de Legendas  -> "Lore ativa: X. Pasta: <caminho>."  + caixa destacada
                                 enquanto falta lore OU pasta
     3.2 Revisão de Lore      -> mesma frase, SEM destaque de caixa (é assim de propósito)
     3.3 Concordância         -> "Obra: X. Pasta que será reescrita: <caminho>."
3. o que mudou de visível: a linha da pasta aparece SEMPRE, inclusive antes de escolher
   a obra ("ainda não informada"). Antes, 3.2 e 3.3 escondiam a linha inteira.
```

Provado por máquina: sintaxe (`node --check`, calibrado contra arquivo doente), a chamada ao
módulo, e a existência de todo id passado a ele. **Não comprovado:** a renderização — não
reiniciei o servidor do Paulo para ver.

---

# ▶ EM ANDAMENTO — 3.3 REVISÃO DE CONCORDÂNCIA (aberta em 2026-08-18, sessão `392007ea`)

**TAREFA ORIGINAL (palavras de Paulo):** *"3.2 terminamos aqui trataremos do meu 3.3 concordancia
e suas correções possíveis"* + *"faça uma analise profunda de engenharia e todo tipo de testes para
ver o que temos hoje e por onde devemos começar a corrigir! seguindo o protocolo de desacoplamento
total com exceção as lores. entretanto, lores não entram aqui! mas provavelmente os dicionários
entram principalmente o em português ptbr"*

**ESCOPO:** a fatia `revisaoConcordancia` (tela 3.3), PT-only. **Lore fora.** Dicionário pt_BR
(hunspell, kernel `core/texto/dicionarioOrtografia`) é candidato declarado.

## O QUE A FATIA É HOJE — medido, não lembrado

```
4 classes / 511 linhas          use case + corretor + resultado + controller
INBOUND  0                      nenhuma outra fatia a consome
OUTBOUND 12 tipos               core(5) + legenda(5) + telemetria(2) — ZERO fatia->fatia
testes    2 classes             6 casos no use case (veto de musica + contra-teste, backup,
                                dry-run, pasta inexistente, telemetria) + 12 no corretor
guardas   NENHUMA propria       sem fronteira ArchUnit e sem catraca de escrita da fatia
```

## ✅ ITEM 1 FECHADO — o corretor estragava fala CORRETA (14 estragos para 1 acerto)

`MedicaoConcordanciaAcervoPtIT` (novo), acervo inteiro `C:\animes`, calibrado obra a obra contra o
próprio caso de uso em dry-run (16 obras, zero divergência):

```
726 legendas (307 PT / 419 nao-PT) | 2.142.264 eventos | 380.697 ao alcance | 1.749.570 musica vetada

ANTES ... 15 falas alteradas:  15x "a -> o"   (TODAS "a Deus")  +  1x "Aquela -> Aquele"
DEPOIS ..  1 fala  alterada:    1x "Aquela -> Aquele"   <- o unico acerto real do acervo
```

**Causa raiz:** `ART_FEM` incluía o `a`, que antes de substantivo masculino é **preposição** e
está CERTA (`"Graças a Deus"` → `"Graças o Deus"`). O detector da 3.1 (`DetectorConcordanciaNominal`)
já documenta essa exceção e já deixa o `a` de fora; o corretor da 3.3 nasceu de uma segunda escrita
da mesma ideia e **perdeu a exceção** — a divergência que a regra da medição prevê.

**Correção assimétrica de propósito:** `ART_FEM_NO_PADRAO` sem o `a`; o lado masculino mantém o `o`
(que nunca é preposição), então `"Vi o menina"` segue corrigido.

```
MUTACAO (devolver o "a" ao padrao) .. 1 failed — naoEstragaPreposicaoAAntesDeSubstantivoMasculino
   contra-teste continuaCorrigindoDeterminanteFemininoDeVerdade seguiu VERDE nos dois mundos
segunda medicao no acervo .......... 15 -> 1 fala tocada, e a que sobrou e o acerto
```

**Contrato mudado e DECLARADO:** `"a menino"` (artigo feminino de verdade) deixa de ser corrigido.
O teste antigo `corrigeMultiplosErrosNaMesmaLinha` afirmava esse comportamento e foi reescrito com
`uma menino`, com o porquê no Javadoc. Custo no acervo: **zero** — não existe uma só ocorrência.

## ✅ ITEM 2 FECHADO — a regra de cores da 3.1/3.2 no console da 3.3 (`b2d34792`)

Ordem de Paulo: *"primeiro aplicar o esquema de cores"*. A 3.3 estava **um passo atrás** da 3.2 —
não imprimia nada por arquivo, só o banner final, e erro de gravação ia só para o `log.warn`.

```
VERDE     [Revisado]  arquivo escrito, com a contagem de falas corrigidas
AMARELO   [Pendente]  nada gravado, mas ha falas que mudariam (a simulacao)
DIM       [OK]        reservado ao arquivo que realmente nao tinha nada
VERMELHO  [Erro]      so erro   <- nao existia
```

O banner de fecho segue a mesma regra (era verde SEMPRE, inclusive em simulação com pendência) e
o rótulo deixa de mentir: em dry-run as falas **mudariam**, não foram corrigidas.

**Achado do caminho:** `falasCorrigidas`/`arquivosAlterados` subiam ANTES do backup e da escrita —
falha ao criar backup deixava o arquivo intacto no disco e o banner dizia "1 fala corrigida".
Contagens movidas para depois da gravação, com o caso vermelho provando.

```
MUTACAO 1 (pintar [Pendente] de verde) .. 1 failed — amareloNaSimulacao
MUTACAO 2 (remover a linha vermelha) .... 1 failed — vermelhoNaFalhaDeGravacao
suite completa --rerun-tasks ............ 1965 testes, 0 falhas, 32 pulados, 353 classes
```

**Corrida REAL, no servidor vivo** (não só no teste que captura `System.out`) — ZZ, 47 arquivos,
dry-run pela API:

```
[OK]       ...S01E05_Track2_PT-BR.ass (concordancia conforme)          <- 46 arquivos assim
[Pendente] ...S01E06_Track2_PT-BR.ass (1 fala(s) mudariam — nada gravado, simulacao)
[PENDENTE — SIMULADO (dry-run, nada gravado)] REVISAO DE CONCORDANCIA (genero PT-BR)
  • Arquivos analisados  : 47      • Falas que mudariam   : 1
  • Arquivos alterados   : 1       • Backups              : 0
[RELATORIO FINAL] Tempo total: 1,4s
```

A fala é a mesma que a medição do acervo apontou: `Aquela garoto` → `Aquele garoto`.

**Batimento de progresso: NÃO copiado, e medido por quê.** O `pode-compilar.ps1` trata 90s de
silêncio como "job terminado". A obra mais lenta do acervo (DanMachi, 1.614.552 eventos em 260
arquivos) leva **12,8s** — 7x de folga —, e com uma linha por arquivo o silêncio máximo é o de um
arquivo só. A medição ganhou coluna de tempo para esse número não voltar a ser palpite.

## ✅ ITEM 3 FECHADO — aviso sonoro de fim de trabalho (`32fba542`)

A 3.3 era a **única** tela que espera a fila e terminava em silêncio. O módulo
`js/avisoSonoro.js` é **importado, não copiado** (invariante 10): armar no clique (o gesto que
libera áudio no navegador), estado dito em três valores, tocar depois da conclusão real, e o
alerta visual sempre primeiro.

**Catraca nova:** `CatracaAvisoSonoroNasTelasLongasTest` — toda tela que consulta
`/api/pipeline/status` importa o módulo e chama `tocarAvisoSonoro`, ou entra na linha de base.
Mais a segunda metade da invariante 10: ninguém obtém `AudioContext` fora do módulo.

- **Linha de base (só desce):** `correcao/correcao.js` espera a fila e não avisa. Fora de escopo
  hoje, visível aqui em vez de virar lacuna silenciosa.
- **Alarme falso corrigido antes de nascer:** a 1ª versão procurava o TEXTO `AudioContext` e
  reprovou meu próprio comentário. O critério virou a CONSTRUÇÃO, com caso-controle próprio.

```
MUTACAO (tirar tocarAvisoSonoro) ... 2 failed — a catraca e a linha de base
navegador REAL (playwright) ........ import('/js/avisoSonoro.js') -> 4 exports
   armarAvisoSonoro() -> 'armado' | tocarAvisoSonoro() -> true
```

## ✅ ITEM 4 FECHADO — seletor de obra, como AUXILIAR (`9ac022fb`)

Pedido de Paulo. Entra na categoria que o projeto já tinha para telas em que **a lore não muda o
resultado** (`ehAuxiliar` no `app.js`: análise, correção, cura, troca de tipo, renomear, karaokê
simples). Ganha, pelo mecanismo compartilhado: opções agrupadas por franquia, abertura travada
com a saída `— Sem obra —`, a **trava de lore** (pasta/Procurar/botão inertes até escolher),
banner de capa, e o nome da obra no console. O tooltip diz o que ele **não** faz.

**Catraca nova:** `CatracaSeletorDeObraRegistradoTest`, nos dois sentidos (11 × 11). A 1ª versão
aceitava "aparece em algum lugar do `app.js`" e a mutação provou a fraqueza — o combo abriria
vazio com a guarda verde. Passou a olhar o trecho das **duas listas** que populam.

## ✅ COMPROVADO NA TELA (Paulo autorizou o reinício, 18/08 21:38)

**O fato operacional que ficou medido:** o `quarkusDev` serve os estáticos do **boot** e não
re-sincroniza `.js`/`.html` em live reload. Antes do reinício o `build/` já tinha o seletor (2
ocorrências) e o servidor devolvia **0**. Depois: `js avisoSonoro=1`, `html select=2`.

Reinício com a guarda em `[0]`, porta 8099 com dono único, log antigo preservado em
`logs/console-web-gradle.ate-2026-08-15.log`. O `iniciar-kronos-dev.cmd` via `Start-Process`
morreu sem log (arranque não deixou rastro); subiu com `gradlew quarkusDev` e **stdin aberto**
(`sleep | gradlew`) — sem isso o console básico do Quarkus recebe EOF.

```
[21:39:37] Aviso sonoro ARMADO: 3 toques ao fim do lote.
[21:39:37] Iniciando revisão de concordância — Obra: UC 0088 - Mobile Suit Gundam ZZ
[21:39:37]   [OK]       ...S01E05_Track2_PT-BR.ass (concordancia conforme)      <- opacity 0.72
[21:39:37]   [Pendente] ...S01E06_Track2_PT-BR.ass (1 fala(s) mudariam...)      <- accent-yellow
[21:39:38]   [PENDENTE — SIMULADO (dry-run, nada gravado)] REVISAO DE CONCORDANCIA
```

O console web **traduz o ANSI para CSS** — conferido pelo `innerHTML`: `[Pendente]` sai em
`var(--accent-yellow)` e `[OK]` em `opacity: 0.72`. A regra de cores vale nos dois mundos.

Seletor: **70 opções** agrupadas por franquia, abre travado, e a **trava** deixa pasta/Procurar/
botão inertes até escolher (`campoPastaDesabilitado: true` antes, `true` liberado depois). Banner
carregou capa e sinopse reais do ZZ. Screenshot do painel conferido — layout íntegro.

## ✅ ITEM 5 FECHADO — cartão do alvo ativo + caixa "o que acontece" (`98d4a934`)

Pedido de Paulo olhando a 3.1. O cartão mantém à vista **obra** e **pasta que SERÁ REESCRITA** —
o rótulo é deliberado, é a cicatriz dos 17 arquivos sobrescritos por apontar para pasta de saída.

**A divergência que apareceu no caminho:** o mesmo cartão existia em TRÊS telas com o JS copiado,
e as cópias **já tinham divergido** — a da 3.2 interpola a pasta no `innerHTML`, a da 3.1 usa
`textContent`. Nasceu `js/cartaoAlvoAtivo.js` com a versão segura, provada com caso-controle no
navegador: pasta `C:\animes\<b>injecao</b>\pt` saiu como TEXTO (`querySelector('b') === null`).
**Fora de escopo hoje:** migrar 3.1 e 3.2 para o módulo — declarado, não esquecido.

A caixa segue o formato da 3.1 e tem duas entradas: o que ela corrige e **o que ela não faz**
(não usa lore, não conserta `ele`/`ela` por personagem, música é veto). O rodapé calibra com o
número medido: *"Espere pouco, e isso é bom sinal — 380.697 falas ao alcance, a tela mudaria 1"*.

## ✅ ITEM 6 FECHADO — a LÓGICA: `.parcial`, relatório honesto, catraca (`0a23730a`)

- **`.parcial` saiu do alcance da escrita** — 38 arquivos, dano gravado zero, superfície total. O
  sufixo é posto pelo próprio pipeline (`traducao.ResolvedorSaidaLegenda`) quando a tradução tem
  pendências; a constante fica local porque fatia não fala com fatia. Sai `[Ignorado]` na tela.
- **O relatório parou de somar o que não abriu** — "Arquivos revisados" e "Fora do alcance" são
  linhas separadas.
- **Catraca da fatia** (`CatracaEscritaDeFalaVetaMusicaConcordanciaTest`), a TERCEIRA irmã: as
  gêmeas varrem prefixo e são verdes por construção fora dele.
- **O harness foi alinhado**: também pula `.parcial`, senão a calibração batia por sorte.
- Números que mudaram junto: **380.697 → 332.545** falas ao alcance. O achado das 15 falas não.

## ✅ ITEM 7 FECHADO — cópia consciente dos tratamentos da tradução (`7590941c`)

Ordem de Paulo: aqui é cópia, não acoplamento. Do corretor da 3.1 só **dois** tratamentos são
PT-only (o resto exige o inglês): `graças ao deus → graças a Deus` e o possessivo de parentesco
`minha pai → meu pai`. Isolados chamando o objeto de produção com original **nulo**.

**Ganho medido hoje: ZERO** nas 332.545 falas — e o zero vale, porque o controle positivo rodou
no mesmo experimento. Estão aqui para a tradução de amanhã, e isso fica escrito.

**Dois erros meus, os dois pegos por instrumento:** o caso-controle sem cedilha que eu quase li
como instrumento cego (virou achado — a cópia aceita as duas formas), e um `grep` com `\b` sobre
texto multibyte que acusou 122 `meu irmã` que eram `meu irmão` corretos.

## ✅ ITEM 8 FECHADO — a tela nasce em dry-run (`66b4636d`)

Era o último 🔴 da fila e estava esperando decisão; Paulo autorizou em 19/08. O checkbox nascia
**desmarcado** e `aplicar = !simular`: abrir a tela e apertar o único botão **gravava**. Agora
nasce marcado, com o rótulo dizendo o que fazer para gravar.

**Catraca:** `CatracaTelaDestrutivaNasceEmDryRunTest` — todo `*-simular` nasce com `checked`.
Regex tolerante à ordem dos atributos, com caso-controle próprio. Mutação reprovou.

## ✅ ITEM 9 FECHADO — a tela sai de 1 para 55 falas corrigidas (`a89123ee` + `1f2eae57`)

O instrumento respondeu a pergunta e a resposta virou correção no mesmo dia.

**A medição:** `MedicaoConcordanciaPorDicionarioIT` pergunta ao dicionário pt_BR em vez de a uma
lista, inferindo gênero por **par mínimo**. 332.545 falas → 106.495 pares → 381 discordantes →
**210 pares distintos**, lidos um a um.

**A correção:** a lista curada tinha 24 palavras, todas de PESSOA. Entraram 20 femininas e 19
masculinas, **cada uma vista numa fala real**; e 20 candidatas foram RECUSADAS por serem ambíguas
(`guia`, `figura`, `caça`, `soldado`) ou de gênero fixo contrário à terminação (`pirata`,
`mecha`, `foto`, `data`…). Os **possessivos** entraram junto — metade dos erros medidos tinha
determinante possessivo, não artigo.

```
antes ... 1 fala tocada no acervo      depois ... 55, e 0 em arquivo nao-PT
"um isca"->"uma isca" (17x) · "o cortina"->"a cortina" · "o mochila"->"a mochila"
"sua avanco"->"seu avanco" · "Uma gato"->"Um gato" · "O alavanca"->"A alavanca"
```

**A meia-correção que o teste pegou antes de virar dano:** em `"a nossa orgulho"`, trocar só o
possessivo devolvia `"a nosso orgulho"` — discordância NOVA onde havia uma só. E o artigo não
pode ir junto porque `a` também é preposição. Possessivo precedido de artigo não é tocado.

**Bônus medido:** a fronteira do `\N`, documentada em 04/08 como "efeito NULO no acervo de hoje",
teve efeito pela primeira vez — `"Leina,\No isca"` → `"Leina,\Na isca"`.

## ✅ ITEM 10 FECHADO — as duas perguntas do Paulo, respondidas por sonda (`cc6a0949`)

Ele perguntou se valia a pena atacar os dois pontos declarados. Respostas **opostas**, as duas
medidas antes de escrever qualquer código.

**1. Análise morfológica para separar verbo de substantivo — NÃO VALE.** A sonda mostrou que o
dicionário analisa o sinal exatamente como o ruído: `resta → st:restar` e `isca → st:iscar`,
`cortina → st:cortinar`, `orgulho → st:orgulhar`. Filtrar por radical em `-ar` mataria o que
queremos. Separar exigiria POS tagger (spaCy/UDPipe) — dependência e modelo novos, para um
instrumento cuja lista já é legível a olho.

**2. Plural — VALE, e o número diz quanto.** Dos 238 pares distintos, 27 são de plural e a
leitura separou **17 ocorrências reais** do ruído conhecido (`os caras`, 86× correto).

```
55 -> 76 falas tocadas no acervo
"as reforcos"->"os reforcos" · "as reparos"->"os reparos" · "os pessoas"->"as pessoas"
```

O plural tem padrão **próprio**: casar número e gênero juntos deixaria `"o meninas"` virar
`"a meninas"` — um erro por outro. Número divergente a tela não toca.

**Erro meu corrigido junto:** `criança`, `pessoa`, `vítima` e `testemunha` estavam na exclusão do
instrumento como se fossem gênero comum. São femininos fixos — excluí-las cegava a medição.

## ✅ ITEM 11 FECHADO — o acervo INTEIRO aplicado, 78 falas (`e6e927ef`)

Paulo autorizou. Rodado obra a obra pela **API de produção**, não por harness.

```
1a rodada .... 76 falas em 62 arquivos      2a (indefinidos) .... 2 falas em 2 arquivos
TOTAL ........ 78 falas | 63 .bak | 11 pastas de backup | 0 fala em arquivo nao-PT
IDEMPOTENCIA . 2a passada dry-run: 15 obras, TODAS falas=0
```

**A conferência do acervo já corrigido revelou outra família:** `algumas reparos` continuava de
pé — o erro sobrevivia por falta de **determinante**, não de substantivo. Acrescentei sete pares
de indefinidos e **medi antes de gravar**: das 5 falas, **3 eram falso positivo**
(`"Você é muito criança"` — advérbio invariável; `"são todos iscas"` — concorda com *alvos*).
A família entrou podada: fora `muito/pouco/tanto` singular, `todo(a)(s)` e `certo(a)`.

**O backup serve** — diff do `.bak` contra o arquivo atual (ZZ S01E06) mostra exatamente as 3
linhas trocadas, com tempo, estilo e tags idênticos.

**Duas sobras que pareciam erro eram do meu grep:** `somos pessoas` e `sacrificamos pessoas`
casaram `os pessoas` por substring — a mesma armadilha de ontem, agora sem `\b` nenhum.

## ✅ ITEM 12 — as duas pendências aplicadas, o DANO que apareceu e a catraca que faltava

**Acentos: 596 aplicados** (`dae1b92d`). A primeira tentativa gravou **103 linhas de ROMAJI** —
`"aa kizutsukeau mae ni"` virou `"...mãe ni"`. O `mae` do romaji é 前 ("antes"). A ferramenta era
a **única** que varria o acervo sem perguntar se a linha é música, e a cicatriz já existia desde
14/08. Desfeito do snapshot **byte a byte** (76 arquivos, zero divergência), o harness virou
`@QuarkusTest` injetando `PoliticaEstiloMusical`, e o ensaio caiu de 699 → 596 (Zeta e ZZ sumiram
inteiros: eram só romaji). Reaplicado: **zero linha musical tocada**.

**Terminologia: 10 aplicadas no cache.** A primeira rodada (127) criou **`Tenente-Major`**,
patente que não existe — trocou `Coronel` dentro de `Tenente-Coronel`. Conserto no **mapa**, não
no dado: o `lore.yaml` do 86 ganhou as formas compostas antes da simples. E a proteção do
enforcer se mostrou viva: as 181 `Tenente-coronel` de ZZ e Guilty Crown **não** foram tocadas,
porque lá o inglês não diz "Major".

**Catraca nova** (`f6f4a23d`): `CatracaFerramentaDeAcervoVetaMusicaTest`. Critério de três marcas
juntas — lê `kronos.acervo` + grava + mexe em `Dialogue:`. Relatório não casa a terceira; fixture
em `@TempDir` não casa a primeira. Mutação reprovou.

> Dois erros do próprio instrumento, os dois pegos: a catraca **acusou a si mesma** (auto-exclusão
> nominal, de um arquivo só) e a catraca do drive Windows reprovou o caso-controle dela. Guarda
> pegando guarda.

## 📊 RETRATO DO ACERVO — bateria inteira rodada em 19/08/2026

24 harnesses de medição, 29 testes, **0 falhas**, 2 pulados (os que exigem frase literal de
escrita ou o LLM). 19m43s. As duas que escrevem no acervo **não dispararam** — exigem
`-Dkronos.aplicar.*` com frase literal.

| medição | número | leitura |
|---|---|---|
| **Acentos faltando** | **699 falas** em 76 arquivos | DanMachi 592 · Zeta 100 · ZZ 2 · GC 2 · F91 2 · Unicorn 1 |
| **Termo de lore a restaurar** | **127 falas** em 355 arquivos | Cardeas 28 · Apsaras 24 · Kelley 21 · Major 19 · Void 16 |
| Concordância (3.3) | **0** | zerada — aplicamos hoje, e a 2ª passada confirma |
| Concordância por dicionário | 426 candidatos | ruído conhecido: oblíquo+verbo, gerúndio, gênero comum |
| Detector 3.1 (com inglês) | 60 suspeitas / 122.640 | 0,05% |
| Predicativo | **0 erro real** / 391 analisáveis | medido e fechado: a lista de 30 adjetivos basta |
| Original repetido | 324 grupos, 880 falas | mesmo EN, traduções DIFERENTES |
| Música x espelho | 55.865 / 245.266 linhas | passivo de música já conhecido |
| Proveniência | 15 arquivos | retraduziriam do zero |
| Cartaz na 3.2 | 3 de 76.603 linhas | ruído mínimo |
| Falas vazias / resíduo EN / lore quebrada | vazio | nada a relatar |

**As duas pendências com número e ferramenta pronta são acento (699) e termo de lore (127).** As
duas gravam no acervo e têm trava dupla — decisão de Paulo.

## 🟡 A MEDIÇÃO, e o que ela ainda não resolve

`MedicaoConcordanciaPorDicionarioIT` (novo): pergunta ao dicionário pt_BR em vez de a uma lista
de 20 substantivos. Gênero inferido por **par mínimo** (a forma em `-o` e a em `-a` existem as
duas), que é o filtro que mantém `problema`, `dia` e `mapa` fora.

```
332.545 falas | 106.495 pares determinante+palavra | 15.533 com genero inferivel
   381 DISCORDANTES (candidatos)  +  426 com "a/as", contados a parte
```

**O instrumento achou erro REAL que as listas curadas não veem:** `um isca` (8×) e
`nossa orgulho`. Mas o número bruto ainda é dominado por falso positivo de uma classe só —
**pronome oblíquo + verbo** (`nos resta`, `os quebra`, `o incomoda`): o par `-o/-a` existe
porque o VERBO conjuga nos dois, e sem análise morfológica o teste não separa substantivo de
verbo. Próximo passo: separar os determinantes ambíguos (`o/a/os/as/nos`, que também são
pronome oblíquo) num balde próprio, como já se faz com `a/as`, e estimar a precisão lendo a
lista distinta inteira.

**Três erros meus nesta rodada, os três pegos por instrumento:** lote inteiro num `hunspell` só
(1.159 s de CPU, timeout de 20 s do adaptador, processo preso); `disponivel()` consultado
ANTES da primeira consulta (ele nasce nulo); e duas esperas com epoch cravado na mão, que
saíram na hora e me fizeram ler relatório velho duas vezes.

## 🔴 ABERTOS — a fila, na ordem, com o número que a justifica

> **Reconciliado em 19/08/2026 (noite):** os itens **1, 2 e 3** desta lista JÁ FORAM FECHADOS mais
> acima, nos ITENS 8 e 6 dos concluídos — a tela nasce em dry-run (`66b4636d`), o `.parcial` saiu
> do alcance e a catraca de escrita da fatia existe (`0a23730a`). A lista abaixo ficou como estava
> quando foi escrita; só os itens **4 e 5** continuam abertos. Conflito factual dentro do próprio
> checkpoint é o que a regra 24 manda corrigir na hora, não deixar para depois.

1. **A tela grava por padrão.** `revisaoConcordancia.html:16` — o checkbox "Apenas simular" nasce
   DESMARCADO, e `aplicar = !simular`. Um clique em Revisar escreve. As outras telas do projeto
   (novoKaraoke, renomearArquivos, traducaoKaraoke) usam **botão "Simular" separado**; a 3.3 é a
   exceção. Decisão de PRODUTO (é a UI que Paulo opera) — proposta: checkbox marcado por padrão
   OU dois botões, como as irmãs.
2. **A tela não distingue PT de inglês nem `.parcial`.** Ao alcance dela hoje: **419 arquivos
   não-PT** e **38 `.parcial`**. Dano gravado hoje: zero. Superfície aberta: total. O critério
   (`eLegendaTraduzida`) mora em `raspagemRevisao` — fatia não fala com fatia, então nasce próprio
   na 3.3 ou sobe para o kernel.
3. **Falta a catraca de escrita da fatia.** As irmãs existem para `raspagemRevisao` e `revisaoLore`
   e são **cegas fora do próprio prefixo** — `revisaoConcordancia` está fora das duas.
4. **Portão de saída por dicionário (pt_BR).** Hunspell disponível nesta máquina (312.369 entradas,
   25.932 regras de afixo). Ele sabe dizer o que NÃO existe (`asdfgh` → `#`) e sugerir
   (`cansadu` → `cansado, cansada`); **NÃO é oráculo de gênero** (`garota` → `st:garotar`). Uso
   correto: nenhuma palavra escrita pela 3.3 pode ser desconhecida do pt_BR. É kernel, então não
   sobe a `CatracaRegraDuplicadaEntreFatiasTest` (hoje em 15).
5. **O tamanho REAL do defeito de concordância PT-only NÃO está medido.** Os instrumentos de hoje
   acham **1 fala em 380.697** — instrumento saturado em zero, o mesmo padrão do erro fluente.
   Ampliar famílias de correção (adjetivo anteposto/posposto, oblíquo, plural/número) ANTES de medir
   é escrever corretor para defeito que ninguém provou existir.

## ▶ PRÓXIMA AÇÃO EXECUTÁVEL EXATA

Paulo prioriza a fila acima (item 1 é decisão de produto dele). Sem a ordem dele, o próximo item
tecnicamente seguro e inteiramente meu é o **3** (catraca de escrita da fatia, irmã das duas
existentes, com caso-controle em `@TempDir` e mutação).

O item 1 (a tela gravar por padrão) segue esperando a decisão dele: checkbox marcado por padrão
OU dois botões separados, como novoKaraoke/renomearArquivos/traducaoKaraoke já fazem.

---

# ✅ FEITO — 3.2 REVISÃO DE LORE (aberta em 2026-08-17, sessão `78943c66`; encerrada por Paulo em 18/08)

## ESCOPO FECHADO POR PAULO, palavras dele

> *"3.2 revisão de lore deve apenas servir para corrigir nomes, locais etc da lore da animação
> que estamos trabalhando, mas nada além disso! é um escopo bem fechado! o resto tiramos tudo!"*

> *"podemos usar os mesmos carimbos, lógica de página e correções visuais que aplicamos dentro
> de 3.1 nessa página de lore!"* — com o print da tela da 3.1 como referência.

**A frase da tela:** corrigir nome, local e termo canônico da lore da obra ativa na legenda
PT-BR, **por comparação determinística de palavras**, com o **LLM como último recurso**. Nada
além disso.

## O LUGAR DO LLM — decidido em três passos por Paulo, 17/08, e FECHADO

1. *"correção de lore acho que nem é caso de chamar o llm, mas verificação de palavras de forma
   determinística"*
2. Apontei a **única perda real**: sem LLM, a 3.2 corrige o que está no mapa de terminologia da
   obra e mais nada — nome fora do `lore.yaml` vira pendência. Ele: *"então nesse caso mudo a
   opinião e vale sim a pena manter ele como último ratio"*.
3. *"pode deixar ele acoplado, no caso o LLM, porque o projeto em si depende dele!"*

**Desenho final: determinístico primeiro, LLM só no que o mapa não alcançou.** É o que o código
já faz — a mudança que sobra é a de ESFORÇO, e é para MENOS: **não** construir modo degradado.

🟡 **ACEITO CONSCIENTE, não lacuna esquecida.** `RevisarLoreUseCase:179` aborta a sessão inteira
quando o LM Studio não tem modelo carregado — inclusive a correção determinística, que não depende
de rede nenhuma. Eu levantei; Paulo reafirmou que o LLM fica acoplado porque o KRONOS inteiro
depende dele. **Não reabrir sem ele pedir.** Uma tentativa de desacoplar foi começada e
**revertida** nesta sessão (`git checkout`), sem chegar a commit.

## ✅ ITEM 1 FECHADO — a 2ª porta de escrita da 3.2 não perguntava se era música

A fatia tem DUAS portas (`RevisarLoreUseCase`, aba "Com inglês", e `RevisarLorePtOnlyUseCase`,
aba PT-only). A segunda filtrava só `temTexto()`: sem juiz de estilo musical, sem `isDialogo()`,
sem karaokê, sem desenho vetorial — e **grava** no `.ass`, com "Apenas simular" DESMARCADO por
padrão na interface.

**Por que ninguém tinha visto:** a `CatracaEscritaDeFalaVetaMusicaTest` varre o prefixo
`org/traducao/projeto/raspagemRevisao` — a fatia da 3.1. A `revisaoLore` estava inteiramente
fora dela, e o relatório daquela catraca é verde por construção fora do seu prefixo.

**MEDIDO** (`MedicaoExposicaoMusicalRevisaoLorePtOnlyIT`, 22 obras do acervo):

```
246.246  linhas de estilo musical ao ALCANCE da aba PT-only
     0   reescritas HOJE pela camada deterministica FORA do Char's Counterattack
    29   no CCA — que e o filme ACHATADO, entao ali sao DIALOGO com nome de estilo vetado
```

Calibração: o laço do harness foi conferido contra o **próprio caso de uso em dry-run** na mesma
pasta — `harness == corrigidas + descartadas` em **22 de 22 obras**. Controle positivo:
246.246 > 0 (acervo sem música acusaria o instrumento).

**Leitura honesta:** dano gravado hoje é ZERO. O que estava aberto é a SUPERFÍCIE — a camada 2
(LLM PT-only) reescreve a linha inteira e decide sobre qualquer uma das 246.246 por um portão de
homógrafo. É o estado exato em que a ponte do cache da 3.1 esteve até morder 687 linhas
`Song ENG` do 08th MS Team, e o mesmo motivo pelo qual o `RevisorPtOnlyUseCase` ganhou veto
sendo inalcançável por menu.

**Mecanismo:** `AlcanceRevisaoLore` — dono ÚNICO da pergunta "esta linha está ao alcance da
3.2?", consultado pelas DUAS portas. Não decide o que é música: pergunta à `PoliticaEstiloMusical`.
A aba "Com inglês" passou a delegar (comportamento idêntico); a PT-only ganhou o veto.

```
MUTACAO (veto desligado com `false &&`) .. 1 failed — LorePtOnlyVetaMusicaTest:96
   "A LETRA DE MUSICA FOI REESCRITA"
   o contra-teste do MESMO metodo (dialogo corrigido) e o que separa
   "vetou musica" de "quebrou a tela"
suite da fatia .......................... 47 testes, 0 falhas
```

Catraca nova: `CatracaEscritaDeFalaVetaMusicaLoreTest` — inventário nominal das 2 portas da
fatia, com caso-controle (árvore em `@TempDir` com escrita plantada, leitura e outra fatia) e
uma asserção extra de que o próprio `AlcanceRevisaoLore` continua consultando o juiz de música.
**Limite declarado:** a catraca prova que a CHAMADA existe; quem prova que ela FUNCIONA é o
`LorePtOnlyVetaMusicaTest` (a mutação passou na catraca e reprovou no teste de comportamento).

## MÉTODOS PEQUENOS PARA O JIT — ordem de Paulo, 17/08, com o bytecode MEDIDO

> *"nessa arte do projeto podemos criar classes com métodos pequenos para nos aproveitarmos do
> JIT, pois é um caso claro de aproveitamento dele"*

Medido com `javap -c -p` sobre `build/classes/java/main` da fatia — **364 métodos**:

```
 bytes        metodo                                  quando roda
  2316  >>>   RevisarLoreUseCase.processarArquivo     POR ARQUIVO, com o laco POR FALA dentro
  1445  >>>   DetectorTermosLoreService.static{}      1x no carregamento da classe
  1040  >>>   RevisarLoreUseCase.executar             1x por sessao
   764  >>>   RevisarLorePtOnlyUseCase.executar       1x por sessao
   441  >>>   RevisorLoreLlmAdapter.postarLinhaUnica  por chamada de rede (a rede domina)
   333  >>>   primeiraDivergenciaEstrutural           por arquivo
```

Limiares do HotSpot: **35** = `MaxInlineSize` (inline até frio) · **325** = `FreqInlineSize`
(inline só se quente) · **8000** = `DontCompileHugeMethods` (acima disso **nunca compila**).

**Leitura honesta:** 6 métodos passam de 325 e **nenhum** passa de 8000 — nada está sendo
excluído da compilação. Dos 6, **só UM está em caminho realmente quente**: o
`processarArquivo`, com **7,1× o teto**, e é ele que carrega o laço por fala (milhares de
execuções por corrida). Os outros cinco rodam uma vez por sessão, por arquivo ou atrás de rede.

🟡 **`Inferência, não evidência direta`** quanto ao GANHO: medi TAMANHO, não desempenho. Que
métodos menores rendam JIT melhor aqui é plausível e é a razão da ordem, mas não foi medido —
exigiria `-XX:+PrintInlining` ou benchmark. O ganho **certo e imediato** da quebra é outro, e
não depende de JIT nenhum: hoje as ~14 saídas do laço só são alcançáveis rodando o caso de uso
inteiro; viradas métodos nomeados, cada uma ganha teste próprio.

**Como isso casa com o resto da fila:** a decomposição do `processarArquivo` é o VEÍCULO para
tirar A, B, C e D — cada saída vira um método com nome, e o que sai de escopo sai apagando um
método inteiro em vez de um `if` no meio de 300 linhas.

**ARMADILHA DE INSTRUMENTO PAGA AQUI:** a primeira versão da medição reportou **1.445 bytes num
lambda de uma linha**. Número implausível denunciou o parser: `static {};` não casava a regex de
assinatura, e os offsets do bloco estático eram somados ao método anterior. O `javap` cru mostra
o lambda com **6 bytes**. Corrigido e re-rodado com dois controles (o lambda trivial precisa sair
minúsculo; o `static{}` precisa aparecer separado). **Nenhum número desta seção vem da primeira
rodada.**

## ✅ ESCOPO MEDIDO ANTES DE CORTAR — `MedicaoEscopoDaRevisaoLoreIT`, 22 obras, 75.419 falas

**O que SAI: 24,0% de todo o ruído da tela.**

```
REGRA (prefixo do motivo)                    motivos       %   veredito
Nome proprio do original                        4221   41,9%   FICA
Nome proprio composto                           3378   33,5%   FICA
Termo de faccao                                   38    0,4%   FICA
Possivel traducao literal                         21    0,2%   FICA
Possivel nome/termo em ingles remanescente      1606   15,9%   SAI  (B: falta de traducao = 3.1)
Sigla ou termo todo em maiusculas                816    8,1%   SAI  (A: nao e nome nem local)
TOTAL                                          10080
```

75,4% é nome próprio — exatamente o escopo que Paulo fechou. As duas regras que saem produzem
**2.422 motivos** que hoje viram pendência da 3.2 sem serem trabalho dela.

**O que ENTRA: 10 falas em 75.419** (0,013%). É o delta de deixar o corretor determinístico rodar
em toda fala no alcance, e não só nas sinalizadas. Amostra — **são erros reais que a heurística
não pega**:

```
Sieg Zeon.              -> PT dizia "Zeon Sieg."      (ordem invertida, CCA, 3 falas)
The Psyco-frame worked. -> PT dizia "psycoframe"      (CCA)
Freya Familia           -> PT dizia "Familia Freya"   (DanMachi S05, 6 falas)
```

**Veredito:** o delta é pequeno e é bom. Liberar o determinístico para tudo é seguro e corrige
10 falas que hoje escapam. 🟡 uma das amostras está num `.parcial.ass`, que não é entrega — não
muda o veredito, e o volume não justifica filtrar agora.

## ⚠️ ERRO MEU NESTA SESSÃO, PEGO E REVERTIDO — não repetir

Tentei remover A e B com um script Python que cortava método por chave balanceada, recuando até
o javadoc anterior. A heurística de recuo comeu **199 linhas** e levou junto
`detectarTraducoesLiteraisSuspeitas`, `detectarNomesPropriosDivergentes` e
`detectarTermosTraduziveisEmIngles` — três regras que FICAM. O sinal foi o tamanho: 23.539 →
13.575 bytes é redução demais para 4 métodos.

`git checkout` no arquivo, integridade conferida (9 ocorrências de volta, contra 5 depois do
estrago). **Transformação em lote sem guarda que aborta é o que a regra proíbe** — os cortes de
A/B/C/D se fazem por edição dirigida, um método por vez, com a suíte entre eles.

## ✅ A e B CORTADOS — 2.422 motivos, 24,0% do ruído da tela

Cortes por **edição dirigida**, um método por vez, com a suíte entre eles. Saíram
`detectarTermosMaiusculosSuspeitos`, `detectarNomesInglesRemanescentes` e os órfãos
(`PALAVRA_LATINA`, `COGNATOS_VALIDOS_PT`, `tokensDeNomesProprios`, `loreMencionaExclusivamente`).

**ACHADO NO CAMINHO:** a suíte passou **verde de primeira** — e a razão não é boa. As duas regras
**não tinham um único teste**. 2.422 motivos por corrida no acervo, produzidos por código sem
cobertura nenhuma. Não havia o que quebrar.

`EscopoDaRevisaoLoreTest` é a cobertura que faltava, escrita no sentido contrário: reprova se
elas VOLTAREM, e a mensagem carrega o volume que cada uma produzia. Caso-controle nos DOIS
sentidos no mesmo arquivo — as falas fora de escopo têm de sair limpas **e** as três regras que
ficaram têm de continuar acusando, senão um detector inteiramente cego passaria.

```
MUTACAO (detectarNomesPropriosDivergentes desligada) .. 1 failed
   CONTROLE POSITIVO: nome proprio composto quebrado continua sendo acusado
   -> os controles ENXERGAM; nao sao assercao decorativa
suite completa --rerun-tasks ......................... 332 classes, 1.897 testes, 0 falhas
```

⚠️ **NÃO REPETIR:** heredoc de `git commit` e `python -c` com aspas duplas **expandem crase** no
bash — foi assim que a primeira versão desta seção foi gravada com os nomes de classe apagados e
o bloco duplicado. Texto com crase vai por `Write`/`Edit`, nunca por heredoc de shell.

## FILA — o que ainda sai ("o resto tiramos tudo")

| # | item | onde | por que sai |
|---|---|---|---|
| A | ✅ FEITO — acusar palavra em CAIXA ALTA | `DetectorTermosLoreService.detectarTermosMaiusculosSuspeitos` | qualquer grito "PARE!" vira motivo; não é nome nem local |
| B | ✅ FEITO — acusar resíduo genérico em inglês | `detectarNomesInglesRemanescentes` | é FALTA DE TRADUÇÃO — trabalho da 3.1 |
| C | encaminhar fala não traduzida à Opção 6 | `RevisarLoreUseCase.ehFalaNaoTraduzida` | idem; e hoje INFLA `falasSinalizadas` e vira pendência, deixando a 3.2 amarela por problema alheio |
| D | checkbox "Revisar todas as falas" | flag `revisarTodasFalas` | **DECIDIDO por Paulo:** *"nada de checkbox, deixa ele habilitado por padrão, o sistema que determina isso"*. O operador não escolhe: o determinístico varre TUDO no alcance, sempre; o LLM entra só em fala suspeita. A parte LLM do modo antigo está **PROVADA inerte** com o caso de uso real (proposta em fala limpa nunca chega ao arquivo) — e "último recurso" não é "recurso preventivo" |
| E | roster de lore hardcoded no detector | `TERMOS_LORE_SOLTEIROS_RELEVANTES` (100+ termos misturando Gundam/86/Macross), `TRADUCOES_LITERAIS_SUSPEITAS` (17), `TERMOS_TRADUZIVEIS_ACEITOS` (11) | SEGUNDA cópia da lore em código, contra a decisão de 15/08 (lore = arquivo único). Exige medir cobertura antes e depois |
| F | visual da 3.1 na 3.2 | `static/revisaoLore/revisaoLore.html` + `.js` | pedido do Paulo com print: combo de obra travando os campos (`travaLore.js`), campos numerados com o destino de escrita em negrito, cartão "Lore ativa", passadas em cartões com etiquetas e botão próprio, faixa de aviso |
| G | console no padrão da 3.1 | `RevisarLoreUseCase.sessao.out` | cor padrão internacional (verde=corrigida, amarelo=pendente, VERMELHO só erro), referência EN em apagado × estado colorido, ruído agregado por arquivo (hoje imprime uma linha DIM por fala auditada), `[ESTILOS] N linha(s) alterada(s)` por arquivo gravado |

## ✅ ITEM D — METADE FEITA: a inércia está PROVADA (falta a troca, que depende de uma medição)

`LlmEmFalaSemIndicioDeLoreEInerteTest` roda o **caso de uso real** com um LLM de mentira que
devolve proposta desenhada para ATRAVESSAR todos os portões anteriores (troca UM token, e o token
inserido existe no inglês E na lore de `gundam_zeta`) — para que o desfecho não pudesse ser
creditado a outra trava.

```
LLM chamado ......... 1x   (calibracao: se fosse 0, "nao gravou" seria trivial)
fala limpa .......... sim  (calibracao: exigida do detector de PRODUCAO antes de medir)
arquivo ............. byte a byte IGUAL
falasSemAlteracao ... 1    <- o DISCRIMINADOR: e o ramo preventivo (703), nao o validador
falasDescartadas .... 0       (se fosse aqui, a proposta teria morrido numa trava anterior)
```

**Conclusão provada, não inferida:** no modo "Revisar todas as falas", a chamada ao LLM em fala
sem indício de lore é incapaz de alterar a legenda. Remover essa chamada não perde capacidade
nenhuma — só deixa de gastar o modelo local.

## ✅ C e D FECHADOS — a 3.2 faz uma coisa só

**C:** fala inteira em inglês continua avisada e registrada (`ENCAMINHADA_OPCAO_6`), mas saiu de
`falasSinalizadas` (não foi a heurística de lore que a achou) e de `falasPendentes` (fazia a tela
fechar amarela por trabalho da 3.1).

**D:** a ordem virou — corretor determinístico em **toda** fala no alcance, sempre; LLM **só** na
que a heurística acusou e o mapa não resolveu. Checkbox fora do HTML e do JS. A flag
`revisarTodasFalas` sobrevive como **rótulo** de relatório/auditoria (para não invalidar dataset
antigo) e não decide mais nada.

**O teste que caiu, e por que é a prova:** `LlmEmFalaSemIndicioDeLoreEInerteTest` nascera
afirmando `chamadas == 1` — e existia para provar que aquela chamada era INERTE. Foi essa prova
que autorizou removê-la; com a remoção, ele reprovou. Reescrito para `chamadas == 0`, que é
asserção **mais forte**, mantendo o par com `falasSemAlteracao == 1` que prova que a fala foi
auditada e não pulada.

```
suite completa --rerun-tasks .. 332 classes, 1.897 testes, 0 falhas, 28 pulados
```

## ▶ PRÓXIMA AÇÃO EXECUTÁVEL EXATA — REMOVER A ABA PT-ONLY (autorizado por Paulo)

> *"pode tirar a aba e as opções que não são válidas, deixando mais limpa a tela"* — 17/08.

**A medição que decidiu** (varredura do acervo com controle positivo):

```
pastas traducao_ptbr no acervo ......... 23
delas, com pasta de ingles irma ........ 23   <- TODAS
sem ingles (o caso que a aba atende) ....  0
```

As 5 obras que pareciam justificá-la (Break Blade 1,3,4,5,6) **têm** `legendas_eng/…_Track3.ass`.
O que falta nelas é a TRADUÇÃO — só existe `_PT-BR.parcial.ass`, e `.parcial` não é entrega, por
isso o pareamento não casava. Não era "obra sem inglês"; era tradução pela metade.

**Ordem da remoção** (12 arquivos mapeados; fazer nesta sequência e rodar a suíte ao fim):

1. **Java produção:** apagar `RevisarLorePtOnlyUseCase`; no `RevisaoLoreController` remover o
   endpoint `/api/revisar-lore-ptonly`, o record `RevisaoLorePtOnlyRequest`, o campo injetado e
   `imprimirBannerPtOnly`; em `CorretorLoreDeterministico` apagar `corrigirPtOnly` (só a aba o
   usa — conferido).
2. **Tela:** em `revisaoLore.html` tirar a barra `.lore-tabs`, o `form-revisao-lore-ptonly` e a
   **passada 2** do cartão; sobrando uma aba só, o `.lore-tab-panel` do form que fica perde a
   classe. Em `revisaoLore.js`: `vincularAbas`, `vincularEventosPtOnly`, o atalho
   `btn-passada-lore-ptonly` e o trecho do cartão de lore ativa que lê a aba ativa.
3. **Catraca:** `CatracaEscritaDeFalaVetaMusicaLoreTest` passa a ter **uma** porta em
   `PORTAS_CONHECIDAS` — e é ela que vai reprovar se algum resquício continuar escrevendo.
4. **Testes que somem com o alvo:** `RevisarLorePtOnlyUseCaseTest`, `LorePtOnlyVetaMusicaTest`,
   `MedicaoExposicaoMusicalRevisaoLorePtOnlyIT` (e a entrada dele em
   `CatracaSuiteSemDriveWindowsTest`), os dois testes `revisarLorePtOnly*` do `ApiEndpointsTest`,
   e o caso de `corrigirPtOnly` em `CorretorLoreDeterministicoTest`.
5. 🔴 **NÃO ERAM FALSOS POSITIVOS — e este passo virou o mais delicado.** Conferido: as três
   `ContextoRevisaoLoreMacross*Filmes` e a `CatracaAgregadorasForaDoCdiTest` citam
   `RevisarLorePtOnlyUseCase` como **a razão de as 3 agregadoras Macross ficarem FORA do CDI**:

   > *"No fluxo EN+PT quase é seguro: o `ValidadorCandidatoLoreService` exige que a sequência nova
   > apareça no ORIGINAL INGLÊS além da lore. **No fluxo PT-ONLY esse portão não existe** — o
   > `RevisarLorePtOnlyUseCase` manda o prompt ao LLM com o original inglês VAZIO. Com a agregadora
   > ali, o modelo pode normalizar um termo de um título para o canônico de OUTRO."*

   **DECIDIDO POR PAULO (17/08):** *"os avisos são para permanecer! o restante pode fazer"*.
   As agregadoras Macross **continuam fora do CDI** e a catraca **continua como está**. A única
   coisa a mexer no texto é a referência à classe apagada: onde hoje se lê *"o
   `RevisarLorePtOnlyUseCase` manda o prompt com o original inglês VAZIO"*, passa a se ler que
   esse fluxo **existiu e foi removido em 17/08/2026**, e que a proteção permanece por precaução
   — sem link morto e sem perder a cicatriz.

   **O que NÃO fazer:** apagar o texto da catraca junto com a aba. O raciocínio dela some, a
   catraca continua verde, e ninguém mais sabe por que aquelas três classes estão fora do CDI —
   que é a forma clássica de uma proteção virar mistério e depois ser removida por engano.
6. `AlcanceRevisaoLore` **FICA** — a porta que sobra continua consultando ele.

**Nota de honestidade para o commit:** o `MedicaoExposicaoMusicalRevisaoLorePtOnlyIT` mediu as
246.246 linhas que justificaram o veto de hoje. Ele sai porque o alvo sai, não porque o número
deixou de valer — o número vira parte da mensagem do commit, senão a cicatriz some com o arquivo.

## ▶ DEPOIS DISSO

**Rodar a 3.2 no acervo** com o KRONOS no ar (LM Studio carregado — o LLM é acoplado por decisão
de Paulo) numa obra com delta conhecido, e conferir que as 10 falas medidas aparecem corrigidas:
CCA (`Zeon Sieg`→`Sieg Zeon`, `psycoframe`→`Psyco-frame`) e DanMachi S05 (`Familia Freya`).
🟡 **NÃO EXECUTADO** nesta sessão: tudo está provado em teste e medição, não em corrida real.

Depois, na ordem: **E** (roster hardcoded de 100+ termos no detector → perguntar à lore da obra,
medindo cobertura antes/depois) · **F/G** (o visual da 3.1 na página de lore: combo travando os
campos, campos numerados com o destino de escrita em negrito, cartão "Lore ativa", passadas em
cartões com etiquetas, cor no padrão internacional, ruído agregado por arquivo) · e a
**decomposição do `processarArquivo`** (2.316 bytes, 7,1× o teto de inline), que ficou menor
depois destes cortes — refatorar antes de cortar teria sido reorganizar código que ia sair.

### (histórico) o que a próxima ação era antes de C e D

**Medir antes de trocar.** A remoção da flag `revisarTodasFalas` tem uma parte que NÃO é neutra: o
corretor determinístico só alcança fala não sinalizada quando o modo está ligado (o `continue` da
heurística acontece ANTES dele, em `RevisarLoreUseCase:519`). Passá-lo a rodar em TODA fala no
alcance — que é o certo, e é seguro por construção porque o enforcer só restaura quando o EN
contém o canônico na grafia exata — **aumenta o que é gravado no acervo**, e isso não se troca às
cegas.

Escrever `MedicaoCorretorLoreForaDaHeuristicaIT` no molde do harness de música (asks production:
`DetectorTermosLoreService`, `CorretorLoreDeterministico`, `GerenciadorPromptRevisaoLore`,
`AlcanceRevisaoLore`, pareamento pelo `ResolvedorArtefatosRevisao`) contando, por obra:

```
falas no alcance | suspeitas pela heuristica | deterministico age E e suspeita (hoje ja corrige)
                                             | deterministico age E NAO e suspeita  <- o DELTA
```

Com o delta na mão: se for pequeno e as amostras forem nome próprio de verdade, roda a troca
(determinístico sempre; LLM só em suspeita; flag e checkbox saem). Se for grande, o número vira
decisão do Paulo antes de gravar.

Depois: A, B e C na mesma passada de `DetectorTermosLoreService`, medindo quantos motivos saem.

**NÃO REPETIR:** não medir a exposição da PT-only com `Get-ChildItem -Filter` — metade do acervo
tem `[` no nome. O harness usa `Files.walk` + `Files.list` e resolve o contexto pelo
`GerenciadorContexto.idsQueReconhecem`, que é a produção.

---

# ✅ 3.1 REVISÃO DE LEGENDAS — FECHADA em 2026-08-17

**Status honesto: `VALIDADO NO ESCOPO TESTADO`.** Não é "perfeita"; é que tudo o que a tela
promete fazer foi medido fazendo, e o que ela não faz está declarado abaixo.

## O que ficou provado, com artefato

| propriedade | prova |
|---|---|
| música é veto absoluto | 4 portas de escrita inventariadas, 2 consertadas, catraca nominal com mutação |
| o acervo não tem passivo | 23 obras medidas, **22 com ZERO**; Guilty Crown teve 283 linhas reparadas |
| corrida cega não passa por sucesso | pasta EN errada devolve `CONCLUIDO_SEM_REFERENCIA`, não verde |
| traduz o que faltou | a regra de repetição parou de recusar tradução de fala em inglês |
| não estraga estrutura | proposta que perde `\N` é rejeitada (7ª pergunta do portão) |
| é idempotente | 16 corridas em 8 obras num dia: **1 escrita**, e era fala legitimamente pendente |
| a tela não engana | auditoria linha a linha: **10 de 10** batendo com o disco; cor no padrão, rolagem livre, ruído agregado |

Suíte: 326 classes, 1.885 testes, 0 falhas.

## 🔴 Aberto e DECLARADO (nada disso impede usar a tela)

1. **`Side Four` → `Lado Four`** — Paulo quer `Lado Quatro`. Falta MECANISMO (exceção de contexto
   "`Four` protegido, exceto precedido de `Side`"), não configuração. 3 topônimos × 188 usos do
   nome. Detalhe no `lore.yaml`.
2. **126 falas do Zeta** com `Quatro` no lugar do nome — reparo é reescrita, **vetado por Paulo**.
   A regra impede novas; não conserta as antigas.
3. **3 cartões de próximo episódio no ZZ** (E35, E41, E42) ainda em inglês.
4. **Char's Counterattack** achatado para estilo vetado — **fora de escopo por decisão do Paulo**
   ("quando eu for mexer nele eu traduzo tudo do zero"). A retradução resolve sozinha.
5. **`Ha ha ha ha ha!`** acusado como não traduzida — gargalhada é idêntica nos dois idiomas.
   Ruído conhecido, 1 ocorrência.
6. **Console duplica linha no log** (7×, cresce dentro do processo) — ouvinte acumulando por
   reconexão. **NÃO medido** se chega ao navegador do Paulo.

## Próxima frente

3.3 Revisão de Concordância (para onde a 3.1 roteia o que não é dela) ou 4.1 Tradução de Karaokê.

## ▶ PRÓXIMA AÇÃO EXECUTÁVEL EXATA

**Decisão do Paulo, pendente:** `Four` (personagem Four Murasame) sai como `Quatro` quando a fala
é traduzida. O `lore.yaml:7242` já declara a regra — *original `"Four"` → saída `"Four"`; original
`"four"` → `"quatro"`* —, mas o mapa de correção só cobre o composto `Quatro Murasame`. Distinguir
nome de numeral **por maiúscula** muda o enforcer no acervo inteiro, então é decisão de produto.
Enquanto isso a fala está **revertida** (voltou ao inglês) e não há dano em disco. O Zeta está
**fora** das corridas de validação por causa disso — rodar gravaria o defeito.

Cenário de boa-fé já fechado e medido: mesma pasta nos dois campos NÃO produz verde falso — o
`ResolvedorArtefatosRevisao` tem invariante declarada de nunca parear um PT com outro PT, então o
caminho cai na mesma cegueira e sai `CONCLUIDO_SEM_REFERENCIA`. Congelado em
`CegueiraDaPastaEnCaracterizacaoTest`.

O passivo de música do acervo **está medido e fechado** — ver abaixo. A 3.1 está liberada para as
obras restantes.

**Char's Counterattack: FORA DE ESCOPO por decisão do Paulo (17/08).** Palavras dele: *"eu não
mexi nesse anime pode ficar sem fazer nada. quando eu for mexer nele eu traduzo tudo do zero"*.
E a retradução resolve sozinha: partindo do `.ass` em inglês, que ainda tem os estilos separados,
o achatamento não se reproduz. Não reabrir sem ele pedir.

---

# ✅ 2026-08-17 — PASSIVO DE MÚSICA DO ACERVO: MEDIDO E FECHADO

`MedicaoMusicaDivergenteDoEspelhoIT` — 23 obras, 245.266 linhas de estilo musical com espelho.
Pergunta o critério musical à `PoliticaEstiloMusical` de produção e o pareamento ao
`ResolvedorArtefatosRevisao`; não reimplementa nenhum dos dois.

**Calibração antes de acreditar:** rodado contra o estado doente do 08th (13 arquivos guardados
no scratchpad) devolveu **687** — exatamente o que o diff em PowerShell mediu ontem por outro
caminho. Dois instrumentos independentes, mesmo número.

## Resultado: 21 obras ZERO. Duas divergiam.

### 🔴→✅ Guilty Crown — 283 linhas, REPARADO

As corridas de 16/08 (22:12 / 22:59 / 23:32) escreveram música pela mesma ponte do cache do 08th.
Prova: o backup de 22:12 tinha divergência **0** de 1.209 linhas musicais; hoje tinha 283.

```
diff backup 22:12 -> antes do reparo: 295 linhas
   123 ED_S2 · 68 OP · 44 ED · 43 OP_S2 · 5 Other songs   = 283 MUSICA (96%)
    12 Default                                            = correcao legitima
```

Reparo: as 283 restauradas do backup (que estava provadamente igual ao espelho), as **12 de
diálogo preservadas**. Conferido: BOM em 23/23, contagem de eventos intacta, e o que ainda difere
do backup são exatamente os 12 `Default`. Re-medição independente: **0**.
Snapshot em `scratchpad\guilty-crown-antes-do-revert`.

### 🔴 Char's Counterattack — 55.865, e NÃO é dano de música — ABERTO

| estilo | inglês | português |
|---|---|---|
| `Char's Counterattack` | 53.346 | **55.935** |
| `Dialogue` | 1.868 | **0** |
| `Mobile Suit Gundam` | 644 | **0** |
| `ED-ENG` + `Dialogue - Alt` | 56 | **0** |

O arquivo foi **achatado**: o diálogo real herdou o nome do estilo decorativo. E
`Char's Counterattack` está na lista `estilos-ignorados` do `application.yml` (linha 142), junto
com `Mobile Suit Gundam`. **Toda tela que veta música enxerga o filme inteiro como música.** O
diálogo já está traduzido — não há urgência de dano —, mas o filme é hoje inauditável pela 3.1,
3.3 e 4.1. É a cicatriz do achatamento por CONTAGEM materializada num filme.

## Armadilha de instrumento pega no caminho

`Get-ChildItem -Filter` e `Test-Path` sem `-LiteralPath` **quebram** em pasta com `[` no nome, e
metade do acervo tem. A primeira tabela de pareamento saiu com 17 de 23 obras marcadas `0` — eu
quase reportei "o 08th está sem legenda em inglês", e ele tem 13.

---

# ✅ 2026-08-17 (tarde) — OS DOIS ITENS DE MELHORIA, APLICADOS E VALIDADOS NO ACERVO

Autorização do Paulo ao sair para a academia: *"pode aplicar tudo o que for necessario de testes e
correcoes e voce mesmo aplicar os testes nos animes que aplicamos agora"*.

## A — `ResumoAlteracaoPorEstilo` (`03159a51`): a prova sai em toda corrida

Saber se a 3.1 escreveu música exigia comparar backup com arquivo final **à mão**, num script fora
do produto. As duas mensagens do console eram verdadeiras e juntas escondiam o dano:
`[CACHE/RECUPERADO]` por evento (soa bem) e `corrigidas=0` no fim (a ponte escreve FORA do laço).

Agora cada arquivo gravado imprime `[ESTILOS] N linha(s) alterada(s): <estilo> N …`, comparando com
a foto do arquivo **como veio do disco** — tirada antes da ponte, senão o caminho que causou o dano
ficaria de fora. Estilo musical sai em VERMELHO com o total à parte. Três estados: comparado ·
nada mudou · **NÃO COMPARÁVEL** (com motivo).

## B — a guarda recusava a própria tradução que a tela existe para fazer (`03159a51`)

Das 27.987 falas do Zeta, 642 seguiam idênticas ao inglês em estilo `Dialogue`. Delas **634 são
nome próprio e estão CERTAS**. As 8 restantes, em três classes:

| classe | n | veredito |
|---|---|---|
| `{\clip(m … l …)}` | 4 | **CORRETAS.** O clip é polígono recortado sobre as letras INGLESAS; traduzir cortaria a frase PT nos lugares errados. Quem barra é o `DetectorEfeitoKaraokeService`, e está certo. |
| nome próprio puro (`The O!`, `Gate of Zedan?`) | 2 | corretas |
| `{\i1}…{\i0}\N{\i1}…{\i0}` | 2 | **defeito, e era MEU** |

A regra de repetição introduzida (nasceu em 16/08) é comparativa e pressupõe que a fala de hoje é
português. Quando ela é o inglês intacto, comparar contagem de palavra PORTUGUESA contra texto
INGLÊS não mede nada: o inglês tem `to` 2×, a tradução repete `para` 2×, e como `para` tinha zero
ocorrências "antes", a regra lia repetição introduzida. **Recusou o LLM e recusou o Google.**
Conserto: quando o texto visível da fala é igual ao do original, a proposta é TRADUÇÃO e essa
pergunta se abstém. As outras cinco seguem valendo.

## O defeito que só apareceu ao VALIDAR no acervo (`dd36c7d5`)

Rodei o Zeta para validar e as duas falas destravaram — uma delas quebrada:

```
antes : {\i1}So, you're saying the Titans went to{\i0}\N{\i1}Side Four to prepare a colony drop?{\i0}
Google: {\i1}Então, você está dizendo que o Titans foi para Side Four se preparar para um lançamento de colônia?
```

O `\N` e três tags sumiram: duas linhas viraram uma, com itálico que nunca fecha. As cinco
perguntas aprovaram — **nenhuma media estrutura**. Entrou a sétima pergunta:
`QUEBRA_DE_LINHA_PERDIDA`. Só `\N`, não tags — perder itálico é autorizado por decisão do Paulo;
quebra de linha é LAYOUT.

**A lição desta parte:** corrigir um gap sem rodar contra o acervo esconde o gap seguinte atrás do
primeiro.

## Validação no acervo — 8 obras, código novo

| obra | status | escreveu |
|---|---|---|
| 0080 · 86 Part 2 · Unicorn | `[SUCESSO]` | nada |
| 0083 · 08th · 86 Part 1 | pendências | nada |
| Guilty Crown | pendências | 1 linha, `Default` |
| Gundam ZZ | pendências | 1 linha, `Dialogue` |

As duas linhas escritas imprimiram `[ESTILOS]` corretamente. Medição final de música no acervo:
**22 das 23 obras com ZERO divergência** (a 23ª é o CCA, que é achatamento e decisão fechada).

**Zeta ficou FORA das corridas de validação de propósito** — tem o achado de lore aberto, e rodar
gravaria o defeito. As duas falas gravadas na corrida de validação foram **revertidas** do backup
`revisao_20260817_103032_173`, conferido BOM=True nas duas.

Suíte: 323 classes, 1.874 testes, 0 falhas, 26 pulados.

---

# ✅ 2026-08-17 — AUDITORIA MÉTODO A MÉTODO DA 3.1 (autorizada por Paulo)

Ordem dele: *"voce quer auditar metodo a metodo com testes para evitarmos mais surpresas o
revisao legendas? leve o tempo que quiaser!"*. A auditoria caça **classe de defeito**, não
defeito solto.

## Achado 1 — terceira porta de escrita sem veto de música (`f924768c`)

Critério de busca: *quem reescreve o texto de uma fala*. Medido: `EventoLegenda.comTexto` é a
**única** porta na fatia (zero `new EventoLegenda(...)` fora dos leitores e da `trocaTipoLegenda`).
Quatro portas, e o inventário completo virou catraca:

| porta | proteção |
|---|---|
| `SincronizadorLegendaCacheService` | veta por si (consertado ontem) |
| `RevisorPtOnlyUseCase` | **estava aberta** — veta por si agora |
| `PreparadorFalaRevisao` | chamador (`RevisarLegendasUseCase:419`) |
| `SessaoRevisaoArquivo` | chamador, mesmo laço |

`RevisorPtOnlyUseCase` alteraria **65 falas no 86 Part 1, 65/65 estilo `Ending`** ("choes!" →
acentuado). Zero diálogo. Nenhum controller o alcança — foi exatamente o estado da ponte do cache
até ela morder.

**`CatracaEscritaDeFalaVetaMusicaTest`** congela o inventário nominal. Mutação: 3 casos doentes,
3 asserções distintas reprovando (linhas 140/162/172); caso-controle em `@TempDir` verde.

## Achado 2 — pasta EN errada dava `[SUCESSO]` verde (`0b84b109`)

Lente de **boa-fé**, não adversarial. A tela depende de DUAS pastas e a ordem dos campos foi
invertida em 16/08. Medido com a pasta EN vazia: 4 falas não comparadas com nada, `status()` =
`CONCLUIDO`, banner **verde**. Regra 12 — cego e limpo davam o mesmo sinal.

O sinal já existia por arquivo (amarelo, `RevisarLegendasUseCase:503`) e morria ali. O conserto
carrega o que a produção já media: `SessaoRevisaoArquivo.ficouCego()` é a **única** definição, e
tanto o aviso por arquivo quanto o total do lote a consultam. Novo status
`CONCLUIDO_SEM_REFERENCIA`, com precedência **sobre** pendência (não saber é pior que saber que
falta). Perda PARCIAL de original segue sem alarme — alarme falso ensina a desligar o alarme.

Mutação: 2 casos doentes, 3 testes reprovando; contra-caso da pasta CERTA verde nas duas rodadas.

**Varredura da mesma classe fora da 3.1 (regra 5):** 2.1, 3.2, 3.3, Opção 5/7 e `traducaoCorrige`
já consultam o juiz de estilo musical. Quem não consulta é o `TraduzirKaraokeUseCase` (que **deve**
traduzir música) e o achatador (feature declarada). **O defeito não era sistêmico.**

Suíte: 320 classes, 1.862 testes, 0 falhas.

---

# 🔴→✅ 2026-08-17 — A PONTE DO CACHE FURAVA O VETO DE MÚSICA (dano REAL no acervo)

**COMO APARECEU:** Paulo rodou a 3.1 no **Gundam 08th MS Team** e o log mostrou 20, 58, 60, 76, 90
falas `[CACHE/RECUPERADO]` por episódio. Medido no backup que a própria corrida criou:

```
linhas efetivamente alteradas: 693
   687  Song ENG     <- 99,1% MUSICA
     6  Dialogue
```

Amostra do que entrou, restaurado de cache da era ANTERIOR ao veto:
```
ANTES : were watching the sun rise.
DEPOIS: Estamos assistindo o sol nascer.        <- trocou pessoa e tempo verbal
```

**A CAUSA:** `SincronizadorLegendaCacheService:127` só perguntava `evento.isDialogo()` — que
responde *"a linha é `Dialogue:`"*, e **não** *"o estilo é diálogo"*. A tela declara veto ABSOLUTO
de música na auditoria (`FiltroAuditoriaLinha:117`) e furava a própria invariante nesta ponte, que
roda ANTES dela. E o console anunciava `[CACHE/RECUPERADO]`, que soa como coisa boa.

**NÃO CONFUNDIR AS DUAS MEDIÇÕES:** esta é do 08th MS Team, 17/08. A do Zeta (1.008 de 1.027 em
`Song ENG`) é de 28/07 e está no `FiltroAuditoriaLinha` — mesma classe de dano, outra porta, um
mês antes. Foi ela que motivou o veto na auditoria.

**ACERVO REPARADO, cirurgicamente:** 687 linhas de música revertidas ao inglês (o espelho), as
**6 correções de diálogo preservadas**. Estado pré-revert guardado em
`%TEMP%\claude\…\scratchpad\08th-antes-do-revert`.
⚠️ **ERRO MEU NO REPARO, pego e corrigido:** o `WriteAllLines` gravou **sem BOM** nos 13 arquivos.
Restaurado; conferido `BOM=True`, contagem de eventos idêntica e o E01 voltou byte a byte
(44.639 → 44.639). **Sempre conferir BOM e CRLF depois de regravar `.ass`.**

**MECANISMO:** o veto entrou na ponte, perguntando à `PoliticaEstiloMusical` — o dono da regra.

```
MUTACAO (veto desligado) ..... 1 test failed — naoRestauraDoCacheUmaFalaDeEstiloMusical
   o contra-teste do mesmo metodo (dialogo ao lado continua sincronizando) seguiu VERDE
suite completa --rerun-tasks .. 1.852 testes, 0 falhas, 25 pulados, 318 classes
```

## ⚠️ ANTES DE SEGUIR PARA AS OUTRAS OBRAS

O cache do acervo tem letra de música traduzida pela revisão ANTIGA. Com o veto na ponte, ela não
volta mais para o `.ass` — mas **as obras já rodadas antes de hoje podem ter música escrita** pelo
mesmo caminho. **Não medido.** Zeta (50 arquivos) e ZZ (47) são os maiores candidatos.
**PRÓXIMA AÇÃO:** varrer as pastas `traducao_ptbr` do acervo contando linhas de estilo musical que
estejam em português, para saber o tamanho real do passivo antes de tocar em qualquer uma.

---

---

# ✅ 2026-08-16 23:41 — A 3.1 FECHADA E PROVADA EM DUAS OBRAS

```
GUILTY CROWN   23 arquivos | 5.754 falas | 2 detectados | 2 pendentes (ambas -> 3.3)
86 Part 1      11 arquivos | 3.469 falas | 2 detectados | 2 pendentes (ambas -> 3.3)
86 Part 2      12 arquivos | 3.552 falas | 0 detectados | 0 pendentes   [SUCESSO] CONCLUIDO
```

**Zero falas em inglês sobrando nas três pastas.** As 4 pendências totais são de concordância,
saem com `Fora do escopo desta tela` e são trabalho da 3.3 — que hoje ganhou o veto de música e
está pronta para recebê-las.

**AS 2 DO 86 SÃO O FALSO POSITIVO ESTRUTURAL JÁ DECLARADO**, e continuam com o texto de Paulo:
```
EN : It probably just thinks that he's a good bed.
PT : Provavelmente ela só acha que ele é uma boa cama.       (ep03 ev.271 e ep11 ev.222)
```
O inglês não marca gênero para o animal; o português exige `ela`. A tradução está certa e o
auditor não tem como saber. Preservar e encaminhar é o desfecho correto — **não perseguir**.

**IDEMPOTENTE:** rodadas repetidas não alteram arquivo nenhum. O ciclo que degradava a cada
passada, que abriu a noite às 22:12, não existe mais.

---

---

# ✅ FEITO (2026-08-16) — O DICIONÁRIO PAROU DE QUEBRAR TERMO DA LORE

**DEFEITO MEU, medido em produção horas depois de eu ligar o dicionário como ajudante:**

```
"Apocalypse Virus"  ->  "Apocalypse Vírus"      (confirmado com a classe de producao, jshell)
```

Ele acentua `Virus` porque em português é assim, quebra o termo canônico, e o portão de lore
recusa a proposta **inteira**. Resultado no Guilty Crown: a fala do ep13 ficou pendente em
**3 rodadas seguidas** — o ajudante custava a tradução que deveria ajudar a entregar. Sem dano
gravado (a guarda de lore pegou), com dano ao trabalho.

**CONSERTO:** o dicionário passou para `ProvedorCorrecaoFala`, e se o ajuste dele alterar termo
canônico o ajuste é **descartado** — a proposta do provedor segue intacta. O dicionário perde a
vez; a fala não. Usa o mesmo `termosCanonicosAlterados` do portão, não uma regra nova.

**A CATRACA DE ARQUITETURA REPROVOU A PRIMEIRA TENTATIVA, E ESTAVA CERTA:** eu tinha injetado
`ProtetorTermosLoreService` na `CadeiaCorrecaoFala`, criando uma TERCEIRA aresta cross-fatia
(`vivas=29 congeladas=28`). Subir o número seria o caminho fácil. O certo era outro: o contrato do
`ProvedorCorrecaoFala` já promete que *"tudo que sai daqui já passou por restauração de termos da
lore"* — o dicionário pertence lá dentro. Movido, a aresta some e a promessa fica verdadeira.

**FALSO-VERDE PAGO NO CAMINHO:** meu primeiro teste passou COM e SEM a guarda — o dublê do Google
devolve texto fixo que não contém o termo da lore, então o dicionário nunca o tocava. Reescrito em
modo LLM com `ensinar`, e aí a mutação reprova.

```
MUTACAO (guarda desligada) ...... 1 test failed — dicionarioEhIgnoradoQuandoAlterariaTermoDaLore
suite completa --rerun-tasks .... 1.851 testes, 0 falhas, 25 pulados, 318 classes
catraca de arquitetura .......... VERDE, sem aresta nova
```

## ✅ PROVADO NO ACERVO — Guilty Crown, 23:32

```
[23:32:18] Revisão não aplicada: O LLM não devolveu uma linha final utilizável...   <- via normal falhou
[23:32:18] PT corrigido: Dedicaremos nossos maiores esforços para erradicar o Apocalypse Virus
```

O espelho entrou, a frase saiu em português e o termo canônico **sobreviveu**. Pendências
**3 → 2**, exatamente como previsto. As 2 restantes são de concordância — trabalho da 3.3.

## O PLACAR DA NOITE NO GUILTY CROWN (23 arquivos, 5.754 falas)

```
                     detectados  corrigidas  pendentes
inicio (22:12)           14           6          8
depois do escopo          7           0          7
depois do espelho         7           4          3
depois do dicionario      3           1          2   <- so concordancia, que e da 3.3
```

**A 3.1 está fazendo exatamente o trabalho dela, e nada além dele.** É idempotente: rodar de novo
não muda nada.

---

---

# ✅ FEITO (2026-08-16) — VETO DE MÚSICA NA 3.3, o último 🔴 da etapa 3

Era o único vermelho, e é o **pré-requisito** para a 3.3 receber a concordância que a 3.1 passou a
encaminhar. Das três telas da etapa 3, só ela não vetava música.

**O PREJUÍZO MEDIDO ANTES DA GUARDA** (86, 2026-08-16): a tela via **22.568 de 26.524** eventos na
Part 1 (85,1%) e **49.458 de 53.175** na Part 2 (93,0%) — quase tudo sílaba solta de karaokê. E ela
mexe em GÊNERO, que é onde a heurística mais erra.

`RevisarConcordanciaUseCase` passa a perguntar a `PoliticaEstiloMusical` — o **dono** da regra, que
é peer — em vez de ter lista própria. Segunda lista divergiria em silêncio no dia em que um estilo
novo entrasse, e o sinal só apareceria numa legenda estragada.

```
MUTACAO (veto desligado) ........ 1 test failed — estiloMusicalNaoEhTocadoPorEstaTela
   o contra-teste aMesmaFalaEmDialogoContinuaSendoCorrigida seguiu VERDE
   (ele e o que separa "vetou musica" de "parou de funcionar")
suite completa --rerun-tasks .... 1.850 testes, 0 falhas, 25 pulados, 318 classes
```

🟡 **NÃO EXECUTADO:** não rodou no acervo. A 3.3 tem dry-run (`simular`) na tela — rodar no 86 e
conferir que as falas musicais deixam de aparecer é a prova que falta.

**AGORA A ETAPA 3 ESTÁ COERENTE:** 3.1 = falta de tradução · 3.2 = lore · 3.3 = concordância, e as
três vetam música. O passo 4 do plano (mover a concordância da 3.1 para a 3.3 de vez) deixou de
estar bloqueado.

---

---

# ✅ FEITO — ESPELHO DE ESTRUTURA (decisão de Paulo, 2026-08-16 noite)

```
espelho ligado em ProvedorCorrecaoFala.obterDoLlm
   portao de entrada: o texto VISIVEL do PT ainda e, letra por letra, o do ingles
   pedido refeito com o texto visivel do original -> nao ha marcador a perder
   remontagem: prefixo de tags do original + traducao   (enfase inline SAI, Paulo autorizou)

MUTACAO (espelho desligado) ..... 1 test failed — falaNaoTraduzidaComTagInlineSaiTraduzidaPeloEspelho
suite completa --rerun-tasks .... 1.848 testes, 0 falhas, 25 pulados, 318 classes
```

**DEFEITO DE DESENHO MEU, pego por `ProvedorCorrecaoFalaMarcadoresTest`:** eu liberei o espelho
pelo MOTIVO da auditoria (`exigeRetraducaoCompletaPeloLlm`), que inclui "resíduo gringo" — e
resíduo aparece em fala JÁ TRADUZIDA. O espelho retraduz a linha inteira: jogaria fora tradução
boa para consertar uma palavra. O portão virou a COMPARAÇÃO com o original, que é a segunda ideia
de Paulo na mesma conversa. Aquele teste é o contra-teste, e foi VISTO reprovando na versão frouxa.

## ✅ PROVADO NO ACERVO — Guilty Crown, 22:59, 23 arquivos / 5.754 falas

```
                    detectados  corrigidas  pendentes
antes do espelho         7           0          7
depois do espelho        7           4          3      <- a previsao era exatamente 3
```

As 4 falas com tag inline saíram traduzidas, sem itálico:

```
What {\i1}is{\i0} this?! Please, give back Inori!  ->  O que é isso?! Por favor, devolva Inori!
But I don't {\i1}want{\i0} to run away!            ->  Mas eu não quero fugir!
Weren't {\i1}you{\i0} the one who kept...          ->  Você não foi quem manteve os detalhes...
The quarantine won't last {\i1}that{\i0} long.     ->  A quarentena não durara tanto tempo.
```

As 3 pendências restantes são as CORRETAS: 2 de concordância (`cara`, `dele`) que a tela manda
para a 3.3, e o `Apocalypse Virus` que o portão de lore barra. A 2ª rodada (Google) confirmou:
3 detectados, 3 pendentes, nada novo.

## 🟡 DEFEITO PEQUENO NA SAÍDA, declarado e NÃO perseguido

`A quarentena não durara tanto tempo.` — falta o acento (`durará`), e o log mostra
`dicionário ajustou a proposta` nessa mesma linha. Não é falha do dicionário: **`durara` é
palavra válida** (mais-que-perfeito), então nenhum corretor ortográfico a acusa. É a família do
erro fluente já medida no projeto — acento se corrige por máquina, tempo verbal não.
UM caso não é medição (regra 14); se aparecer em volume, vira frente própria.

---

# (histórico) ESPELHO — o plano, antes de executar

**IDEIA DELE, e ela ELIMINA o cache do plano:** *"e as legendas originais não existem para servir
de espelho? o cache falha, não é uma solução muito melhor?"* — está certo, e por três motivos
medidos:

```
cache -> 8 entradas por episodio, blobs de 6.511 a 11.315 caracteres   (por LOTE)
.ass  -> uma linha por fala, com a estrutura de tag EXATA              (por FALA)
e o .ass original JA esta carregado: "Legenda .ass EN: Guilty Crown - 12_Track4.ass"
```

**O DEFEITO A FECHAR** (4 falas no Guilty Crown, corrida de 22:31): fala NÃO TRADUZIDA com tag
inline. O mascarador vira `{\i1}` em `[[TAG0]]`, o modelo não devolve o marcador, e a proposta é
recusada com `LLM_SEM_CONTEUDO_UTILIZAVEL`; o Google devolve `TAG_CORROMPIDA`. **As duas etapas
falham na mesma classe** — a classe que a tela existe para resolver.

**DECISÃO DE PRODUTO — opção A, pela régua "legível, não perfeito":** preservar o bloco de tags
INICIAL (posicionamento, `{\an8}` — perder isso move a legenda na tela) e **abrir mão da ênfase
inline**. Fala em português sem itálico lê-se; fala inteira em inglês, não.

```
EN  What {\i1}is{\i0} this?!    ->    PT  O que é isso?!        (perde o italico, entrega a fala)
EN  {\an8}I knew it...          ->    PT  {\an8}Eu sabia...     (mantem o \an8, que e posicao)
```

**POR QUE NÃO RECOLOCAR A ÊNFASE:** a palavra enfatizada muda de lugar em português; recolocar por
posição italiciza a palavra errada. Guarda que erra em silêncio é pior que capacidade perdida.

**ONDE:** `ProvedorCorrecaoFala.obterDoLlm` — quando a 1ª tentativa volta sem conteúdo utilizável
E o motivo é retradução completa (`PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm`), refazer o
pedido com o texto VISÍVEL do original (sem marcador nenhum, então não há o que perder) e remontar
`prefixo de tags do original + tradução`.

**ESCOPO FECHADO:** só vale para fala não traduzida. Fala já traduzida com tag continua pelo
caminho de hoje — ali a ênfase existe e não se joga fora.

---

---

# 🔴 EM ANDAMENTO (2026-08-16, fim da sessão `335d5be0`) — SIMPLIFICAÇÃO DA 3.1

DECISÃO DE PAULO: *"nesse menu temos de apenas traduzir tudo o que faltou! usando o motor de
traducao via llm, o google como segunda etapa na que a primeira falhou e como ajudantes das duas
etapas todos os dicionários... mas sempre bloqueando o karaoke"*. Cada menu numerado é uma etapa;
a 3.1 volta a ter uma frase só.

**ÁRVORE SUJA, NADA COMMITADO.** Suíte: **1.846 passam, 1 REPROVA** (ver bloqueio abaixo).

## ✅ FEITO E VERDE

1. **`PoliticaRetraducao.ehFalhaDeTraducao`** — o escopo da tela num nome só, e
   `exigeRetraducaoPeloGoogle` passa a delegar nele. Não são duas regras: "o que a 3.1 conserta" e
   "o que se pode mandar a tradutor sem lore" são a mesma pergunta.
2. **Portão de escopo em `CadeiaCorrecaoFala`**, com evidência `FORA_DO_ESCOPO_DA_TELA`.
   ⚠️ **ERRO MEU QUE 5 TESTES PEGARAM:** eu tinha posto o corte na `TriagemFalaSuspeita`, e ele
   levava junto a correção **determinística** — local, grátis, provada (`"Minha mãe"`←`"My dad"`
   sai corrigido no `.ass`). O corte certo é sobre **gastar rede**, não sobre enxergar. Revertido
   para a cadeia, e os 5 voltaram ao verde.
3. **Dicionários como AJUDANTES** da proposta (`revisorPtOnly` injetado na cadeia).
   **MEDIÇÃO QUE MATOU A OUTRA OPÇÃO:** varredura do arquivo inteiro alteraria 65 falas no 86
   Part 1 e **as 65 são estilo `Ending`** — 65 cópias do fragmento `choes!` (pedaço de `echoes!`
   pintado pelo gradiente) viradas em `chões!`. **Zero diálogo.** Era o `mae`→`mãe` do Unicorn por
   outra porta, e a ressalva do Paulo ("sempre bloqueando o karaokê") foi o que pegou. Como
   ajudante é seguro por construção: só age em fala que já passou pelo veto de música.
4. **Rótulo do provedor efetivo** — corrigida pelo Google na cascata não sai como `CORRIGIDA_LLM`.
5. **Perfil de teste** de `CorrecaoViaLlmChegaAoArquivoTest` ganhou o dublê do tradutor externo:
   sem ele, a cascata faria o teste bater na REDE de verdade. Defeito que eu introduzi.

## ✅ BLOQUEIO RESOLVIDO — era o INSTRUMENTO, não a produção

A sonda (`System.out` em `decidir`, já removida) mostrou a fala entrando duas vezes — mas eram
**dois testes diferentes**, cada um processando a fala UMA vez. Os dois passaram a usar
`"Get out of there!"` depois da re-fixturação.

**A causa:** os dublês são `@ApplicationScoped` e sobrevivem entre testes.
`CorrecaoViaLlmChegaAoArquivoTest.limparDuble()` zerava só o LLM; o `RecuperacaoExternaContadora`
que EU injetei nunca era zerado, então a contagem vazou do teste da cascata para o vizinho. O que
parecia "produção resolveu pelo LLM E mandou pro Google" era contador sujo.

Corrigido com `tradutorExterno.reiniciar()` no `@BeforeEach`, com o motivo no Javadoc.
**Suíte: 1.847 testes, 0 falhas, 25 pulados, 318 classes.**

MUTAÇÃO da cascata (`false &&`): **3 tests failed** — os dois unitários e o ponta-a-ponta. A
cascata é vista funcionando e vista faltando.

## 🟡 NÃO EXECUTADO — prova em produção

O `gradlew test` derrubou o dev mode (porta 8099 recusa conexão). A 3.1 nova **não foi rodada no
acervo**. Está provada em teste e ponta-a-ponta com dublê, não no `.ass` real.
**PRÓXIMA AÇÃO:** subir o KRONOS e rodar a 3.1 nas duas partes do 86.

## Histórico do bloqueio (mantido)

`CorrecaoViaLlmChegaAoArquivoTest > o defeito sem conserto local vai ao LLM e VOLTA corrigido`
falha com três fatos que não fecham entre si na MESMA execução:

```
llm.chamadas() == 1                          (só uma fala foi ao modelo)
saida contém "Saia daí!"                     (a proposta do LLM chegou ao arquivo)
tradutorExterno.pedidos() == [Get out of there!]   (a MESMA fala desceu para a 2ª etapa)
```

Se o LLM resolveu, a cascata não devia ter disparado. **Não ajustei a asserção para ficar verde** —
o teste está certo em reclamar. Hipóteses ainda NÃO verificadas: (a) a fala é processada duas vezes
pelo laço (referência de cache + evento); (b) o dublê do LLM responde diferente em
`corrigirTraducao` e em `revisarConcordancia`, e a via de retradução completa recusa antes;
(c) o `retry` do laço reentra com outro modo.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA

1. Instrumentar `CadeiaCorrecaoFala.decidir` com log por chamada (fala + modo + desfecho) e rodar
   SÓ `CorrecaoViaLlmChegaAoArquivoTest#concordanciaNominalCorrigidaPeloLlmChegaAoArquivo` para
   ver quantas vezes a fala entra e por qual via. A hipótese (a) é a mais barata de testar.
2. Só depois de a cascata estar explicada: rodar a 3.1 no 86 (as duas partes) e commitar.
3. **NÃO commitar antes disso** — cascata que grava no acervo e que eu não sei explicar é
   exatamente o que a regra 19 manda suspender.

---

---

# ⏸ 2026-08-16 — 3.1 REVISÃO DE LEGENDAS RODADA NO 86 (sessão `335d5be0`, portão rc=0)

TAREFA ORIGINAL: Paulo, 16/08 — *"nessa sessão só mexeremos com revisão legendas 3.1"*, com o
                 objetivo declarado de *"revisão se faltou tradução do anime, não dos karaokês"*.
                 Karaokê (4.1) fica por último, de propósito.
BRANCH / COMMIT BASE: main — `6b9bf1e8`, ahead 4 de origin/main.

## O MAPA DO MENU, conferido no `index.html` (armadilha de nome — não repetir o engano)

```
3.1 Revisão de Legendas      -> raspagemRevisao      TEM veto absoluto de música
3.3 Revisão de Concordância  -> revisaoConcordancia  NÃO tem veto de música
```
E dentro da PRÓPRIA 3.1 há dois botões: `/api/revisar-legendas` (Google) e
`/api/revisar-legendas-concordancia` (LLM). O segundo tem nome quase igual ao da 3.3 e **não é**
a 3.3. Escolher por semelhança de nome leva ao módulo errado.

## O VETO DE MÚSICA DA 3.1 — perguntado à produção, não lido

`jshell` sobre `build/classes/java/main`, `PadraoEstiloMusical.nomeDeclaraMusica`:
```
Opening -> true   Ending -> true   Song JP -> true      <- vetados em FiltroAuditoriaLinha:117
Default -> false  Default - Alt -> false                <- auditados  (controle negativo)
Signs   -> false  (cai no veto de "sign", linha 128)
OPL2    -> false  <- confirma a lacuna já conhecida da regex; o 86 não tem esse estilo
```
Por isso os 85,1% / 93,0% de eventos musicais do 86 **não chegam** na 3.1. Aquele número é da
3.3, e só dela.

## FECHADO COM ARTEFATO — 4 passadas, 23 episódios

```
                        auditadas  detectados  corrigidas  pendentes  arquivos alterados
Part 1  LLM  16:29:51      3.469        4           4          0            4
Part 1  Google 16:39:06    3.469        1           0          1            0
Part 2  LLM  16:39:53      3.552        0           0          0            0
Part 2  Google 16:40:1x    3.552        0           0          0            0
```
Relatórios em `relatorios/traducao_ptbr/`. Backup da única passada que escreveu:
`backups/revisao-legendas/revisao_20260816_162939_318` (4 arquivos).

**O teto que eu previ antes de rodar bateu com a produção:** 3.469 (previsto 3.469) e
3.552 (previsto 3.553).

## RESPOSTA À PERGUNTA DO PAULO — "faltou tradução do anime?"

Do log de `falas-nao-traduzidas` da tradução de hoje (08:09→08:48), SÓ os arquivos de hoje:
```
78.321 registros | 78.173 PRESERVADA_POR_REGRA (99,81%, veto de música/karaokê intencional)
                 |    143 TRADUCAO_IGUAL_AO_ORIGINAL |  5 PENDENTE
```
**Praticamente não faltou.** Em 23 episódios, UMA fala de diálogo estava em inglês cru, e a 3.1
a recuperou:
```
ep06 evento 62   EN     : She'll greet you with, "And a fine morning to you!"
                 ANTES  : (idêntico ao inglês)
                 DEPOIS : Ela lhe dará as boas-vindas com um "Bom dia para você!"
```

## 🔴 DANO QUE A PASSADA LLM CAUSOU — 2 linhas, e NENHUMA opção do menu enxerga

```
EN     : It probably just thinks that he's a good bed.
ANTES  : Provavelmente, ele apenas pensa que ela é uma boa cama.      <- gênero trocado
DEPOIS : Provavelmente, ela provavelmente pensa que ele é uma boa cama.   (ep03 evento 271)
DEPOIS : {\i1}Provavelmente ele provavelmente pensa que é uma boa cama.   (ep11 evento 222)
```
O detector estava CERTO (o original diz `he`, a tradução dizia `ela`). A `GuardaCorrecaoSegura`
mede concordância e **não mede regressão de fluência**, então aceitou a proposta que acerta o
gênero e dobra o advérbio.

MEDIDO: a passada Google reauditou as MESMAS 3.469 falas depois disso e **não tocou nas duas**
(0 arquivos alterados). Varrida a base inteira por detector de repetição: só existe
`DetectorTraducaoIdenticaService.PADRAO_GAGUEIRA_NOME`, que NORMALIZA gagueira de nome para
comparar ("Sh-Shin"), não corrige advérbio dobrado. **Não há opção de menu que repare isso.**

## 🔴 GAP NOVO — pendência sem detalhe é saída vazia ambígua (regra 12)

`revisao_legendas_20260816_163906.txt`: `Problemas detectados: 1 · Falas pendentes: 1` e
`DETALHES POR OCORRÊNCIA: Nenhuma ocorrência detalhada registrada.` O operador não tem como
saber QUAL fala. Causa provável, por leitura: `CadeiaCorrecaoFala:187` só cria `DetalheRevisao`
quando `recusada.codigo() != null` — recusa sem código vira pendência invisível. NÃO comprovado
(não isolei a fala).

## ERRO MEU, CORRIGIDO E COM A CAUSA ACHADA

Tentei apontar as falas pelo índice do log `falas-nao-traduzidas` e mostrei linhas erradas. O
relatório oficial deu a causa: **deslocamento constante de 2** (log `Evento: 62` = 60º evento
`Dialogue:` do arquivo). Os números 143/5 são da produção e valem; a lista de exemplos que
publiquei no chat, não.

## ✅ O MENU RODADO DE NOVO PIOROU — e por isso a correção foi para o MECANISMO

Ordem de Paulo: *"para isso temos as opções de menu do sistema para serem usadas"* e *"nesse caso
é legenda então acho que isso vale a pena em 3.1"*. Testado antes de aceitar: 2ª passada LLM na
Part 1 (16:45), o detector acusou a MESMA fala (o `ela` ainda estava lá) e a via "consertou"
**apagando o sujeito** — `"Provavelmente, provavelmente pensa que ele é uma boa cama."`
**A opção de menu não é idempotente nesta classe: degrada a cada rodada.**

ep03 e ep11 **restaurados** do backup `revisao_20260816_162939_318`. O estado degradado ficou em
`%TEMP%\claude\...\335d5be0...\scratchpad\estado-degradado` para auditoria. Mantidos: o ganho do
ep06 (resíduo inglês traduzido) e a troca do ep02 (`filho da puta`→`seu merda`, escolha de texto).

## ✅ SEXTA PERGUNTA DO PORTÃO — `GuardaCorrecaoSegura` (árvore suja, NADA commitado)

A régua é **COMPARATIVA**, e a medição é a razão: 197 das 7.022 falas de diálogo do 86 (**2,81%**)
já repetem palavra longa — `Pare, pare!`, `Certo, certo.`, `unidades … unidades`. Régua absoluta
reprovaria as 197, e guarda que reprova texto correto ensina a desligar a guarda. Só conta o que a
PROPOSTA acrescenta; piso de 4 letras exclui repetição gramatical (`que`, `de`, `ela`).

Reusa `protecaoAss.textoVisivel` em vez de regex nova — a catraca de regra duplicada entre fatias
não se mexeu.

```
gradlew test --tests *GuardaCorrecaoSeguraTest* --rerun-tasks ... rc=0
MUTACAO (regra desligada com `false &&`) ................. 8 tests, 1 failed
   reprovou SO propostaQueDobraAdverbioEhRejeitadaEAvisaOperador
   o contra-teste repeticaoQueJaExistiaNaFalaNaoBarraACorrecao seguiu VERDE
   -> a guarda VE o defeito e NAO e alarme falso
suite completa --rerun-tasks ...... 1.840 testes, 0 falhas, 25 pulados, 318 classes
```

## ✅ PROVADO NO FLUXO REAL — 3.1 rodada com a guarda nova (16:57, relatório `...165702`)

O LLM propôs **exatamente o mesmo texto ruim** e o portão recusou os dois:

```
Problemas detectados: 2 | Corrigidas via LLM: 0 | Pendentes: 2
ep03 ev.271  LLM_REJEITADO_SEM_MELHORIA   proposta: "Provavelmente, ela provavelmente pensa..."
ep11 ev.222  LLM_REJEITADO_SEM_MELHORIA   proposta: "{\i1}Provavelmente ele provavelmente pensa..."
arquivos alterados: 0 | backups novos: 0 | as 2 falas intactas no .ass
```

O ciclo que degradava a cada rodada **parou**: a fala fica PENDENTE e visível no relatório, com a
proposta registrada para o operador julgar, em vez de ser gravada.

## 🟡 GAP MENOR, declarado

O relatório rotula a recusa como `LLM_REJEITADO_SEM_MELHORIA` e imprime o diagnóstico genérico
"resposta LLM inválida ou sem melhoria". O motivo específico ("repete uma palavra que a fala não
repetia") sai no CONSOLE, via `avisosAoOperador`, e não entra no `DetalheRevisao`. Quem ler só o
arquivo de relatório não sabe qual das seis perguntas barrou.

## ✅ MODELO: aya-expanse-8b É MELHOR QUE mistral-nemo NESTA PASSADA (medido, 16/08)

Paulo trocou o modelo. Comparação nas MESMAS 2 falas, mesmo ponto de partida:

```
                mistral-nemo                              aya-expanse-8b
ep03  "…ela provavelmente pensa que ele…"  RECUSADA   "…ela apenas pensa que é…"  APROVADA
ep11  "…ele provavelmente pensa que é…"    RECUSADA   LLM_SEM_CONTEUDO_UTILIZAVEL (pendente honesta)
```

Dois motivos, e o segundo importa tanto quanto o primeiro: a aya não dobra advérbio, **e quando
não consegue ela declara** em vez de devolver frase estragada. O mistral produziu lixo confiante
nas duas.

**ALCANCE DO TESTE, declarado:** o modelo NÃO muda o que é detectado — a detecção é determinística
(`AuditorProblemasLegendaService`). Ele muda só a PROPOSTA. Por isso a Part 2 segue em zero e não
precisa de nova rodada, e a comparação vale para estas 2 falas, não para o acervo.

**ARMADILHA DE MEDIÇÃO PAGA NESTA SESSÃO:** `/v1/models` lista o CATÁLOGO baixado (5 ids), não o
que está em memória. Quem confere estado real é `/api/v0/models` (campo `state`). Eu afirmei
"cinco modelos carregados" e era UM. O próprio `LlmClientAdapter:96-99` já documenta a diferença.
E `escolherEntreCarregados:191` cai em `carregados.get(0)` porque o configurado é `"current"` e não
casa com id nenhum — **trocar de modelo exige DESCARREGAR o anterior**, não só subir outro.

## ✅ TEXTO FINAL DAS 2 FALAS — escrito por decisão de Paulo (16/08, "sim")

```
ep03  Provavelmente ela só acha que ele é uma boa cama.
ep11  {\i1}Provavelmente ela só acha que ele é uma boa cama.
```

Edição direta no `.ass` porque **nenhuma opção do menu escreve texto de autor** — o menu propõe e
julga, não redige. Feita com troca de ocorrência ÚNICA conferida (`1 ocorrência` em cada arquivo,
abortaria se fosse diferente), BOM UTF-8 e CRLF preservados, tag `{\i1}` intacta. Cópia do estado
anterior em `%TEMP%\claude\…\scratchpad\antes-texto-paulo`.

**VALIDAÇÃO INDEPENDENTE:** rodada a 3.1 de novo depois da edição, a aya devolveu
`LLM_SEM_ALTERACAO` no ep03 — *"o modelo respondeu, mas manteve a tradução atual"*, propondo texto
IDÊNTICO ao escrito. Um modelo independente não achou o que mudar.

## 🟡 FALSO POSITIVO DE CLASSE CONHECIDA — não perseguir

As 2 falas **continuam sendo detectadas** e sempre serão:

```
Problemas: Original usa 'he' sem referência feminina, mas a tradução contém o feminino 'ela'
```

O inglês diz `It … he` sobre um animal; o português exige gênero, e a gata é `ela`. A regra do
auditor não tem como saber o gênero do bicho em PT. É **falso positivo estrutural de EN→PT**, não
defeito da legenda — e o desfecho é o correto: fala preservada, pendência visível. Um dos dois
motivos anteriores sumiu com o texto novo (eram 2, ficou 1).

## 🟡 PADRÃO A OBSERVAR, sem ação

O ep11 deu `LLM_SEM_CONTEUDO_UTILIZAVEL` nas DUAS rodadas da aya, e é a única das duas que tem
`{\i1}` no início. Mesma frase, mesma lore, mesmo modelo — o que difere é a tag. Um caso não é
medição; se repetir em outra obra, é a cicatriz do marcador perdido voltando por outra porta.

## ✅ RITO COMPLETO APLICADO SOBRE O TRABALHO DESTA SESSÃO (16/08, ordem de Paulo)

**PARTE A — refutar as próprias conclusões.**

| lente | alvo | veredito |
|---|---|---|
| o instrumento mediu o certo? | minha edição manual nos 2 `.ass` | **sobrevive** — diff byte a byte: 1 linha alterada por arquivo, contagem de linhas e de eventos IDÊNTICAS (2841/2667 e 2024/1758) |
| explicação mais simples? | "faltou tradução = 1 fala" | **sobrevive, e agora com prova** — os 143 `TRADUCAO_IGUAL_AO_ORIGINAL` mapeados: **143 de 143 seguem PT==EN** e são nome próprio (`Vladilena Milizé`, `Shin! Shinei Nouzen!`, `Daiya…`, `Humbert!`). Fecha o `Não comprovado` anterior |
| que dano passou? | a guarda que EU escrevi | **DERRUBADA** — piso de 4 letras deixava `ele ele` passar |

**CALIBRAÇÃO DO DESLOCAMENTO** (o mapeamento acima só vale porque o instrumento discrimina):

```
offset 0 ->  27 de 143 identicos (18,9%)     offset 2 -> 143 de 143 (100,0%)   <- o certo
offset 1 ->  26 de 143 (18,2%)               offset 3 ->  26 de 143 (18,2%)
```

**PARTE B — revisão de design da guarda nova.**

- **Adversarial:** achou o furo do piso. **FECHADO** com a regra de adjacência (`colarPalavrasIguais`),
  e o furo não era hipótese: na 2ª rodada o mistral devolveu `"Provavelmente, provavelmente pensa…"`,
  adjacente — só não escapou porque a palavra é longa.
- **Boa-fé:** baixar o piso reprovaria a correção que a 3.1 MAIS faz — acrescentar pronome
  (`"Ele disse que viria"` → `"…que ele viria"`, `ele` de 1 para 2). Por isso a régua exige
  ADJACÊNCIA em vez de piso menor, e há teste dedicado (`pronomeAcrescentadoLongeDoOutroNaoBarra…`).
  Medido: 57 das 7.022 falas (0,81%) já colam palavras iguais, **todas legítimas** (`Sim, sim.`,
  `Certo, certo.`, `Manhã! Manhã!`, `Buá! Buá!`) — e passam, porque a comparação é do que a proposta
  ACRESCENTA.
- **Falha operacional:** eu introduzi chamada nova a `protecaoAss.textoVisivel` num método cujo
  contrato diz "nunca lança". Conferido em `ProtecaoLegendaAssService:178-188`: trata `null`, só faz
  `replaceAll`/`replace`/`strip`, sem caminho de exceção. **Invariante intacta.**

```
MUTACAO da regra de adjacencia (isolada, `false &&`) ... 10 tests, 1 failed
   reprovou SO propostaQueColaPalavraCurtaRepetidaEhRejeitada
suite completa --rerun-tasks ..... 1.842 testes, 0 falhas, 25 pulados, 318 classes
FLUXO REAL depois da mudanca ..... 2 detectados, 0 gravados, 0 arquivos alterados (sem regressao)
```

## 🟡 O QUE O RITO NÃO CONSEGUIU MEDIR — lacuna conhecida, não silenciosa

O **falso positivo da guarda nova** não tem como ser medido no acervo: ela julga PROPOSTAS, e não
existe corpus de propostas históricas — só as 4 de hoje. O que foi medido é o proxy (repetição
legítima já existente nas falas, que a régua comparativa protege por construção). Para virar
medição de verdade seria preciso registrar as propostas recusadas ao longo do uso.

## ✅ PENDÊNCIA INVISÍVEL E RÓTULO GENÉRICO — FECHADOS (16/08)

**INVARIANTE DECLARADA — INV-REVISAO-EVIDENCIA-001:** nenhuma fala contada como PENDENTE pode
existir sem linha de evidência no relatório. *Dano se quebrado:* o operador lê "Pendentes: N",
não tem como agir, e "nada a fazer aqui" fica idêntico a "não consegui". *Camadas:* código
obrigatório no provedor · motivo tipado no portão · testes com mutação.

**A CAUSA, achada no código e reproduzida por medição** (não deduzida): `ProvedorCorrecaoFala`
tinha DOIS caminhos de recusa com `codigo=null`, e recusa sem código não gera `DetalheRevisao`.
Reproduzido na hora: passada Google no 86 Part 1 = **2 detectados, 2 pendentes, ZERO detalhes**
— 100% das pendências invisíveis. `RevisarLegendasUseCase:292` conta TODA `DecisaoFala.Pendente`,
com ou sem evidência, o que mata a objeção de "vai inflar o relatório": a fala já era contada.

**OPÇÕES CONSIDERADAS, e por que as outras foram descartadas:**

| opção | veredito |
|---|---|
| A — dar código às duas recusas nulas | **escolhida**, resolve 100% do medido, +0 na contagem |
| B — motivo tipado no portão | **escolhida**, resolve o rótulo genérico e torna auditável qual pergunta reprova mais |
| C — invariante "toda decisão gera evidência" | descartada: quebraria o reaproveitamento de memória, que é intencional e documentado |
| D — resumo agregado por motivo | descartada: diz por quê e não diz QUAL fala; o operador continua sem poder agir |
| E — guarda de relatório (`pendentes>0 ⟹ detalhes>0`) | adiada: A+B a tornam redundante hoje; vira catraca quando houver 2º produtor de recusa |

**FEITO:** `GOOGLE_NAO_ACIONADO` e `GOOGLE_SEM_ALTERACAO` no provedor · `MotivoRecusa` (enum de
6 valores com código e descrição) viajando dentro de `Veredicto.Rejeitada` · `CadeiaCorrecaoFala`
usa o código do motivo em vez de `LLM_REJEITADO_SEM_MELHORIA`.

```
MUTACAO (codigo de volta para null) ..... 6 tests, 1 failed — so o caso doente
suite completa --rerun-tasks ............ 1.845 testes, 0 falhas, 25 pulados, 318 classes
FLUXO REAL, mesmo relatorio de antes:
   antes : "Pendentes: 2"  +  "Nenhuma ocorrencia detalhada registrada."
   agora : as 2 nomeadas, com Resultado=GOOGLE_NAO_ACIONADO, o motivo da auditoria,
           o EN, o PT e o diagnostico que diz o que fazer (rodar a passada LLM)
```

**ACHADO DE BRINDE, do motivo tipado:** o teste `propostaQueNaoMelhoraAAuditoriaEhRejeitada`
**nunca exercitou** a pergunta do nome dele — a proposta é barrada antes, por `PROBLEMA_NOVO`.
Só apareceu porque o veredito passou a dizer QUAL pergunta barrou. Corrigido, e criado
`propostaComOMesmoDefeitoDoOriginalParaNaUltimaPergunta`, que injeta o auditor REAL: com motivo
sintético qualquer motivo apurado conta como "novo" e a execução para uma pergunta antes.

## 🟡 NÃO COMPROVADO desta rodada

O rótulo tipado na recusa do PORTÃO está provado em teste, **não no arquivo**: em produção as 2
falas param antes, no provedor (`LLM_SEM_ALTERACAO` / `LLM_SEM_CONTEUDO_UTILIZAVEL`). Provar no
fluxo real exigiria plantar de volta a proposta ruim no acervo, e não vale desestabilizar a
legenda por isso.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA
3. Só então **4.1 Tradução de Karaokê**, que é terminal e gera a pasta irmã final. Com a aya
   carregada, e não o mistral.

## NÃO REPETIR

- Não mapear fala pelo índice do `falas-nao-traduzidas` sem descontar o deslocamento de 2 —
  e sem conferir contra o relatório oficial da revisão.
- Não medir estilo musical com regex própria: perguntar a `PadraoEstiloMusical` via `jshell`
  sobre `build/classes/java/main`. Minha regex concordou por acaso neste acervo.
- A pasta `logs/falas-nao-traduzidas/86 Part 1` tem 23 arquivos para 11 episódios: 12 são de
  14/08, quando os eps da Part 2 moravam na pasta da Part 1. Filtrar por data, sempre.

---

> **DUAS SESSÕES EM PARALELO em 2026-08-15.** Paulo dividiu o trabalho: uma sessão só na
> **Tradução Local** (esta seção) e outra só no **Karaokê** (as seções abaixo). Consequências
> operacionais medidas: o comprovante `~/.claude/LEITURA-REGRA-ATUAL.md` é ARQUIVO ÚNICO e as
> duas sessões se sobrescrevem (aconteceu 3×); a árvore de trabalho é compartilhada e o
> `:compileJava` chegou a reprovar por edição em curso da outra frente. Commitar SÓ os
> próprios arquivos, nominalmente.

---

# ✅ FECHADO (2026-08-15) — aviso sonoro de fim de lote na Tradução Local

TAREFA ORIGINAL: "ao fim da tradução nesse menu, forçar o computador a dar um alerta sonoro
                 que toca 3x" (Paulo, 15/08). Inspiração declarada: o som de fim de tarefa do
                 Antigravity — *"mas aqui estamos no VS Code"*, e foi essa frase que definiu o
                 desenho.
COMMIT: `79531dc0` (5 arquivos, nada da outra frente junto). Suíte 1811/0 falhas/305 classes.

## O desenho, e o que ele RECUSA

Máquina primeiro (`AvisoSonoroSistema`, PowerShell `[console]::beep`, três estados), navegador
como SEGUNDA VIA — o veredito do som viaja no corpo do evento (`<desfecho>|<TOCOU|...>`) para a
tela não tocar por cima. Beep de navegador sozinho não serve a quem fechou o navegador e foi
programar: depende de aba aberta, autoplay liberado e aba fora do mudo.

Duas alternativas recusadas, com o motivo:
- **casar a string do banner** `[CONCLUÍDO] TRADUÇÃO LOCAL VIA LLM` — rótulo é apresentação;
  trocar a palavra emudeceria o aviso sem quebrar nada visível;
- **tocar só no sucesso** — o corpo do job tem QUATRO returns antecipados e são os piores
  casos: morre em 2s e quem saiu de perto espera horas. O aviso mora no `finally`.

## FECHADO COM ARTEFATO

```
beep real (o mesmo comando que o Java monta) .... exit=0, 2,80s, AUDÍVEL (Paulo confirmou)
AvisoFimDeLoteTest ............................. tests=1 failures=0
MUTAÇÃO (publicar só no sucesso) ............... 1 test, 1 failed  <- a guarda VÊ o defeito
suíte --rerun-tasks ............................ 1811 testes, 0 falhas, 25 pulados, 305 classes
FLUXO REAL, app no ar, pasta vazia de propósito:
  06:43:01.844 [traducao]            Nenhum arquivo de legenda encontrado   <- saída antecipada
  06:43:03.450 [traducao-finalizada] ENCERRADO SEM RELATÓRIO|TOCOU
  06:43:03.457 [traducao]            [RELATÓRIO FINAL] Tempo total: 1,6s
```

Os DOIS caminhos antecipados estão provados por instrumentos diferentes: LLM offline no teste
unitário, zero arquivos no fluxo real (o LLM estava ONLINE nessa execução).

## NÃO COMPROVADO

- **A tela**: o evento saiu, mas o toast e a linha de console não foram vistos no navegador
  (Playwright fora do ar nesta sessão). O beep da máquina não depende disso.
- **O caminho `CONCLUÍDO`**: só se prova na primeira tradução de verdade que terminar bem.

## GAPS DECLARADOS (nenhum corrigido, todos conhecidos)

- 🔴 JVM morta no meio (live-reload, kill) não roda o `finally`: sem aviso. Sem correção
  possível de dentro do processo.
- 🟡 Em contêiner Linux não há som na máquina; o navegador assume — e se ele também estiver
  fechado, ninguém avisa.
- 🟡 `TOCOU` significa "o comando rodou", não "foi ouvido". Máquina sem dispositivo de áudio
  daria exit 0 e a tela suprimiria o beep dela.
- 🟡 Três toques IGUAIS no sucesso e na falha: quem ouve de longe assume que deu certo. A tela
  distingue, o som não. Decisão de produto pendente de Paulo (toque grave para falha?).
- 🟡 Duplo clique em Traduzir enfileira dois lotes → dois avisos. Defeito pré-existente; o que
  mudou é que agora ele é audível.
- FORA DE ESCOPO: a tela **2.2 Tradução sem Lore** recebe o evento e ainda não o escuta. O
  backend já publica para ela; falta só o ouvinte no `traducaoSemLore.js`.

## ARMADILHAS DE INSTRUMENTO DESTA SESSÃO (não repetir)

- **`curl` por Git Bash mangla caminho do Windows.** O POST com `C:\...` chegava com o JSON
  corrompido e a rota devolvia **400 com corpo VAZIO e sem exceção no log** — que parece
  defeito da aplicação e é do instrumento. O que denunciou foi a mensagem de recusa mostrando
  `C:\nao-existe-xyz` com o `\n` virado quebra de linha real. Usar `Invoke-RestMethod` do
  PowerShell para qualquer POST com caminho.
- **`app.js` é carregado SEM `?v=`**, de propósito (`index.html:2114` — `?v=` ali duplicaria o
  orquestrador e os listeners). Depois de alterar `app.js`, só **Ctrl+Shift+R** traz a versão
  nova; e como o import versionado do módulo mora DENTRO do `app.js`, um `app.js` em cache
  também esconde o `?v=` novo dos módulos.

---

# 🔴 ORDEM DE PAULO (2026-08-15) — TODAS AS LORES EM UM ARQUIVO ÚNICO

*"todas as lores devem ficar em um unico arquivo!"*

**Isto SUBSTITUI a FASE 1 entrada a entrada abaixo.** Com arquivo único os dois catálogos
colapsam **por construção** — as 16 obras que faltavam unificar deixam de ser trabalho.

## O que foi MEDIDO antes de propor forma

```
162 arquivos · 11.369 linhas · 596 KB      (82 tradução + 80 revisão)
lógica condicional ................ ZERO   -> a lore é DADO, não comportamento
   (única exceção: 5 filmes do Reconguista reusam termosProtegidos da série — também é dado)
Javadoc de método ................. 3.285 linhas -> some (os métodos viram 2 classes genéricas)
comentário INLINE (cicatriz) ......   477 linhas -> TEM de sobreviver
   89% em 8 arquivos: CorrecoesTerminologiaGundamUc 97 · Contexto86 56 · 08thMSTeam 50 ·
   Unicorn 50 · Zeta 38 · GundamZz 29 · CharsCounterattack 27 · F91 27
```

**FORMATO: YAML.** A razão é a cicatriz — YAML aceita comentário, JSON não. Em JSON as 477
linhas morreriam ou virariam campo de texto morto. SnakeYAML 2.6 **já está no classpath** por
`quarkus-config-yaml`; não entra dependência nova.

## FASES (escopo FECHADO)

- **✅ FASE A — FEITA, commit `00a1539b`.** Congelar os 3 campos que nenhuma guarda cobria.
- **FASE B — gerar o YAML A PARTIR dos provedores reais** (nunca digitado), com os DOIS mapas
  de terminologia lado a lado (`correcoesTraducao` / `correcoesRevisao`), e migrar as 477
  linhas de cicatriz como comentário.
- **FASE C — 2 classes genéricas** que leem o arquivo e produzem os 69+69 provedores por CDI.
- **FASE D — apagar as 162 classes**, com as guardas verdes provando equivalência.
- **FASE E — unificar os dois mapas DENTRO do arquivo** (aí sim a decisão de terminologia).
- **FASE F — `lore` vira PACOTE IRMÃO de primeiro nível** (Paulo, 15/08): *"o certo era ele se
  tornar um pacote irmão e ser aproveitado por todos"* + *"ao invés de `revisaoLore` ele passaria
  a se chamar `lore`, e o `revisaoLore` continua como função interna à parte"*.

  ```
  org.traducao.projeto.lore          PACOTE IRMÃO — toda a lore, consumida por todos
  org.traducao.projeto.revisaoLore   FATIA — só a FUNÇÃO de revisão (25 arquivos)
  org.traducao.projeto.contexto      12 de maquinaria, absorvidos por `lore`
  ```

  O achado que simplifica: **o irmão já existe em função, só não no nome.**
  `contexto` = 94 arquivos (82 lore + 12 maquinaria) · `revisaoLore` = 105 (80 lore + 25 função).
  Os dois devolvem o dado e ficam com a função. Renomear toca import de **26 consumidores** e as
  regras ArchUnit — por isso vai **depois** de D e E, quando as 162 classes já não existirem e o
  diff for pequeno.

  Registrado no vault: [[decisoes/2026-08-15-lore-arquivo-unico-e-fonte-unica]] (commit `4bfad03`).

**POR QUE B E E SÃO SEPARADAS, e não fazer as duas de uma vez:** a FASE B tem de ser MOVE PURO
— só assim as guardas provam "nada mudou". Unir os mapas no mesmo passo é uma MUDANÇA, e aí
uma falha não distingue "o move quebrou" de "a união quebrou". É o mesmo motivo pelo qual a
E7a moveu o peer com gate de hash antes de tocar em conteúdo.

## AS TRÊS REDES QUE JÁ EXISTEM E ESTÃO CALIBRADAS

```
manifesto-lore.properties ......... prompt + nome + termosProtegidos (hash por obra)
baseline-terminologia-lore.tsv .... 4.104 entradas de terminologia      (78f41df0)
baseline-campos-lore.tsv .......... 108 linhas: apelido, par, visibilidade (00a1539b)
```
Um arquivo único que passe nas TRÊS provou que não mexeu em nada. **Não reescrever nenhum dos
dois `.tsv` durante as fases** — eles continuarem verdes *é* a prova.

## RISCO RESIDUAL DECLARADO

Arquivo de ~10 mil linhas é ímã de conflito entre as duas sessões que trabalham nesta árvore.
Não muda a decisão; quem editar tem de fazê-lo por obra, nunca em bloco.

## ✅ FASES B e C — FEITAS, commit `80669af8` (pushado)

```
src/main/resources/lore/lore-traducao.yaml ... 69 obras · 8.923 linhas · 523.476 bytes
GeradorLoreYamlIT ..... gera dos provedores REAIS e prova IDA E VOLTA (aborta sem escrever)
CatalogoLoreYaml ...... leitor, falha FECHADA, 4 fixtures doentes versionadas
EquivalenciaLoreYamlIT  69/69 obras × 7 campos == classes Java  <- É O PORTÃO DA FASE D
   calibrado: nome/prompt/entrada alterados => rc=1 apontando obra e campo; real => rc=0
suíte --rerun-tasks ... 1.824 testes, 0 falhas, 25 pulados, 315 classes
```

A catraca `FronteiraContextoArchTest` **reprovou a classe nova antes do commit** (congelamento
nominal de `contexto.infrastructure`). Não é alarme falso — `CatalogoLoreYaml` entrou na lista
de propósito, com o motivo escrito no teste.

## 🔴 RISCO QUE EU CRIEI, e a guarda que ele exige ANTES da próxima etapa

O YAML é **gerado**, e as 477 linhas de cicatriz vão ser escritas **à mão** dentro dele. Quem
rodar o gerador e copiar por cima **apaga todas** — e o arquivo continuaria passando em TODAS
as guardas de hoje, porque nenhuma delas olha comentário. Seria perda silenciosa da parte mais
valiosa da lore.

**Antes de migrar a primeira cicatriz:** guarda que conta as linhas de comentário do
`lore-traducao.yaml` com **linha de base como catraca** (só sobe). Arquivo regenerado, sem
comentário, tem de REPROVAR. Sem ela a migração é trabalho que qualquer `Copy-Item` distraído
desfaz.

## ⚠️ ORDEM OBRIGATÓRIA DAS DUAS ÚLTIMAS FASES

**A cicatriz migra ANTES de a FASE D apagar as classes.** Se apagar primeiro, as 477 linhas
saem da árvore viva e passam a existir só no histórico do git — que é onde ninguém lê antes de
mexer numa lore. É a regra 4 (olhar antes de destruir) aplicada ao que não é código.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA

1. **Guarda de comentário** do `lore-traducao.yaml`, catraca com linha de base, calibrada
   contra o arquivo regenerado (que tem ZERO comentário e tem de reprovar).
2. **Migrar a cicatriz**, obra a obra, começando pelos 8 que são 89% dela:
   `CorrecoesTerminologiaGundamUc` 97 · `Contexto86` 56 · `ContextoGundam08thMSTeam` 50 ·
   `ContextoGundamUnicorn` 50 · `ContextoGundamZeta` 38 · `CorrecoesTerminologiaGundamZz` 29 ·
   `ContextoCharsCounterattack` 27 · `ContextoGundamF91` 27.
3. **FASE D:** trocar `ContextoBeansConfig` para produzir de `CatalogoLoreYaml` e apagar as 82
   classes, com `EquivalenciaLoreYamlIT` verde antes e as três redes verdes depois.
4. **FASE E:** a revisão (80 arquivos) entra no mesmo arquivo, e aí os dois mapas se unem.

---

# 🆕 FRENTE ABERTA PARA SESSÃO PRÓPRIA — fonte única de terminologia de lore

Paulo decidiu em 2026-08-15, com estas palavras: *"a lore tem de ser compartilhada, é a única
exceção. Se não, temos problemas que não valem a pena."* E pediu que esta frente rode em
**assistente novo**, separada da Tradução Local e do Karaokê. O que segue é tudo o que ela
precisa para começar sem repetir medição.

## O QUE JÁ ESTÁ MEDIDO (não remedir — os harnesses estão commitados em `2bba92a7`)

```
MedicaoDivergenciaEntreCatalogosDeLoreIT
  68 obras nos DOIS catalogos | 17 divergentes (25%)
  18 entradas so na TRADUCAO | 69 so na REVISAO | ZERO conflito
  -> unificar e UNIAO, nao arbitragem

MedicaoTermoDeLorePerdidoIT
  100.220 pares | 1.547 termos de lore ausentes na traducao (1,54%)
  Zeta 3,31% (pior) | 86 0,62% | A.E.U.G. 209 · Mobile Suit 226 · Freya Familia 69
```

## OS TRÊS FATOS QUE DISPENSAM REABRIR A DISCUSSÃO ARQUITETURAL

1. **Não precisa de brecha.** `contexto` é PEER; fatia → peer é a seta legal do modelo. SEIS
   fatias já consomem esse peer (traducao, traducaoKaraoke, traducaoCorrige, raspagemRevisao,
   raspagemCorrecao, correcaoLegendas). `revisaoLore` é a única que não.
2. **A guarda não proíbe.** `FronteiraTraducaoArchTest:830-852` bloqueia lista NOMINAL —
   `LlmPort/StatusLlm/LlmProperties/JsonHttpClient/RecordsLlm/LlmClientAdapter/GerenciadorContexto`.
   `ProvedorContexto`, o contrato público do peer, NÃO está nela.
3. **A regra de arquitetura já previa.** O princípio raiz é "duplicação consciente >
   acoplamento", com exceção explícita para *"invariantes onde divergir é bug, não evolução"*.
   Lore é isso: `Spearhead` é `Spearhead` nos dois lados.

## PLANO MESTRE (escopo FECHADO — não ampliar durante a execução)

- **✅ FASE 0 — FECHADA COM ARTEFATO em 2026-08-15, commit `78f41df0`** (sessão
  `a1057386`, portão rc=0). Congelou, por obra, o mapa efetivo dos dois lados.
- **FASE 1 — união.** As 69 da revisão + as 18 da tradução passam a viver no peer `contexto`.
  Zero conflito a arbitrar.
- **FASE 2 — revisão lê do peer.** `ProvedorPromptRevisaoLore.correcoesTerminologia()` deixa de
  ter implementação própria e delega por id. Os 68 arquivos perdem o mapa e MANTÊM o prompt.
- **FASE 3 — guarda.** Catraca: nenhum `ContextoRevisaoLore*` pode declarar
  `correcoesTerminologia()`. E liberar `ProvedorContexto` nominalmente na fronteira, mantendo
  `GerenciadorContexto` proibido — a FASE D-Lore quis separar o AGREGADOR, não o contrato.

**FORA DE ESCOPO, declarado:** o PROMPT continua duplicado de propósito (revisão ≠ tradução);
o gatilho de retentativa por termo perdido é frente própria e esta não depende dele.

**RISCO RESIDUAL:** as 69 entradas novas passam a agir na tradução. Cada uma só dispara com o
canônico presente no inglês em grafia exata, então o risco é baixo — mas "baixo" não é
"medido", e é para isso que a FASE 0 existe.

**ARMADILHA JÁ PAGA, não repetir:** `termosProtegidos()` NÃO protege — só remove o termo antes
das checagens de resíduo. Quem restaura é `correcoesTerminologia()` via `EnforcadorTermosLore`.

## ✅ FASE 0 — FECHADA COM ARTEFATO (`78f41df0`)

`BaselineTerminologiaLoreIT` + `src/test/resources/contexto/baseline-terminologia-lore.tsv`.

O BURACO QUE ELA FECHOU, e que ninguém tinha visto: o manifesto que já protegia as lores
(`/contexto/manifesto-lore.properties`, de `ProtecaoConteudoLoreTest`) hasheia **id + nome +
prompt + termosProtegidos** e **NÃO cobre `correcoesTerminologia()`** — exatamente o mapa que
a unificação vai mexer. O congelamento não existia.

```
snapshot vivo, dos provedores REAIS pelo CDI ... 4.104 entradas
   TRADUCAO 2.021 · REVISAO 2.083
suíte --rerun-tasks ........... 1.819 testes, 0 falhas, 25 pulados, 311 classes
```

CALIBRAÇÃO (4 doentes reprovando + 1 são aprovando — a contra-prova importa):

```
baseline AUSENTE ............. rc=1  NÃO VERIFICADO, que não é aprovação
entrada PERDIDA .............. rc=1
canônico MUDOU ............... rc=1
TRUNCADO sem mexer no total ... rc=1  <- o modo que aprovaria por cegueira
baseline REAL ................ rc=0
```

**É CATRACA DE UM LADO SÓ, e isso decide como a FASE 1 se prova:** o baseline é subconjunto
obrigatório do estado vivo. Acrescentar é livre (é o que a união faz); perder ou trocar
canônico reprova. **NÃO reescrever o `.tsv` na FASE 1** — ele continuar valendo depois da
união É a prova de que nada se perdeu. Regenerar é copiar
`build/tmp/baseline-terminologia-lore.gerado.tsv`, que a própria execução regrava; digitar
entrada à mão é como as duas cópias divergiram para começo de conversa.

O lado da revisão é lido pelo `GerenciadorPromptRevisaoLore` (o agregador de PRODUÇÃO), não
por coleta própria — a guarda mede o que o pipeline enxerga.

## ⚠️ O NÚMERO QUE MUDA A PREMISSA DA FASE 1 (`711f7f03`)

`MedicaoEfeitoDaUniaoDeLoreIT` aplicou o `EnforcadorTermosLore` REAL sobre o cache real, com
SOMENTE o delta (o que a revisão tem e a tradução não):

```
obras com delta ...... 17   (medidas 6 · SEM ACERVO 11)
entradas no delta .... 69   (45 delas em obra sem acervo — NÃO VERIFICADAS)
pares lidos .......... 28.437 em 506 caches
REESCREVERIA ......... 9 falas (0,032%), TODAS em DanMachi S5
                       8x Freya Familia · 1x Goddess of Beauty
86 (Eighty-Six) ...... 0 de 16.120
```

**O ganho da união NÃO é retroativo, e é ZERO justamente no 86** — a obra cujo
`Spearhead → Esquadroe de Ponta` originou a decisão. As 4 entradas que só a revisão do 86
conhece (`Canela`, `Jugernaut`, `Para RAID`, `Para Raid`) não ocorrem em nenhuma das 16.120
falas já traduzidas. O valor da união está nas traduções **FUTURAS**. Isso não invalida a
decisão do Paulo — invalida a frase "ganho imediato e medido" com que a FASE 1 foi escrita.

**DECISÃO DE PRODUTO ISOLADA, do Paulo (não é bloqueio técnico):** o delta traz EPÍTETO, não só
nome próprio. Medido na amostra:

```
"...tomada pela Deusa da Beleza e seu poder de charme"
             -> "...tomada pela Goddess of Beauty e seu poder de charme"
```

Nome próprio é inequívoco (`Sino Cranel → Bell Cranel` — o modelo traduziu "Bell" como "sino";
`Familia Freya → Freya Familia`; `Canela → Shin`). Epíteto em inglês no meio da frase em
português **atrapalha ler**, que é a régua do Paulo. Candidatos a ficar de fora:
`Deusa da Beleza`, `Deusa da Fertilidade`, `Anfitria/Anfitriã da Fertilidade`.

## ✅ DECISÃO DE PAULO (15/08) — epíteto FICA DE FORA da união

Só **nome próprio** entra na tradução. Epíteto continua no catálogo da REVISÃO, onde já está —
nada se perde, e o baseline da FASE 0 prova isso continuando verde. Fora, por essa régua:
`Deusa da Beleza`→Goddess of Beauty · `Deusa da Fertilidade`→Goddess of Fertility ·
`Anfitria/Anfitriã da Fertilidade`→Hostess of Fertility (os quatro que ele julgou) e
`Princesa Espadachim`→Sword Princess (**aplicação minha da régra dele, não da lista literal** —
é epíteto de personagem, mesma família; se ele discordar, entra).

## ⏳ FASE 1 — EM ANDAMENTO: 1 obra de 17 feita

**FEITO: `eight_six` — commit `206c2134`.** 4 entradas (`Canela`→Shin, `Jugernaut`→Juggernaut,
`Para RAID`/`Para Raid`→Para-RAID). Suíte 1.820/0. Baseline verde SEM reescrita.

**O DELTA COMPLETO, já extraído e conferido contra o harness (69 em obras nos DOIS catálogos):**
está em `src/test/resources/contexto/baseline-terminologia-lore.tsv` — as linhas `REVISAO` cuja
`(obra, forma-ruim)` não tem par `TRADUCAO`.

⚠️ **ARMADILHA DE INSTRUMENTO JÁ PAGA, não repetir:** ao extrair o delta do `.tsv` com tabela
hash do PowerShell, ela é **case-insensitive por padrão** e `Para RAID` engoliu `Para Raid` —
deu 68 onde a produção diz 69. Usar
`[System.Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)`.
O sinal que denunciou foi o total não bater com o harness; sem essa conferência, `Para Raid`
teria ficado de fora em silêncio.

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA (frente da lore)

**FASE 1, obra 2: `gundam_cca`.** `ContextoCharsCounterattack:105` já usa
`CorrecoesTerminologiaGundamUc.comExtras(...)` — acrescentar ao bloco de extras:
`Contra-ataque do Char`/`Contraataque do Char`→`Char's Counterattack`,
`Moldura Psiquica`/`Moldura Psíquica`→`Psycho-Frame`, `Novo Gundam`→`Nu Gundam`.

**Depois, as duas famílias Macross — e elas exigem um passo a mais, já diagnosticado:** as 7
obras chamam `CorrecoesTerminologiaMacross.mapa()` e essa classe **NÃO tem `comExtras`** (só
`mapa()`). Os deltas DIFEREM entre as famílias, então despejar tudo no mapa comum daria à
Frontier termos do Macross 7:

```
macross_7 · _encore · _filme · dynamite_7   Energia da Cancao/Canção -> Song Energy
                                            Protodemonios/Protodemônios/Protodevilns -> Protodeviln
macross_frontier · _filme1 · _filme2        Falha Fold/Falha de Fold -> Fold Fault · Vajras -> Vajra
```

Criar `comExtras` em `CorrecoesTerminologiaMacross` espelhando o padrão de
`CorrecoesTerminologiaDanMachi:56` e `CorrecoesTerminologiaGundamUc`, e ligar por obra.

**Por último, DanMachi (8 obras).** Todas chamam `CorrecoesTerminologiaDanMachi.mapa()`, que
**já tem `comExtras`** — usar por temporada, espelhando o escopo da revisão:
`Sino Cranel`→Bell Cranel (todas as 8) · `Lilisuka`/`Liriruca`→Liliruca Arde (geral, s2, s4, s5)
· `Haruhime Sanjono`/`Haruhime Sanjouono`→Haruhime Sanjouno (s2) · `Alienigenas`/`Alienígenas`→
Xenos, `Andares Profundos`→Deep Floors, `Jugernaut`→Juggernaut (s4) ·
`Familia Freya`/`Família Freya`→Freya Familia (s5) · `Ais Wallenstein`→Aiz Wallenstein (so).

**FORA DA UNIÃO, declarado:** `macross_dyrl` tem 11 entradas e existe **só** no catálogo da
revisão — não há provedor de tradução com esse id (o `manifesto-lore.properties` não o lista).
Não é entrada perdida; é obra sem contraparte, e criar contexto de tradução para ela é frente
própria.

**CRITÉRIO DE ENCERRAMENTO CORRIGIDO:** a FASE 1 sozinha **não** zera a divergência — depois
dela a tradução vira superconjunto e as obras continuam divergindo *no outro sentido* (entradas
que só a tradução tem). O zero só chega na FASE 2, quando a revisão passa a ler do peer. A
`ParidadeMapasTerminologiaTest` reprova nos dois sentidos de propósito: obra que parar de
divergir tem de sair de `DIVERGENCIAS_DECLARADAS` no mesmo commit. Critério de fechamento da fase: `MedicaoDivergenciaEntreCatalogosDeLoreIT` sai de
**17 obras divergentes para 0**, `BaselineTerminologiaLoreIT` segue verde SEM ser reescrito, e
`ParidadeMapasTerminologiaTest` acusa as 17 em `DIVERGENCIAS_DECLARADAS` como "PARARAM de
divergir" — que é a catraca registrando progresso, e cada id sai da lista no mesmo commit.

ATENÇÃO PARA A FASE 1: `ParidadeMapasTerminologiaTest` reprova **nos dois sentidos** de
propósito. Obra que deixa de divergir e continua na lista reprova. Não é regressão — é a
catraca pedindo que o progresso seja registrado.

---

## PRÓXIMA AÇÃO EXECUTÁVEL EXATA (Tradução Local, esta sessão)

⚠️ **CORRIGIDO EM 15/08 — esta ação JÁ FOI FEITA e o texto abaixo estava desatualizado.**
Dizia que o `DicionarioOrtograficoPort` existia e que "NINGUÉM a chama". Conferido no código,
não no documento: `CorretorOrtograficoLegenda` está injetado em
`traducao.ProcessarArquivoUseCase:88` **e** em `traducaoKaraoke.TraduzirKaraokeUseCase:136`,
além de `raspagemRevisao.RevisorPtOnlyService` e `qualidadeTraducao...DetectorNomeProprio
Traduzido`. Foi ligado pelo commit `62a48c3d` ("corretor por dicionario LIGADO ao pipeline de
traducao"). Quem ler o texto antigo refaz trabalho pronto.

Texto original, preservado como registro: *"plugar o `DicionarioOrtograficoPort` no pipeline da
Tradução Local — a porta existe, tem 5 testes e falha fechada em 3 estados, e NINGUÉM a chama.
Antes de ligar, aplicar a correção de desenho já medida: só o `pt_BR` reprova; `en_US`/`de_DE`
apenas ROTULAM o tipo de não-português (o `de_DE` aceitou `Resonância`, que é grafia errada).
Prejuízo que justifica: 306 erros reais medidos no Zeta/aya e 197 acentos faltando no
Unicorn."*

**O que dela CONTINUA aberto:** a correção de desenho dos dicionários secundários — só o
`pt_BR` deve reprovar, `en_US`/`de_DE` devem ROTULAR e não aprovar. Isso não foi verificado.

Obra do dia: **86**, em `C:\animes\86` — Part 1 com 11 mkv + 11 `.ass`, Part 2 com 12 mkv +
12 `.ass`, saída anterior apagada por Paulo de propósito. ATENÇÃO: o cache do 86 continua em
`cache/`, então uma tradução nova vem quase toda de lá em minutos — ótimo para legenda final,
inútil para medir modelo (aí é `permitirRetraducao`).

---

# ⏸ TAREFA ATIVA (2026-08-14) — marcador perdido descarta tradução boa no karaokê

TAREFA ORIGINAL: auditar o tradutor de karaokê e achar por que parte da legenda do 86
                 ficou sem tradução (ordem do Paulo, 14/08 ~20:40).
OBJETIVO FINAL:  causa raiz com artefato + correção no MECANISMO, não no sintoma.
CRITÉRIO DE ENCERRAMENTO: as 7 frases da letra do 86 chegam traduzidas ao `.ass`,
                 com caso-controle visto reprovando.
BRANCH / COMMIT BASE: main — `66f4a463`
SESSÃO DO COMPROVANTE: ac523f8b-b086-48b9-84bc-d7da735ffec0 (portão rc=0 em 14/08)

## FECHADO COM ARTEFATO — a auditoria

Manifesto `logs/traducao-karaoke/manifestos/kronos_traducao_karaoke_20260814_192920.json`
(run 19:03→19:29 sobre `C:\animes\86\86 Part 1\nova`, modelo `aya-expanse-8b`):

```
avisos na run ......................... 2.989
  Marcador perdido .................... 2.987   (99,9%)
  Alucinação .......................... 2
itensDetectados 7.907 / corrigidos 4.918  =>  2.989 sem tradução (37,8%)
originais distintos recusados ......... 326
  com \t( .............................. 322
  com tag no MIOLO (veto legítimo) ....... 4
  \t( só na BORDA => RECUPERÁVEIS ...... 322  (2.979 ocorrências, 99,7%)
```

As 7 frases da letra, traduzidas CERTO pelo aya e descartadas:
`A flower blooms only to be crushed` (650×) · `No matter how hard I wish, nothing ever
changes` (650×) · `Swept up and scattered by the wind` (325×) · `No matter what happens,
I'll never forget your voice` (325×) · `I can't see the future in your reflectionless
eyes` (325×) · `Don't worry, ease up, why don't you` (325×) · `We're all guilty in the
end` (325×).

CAUSA RAIZ: `core/texto/TextoSemTags.java:103` — `TAG_TRANSFORMACAO.matcher(texto).find()`
veta `\t(` no TEXTO INTEIRO, antes de perguntar ONDE a tag está. No 86 o `\t(` mora dentro
do bloco de BORDA (moldura estática do gradiente), então a linha cai no mascarador, vira
`[[TAG0]]`, o modelo não devolve o marcador e a tradução correta é recusada.
É a cicatriz do 08th MS Team (1.258 de 1.258) voltando por uma porta que a defesa não cobre.

AGRAVANTE: `TraduzirKaraokeUseCase:404` só memoiza SUCESSO — cada repetição de `\clip`
rechama o LLM e falha de novo. 2.979 chamadas para 322 textos (9,3×); a run levou 27 min.

## CORREÇÃO APLICADA (14/08, 21:16→21:32) — árvore suja, NADA commitado

`pode-compilar.ps1` respondeu `[0] PODE COMPILAR` (log parado há 2.672s) antes de tocar em `src/`.

- `core/texto/TextoSemTags.java` — o veto global de `\t(` SAIU. A constante
  `TAG_TRANSFORMACAO` foi removida (não é mais alcançável): em ASS toda tag vive dentro de
  `{...}`, então `\t(` fora da borda é bloco no miolo e já cai no teste `BLOCO_TAGS(miolo)`.
  Um segundo teste só para `\t(` seria guarda MORTA parecendo proteção.
  Javadoc registra a cicatriz e o contraste com `GradienteKaraoke` — que veta `\t(` COM razão,
  porque lá as tags são redistribuídas caractere a caractere.
- `TextoSemTagsTest.java` — o teste `vetaAnimacao` congelava o comportamento ERRADO para o caso
  de borda. Foi substituído (mudança de invariante declarada em voz alta, não silenciosa) por:
  `animacaoNaBordaEntraNoRecorte` (linha real do OP do 86, byte a byte do manifesto),
  `animacaoNaBordaRecompoeLiteral` e `vetaAnimacaoNoMeio`.
- A catraca `CatracaRegraDuplicadaEntreFatiasTest` NÃO muda: `\\\\t\\(` tem 7 caracteres e o
  limiar dela é 12 — a regex nunca entrou no total de 15.

### Artefatos

```
gradlew test --tests *TextoSemTagsTest* --rerun-tasks ......... rc=0
MUTAÇÃO (veto restaurado) .................... 12 tests, 2 failed  <- guarda VÊ o defeito
suíte completa --rerun-tasks ......... 1803 testes, 2 falhas, 25 skipped
  as 2 sao PRE-EXISTENTES: provado com `git stash` do fix -> 4 tests, 2 failed IGUAL.
  Vivem em CorretorNaoAlcancaRomajiDoKaraokeTest (arquivo NAO RASTREADO, ja na arvore
  no inicio da sessao). Codigo rastreado esta VERDE.
EFEITO, produção contra produção (jshell + build/classes/java/main, as 326 recusadas reais):
  total 326 | AGORA decompoem 322 | seguem vetadas 4 (todas tag no MIOLO, letra a letra)
```

## ✅ VERIFICADO NO ARQUIVO (Paulo rodou 21:32→21:34, `traducao_ptbr` → `-karaoke-ptbr`)

`kronos_traducao_karaoke_20260814_213421.json`:

```
                     run 19:29 (antes)   run 21:34 (depois)
avisos ............... 2.989                    6
  marcador perdido ... 2.987                    4   <- os 4 sao o cartaz letra a letra, previsto
  alucinacao ......... 2                        2
sem traducao ......... 2.989 / 7.907 = 37,8%    6 / 6.731 = 0,09%
traduzidas (LLM) ..... 4.918                6.491
```
Denominadores DIFEREM (25 arquivos em `nova` × 23 em `traducao_ptbr`) — comparar a TAXA, não o
absoluto. As 7 frases do OP/ED estão em português no `.ass`; as 26 ocorrências que sobraram em
inglês são `original\Ntraducao`, a camada preservada em cima, que é a promessa do modo.

## ✅ FEITO — dicionários no karaokê, pela regra de desacoplamento de Paulo (14/08)

Ordem dele: *"a camada resolve o problema DELA e não atravessa para o módulo comum"*. A rota que
eu havia proposto (mover para `core`) foi DESCARTADA — e duas afirmações minhas eram falsas:
`core` não é zona franca (é congelado por nome, 20 tipos homologados, `coreCongeladoPorTipo`
reprova o 21º), e `traducaoKaraoke` **já importa 4 tipos** de `qualidadeTraducao`, que é peer.

O que existe agora:
- `traducaoKaraoke.domain.AcentosLetraKaraoke` — lista PRÓPRIA, montada do que foi MEDIDO na
  saída do karaokê (`nao`). Delega a substituição a `CorretorAcentoPorDicionario.aplicar` do
  core, então a MECÂNICA não é duplicada e a **catraca seguiu em 15**, não 16 como eu previ.
- Guarda de honorífico: o caso-controle `Nao-chan` REPROVOU (o hífen é fronteira), e a resposta
  foi proteger com sentinela ``, não afrouxar a asserção.
- Telemetria da fatia: `acentosRepostos` no record, no manifesto e no resumo, com o terceiro
  estado (`dicionário AUSENTE — NÃO VERIFICADO`).
- `CorretorNaoAlcancaRomajiDoKaraokeTest` (as 2 falhas pré-existentes) agora exercita a corrente
  real da fatia e passa — o alvo passou a existir, a asserção não mudou.

MEDIÇÃO QUE SUSTENTA A DECISÃO: das 162 entradas de `NormalizadorAcentosComuns`, **4 são romaji
válido** contra o dicionário `ja_ROMAJI` — `ate`, `mae`, `nao`, `sao`. Reusar a lista traria de
volta o `mae`(前)→`mãe`, 100 ocorrências no Unicorn.

`gradlew test --rerun-tasks` = **1.808 testes, 0 falhas, 25 pulados, 304 classes**. Nada commitado.

PRÓXIMA AÇÃO: rodar a Tradução de Karaokê de novo sobre `traducao_ptbr` e conferir que
`acentosRepostos` > 0 no manifesto e que as 5 falas saem acentuadas no `.ass`. Só isso fecha o
efeito — hoje está provado em teste, não no arquivo.

## ✅ FEITO (14/08, madrugada) — telemetria de falha do karaokê, os 7 buracos da auditoria

`StatusExecucaoKaraoke` (COMPLETA/INTERROMPIDA/ABORTADA) · `FalhaArquivoKaraoke` (arquivo+motivo)
· `DesfechoKaraoke` com `EstadoDicionario` de TRÊS estados (DISPONIVEL/AUSENTE/NAO_CONSULTADO).
`registrarArtefatos` foi para um `finally` e roda SEMPRE — inclusive com resultado vazio e
inclusive abortando. Contexto/proveniência nulos passaram a ser tolerados (o aborto por LLM fora
acontece ANTES do congelamento). Manifesto perdido virou `log.error` + linha no console.

CALIBRADA (regra 9): com o `!resultados.isEmpty()` de volta, `abortoPorLlmForaDoArAindaRegistra
OManifesto` REPROVA (8 tests, 1 failed) e o contra-teste da execução normal segue verde.
`gradlew test --rerun-tasks` = **1.811 testes, 0 falhas, 25 pulados, 305 classes**. Nada commitado.

## 🔴 BLOQUEADO — comprovante do portão é de slot único

Três colisões em 20 min entre esta sessão (karaokê) e a outra (tradução local): cada carimbo
invalida o da outra e bloqueia quem perde. Correção desenhada e NÃO aplicada — o classificador
negou a edição do `checar-portao.ps1`. **Eu quebrei o script no meio (metade da edição passou,
metade foi negada) e RESTAUREI**; conferido: sem referência solta, `rc=0`. Lição: edição que
depende de outra vai num bloco só.

Patch pendente, 4 pontos: (1) `checar-portao.ps1:42` resolve `LEITURA-REGRA-<sessao>.md` com
sanitização `^[A-Za-z0-9._-]+$`; (2) `-Gerar` escreve o da sessão E o legado; (3)
`portao-leitura.ps1:72` prefere o da sessão e cai no legado; (4) a isenção da linha 78 passa a
casar `LEITURA-REGRA-*.md`. Invariante intacta: cada sessão continua obrigada a ter lido.

## 🔴 ACHADO NOVO (Paulo, 14/08 ~23h) — OP da Part 2 do 86 sai 100% sem tradução

MEDIDO no `.ass` de saída de `[DB]86 Part 2_-_01`:

```
Opening: 4.134 eventos  ->  3.580 de UMA LETRA · 554 vazios · ZERO frases de 2+ palavras
```

**Não é o classificador errando: não existe linha de frase para o LLM ler.** A Part 1 funcionou
porque o fansub entregou uma camada de letra inteira (as cópias de `\clip` do gradiente); a
Part 2 pinta letra a letra e não tem essa camada.

E o problema DIFÍCIL, que é o que Paulo nomeou: remontando os fragmentos, a linha sai
**bilíngue na mesma janela** — `W h a t d o y o u s` ao lado de `d o n` / `f u u` / `k e i`.
Classificar a linha por idioma não resolve, porque ela tem dois. Traduzir inteira corrompe o
romaji; preservar inteira deixa o inglês sem tradução. **A linha precisa ser PARTIDA.**

O ED do ep02 foi verificado e NÃO é defeito: as 7 frases em inglês estão traduzidas e os ~970
fragmentos são sílabas de romaji (`wai yo yu bi ga ki sa su ka hi ri`) — preservar é a regra.

Meia máquina já existe: o Karaokê Simples funde KFX em linha inteira, e está medido que achatar
antes de traduzir não muda a tradução ([[achatar-antes-de-traduzir-nao-muda-traducao]]). O que
não existe é o corte DENTRO da linha.

PRÓXIMA AÇÃO: Plano Mestre com alvo MEDIDO antes de qualquer código — quantas linhas do acervo
são bilíngues na mesma janela, em quais obras, e qual o discriminador do corte. Régua nasce de
medição, não de impressão.

### Diagnóstico que originou (mantido) `NormalizadorAcentosComuns` (lista nominal com `nao`/`voce`/`sao`/`vao`)
vive em `qualidadeTraducao.application` e é usada por `traducao.ProcessarArquivoUseCase` e
`raspagemRevisao.RevisorPtOnlyService` — **nunca pelo karaokê**. A fatia recebeu só a segunda
metade da corrente: `CorretorAcentoPorDicionario`, cujo piso é `\p{L}{4,}` — e `nao` tem 3
letras, então nunca é sequer consultado. O piso NÃO pode descer para 3: o Javadoc registra que
`mae` viraria `mãe` e `mae` é 前 em romaji (100 ocorrências no Unicorn).

Controle positivo (o discriminador): mesma medição nas duas saídas do MESMO 86 —
diálogo **0** ocorrências, karaokê **1.418**. A diferença é exatamente a lista nominal.

Unidade certa: **5 textos distintos**, 918 linhas do arquivo (o gradiente multiplica).
`Eu nao consigo ver...` 400x · `Nao se preocupe, relaxe, por que nao?` 350x · variante 150x.

Rota proposta: mover `NormalizadorAcentosComuns` para `core.texto` — precedente JÁ estabelecido
por `TextoSemTags` e `FronteiraTermoAss`, e o Javadoc de `TraduzirKaraokeUseCase:124-130` cita
esse precedente nominalmente para o corretor ortográfico. `core` é consumo livre por contrato,
então nenhuma aresta fatia→fatia nasce. Depois ligar em `TraduzirKaraokeUseCase:413`, no MESMO
ponto do corretor — que só vê o texto TRADUZIDO, nunca o romaji.

RISCO A GUARDAR ANTES DE LIGAR: `Nao` é nome próprio japonês comum. A lista nominal o
transformaria em `Não` dentro de uma fala legítima. Precisa de caso-controle antes de confiar.

## GAPS / RISCO DECLARADO

- **CORRIGIDO o meu próprio número**: eu havia declarado "3.620 eventos não-musicais em risco
  na tradução de diálogo". Errado duas vezes. (1) Medido por estilo: 29.183 `Opening` +
  **3.557 `Ending`** + 61 `Signs` + 2 `Default` — eu excluí `Opening` da conta e esqueci que
  `Ending` também é música. (2) O `falas-nao-traduzidas/*.jsonl` da run de diálogo mostra
  78.322 registros com **ZERO marcador perdido**: 78.173 `PRESERVADA_POR_REGRA` (veto
  intencional), 144 `TRADUCAO_IGUAL_AO_ORIGINAL`, 5 `PENDENTE`. Exposição real no diálogo:
  ~63 eventos, quase todos vetados como efeito. **O defeito era só do karaokê.**
- Residual: 8 ocorrências (0,27%) seguem no mascarador — o cartaz `Well done!!` pintado letra
  a letra. Veto legítimo, sem ação.
- Memoizar a FALHA no karaokê: **FORA DE ESCOPO, e por um motivo técnico**, não por preguiça —
  memorizar falha transitória impediria a retentativa que hoje às vezes acerta. Com a causa
  raiz fechada a tempestade de retentativas desaparece sozinha.
- `regra-java-quarkus-qute.md` (893 linhas) NÃO lida: a memória local registra decisão do
  Paulo de que ela não prende o KRONOS, e a alteração é lógica de texto puro em `core`,
  sem Quarkus nem Qute. Lacuna conhecida, declarada.
- 🔴 ACHADO NOVO, fora do escopo desta correção: `CorretorNaoAlcancaRomajiDoKaraokeTest`
  reprova em `CASO SÃO: a letra em português recebe acento` — o corretor NÃO acentuou
  `nao → não` com o dicionário disponível. Pré-existente, mas é defeito de verdade e merece
  sessão própria.

## NÃO REPETIR

- Não concluir por `Get-ChildItem`/`-Filter` com `[DB]` no nome — os colchetes são curinga;
  usar `-LiteralPath`.
- A pasta `nova`/`nova-karaoke-ptbr` do manifesto já não existe; a prova viva é o
  manifesto + `cache/karaoke/*.json`, não o `.ass` de saída.

---

# TAREFA ANTERIOR (2026-08-13) — modelo titular e dicionários no karaokê

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

⚠️ **DESATUALIZADO — corrigido em 15/08, ocorrência irmã da de cima.** Era verdade em 13/08 e
deixou de ser em `62a48c3d`: `CorretorOrtograficoLegenda` está plugado em
`ProcessarArquivoUseCase:88`, `TraduzirKaraokeUseCase:136`, `RevisorPtOnlyService` e
`DetectorNomeProprioTraduzido`. Texto original: *"A porta existe, está testada e NÃO está
plugada no pipeline. Ninguém chama `DicionarioOrtograficoPort` ainda."*

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


## VARREDURA DOS 22 ARQUIVOS (13/08) — o padrao, e a correcao de um diagnostico meu

    ep 01-12   ED        15 linhas    3 traduzidas   12 preservadas
               ED - EN   15          15              0
               OPL2      69           0             69
               Sign       1           1              0
    ep 13-22   ED2       23          22              1
               OPL2      69           0             69
               Sign       1           1              0

CORRIJO O QUE REGISTREI EM 0460bdbe: as 3 linhas do ED NAO sao romaji vazando. Elas estao
em INGLES na entrada:

    0:22:54.94  "Take off my sought idol"        -> "Tire o meu idolo conquistado."
    0:22:57.57  "Then I can breathe in so deep"  -> "Entao eu posso respirar tao fundo."
    0:23:09.95  "Take off my dress and crown..." -> "Tire minha roupa e coroa..."

O ED do Unicorn e MISTO: 12 linhas em romaji + 3 em ingles. O tradutor acertou linha a
linha DENTRO do mesmo estilo — preservou as 12 japonesas e traduziu as 3 inglesas. Isso e
comportamento CORRETO, e melhor do que eu supus.

A duplicata da tela aos 23:09 tem outra causa: ED e ED - EN cantam O MESMO TRECHO em
ingles, as duas foram traduzidas certo, e as duas aparecem juntas dizendo a mesma frase.
Nao e sobrescrita — e empilhamento de duas camadas legitimamente iguais.

DEFEITOS REAIS QUE SOBRAM, todos confirmados nos 22:
  1. OPL2: 0 de 69 traduzidas, em TODOS os episodios. Nunca traduziu.
  2. ED2 (eps 13-22): 22 de 23 traduzidas e a original NAO foi preservada — camada unica
     substituida em vez de duplicada.
  3. Duplicata visual quando ED e ED - EN dizem a mesma coisa (eps 01-12).
  4. Fonte Arial com o tamanho numerico da fonte original.

ESTILOS FALTANDO, conferir se e da obra ou da extracao: E09 sem ED/ED - EN; E18 e E22 sem
OPL2.


## CORRIGIDO 13/08 — OPL2: a marca de efeito nao basta, o discriminador e o TEXTO

ClassificadorLetraKaraokeService devolvia EFEITO_KFX para qualquer linha com tag de efeito.
No Unicorn TODAS as 155 linhas do OPL2 tem tag — inclusive as 17 que sao a letra inteira.
Resultado medido: 0 de 69 traduzidas em TODOS os 22 episodios.

Agora: linha com tag de efeito so e KFX se NAO for frase completa. Fragmento e UMA palavra
visivel ("Do", "you", "feel"); letra e frase (2+). Medido no E01: 17 frases, 138 fragmentos.
Palavra unica segue preservada, entao o vies de preservar continua de pe.

Suite 1723 / 0 falhas.

## IDEIA DO PAULO (13/08) — TAG DE GUARDA no campo Effect, para auditar sem adivinhar

"e se aplicassemos uma tag de guarda para esses casos, introduzindo ela propositalmente,
sem bugar o arquivo"

O formato ASS ja tem o lugar: o campo Effect do Dialogue. O renderizador IGNORA qualquer
valor que nao seja Banner / Scroll up / Scroll down / Karaoke. Escrever ali nao muda um
pixel.

    Dialogue: 0,0:22:54,OPL2,,0,0,0,KRONOS:LETRA,{\t(..)}And Im calling...
    Dialogue: 0,0:22:55,OPL2,,0,0,0,KRONOS:KFX,{\t(..)}Do

GANHO: a decisao do classificador fica GRAVADA no arquivo. Auditar vira contagem em vez de
deducao — passei 13/08 inteiro inferindo por regex se uma linha era letra ou fragmento.

DOIS CUIDADOS (boa-fe):
  1. "Karaoke" e palavra RESERVADA nesse campo — prefixo proprio (KRONOS:) evita mudar o
     comportamento de renderizacao da linha.
  2. Reprocessar nao pode ACUMULAR: sobrescrever a marca, nunca concatenar.

NAO IMPLEMENTADO: mexer na escrita do .ass e o caminho por onde se perde legenda.


## TESTE EM ARQUIVO REAL 13/08 18:55 — E01 + E22, com os dois consertos

Traducao: 2 concluidos, 0 pendencias, 5m34s. Karaoke: 2 arquivos.

OPL2 do E01 — era 0 de 69 traduzidas em TODOS os episodios. Agora:
    128 fragmentos intactos        (Do, you, feel, lone)  -> preservacao mantida
      6 frases EMPILHADAS          original\Ntraducao
     11 frases traduzidas sem empilhar

    "Do you feel alone\NVoce se sente sozinho?"
    "Can you hear me now\NVoce pode me ouvir agora?"
    "Let light shine through\NDeixe a luz brilhar atraves."

ED do E01 — romaji PRESERVADO, sem traducao colada (tem irma ED - EN, entao nao empilha).
ED - EN    — traduzido sem empilhar, correto pelo mesmo motivo.

DUAS COISAS ABERTAS, medidas mas NAO explicadas:

1. As 11 frases do OPL2 que traduziram SEM empilhar. O classificador as viu com camada irma
   no mesmo instante. Pode estar certo (OPL2 simultaneo ao ED) ou ser o mesmo casamento
   acidental que mascarou o defeito antes. NAO afirmar sem medir.

2. O E22 NAO TEM ED2 — so OPL2 e Sign. O estilo de camada unica esta nos episodios 13 a 21.
   Entao o conserto do ED2 tem teste unitario mas NAO foi exercitado em arquivo real. Para
   fechar, o episodio certo e o E13 ou o E19, nao o E22.


### FECHADO EM ARQUIVO REAL — E13/ED2, o caso de CAMADA UNICA

    antes:  22 de 23 linhas com a original APAGADA
    agora:  22 de 23 EMPILHADAS (original\Ntraducao), 1 sem empilhar (tem irma no instante)

    "Behind your mask\NPor tras-com-acento da sua mascara"
    "I don't wanna be like you\NEu nao-com-til quero ser como voce-com-acento."
    "The feud between us escalates deeper\NA rivalidade entre nos se intensifica ainda mais."

O numero que era o dano virou o numero do acerto.

E o corretor ortografico aparece na mesma linha: "tras" saiu ACENTUADO (era "tras" na
saida de 17:53), e "mascara" continua sem acento — o teto que o teste ja fixava, porque
"mascara" E palavra em portugues (verbo mascarar) e o dicionario nao sabe que ali era o
substantivo.

OS TRES DEFEITOS DO KARAOKE, agora provados em disco:
  OPL2 0/69 em todos      -> 6 frases empilhadas, 128 fragmentos intactos (E01)
  ED2 camada unica        -> 22 de 23 empilhadas (E13)
  corretor nao alcancava  -> "tras" acentuado na saida

