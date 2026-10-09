-- CONSULTAS DE AUDITORIA DA LORE
--
-- Primeiro monte o banco:   pwsh -File ferramentas/lore-db.ps1
-- Depois rode tudo:          sqlite3 -readonly build/lore/lore.db ".read ferramentas/lore-consultas.sql"
-- Ou copie a consulta que interessa e troque os valores de .parameter set.
--
-- ATENCAO ao lado da terminologia: em correcao_terminologia, forma_ruim e o que o modelo ERRA
-- ("Traje Movel") e canonico e o que deve ficar ("Mobile Suit"). Procurar o termo certo na coluna
-- errada devolve zero — e zero aqui significa "procurei no lado errado", nao "nenhuma obra usa".

.headers on
.mode box
.parameter set @termo 'Gauna'
.parameter set @canonico 'Mobile Suit'
.parameter set @obra_a 'gundam_zeta'
.parameter set @obra_b 'gundam_zz'

SELECT texto AS aviso FROM aviso;

-- 1. Quem protege um termo (nome que nao pode ser traduzido)
SELECT obra_id FROM termo_protegido WHERE termo = @termo ORDER BY obra_id;

-- 2. Em quantas obras esta a regra que leva a um canonico, e por quais formas erradas
SELECT canonico, count(DISTINCT obra_id) AS obras, count(*) AS regras,
       group_concat(DISTINCT forma_ruim) AS formas_erradas
FROM correcao_terminologia WHERE canonico = @canonico GROUP BY canonico;

-- 3. Resumo por obra
SELECT o.id AS obra,
       (SELECT count(*) FROM termo_protegido t WHERE t.obra_id = o.id) AS termos,
       (SELECT count(*) FROM correcao_terminologia c WHERE c.obra_id = o.id) AS correcoes,
       (SELECT count(*) FROM par_inconfundivel p WHERE p.obra_id = o.id) AS pares,
       (SELECT count(*) FROM apelido_pasta a WHERE a.obra_id = o.id) AS apelidos,
       (SELECT count(*) FROM equivalencia_aceita e WHERE e.obra_id = o.id) AS equivalencias,
       EXISTS (SELECT 1 FROM lore_traducao lt WHERE lt.obra_id = o.id) AS tem_traducao,
       EXISTS (SELECT 1 FROM lore_revisao lr WHERE lr.obra_id = o.id) AS tem_revisao
FROM obra o ORDER BY o.id;

-- 4. Obras sem par inconfundivel declarado
SELECT id AS obra_sem_par FROM obra WHERE id NOT IN (SELECT obra_id FROM par_inconfundivel) ORDER BY id;

-- 5. Diferenca de terminologia entre duas obras (o que uma tem e a outra nao)
SELECT 'so em ' || @obra_a AS lado, forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = @obra_a
EXCEPT SELECT 'so em ' || @obra_a, forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = @obra_b
UNION ALL
SELECT 'so em ' || @obra_b, forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = @obra_b
EXCEPT SELECT 'so em ' || @obra_b, forma_ruim, canonico FROM correcao_terminologia WHERE obra_id = @obra_a
ORDER BY 1, 2;

-- 6. Mesma forma errada levando a canonicos diferentes em obras diferentes (pode ser legitimo:
--    a mesma palavra e outra coisa em outra franquia; vale olhar)
SELECT forma_ruim, group_concat(DISTINCT canonico) AS canonicos, count(DISTINCT obra_id) AS obras
FROM correcao_terminologia GROUP BY forma_ruim HAVING count(DISTINCT canonico) > 1 ORDER BY forma_ruim;
