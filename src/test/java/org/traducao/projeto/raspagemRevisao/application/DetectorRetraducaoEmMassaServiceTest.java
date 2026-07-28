package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DiagnosticoRetraducao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a proteção contra retradução em massa no nível do ELO, sem disco e
 * sem rede — critério de saída da FASE 4. Até aqui ela só era exercitável através do caso de uso
 * inteiro, com arquivos em disco: um teste que leva 20 segundos e falha por dez motivos diferentes
 * não é rede de segurança de uma regra de decisão.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Documentos são montados em memória; nenhum arquivo é lido ou escrito.</li>
 *   <li>Os cenários usam o limiar REAL (20 falas e um décimo do auditável). Baixá-lo para o teste
 *       provaria uma regra que não existe em produção.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se o detector deixar de contar, a Opção 6 volta a aceitar arquivos não traduzidos e gasta a GPU
 * refazendo o trabalho da etapa de Tradução Local.
 */
@QuarkusTest
class DetectorRetraducaoEmMassaServiceTest {

    @Inject
    DetectorRetraducaoEmMassaService detector;

    private static final ContextoRevisao CONTEXTO =
        new ContextoRevisao("danmachi", "Lore de teste", Set.of());

    private static String fraseInglesa(int i) {
        return "The commander said we should move out at once, number " + i + ".";
    }

    private static EventoLegenda dialogo(int indice, String texto) {
        return new EventoLegenda(indice, "Dialogue", "Default", "", texto);
    }

    /** Documento com {@code emIngles} falas idênticas ao original e {@code traduzidas} em PT. */
    private static DocumentoLegenda documento(int emIngles, int traduzidas, Map<Integer, String> originais) {
        List<EventoLegenda> eventos = new ArrayList<>();
        int indice = 0;
        for (int i = 0; i < emIngles; i++, indice++) {
            eventos.add(dialogo(indice, fraseInglesa(i)));
            originais.put(indice, fraseInglesa(i));
        }
        for (int i = 0; i < traduzidas; i++, indice++) {
            eventos.add(dialogo(indice, "O comandante mandou sair agora, número " + i + "."));
            originais.put(indice, fraseInglesa(100 + i));
        }
        return new DocumentoLegenda("[Events]", eventos, "\n", false);
    }

    @Test
    @DisplayName("Arquivo quase todo em inglês: bloqueia")
    void bloqueiaArquivoNaoTraduzido() {
        Map<Integer, String> originais = new HashMap<>();
        DocumentoLegenda doc = documento(25, 2, originais);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(27, d.falasAuditaveis(), "as 27 falas têm original comparável");
        assertEquals(25, d.falasNaoTraduzidas());
        assertTrue(d.deveBloquear(), "25 de 27 passa nas duas condições do limiar");
    }

    @Test
    @DisplayName("Poucas falas em inglês num arquivo grande: NÃO bloqueia")
    void naoBloqueiaPoucasPendencias() {
        Map<Integer, String> originais = new HashMap<>();
        DocumentoLegenda doc = documento(5, 300, originais);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(5, d.falasNaoTraduzidas());
        assertFalse(d.deveBloquear(),
            "5 falas não atinge o mínimo absoluto — revisar 5 é o trabalho da Opção 6");
    }

