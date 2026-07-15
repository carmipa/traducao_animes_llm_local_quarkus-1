package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: reproduz as propostas reais que a opção 7 deve aceitar
 * ou bloquear antes de sobrescrever uma legenda.
 * <p>INVARIANTES DO DOMÍNIO: somente termo presente no EN e na lore pode mudar.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova a suíte.
 */
class ValidadorCandidatoLoreServiceTest {

    private static final String LORE = """
        Personagens: Jona Basta, Michele Luio.
        Faccoes: Titans, Luio & Co., Shezarr Unit.
        Mechas: Unicorn Gundam, Phenex.
        Termos: psycho-frame, NT-D, Operation Phoenix Hunt.
        """;

    /**
     * PROPÓSITO DE NEGÓCIO: permite restaurar o nome oficial de uma unidade.
     * <p>INVARIANTES DO DOMÍNIO: Phenex existe no EN e na lore.
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeição inesperada reprova o teste.
     */
    @Test
    void aceitaTrocaLocalPorTermoCanonicoComprovado() {
        assertTrue(ValidadorCandidatoLoreService.validar(
            "Our mission is to capture the elusive Phenex.",
            "Nossa missão é capturar este Phoenix.",
            "Nossa missão é capturar este Phenex.",
            LORE
        ).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite recuperar nome omitido sem reescrever fala.
     * <p>INVARIANTES DO DOMÍNIO: Jona aparece nas duas fontes confiáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeição inesperada reprova o teste.
     */
    @Test
    void aceitaInsercaoPequenaDeNomePresenteNoOriginal() {
        assertTrue(ValidadorCandidatoLoreService.validar(
            "Jona! Michele!", "Michele!", "Jona! Michele!", LORE
        ).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia tradução destrutiva de tecnologia oficial.
     * <p>INVARIANTES DO DOMÍNIO: quadro psicológico não é termo da lore nem EN.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação insegura reprova o teste.
     */
    @Test
    void bloqueiaPsychoFrameCorrompido() {
        assertFalse(ValidadorCandidatoLoreService.validar(
            "You are using a psycho-frame.",
            "Você está usando um psycho-frame.",
            "Você está usando um quadro psicológico.",
            LORE
        ).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que a opção de lore converta patente já
     * localizada para inglês.
     * <p>INVARIANTES DO DOMÍNIO: Ensign não consta da lore ativa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação insegura reprova o teste.
     */
    @Test
    void bloqueiaPatenteEmInglesForaDaLore() {
        assertFalse(ValidadorCandidatoLoreService.validar(
            "Ensign Jona!", "Alferes Jona!", "Ensign Jona!", LORE
        ).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede alteração estilística sem relação com lore.
     * <p>INVARIANTES DO DOMÍNIO: universo não é termo canônico do recorte.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação insegura reprova o teste.
     */
    @Test
    void bloqueiaTrocaDeSinonimo() {
        assertFalse(ValidadorCandidatoLoreService.validar(
            "Fly through the cosmos!", "Voe pelo cosmos!", "Voe pelo universo!", LORE
        ).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia retradução completa motivada por um nome.
     * <p>INVARIANTES DO DOMÍNIO: mais de quatro tokens alterados é escopo amplo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação insegura reprova o teste.
     */
    @Test
    void bloqueiaReescritaAmplaDaFala() {
        assertFalse(ValidadorCandidatoLoreService.validar(
            "Is there a Gundam at point Oscar?",
            "Eu sei disso.",
            "Há um Gundam no ponto designado Oscar?",
            LORE
        ).isEmpty());
    }
}
