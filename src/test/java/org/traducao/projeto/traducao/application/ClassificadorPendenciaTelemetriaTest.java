package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.domain.CategoriaConteudo;
import org.traducao.projeto.traducao.domain.CausaRaizPendencia;
import org.traducao.projeto.traducao.domain.ResumoPendencia;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a classificação estruturada de pendências que
 * alimenta o KPI da telemetria — causa-raiz (com precedência), balde de conteúdo e
 * consolidação —, travando o comportamento contra o texto livre ambíguo anterior.
 * <p>INVARIANTES DO DOMÍNIO: marcador corrompido vence eco; o balde vem do detector de
 * karaokê real; a consolidação soma falas por (categoria, causa).
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer classificação divergente reprova o teste.
 */
class ClassificadorPendenciaTelemetriaTest {

    private final ClassificadorPendenciaTelemetria classificador =
        new ClassificadorPendenciaTelemetria(new DetectorEfeitoKaraokeService());

    @Test
    void precedenciaMarcadoresVenceEco() {
        // O caso do overlap 2167 em 86: tag corrompida devolve o original e seria
        // contada também como eco. A causa-raiz real é o marcador corrompido.
        assertEquals(CausaRaizPendencia.MARCADORES_CORROMPIDOS,
            CausaRaizPendencia.maisGrave(CausaRaizPendencia.MARCADORES_CORROMPIDOS, CausaRaizPendencia.ECO));
        assertEquals(CausaRaizPendencia.MARCADORES_CORROMPIDOS,
            CausaRaizPendencia.maisGrave(CausaRaizPendencia.ECO, CausaRaizPendencia.MARCADORES_CORROMPIDOS));
        assertEquals(CausaRaizPendencia.ECO,
            CausaRaizPendencia.maisGrave(CausaRaizPendencia.ECO, null));
    }

    @Test
    void mapeiaMotivoFinalParaCausaRaiz() {
        assertEquals(CausaRaizPendencia.ECO,
            classificador.causaDeMotivoFinal("modelo devolveu o texto original sem tradução"));
        assertEquals(CausaRaizPendencia.ESTRUTURA_DIVERGENTE,
            classificador.causaDeMotivoFinal("tags ASS/SSA ou quebras de linha divergentes do original"));
        assertEquals(CausaRaizPendencia.VAZIA,
            classificador.causaDeMotivoFinal("resposta vazia"));
        assertEquals(CausaRaizPendencia.RESIDUO,
            classificador.causaDeMotivoFinal("Resíduo gringo detectado: The Legion?"));
        assertEquals(CausaRaizPendencia.ECO, classificador.causaDeMotivoFinal(null));
    }

    @Test
    void classificaBaldeDeConteudoPeloDetectorReal() {
        // Romaji preservado (86: topo MarginV=0)
        assertEquals(CategoriaConteudo.ROMAJI_PRESERVADO,
            classificador.categoria("Opening", "fuminijirareru dake no hana"));
        // Glosa inglesa traduzível (86: base MarginV=910)
        assertEquals(CategoriaConteudo.MUSICA_LATINA,
            classificador.categoria("Opening", "A flower blooms only to be crushed"));
        // Efeito KFX cru
        assertEquals(CategoriaConteudo.KFX,
            classificador.categoria("Opening", "{\\k30}fu"));
        // Diálogo comum
        assertEquals(CategoriaConteudo.DIALOGO,
            classificador.categoria("Default", "Bom dia, major."));
        // Letreiro
        assertEquals(CategoriaConteudo.LETREIRO,
            classificador.categoria("Signs", "Gran Mur"));
    }

    @Test
    void consolidaSomandoPorCategoriaECausa() {
        List<ResumoPendencia> unitarios = List.of(
            new ResumoPendencia("DIALOGO", "ECO", 1),
            new ResumoPendencia("DIALOGO", "ECO", 1),
            new ResumoPendencia("DIALOGO", "MARCADORES_CORROMPIDOS", 1),
            new ResumoPendencia("MUSICA_LATINA", "MARCADORES_CORROMPIDOS", 1));
        List<ResumoPendencia> resumo = classificador.consolidar(unitarios);
        assertEquals(3, resumo.size());
        assertEquals(2, resumo.stream()
            .filter(r -> r.categoria().equals("DIALOGO") && r.causaRaiz().equals("ECO"))
            .findFirst().orElseThrow().quantidade());
        assertEquals(1, resumo.stream()
            .filter(r -> r.categoria().equals("MUSICA_LATINA") && r.causaRaiz().equals("MARCADORES_CORROMPIDOS"))
            .findFirst().orElseThrow().quantidade());
    }

    @Test
    void consolidaListaVaziaDevolveVazia() {
        assertEquals(List.of(), classificador.consolidar(List.of()));
        assertEquals(List.of(), classificador.consolidar(null));
    }
}
