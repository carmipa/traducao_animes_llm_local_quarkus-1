# ⚡ Ref — Desempenho do Pipeline

> Onde o KRONOS gasta o tempo, com número por operação, medido sobre fala **real** do acervo e com
> os objetos de **produção**. Esta página explica como ler a tela **Sistema → Desempenho** e como
> gerar uma medição nova.

---

## Por que esta medição existe

O projeto pagou três vezes por não a ter. Nas três, o custo apareceu **em produção**, com o
operador esperando:

| quando | o que aconteceu | o que faltava saber |
|---|---|---|
| tela 3.3, uma passada | **291,1s → 14,4s** depois de descobrir que o elo do dicionário era 97% do tempo | qual elo custava |
| arquivo `CCA` | **15 minutos parados** num arquivo só, com um aviso no log | que 2.743 formas distintas estouram o processo externo de uma vez |
| seis arquivos | **5 minutos**, sem saber de onde vinha | idem — foi isso que fez o relógio entrar **por elo** |

Um total único não separa **operação cara** de **pasta grande**. Por isso toda linha da tabela
traz o custo **por unidade**.

---

## Como gerar uma medição

```bash
gradlew test --tests "*MedicaoDesempenhoDoPipelineIT*" -Dkronos.medicao=true "-Dkronos.acervo=C:\animes\ANIMES-TESTES"
```

Grava `relatorios/desempenho.json`. A tela lê exatamente esse arquivo.

> ⚠️ **Rode com a máquina livre.** A medição disputa CPU e disco com qualquer tradução em
> andamento, e o número sai contaminado justamente quando você quer confiar nele. Pior: a medição
> roda pelo Gradle, e **compilar mata tradução em andamento** — rode antes o `pode-compilar.ps1`.

### Por que a tela não tem botão "medir agora"

Seria um botão que degrada o próprio número que promete. A divisão é deliberada: **o harness
mede, a tela mostra.**

---

## Como ler a tabela

Números medidos em 27/08/2026, sobre `ANIMES-TESTES`:

| operação | unidades | por unidade | o que isso quer dizer |
|---|---|---|---|
| `classificar palavra · 1a vez` | 300 | **34,5 ms** | não é só ortografia: são 6 idiomas (pt/en/de/fr/ja/es) por palavra desconhecida |
| `classificar palavra · 2a vez` | 300 | **0,003 ms** | as MESMAS palavras — a memória responde sem arrancar processo |
| `classificar palavra INVENTADA` | 60 | **38,9 ms** | o caso caro: o hunspell gera **sugestão** para cada uma |
| `cadeia 3.3 · TOTAL com arranque` | 400 | 27,7 ms | inclui **8,1s de custo FIXO** que não cresce por fala |
| `cadeia 3.3 · só os 5 elos` | 400 | **7,4 ms** | o custo que **realmente** cresce com a pasta |
| ` elo · acento por POS tagger` | 400 | **7,3 ms** | **98% do custo real da cadeia** |
| ` elo · acento por padrão` | 400 | 0,038 ms | |
| ` elo · gênero (determinante)` | 400 | 0,033 ms | |
| ` elo · acento por dicionário` | 400 | 0,030 ms | |
| ` elo · caractere fora do português` | 400 | 0,017 ms | |
| `leitura do .ass` | 40 | 6,0 ms | por arquivo |

### As três leituras que os números permitem

**1. A memória vale 11.500×.** Primeira consulta 34,5 ms, segunda 0,003 ms. É esse número, e não
uma opinião sobre cache, que explica os 291,1s virarem 14,4s.

**2. Palavra desconhecida é o caso caro — não a conhecida.** O hunspell gasta o tempo gerando
sugestão, não lendo. Por isso um arquivo com muitas formas inéditas (nome próprio, termo de
franquia) custa desproporcionalmente, e por isso a consulta vai em **lotes de 800**: sem lote, o
timeout derruba **todas** as palavras do arquivo, não só as lentas.

**3. Otimizar a cadeia é otimizar o POS tagger.** Ele é 7,3 dos 7,4 ms. Os outros quatro elos
somados custam 0,12 ms — mexer neles não muda nada perceptível.

### O que o total NÃO diz

`27,7 ms/fala` no total contra `7,4 ms/fala` nos elos: a diferença é **arranque** — subir o
LanguageTool, aquecer o dicionário do arquivo, ler e reescrever. Esse custo **se dilui** numa
pasta grande. Ler o total como custo por fala superestima em quase 4×.

---

## Guardas desta medição

- **Caso-controle do relógio.** Antes de afirmar qualquer tempo, o harness cronometra uma pausa de
  5 ms e outra de 40 ms e exige que a segunda saia maior. Um cronômetro quebrado devolveria zero
  para tudo, e o relatório sairia com todas as operações "instantâneas" — o modo de falha mais
  convincente que uma medição de desempenho pode ter.
- **O relógio é consultado, não recriado.** A cadeia da 3.3 já cronometra cada elo e devolve isso
  em `ResultadoConcordancia`. O harness lê aqueles nanos. Uma segunda cronometragem por fora
  divergiria da primeira — e divergiria em silêncio, porque os dois números teriam a mesma cara.
- **Três estados na tela.** "Nunca mediram" (HTTP 404, com o comando) é diferente de "mediram e
  deu vazio". Um estado só faria os dois chegarem iguais.
- **Data sempre visível.** Relatório sem data envelhece sem avisar, e um número de três semanas
  lido como atual é pior que número nenhum.

---

## Navegação

- 📊 [Telemetria](modulo-telemetria.md) — observabilidade da tradução em tempo real
- 🔤 [Etapa 3.3 — Revisão de Concordância](etapa-3.3-revisao-concordancia.md) — a cadeia medida aqui
- 🛡️ [Catracas e Fronteiras](catracas-e-fronteiras.md)
- 🏠 [README](../README.md)
