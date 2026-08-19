package org.traducao.projeto.revisaoLore.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: responde à ÚNICA pergunta que delimita a tela 3.2 — <i>esta linha está
 * ao alcance da revisão de lore?</i> — para a porta viva da fatia, a aba "Com inglês"
 * ({@link RevisarLoreUseCase}).
 *
 * <p><b>Atualizado em 2026-08-19:</b> a aba <b>PT-only</b> ({@code RevisarLorePtOnlyUseCase}) foi
 * REMOVIDA em 17/08/2026 — ela não atendia nenhum caso do acervo. Esta classe continua existindo
 * porque a decisão de alcance é do domínio, não daquela aba: quem nascer como segunda porta da
 * fatia pergunta aqui em vez de escrever o próprio laço, que foi exatamente o defeito abaixo.
 *
 * <h2>O prejuízo que originou — medido, não hipótese</h2>
 * A resposta existia só dentro do {@code RevisarLoreUseCase}, como método privado. A aba PT-only
 * nasceu depois, com laço próprio, e filtrava apenas {@code temTexto()}: sem juiz de estilo
 * musical, sem {@code isDialogo()}, sem detector de karaokê, sem desenho vetorial. Ela grava no
 * {@code .ass} quando {@code aplicar=true}, e "Apenas simular" nasce DESMARCADO na interface.
 *
 * <p>Medido no acervo em 2026-08-17 ({@code MedicaoExposicaoMusicalRevisaoLorePtOnlyIT}, 22 obras,
 * calibrado contra o próprio caso de uso em dry-run — harness e produção bateram em 22 de 22):
 * <pre>
 *   246.246  linhas de estilo musical ao ALCANCE da aba PT-only
 *        0   reescritas HOJE pela camada determinística fora do Char's Counterattack
 *       29   no Char's Counterattack, que é o filme ACHATADO (todo evento herdou o nome do
 *            estilo decorativo, que está em estilos-ignorados) — ali as 29 são DIÁLOGO
 * </pre>
 * Ou seja: a camada determinística não morde música hoje. O que está aberto é a SUPERFÍCIE — a
 * camada 2 (LLM PT-only) reescreve a linha inteira, e decide sobre qualquer uma das 246.246 por
 * um portão de homógrafo de uma palavra. É a mesma forma exata da porta que reescreveu <b>687
 * linhas {@code Song ENG}</b> no Gundam 08th MS Team em 17/08/2026, e do
 * {@code RevisorPtOnlyUseCase} da 3.1, que ganhou veto mesmo sendo inalcançável por menu — porque
 * "inalcançável hoje" foi exatamente o estado da ponte do cache até ela morder.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Uma implementação só. Duas cópias desta decisão divergiriam em silêncio no dia em que um
 *       estilo novo entrasse, e o sinal apareceria numa legenda estragada — é o que a
 *       {@code CatracaEscritaDeFalaVetaMusicaLoreTest} congela nominalmente.</li>
 *   <li>Não decide o que é música: PERGUNTA à {@link PoliticaEstiloMusical}, que é o dono da
 *       regra e mora no peer {@code legenda}. Idem karaokê e typesetting.</li>
 *   <li>Falha FECHADA: evento nulo, sem texto ou sem estilo fica FORA do alcance. Na dúvida a
 *       tela não escreve — perder uma correção de nome custa menos que reescrever um ED.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Método puro e sem estado; não lança. Qualquer entrada degenerada devolve {@code false}.
 */
@Component
public class AlcanceRevisaoLore {

    /**
     * Comandos de desenho vetorial do ASS ({@code \p1}…): a "fala" é uma sequência de coordenadas,
     * não texto. Já custou 2.062 falsos "cartazes" numa medição deste projeto.
     */
    private static final Pattern PADRAO_DESENHO_VETORIAL = Pattern.compile("\\\\p[1-9]\\d*");

    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;
    private final MascaradorTags mascarador;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne os DONOS das regras que delimitam o alcance da 3.2.
     * <p>INVARIANTES DO DOMÍNIO: nenhuma regra é reimplementada aqui; todas são consultadas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a inicialização.
     */
    public AlcanceRevisaoLore(
        PoliticaEstiloMusical politicaEstiloMusical,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss,
        MascaradorTags mascarador
    ) {
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
        this.mascarador = mascarador;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a tela 3.2 pode olhar — e, portanto, reescrever — esta linha.
     *
     * <p>INVARIANTES DO DOMÍNIO: recusa linha que não seja {@code Dialogue}, sem texto, de estilo
     * musical/ignorado, de desenho vetorial, de typesetting protegido, de karaokê não traduzível,
     * ou sem nada traduzível depois de mascaradas as tags.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula ou campo ausente devolve {@code false}.
     *
     * @param evento a linha a julgar
     * @return {@code true} somente quando a linha é diálogo comum da obra
     */
    public boolean estaNoAlcance(EventoLegenda evento) {
        if (evento == null || !evento.isDialogo() || !evento.temTexto()) {
            return false;
        }
        String texto = evento.texto();
        String estilo = evento.estilo();
        if (estilo == null || politicaEstiloMusical.estiloIgnorado(estilo)) {
            return false;
        }
        if (PADRAO_DESENHO_VETORIAL.matcher(texto).find()) {
            return false;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(estilo, texto)) {
            return false;
        }
        // LETREIRO nao e fala. A regra acima exige `clip` longo na ultima porta — ela nasceu do
        // karaoke com {\clip(m ... l ...)} do Zeta — e por isso deixava passar cartaz posicionado
        // sem clip. Medido em 17/08/2026: 114 linhas de \fs>=100 chegavam a esta tela em 232
        // arquivos, TODAS ja traduzidas corretamente ("Elas chamaram isso de Gundam", "Proximo
        // episodio"), so produzindo ruido. O veto e AQUI e nao no dono da regra porque a
        // TRADUCAO precisa mandar cartaz ao modelo — e por isso os letreiros do acervo estao em
        // portugues. A camada resolve o problema dela.
        if (protecaoAss.ehLetreiroDeCartaz(estilo, texto)) {
            return false;
        }
        if (detectorKaraoke.eEfeitoKaraoke(texto)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(estilo, texto)) {
            return false;
        }
        return mascarador.contemTextoTraduzivel(texto);
    }
}
