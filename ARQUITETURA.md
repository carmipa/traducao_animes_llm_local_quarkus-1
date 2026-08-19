# Notas de arquitetura, qualidade e verificação — KRONOS CORE

Documento técnico da plataforma: arquitetura, modelo de execução, invariante de cache,
blindagem contra o modelo de linguagem e método de verificação. Escrito para engenharia —
cada número aqui foi lido do código ou do acervo em disco, e onde a medição não existe está
escrito que não existe.

**Data da leitura: 06/08/2026 · números remedidos em 19/08/2026.**

---

## 1. O problema que define a arquitetura

Traduzir legenda de anime com LLM local não é um problema de escala. É um problema de
**confiança num componente que erra em silêncio**.

O modelo devolve texto plausível quando falha. Ele apaga uma tag de posicionamento e a
legenda cai no meio da tela; ele responde *"Claro! Aqui está a tradução:"* e essa frase vai
para o arquivo; ele traduz `Fa` — nome de personagem — como `Fogo`; ele deixa metade da fala
em inglês porque a outra metade já estava traduzida na fonte. Nenhuma dessas falhas gera
exceção, nenhuma aparece em log de erro, e **todas só se manifestam quando alguém está
assistindo** — momento em que o defeito já custou horas de GPU e está distribuído por 23
episódios.

Isso decide a arquitetura antes de qualquer escolha de linguagem. Um sistema cujo componente
central mente precisa de **verificação mecânica em cada junta**, e precisa que essa
verificação seja mais barata que a re-execução. Se conferir custar o mesmo que traduzir de
novo, ninguém confere.

A segunda restrição vem do inverso da escala: é **um usuário, uma máquina, uma GPU**. Não
existe inquilino, não existe pico de carga, não existe custo marginal por cliente. O recurso
escasso é tempo de GPU e atenção humana. Uma tradução ruim não derruba o sistema — ela é
aceita, salva em cache, e volta a aparecer em toda execução seguinte, porque cache é
justamente a memória do erro.

Daí as duas decisões estruturantes:

**Toda etapa é independente e re-executável.** Não há pipeline monolítico que precisa rodar
do começo. Errou a revisão de lore? Roda só ela. O acervo é o estado; as etapas são funções
sobre ele.

**Cache carrega proveniência, não só resultado.** Guardar "esta fala virou aquela" é inútil
se não se souber *sob quais condições*. Mudou o prompt, o modelo, a lore ou o idioma — a
entrada deixa de valer. É o equivalente aqui ao isolamento de inquilino de um sistema
multiempresa: o invariante que, se furar, contamina tudo em silêncio.

**Escala atual do código (medida em 19/08/2026):** 63.049 linhas de Java em 471 classes,
20 fatias verticais, 5 peers, 21 controllers, 69 obras de lore no `lore.yaml` (68 na lista da
UI), 2.004 testes automatizados dos quais 136 são guardas de arquitetura. Acervo em disco: 34
pastas de obra no cache, 553 arquivos de cache, 122.307 entradas, 90 MB.

> A contagem anterior desta linha (06/08: 577 classes, 72 lores, 1.440 testes, 62 guardas)
> ficou treze dias sem revisão e cada número dela estava errado hoje. A lore, em particular,
> deixou de ser classe Java e virou dado (`lore.yaml`), então "contar classes" nem mede mais a
> mesma coisa — por isso a contagem passou a ser perguntada à produção (`/api/contextos`).

---

## 2. Stack

**Núcleo** — Java 25 com Quarkus 3.37, usando as extensões de compatibilidade Spring
(`quarkus-spring-di`, `quarkus-spring-web`, `quarkus-spring-boot-properties`). A escolha do
Quarkus é por tempo de partida e recarga a quente em desenvolvimento: o ciclo de trabalho aqui
é *mexer numa regra, rodar sobre 60 mil falas, olhar o resultado*, e três segundos de partida
contra trinta muda o número de iterações por hora.

