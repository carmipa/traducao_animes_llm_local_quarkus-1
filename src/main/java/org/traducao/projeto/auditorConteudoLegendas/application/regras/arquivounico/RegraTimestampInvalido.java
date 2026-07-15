package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.auditorConteudoLegendas.domain.TempoEventoUtil;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza eventos cujo instante de fim é anterior ou igual
 * ao de início. Uma linha com duração zero ou negativa não aparece na tela e
 * costuma indicar corrupção de timestamps na legenda.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com tempo legível são
 * avaliados; a comparação usa milissegundos absolutos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: evento sem tempo interpretável é ignorado
 * (a regra {@link RegraTagOverrideNaoFechada} e as demais cobrem outros danos).
 */
@ApplicationScoped
public class RegraTimestampInvalido implements RegraAuditoriaArquivoUnico {

    @Override
    public String getNome() {
        return "Timestamp Inválido ou Ilegível";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo()) {
                continue;
            }
            TempoEventoUtil.Diagnostico d = TempoEventoUtil.diagnosticar(evento);
            AnomaliaConteudo anomalia = switch (d.status()) {
                case OK -> null;
                case FIM_ANTES_INICIO -> criar(AnomaliaConteudo.TipoSeveridade.ERROR, evento,
                    "Fim (" + d.fimMs() + " ms) menor ou igual ao início (" + d.inicioMs()
                        + " ms). A linha tem duração inválida e não será exibida.",
                    "Corrigir o timestamp de fim para depois do início.");
                case AUSENTE -> criar(AnomaliaConteudo.TipoSeveridade.CRITICAL, evento,
                    "Diálogo sem timestamp. A linha não pode ser exibida no tempo certo.",
                    "Adicionar o carimbo de tempo (início e fim) da fala.");
                case ILEGIVEL -> criar(AnomaliaConteudo.TipoSeveridade.CRITICAL, evento,
                    "Timestamp ilegível (sintaxe hh:mm:ss inválida). O parser não consegue posicionar a fala.",
                    "Corrigir o formato do tempo (ex.: 0:00:01.00 no ASS, 00:00:01,000 no SRT).");
                case FORA_INTERVALO -> criar(AnomaliaConteudo.TipoSeveridade.ERROR, evento,
                    "Timestamp com minutos ou segundos fora do intervalo (0–59).",
                    "Normalizar minutos/segundos para 0–59, ajustando as horas se necessário.");
                case SETA_SRT_INVALIDA -> criar(AnomaliaConteudo.TipoSeveridade.CRITICAL, evento,
                    "Linha de tempo SRT sem a seta '-->' válida entre início e fim.",
                    "Usar exatamente 'início --> fim' na linha de tempo do bloco SRT.");
                case INCOMPLETO -> criar(AnomaliaConteudo.TipoSeveridade.CRITICAL, evento,
                    "Linha Dialogue ASS incompleta: faltam os campos de início/fim.",
                    "Restaurar os campos Layer,Início,Fim,Style,... do evento.");
            };
            if (anomalia != null) {
                anomalias.add(anomalia);
            }
        }
        return anomalias;
    }

    private AnomaliaConteudo criar(AnomaliaConteudo.TipoSeveridade severidade, EventoLegenda evento,
                                   String descricao, String sugestao) {
        return new AnomaliaConteudo(severidade, getNome(), descricao, evento, null, sugestao);
    }
}
