package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: subfase D0 do Plano-Mestre da correção de gênero por contexto de
 * cena — caracteriza o comportamento ATUAL (a "régua" {@link VerificadorPropriedadeGenero}
 * e a inversão real do conjunto-ouro {@link GoldSet08thMSTeam}), estabelecendo a linha-base
 * verde do futuro A/B. Prova, sem LLM e sem tocar produção/cache/telemetria, que: (a) a
 * régua classifica flexão de gênero corretamente; (b) toda baseline do 08th é uma inversão
 * de fato detectada pela régua; (c) a formulação neutra e a flexão correta seriam ACEITAS —
 * o critério de sucesso da correção vindoura.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro (sem CDI/@QuarkusTest); termina VERDE (D0 fixa o
 * estado atual, não corrige nada); nenhuma asserção depende de rede ou do modelo local.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio da régua ou do gold reprova, sinalizando
 * regressão do instrumento de medição — não do pipeline de tradução.
 */
class ConcordanciaGeneroGoldSetTest {

    @Test
    @DisplayName("regua: detecta flexao feminina, masculina, neutra e ambigua por palavra inteira")
    void reguaClassificaFlexao() {
        assertEquals(GeneroFlexao.FEMININO, VerificadorPropriedadeGenero.detectar("Estou certa..."));
        assertEquals(GeneroFlexao.MASCULINO, VerificadorPropriedadeGenero.detectar("Estou certo."));
        assertEquals(GeneroFlexao.FEMININO, VerificadorPropriedadeGenero.detectar("Obrigada, Norris."));
        assertEquals(GeneroFlexao.MASCULINO, VerificadorPropriedadeGenero.detectar("Obrigado, Norris."));
        // "certeza"/"certamente" NAO sao a forma marcada "certa"/"certo": neutro.
        assertEquals(GeneroFlexao.NEUTRO, VerificadorPropriedadeGenero.detectar("Tenho certeza disso."));
        assertEquals(GeneroFlexao.NEUTRO, VerificadorPropriedadeGenero.detectar("Ele fez isso certamente."));
        // Marcas dos dois generos na mesma fala: ambiguo (caso de revisao, nunca violacao simples).
        assertEquals(GeneroFlexao.AMBIGUO,
            VerificadorPropriedadeGenero.detectar("Estou cansado e ela esta cansada."));
        // Sem adjetivo/participio marcado: neutro. (Evita "Tudo pronto?": "pronto" e forma
        // masculina marcada, entao a regua lexical acusa MASCULINO — limitacao conhecida do v0.)
        assertEquals(GeneroFlexao.NEUTRO, VerificadorPropriedadeGenero.detectar("Nao aguento mais isso."));
    }

    @Test
    @DisplayName("regua: null/branco nao lanca e conta como neutro")
    void reguaToleraVazio() {
        assertEquals(GeneroFlexao.NEUTRO, VerificadorPropriedadeGenero.detectar(null));
        assertEquals(GeneroFlexao.NEUTRO, VerificadorPropriedadeGenero.detectar("   "));
    }

    @Test
    @DisplayName("regua: violacao so quando a flexao e o oposto exato do esperado; neutro nao viola")
    void violacaoSoNoOpostoExato() {
        assertTrue(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.MASCULINO, "Estou certa..."));
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.MASCULINO, "Estou certo."));
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.MASCULINO, "Tenho certeza disso."));
        assertTrue(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.FEMININO, "Obrigado, Norris."));
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.FEMININO, "Obrigada, Norris."));
        // Esperado sem genero definido (NEUTRO) nunca acusa violacao.
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.NEUTRO, "Estou certa..."));
    }

    @Test
    @DisplayName("gold 08th: bem formado (nao vazio, genero esperado marcado, campos preenchidos)")
    void goldBemFormado() {
        List<GoldCaseConcordancia> casos = GoldSet08thMSTeam.casos();
        assertFalse(casos.isEmpty(), "o conjunto-ouro do 08th nao pode ser vazio");
        for (GoldCaseConcordancia c : casos) {
            assertTrue(c.generoEsperado() == GeneroFlexao.MASCULINO
                    || c.generoEsperado() == GeneroFlexao.FEMININO,
                "genero esperado deve ser M ou F em: " + c.originalEn());
            assertFalse(c.originalEn().isBlank(), "original em ingles nao pode ser vazio");
            assertFalse(c.personagemReferido().isBlank(), "personagem referido nao pode ser vazio");
            assertFalse(c.baselineAtual().isBlank(), "baseline observada nao pode ser vazia");
        }
    }

    @Test
    @DisplayName("gold 08th: caracterizacao — TODA baseline atual e uma inversao real detectada pela regua")
    void baselineAtualEInversaoReal() {
        for (GoldCaseConcordancia c : GoldSet08thMSTeam.casos()) {
            assertTrue(VerificadorPropriedadeGenero.violaEsperado(c.generoEsperado(), c.baselineAtual()),
                "esperava inversao de genero em '" + c.baselineAtual()
                    + "' (esperado " + c.generoEsperado() + ") para " + c.personagemReferido());
        }
    }

    @Test
    @DisplayName("gold 08th: criterio de sucesso — formulacao neutra e flexao correta SERIAM aceitas")
    void neutroECorretoSeriamAceitos() {
        // O que a correcao vindoura deve produzir para o caso "I'm sure..." do Shiro (m):
        // uma formulacao neutra ("Tenho certeza") nao viola o masculino esperado.
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.MASCULINO, "Tenho certeza..."));
        // E a flexao masculina correta tambem passa.
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.MASCULINO, "Tenho certeza. Estou certo."));
        // Para Aina (f), "Obrigada" (flexao feminina correta) passa; hoje sai "Obrigado" e reprova.
        assertFalse(VerificadorPropriedadeGenero.violaEsperado(GeneroFlexao.FEMININO, "Obrigada, Norris."));
    }
}