**Persistência — nenhum banco.** O estado é **JSON por arquivo de legenda**, na pasta `cache`.
A decisão é deliberada e vale registrar o motivo: o dado aqui é naturalmente particionado por
episódio, nunca há consulta transversal, nunca há escrita concorrente sobre o mesmo arquivo, e
o arquivo precisa ser **legível e editável à mão** durante uma auditoria. Um Postgres traria
migração, subida de serviço e um passo de exportação toda vez que alguém quisesse olhar um
caso — sem devolver nada que o problema exija.

**Modelo de linguagem** — LM Studio local em `127.0.0.1:1234`, via contrato HTTP compatível
com a API da OpenAI. O `model` na configuração é a string literal `"current"`: o LM Studio
resolve para o modelo carregado na UI. Cravar o id exato ali faz o sistema quebrar toda vez
que se troca de modelo, que é uma operação frequente neste projeto.

**Interface** — SPA em HTML/CSS/JS puro, **sem framework**: 9 arquivos HTML, 24 de JavaScript,
20 de CSS. Não é nostalgia — é que a superfície é um painel de operação com formulário e
console, e um framework traria build, bundler e uma camada de estado para um problema que
`fetch` mais `EventSource` já resolvem. Logs em tempo real por **SSE**.

**Ferramentas externas** — MKVToolNix (`mkvmerge`, `mkvextract`) para faixas e remux, FFmpeg
(`ffprobe`) para auditoria de mídia. Ambas invocadas como processo, nunca via biblioteca: são
programas maduros, o contrato de linha de comando é estável, e a alternativa seria manter um
parser de container próprio.

**Rede externa opcional** — Jikan/MAL e TMDB para metadados de exibição, e raspagem da API
pública do Google Tradutor para o fluxo de correção. Nenhum dos dois é caminho crítico: o
pipeline inteiro funciona offline.

---

## 3. Infraestrutura

**Não há infraestrutura.** Isso é uma decisão, não uma lacuna.

A aplicação sobe com `gradlew quarkusDev` e escuta em `127.0.0.1:8099` — **loopback, nunca
`0.0.0.0`**. Não há container, não há borda TLS, não há orquestrador, não há deploy. O sistema
lê e escreve arquivos no disco local do usuário e conversa com um LLM que roda na mesma
máquina.

A consequência de segurança é agradável e vale explicitar: **a superfície de rede é zero**.
Não existe autenticação porque não existe acesso remoto; não existe sessão porque não existe
segundo usuário. Adicionar login aqui seria cerimônia — protegeria o usuário dele mesmo.

A consequência operacional é menos agradável e está declarada em `§9`: sem processo
supervisionado, uma atualização do Windows que reinicia a máquina de madrugada mata uma
tradução de seis horas. Aconteceu em 06/08/2026, às 01:28.

---

## 4. Modelo de execução

### 4.1 Fila única, e por quê

Operações pesadas — tradução, extração, remux, revisão — passam por uma **fila de execução
serializada** (`FilaExecucaoPipeline`). Não é limitação de arquitetura: é que todas competem
pelo mesmo recurso escasso, que é a GPU do LM Studio, e duas traduções simultâneas não
terminam em metade do tempo — terminam no mesmo tempo somado, com o dobro de chance de
estourar o timeout de leitura de 180 s.

A exceção é deliberada e documentada: `/api/novo-karaoke/simular` roda **fora** da fila, no
`ForkJoinPool`, porque é read-only e existe para dar resposta imediata na tela. Foi auditada
em 2026-07-31 e confirmada como correta — está registrado para não ser re-sinalizada como bug.

### 4.2 Lote de tamanho 1, e o que isso custou descobrir

```yaml
tradutor:
  tamanho-lote: 1
```

A configuração óbvia seria mandar 20 falas por requisição e economizar chamadas. Não funciona:
o modelo local **mistura as linhas**. Devolve 20 traduções para 20 entradas, contagem
perfeita, com o conteúdo da linha 7 aparecendo na 9. A validação por contagem passa e o
defeito chega à tela.

