package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

/**
 * PROPÓSITO DE NEGÓCIO: decide se uma linha da legenda entra ou não na revisão linguística. É o
 * porteiro do processo: tudo que passa por aqui será enviado a um provedor externo (Google ou LLM)
 * e poderá ser reescrito, então o custo de errar para o lado permissivo é uma placa de tela, um
 * karaokê ou um desenho vetorial voltando alterado.
 *
 * <p>Cinco perguntas, em ordem de custo crescente: tem texto traduzível? o estilo é musical e não é
 * karaokê cantado? é efeito de karaokê? a proteção de ASS manda a IA não encostar? o estilo é de
 * placa ({@code sign})? Só o que sobrevive às cinco é diálogo de verdade.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Na dúvida, IGNORA. Uma fala de diálogo deixada de fora perde uma correção; uma placa
 *       deixada entrar volta com o typesetting quebrado, e isso não tem desfazer automático.</li>
 *   <li>Não imprime, não escreve, não decide o que fazer com a linha — só se ela é candidata.</li>
 *   <li>Quatro colaboradores, todos de peers ({@code legenda}, {@code qualidadeTraducao}). Nenhum
 *       vem da própria fatia: a pergunta "isto é diálogo?" é conhecimento de legenda, não de
 *       revisão. Extraí-los para cá tirou {@code PoliticaEstiloMusical} e
 *       {@code DetectorEfeitoKaraokeService} do caso de uso, onde não eram usados para mais nada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estilo nulo degrada para vazio e a linha segue sendo avaliada pelas demais regras; nunca lança.
 */
@Service
public class FiltroAuditoriaLinha {

    private final MascaradorTags mascaradorTags;
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne os quatro conhecimentos que definem "isto é diálogo".
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do filtro.
     */
    public FiltroAuditoriaLinha(
            MascaradorTags mascaradorTags,
            PoliticaEstiloMusical politicaEstiloMusical,
            DetectorEfeitoKaraokeService detectorKaraoke,
            ProtecaoLegendaAssService protecaoAss) {
        this.mascaradorTags = mascaradorTags;
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a triagem COMPLETA de uma linha — estrutura e conteúdo juntos. Responde
     * a única pergunta que o laço de revisão precisa fazer antes de qualquer trabalho: esta linha
     * entra?
     *
     * <p>INVARIANTES DO DOMÍNIO: as checagens estruturais (não é diálogo, texto nulo, texto em
     * branco) vêm ANTES das de conteúdo, e não por estilo: {@link #deveIgnorar} assume texto
     * utilizável. No laço, estas três eram {@code if}s separados com desfecho IDÊNTICO — manter a
     * fala e seguir —, e três formas de dizer a mesma coisa é onde nasce a divergência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; na dúvida, IGNORA.
     *
     * @param evento a linha da legenda
     * @return {@code true} se a linha NÃO deve entrar na revisão
     */
    public boolean deveIgnorarLinha(EventoLegenda evento) {
        if (!evento.isDialogo() || evento.texto() == null || evento.texto().isBlank()) {
            return true;
        }
        return deveIgnorar(evento, evento.texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: exclui da revisão linguística elementos estruturais, desenhos, estilos
     * ignorados e karaokê que não representam diálogo PT-BR.
     *
     * <p>INVARIANTES DO DOMÍNIO: conteúdo vetorial ASS e efeitos protegidos NUNCA são enviados ao
     * Google ou ao LLM; música traduzível continua auditável — a exceção do karaokê cantado existe
     * porque letra de abertura É diálogo para o espectador.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo sem texto visível é tratado como ignorável,
     * evitando alteração estrutural.
     *
     * @param evento a linha da legenda, com seu estilo
     * @param texto o texto da linha
     * @return {@code true} se a linha NÃO deve ser auditada
     */
    public boolean deveIgnorar(EventoLegenda evento, String texto) {
        if (!mascaradorTags.contemTextoTraduzivel(texto)) {
            return true;
        }
        if (evento.estilo() != null
            && politicaEstiloMusical.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (detectorKaraoke.eEfeitoKaraoke(texto)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(evento.estilo(), texto)) {
            return true;
        }
        String estilo = evento.estilo() != null ? evento.estilo().toLowerCase() : "";
        if (estilo.contains("sign")) {
            return true;
        }
        String visivel = protecaoAss.textoVisivel(texto);
        return estilo.contains("romaji") && visivel.equalsIgnoreCase("you");
    }
}
