package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Detecta dano de tradução em karaokê/música comparando cada evento traduzido
 * com o original. Usa o {@link DetectorEfeitoKaraokeService} como fonte única
 * de verdade, a mesma régua da tradução, correção e revisão.
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
            boolean protegido = detectorKaraoke.devePreservarKaraokeOriginal(eventoOrig.estilo(), textoOrig);
            boolean musicaTraduzivel = detectorKaraoke.eKaraokeOuMusicaTraduzivel(eventoOrig.estilo(), textoOrig);
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