Com lote 1 o problema desaparece por construção, ao custo de uma chamada HTTP por fala. É a
troca certa quando o recurso escasso é a **atenção humana para revisar**, não o tempo de
máquina.

### 4.3 A frase partida, e a medição que a autorizou

Lote 1 cria um defeito novo: uma frase quebrada entre dois eventos consecutivos do `.ass`
("...realizar uma cerimônia" / "at the Prime Minister's residence") nunca é vista inteira pelo
modelo, que trata a segunda metade como frase nova — `at` vira `Até`, `to usher` vira `Para`.

A correção agrupa a corrente na mesma chamada. Medido no Unicorn RE:0096, 22 episódios,
2026-07-29, sobre 583 correntes e 706 ligações:

| | antes | depois |
|---|---|---|
| maiúscula indevida na continuação | 93,8% | ~13% |
| conector virado em abertura | 29,7% | ~1% |
| chamadas ao LLM | 1.289 | 985 (−23,6%) |

Corrente **longa** sai melhor que curta — K=4 deu 3,5% de maiúscula indevida contra 15,8% do
K=2 —, então não há teto de agrupamento.

E o que interessa mais: **a troca não é gratuita, e isso está escrito na configuração**. Ligar
o agrupamento troca ~30% de conector errado por ~4% de deslocamento de conteúdo — linha com o
texto da vizinha, contagem certa, que a validação por contagem **não pega**. Quem intercepta é
a `GuardaCorrenteTraduzida`, devolvendo a corrente ao fluxo individual quando desconfia.

### 4.4 Console em tempo real, e o batimento que carrega identidade

Logs vão ao navegador por SSE. Duas armadilhas já pagas:

**Conexão zumbi.** Sem tráfego, a conexão apodrece em timeout de NAT ou do servidor; o
navegador segue achando que está conectado e o console congela. Resolvido com batimento a cada
15 s mais watchdog de 40 s no cliente.

**Verde eterno depois do reinício.** Até 05/08/2026 o batimento carregava
`System.currentTimeMillis()` — que prova que *alguém* está vivo agora, e não diz **qual
execução** está do outro lado. Reiniciada a aplicação, o navegador reconectava e recebia um
batimento indistinguível do anterior: mantinha no DOM o console da execução morta. Era por
isso que só `Ctrl+F5` limpava a tela.

Hoje o batimento carrega uma **identidade sorteada por execução**, constante do início ao fim
dela, e enviada também no registro da conexão — para o reconecte automático perceber a troca
sem esperar até 15 s. A frescura do sinal continua vindo da **chegada** do evento; o conteúdo
existe para responder "é a mesma execução?". Um teste mutação-verificado impede o relógio de
voltar.

---

## 5. Capacidade — o que foi medido, e o que não foi

### 5.1 O acervo

| | |
|---|---|
| Obras com cache | 16 |
| Arquivos de cache | 219 |
| Falas traduzidas | **68.295** |
| Tamanho em disco | 11,0 MB |
| Falas com quebra `\N` | 16.023 (23,7%) |

### 5.2 O que NÃO tenho

**Não existe medição de vazão de tradução neste projeto.** Tentei extraí-la dos carimbos de
tempo do cache em 06/08/2026 e o número saiu errado: os arquivos foram reescritos em bloco
pelas correções de terminologia, então o carimbo é do conserto, não da tradução. Uma amostra
válida exigiria instrumentar o pipeline com início e fim por episódio, o que não está feito.

Registro isso porque a alternativa — estimar e apresentar como medido — é exatamente o tipo de
número que contamina um documento inteiro. Quando alguém perguntar "quanto tempo leva uma
temporada", a resposta honesta hoje é **não sei**.

### 5.3 O que está medido: qualidade da saída

