package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.c;
import static org.traducao.projeto.telemetria.FixtureCaminhoWindows.marcadorDrive;

/**
 * PROPÓSITO DE NEGÓCIO: prova que toda operação registrada por qualquer fatia
 * chega ao fluxo AO VIVO — na fatia certa e já sanitizada — e que a ausência do
 * fluxo não afeta nada.
 *
 * <h2>Por que este ponto</h2>
 * Treze fatias chamam {@code registrarOperacao}. Testar aqui cobre as treze; a
 * alternativa seria treze testes iguais em treze pacotes, que é como uma regra
 * vira cópia incompleta — defeito que este projeto já pagou.
 */
class PublicacaoNoFluxoPorFatiaTest {

    /** Fluxo de mentira que só anota o que recebeu. */
    private static final class FluxoEspiao implements FluxoTelemetriaPort {
        record Publicado(String fatia, String tipo, Map<String, String> campos) {}

        final List<Publicado> recebidos = new ArrayList<>();

        @Override
        public void publicar(String fatia, String tipo, Map<String, String> campos) {
            recebidos.add(new Publicado(fatia, tipo, campos));
        }

        @Override
        public StatusFluxoTelemetria status() {
            return new StatusFluxoTelemetria(true, "espiao", recebidos.size());
        }
    }

