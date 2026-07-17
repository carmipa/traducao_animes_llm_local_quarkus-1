package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;

import java.util.HashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: decide quais falas de uma legenda devem ir ao LLM, blindando
 * typesetting pesado (karaokê cru, desenho vetorial, letreiros animados quadro a quadro)
 * que a tradução destruiria — isolando essa elegibilidade da orquestração de
 * {@link ProcessarArquivoUseCase} (FASE F, R4).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só falas de diálogo com texto visível são candidatas; estilo musical ignorado só
 *       passa quando o detector de karaokê o reconhece como música traduzível.</li>
 *   <li>Karaokê cru, desenho vetorial ({@code \p1}) e typesetting de alto risco são
 *       bloqueados antes do LLM.</li>
 *   <li>Um letreiro animado é reconhecido pela CONJUNÇÃO tag de efeito pesada + pouco
 *       texto visível + repetição do mesmo texto >= {@value #LIMIAR_REPETICAO_LETREIRO} no
 *       arquivo; a repetição é o sinal decisivo para não descartar por engano uma fala
 *       isolada com efeito visual.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Métodos puros sobre o documento em memória, sem I/O nem exceção: na dúvida a decisão
 * pende para o lado seguro do typesetting — texto sem parte visível ou de alto risco é
 * excluído da tradução, preservando o efeito original.
 */
@Component
public class SeletorEventosTraduziveis {

    private static final Logger log = LoggerFactory.getLogger(SeletorEventosTraduziveis.class);

    // Um letreiro/título animado quadro a quadro reaparece muitas vezes com o
    // mesmo texto visível (só a tag de efeito muda a cada quadro). Abaixo
    // disso é mais provável ser só uma fala com efeito visual pontual (ex.:
    // duas camadas contorno+preenchimento de uma mesma linha de encerramento).
    private static final int LIMIAR_REPETICAO_LETREIRO = 5;

    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;
    private final MascaradorTags mascarador;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as blindagens que decidem a elegibilidade de uma fala —
     * política de estilo musical, detector de karaokê, proteção ASS e mascarador.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param politicaEstiloMusical define quais estilos musicais são, por padrão, ignorados
     * @param detectorKaraoke reconhece karaokê cru e música traduzível
     * @param protecaoAss extrai texto visível e bloqueia typesetting de alto risco
     * @param mascarador decide, ao final, se há texto realmente traduzível
     */
    public SeletorEventosTraduziveis(
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
     * PROPÓSITO DE NEGÓCIO: conta, por texto "limpo" (sem tags de override ASS), quantas
     * vezes ele aparece entre as falas de diálogo — a base para reconhecer letreiro
     * animado, que reaproveita o mesmo texto visível dezenas ou milhares de vezes.
     *
     * <p>INVARIANTES DO DOMÍNIO: só falas de diálogo com texto visível entram na contagem;
     * o texto recebido nunca é modificado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; documento sem falas de diálogo
     * devolve um mapa vazio.
     */
    public Map<String, Long> calcularFrequenciaTextoLimpo(DocumentoLegenda documento) {
        Map<String, Long> frequencia = new HashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            String textoLimpo = protecaoAss.textoVisivel(evento.texto());
            if (!textoLimpo.isEmpty()) {
                frequencia.merge(textoLimpo, 1L, Long::sum);
            }
        }
        return frequencia;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma fala deve ser enviada ao LLM ou preservada como
     * está, aplicando em ordem as blindagens de estilo musical, karaokê, desenho vetorial,
     * typesetting de alto risco e letreiro animado.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada blindagem só exclui a fala quando seus sinais
     * conjuntos se confirmam; a repetição do texto visível (via {@code frequenciaTextoLimpo})
     * é o gatilho decisivo do bloqueio de letreiro animado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; a decisão final recai sobre
     * {@code mascarador.contemTextoTraduzivel}, garantindo que só texto realmente
     * traduzível chegue ao LLM.
     */
    public boolean isTraduzivel(EventoLegenda evento, Map<String, Long> frequenciaTextoLimpo) {
        if (!evento.isDialogo() || !evento.temTexto()) {
            return false;
        }
        String texto = evento.texto();
        if (politicaEstiloMusical.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return false;
        }

        // Blindagem contra karaokê cru (\k, \kf, \ko). Só as tags de timing:
        // a detecção agressiva de pós-template (eEfeitoKaraoke) pegaria também
        // letreiros/títulos com \t e texto curto, que aqui DEVEM ser traduzidos
        // — o caso karaokê pós-template é coberto pela heurística de letreiro
        // animado logo abaixo (que exige repetição).
        if (detectorKaraoke.devePreservarKaraokeOriginal(evento.estilo(), texto)) {
            return false;
        }

        // 1. Blindagem Contra Lixo Vetorial Absoluto (modo de desenho \p1, \p2, ... do Aegisub)
        if (protecaoAss.temDesenhoVetorial(texto)) {
            return false;
        }

        String textoLimpo = protecaoAss.textoVisivel(texto);
        if (textoLimpo.isEmpty()) {
            return false;
        }
        long repeticoes = frequenciaTextoLimpo.getOrDefault(textoLimpo, 1L);
        if (protecaoAss.deveBloquearLinhaAntesDoLlm(evento.estilo(), texto, repeticoes)) {
            log.debug("Bloqueando evento de typesetting de alto risco antes do LLM. Repetido {}x. Estilo: {} Texto: {}",
                repeticoes, evento.estilo(), textoLimpo);
            return false;
        }

        // 2. Blindagem Contra Typesetting Dinâmico (letreiros/títulos animados quadro a quadro):
        // tag de efeito pesada + pouco texto visível + o mesmo texto repetido muitas vezes no
        // arquivo. A repetição é o sinal decisivo: sem ela, uma fala isolada com efeito visual
        // (ex.: a camada de contorno de uma legenda de encerramento) seria descartada por engano.
        boolean temTagDeAnimacao = texto.contains("\\clip") || texto.contains("\\move")
            || texto.contains("\\pos") || texto.contains("\\fad") || texto.contains("\\t(");
        if (temTagDeAnimacao && texto.length() > 40 && textoLimpo.length() * 3 < texto.length()) {
            if (repeticoes >= LIMIAR_REPETICAO_LETREIRO) {
                log.debug("Bloqueando evento suspeito de letreiro animado (repetido {}x). Estilo: {} Texto: {}",
                    repeticoes, evento.estilo(), textoLimpo);
                return false;
            }
        }

        return mascarador.contemTextoTraduzivel(texto);
    }
}
