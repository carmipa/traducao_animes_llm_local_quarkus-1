package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.application.SanitizadorTagsService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: deixa uma fala pronta para ser julgada — encontra o original inglês que
 * serve de referência e conserta tags de karaokê mutiladas, nessa ordem.
 *
 * <h2>A ordem é a regra, não um detalhe</h2>
 * O original EN é resolvido ANTES do saneamento de karaokê, e isso não pode ser invertido por
 * "organização". A busca por texto traduzido usa o texto COMO ESTÁ no cache (pré-correção); se o
 * saneamento rodar primeiro, a chave muda, o mapa devolve nada e a fala passa a ser tratada como
 * "sem original" — deixando de ser auditada. Além disso, o original serve de referência ao
 * sanitizador para preservar comentários legítimos entre chaves em vez de escapá-los como
 * alucinação.
 *
 * <h2>A guarda de integridade</h2>
 * Uma correção NUNCA pode apagar uma fala. Se o saneamento deixou o texto VISÍVEL vazio numa linha
 * que tinha conteúdo, a "correção" é recusada e o original é mantido. Sem essa guarda, um bloco
 * contínuo de diálogo do Guilty Crown ep4 (13:58–20:52) foi esvaziado: as linhas ficaram com tempo
 * e estilo, e o texto em branco.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO imprime e NÃO conta. Devolve o que aconteceu — inclusive o texto anterior — e quem
 *       narra e contabiliza é o caso de uso. Aqui não há mensagem que precise sair antes de outra,
 *       então a coleta não inverte ordem nenhuma.</li>
 *   <li>O evento devolvido pode ser OUTRO objeto (records são imutáveis): quem chama tem de usar o
 *       devolvido, não o que passou.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem original EN, o saneamento ainda roda — só perde a referência que preserva comentários. Nunca
 * lança.
 */
@Service
public class PreparadorFalaRevisao {

    private final SanitizadorTagsService sanitizadorTags;
    private final ProtecaoLegendaAssService protecaoAss;
    private final RemovedorItalico removedorItalico;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne o sanitizador de tags e a noção de texto visível.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do preparador.
     */
    public PreparadorFalaRevisao(
            SanitizadorTagsService sanitizadorTags,
            ProtecaoLegendaAssService protecaoAss,
            RemovedorItalico removedorItalico) {
        this.sanitizadorTags = sanitizadorTags;
        this.protecaoAss = protecaoAss;
        this.removedorItalico = removedorItalico;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o que a preparação apurou sobre a fala.
     *
     * @param evento a fala, possivelmente com o karaokê já corrigido
     * @param originalEn o original inglês de referência, ou {@code null}
     * @param temOriginalEn se há original utilizável
     * @param karaokeCorrigido se o saneamento foi aplicado
     * @param karaokeRecusadoPorEsvaziamento se a guarda impediu o saneamento
     * @param textoAnterior o texto antes do saneamento, para a mensagem ao operador
     * @param italicoRemovido se a regra do itálico tirou a tag desta fala
     */
    public record FalaPreparada(
        EventoLegenda evento,
        String originalEn,
        boolean temOriginalEn,
        boolean karaokeCorrigido,
        boolean karaokeRecusadoPorEsvaziamento,
        String textoAnterior,
        boolean italicoRemovido
    ) {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve o original e sanea o karaokê, nessa ordem obrigatória.
     *
     * <p>INVARIANTES DO DOMÍNIO: o original é buscado primeiro por ÍNDICE e, só se faltar, por
     * texto traduzido — o índice é vínculo forte, o texto é heurística.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    public FalaPreparada preparar(
        EventoLegenda evento,
        Map<Integer, String> originaisPorIndice,
        Map<String, String> originalPorTraduzido
    ) {
        String textoAnterior = evento.texto();
        String originalEn = originaisPorIndice.get(evento.indice());
        if (originalEn == null || originalEn.isBlank()) {
            originalEn = originalPorTraduzido.get(normalizar(textoAnterior));
        }
        boolean temOriginalEn = originalEn != null && !originalEn.isBlank();

        String corrigidoKaraoke = sanitizadorTags.escaparChavesInvalidas(textoAnterior, originalEn);
        boolean esvaziaria = esvaziariaFala(textoAnterior, corrigidoKaraoke);
        String base = esvaziaria ? textoAnterior : corrigidoKaraoke;
        boolean karaokeCorrigido = !esvaziaria && !textoAnterior.equals(corrigidoKaraoke);

        // A REGRA DO ITALICO (22/08/2026) aplicada na 3.1, DEPOIS do saneamento de karaoke:
        // primeiro conserta a tag mutilada, so entao tira o italico. Invertido, o removedor
        // veria uma chave malformada e nao reconheceria o token.
        //
        // POR QUE AQUI, e nao so na Traducao: a 3.1 e a tela que o operador passa no acervo
        // JA traduzido. A regra na 2.1 so alcanca episodio processado dali para a frente; sem
        // esta etapa, as 9.769 falas de dialogo em italico do acervo continuariam la para
        // sempre, e a tela que existe para corrigir passaria por elas sem ver.
        //
        // MUSICA NAO CHEGA AQUI: FiltroAuditoriaLinha.deveIgnorarLinha ja mandou embora
        // musica, karaoke, conteudo vetorial e estilo-ignorado, com veto ABSOLUTO.
        String semItalico = removedorItalico.remover(base);
        boolean italicoRemovido = !base.equals(semItalico) && !esvaziariaFala(base, semItalico);
        String textoFinal = italicoRemovido ? semItalico : base;

        if (karaokeCorrigido || italicoRemovido) {
            return new FalaPreparada(evento.comTexto(textoFinal), originalEn, temOriginalEn,
                karaokeCorrigido, false, textoAnterior, italicoRemovido);
        }
        return new FalaPreparada(evento, originalEn, temOriginalEn, false, esvaziaria,
            textoAnterior, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda de integridade — uma correção JAMAIS pode transformar uma fala
     * com texto numa linha vazia. O resultado seria uma legenda que some da tela mantendo tempo e
     * estilo.
     *
     * <p>INVARIANTES DO DOMÍNIO: só acusa quando o ORIGINAL tinha texto visível e o CORRIGIDO ficou
     * sem nenhum — reordenar, reescrever ou trocar tags continua permitido; apagar, não.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos degradam para vazio; nunca lança. Original já vazio
     * devolve {@code false} (não há fala a proteger).
     */
    boolean esvaziariaFala(String original, String corrigido) {
        return !protecaoAss.textoVisivel(original).isBlank()
            && protecaoAss.textoVisivel(corrigido).isBlank();
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