    private static OperacaoTelemetria operacao(String tipo, String detalhe) {
        return new OperacaoTelemetria(tipo, detalhe, 1234L, 5, 7, 3, "2026-08-07T09:00:00");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caminho principal. Registrar leva ao fluxo, na fatia
     * que o mapa determina.
     */
    @Test
    @DisplayName("registrar uma operacao publica no fluxo da fatia correta")
    void publicaNaFatiaCerta() {
        FluxoEspiao espiao = new FluxoEspiao();
        TelemetriaService servico = new TelemetriaService(espiao);

        servico.registrarOperacao(operacao("Auditoria de Conteudo de Legendas", "sem caminho aqui"));

        assertEquals(1, espiao.recebidos.size());
        assertEquals("auditoria", espiao.recebidos.get(0).fatia());
        assertEquals("Auditoria de Conteudo de Legendas", espiao.recebidos.get(0).tipo());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o teste que justifica a fase inteira. O campo livre
     * carrega caminho absoluto em 69% dos registros do acervo, e é AQUI que ele
     * deixa de carregar — não no momento de publicar o dataset, quando já é tarde
     * para saber o que se perdeu.
     */
    @Test
    @DisplayName("o detalhe vai ao fluxo SANITIZADO, nunca cru")
    void detalheVaiSanitizado() {
        FluxoEspiao espiao = new FluxoEspiao();
        TelemetriaService servico = new TelemetriaService(espiao);

        servico.registrarOperacao(operacao("Limpeza de Cache",
            c("animes", "[Joseki] Mobile Suit Gundam 0083", "traducao_ptbr") + " | 15 arquivos"));

        String detalhe = espiao.recebidos.get(0).campos().get("detalhe");
        assertFalse(detalhe.contains(marcadorDrive('C')), "letra de drive nao pode ir ao dataset");
        assertTrue(detalhe.contains("[Joseki]"), "grupo de release FICA — e o valor do dado");
        assertTrue(detalhe.contains("15 arquivos"), "a medicao nao pode sair junto com o caminho");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: pasta pessoal nunca chega ao fluxo, nem aparada.
     */
    @Test
    @DisplayName("detalhe com pasta de usuario e REDIGIDO antes de publicar")
    void pastaPessoalNaoChegaAoFluxo() {
        FluxoEspiao espiao = new FluxoEspiao();
        TelemetriaService servico = new TelemetriaService(espiao);

        servico.registrarOperacao(operacao("Renomear Arquivos", c("Users", "Paulo", "Documents", "lote")));

        assertEquals(SanitizadorTelemetria.REDIGIDO, espiao.recebidos.get(0).campos().get("detalhe"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tipo que ninguém mapeou não pode sumir. Vai para
     * {@code outros} e continua contável.
     *
     * <p>É a diferença entre "não sabemos classificar" e "não existiu" — e a
     * segunda é uma mentira que o dataset carregaria para sempre.
     */
    @Test
    @DisplayName("tipo desconhecido cai em outros, nao desaparece")
    void tipoDesconhecidoVaiParaOutros() {
        FluxoEspiao espiao = new FluxoEspiao();
        TelemetriaService servico = new TelemetriaService(espiao);

        servico.registrarOperacao(operacao("Operacao Que Ninguem Mapeou Ainda", "detalhe"));

        assertEquals(1, espiao.recebidos.size(), "o evento NAO pode ser descartado");
        assertEquals(FatiaTelemetria.OUTROS, espiao.recebidos.get(0).fatia());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE da degradação. Sem fluxo, registrar
     * continua funcionando por inteiro — é a garantia central do desenho, e sem
     * este teste ela é só uma intenção escrita em Javadoc.
     */
    @Test
    @DisplayName("sem fluxo, registrar funciona igual e nao lanca")
    void semFluxoContinuaFuncionando() {
        TelemetriaService servico = new TelemetriaService();

        servico.registrarOperacao(operacao("Limpeza de Cache", c("animes", "obra")));

        assertEquals(FluxoTelemetriaPort.INERTE.status().conectado(), false,
            "a porta inerte se declara desconectada, nunca finge estar de pe");
    }

    /** As fatias com volume real do acervo têm de estar todas mapeadas. */
    @Test
    @DisplayName("os tipos medidos no acervo caem nas fatias esperadas")
    void tiposDoAcervoCaemNaFatiaEsperada() {
        assertEquals("auditoria", FatiaTelemetria.de("Auditoria de Conteudo de Legendas"));
        assertEquals("cache", FatiaTelemetria.de("Limpeza de Cache"));
        assertEquals("cache", FatiaTelemetria.de("Correção Google (cache)"));
        assertEquals("revisao", FatiaTelemetria.de("Revisao de Lore (.ass LLM)"));
        assertEquals("extracao", FatiaTelemetria.de("Extracao de Legendas (ASS)"));
        assertEquals("extracao", FatiaTelemetria.de("Remux (mkvmerge)"));
        assertEquals("karaoke", FatiaTelemetria.de("NOVO_KARAOKE"));
        assertEquals("terminologia", FatiaTelemetria.de("Reforço de Terminologia"));
        assertEquals("arquivos", FatiaTelemetria.de("Renomear Arquivos"));
        assertEquals("legenda", FatiaTelemetria.de("Achatar Estilos Decorativos"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o mesmo tipo aparece no acervo com e sem acento,
     * porque o registro vem de treze pontos e a grafia nunca foi centralizada.
     * As duas grafias têm de cair na mesma fatia.
     */
    @Test
    @DisplayName("grafia com e sem acento cai na MESMA fatia")
    void grafiaVarianteNaoSeparaFatia() {
        assertEquals(FatiaTelemetria.de("Correção Google (cache)"),
            FatiaTelemetria.de("Correcao Google (cache)"));
        assertEquals(FatiaTelemetria.de("Extração de Legendas (ASS)"),
            FatiaTelemetria.de("Extracao de Legendas (ASS)"));
        assertEquals("karaoke", FatiaTelemetria.de("KARAOKÊ SIMPLES"));
    }

    /** Entradas degeneradas não podem virar chave de stream degenerada. */
    @Test
    @DisplayName("tipo nulo ou vazio cai em outros")
    void tipoDegeneradoCaiEmOutros() {
        assertEquals(FatiaTelemetria.OUTROS, FatiaTelemetria.de(null));
        assertEquals(FatiaTelemetria.OUTROS, FatiaTelemetria.de(""));
        assertEquals(FatiaTelemetria.OUTROS, FatiaTelemetria.de("   "));
    }
}