Aqui há número, e ele é o que importa para o produto. Legendas PT em disco, contando só
eventos de diálogo (karaokê e letreiro excluídos):

| Obra | Episódios | Falas | Vazias | Com inglês | % |
|---|---:|---:|---:|---:|---:|
| 86 (simplificada) | 23 | 7.022 | 0 | 2 | **0,03%** |
| Gundam ZZ | 49 | 17.437 | 2 | 14 | 0,08% |
| Unicorn | 22 | 5.665 | 0 | 5 | 0,09% |
| Zeta Gundam | 50 | 16.870 | 6 | 23 | 0,14% |
| Guilty Crown | 23 | 5.759 | 1 | 13 | 0,23% |
| 08th MS Team | 13 | 3.842 | 2 | 126 | 3,28% |

Duas leituras que o número bruto não dá e a inspeção manual deu:

- Das 14 do ZZ, **cinco são a mesma cartela** "Next time on Gundam ZZ" repetida — é **um**
  defeito, não cinco. E uma é falso positivo do meu medidor: `Will Astonaige` é nome próprio,
  e a régua leu `Will` como verbo modal.
- Os 3,28% do 08th MS Team incluem *code-switching* legítimo — a obra tem falas
  deliberadamente em inglês. Parte daquilo é correto e precisa de olho humano, não de
  máquina.

Ou seja: percentual bruto de "contém palavra inglesa" é **triagem**, não veredito.

---

## 6. O invariante central: proveniência do cache

A regra: **cache não guarda tradução, guarda tradução sob condições.** Mudou a condição, a
entrada deixa de valer — e o sistema retraduz em vez de servir o resultado antigo.

`ProvenienciaCache.mesmaProveniencia()` compara **seis campos**: versão do schema, id do
contexto de lore, hash do prompt de sistema, modelo do LLM, idioma de origem e idioma de
destino. O hash do prompt é SHA-256 do texto inteiro: mexeu uma vírgula na instrução, todo o
acervo daquela obra sabe que envelheceu.

**`cachetraducao` é dono único.** Nenhuma outra fatia escreve cache paralelo, e isso é
congelado por `FronteiraCacheTraducaoArchTest`. A tentação de "guardar um cachezinho aqui" é
como a arquitetura morre: dois donos, duas políticas de invalidação, e a mais frouxa vence.

### 6.1 A guarda de obra × contexto

Existe uma pergunta anterior ao cache: *o arquivo que está prestes a ser traduzido pertence à
lore selecionada na tela?* Se não pertencer, o resultado é pior do que um erro — é um cache
inteiro carimbado com a lore errada, que parece válido e vai ser reusado.

Isso não é hipótese: **15 caches de Gundam 0083 foram gravados com `contextoId =
guilty_crown`**. A guarda nasceu disso.

A identidade da obra é **derivada**, não declarada: sai do id e do nome de exibição do
contexto, normalizados, mais apelidos opcionais de pasta. Toda obra do catálogo ganha
identidade sem tocar em nenhuma lore. E a comparação é **determinística, nunca difusa** — um
nome canônico só casa quando aparece no nome da pasta como sequência de palavras inteiras.
Sem `contains` permissivo, sem distância de edição: `Crown` jamais identifica Guilty Crown, e
`86` jamais casa dentro de `(1986)`.

Número, ano e sigla **são identidade, não ruído**: `0083`, `0080`, `86`, `ZZ`, `F91`, `NT`
distinguem obras inteiras. Por isso a normalização preserva dígitos e não existe remoção
genérica de "ruído de release" — apagar `0083` por parecer um número apagaria a própria obra.

Os desfechos são três, e o do meio é o que mais importa: divergência **bloqueia**, ambiguidade
**bloqueia**, indeterminação **avisa e segue**. Indeterminação é o caso da pasta que não prova
nada (`Season 01`), e bloquear ali só ensinaria o operador a desligar a guarda.

### 6.2 Uma fragilidade declarada

