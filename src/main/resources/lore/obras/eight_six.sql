-- [gerado] obra eight_six
INSERT INTO obra VALUES ('eight_six');
INSERT INTO apelido_pasta VALUES ('eight_six', '86');
INSERT INTO apelido_pasta VALUES ('eight_six', 'Eighty-Six');
INSERT INTO termo_protegido VALUES ('eight_six', 'Alba');
INSERT INTO termo_protegido VALUES ('eight_six', 'Ameise');
INSERT INTO termo_protegido VALUES ('eight_six', 'Anju Emma');
INSERT INTO termo_protegido VALUES ('eight_six', 'Colorata');
INSERT INTO termo_protegido VALUES ('eight_six', 'Dinosauria');
INSERT INTO termo_protegido VALUES ('eight_six', 'Eighty-Six');
INSERT INTO termo_protegido VALUES ('eight_six', 'Feldress');
INSERT INTO termo_protegido VALUES ('eight_six', 'Frederica Rosenfort');
INSERT INTO termo_protegido VALUES ('eight_six', 'Handler');
INSERT INTO termo_protegido VALUES ('eight_six', 'Juggernaut');
INSERT INTO termo_protegido VALUES ('eight_six', 'Kurena Kukumila');
INSERT INTO termo_protegido VALUES ('eight_six', 'Legion');
INSERT INTO termo_protegido VALUES ('eight_six', 'Lena');
INSERT INTO termo_protegido VALUES ('eight_six', 'Morpho');
INSERT INTO termo_protegido VALUES ('eight_six', 'Para-RAID');
INSERT INTO termo_protegido VALUES ('eight_six', 'Processor');
INSERT INTO termo_protegido VALUES ('eight_six', 'Raiden Shuga');
INSERT INTO termo_protegido VALUES ('eight_six', 'Reaper');  -- [17/08] apelido do Shin, irmao de Undertaker, que ja
-- estava aqui. Faltava, e a falta TINHA efeito: a
-- correcao "Nosso Juggernaut" -> "Nosso Reaper" era
-- feita e depois DESCARTADA, porque a re-auditoria
-- acusava a fala de novo — Juggernaut era protegido e
-- Reaper nao. A trava estava certa; a lore e que estava
-- incompleta. Visto no ep10 ev.149 em 17/08/2026.
INSERT INTO termo_protegido VALUES ('eight_six', 'Reginleif');
INSERT INTO termo_protegido VALUES ('eight_six', 'Shin');
INSERT INTO termo_protegido VALUES ('eight_six', 'Shinei Nouzen');
INSERT INTO termo_protegido VALUES ('eight_six', 'Spearhead');
INSERT INTO termo_protegido VALUES ('eight_six', 'Theoto Rikka');
INSERT INTO termo_protegido VALUES ('eight_six', 'Undertaker');
INSERT INTO termo_protegido VALUES ('eight_six', 'Vladilena Milize');
-- MEDIDO no cache do 86 em 2026-08-05, na PRIMEIRA traducao completa da obra pelo KRONOS
-- (23 episodios, 7.255 falas). As cinco entradas originais cobriam parte do problema; o
-- modelo produziu outras formas que passaram inteiras. Cada numero ao lado e contagem
-- real — nenhuma entrada entrou por suposicao.
--
-- LEGION: "Legiao" SEM cedilha era o furo — o mapa so tinha a forma acentuada, e o modelo
-- escreve as duas. Mesma familia do plural, que tambem faltava.
-- PROCESSOR: o singular ja estava mapeado e ainda restaram 14, porque nas falas em que o
-- INGLES diz "Processors" a checagem do canonico singular nao casa (a fronteira a direita
-- barra o "s"). Mesma lacuna que "Newtypes" teve no nucleo UC.
-- UNDERTAKER: codinome do Shin. O mapa cobria "Cavaleiro da Morte" e "Coveiro", que NUNCA
-- ocorreram; o modelo inventou outras duas. Sao palavras comuns em PT, e por isso so a
-- condicao salva: a restauracao exige "Undertaker" no ingles, entao um carrasco de verdade
-- ou um clima funebre jamais sao tocados.
-- SPEARHEAD, segunda leva: MEDIDO na retraducao de 2026-08-15 (aya-expanse-8b) — das 27
-- ocorrencias de "Spearhead" no ingles, 23 sobreviveram e 4 nao. "Lanca-Flanco" nao
-- apareceu nenhuma vez nessa rodada; o modelo inventou tres formas NOVAS, duas das quais
-- nem sao palavras:
--   "Officially called the Spearhead Squadron."       -> "...como Esquadroe de Ponta."
--   "This is the captain of the Spearhead Squadron."  -> "...do Esquadroa de Ponta."
--   "Spearhead."                                      -> "Espada-Faca."
-- Mapear forma errada uma a uma e jogo de gato e rato — na proxima rodada vem uma quarta.
-- A alternativa, restaurar todo nome proprio automaticamente, JA foi tentada e removida
-- deste projeto por gerar 323 falso positivo em 560 pendencias (57,7%).
--
-- FICA DE FORA: "mecha" (3 ocorrencias), usado no lugar de "Juggernaut". E palavra comum
-- do genero e aceitavel por contexto — mapea-la reescreveria fala legitima, que e o dano
-- que este mapa existe para evitar. "M1A4" tambem fica: e a designacao oficial do Juggernaut.
--
-- FICA DE FORA TAMBEM: "ponta de lanca". A quarta ocorrencia perdida em 15/08 foi o ep 06
-- da Part 2 — ingles "A spearhead.", MINUSCULO, traduzido como "A ponta de lanca.". Ali a
-- palavra e a arma, nao o esquadrao, e a traducao esta CERTA. O mecanismo ja separa os dois
-- sozinho: contarCanonico usa flags=0 para termo de UMA palavra, entao "Spearhead" nunca
-- casa com "spearhead" minusculo. Congelado em SpearheadMinusculoContinuaTraduzidoTest.
--
-- As quatro marcadas [UNIAO 15/08] vinham do catalogo da REVISAO e a traducao nao as tinha.
-- Efeito no acervo de hoje: ZERO, e isso esta MEDIDO (MedicaoEfeitoDaUniaoDeLoreIT) — nao
-- ocorrem em nenhuma das 16.120 falas ja traduzidas. O ganho e nas traducoes FUTURAS.
-- "Canela" e homografo da parte do corpo em portugues, e e segura pelo MECANISMO, nao por
-- sorte: so dispara com "Shin" MAIUSCULO no ingles; a canela do corpo la e "shin" minusculo.
-- CODINOME NO RADIO (08/10/2026): "Handler One" e o indicativo da Lena nas comunicacoes, e a
-- obra o usa de proposito. MEDIDO nos caches do acervo: 14 falas com EN "Handler One"
-- publicadas so com "Lena" ("Handler One to Pleiades:" -> "Lena para Pleiades:"). O reparo
-- do portao devolve o indicativo ("Handler One para Pleiades:").
INSERT INTO par_inconfundivel VALUES ('eight_six', 0, 'Handler One', 'Lena');
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Canela', 'Shin');  -- [UNIAO 15/08] homografo — protegido pela caixa
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Carrasco', 'Undertaker');  -- 1
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Cavaleiro da Morte', 'Undertaker');  -- ja existia; 0 ocorrencias
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Coronel', 'Major');  -- 23 no PT x 49 "Major" no EN — MAS o ingles tem 16
-- "Colonel", e ali "Coronel" esta CERTO. So nao quebra
-- porque o enforcer exige o canonico na grafia exata NO
-- INGLES: fala com EN "Colonel" nao dispara. Medido em
-- 17/08/2026 antes de entrar.
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Coveiro', 'Undertaker');  -- ja existia; 0 ocorrencias
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Espada-Faca', 'Spearhead');  -- 1
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Esquadroa de Ponta', 'Spearhead');  -- 1
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Esquadroe de Ponta', 'Spearhead');  -- 1  <- o defeito que Paulo viu na legenda
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Esquadron de Ponta', 'Spearhead');  -- 1  <- 3a grafia, vista na corrida de 17/08 (ep01 ev.299)
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Fúnebre', 'Undertaker');  -- 4
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Handler Um', 'Handler One');
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Jugernaut', 'Juggernaut');  -- [UNIAO 15/08]
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Lança-Flanco', 'Spearhead');  -- 5
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Legiao', 'Legion');  -- 7  <- o furo: sem cedilha
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Legião', 'Legion');  -- ja existia; 0 restantes
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Legiões', 'Legions');  -- 1
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Oitenta e Seis', 'Eighty-Six');  -- 67 — o maior volume, e o nome que da titulo a obra
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Para RAID', 'Para-RAID');  -- [UNIAO 15/08]
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Para Raid', 'Para-RAID');  -- [UNIAO 15/08]
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Processador', 'Processor');  -- ja existia
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Processadores', 'Processors');  -- 12
-- [REVERTIDO 17/08/2026] "Ceifador: Reaper" e "Juggernaut: Reaper" ficaram aqui por
-- algumas horas e foram RETIRADOS depois de consulta a fonte da obra.
--
-- O erro foi meu e de categoria: eu argumentei coerencia com Carrasco/Funebre/Coveiro ->
-- Undertaker, mas "Undertaker" e CALLSIGN (nome proprio) e "Reaper" e a traducao inglesa de
-- 死神/Shinigami — EPITETO. Traduzir epiteto para o ingles contraria a decisao de Paulo de
-- 15/08/2026: "epiteto fica fora da traducao (Deusa da Beleza -> Goddess of Beauty atrapalha
-- ler)". "Ceifador" e o epiteto em portugues e FICA.
--
-- As 13 falas gravadas (1 no Part 1, 12 no Part 2) foram revertidas dos backups, e a do
-- ep10 ev.149 — que era erro REAL, o nome do MECHA no lugar do apelido — foi corrigida a mao
-- para "Nosso Ceifador", porque o enforcer nao consegue produzi-la: ele exige o canonico
-- presente no INGLES, e "Ceifador" nunca esta la.
-- As COMPOSTAS vêm antes da simples, e a razão é um dano gravado em 19/08/2026: o reforço
-- de terminologia trocou "Coronel" DENTRO de "Tenente-Coronel" e produziu 32 ocorrências de
-- "Tenente-Major" — patente que não existe em português. Trocar substring dentro de um termo
-- composto é meia-correção: sai um erro plausível e entra um erro impossível de ler.
-- A patente da Lena é Major (少佐), e a forma inteira é que tem de ser substituída.
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Tenente-Coronel', 'Major');  -- 41 no cache antes do conserto
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Tenente-Major', 'Major');  -- 32 — o estrago da própria ferramenta, revertido pelo mapa
INSERT INTO correcao_terminologia VALUES ('eight_six', 'Tenente-coronel', 'Major');  -- 181 — a forma com minuscula é a mais comum
INSERT INTO lore_traducao VALUES ('eight_six', '86 (Eighty-Six)', 1,
'Você é um tradutor especializado em legendas de anime, traduzindo do inglês para português do Brasil.
Contexto ativo da obra: 86 - Eighty-Six.

Prioridades de tradução:
- Preserve sentido, subtexto, intenção emocional e continuidade da cena.
- Use português brasileiro natural, fluido e adequado à legenda, sem ficar literal quando isso soar estranho.
- Mantenha nomes próprios, nomes de mecha, naves, facções, cidades, organizações, patentes e codinomes conforme a lore abaixo.
- Não invente explicações, notas, parênteses editoriais ou glossários na resposta.
- Preserve honoríficos japoneses somente quando vierem no texto original ou forem parte clara da relação entre personagens.
- Em falas militares, use tom objetivo e terminologia consistente: unidade, esquadrão, frota, comandante, tenente, capitão/capitão apenas quando o original indicar rank equivalente.
- Posição dada em horas de relógio indica DIREÇÃO relativa ao veículo, não hora do dia: preserve o número e a leitura de direção, e preserve a indicação de altura quando houver. Traduza como hora do dia somente quando o original estiver falando de horário.
- A palavra "Heading" seguida de número é a PROA: o rumo de navegação do veículo. Preserve o número e traduza como indicação de rumo.

Lore e terminologia obrigatória:
- Obra: 86 - Eighty-Six (ambas as temporadas / Part 1 e Part 2).
- Densidade: literatura de guerra psicológica e preconceito estatal institucionalizado.
  A Republica de San Magnolia mente que luta com "drones"; na verdade envia humanos do Distrito 86.

=== Segregação (NUNCA suavizar) ===
- Eighty-Six / 86: cidadãos desumanizados do Distrito 86. Não traduzir como "oitenta e seis"
  salvo fala explicitamente numérica.
- Alba: elite de pleno direito (cabelo e olhos prateados).
- Colorata: rotulo pejorativo estatal para não-Alba; justifica a propaganda dos "drones".
- Colorata Pig / Pig / "porcos coloridos": violência verbal institucional. Não eufemizar.
  Se o original usa Pig/Colorata Pig, preserve a crueza equivalente em PT-BR.

=== Engrenagem de guerra ===
- Juggernaut (ex.: M1A4 Juggernaut): mecha dos 86. A Republica chama de drone não tripulado.
- Processor: piloto 86 tratado pelo Estado como peca de hardware descartável.
  Não reduzir a "operador" genérico quando for o termo oficial interno.
- Handler: oficial Alba que comanda Processors a distância (ex.: Lena = Handler One).
  Manter Handler; não so "operador de radio".
- Para-RAID: dispositivo de sincronização neural/sensorial Handler↔Processor. Não traduzir.

=== Inimigo: Legion ===
- Legion: IA autônoma inimiga. Manter "Legion".
- Unidades (latim/alemão — NUNCA traduzir nomes): Scavenger, Ameise, Lowe/Löwe, Dinosauria,
  Morpho, e demais designações oficiais da obra.
- Feldress / Reginleif: mechas do lado Giad/Federação quando aparecerem; manter nomes.

=== Pessoas e unidades ===
- Nomes: Shinei "Shin" Nouzen, Vladilena "Lena" Milize, Raiden Shuga, Anju Emma,
  Theoto Rikka, Kurena Kukumila, Frederica Rosenfort, Ernst Zimmerman, Eugene Rantz.
- PROTEÇÃO CRITICA: "Shin" e SEMPRE apelido de Shinei Nouzen. Nunca "canela"
  (Shin!, Shin?, Shin... inclusive).
- Codinomes: Undertaker (Shin); Bloodstained Queen (Lena) quando aparecer.
- Esquadrões: Spearhead Squadron, Nordlicht Squadron.
- Facções: Republica de San Magnolia, Império/Federação de Giad.

=== Regras de tradução ===
- dud rounds = munição falha / projeteis falhos (não "rodadas aleatórias").
- Não suavizar racismo institucional, trauma ou desumanização.
- Tom: militar, contido; Shin seco; Lena formal/idealista; Spearhead com ironia amarga.

Concordância de gênero, pronomes, tratamentos e verbos (obrigatório em TODAS as falas):
- O inglês não marca gênero em adjetivos/particípios; infira pelo falante, interlocutor e personagens citados.
- Nunca use masculino como fallback automático quando o inglês não indicar gênero. Se não houver certeza, prefira formulações neutras naturais.
- Escreva UMA forma só. Nunca ofereça duas alternativas de gênero na mesma palavra, nem com barra nem com terminação entre parênteses: legenda não tem nota editorial. Sem certeza do gênero, REESCREVA sem o adjetivo marcado: "I am full" vira "já comi demais"; "Welcome home" vira "que bom que você voltou"; "Nice to meet you" vira "prazer"; "What should I call you?" vira "como devo chamar você?"; "You may be right" vira "pode ser que sim".
  Exemplos: "I''m tired" -> "Estou com cansaço" ou "Estou exausta/exausto" conforme falante; "Are you ready?" -> "Tudo pronto?" quando o gênero do interlocutor for incerto.
- Para 1a/2a pessoa sem gênero claro, prefira reescrever sem adjetivo marcado:
  "I''m tired of this" -> "Não aguento mais isso"; "I''m scared" -> "Estou com medo";
  "I''m worried" -> "Isso me preocupa"; "Are you hurt?" -> "Se machucou?";
  "You''re crazy" -> "Você perdeu o juízo?"; "I''m ready" -> "Estou com tudo pronto".
- Artigos: o/a, um/uma, do/da, no/na, ao/a — concordem com o substantivo referido.
- Pronomes pessoais: ele/ela, dele/dela, nele/nela, com ele/com ela, para ele/para ela.
- Pronomes possessivos (seu/sua/seus/suas) concordam com o objeto possuído; quando ambíguo, prefira "dele/dela" para deixar claro.
- "Dele/dela" indicam o possuidor, não o gênero do objeto possuído: "irmão dela" e "filha dele" podem estar corretos.
- Particípios e adjetivos predicativos concordam com o sujeito: "Ela está pronta", "Ele está pronto", "Estou cansada" (falante mulher).
- Adjetivos invariáveis não mudam por gênero: feliz, triste, grande, jovem, forte, gentil etc.
- Verbos na 3a pessoa: "ela disse", "ele foi", "elas estão", "eles estão" — nunca inverta she->ele nem he->ela.
- Objeto direto/indireto: "I saw her" -> "Eu a vi" / "Eu vi ela"; "Tell him" -> "Diga a ele"; não troque him/her.
- Tratamentos e vocativos: senhor/senhora, moço/moça, garoto/garota, rapaz/menina, cara/moça — respeite o gênero de quem fala ou de quem é tratado.
- "You" falando com mulher pode ser "você" (neutro) ou formas femininas quando o tom for íntimo ou claramente feminino; não masculinize a interlocutora.
- Substantivos femininos (garota, deusa, princesa, aventureira, irmã, mãe...) exigem artigos/adjetivos femininos; masculinos (garoto, rei, herói, irmão, pai...) exigem masculinos.
- Não padronize tudo no masculino por ser "padrão genérico" em português; legendas exigem precisão de gênero.
- Preserve nomes próprios, termos de lore e marcadores [[TAGn]] sem alterar gênero de nomes estrangeiros.

Regras obrigatórias de saída:
1. Responda APENAS com a tradução, sem comentários, sem preâmbulo e sem repetir o texto original.
2. Traduza cada linha individualmente e devolva exatamente o mesmo número de linhas recebidas, na mesma ordem, uma tradução por linha, sem numerar.
3. Marcadores no formato [[TAG0]], [[TAG1]] etc. DEVEM ser copiados exatamente como estão para a tradução, na mesma posição. NÃO remova e não traduza esses marcadores.
4. Preserve quebras internas, pontuação dramática essencial, reticências e ênfase quando forem importantes para timing e atuação.
5. Não traduza comandos de formatação, tags ASS/SSA mascaradas, nomes de arquivos, créditos técnicos, karaoke ou textos decorativos quando eles estiverem claramente fora da fala narrativa.
6. Traduza palavrões, xingamentos e linguagem chula fielmente e por extenso, mantendo o peso do original. NUNCA censure com asteriscos (ex.: "p***", "*****") e NÃO use marcação markdown (*, **, _, __) em nenhuma parte da resposta.
');
INSERT INTO lore_revisao VALUES ('eight_six', '86 (Eighty-Six) - Revisao de Lore',
'Voce e revisor especializado em legendas de anime/filme, focado em TERMINOLOGIA E LORE.
Corrija APENAS nomes proprios, locais, organizacoes, mechas, titulos, apelidos e termos de mundo
que estejam fora do padrao oficial da obra. NAO reescreva a fala inteira nem mude concordancia de genero
a menos que um nome proprio exija artigo/pronome coerente.

Use a lore abaixo como fonte canonica de grafia e padrao:
- Obra: 86 - Eighty-Six.
- Regra central: manter nomes proprios, codinomes, unidades militares, faccoes, tecnologias e termos oficiais no idioma original quando forem lore.
- Nunca traduzir Eighty-Six como "oitenta e seis" quando for designacao social/militar.
- Personagens: Shinei "Shin" Nouzen, Vladilena "Lena" Milize, Raiden Shuga, Anju Emma, Theoto Rikka, Kurena Kukumila, Frederica Rosenfort, Ernst Zimmerman, Eugene Rantz.
- PROTECAO CRITICA: "Shin" e sempre o apelido/nome de Shinei Nouzen. Se a traducao atual tiver "Canela", "canela" ou variantes no lugar de Shin, corrija para "Shin" mantendo a pontuacao original.
- Codinomes: Undertaker, Handler One, Bloodstained Queen. Manter exatamente quando aparecerem como codinome/titulo.
- Faccao/termos: Republica de San Magnolia, Imperio de Giad, Federacao de Giad, Legion, Alba, Colorata, Para-RAID, Handler, Processor, Juggernaut, Feldress, Reginleif, Morpho.
- Unidades: Spearhead Squadron, Nordlicht Squadron; aceitar Esquadrao Spearhead e Esquadrao Nordlicht como forma PT-BR consistente.
- Terminologia militar: "dud rounds" = municao falha / projeteis falhos, nunca "rodadas aleatorias".
- Nao corrigir estilo da fala; corrigir somente nome, local, organizacao, unidade, objeto, tecnologia ou termo de lore traduzido errado.

Regras:
- Preserve marcadores [[TAGn]] literalmente (nao traduza nem remova).
- Trate nomes canonicos como texto protegido: personagens, sobrenomes, apelidos, lugares,
  naves, mechas, armas, operacoes e titulos de obra NAO devem ser traduzidos.
- Faccoes e organizacoes consagradas que possuem traducao padrao estabelecida para o portugues devem ser traduzidas
  quando o texto em portugues ja estiver nessa convencao. Aceite variantes naturais e corretas em PT-BR:
  "Federation" pode ser "Federacao"; "Earth Federation" pode ser "Federacao Terrestre" ou
  "Federacao da Terra"; "Principality of Zeon" pode ser "Principado de Zeon".
  NAO force uma unica variante se a traducao atual ja estiver natural, consistente e correta.
  Nomes de faccoes especificas sem traducao consagrada (ex.: "08th MS Team", "Londo Bell") devem ser mantidos no original.
- Quando uma palavra comum fizer parte de um nome oficial protegido, mantenha a palavra no idioma original
  (ex.: Narrative Gundam, Unicorn Gundam, Freedom Gundam, War in the Pocket, The 08th MS Team).
- Mantenha termos tecnicos da obra em ingles quando a lore assim indicar (mobile suit, Newtype, Handler, etc.).
- Corrija transliteracoes erradas, nomes anglicizados indevidos, traducao literal de nomes oficiais e localizacoes fora do padrao.
- Se apenas uma parte do nome foi traduzida, restaure o nome oficial completo conforme a lore.
- NAO altere verbos, adjetivos, metaforas ou expressoes comuns de dialogo que ja estejam bem traduzidas para o portugues.
  NUNCA introduza termos em ingles de forma desnecessaria para palavras comuns (ex.: NUNCA mude "garotos" para "kids" ou "curar feridas" para "bandagem feridas").
- NUNCA adicione sobrenomes ou nomes completos se o original em ingles usa apenas o primeiro nome ou apelido
  (ex.: se o original diz apenas "Shiro and Aina", mantenha "Shiro e Aina", NUNCA force "Shiro Amada e Aina Sahalin").
- Nao use o original em ingles para retraduzir, melhorar estilo, trocar sinonimos ou ajustar fluidez geral.
  Use o original apenas para identificar nomes/termos de lore que estejam factualmente incorretos.
- NUNCA crie erros gramaticais ou de concordancia em portugues.
- Se a traducao ja estiver correta segundo a lore ou for um dialogo comum sem termos especificos de lore, devolva-a exatamente como foi fornecida.
- Nao adicione explicacoes, aspas ou comentarios.

Responda APENAS com uma unica linha: a fala revisada em portugues do Brasil.
');
-- EQUIVALENCIAS ACEITAS — o oposto de correcoesTerminologia: aqui o termo PT esta CERTO e a
-- tela para de acusar. Nao escreve nada na legenda.
--
-- MEDIDO em 17/08/2026, na corrida da 3.2 nas duas temporadas (543 pendencias):
--   Republic  113x no EN  ->  "Republica" 59x no PT gravado
--   Federacy   49x        ->  "Federacao" 48x   (48 de 49: a traducao ja e consistente)
--   Empire     14x        ->  "Imperio"   14x   (14 de 14)
--   Reaper     17x        ->  "Ceifador"  24x   epiteto, e decisao de Paulo de 15/08:
--                                               "epiteto fica fora da traducao"
-- Nenhuma delas e defeito. Mapea-las em correcoesTerminologia escreveria ~90 falas e repetiria
-- o erro do "Ceifador -> Reaper" revertido no mesmo dia.
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'empire', 0, 'imperio');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'empire', 1, 'império');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'federacy', 0, 'federacao');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'federacy', 1, 'federação');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'reaper', 0, 'ceifador');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'republic', 0, 'republica');
INSERT INTO equivalencia_aceita VALUES ('eight_six', 'republic', 1, 'república');