    @Test
    @DisplayName("Muitas falas em inglês, mas arquivo enorme: NÃO bloqueia por proporção")
    void naoBloqueiaQuandoAProporcaoNaoFecha() {
        Map<Integer, String> originais = new HashMap<>();
        DocumentoLegenda doc = documento(21, 400, originais);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(21, d.falasNaoTraduzidas(), "passa dos 20 absolutos");
        assertFalse(d.deveBloquear(),
            "21 em 421 é menos de um décimo: é revisão normal, não arquivo não traduzido");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma fala só entra na contagem se houver original comparável. Sem esta
     * regra, um arquivo sem cache teria denominador zero e o comportamento viraria loteria.
     */
    @Test
    @DisplayName("Sem originais, o diagnóstico é vazio e NÃO bloqueia")
    void semOriginaisNaoBloqueia() {
        Map<Integer, String> vazio = new HashMap<>();
        DocumentoLegenda doc = documento(30, 0, new HashMap<>());

        DiagnosticoRetraducao d = detector.diagnosticar(doc, vazio, CONTEXTO);

        assertEquals(0, d.falasAuditaveis());
        assertFalse(d.deveBloquear(), "na ausência de dados não se recusa o arquivo");
        assertFalse(detector.diagnosticar(null, vazio, CONTEXTO).deveBloquear());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: espaçamento diferente NÃO é tradução diferente. Sem normalizar, uma
     * fala em inglês com dois espaços escaparia da contagem e a proteção furaria em silêncio.
     */
    @Test
    @DisplayName("Diferença só de espaçamento continua contando como não traduzida")
    void espacamentoNaoEscondeFalaEmIngles() {
        Map<Integer, String> originais = new HashMap<>();
        List<EventoLegenda> eventos = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            eventos.add(dialogo(i, fraseInglesa(i).replace(" ", "  ")));
            originais.put(i, fraseInglesa(i));
        }
        DocumentoLegenda doc = new DocumentoLegenda("[Events]", eventos, "\n", false);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(25, d.falasNaoTraduzidas(),
            "espaço duplo não torna a fala traduzida");
        assertTrue(d.deveBloquear());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: linhas que a revisão não audita (placas, karaokê, não-diálogo) não
     * podem inflar o denominador — se inflassem, a proporção nunca fecharia e a proteção deixaria
     * de disparar em arquivos com muito typesetting.
     */
    @Test
    @DisplayName("Não-diálogo e texto vazio ficam fora da contagem")
    void naoDialogoNaoContaComoAuditavel() {
        Map<Integer, String> originais = new HashMap<>();
        List<EventoLegenda> eventos = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            eventos.add(dialogo(i, fraseInglesa(i)));
            originais.put(i, fraseInglesa(i));
        }
        for (int i = 25; i < 60; i++) {
            eventos.add(new EventoLegenda(i, "Comment", "Default", "", fraseInglesa(i)));
            originais.put(i, fraseInglesa(i));
        }
        eventos.add(dialogo(60, "   "));
        originais.put(60, fraseInglesa(60));
        DocumentoLegenda doc = new DocumentoLegenda("[Events]", eventos, "\n", false);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(25, d.falasAuditaveis(),
            "os 35 comentários e a linha em branco não são auditáveis");
        assertTrue(d.deveBloquear(), "25 de 25 bloqueia; se os comentários contassem, não bloquearia");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reproduz o episódio 02 do Gundam 08th MS Team como ele estava em
     * 2026-07-28 — 262 falas de diálogo traduzidas e 70 linhas de estilo {@code Song ENG} em inglês,
     * que é como a letra do OP/ED DEVE ficar enquanto a fatia {@code traducaoKaraoke} não a assume.
     *
     * <p>Naquele dia o arquivo foi BLOQUEADO: as 70 linhas de música eram exatamente as 70 "falas
     * iguais ao original" de 335 auditáveis (21%), e o bloqueio impediu que qualquer uma das 262
     * falas de diálogo fosse revisada. Aconteceu em 10 dos 13 episódios.
     *
     * <p>INVARIANTES DO DOMÍNIO: música não entra no numerador NEM no denominador. Contar dos dois
     * lados manteria a proporção e o bloqueio; contar só no denominador mascararia arquivo
     * realmente não traduzido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se a música voltar à contagem, o teste bloqueia um arquivo
     * cujas 262 falas estão todas traduzidas.
     */
    @Test
    @DisplayName("letra de música em inglês não bloqueia arquivo de diálogo traduzido")
    void musicaEmInglesNaoContaComoArquivoNaoTraduzido() {
        Map<Integer, String> originais = new HashMap<>();
        List<EventoLegenda> eventos = new ArrayList<>();
        int indice = 0;
        for (int i = 0; i < 262; i++, indice++) {
            eventos.add(dialogo(indice, "O comandante mandou sair agora, número " + i + "."));
            originais.put(indice, fraseInglesa(i));
        }
        for (int i = 0; i < 70; i++, indice++) {
            String letra = "You see the dream shining within the storm, verse " + i + ".";
            eventos.add(new EventoLegenda(indice, "Dialogue", "Song ENG", "", letra));
            originais.put(indice, letra);
        }
        DocumentoLegenda doc = new DocumentoLegenda("[Events]", eventos, "\n", false);

        DiagnosticoRetraducao d = detector.diagnosticar(doc, originais, CONTEXTO);

        assertEquals(262, d.falasAuditaveis(),
            "as 70 linhas de música estão fora do escopo desta etapa e não são auditáveis");
        assertEquals(0, d.falasNaoTraduzidas());
        assertFalse(d.deveBloquear(),
            "as 262 falas de diálogo estão traduzidas; bloquear aqui impede a revisão de todas elas");
    }
}