A chave do cache é o **nome da pasta avó** do arquivo. Para uma estrutura
`<obra>/Season 01/legendas_eng/ep.ass`, isso resolve para `Season 01` — genérico.

Já materializou: existe `cache/Season 1` com 22 arquivos, que é o Unicorn. Duas obras
organizadas assim, com a mesma grafia de pasta, misturariam cache — e cache misturado não se
desfaz. Está em `§9`.

---

## 7. Blindagem contra o modelo

Num sistema multiempresa, o adversário do modelo de ameaças é quem está do outro lado da
rede. Aqui **o adversário é o próprio componente central**, e ele não é malicioso — é
descuidado, confiante e incansável.

### 7.1 O que nunca chega ao LLM

`SeletorEventosTraduziveis` decide elegibilidade **antes** da chamada, e bloqueia karaokê cru,
desenho vetorial (`\p1`) e letreiro animado quadro a quadro. Traduzir sílaba de karaokê
produz lixo caro.

**Prova medida:** nas 68.295 falas que já foram ao LLM, **zero** têm tag de karaokê (`\k`) no
original. O número não depende de nomenclatura de estilo — `\k` é a tag, esteja o estilo
chamado como estiver. A blindagem funciona.

Letreiro animado é reconhecido pela **conjunção** de três sinais: tag de efeito pesada, pouco
texto visível e repetição do mesmo texto ≥ 5 vezes no arquivo. A repetição é o sinal decisivo
— sem ela, uma fala isolada com efeito visual seria descartada por engano.

### 7.2 O que é escondido do LLM

As tags ASS são **mascaradas** antes do envio (`[[TAG0]]`, `[[TAG1]]`…) e restauradas depois.
O modelo não vê `{\an8\pos(640,50)\fad(200,200)}` e portanto não tem como "melhorar" aquilo.
Quando ele corrompe o marcador mesmo assim, a fala volta ao texto original — degradar para o
original é sempre preferível a degradar para saída corrompida.

### 7.3 O que é conferido na volta

- **Resíduo em inglês** — lista de *function words* sem homógrafo em português, mais contrações
  inequívocas (`it's`, `don't`, `gonna`). A lista cresceu por medição: `only` entrou depois que
  33 falas do acervo apareceram com ele cru, sempre com o mesmo gatilho — o `Just` enfático do
  inglês abrindo oração.
- **Vazamento para francês** — o modelo local já "corrigiu" `WITH SHINING BLUE FIRE` para
  `AURA BLEU BRILLANTE`.
- **Recusa e meta-resposta** — `PADRAO_RECUSA_META` impede que *"Desculpe, não posso traduzir"*
  vire legenda.
- **Fala idêntica ao original** — sinal de que o modelo devolveu a entrada.
- **Corrente deslocada** — `GuardaCorrenteTraduzida` compara o que foi enviado com o que
  voltou, e devolve ao fluxo individual quando o conteúdo escorregou de linha.

### 7.4 Fronteira de termo, e por que ela é mecânica de formato

`\N` — a quebra de linha do ASS — ocupa **dois caracteres**, e o `N` é letra para `\p{L}`. Um
lookbehind ingênuo `(?<![\p{L}\p{N}])` conclui que o termo colado na quebra é sufixo de outra
palavra, e **o termo fica invisível**.

Isso não é sutileza acadêmica. Medido no acervo: 24,6% das falas têm a quebra; das 12 formas
sem acento do normalizador, **0 sobreviveram soltas e 11 sobreviveram coladas na quebra** — a
forma solta sempre foi corrigida, a colada nunca foi.

A mecânica mora em `core.texto.FronteiraTermoAss`, e **não** é duplicada por fatia. O
princípio geral do projeto é *duplicação consciente > acoplamento*, mas ele vale para **regra
de negócio**, que tem versão por fatia. Mecânica de formato não tem versão: não existe "quebra
de linha segundo o corretor". `core` é consumo livre por contrato, então centralizar ali não
cria aresta entre fatias.

