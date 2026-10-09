-- [gerado] obra gundam_zeta
INSERT INTO obra VALUES ('gundam_zeta');
INSERT INTO apelido_pasta VALUES ('gundam_zeta', 'Mobile Suit Z Gundam');
INSERT INTO apelido_pasta VALUES ('gundam_zeta', 'Z Gundam');
INSERT INTO apelido_pasta VALUES ('gundam_zeta', 'Zeta Gundam');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'A.E.U.G.');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'AEUG');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Addis');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Alexandria');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Amman');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Amuro');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Amuro Ray');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Anaheim Electronics');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Anti-Earth Union Group');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Apolly');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Apolly Bay');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Argama');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Asshimar');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Asshimars');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Astonaige');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Astonaige Medoz');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Audhumla');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Axis');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Axis Zeon');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Barzam');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Barzams');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Bask');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Bask Om');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Batch');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Baund');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Baund Doc');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Beam Rifle');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Beam Saber');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Beltorchika');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Beltorchika Irma');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Ben Wooder');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Blex');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Blex Forer');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Bosnia');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Botty');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Bright Noa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Buran');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Buran Blutarch');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Byarlant');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Char');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Char Aznable');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Colony 30 Incident');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Colony Laser');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Cyber-Newtype');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dakar');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dakar Speech');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dava Baro');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dijeh');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dogosse');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Dogosse Gier');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Earth Federation');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Earthnoid');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Emma');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Emma Sheen');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Fa Yuiry');
-- FOUR isolada — entrou em 2026-08-17 por DECISÃO do Paulo: "usa a própria regra do sistema,
-- nada de reescrever". A regra aqui RECUSA a proposta que perdeu o termo; ela NÃO reescreve
-- nada, e por isso não colide com "Quatro: Quattro" (o Char), cuja chave no mapa de correção
-- já está ocupada. Dois personagens chegam ao PT como a mesma palavra e o mapa não sabe
-- separá-los; a recusa sabe, porque olha o INGLÊS.
--
-- MEDIDO nos 50 ASS do Zeta em 17/08/2026, contra o espelho inglês:
--   Four maiúsculo isolado (fora de "Side Four") ... 188 falas
--     nome PRESERVADO no PT ......................... 62
--     nome virado em "Quatro" ....................... 126  <- passivo JÁ existente
--   four minúsculo (numeral) ........................ 9
--
-- CUSTO ACEITO: a comparação ignora caixa, então uma proposta para fala cujo inglês diz
-- "four" numeral também é recusada e a fala fica como está. São ~5 casos identificados
-- ("Four units!", "Four minutes to atmospheric entry", "Four... three...", "Four o'clock",
-- "Four Marine Hizacks") e todos JÁ estão corretos no acervo — recusar uma proposta neles
-- significa manter o que já está bom, não estragar.
--
-- O QUE ESTA ENTRADA NÃO FAZ: não conserta as 126 já quebradas. Reparo é reescrita, e Paulo
-- vetou reescrever. Ela impede que NOVAS falas quebrem o nome.
--
-- EFEITO COLATERAL CONHECIDO — 🔴 ABERTO, e a decisão do Paulo é PELA tradução:
-- "Side Four" sai como "Lado Four", híbrido. A proteção MASCARA a palavra inteira antes de
-- mandar ao tradutor e não sabe distinguir a personagem do numeral do topônimo, então "Four"
-- volta intacto também dentro de "Side Four". Paulo, 17/08/2026: "lado quatro nao tem
-- problema ser traduzido" e "esse é o tipo de situação que merece tradução".
--
-- NÃO proteger "Side Four" é decisão dele — o desejado é "Lado Quatro", inteiro em português.
-- O que falta para chegar lá é MECANISMO, não configuração: uma exceção de contexto
-- ("Four protegido, exceto precedido de Side"). Medido no Zeta: 3 topônimos contra 188 usos
-- do nome. Enquanto a exceção não existir, o híbrido fica — é preferível a devolver as 188.
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Four');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Four Murasame');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Franklin Bidan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Fraw');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Fraw Bow');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'G-Defenser');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'GM II');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gabthley');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gabthleys');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gady');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Galbaldy');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Galbaldy Beta');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gaplant');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Garuda');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gate of Zedan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gates');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gates Capa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gaza-C');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Granada');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Green Noa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Green Oasis');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gryps');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gryps Conflict');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gundam');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gundam Mk-II');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Gwadan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Haman');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Haman Karn');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hambrabi');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hambrabis');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hamil');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Haro');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hasan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hathaway Noa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hayato');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hayato Kobayashi');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Henken');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Henken Bekkener');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hickory');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hilda Bidan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hizack');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hizacks');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hyaku');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Hyaku Shiki');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jaburo');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jamaican');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jamaican Daninghan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jamitov');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jamitov Hymen');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jerid');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jerid Messa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Jupitris');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kacricon');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kacricon Cacooler');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kai');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kai Shiden');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kamille');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kamille Bidan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Karaba');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Katz');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Katz Kobayashi');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Kilimanjaro');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Lila');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Lila Milla Rira');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Luio');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Luio Woomin');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Manack');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Marasai');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Marasais');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mega Particle Cannon');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Messala');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Methuss');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mineva');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mineva Lao Zabi');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Minovsky');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mirai');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mirai Yashima');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mobile Armor');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mobile Suit');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mont Blanc');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mouar');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Mouar Pharaoh');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Murasame');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Murasame Laboratory');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Namicar');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Namicar Cornell');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Nemo');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Nemos');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'New Hong Kong');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Newtype');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Newtypes');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Oldtype');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Oldtypes');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'One Year War');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Operation Apollo');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Operation Maelstrom');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Palace Athene');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Paptimus');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Paptimus Scirocco');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Psycho Gundam');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Psycho Gundam Mk-II');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Psycommu');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Quattro');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Quattro Bajeena');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Qubeley');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Qum');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Radish');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Ramsus');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Reccoa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Reccoa Londe');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Rick Dias');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Roberto');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Rosamia');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Rosamia Badam');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Rosammy');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Saegusa');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Sarah');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Sarah Zabiarov');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Scirocco');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Shangri-La');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Shinta');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Siddeley');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Sieg Zeon');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Spacenoid');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Spacenoids');  -- [03/09] usado na legenda, ausente da lore
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Stephanie Luio');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Sudori');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Super Gundam');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'The O');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Titans');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Torres');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Von Braun City');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'White Base');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Wong');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Wong Lee');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Yazan');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Yazan Gable');
INSERT INTO termo_protegido VALUES ('gundam_zeta', 'Zeta Gundam');
INSERT INTO par_inconfundivel VALUES ('gundam_zeta', 0, 'Char', 'Quattro');
INSERT INTO par_inconfundivel VALUES ('gundam_zeta', 1, 'Four', 'Quattro');
-- MEDIDO EM 2026-09-09 no acervo PUBLICADO: 24 falas com "Four" injetado onde o
-- original nao o tem, e o padrao dominante e "Fa!" -> "Four!". Fa Yuiry e Four
-- Murasame sao personagens DIFERENTES, e o espectador le o nome errado. O par
-- Four x Quattro ja existia e por isso Quattro era pego; Fa nao estava declarada.
INSERT INTO par_inconfundivel VALUES ('gundam_zeta', 2, 'Four', 'Fa');
INSERT INTO par_inconfundivel VALUES ('gundam_zeta', 3, 'Gundam Mk-II', 'Zeta Gundam');
-- ===== CICATRIZ MIGRADA de ContextoGundamZeta.java (2026-08-15) =====
-- Aliases usados centenas de vezes nas falas. Fa e Bright ficam apenas nas
-- formas completas: isolados colidem com palavras comuns e a proteção ignora caixa.
-- FOUR SAIU DESTA RESSALVA em 2026-08-17: a colisão foi MEDIDA e é de 9 falas (numeral
-- minúsculo) contra 188 do nome, e todas as 9 já estão corretas no acervo — recusar uma
-- proposta nelas mantém o que já está bom. Ver a justificativa completa em termosProtegidos.
-- Formas medidas nos 50 ASS de Zeta em 2026-08-09. O enforcer só aplica quando o
-- canônico da direita existe no EN, portanto palavras comuns como "Quem" e "Lote"
-- não são tocadas fora das falas que realmente contêm Qum e Batch.
-- FA YUIRY — minerado em 2026-08-04. A fala cujo texto INTEIRO e "Fa" aparece 51x
-- no acervo e o Zeta concentra 43 delas; so 8 preservaram o nome. O modelo le "Fa!"
-- como palavra comum: "Fogo!" 27x, "Fala!" 7x, "Fá..." 6x, "Pá!" 1x, "Fale!" 1x.
-- "Fa Yuiry" ja esta em termosProtegidos e nao adiantou — aquele conjunto isenta da
-- checagem de residuo, nao restaura grafia.
-- 
-- Seguro por construcao: so dispara com "Fa" no INGLES, e canonico de uma palavra e
-- comparado com SENSIBILIDADE A CAIXA. Colisao medida no acervo: zero legitima (as 6
-- encontradas sao o mesmo defeito em fala maior, e a regra as corrige).
-- Justificativa completa e risco residual em CorrecoesTerminologiaGundamZz.
-- ---------------------------------------------------------------------------
-- Formas-ruim MEDIDAS nas 16.778 falas do acervo (cache, 2026-07-30).
-- Quinta obra a receber o mapa; a de maior volume absoluto de perdas (433).
-- ---------------------------------------------------------------------------
-- ERRO SEMÂNTICO, não de grafia: 8 de 19. G-Defenser é a unidade de apoio;
-- Super Gundam é o Mk-II JÁ acoplado a ela. São coisas diferentes e ambas
-- existem na obra. Seguro porque ZERO falas trazem as duas no inglês.
-- "Gate of Zedan" perdido em 30 de 30 — é o nome que a A Baoa Qu recebeu.
-- 7 de 7. O mapa do 08th já trazia esta entrada; o Zeta não.
-- 4 de 6, nas duas formas que apareceram.
-- 4 de 4.
-- 2 de 5 — o mobile suit virou residência.
-- 2 de 8 — "Four" é o NOME da personagem (Four Murasame), não o número.
-- NÃO entram, e a medição é a razão:
--   A.E.U.G. (178 de 211) -- o PT escreve "AEUG" sem pontos, de forma
--     CONSISTENTE nas 178. É formatação, não erro de sentido, e mudá-la
--     reescreveria 178 falas publicadas. Decisão do Paulo, não minha.
--   Colony Laser (10 de 25) -- entrada seria INERTE: a legenda escreve
--     "colony Laser" com c minúsculo nas 24 ocorrências, e o enforcer exige o
--     canônico na grafia exata. Mesmo caso de Bio-Computer no F91.
--   Earth Federation (33 de 33) -- "Forças Federais da Terra", decisão de
--     produto consistente com CCA, F91, Unicorn e 08th.
--   Mobile Suit (116 de 301) -- plural ou omissão da fala inteira.
INSERT INTO traducao_obrigatoria VALUES ('gundam_zeta', 'Universal Century', 'Século Universal');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Armadura Móvel', 'Mobile Armor');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Armadura Normal', 'Normal Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Armaduras Móveis', 'Mobile Armors');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Armaduras Normais', 'Normal Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Brilhante', 'Bright');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Cem Estilos', 'Hyaku Shiki');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Conflito de Gryps', 'Gryps Conflict');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Cubely', 'Qubeley');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Dogosse Giar', 'Dogosse Gier');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Eixo', 'Axis');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Espacenoide', 'Spacenoid');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Espacenóide', 'Spacenoid');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Espada de Raio', 'Beam Saber');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Espadas de Raio', 'Beam Sabers');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Fala', 'Fa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Fale', 'Fa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Fogo', 'Fa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Fuzil de Feixe', 'Beam Rifle');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Fá', 'Fa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Grupo da Uniao Anti-Terra', 'AEUG');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Grupo da União Anti-Terra', 'AEUG');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Guerra de Um Ano', 'One Year War');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Gundam Mark II', 'Gundam Mk-II');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Gundam Mk II', 'Gundam Mk-II');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Incidente da Colonia 30', 'Colony 30 Incident');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Incidente da Colônia 30', 'Colony 30 Incident');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Laser de Colonia', 'Colony Laser');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Laser de Colônia', 'Colony Laser');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Lote', 'Batch');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Lâmina de Energia', 'Beam Saber');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Lâminas de Energia', 'Beam Sabers');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Mancack', 'Manack');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Mobil Suit', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móveis Suits', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móvel Suit', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móvel de Assalto', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móvel de Assento', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móvel de Combate', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Móvel de Guerra', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Neotipo', 'Newtype');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Neotipos', 'Newtypes');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Nova Hong Kong', 'New Hong Kong');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Novo Tipo', 'Newtype');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Novos Tipos', 'Newtypes');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'O O', 'The O');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Oasis Verde', 'Green Oasis');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Oásis Verde', 'Green Oasis');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Palacio Atena', 'Palace Athene');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Palácio Atena', 'Palace Athene');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Paraiso Verde', 'Green Oasis');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Paraíso Verde', 'Green Oasis');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Portao de Zedan', 'Gate of Zedan');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Portão de Zedan', 'Gate of Zedan');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Psico Gundam', 'Psycho Gundam');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Pá', 'Fa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Quatro', 'Quattro');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Quatro Murasame', 'Four Murasame');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Qubelei', 'Qubeley');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Quem', 'Qum');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Quim', 'Qum');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Ramus', 'Ramsus');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Rifle de Feixe', 'Beam Rifle');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Robô Móvel', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Robôs Móveis', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Roupa Normal', 'Normal Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Roupas Normais', 'Normal Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Sabre de Feixe', 'Beam Saber');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Sabre de Luz', 'Beam Saber');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Sabre de Raio', 'Beam Saber');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Sabres de Luz', 'Beam Sabers');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Suit Móvel', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Suits Móveis', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Super Gundam', 'G-Defenser');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terno Espacial Potenciado', 'Powered Spacesuit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terno Móvel', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terno Normal', 'Normal Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terno de Combate', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Ternos Móveis', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Ternos Normais', 'Normal Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Ternos de Combate', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terranoide', 'Earthnoid');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Terranóide', 'Earthnoid');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Titas', 'Titans');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Titãs', 'Titans');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Traje Espacial Potenciado', 'Powered Spacesuit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Traje Móvel', 'Mobile Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Traje Normal', 'Normal Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Trajes Móveis', 'Mobile Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Trajes Normais', 'Normal Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Uniao Anti-Terra', 'AEUG');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Uniforme Normal', 'Normal Suit');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Uniformes Normais', 'Normal Suits');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'União Anti-Terra', 'AEUG');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Velho Tipo', 'Oldtype');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Velhos Tipos', 'Oldtypes');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'Verde Noa', 'Green Noa');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'canhão de partículas megas', 'Mega Particle Cannon');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'cidade de Von Braun', 'Von Braun City');
INSERT INTO correcao_terminologia VALUES ('gundam_zeta', 'mega canhão de partículas', 'Mega Particle Cannon');
INSERT INTO lore_traducao VALUES ('gundam_zeta', 'Mobile Suit Zeta Gundam', 1,
'Você é um tradutor especializado em legendas de anime, traduzindo do inglês para português do Brasil.
Contexto ativo da obra: Mobile Suit Zeta Gundam.

Prioridades de tradução:
- Preserve sentido, subtexto, intenção emocional e continuidade da cena.
- Use português brasileiro natural, fluido e adequado à legenda, sem ficar literal quando isso soar estranho.
- Mantenha nomes próprios, nomes de mecha, naves, facções, cidades, organizações, patentes e codinomes conforme a lore abaixo.
- "Psyco Gundam" é um mobile armor gigante e o nome se escreve "Psyco Gundam" em português também: "Psyco Gundam?" fica "Psyco Gundam?", "As you wish, but I''ll entrust the Psyco Gundam to you." fica "Como quiser, mas vou confiar o Psyco Gundam a você.", "You must get out of the Psyco Gundam''s cockpit! Hurry!" fica "Você precisa sair do cockpit do Psyco Gundam! Rápido!".
- Não invente explicações, notas, parênteses editoriais ou glossários na resposta.
- Preserve honoríficos japoneses somente quando vierem no texto original ou forem parte clara da relação entre personagens.
- Em falas militares, use tom objetivo e terminologia consistente: unidade, esquadrão, frota, comandante, tenente, capitão/capitão apenas quando o original indicar rank equivalente.
- Posição dada em horas de relógio indica DIREÇÃO relativa ao veículo, não hora do dia: preserve o número e a leitura de direção, e preserve a indicação de altura quando houver. Traduza como hora do dia somente quando o original estiver falando de horário.
- A palavra "Heading" seguida de número é a PROA: o rumo de navegação do veículo. Preserve o número e traduza como indicação de rumo.

Lore e terminologia obrigatória:
- Obra: Mobile Suit Zeta Gundam (TV) — Universal Century U.C. 0087, Gryps Conflict.
- Premissa: AEUG vs Titans; Axis Zeon (Haman Karn / Mineva); Cyber-Newtypes;
  Kamille Bidan e o MSZ-006 Zeta Gundam. Tom militar/politico sombrio, trauma,
  abuso de autoridade. Evitar gírias modernas.

=== Núcleo UC ===
- Newtype (NUNCA Novo Tipo); Cyber-Newtype; Oldtype; Psycommu / psycommu;
  Minovsky particles; Spacenoid vs Earthnoid.
- Mobile Suit vs Mobile Armor; Beam Rifle / Beam Saber; Mega Particle Cannon.
- Earth Federation / Federation Forces; One Year War (legado).

=== Facções (NUNCA fundir / NUNCA mitologizar) ===
- A.E.U.G. / AEUG (Anti-Earth Union Group) — preservar pontos quando o EN trouxer A.E.U.G.
- Titans (NUNCA Titãs); Karaba (Terra); Anaheim Electronics.
- Axis / Axis Zeon (NUNCA Eixo) — Haman Karn como regente de Mineva Lao Zabi.

=== Roster — AEUG / Argama / Karaba ===
- Kamille Bidan (m — pronomes masculinos; piada de confusão de gênero);
  Quattro Bajeena / Char Aznable (m); Bright Noa (m);
  Emma Sheen (f); Fa Yuiry (f); Reccoa Londe (f — defeita aos Titans depois);
  Katz Kobayashi (m); Henken Bekkener (m); Astonaige Medoz (m);
  Apolly Bay (m); Roberto (m); Torres (m); Wong Lee (m).
- Amuro Ray (m); Hayato Kobayashi (m); Mirai Yashima (f); Hathaway Noa (m, criança);
  Franklin Bidan (m); Hilda Bidan (f); Beltorchika Irma (f) quando aparecer.
- Blex Forer; Dr. Hasan; Kai Shiden; Fraw Bow; Shinta; Qum; Haro;
  Luio Woomin; Stephanie Luio; Ben Wooder; Namicar Cornell.

=== Roster — Titans / Scirocco ===
- Jerid Messa (m); Bask Om (m); Jamitov Hymen (m); Jamaican Daninghan (m);
  Paptimus Scirocco (m); Yazan Gable (m); Buran Blutarch (m);
  Lila Milla Rira (f); Mouar Pharaoh (f); Sarah Zabiarov (f);
  Kacricon Cacooler (m); Gates Capa (m) quando aparecerem.
- Tripulantes e pilotos recorrentes: Gady; Siddeley; Batch; Saegusa; Ramsus;
  Botty; Manack; Hamil; Addis; Dava Baro.

=== Roster — Cyber-Newtype / Axis ===
- Four Murasame (f); Rosamia Badam (f); Haman Karn (f); Mineva Lao Zabi (f, criança).
- Rosammy e o apelido usado por Kamille para Rosamia. Preserve exatamente Rosammy quando
  o original usar Rosammy; não normalize o apelido para Rosamia.

=== Naves / lugares / eventos ===
- Naves: Argama; Mont Blanc; Radish; Alexandria; Audhumla (Karaba); Jupitris;
  Gwadan; Dogosse Gier; Bosnia; Sudori; Garuda; White Base.
- Lugares: Gryps / Gate of Zedan; Jaburo; Hong Kong; Dakar; Kilimanjaro; Axis;
  Shangri-La; Luna II; Granada; Von Braun City; Green Oasis; Green Noa; Hickory;
  Amman; New Hong Kong; Murasame Laboratory.
- Eventos: Gryps Conflict; Colony 30 Incident (background); Colony Laser;
  Dakar Speech (Quattro); Operation Apollo; Operation Maelstrom.

=== Mecha ===
- AEUG/Anaheim: MSZ-006 Zeta Gundam; RX-178 Gundam Mk-II / Super Gundam (G-Defenser);
  MSN-00100 Hyaku Shiki (NUNCA Cem Estilos); Rick Dias; Methuss; Nemo; GM II; Dijeh (Amuro).
- Titans: Hizack; Marasai; Barzam; Gaplant; Gabthley; Hambrabi; Palace Athene;
  Byarlant; Asshimar; Galbaldy Beta; Messala; Baund Doc; The O (NUNCA reduzir a O);
  Psycho Gundam / Psycho Gundam Mk-II.
- Axis: Qubeley (Haman); Gaza-C quando aparecer.

=== Três superfícies parecidas, três coisas diferentes ===
- Four Murasame (f), a Cyber-Newtype do Psycho Gundam. "Four" é o NOME dela e se escreve "Four" em português também: "Four!" fica "Four!", "Four, can you run?" fica "Four, você consegue correr?", "Open your eyes, Four!" fica "Abra os olhos, Four!". Só é numeral quando vier antes de substantivo contável em inglês ("four units").
- Original "Quattro" -> saída "Quattro".  Personagem: Quattro Bajeena.
- Não troque o token do original pelo nome de OUTRO personagem.

=== Char e Quattro: a identidade oculta e o eixo da obra ===
- Original "Char"    -> saída "Char".
- Original "Quattro" -> saída "Quattro".
- Nunca "Quattro Aznable" nem "Char Bajeena": esses nomes NÃO existem.
- Char Aznable se apresenta como Quattro Bajeena, e a obra esconde isso DE PROPÓSITO.
  Quem pergunta "do you know of a man by the name of Char Aznable?" esta perguntando
  pela identidade OCULTA — trocar por "Quattro Bajeena" entrega o segredo e destrói
  a cena. Escreva o nome que o original escreveu, sempre, sem resolver a identidade.

=== Regras duras ===
- Titans não vira Titãs; Axis não vira Eixo; Hyaku Shiki não vira Cem Estilos;
  The O não vira O; Newtype não vira Novo Tipo.
- Kamille masculino; Quattro estratégico; Titans autoritários; Scirocco manipulador;
  Haman fria/regente Axis.

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
INSERT INTO lore_revisao VALUES ('gundam_zeta', 'Mobile Suit Zeta Gundam - Revisao de Lore',
'Voce e revisor especializado em legendas de anime/filme, focado em TERMINOLOGIA E LORE.
Corrija APENAS nomes proprios, locais, organizacoes, mechas, titulos, apelidos e termos de mundo
que estejam fora do padrao oficial da obra. NAO reescreva a fala inteira nem mude concordancia de genero
a menos que um nome proprio exija artigo/pronome coerente.

Use a lore abaixo como fonte canonica de grafia e padrao:
- Obra: Mobile Suit Zeta Gundam, Universal Century 0087, Gryps Conflict.
- Papel: corrigir APENAS nomenclatura. Nomes/mechas/naves/faccoes NAO sao localizados.

=== Faccoes ===
- A.E.U.G. / AEUG (Anti-Earth Union Group) — preservar pontos quando houver A.E.U.G.
- Titans (NUNCA Titãs); Karaba; Anaheim Electronics; Earth Federation.
- Axis / Axis Zeon (NUNCA Eixo) — Haman Karn / Mineva Lao Zabi.

=== Roster — AEUG / Argama / Karaba ===
- Kamille Bidan (m); Quattro Bajeena / Char Aznable (NUNCA Quatro); Bright Noa;
  Emma Sheen; Fa Yuiry; Reccoa Londe; Katz Kobayashi; Henken Bekkener;
  Astonaige Medoz; Apolly Bay; Roberto; Torres; Wong Lee;
  Amuro Ray; Hayato Kobayashi; Mirai Yashima; Hathaway Noa;
  Franklin Bidan; Hilda Bidan; Beltorchika Irma.
- Blex Forer; Dr. Hasan; Kai Shiden; Fraw Bow; Shinta; Qum; Haro;
  Luio Woomin; Stephanie Luio; Ben Wooder; Namicar Cornell.

=== Roster — Titans / Scirocco ===
- Jerid Messa; Bask Om; Jamitov Hymen; Jamaican Daninghan; Paptimus Scirocco;
  Yazan Gable; Buran Blutarch; Lila Milla Rira; Mouar Pharaoh; Sarah Zabiarov;
  Kacricon Cacooler; Gates Capa.
- Tripulantes e pilotos recorrentes: Gady; Siddeley; Batch; Saegusa; Ramsus;
  Botty; Manack; Hamil; Addis; Dava Baro.

=== Roster — Cyber-Newtype / Axis ===
- Four Murasame; Rosamia Badam; Haman Karn; Mineva Lao Zabi.
- Rosammy e o apelido usado por Kamille para Rosamia. Preserve exatamente Rosammy quando
  o original usar Rosammy; nao normalize o apelido para Rosamia.

=== Naves / lugares / eventos ===
- Argama; Mont Blanc; Radish; Alexandria; Audhumla; Jupitris; Gwadan;
  Dogosse Gier; Bosnia; Sudori; Garuda; White Base.
- Gryps / Gate of Zedan; Jaburo; Dakar; Kilimanjaro; Axis; Shangri-La;
  Granada; Von Braun City; Green Oasis; Green Noa; Hickory; Amman;
  New Hong Kong; Murasame Laboratory.
- Gryps Conflict; Colony Laser; Colony 30 Incident; Dakar Speech;
  Operation Apollo; Operation Maelstrom.

=== Mecha ===
- Zeta Gundam; Gundam Mk-II / Super Gundam / G-Defenser; Hyaku Shiki (NUNCA Cem Estilos);
  Rick Dias; Methuss; Nemo; Dijeh; Hizack; Marasai; Gaplant; Gabthley; Hambrabi;
  Palace Athene; Byarlant; Messala; Baund Doc; The O (NUNCA reduzir a O);
  Psycho Gundam / Psycho Gundam Mk-II; Qubeley; Gaza-C.

=== Formas-ruim (restaurar) ===
- Titans nao vira Titãs; Quattro nao vira Quatro; Axis nao vira Eixo;
  Hyaku Shiki nao vira Cem Estilos; The O nao vira O; Newtype nao vira Novo Tipo.
- Titãs/Titas → Titans; Quatro → Quattro; Eixo → Axis;
  Cem Estilos → Hyaku Shiki; O O → The O;
  União Anti-Terra → AEUG; Conflito de Gryps → Gryps Conflict;
  Laser de Colônia → Colony Laser; Psico Gundam → Psycho Gundam;
  Cubely → Qubeley; Gundam Mark II → Gundam Mk-II.

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
-- Traducao CORRETA que a tela vinha acusando. Declarar equivalencia CALA a
-- acusacao e NAO escreve legenda — e o oposto de correcoesTerminologia.
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'a.e.u.g', 0, 'aeug');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'a.e.u.g', 1, 'a.e.u.g.');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'earth sphere', 0, 'esfera terrestre');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'earth sphere', 1, 'esfera terrestre');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'psyco gundam', 0, 'psycho gundam');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'zabi family', 0, 'família zabi');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'zabi family', 1, 'familia zabi');
INSERT INTO equivalencia_aceita VALUES ('gundam_zeta', 'zabi family', 2, 'família zabi');
