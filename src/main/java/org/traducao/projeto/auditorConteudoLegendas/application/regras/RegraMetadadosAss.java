package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class RegraMetadadosAss implements RegraAuditoriaConteudo {

    private static final Pattern PLAYRES_X_PATTERN = Pattern.compile("(?i)^PlayResX:\\s*(\\d+)", Pattern.MULTILINE);
    private static final Pattern PLAYRES_Y_PATTERN = Pattern.compile("(?i)^PlayResY:\\s*(\\d+)", Pattern.MULTILINE);

    @Override
    public String getNome() {
        return "Validação de Metadados Críticos (Resolução)";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        
        String resXOriginal = extrairValor(original.cabecalho(), PLAYRES_X_PATTERN);
        String resYOriginal = extrairValor(original.cabecalho(), PLAYRES_Y_PATTERN);
        
        String resXTrad = extrairValor(traduzido.cabecalho(), PLAYRES_X_PATTERN);
        String resYTrad = extrairValor(traduzido.cabecalho(), PLAYRES_Y_PATTERN);

        if (resXOriginal != null && !resXOriginal.equals(resXTrad)) {
            anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.CRITICAL,
                    getNome(),
                    "Cabeçalho PlayResX alterado. Original: " + resXOriginal + ", Traduzido: " + (resXTrad == null ? "ausente" : resXTrad),
                    null,
                    null,
                    "Restaurar PlayResX original."
            ));
        }

        if (resYOriginal != null && !resYOriginal.equals(resYTrad)) {
            anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.CRITICAL,
                    getNome(),
                    "Cabeçalho PlayResY alterado. Original: " + resYOriginal + ", Traduzido: " + (resYTrad == null ? "ausente" : resYTrad),
                    null,
                    null,
                    "Restaurar PlayResY original."
            ));
        }

        return anomalias;
    }

    private String extrairValor(String cabecalho, Pattern padrao) {
        if (cabecalho == null) return null;
        Matcher m = padrao.matcher(cabecalho);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
