-- [gerado] ESQUEMA DA LORE DO KRONOS — gerado em 2026-10-09 a partir do antigo lore.yaml; desde então, editado à mão.
-- [gerado] Ordem de carga: este arquivo, depois cada id de obras.lst (obras/<id>.sql), numa transação.
-- [gerado] Os comentários sem a marca [gerado] são CICATRIZ migrada do lore.yaml, com a medição que a criou.

-- =====================================================================================
-- ARQUIVO UNICO DE LORE — TRADUCAO
--
-- Gerado dos provedores REAIS por GeradorLoreYamlIT (nunca digitado), e a geracao so
-- escreve depois de provar IDA E VOLTA campo a campo. As CICATRIZES abaixo e ao longo do
-- arquivo foram migradas a mao das classes Java em 2026-08-15 — sao medicao real, nao
-- comentario decorativo, e sao o motivo de o formato ser YAML e nao JSON.
--
-- ATENCAO: regenerar este arquivo com o gerador produz ZERO comentario. Copiar o gerado
-- por cima APAGA toda a cicatriz. A catraca CatracaCicatrizNoLoreYamlTest existe para que
-- isso reprove o build em vez de passar em silencio.
-- =====================================================================================
-- ===== CICATRIZ: NUCLEO UC — compartilhado pelas obras do Universal Century — de CorrecoesTerminologiaGundamUc.java =====
-- PLURAL: o canônico "Newtype" é palavra ÚNICA e a checagem exige fronteira à direita,
-- então "Newtypes" no inglês NÃO casa com ele e a restauração nunca dispara. Medido no
-- run de ZZ: "Is this a meeting of fellow Newtypes?" saiu como "uma reunião de novos
-- tipos". Mesma lacuna que "Beam Sabers" tinha; as famílias Mobile Suit e Mobile Armor
-- já traziam singular e plural desde o início.
-- Compostos MEIO-traduzidos (o LLM localiza "Mobile"->"Móvel"/"Móveis" mas mantém "Suit"),
-- e os plurais das formas-ruim acima — o canônico plural "Mobile Suits" casa o EN
-- "Mobile suits" (checagem multi-palavra é case-insensitive no EnforcadorTermosLore).
-- "terno" é roupa social: JAMAIS serve para Mobile Suit, em nenhuma combinação — decisão
-- do dono do acervo. "Terno Móvel" já estava coberto; falta a variante sem o adjetivo.
-- Variantes de "Beam Saber" MEDIDAS no run completo de Gundam ZZ (47 episódios, 16.716
-- pares), não imaginadas: o termo sobreviveu em 0% das 7 ocorrências e NENHUMA destas
-- formas estava aqui, por isso passaram inteiras.
-- 
-- "sabre de luz" é terminologia de OUTRA franquia e apareceu 3 vezes; as demais são
-- localizações plausíveis mas fora do glossário UC.
-- 
-- Decisão do dono do acervo sobre "Mobile Suit", registrada porque a fronteira aqui é
-- fina e não se deduz do código:
--   - o canônico é "Mobile Suit" e as entradas acima que o restauram FICAM, incluindo
--     "Traje Móvel" e "Terno Móvel";
--   - mas "móvel de combate", "unidade móvel" e "unidades móveis" são aceitáveis por
--     contexto e por isso NÃO entram no mapa. Foram 101 falas do run de ZZ que este
--     enforcer deixará em paz de propósito — reescrevê-las seria corromper tradução
--     legítima, que é o dano que ele existe para evitar.
-- Mesma lógica para "Beam Rifle": "rifle de feixe" e "arma de feixe" passam.
-- NORMAL SUIT / POWERED SUIT — o traje pressurizado do piloto.
-- Medido no run completo de ZZ: 23 ocorrências de "normal suit" no inglês e ZERO
-- preservadas. O termo não estava na lore, nem no termosProtegidos, nem aqui: nada o
-- protegia, e o modelo produziu NOVE renderizações diferentes para a mesma coisa
-- ("terno normal", "traje normal", "uniforme normal", "armadura normal", "roupa
-- normal"...). O custo não é só terminológico: a mesma peça muda de nome entre
-- episódios e o espectador perde o referente.
-- 
-- As entradas abaixo cobrem as variantes LEXICAIS, que é o que restauração determinística
-- sabe fazer. Onde o modelo troca o traje por outro substantivo do universo, o mapa não
-- alcança — isso é leitura de cena, não variante de palavra, e exige revisão humana.
-- Cartão de título "POWERED SPACESUIT" do ep01, única ocorrência observada.
-- Vieram do catálogo ESPELHO da revisão de lore, onde já existiam. A duplicação entre os
-- dois catálogos é deliberada (fatia não importa fatia), mas ela derivou nos DOIS sentidos:
-- as variantes de Beam Saber e Normal Suit acima só existiam aqui, e estas seis só lá.
-- Duplicação consciente exige quem acuse a divergência — ver ParidadeMapasTerminologiaTest.
-- MINERADAS no acervo em 2026-08-04 (61.051 falas): a fala cujo texto visivel INTEIRO e
-- "Mobile Suit" aparece 149 vezes, em ZZ (95) e Zeta (54), e a MAIORIA sao CARTOES DE
-- TITULO -- o ingles vem em caixa alta ("MOBILE SUIT"), texto em tela, nao dialogo. Ali
-- nao ha contexto que salve: e o nome da franquia sendo reescrito.
-- 
--   MOVEL DE ASSALTO  74x     MOVEL DE GUERRA   2x
--   Movel de Assento  31x     Mobil Suit        2x   (erro de grafia do proprio canonico)
-- 
-- COLISAO MEDIDA: ZERO. Nenhuma fala do acervo traz "Mobile Suit" no ingles e uma destas
-- formas em PT sem ser exatamente este defeito -- entao a restauracao so corrige.
-- 
-- "Movel de Combate" (8x) FICA DE FORA: e a decisao registrada acima, de que essa familia
-- e aceitavel por contexto. Confirmada pelo dono do acervo em 04/08 ao aprovar estas
-- quatro. "MS" (2x) tambem fica: e abreviacao oficial, nao forma-ruim.
-- 
-- GAP DECLARADO: os PLURAIS destas quatro nao foram medidos e por isso nao entram. E a
-- mesma lacuna que "Newtypes" teve (ver acima); se aparecerem, medir antes de adicionar.
-- "MOVEL DE COMBATE" — a exclusao acima foi REVISTA em 05/08/2026, com o numero na mao.
-- 
-- A regra "aceitavel por contexto" foi escrita pensando em DIALOGO, e continua certa la.
-- Mas a mesma forma aparece em CARTAO DE TITULO, onde o ingles vem "MOBILE SUIT" em caixa
-- alta e nao existe contexto que a salve: e o nome da franquia escrito errado na tela.
-- 
-- MEDIDO no acervo, e a separacao e TOTAL:
--     EN-CAIXA-ALTA / PT-Capitalizado ....... 8   (cartao de titulo)
--     en-normal     / pt-minusculo .......... 1   (dialogo)
-- 
-- CUSTO ACEITO, e ele e real: a unica fala de dialogo tem "a combat mobile suit" no
-- ingles, entao a restauracao TAMBEM a alcanca e ela perde o "de combate":
--     EN "My Geze has the mobility of a combat mobile suit!"
--     PT "Meu Geze tem a mobilidade de um móvel de combate!"  ->  "...de um Mobile Suit!"
-- Oito cartoes com o nome da franquia errado valem mais que uma fala perdendo um
-- adjetivo. Se a leitura mudar, e esta linha unica que sai.
-- 
-- "unidade movel" e "unidades moveis" seguem FORA — aquela parte da decisao nao mudou.
-- "BRIGHT" — Bright Noa, capitao da White Base / Argama / Ra Cailum. Entrou em
-- 2026-08-11 depois que a correcao online do cache do Zeta devolveu "Commander Bright!"
-- como "Comandante Brilhante!" (ep02) e "- Certo. [Brilhante." (ep33): o nome do
-- personagem virou adjetivo. "Bright Noa" ja estava no termosProtegidos das obras e NAO
-- adiantou — aquele conjunto isenta da checagem de residuo, nao restaura grafia, e o
-- texto que quebra traz "Bright" sozinho, que nem casa a forma composta.
-- 
-- MEDIDO no acervo antes de entrar, nao imaginado:
--     "Bright" maiusculo no ingles ............ 149  (ZZ 77, Zeta 67, CCA 5)
--     delas, o adjetivo "bright" ..............   0  — as 149 sao o personagem
--     colisao com "brilhante" na MESMA fala ...   0  em 67 pares EN/PT do Zeta
-- 
-- RISCO RESIDUAL DECLARADO: a forma-ruim e comparada IGNORANDO CAIXA, entao uma fala que
-- trouxesse "Bright" no ingles E "brilhante" como adjetivo na traducao perderia o
-- adjetivo. Nao existe uma sequer nos 67 pares medidos; os 82 pares de ZZ e CCA nao
-- foram pareados por nome de arquivo e seguem NAO conferidos.
-- "Partículas Minovsky" NÃO entra, por decisão do dono do acervo: a forma em português é
-- aceitável e forçar o inglês corromperia tradução legítima. Mesma régua de "rifle de
-- feixe", "arma de feixe" e "unidade móvel". O espelho da revisão também a perdeu, para
-- os dois catálogos seguirem dizendo a mesma coisa.
-- ===== CICATRIZ: NUCLEO ZZ — de CorrecoesTerminologiaGundamZz.java =====
-- ---------------------------------------------------------------------------
-- FA YUIRY. Minerado no acervo em 2026-08-04: a fala cujo texto INTEIRO e "Fa"
-- aparece 51 vezes (Zeta 43, ZZ 8) e so 8 preservaram o nome. O modelo le "Fa!"
-- como palavra comum e produz cinco coisas diferentes:
-- 
--   Fogo!  27x     Fa...  ->  Fá...  6x     Pá!   1x
--   Fala!   7x                             Fale!  1x
-- 
-- "Fa Yuiry" JA esta em termosProtegidos desde sempre, e nao adiantou: aquele
-- conjunto isenta o termo da checagem de residuo, nao restaura grafia. Quem
-- restaura e este mapa.
-- 
-- SEGURANCA: a restauracao so dispara quando o INGLES contem "Fa", e o canonico
-- e de UMA palavra -- entao a checagem e SENSIVEL A CAIXA e "fa" minusculo nunca
-- casa. Um "Fire!" qualquer jamais e tocado, porque nao traz "Fa" no original.
-- 
-- COLISAO MEDIDA no acervo: 6 falas trazem "Fa" no ingles e uma destas formas em
-- PT dentro de fala MAIOR -- e as 6 sao o MESMO defeito, nao colisao legitima:
--   EN "Fa! Katz!"                -> PT "Fala, Katz!"
--   EN "Fa, taking off!"          -> PT "Fá, decolando!"
--   EN "Fa! We're outnumbered!"   -> PT "Fogo! Estamos em menor número!"
-- A regra corrige as seis.
-- 
-- RISCO RESIDUAL DECLARADO: "Fala" e "Fale" sao palavras comuns em PT. Se um dia
-- uma fala trouxer "Fa" no ingles E um "fala" MINUSCULO legitimo ("Fa, escute
-- minha fala"), o orcamento de minusculas de restaurarLimitado pode troca-lo.
-- Hoje isso e ZERO no acervo; se aparecer, o sintoma sera "Fa" no meio da frase.
-- 
-- "Faça isso" (1x) fica de fora: nao e variante do nome, e outra frase inteira.
PRAGMA foreign_keys = ON;

-- [gerado] Identidade da obra. 70 ids: sem_lore só existe na tradução e macross_dyrl só na revisão.
CREATE TABLE obra (
  id TEXT PRIMARY KEY CHECK (length(id) > 0 AND id NOT GLOB '*[^a-z0-9_]*')
) STRICT;

-- [gerado] Lado da TRADUÇÃO. O prompt vira o contextoHash do cache: \r proibido, porque o literal SQL
-- [gerado] não normaliza quebra de linha como o YAML normalizava.
CREATE TABLE lore_traducao (
  obra_id          TEXT PRIMARY KEY REFERENCES obra(id),
  nome             TEXT NOT NULL CHECK (length(trim(nome)) > 0),
  aparece_na_lista INTEGER NOT NULL CHECK (aparece_na_lista IN (0, 1)),
  prompt           TEXT NOT NULL CHECK (length(prompt) > 0 AND instr(prompt, char(13)) = 0)
) STRICT;

-- =====================================================================================
-- LADO DA REVISAO DE LORE (FASE E, 2026-08-15)
--
-- Ate aqui a lore de uma obra era a UNIAO de dois pacotes, e essa uniao nao existia em
-- lugar nenhum do codigo: 82 classes em contexto.lore e 80 em revisaoLore.contexto, com
-- 18 entradas que so a traducao conhecia e 69 que so a revisao conhecia. Foi isso que o
-- "Spearhead -> Esquadroe de Ponta" cobrou. A partir daqui a lore existe INTEIRA aqui.
--
-- O PROMPT segue DUPLICADO de proposito: prompt de revisao e legitimamente diferente do
-- de traducao (um corrige terminologia, o outro traduz). O que se unifica e a
-- TERMINOLOGIA, porque so ela e invariante — "Spearhead" e "Spearhead" nos dois lados.
-- =====================================================================================

-- [gerado] Lado da REVISÃO. O prompt é diferente do da tradução de propósito.
CREATE TABLE lore_revisao (
  obra_id TEXT PRIMARY KEY REFERENCES obra(id),
  nome    TEXT NOT NULL CHECK (length(trim(nome)) > 0),
  prompt  TEXT NOT NULL CHECK (length(prompt) > 0 AND instr(prompt, char(13)) = 0)
) STRICT;

-- [gerado] Terminologia UMA vez por obra: tradução e revisão recebem o mesmo conjunto e o mesmo mapa.
CREATE TABLE termo_protegido (
  obra_id TEXT NOT NULL REFERENCES obra(id),
  termo   TEXT NOT NULL CHECK (length(trim(termo)) > 0),
  PRIMARY KEY (obra_id, termo)
) STRICT, WITHOUT ROWID;

CREATE TABLE correcao_terminologia (
  obra_id    TEXT NOT NULL REFERENCES obra(id),
  forma_ruim TEXT NOT NULL,
  canonico   TEXT NOT NULL,
  CHECK (forma_ruim <> canonico),
  PRIMARY KEY (obra_id, forma_ruim)
) STRICT, WITHOUT ROWID;

-- [gerado] Só tradução.
CREATE TABLE traducao_obrigatoria (
  obra_id TEXT NOT NULL REFERENCES obra(id),
  origem  TEXT NOT NULL,
  destino TEXT NOT NULL,
  PRIMARY KEY (obra_id, origem)
) STRICT, WITHOUT ROWID;

-- [gerado] A ordem dos pares é a que o consumidor itera; "ordem" a preserva.
CREATE TABLE par_inconfundivel (
  obra_id TEXT NOT NULL REFERENCES obra(id),
  ordem   INTEGER NOT NULL,
  a       TEXT NOT NULL,
  b       TEXT NOT NULL,
  CHECK (a <> b),
  PRIMARY KEY (obra_id, ordem)
) STRICT, WITHOUT ROWID;

-- [gerado] O mesmo apelido em duas obras já derruba o arranque (colisão de identidade); UNIQUE antecipa.
CREATE TABLE apelido_pasta (
  obra_id TEXT NOT NULL REFERENCES obra(id),
  apelido TEXT NOT NULL UNIQUE,
  PRIMARY KEY (obra_id, apelido)
) STRICT, WITHOUT ROWID;

-- [gerado] Só revisão.
CREATE TABLE equivalencia_aceita (
  obra_id TEXT NOT NULL REFERENCES obra(id),
  termo   TEXT NOT NULL,
  ordem   INTEGER NOT NULL,
  forma   TEXT NOT NULL,
  PRIMARY KEY (obra_id, termo, ordem)
) STRICT, WITHOUT ROWID;