### 7.5 O único segredo do sistema

Há exatamente uma credencial: a chave da API do TMDB, em `application-local.yml`, fora do
controle de versão. É a única razão pela qual esse arquivo não deve ser aberto em sessão
compartilhada.

Uma armadilha de configuração vale registro: `application.yml` tem `config_ordinal` 255 e
**vencia** o `application-local.yml` (250), então flag definida no local nunca valia. Corrigido
com `config_ordinal: 260`. Configuração do SmallRye vincula no **boot** — reiniciar significa
matar o processo, não recarregar a página.

---

## 8. Como se trabalha aqui

### 8.1 Toda regra dura veio de um prejuízo

**Log registra tentativa, não desfecho.** Auditar uma funcionalidade pelo `tradutor.log` deu
três resultados errados no mesmo dia. O log diz que a fala foi enviada; não diz o que voltou
nem o que foi gravado. A verdade é o `.ass` em disco, casado pelo **instante** do evento — o
texto muda ao traduzir, o instante não.

**Quantidade igual não é identidade igual.** Contagem de eventos batendo entre original e
traduzido não prova nada sobre o conteúdo. Foi assim que seis filmes do Break Blade passaram
por "traduzidos com sucesso" tendo 65 eventos quando o diálogo real tem 646.

**Resultado zero é hipótese, não conclusão.** Busca vazia significa "não achei com este
instrumento". Aconteceu quatro vezes em uma única sessão de trabalho: uma regex devolveu 0
nomes partidos onde havia 435; um agrupamento devolveu 0 divergências onde havia 139 grupos;
uma comparação sensível a caixa devolveu 0 colisões onde havia 1.

**Nunca descartar o motivo de uma rejeição.** Um motivo calculado e jogado fora cegou 996
pendências — o sistema sabia por que tinha recusado e não contou a ninguém.

**Estado calculado contra o relógio, nunca anotado na execução.** Indicador que depende do
processo vivo para se atualizar fica verde eterno justamente quando o processo morre. Pago
duas vezes aqui: no console SSE zumbi e no batimento sem identidade de execução.

**Termo "protegido" não protege.** O `termosProtegidos()` apenas isenta o termo da checagem de
resíduo — quem restaura é `correcoesTerminologia()`, e ela é **condicional**: só dispara
quando o original em inglês contém o canônico. Confundir os dois levou a acreditar que nomes
estavam blindados quando não estavam.

**Ordem obrigatória: traduzir → medir → mapear.** Adivinhar quais formas erradas o modelo vai
inventar não funciona. `Undertaker` virou `Fúnebre` e `Carrasco`, formas que ninguém previu, e
que só entraram no mapa depois de aparecerem no acervo.

**IA com permissão de commit baixa a catraca.** Um agente com escrita fez o guarda-corpo
*concordar*: baixou o número congelado de 14 para 11 quando a própria refatoração cegou o
scanner, alegou "confirmado nos logs" sem log nenhum, e entregou teste verde sobre o vácuo.
Conferir o número de catraca em todo commit de IA, e exigir a mutação.

### 8.2 Guardas executáveis

Dos 2.004 testes, **136 não verificam comportamento** — verificam que um padrão perigoso não
voltou. Leem o código-fonte, a estrutura de pacotes ou o HTML, e reprovam o build ao encontrar
a forma do bug. São **35 guardas: 10 fronteiras ArchUnit e 25 catracas**.

