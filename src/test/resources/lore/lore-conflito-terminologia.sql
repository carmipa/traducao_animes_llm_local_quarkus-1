-- CASO DOENTE, montado a mao: a MESMA forma-ruim apontando para canonicos DIFERENTES. Com a lore
-- em SQL a terminologia e UMA tabela por obra, entao o conflito vira violacao de chave primaria — e o
-- carregamento tem de falhar FECHADO mostrando os dois canonicos, em vez de um vencer em silencio.
--
-- Nao e lore de obra real e nunca e lido em producao: so a catraca de terminologia aponta para ca.
INSERT INTO obra VALUES ('obra_de_teste');
INSERT INTO correcao_terminologia VALUES ('obra_de_teste', 'Lanca-Flanco', 'Spearhead');
INSERT INTO correcao_terminologia VALUES ('obra_de_teste', 'Lanca-Flanco', 'Ponta de Lanca');
INSERT INTO lore_traducao VALUES ('obra_de_teste', 'Obra de Teste', 1, 'prompt qualquer');
INSERT INTO lore_revisao VALUES ('obra_de_teste', 'Obra de Teste - Revisao de Lore', 'prompt de revisao qualquer');
