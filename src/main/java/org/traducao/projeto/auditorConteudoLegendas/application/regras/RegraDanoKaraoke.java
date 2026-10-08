package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PROPÓSITO DE NEGÓCIO: detecta dano de tradução em karaokê/música comparando cada evento
 * traduzido com o original. Usa o {@link DetectorEfeitoKaraokeService} como fonte única de
 * verdade, a mesma régua da tradução, correção e revisão.
 *
 * <p>INVARIANTES DO DOMÍNIO: letra japonesa/romaji alterada é CRITICAL; a camada que o fansub
 * declara inglesa ({@code ED - EN}, {@code Song ENG}) é tratada como música traduzível — é a
 * que a Tradução de Karaokê (4.1) traduz por decisão —, e nela só expansão anormal e tag
 * {@code \k} perdida são acusadas. Linha com texto no original que ficou só de tags/comentário
 * na tradução é ERROR (verso ou fala apagado) — o controle que cobre o que a regra de arquivo
 * único deixou de acusar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: evento sem par ou sem texto é ignorado; nunca lança.
 */
@ApplicationScoped
public class RegraDanoKaraoke implements RegraAuditoriaConteudo {

    private final DetectorEfeitoKaraokeService detectorKaraoke;

    public RegraDanoKaraoke(DetectorEfeitoKaraokeService detectorKaraoke) {
        this.detectorKaraoke = detectorKaraoke;
    }

    @Override
    public String getNome() {
        return "Dano Estrutural em Karaoke/Musica";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        Map<Integer, EventoLegenda> mapOriginal = original.eventos().stream()
            .collect(Collectors.toMap(EventoLegenda::indice, Function.identity(), (a, b) -> a));

        for (EventoLegenda eventoTrad : traduzido.eventos()) {
            if (!eventoTrad.isDialogo() || !eventoTrad.temTexto()) {
                continue;
            }

            EventoLegenda eventoOrig = mapOriginal.get(eventoTrad.indice());
            if (eventoOrig == null || !eventoOrig.temTexto()) {
                continue;
            }

            String textoOrig = eventoOrig.texto();
            // Controle compensatório (auditoria de 08/10, A8): a regra de arquivo único deixou de
            // acusar a linha só de tags em estilo de música e a nota só de comentário, porque no
            // ORIGINAL elas são o portador de efeito e a nota do fansub. Com o par na mão dá para
            // separar: se o original tinha texto, a tradução apagou o verso/a fala.
            if (TempoEventoUtil.ehLinhaSemFalaDeProposito(eventoTrad)
                && !extrairTextoVisivelAss(textoOrig).isEmpty()) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.ERROR,
                    getNome(),
                    "A linha tinha texto visível no original e ficou só com tags/comentário na tradução "
                        + "(verso ou fala apagado).",
                    eventoOrig,
                    eventoTrad,
                    "Restaure o texto da linha a partir do original e traduza de novo."
                ));
                continue;
            }
            // A camada que o fansub declara inglesa é a que a 4.1 traduz de propósito: não é
            // romaji, ainda que as palavras se decomponham em sílabas (auditoria de 08/10, A9).
            boolean camadaInglesa = detectorKaraoke.ehCamadaInglesaDeclarada(eventoOrig.estilo(), textoOrig)
                && detectorKaraoke.temIndicadorDeMusica(eventoOrig.estilo(), textoOrig);
            boolean protegido = !camadaInglesa
                && detectorKaraoke.devePreservarKaraokeOriginal(eventoOrig.estilo(), textoOrig);
            boolean musicaTraduzivel = camadaInglesa
                || detectorKaraoke.eKaraokeOuMusicaTraduzivel(eventoOrig.estilo(), textoOrig);
            if (!protegido && !musicaTraduzivel) {
                continue;
            }

            String visivelOriginal = extrairTextoVisivelAss(textoOrig);
            String visivelTraduzido = extrairTextoVisivelAss(eventoTrad.texto());

            if (protegido && !visivelOriginal.equals(visivelTraduzido)) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.CRITICAL,
                    getNome(),
                    "Karaoke japones/romaji foi alterado na traducao; deveria permanecer intacto.",
                    eventoOrig,
                    eventoTrad,
                    "Rode a Correcao de Karaoke para restaurar a linha original automaticamente."
                ));
                continue;
            }

            if (musicaTraduzivel && visivelOriginal.length() > 5
                && visivelTraduzido.length() > visivelOriginal.length() * 2.5) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "O texto da musica sofreu expansao anormal na traducao (possivel alucinacao do LLM).",
                    eventoOrig,
                    eventoTrad,
                    "Revise a linha; se for alucinacao, rode a Correcao de Karaoke."
                ));
            }

            if (detectorKaraoke.temTagKaraoke(textoOrig) && !detectorKaraoke.temTagKaraoke(eventoTrad.texto())) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "As tags de timing de karaoke (\\k) sumiram na traducao.",
                    eventoOrig,
                    eventoTrad,
                    "Restaure as tags originais pela Correcao de Karaoke."
                ));
            }
        }
        return anomalias;
    }

    private String extrairTextoVisivelAss(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("\\{[^}]+\\}", "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }
}