| Guarda | Impede |
|---|---|
| `FronteiraTraducaoArchTest` (15) | a fatia de tradução ganhar aresta para outra fatia funcional |
| `FronteiraContextoArchTest` (8) | o peer de lore (69 obras) depender de fatia funcional |
| `FronteiraCacheTraducaoArchTest` (3) | outra fatia escrever cache paralelo |
| `FronteiraLlmArchTest` (4) | o contrato do modelo vazar detalhe de provedor |
| `CatracaFronteiraQuebraAssTest` (2) | fronteira de termo esquecer que `\N` são dois caracteres |
| `CatracaRegraDuplicadaEntreFatiasTest` (2) | duplicação silenciosa — duplicar pode, esconder não |
| `CatracaAgregadorasForaDoCdiTest` (4) | alguém "consertar" uma ausência deliberada de `@Component` |
| `CatracaSlotsReservadosLoreTest` (3) | a dívida de obra sem lore ficar invisível |
| `CatracaTelaDestrutivaNasceEmDryRunTest` (2) | tela que reescreve o acervo abrir gravando |
| `CatracaFerramentaDeAcervoVetaMusicaTest` (2) | ferramenta que varre o acervo reescrever música |
| `CatracaPaginaDeDocumentacaoAbreTest` (3) | documento existir em `docs/` e não abrir na tela |

*(Inventário completo em [docs/catracas-e-fronteiras.md](docs/catracas-e-fronteiras.md).)*

Por que isso importa **neste** projeto em particular: o repositório é trabalhado por várias
IAs — Claude, Gemini, GPT, Codex, Cursor — além do humano. Um arquivo de instruções pode ser
ignorado por qualquer agente que abra o projeto amanhã. Um teste vermelho, não. A guarda
versionada junto do código é a única forma conhecida de uma regra sobreviver à troca de
modelo.

**Duas regras de operação da guarda:**

*Guarda é calibrada contra caso-controle.* Precisa ter sido vista **reprovando** um caso doente
montado à mão. Caso real: a catraca da quebra `\N` procurava **uma** grafia e por isso
afirmava cobertura que não tinha. Passou a cobrir nove, cada uma verificada contra
caso-controle — 9 de 9 —, e na primeira execução real encontrou um ponto cego que estava verde
havia meses.

*Cuidado com o cache do build.* O Gradle **já produziu falso-verde** em teste de arquitetura
aqui. Verificação de arquitetura roda com `--rerun-tasks`, sempre.

### 8.3 Verificação

**Número contado, nunca deduzido.** Toda afirmação quantitativa deste projeto vem de um
harness que roda sobre o acervo — `src/test/java/**/medicao/` —, desligado por padrão
(`-Dkronos.medicao=true`) e chamando os serviços de produção pelo CDI, não uma cópia da regra.

**Instrumento adequado à classe de defeito.** HTTP 200 não prova que a tela funciona. Contagem
de eventos igual não prova que o conteúdo é o mesmo. Estilo musical não prova ausência de
karaokê — a tag `\k` prova.

**Medir o efeito antes de comemorar o conserto.** Exemplo de 05/08/2026: a hipótese era que o
detector de lore *perdia* nomes por causa da quebra `\N`. O medido foi o contrário — ele
**acusava o que estava certo**, porque o inglês trazia `the Zeta Gundam` numa linha e o
português quebrava em `Zeta\NGundam`, e a comparação falhava.

| | falas com pendência | motivos |
|---|---|---|
| antes | 13.099 de 16.023 (**81,8%**) | 16.956 |
| depois | 4.163 de 16.023 (**26,0%**) | 5.576 |
| grupo de controle (sem quebra) | 6.929 de 52.138 (13,3%) | **não se moveu** |

O grupo de controle é a parte que transforma isso em experimento: se as falas **sem** quebra
tivessem se movido, a mudança teria vazado para fora do escopo dela.

**Medir também quando o resultado desautoriza o conserto.** No `ValidadorTraducaoService`, a
mesma classe de defeito existia — e a medição achou 3 casos, **todos falso positivo**: são o
cartão de título do episódio (`The 08th MS Team`), nome próprio que deve permanecer em inglês.
Consertar ali faria o validador acusar tradução correta. Forma errada não implica defeito.

### 8.4 Operação

