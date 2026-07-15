package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: detecta diálogos que se sobrepõem no tempo — uma fala que
 * começa antes de a anterior terminar — apontando só sobreposições que realmente
 * colidem na tela. Karaokê, placas, efeitos, estilos diferentes e camadas
 * diferentes se sobrepõem por design e são ignorados para evitar milhares de
 * falsos positivos.
 *
 * <p>INVARIANTES DO DOMÍNIO: só entram eventos {@code Dialogue} de "diálogo comum"
 * (sem tags de karaokê, sem estilo de música, sem tags de posicionamento/efeito e
 * sem campo Effect preenchido); a colisão só é reportada entre eventos do MESMO
 * estilo e da MESMA camada (Layer), pois eles compartilham a mesma posição visual.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: eventos sem tempo interpretável ou de duração
 * inválida são ignorados (a duração inválida é tratada pela
 * {@code RegraTimestampInvalido}); a régua de karaokê/música é a mesma do resto do
 * pipeline ({@link DetectorEfeitoKaraokeService}).
 */
@ApplicationScoped
public class RegraSobreposicaoTempo implements RegraAuditoriaArquivoUnico {

    // Tags de posicionamento/efeito/animação: marcam placa ou letreiro, não diálogo comum.
    private static final Pattern TAG_POSICIONAMENTO_OU_EFEITO =
        Pattern.compile("\\\\(pos|move|i?clip|org|fade?|t)\\(");

    // Separador improvável em nomes de estilo, para compor a chave (layer + estilo).
    private static final String SEP_CHAVE = "";

    private final DetectorEfeitoKaraokeService detectorKaraoke;

    public RegraSobreposicaoTempo(DetectorEfeitoKaraokeService detectorKaraoke) {
        this.detectorKaraoke = detectorKaraoke;
    }

    private record EventoTempo(EventoLegenda evento, long inicio, long fim) {}

    @Override
    public String getNome() {
        return "Sobreposição de Tempo Entre Diálogos";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        // Agrupa apenas diálogos comuns por (camada + estilo). Sobreposições entre
        // grupos distintos, ou envolvendo karaokê/placas/efeitos, são intencionais.
        Map<String, List<EventoTempo>> grupos = new LinkedHashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!eDialogoComum(evento)) {
                continue;
            }
            long[] tempo = TempoEventoUtil.extrairInicioFimMs(evento);
            if (tempo == null || tempo[1] <= tempo[0]) {
                continue;
            }
            String chave = extrairLayer(evento) + SEP_CHAVE + estiloNormalizado(evento);
            grupos.computeIfAbsent(chave, k -> new ArrayList<>())
                .add(new EventoTempo(evento, tempo[0], tempo[1]));
        }

        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (List<EventoTempo> grupo : grupos.values()) {
            grupo.sort(Comparator.comparingLong(EventoTempo::inicio));
            long maiorFimAnterior = Long.MIN_VALUE;
            boolean primeiro = true;
            for (EventoTempo atual : grupo) {
                if (!primeiro && atual.inicio() < maiorFimAnterior) {
                    anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.WARNING,
                        getNome(),
                        "Este diálogo começa em " + atual.inicio()
                            + " ms, antes de a fala anterior de mesmo estilo e camada terminar em "
                            + maiorFimAnterior + " ms. As linhas se sobrepõem na tela.",
                        atual.evento(),
                        null,
                        "Ajustar início/fim para que diálogos do mesmo estilo e camada não se sobreponham."
                    ));
                }
                if (atual.fim() > maiorFimAnterior) {
                    maiorFimAnterior = atual.fim();
                }
                primeiro = false;
            }
        }
        return anomalias;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa diálogo falado real de karaokê, música, placas
     * e efeitos, que se sobrepõem de propósito.
     * <p>INVARIANTES DO DOMÍNIO: usa a mesma régua de karaokê/música do pipeline e
     * trata como não-diálogo qualquer linha com tag de posicionamento/efeito ou
     * campo Effect preenchido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: linha sem texto ou que não é diálogo é
     * descartada.
     */
    private boolean eDialogoComum(EventoLegenda evento) {
        if (!evento.isDialogo() || !evento.temTexto()) {
            return false;
        }
        String texto = evento.texto();
        String estilo = evento.estilo();
        if (detectorKaraoke.temTagKaraoke(texto)
            || detectorKaraoke.eEfeitoKaraoke(texto)
            || detectorKaraoke.eEstiloDeMusica(estilo)) {
            return false;
        }
        if (TAG_POSICIONAMENTO_OU_EFEITO.matcher(texto).find()) {
            return false;
        }
        return extrairEffect(evento).isBlank();
    }

    private String estiloNormalizado(EventoLegenda evento) {
        return evento.estilo() == null ? "" : evento.estilo().trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê a camada (Layer) do evento para separar linhas que
     * compartilham a mesma posição visual das que ficam em planos diferentes.
     * <p>INVARIANTES DO DOMÍNIO: o Layer é o 1º campo do prefixo ASS; SRT não tem
     * camadas e é tratado como camada 0.
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo ausente ou ilegível resulta em 0.
     */
    private int extrairLayer(EventoLegenda evento) {
        String prefixo = evento.prefixo();
        if (prefixo == null || prefixo.contains("-->")) {
            return 0;
        }
        int idxColon = prefixo.indexOf(':');
        String corpo = idxColon >= 0 ? prefixo.substring(idxColon + 1) : prefixo;
        String[] campos = corpo.split(",");
        if (campos.length == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(campos[0].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê o campo Effect do evento ASS (ex.: "Banner",
     * "Scroll up"), sinal claro de linha de efeito e não de diálogo.
     * <p>INVARIANTES DO DOMÍNIO: no Format V4+ padrão o Effect é o 9º campo do
     * prefixo; SRT não possui Effect.
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo fora do formato padrão devolve "".
     */
    private String extrairEffect(EventoLegenda evento) {
        String prefixo = evento.prefixo();
        if (prefixo == null || prefixo.contains("-->")) {
            return "";
        }
        int idxColon = prefixo.indexOf(':');
        String corpo = idxColon >= 0 ? prefixo.substring(idxColon + 1) : prefixo;
        String[] campos = corpo.split(",");
        return campos.length >= 9 ? campos[8].trim() : "";
    }
}
