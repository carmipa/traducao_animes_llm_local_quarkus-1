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
 * <p>Cinco perguntas, em ordem de custo crescente: tem texto traduzível? o estilo é musical? é
 * efeito de karaokê que não seja música latina? a proteção de ASS manda a IA não encostar? o estilo
 * é de placa ({@code sign})? Só o que sobrevive às cinco é diálogo de verdade.
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
     * Google ou ao LLM. Estilo musical é veto ABSOLUTO — a REGRA DE ESCOPO (Paulo, 2026-07-25,
     * gravada em {@code traducao.SeletorEventosTraduziveis}) diz que música e karaokê, inclusive em
     * inglês, pertencem à fatia {@code traducaoKaraoke}, que sabe lidar com KFX, camadas e timing
     * por sílaba. Aqui não é "não consigo revisar", é "não é meu trabalho".
     *
     * <p>Esta guarda tinha uma exceção — {@code && !eKaraokeOuMusicaTraduzivel(...)} — que readmitia
     * letra em inglês por ela ser "música traduzível". A exceção custou, medida em 2026-07-28:
     * <ul>
     *   <li><b>Gundam 08th</b>: 10 dos 13 episódios BLOQUEADOS por retradução em massa. No ep02, as
     *       70 linhas {@code Song ENG} eram exatamente as 70 "falas iguais ao original" de 335
     *       auditáveis (21%). O {@code .cache.json} do mesmo episódio tem 267 entradas e ZERO
     *       {@code Song ENG}: a Opção 4 nunca as traduziu, como manda o escopo. Nenhuma fala de
     *       diálogo desses 10 arquivos chegou a ser revisada.</li>
     *   <li><b>Zeta</b>: dos 1.027 eventos que a revisão alterou, <b>1.008 eram {@code Song ENG}</b>
     *       e 19 eram diálogo. 98,1% da rodada foi gasta na fatia errada.</li>
     * </ul>
     *
     * <p>O predicado {@code eKaraokeOuMusicaTraduzivel} NÃO foi alterado: ele responde "isto é
     * música latina, não romaji", que é CLASSIFICAÇÃO, e segue servindo à telemetria de pendência e
     * à auditoria de dano em karaokê. Escopo e classificação divergem aqui de propósito.
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
        if (evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo())) {
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