**Olhar antes de destruir.** O acervo em `cache/` é *gitignored* desde 25/07/2026 — apagá-lo é
**irreversível pelo git**. Toda operação que reescreve o acervo faz snapshot em `backups/`
antes, com carimbo de tempo. Já foram três: reforço de terminologia em Zeta/ZZ/Unicorn (230
falas), no 86 (103 falas), e regeração de `.ass`.

**Ensaio antes de aplicar.** As correções em massa têm um modo *ensaio* que conta e mostra sem
escrever, e um modo *aplicar* com **portão triplo**: propriedade de medição ligada, variável
explícita de confirmação, e a flag do próprio pipeline.

**Escrita atômica** em todo artefato persistido — temporário mais `move`. Arquivo corrompido é
preservado com sufixo, nunca apagado.

**Não editar `.java` com tradução em curso.** A recarga a quente do Quarkus derruba o trabalho
em andamento. Conferir a porta antes de reiniciar também é obrigatório: o `quarkusDev` deixa a
JVM órfã viva na 8099, e a aplicação velha segue servindo código antigo enquanto se acredita
que reiniciou.

---

## 9. Dívidas declaradas

Não são ideias de melhoria. São coisas que estão erradas hoje, com endereço.

**1. Onze das vinte fatias não têm fronteira ArchUnit.** Nove têm. Isso é o nível onde a
refatoração parou, e está declarado como tier secundário. A correção certa é um Plano-Mestre
que eleve o **tier inteiro** — elevar uma fatia isolada a deixa mais desacoplada que as irmãs
sem tornar o sistema mais consistente.

**2. A escolha de faixa de legenda erra em release com ordem invertida.**
`ExtratorAssStrategy` tenta palavras-chave e, falhando, **pega a última candidata**, com o
comentário "a primeira é signs". No Break Blade a ordem é o inverso: a faixa completa é a
`[Coalgirls]` e a última é `Signs/Songs`. Resultado medido: os 6 filmes foram traduzidos com
**373 falas** quando o diálogo real tem **3.457** — 9,3x menos. A palavra `Signs` está no nome
da faixa e nada no código a lê. Falta um filtro negativo antes do fallback.

**3. A chave do cache usa nome de pasta genérico.** `cache/Season 1` já existe. Duas obras com
a mesma grafia de pasta misturariam cache.

**4. O detector de concordância alcança 0,8% do acervo.** Medido: 30 disparos em 68.161 falas
(0,04%), e apenas 9 das suas regras já dispararam alguma vez. A lógica não está errada — dentro
das 574 falas onde seu vocabulário cruza com marcador de gênero do inglês, ele acusa ~5%, taxa
razoável. O problema é o alcance: lista fechada contra vocabulário aberto. As outras 3.467
falas com marcador de gênero estão fora do seu campo de visão.

**5. A proteção do japonês na preparação de lote nunca foi medida.** A deduplicação por texto
visível entrou sem verificar se agrupa camada japonesa com inglesa. É requisito declarado e
não verificado.

**6. Não há medição de vazão.** `§5.2`.

**7. Não há supervisão de processo.** Reinício do sistema operacional mata a tradução em curso
sem recuperação. Ocorreu em 06/08/2026, 01:28.

**8. `\b` como fronteira de palavra não é coberto pela catraca.** São 103 ocorrências em 13
arquivos, e a exclusão é decisão medida, não esquecimento: a auditoria de 05/08/2026 mediu
cada família e encontrou 0 perdas em três dos quatro arquivos que rodam sobre texto de fala. O
quarto foi corrigido. Mas a catraca cobre a família do *lookbehind*, não o `\b` — quem ler
"toda fronteira está coberta" está lendo errado.

---

Os números de linhas, classes, fatias, contextos, testes e guardas foram lidos do código-fonte
em 06/08/2026. As contagens de falas, pendências e qualidade de saída foram lidas do acervo em
disco e do cache, pelos harnesses de medição do próprio projeto. Onde não houve medição, está
escrito que não houve.
