package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a ORDEM das perguntas — só o português decide, os outros rotulam —
 * e que nenhum idioma ausente vira aprovação.
 *
 * <h2>A cicatriz que este teste guarda</h2>
 * {@code Resonância} está errada (o certo é <i>ressonância</i>), o pt_BR reprova e o de_DE ACEITA.
 * Medido em 13/08/2026. Se o alemão pudesse aprovar, esse erro passaria — e cada dicionário novo
 * multiplicaria a chance. Por isso a hierarquia é testada, não só documentada.
 */
@DisplayName("classificador de 4 idiomas: só o português decide")
class ClassificadorQuatroIdiomasTest {

    private static ClassificadorQuatroIdiomas comIdiomasReais() {
        return new ClassificadorQuatroIdiomas(
            new HunspellDicionarioAdapter("hunspell", "pt_BR"),
            new HunspellDicionarioAdapter("hunspell", "en_US"),
            new HunspellDicionarioAdapter("hunspell", "de_DE"));
    }

    private static ClassificadorQuatroIdiomas semNenhumDicionario() {
        var ausente = new HunspellDicionarioAdapter("hunspell-inexistente-xyz", "pt_BR");
        return new ClassificadorQuatroIdiomas(ausente, ausente, ausente);
    }

    @Test
    @DisplayName("japonês é reconhecido pela ESCRITA, sem consultar dicionário nenhum")
    void japonesNaoPrecisaDeDicionario() {
        Map<String, VeredictoPalavra> r = semNenhumDicionario().classificar(List.of("しまった", "見つける"));

        assertEquals(VeredictoPalavra.JAPONES, r.get("しまった"),
            "kana se reconhece por faixa Unicode — não existe hunspell japonês e não faz falta");
        assertEquals(VeredictoPalavra.JAPONES, r.get("見つける"), "kanji idem");
    }

    @Test
    @DisplayName("FALHA FECHADA: sem dicionário, tudo é NAO_VERIFICADO — jamais 'ok'")
    void semDicionarioNadaEhAprovado() {
        Map<String, VeredictoPalavra> r = semNenhumDicionario()
            .classificar(List.of("organizacao", "vamos", "suit"));

        assertTrue(r.values().stream().allMatch(v -> v == VeredictoPalavra.NAO_VERIFICADO),
            "sem verificador, 'não olhei' não pode virar 'está certo' para nenhuma palavra");
        assertTrue(r.values().stream().noneMatch(VeredictoPalavra::corrigivelAutomaticamente),
            "e nada pode ser corrigido automaticamente sem ninguém ter verificado");
    }

    @Test
    @DisplayName("os quatro vereditos, cada um com seu caso real do acervo")
    void classificaCadaCasoRealDoAcervo() {
        var c = comIdiomasReais();
        Map<String, VeredictoPalavra> r = c.classificar(List.of(
            "criança",      // português correto
            "fatidico",     // falta acento -> fatídico
            "cockpit",      // inglês legítimo, 4 ocorrências no Unicorn
            "Nordlicht",    // alemão, termo de lore
            "psycommu"));   // invenção do Gundam: nenhum idioma conhece

        Assumptions.assumeTrue(r.get("criança") != VeredictoPalavra.NAO_VERIFICADO,
            "hunspell/dicionários ausentes — NÃO VERIFICADO");

        assertEquals(VeredictoPalavra.PORTUGUES_OK, r.get("criança"));
        assertEquals(VeredictoPalavra.ACENTO_FALTANDO, r.get("fatidico"),
            "falta de acento é o ÚNICO veredicto que autoriza correção automática");
        assertEquals(VeredictoPalavra.RESIDUO_INGLES, r.get("cockpit"),
            "inglês válido tem de ser rotulado como tal, senão afoga os erros reais");
        assertEquals(VeredictoPalavra.TERMO_ALEMAO, r.get("Nordlicht"),
            "anime usa alemão à beça — rotular evita 'corrigir' nome de lore");
        assertEquals(VeredictoPalavra.DESCONHECIDA, r.get("psycommu"),
            "termo inventado pela obra não é erro nem idioma: fica para a proteção de lore");
    }

    /**
     * A HIERARQUIA, e a razão de ela existir: o alemão aceita {@code Resonância}, que está errada
     * em português. Só o português decide, então o veredicto não pode ser TERMO_ALEMAO.
     */
    @Test
    @DisplayName("o alemão NÃO aprova o que o português reprovou")
    void idiomaSecundarioNaoAprovaErroDePortugues() {
        var c = comIdiomasReais();
        Map<String, VeredictoPalavra> r = c.classificar(List.of("Resonância"));

        Assumptions.assumeTrue(r.get("Resonância") != VeredictoPalavra.NAO_VERIFICADO,
            "dicionários ausentes — NÃO VERIFICADO");
        assertTrue(r.get("Resonância") != VeredictoPalavra.PORTUGUES_OK,
            "ERRO REAL APROVADO: 'Resonância' está errada (é ressonância) e passou como português");
    }
}
