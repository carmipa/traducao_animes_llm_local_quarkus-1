package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: aponta linhas com número anormal de quebras {@code \N}
 * numa mesma fala. Sem arquivo de referência não dá para comparar com o original,
 * então esta é a heurística de "formatação quebrada / alucinação" para arquivo
 * único — muitas quebras costumam destruir posicionamento e legibilidade.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto entram; o
 * limite mínimo para alerta é {@value #LIMITE_QUEBRAS} quebras na mesma linha.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo é ignorado; a regra nunca lança.
 */
@ApplicationScoped
public class RegraQuebrasLinhaExcessivas implements RegraAuditoriaArquivoUnico {

    private static final int LIMITE_QUEBRAS = 4;

    @Override
    public String getNome() {
        return "Quebras de Linha Excessivas (\\N)";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            int quebras = contarOcorrencias(evento.texto(), "\\N");
            if (quebras >= LIMITE_QUEBRAS) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "Linha com " + quebras + " quebras de linha (\\N). Excesso de quebras costuma indicar formatação quebrada ou alucinação de IA.",
                    evento,
                    null,
                    "Revisar a linha; reduzir para no máximo uma ou duas quebras."
                ));
            }
        }
        return anomalias;
    }

    private int contarOcorrencias(String texto, String padrao) {
        int count = 0;
        int idx = 0;
        while ((idx = texto.indexOf(padrao, idx)) != -1) {
            count++;
            idx += padrao.length();
        }
        return count;
    }
}
