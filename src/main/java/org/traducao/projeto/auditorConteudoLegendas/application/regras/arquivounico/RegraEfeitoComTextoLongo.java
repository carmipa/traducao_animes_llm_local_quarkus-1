package org.traducao.projeto.auditorConteudoLegendas.application.regras.arquivounico;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: versão de arquivo único da caça a "efeito vazado". Uma
 * linha com tags de animação pesada (\t, \move, \clip, \fad) normalmente é um
 * efeito visual curto; se ela carrega texto visível longo, é forte indício de
 * que uma sentença completa vazou para dentro de um evento de efeito.
 *
 * <p>INVARIANTES DO DOMÍNIO: só eventos {@code Dialogue} com texto e com tag de
 * animação pesada são avaliados; o alerta exige texto visível acima de
 * {@value #LIMITE_TEXTO_VISIVEL} caracteres para evitar ruído.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: eventos sem tags de animação ou sem texto
 * são ignorados; a regra nunca lança.
 */
@ApplicationScoped
public class RegraEfeitoComTextoLongo implements RegraAuditoriaArquivoUnico {

    private static final Pattern TAG_ANIMACAO_PESADA = Pattern.compile("\\\\(t\\(|move\\(|clip\\(|fad\\()");
    private static final int LIMITE_TEXTO_VISIVEL = 60;

    @Override
    public String getNome() {
        return "Efeito Visual com Texto Longo (possível vazamento)";
    }

    @Override
    public List<AnomaliaConteudo> auditar(DocumentoLegenda documento) {
        List<AnomaliaConteudo> anomalias = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            String texto = evento.texto();
            if (!TAG_ANIMACAO_PESADA.matcher(texto).find()) {
                continue;
            }
            String visivel = textoVisivel(texto);
            if (visivel.length() > LIMITE_TEXTO_VISIVEL) {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.WARNING,
                    getNome(),
                    "Linha com efeito de animação pesada carrega " + visivel.length()
                        + " caracteres visíveis. Pode ser uma sentença que vazou para um evento de efeito.",
                    evento,
                    null,
                    "Verificar se o texto pertence a esta linha de efeito ou se deveria ser um diálogo separado."
                ));
            }
        }
        return anomalias;
    }

    private String textoVisivel(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }
}
