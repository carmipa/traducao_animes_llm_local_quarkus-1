package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService.ProtecaoCamadas;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.ClassificacaoCamadas;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.ClassificadorCamadaMusicalPort;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: única classe da fatia {@code trocaTipoLegenda} que conhece o
 * peer {@code legenda}. Traduz as regras de karaokê daquele peer para a resposta que o
 * achatamento precisa, mantendo o {@code application} isolado de mudanças na regra
 * alheia.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Camada ORIGINAL (romaji/japonês) entra em {@code preservarIntacto}: achatá-la a
 *       jogaria no rodapé, em cima da fala, e dissolveria a pista que separa as duas
 *       camadas do karaokê.</li>
 *   <li>Sílaba de timing só é reconhecida DENTRO de linha musical. Fora dela, texto
 *       curto sem espaço é diálogo legítimo ("Sim!", "Banagher!") e jamais é descartado
 *       — essa restrição é o que impede a regra de comer fala.</li>
 *   <li>Uma linha nunca é preservada E sílaba ao mesmo tempo: preservar tem precedência,
 *       porque manter erra a favor do conteúdo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Documento nulo ou serviços ausentes devolvem {@link ClassificacaoCamadas#VAZIA}; o
 * achatamento então se comporta como antes de existir a classificação.
 */
@Component
public class ClassificadorCamadaMusicalLegendaAdapter implements ClassificadorCamadaMusicalPort {

    /**
     * A camada de timing de um karaokê quebra a frase em pedaços de uma "palavra" por
     * linha, cada um com seu \k/\pos. Medido no Gundam Unicorn RE:0096 em 2026-07-29: no
     * estilo OPL2 do ep.1 são 17 linhas com a frase inteira ("Do you feel alone") contra
     * 138 pedaços soltos ("Do", "you", "feel", "a", "lone"). Achatá-los para Default —
     * como a fatia fazia — produz 138 legendas brancas piscando uma palavra por vez sobre
     * o vídeo, que na TV é pior que a tag animada que se queria remover.
     */
    private static final Pattern TAGS_ASS = Pattern.compile("\\{[^}]*\\}");

    /**
     * Nome de estilo de abertura/encerramento reconhecido POR ESTA FATIA. É duplicação
     * consciente do conceito que o peer {@code legenda} também tem — e existe porque
     * herdar a decisão dele trouxe o defeito dele junto: o
     * {@code ESTILO_MUSICA_AMPLO_PATTERN} usa lookaround de LETRA
     * ({@code (?<!\p{L})(op|ed|...)(?!\p{L})}), então "OP2" e "ED_S2" casam mas
     * <b>"OPL2" não</b> — o "L" depois de "OP" é letra. OPL2 é exatamente o estilo do
     * Gundam Unicorn RE:0096, com 3.410 linhas no acervo, e sem este padrão as sílabas
     * dele passavam intactas para o achatamento (medido em 2026-07-29: 141 legendas de
     * uma palavra sobreviviam ao descarte).
     *
     * <p>Aqui o critério pode ser mais largo com segurança: o veto do peer decide o que
     * é TRADUZIDO, e alargá-lo mudaria as 68 obras; este decide apenas o que é
     * DESCARTADO no achatamento, e só age em conjunto com "texto sem espaço".
     */
    private static final Pattern ESTILO_ABERTURA_OU_ENCERRAMENTO =
        Pattern.compile("(?i)^(op|ed)(?![a-z])|^(op|ed)[a-z]?\\d");

    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoCamadasMusicaisService protecaoCamadasMusicais;

    public ClassificadorCamadaMusicalLegendaAdapter(
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoCamadasMusicaisService protecaoCamadasMusicais
    ) {
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoCamadasMusicais = protecaoCamadasMusicais;
    }

    @Override
    public ClassificacaoCamadas classificar(DocumentoLegenda documento) {
        if (documento == null || documento.eventos() == null) {
            return ClassificacaoCamadas.VAZIA;
        }
        ProtecaoCamadas protecao = protecaoCamadasMusicais == null
            ? ProtecaoCamadas.VAZIA
            : protecaoCamadasMusicais.calcular(documento);

        Set<Integer> preservar = new HashSet<>();
        Set<Integer> silabas = new HashSet<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (evento == null || !evento.isDialogo()) {
                continue;
            }
            if (protecao.protege(evento.indice())
                || (detectorKaraoke != null
                    && detectorKaraoke.devePreservarKaraokeOriginal(evento.estilo(), evento.texto()))) {
                preservar.add(evento.indice());
                continue;
            }
            if (ehSilabaDeTiming(evento)) {
                silabas.add(evento.indice());
            }
        }
        return new ClassificacaoCamadas(preservar, silabas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece o pedaço de frase que só existe para o efeito de
     * karaokê acender no tempo certo.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige as DUAS condições — a linha ser de camada musical
     * (pelo estilo/conteúdo, decisão do peer {@code legenda}) E o texto visível não ter
     * espaço. A primeira condição é o que confina a regra ao karaokê; sem ela, "Sim!" e
     * "Banagher!" seriam descartados como sílaba.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo/vazio ou detector ausente devolve
     * {@code false} — na dúvida a linha permanece.
     */
    private boolean ehSilabaDeTiming(EventoLegenda evento) {
        if (!ehLinhaMusical(evento)) {
            return false;
        }
        String visivel = TAGS_ASS.matcher(evento.texto() == null ? "" : evento.texto())
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\h", " ")
            .trim();
        return !visivel.isEmpty() && !visivel.contains(" ");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se a linha pertence a uma camada de música, aceitando
     * o veredito do peer {@code legenda} OU o reconhecimento próprio desta fatia pelo
     * nome do estilo.
     *
     * <p>INVARIANTES DO DOMÍNIO: basta um dos dois sinais. O do peer cobre "Song JP",
     * "Insert Song", "OP - Romaji"; o próprio cobre a forma "OP/ED colado a letra ou
     * dígito" ({@code OPL2}, {@code OP2}, {@code ED_S2}) que o padrão do peer não
     * alcança.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: estilo nulo devolve {@code false}.
     */
    private boolean ehLinhaMusical(EventoLegenda evento) {
        if (detectorKaraoke != null
            && detectorKaraoke.podeSerCamadaMusical(evento.estilo(), evento.texto())) {
            return true;
        }
        String estilo = evento.estilo();
        return estilo != null && ESTILO_ABERTURA_OU_ENCERRAMENTO.matcher(estilo.trim()).find();
    }
}
