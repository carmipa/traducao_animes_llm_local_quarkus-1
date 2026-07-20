package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.revisaoLore.contexto.ContextoRevisaoLore86;
import org.traducao.projeto.revisaoLore.contexto.ContextoRevisaoLoreGundamZeta;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLore;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: protege as fronteiras de segurança e os desfechos da
 * opção 7 contra regressões.
 * <p>INVARIANTES DO DOMÍNIO: testes não acessam LLM ou arquivos reais do usuário.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer quebra de contrato reprova a suíte.
 */
class RevisarLoreUseCaseTest {

    private static final String CABECALHO =
        "[Events]\n"
        + "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n";

    /**
     * PROPÓSITO DE NEGÓCIO: cria diálogo ASS mínimo para cenários de alinhamento.
     * <p>INVARIANTES DO DOMÍNIO: prefixo contém Start e End nas posições canônicas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dados inválidos permanecem visíveis no teste.
     */
    private static EventoLegenda dialogo(int indice, String inicio, String fim, String texto) {
        String prefixo = "Dialogue: 0," + inicio + "," + fim + ",Default,,0,0,0,,";
        return new EventoLegenda(indice, "Dialogue", "Default", prefixo, texto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria comentário ASS para testar divergência de tipo.
     * <p>INVARIANTES DO DOMÍNIO: usa a mesma estrutura temporal do diálogo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não normaliza os parâmetros recebidos.
     */
    private static EventoLegenda comentario(int indice, String inicio, String fim, String texto) {
        String prefixo = "Comment: 0," + inicio + "," + fim + ",Default,,0,0,0,,";
        return new EventoLegenda(indice, "Comment", "Default", prefixo, texto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: agrupa eventos num documento ASS mínimo e comparável.
     * <p>INVARIANTES DO DOMÍNIO: cabeçalho declara as colunas usadas nos prefixos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista inválida falha imediatamente no teste.
     */
    private static DocumentoLegenda doc(List<EventoLegenda> eventos) {
        return new DocumentoLegenda(CABECALHO, eventos, "\n", false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que resposta idêntica do LLM transforme uma
     * violação detectada em falso conforme.
     * <p>INVARIANTES DO DOMÍNIO: Canela permanece suspeita para Shin; Shin limpa
     * o indício quando a lore ativa confirma o nome.
     * <p>COMPORTAMENTO EM CASO DE FALHA: decisão incorreta reprova o teste.
     */
    @Test
    void exigeQuePropostaElimineIndicioDeLore() {
        DetectorTermosLoreService detector = new DetectorTermosLoreService();
        ResultadoDeteccaoLore antes = detector.auditar("Shin!", "Canela!", "Nome oficial: Shin");
        ResultadoDeteccaoLore semMelhoria = detector.auditar("Shin!", "Canela!", "Nome oficial: Shin");
        ResultadoDeteccaoLore corrigida = detector.auditar("Shin!", "Shin!", "Nome oficial: Shin");

        assertTrue(antes.suspeito());
        assertFalse(RevisarLoreUseCase.problemaLoreFoiResolvido(antes, semMelhoria));
        assertTrue(RevisarLoreUseCase.problemaLoreFoiResolvido(antes, corrigida));
        assertFalse(RevisarLoreUseCase.problemaLoreFoiResolvido(antes, null));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o banner final diferencie conclusão,
     * pendências, cancelamento e ausência de arquivos.
     * <p>INVARIANTES DO DOMÍNIO: a precedência segue sem arquivos, cancelado,
     * pendências e concluído.
     * <p>COMPORTAMENTO EM CASO DE FALHA: status divergente reprova o teste.
     */
    @Test
    void determinaStatusRealDaSessao() {
        assertEquals(StatusRevisaoLore.SEM_ARQUIVOS,
            RevisarLoreUseCase.determinarStatus(true, false, List.of(), 0));
        assertEquals(StatusRevisaoLore.CANCELADO,
            RevisarLoreUseCase.determinarStatus(false, true, List.of(), 0));
        assertEquals(StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS,
            RevisarLoreUseCase.determinarStatus(false, false, List.of("erro"), 0));
        assertEquals(StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS,
            RevisarLoreUseCase.determinarStatus(false, false, List.of(), 1));
        assertEquals(StatusRevisaoLore.CONCLUIDO,
            RevisarLoreUseCase.determinarStatus(false, false, List.of(), 0));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que a primeira versão PT-BR é preservada
     * antes de qualquer sobrescrita da opção 7.
     * <p>INVARIANTES DO DOMÍNIO: chamadas repetidas na mesma sessão não
     * substituem o primeiro backup.
     * <p>COMPORTAMENTO EM CASO DE FALHA: cópia ausente ou sobrescrita reprova o teste.
     */
    @Test
    void backupPreservaPrimeiraVersaoDaSessao(@TempDir Path tempDir) throws IOException {
        Path legenda = tempDir.resolve("episodio_PT-BR.ass");
        Path pastaBackup = tempDir.resolve("backup").toAbsolutePath().normalize();
        Files.writeString(legenda, "versao-original");

        Path backup = RevisarLoreUseCase.criarBackup(legenda, pastaBackup);
        Files.writeString(legenda, "versao-alterada");
        Path backupRepetido = RevisarLoreUseCase.criarBackup(legenda, pastaBackup);

        assertEquals(backup, backupRepetido);
        assertEquals("versao-original", Files.readString(backup));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o dataset canônico permita localizar
     * execuções incompletas sem depender do relatório detalhado.
     * <p>INVARIANTES DO DOMÍNIO: status, pendências, sem-resposta, descartes e
     * erros usam chaves estáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de qualquer campo reprova o teste.
     */
    @Test
    void detalheCanonicoIncluiStatusEPendencias(@TempDir Path tempDir) {
        String detalhe = RevisarLoreUseCase.montarDetalheTelemetria(
            tempDir, "Gundam NT", StatusRevisaoLore.CONCLUIDO_COM_PENDENCIAS,
            4, 1, 2, 1, 1);

        assertTrue(detalhe.contains("status=CONCLUIDO_COM_PENDENCIAS"));
        assertTrue(detalhe.contains("pendentes=4"));
        assertTrue(detalhe.contains("semResposta=1"));
        assertTrue(detalhe.contains("descartadas=2"));
        assertTrue(detalhe.contains("encaminhadasOpcao6=1"));
        assertTrue(detalhe.contains("erros=1"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que fala ainda integralmente em inglês gaste
     * uma chamada do LLM especializado em lore.
     * <p>INVARIANTES DO DOMÍNIO: tags ASS e caixa não mudam a identidade visual;
     * uma tradução efetiva não pode ser encaminhada à Opção 6.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação divergente reprova o teste.
     */
    @Test
    void identificaFalaIntegralmenteNaoTraduzida() {
        assertTrue(RevisarLoreUseCase.ehFalaNaoTraduzida(
            "{\\i1}A bridge, huh?", "{\\i1}a bridge, huh?"));
        assertFalse(RevisarLoreUseCase.ehFalaNaoTraduzida(
            "{\\i1}A bridge, huh?", "{\\i1}Uma ponte, hein?"));
        assertFalse(RevisarLoreUseCase.ehFalaNaoTraduzida("", ""));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o console mostre progresso global do
     * lote sem reiniciar o denominador a cada arquivo.
     * <p>INVARIANTES DO DOMÍNIO: marcador contém arquivo, fala global e evento.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato incompleto reprova o teste.
     */
    @Test
    void marcadorExibeTotaisGlobaisDoLote() {
        assertEquals(
            "[Arquivo 2/13 | Fala 74/3598 | evento 5]",
            RevisarLoreUseCase.formatarMarcadorProgresso(2, 13, 74, 3598, 5)
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita pares EN/PT com estrutura temporal equivalente.
     * <p>INVARIANTES DO DOMÍNIO: quantidade, tipo e tempos coincidem.
     * <p>COMPORTAMENTO EM CASO DE FALHA: divergência artificial reprova o teste.
     */
    @Test
    void paresAlinhadosNaoAcusamDivergencia() {
        DocumentoLegenda en = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Hello Shin."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "The Legion is coming.")));
        DocumentoLegenda pt = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Ola Shin."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "A Legiao esta chegando.")));

        assertTrue(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tolera arredondamento pequeno entre fontes de legenda.
     * <p>INVARIANTES DO DOMÍNIO: diferenças permanecem dentro de 500 ms.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso bloqueio reprova o teste.
     */
    @Test
    void jitterDentroDaToleranciaNaoAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Hello.")));
        DocumentoLegenda pt = doc(List.of(dialogo(0, "0:00:01.20", "0:00:03.10", "Ola.")));

        assertTrue(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia pareamento de falas temporalmente reordenadas.
     * <p>INVARIANTES DO DOMÍNIO: deslocamento excede a tolerância operacional.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de diagnóstico reprova o teste.
     */
    @Test
    void temposReordenadosAcusamDivergencia() {
        DocumentoLegenda en = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "First line."),
            dialogo(1, "0:00:04.00", "0:00:06.00", "Second line.")));
        // PT com a segunda fala deslocada em segundos (reordenacao/insercao a montante).
        DocumentoLegenda pt = doc(List.of(
            dialogo(0, "0:00:01.00", "0:00:03.00", "Primeira fala."),
            dialogo(1, "0:00:10.00", "0:00:12.00", "Segunda fala.")));

        Optional<String> r = RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500);
        assertTrue(r.isPresent());
        assertTrue(r.get().contains("tempos divergentes"));
        assertTrue(r.get().contains("evento 2"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede comparar Dialogue com Comment na mesma posição.
     * <p>INVARIANTES DO DOMÍNIO: tempos coincidem, isolando a divergência de tipo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência do bloqueio reprova o teste.
     */
    @Test
    void tipoDivergenteAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Line.")));
        DocumentoLegenda pt = doc(List.of(comentario(0, "0:00:01.00", "0:00:03.00", "Linha.")));

        Optional<String> r = RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500);
        assertTrue(r.isPresent());
        assertTrue(r.get().contains("tipo divergente"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite restyle legítimo sem confundir com desalinhamento.
     * <p>INVARIANTES DO DOMÍNIO: tipo e tempos coincidem; apenas Style muda.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso bloqueio reprova o teste.
     */
    @Test
    void estiloDiferenteNaoAcusaDivergencia() {
        DocumentoLegenda en = doc(List.of(dialogo(0, "0:00:01.00", "0:00:03.00", "Line.")));
        // PT restilizada (Style diferente), mesmo tempo/tipo: legitimo, nao bloqueia.
        EventoLegenda ptEvt = new EventoLegenda(
            0, "Dialogue", "Italico", "Dialogue: 0,0:00:01.00,0:00:03.00,Italico,,0,0,0,,", "Linha.");
        DocumentoLegenda pt = doc(List.of(ptEvt));

        assertFalse(RevisarLoreUseCase.primeiraDivergenciaEstrutural(en, pt, 500).isPresent());
    }

    @Test
    void contexto86ExpoeMapaDeTerminologia() {
        Map<String, String> mapa = new ContextoRevisaoLore86().correcoesTerminologia();
        assertEquals("Legion", mapa.get("Legião"));
        assertEquals("Undertaker", mapa.get("Coveiro"));
        assertEquals("Handler One", mapa.get("Handler Um"));
    }

    @Test
    void contextoZetaExpoeMapaDeTerminologia() {
        Map<String, String> mapa = new ContextoRevisaoLoreGundamZeta().correcoesTerminologia();
        assertEquals("Titans", mapa.get("Titãs"));
        assertEquals("Quattro", mapa.get("Quatro"));
        assertEquals("Axis", mapa.get("Eixo"));
        assertEquals("Hyaku Shiki", mapa.get("Cem Estilos"));
        assertEquals("Colony Laser", mapa.get("Laser de Colônia"));
        assertEquals("Qubeley", mapa.get("Cubely"));
    }
}
