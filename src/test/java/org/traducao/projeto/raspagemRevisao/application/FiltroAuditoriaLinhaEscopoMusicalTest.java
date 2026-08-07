package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: sela a REGRA DE ESCOPO (Paulo, 2026-07-25) no porteiro da Opção 6 — música e
 * karaokê, INCLUSIVE em inglês, pertencem à fatia {@code traducaoKaraoke} e não entram na revisão de
 * fala.
 *
 * <h2>O que a exceção custava</h2>
 * A guarda de estilo musical tinha um escape, {@code && !eKaraokeOuMusicaTraduzivel(...)}, que
 * readmitia letra latina "porque é traduzível". Medido em 2026-07-28:
 * <pre>
 *   Gundam 08th   10 de 13 episódios BLOQUEADOS por retradução em massa.
 *                 ep02: 70 linhas "Song ENG" = as 70 "falas iguais ao original" de 335 auditáveis.
 *                 O .cache.json do ep02 tem 267 entradas e ZERO "Song ENG" — a Opção 4 nunca as
 *                 traduziu, exatamente como o escopo manda.
 *   Zeta          dos 1.027 eventos alterados pela revisão, 1.008 eram "Song ENG" e 19 diálogo.
 * </pre>
 *
 * <h2>Invariantes do domínio</h2>
 * O predicado {@code eKaraokeOuMusicaTraduzivel} NÃO mudou. Ele responde "isto é música latina, não
 * romaji" — CLASSIFICAÇÃO —, e continua servindo à telemetria de pendência e à auditoria de dano em
 * karaokê. É por isso que o primeiro teste afirma as DUAS coisas sobre a MESMA linha: fora de
 * escopo, e ainda assim classificada como música traduzível. Escopo e classificação divergem aqui de
 * propósito, e um teste que só olhasse um dos lados não provaria a separação.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o escape voltar, a Opção 6 volta a gastar a rodada traduzindo abertura e a bloquear os arquivos
 * por causa dela.
 */
@QuarkusTest
class FiltroAuditoriaLinhaEscopoMusicalTest {

    @Inject
    FiltroAuditoriaLinha filtro;

    @Inject
    DetectorEfeitoKaraokeService detectorKaraoke;

    /** Linha real do OP do Gundam 08th MS Team, ep02, estilo {@code Song ENG}. */
    private static final String LETRA_EN =
        "{\\fad(100,100)\\blur1\\bord0}You see the dream shining within the storm.";

    private static EventoLegenda evento(String estilo, String texto) {
        return new EventoLegenda(1, "Dialogue", estilo, "", texto);
    }

    @Test
    @DisplayName("letra em inglês fica FORA do escopo, e mesmo assim segue classificada como música")
    void escopoDivergeDaClassificacao() {
        EventoLegenda linha = evento("Song ENG", LETRA_EN);

        assertTrue(filtro.deveIgnorar(linha, LETRA_EN),
            "estilo musical é veto absoluto: a revisão de fala não toca em letra de música");
        assertTrue(detectorKaraoke.eKaraokeOuMusicaTraduzivel("Song ENG", LETRA_EN),
            "a CLASSIFICAÇÃO não mudou — quem pergunta 'é música latina?' continua ouvindo 'sim'");
    }

    /**
     * Isola a CAUSA. Sem este par, o teste acima passaria mesmo que a linha fosse descartada por
     * qualquer outro motivo (tag, texto não traduzível, proteção de ASS) — e provaria nada sobre o
     * estilo.
     */
    @Test
    @DisplayName("o mesmo texto, em estilo de diálogo, continua entrando na revisão")
    void oQueDecideEhOEstilo() {
        assertFalse(filtro.deveIgnorar(evento("Default", LETRA_EN), LETRA_EN),
            "o texto é idêntico; se esta linha também fosse ignorada, o estilo não seria a causa");
    }

    @Test
    @DisplayName("diálogo comum não é afetado")
    void dialogoComumContinuaEntrando() {
        String fala = "Master Chief, qual era mesmo o seu nome?";
        assertFalse(filtro.deveIgnorar(evento("Dialogue", fala), fala));
        assertFalse(filtro.deveIgnorar(evento("Default", fala), fala));
    }

