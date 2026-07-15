package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class RegraSincroniaEstilos implements RegraAuditoriaConteudo {

    private static final Pattern STYLE_PATTERN = Pattern.compile("(?i)^Style:\\s*([^,]+)", Pattern.MULTILINE);

    @Override
    public String getNome() {
        return "Sincronia de Estilos [V4+ Styles]";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda original, DocumentoLegenda traduzido) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        
        Set<String> estilosOriginais = extrairEstilos(original.cabecalho());
        Set<String> estilosTraduzidos = extrairEstilos(traduzido.cabecalho());

        for (String estiloOrig : estilosOriginais) {
            if (!estilosTraduzidos.contains(estiloOrig)) {
                anomalias.add(new AnomaliaConteudo(
                        AnomaliaConteudo.TipoSeveridade.ERROR,
                        getNome(),
                        "O estilo '" + estiloOrig + "' desapareceu no arquivo traduzido.",
                        null,
                        null,
                        "Copiar cabeçalho integralmente."
                ));
            }
        }

        return anomalias;
    }

    private Set<String> extrairEstilos(String cabecalho) {
        Set<String> estilos = new HashSet<>();
        if (cabecalho == null) return estilos;
        
        Matcher m = STYLE_PATTERN.matcher(cabecalho);
        while (m.find()) {
            estilos.add(m.group(1).trim());
        }
        return estilos;
    }
}
