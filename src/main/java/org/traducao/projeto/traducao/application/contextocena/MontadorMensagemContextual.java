package org.traducao.projeto.traducao.application.contextocena;

import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;

import java.util.Objects;

/**
 * PROPÓSITO DE NEGÓCIO: monta a MENSAGEM-USUÁRIO de uma tradução contextual — apresenta as
 * falas vizinhas como REFERÊNCIA ("nao traduzir") e a fala-alvo como a única linha a
 * traduzir, com a instrução de não vazar o contexto e de usar formulação neutra quando o
 * gênero for incerto. É o formato que dá ao modelo a cena sem confundir contexto com alvo —
 * o coração da correção de gênero por contexto de cena. É determinístico e não fala com o
 * LLM (o adaptador é quem envia a mensagem depois).
 *
 * <p>INVARIANTES DO DOMÍNIO: a mensagem sempre traz a fala-alvo e a instrução final; as
 * seções de contexto anterior/posterior só aparecem quando há vizinhas; a ordem é
 * anterior → alvo → posterior; o texto segue o estilo dos prompts da fatia (sem acento) para
 * casar com {@code ContextoPrompt}. Serviço sem estado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@code janela} ou {@code janela.alvo()} nulos lançam
 * {@link NullPointerException} (uso indevido); nenhum I/O.
 */
@Service
public class MontadorMensagemContextual {

    private static final String INSTRUCAO_FINAL =
        "Traduza SOMENTE a fala-alvo, em uma unica linha. Nao traduza, nao repita e nao comente o contexto acima ou abaixo.\n"
        + "Se o genero do falante ou do interlocutor for incerto pela cena, use formulacao neutra em portugues do Brasil.";

    /**
     * PROPÓSITO DE NEGÓCIO: constrói a mensagem-usuário a partir da janela contextual.
     * <p>INVARIANTES DO DOMÍNIO: contexto rotulado "NAO traduzir"; alvo isolado; instrução
     * final sempre presente; seções de contexto omitidas quando vazias.
     * <p>COMPORTAMENTO EM CASO DE FALHA: NPE se {@code janela}/alvo nulos.
     *
     * @param janela janela contextual (fala-alvo + vizinhas)
     * @return a mensagem-usuário pronta para o papel {@code user} da chamada ao LLM
     */
    public String montarMensagemUsuario(JanelaContextual janela) {
        Objects.requireNonNull(janela, "janela");
        LinhaAlvoContextual alvo = Objects.requireNonNull(janela.alvo(), "janela.alvo");

        StringBuilder sb = new StringBuilder();
        if (!janela.antes().isEmpty()) {
            sb.append("Contexto anterior (NAO traduzir, apenas para entender a cena):\n");
            for (LinhaAlvoContextual l : janela.antes()) {
                sb.append(l.texto()).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Fala-alvo (traduza SOMENTE esta linha):\n");
        sb.append(alvo.texto());
        if (!janela.depois().isEmpty()) {
            sb.append("\n\n");
            sb.append("Contexto posterior (NAO traduzir, apenas para entender a cena):\n");
            for (LinhaAlvoContextual l : janela.depois()) {
                sb.append(l.texto()).append('\n');
            }
            // remove a quebra final da ultima vizinha para a instrucao colar limpa
            sb.setLength(sb.length() - 1);
        }
        sb.append("\n\n").append(INSTRUCAO_FINAL);
        return sb.toString();
    }
}