    /**
     * Nomes REAIS de estilo colhidos no acervo (490 arquivos {@code .ass}). Não são exemplos
     * inventados: são as grafias que os fansubs usaram e que a Opção 6 precisa recusar.
     */
    @Test
    @DisplayName("as grafias de estilo musical que existem no acervo ficam todas de fora")
    void grafiasReaisDoAcervoFicamDeFora() {
        for (String estilo : new String[] {
            "Song ENG", "OP - English", "ED - English", "OP Eng", "ED Eng", "Insert Song",
            "Insert Song TL", "OP Lyrics", "ED Lyrics", "Copy of OP", "insert",
            "ED Romaji", "OP Roma", "ED_Romaji1", "Song JP"
        }) {
            assertTrue(filtro.deveIgnorar(evento(estilo, LETRA_EN), LETRA_EN),
                "estilo musical do acervo entrou na revisão de fala: " + estilo);
        }
    }

    /**
     * RESÍDUO FECHADO em 07/08/2026, com autorização de Paulo. O texto abaixo é o registro do
     * que ele era e por que a decisão anterior de adiar foi revisitada — não o apague.
     *
     * <h2>O que mudou</h2>
     * A pergunta "este estilo declara música?" passou a ter dono único
     * ({@code legenda.domain.PadraoEstiloMusical}), com fronteira de LETRA em vez de {@code \b}.
     * A divergência entre os dois donos anteriores estava medida em <b>1.385 linhas de
     * 1.719.242 falas</b> ({@code MedicaoDivergenciaPadraoMusicalIT}) e foi a ZERO.
     *
     * <p>Este teste caiu quando a convergência entrou — <b>funcionando exatamente como o
     * Javadoc abaixo previa</b>: "se a fase de karaokê alargar o padrão, o teste cai e obriga a
     * revisitar a caracterização". A implementação foi PARADA, o efeito medido e a decisão
     * devolvida a Paulo, que autorizou fechar agora.
     *
     * <p>Impacto medido no acervo: <b>1.385 linhas em 8 obras</b> — Guilty Crown 633,
     * Gundam ZZ 460, Unicorn 207, DanMachi 70, Gundam 0083 15. Maior que as 494 previstas,
     * porque a convergência traz também {@code Other songs}, {@code insertita} e as camadas
     * {@code _roma}.
     *
     * <hr>
     *
     * <p>REGISTRO DO QUE ERA — resíduo conhecido, medido e deliberadamente NÃO fechado até 07/08:
     *
     * <p>Este filtro decide por {@code PoliticaEstiloMusical.estiloIgnorado}, cuja regex usa
     * {@code \b}. Sublinhado e dígito são caracteres de palavra, então {@code \bed\b} não alcança
     * {@code ED_S2} nem {@code OP2}. A Opção 4 decide por {@code podeSerCamadaMusical}, cujo padrão
     * usa fronteira por LETRA e alcança os dois — então estas linhas não são traduzidas lá e
     * continuam auditáveis aqui, que é a mesma forma do defeito que este arquivo acabou de fechar.
     *
     * <p>Não foi fechado porque a decisão de deixá-los fora é anterior, explícita e datada, no
     * Javadoc de {@code DetectorEfeitoKaraokeService.temIndicadorDeMusica}: <i>"a MESMA limitação do
     * {@code \b} continua valendo para OP2, ED_S2 e OP_S2 (as camadas em inglês, 494 linhas no
     * acervo)... alargar o padrão genérico arrastaria essas linhas para dentro do simplificador de
     * karaokê e do fluxo de correção de uma vez — mudança de outra ordem, que pertence à fase de
     * retrabalho do karaokê simples, com caracterização própria."</i>
     *
     * <p>O tamanho está medido e é pequeno: <b>494 linhas no acervo inteiro</b>, contra as 70 por
     * EPISÓDIO que bloqueavam o Gundam 08th. Não chega perto do limiar de retradução em massa (20
     * falas E um décimo do auditável) em nenhum arquivo real.
     *
     * <p>Este teste não afirma que o resíduo é desejável. Afirma que ele é ESTE, e trava o tamanho:
     * se a fase de karaokê alargar o padrão, o teste cai e obriga a revisitar a caracterização.
     */
    @Test
    @DisplayName("o residuo da fronteira \\b foi FECHADO em 07/08/2026")
    void residuoConhecidoDaFronteiraComBarraB() {
        for (String estilo : new String[] {"ED_S2", "OP_S2", "OP2", "ED2-English", "ED_English"}) {
            assertTrue(filtro.deveIgnorar(evento(estilo, LETRA_EN), LETRA_EN),
                "camada em inglês do karaokê voltou a entrar na revisão: " + estilo);
        }
    }
}
