package org.traducao.projeto.traducao.application.contextocena;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.application.ClassificadorPendenciaTelemetria;
import org.traducao.projeto.traducao.domain.CategoriaConteudo;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PROPÓSITO DE NEGÓCIO: monta a "janela contextual de diálogo" de uma fala-alvo — reúne suas
 * vizinhas imediatas ELEGÍVEIS (só diálogo narrativo) antes e depois, para dar ao modelo a
 * cena em volta e permitir inferir o falante/gênero que a fonte não declara. É a peça que
 * ataca a raiz da inversão de gênero do 08th MS Team; ainda NÃO está ligada ao pipeline (a
 * fiação vem em subfase posterior, sob a flag desligada).
 *
 * <p>INVARIANTES DO DOMÍNIO: elegibilidade usa a MESMA classificação do pipeline
 * ({@link ClassificadorPendenciaTelemetria#categoria} {@code == DIALOGO}), excluindo Comment,
 * karaokê/KFX, romaji preservado, música e letreiro; falas não elegíveis são PULADAS (não
 * encerram a janela), então a janela junta as vizinhas de diálogo mais próximas, ignorando
 * typesetting no meio. As vizinhas de {@code antes} saem em ordem cronológica (documento);
 * cada lado é limitado a {@code tamanhoJanela} falas. Só a fala-alvo será traduzida — as
 * vizinhas são referência. Como {@link EventoLegenda} não tem tempo nem fronteira de cena,
 * isto é VIZINHANÇA, não reconhecimento real de cena (limitação declarada).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@code eventos} nulo lança {@link NullPointerException};
 * {@code indiceAlvo} fora da faixa lança {@link IndexOutOfBoundsException}; {@code tamanhoJanela}
 * negativo é normalizado para 0 (janela só com a fala-alvo). Nenhum I/O.
 */
@Service
public class MontadorJanelaContextual {

    private final ClassificadorPendenciaTelemetria classificador;

    public MontadorJanelaContextual(ClassificadorPendenciaTelemetria classificador) {
        this.classificador = classificador;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: constrói a janela contextual da fala na posição {@code indiceAlvo}.
     * <p>INVARIANTES DO DOMÍNIO: vizinhas apenas de diálogo elegível, no máximo
     * {@code tamanhoJanela} de cada lado, {@code antes} em ordem cronológica; a fala-alvo é
     * incluída como está (o chamador decide quais alvos ganham janela).
     * <p>COMPORTAMENTO EM CASO DE FALHA: ver contrato da classe (NPE/IndexOutOfBounds).
     *
     * @param eventos lista ordenada de eventos da legenda ({@code documento.eventos()})
     * @param indiceAlvo posição da fala-alvo dentro de {@code eventos}
     * @param tamanhoJanela número máximo de vizinhas elegíveis por lado
     * @return a {@link JanelaContextual} da fala-alvo
     */
    public JanelaContextual montar(List<EventoLegenda> eventos, int indiceAlvo, int tamanhoJanela) {
        Objects.requireNonNull(eventos, "eventos");
        if (indiceAlvo < 0 || indiceAlvo >= eventos.size()) {
            throw new IndexOutOfBoundsException("indiceAlvo fora da faixa: " + indiceAlvo
                + " (tamanho " + eventos.size() + ")");
        }
        int janela = Math.max(0, tamanhoJanela);
        LinhaAlvoContextual alvo = mapear(eventos.get(indiceAlvo));

        List<LinhaAlvoContextual> antes = new ArrayList<>();
        for (int i = indiceAlvo - 1; i >= 0 && antes.size() < janela; i--) {
            EventoLegenda e = eventos.get(i);
            if (elegivel(e)) {
                antes.add(mapear(e));
            }
        }
        Collections.reverse(antes); // coletado de trás para frente → devolve em ordem cronológica

        List<LinhaAlvoContextual> depois = new ArrayList<>();
        for (int i = indiceAlvo + 1; i < eventos.size() && depois.size() < janela; i++) {
            EventoLegenda e = eventos.get(i);
            if (elegivel(e)) {
                depois.add(mapear(e));
            }
        }
        return new JanelaContextual(alvo, antes, depois);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um evento vizinho conta como diálogo narrativo elegível.
     * <p>INVARIANTES DO DOMÍNIO: precisa ser linha {@code Dialogue} (não Comment), ter texto
     * não vazio e ser classificado como {@link CategoriaConteudo#DIALOGO} — a mesma regra que
     * o pipeline usa para separar diálogo de karaokê/romaji/música/letreiro.
     * <p>COMPORTAMENTO EM CASO DE FALHA: evento nulo devolve {@code false}, sem lançar.
     */
    private boolean elegivel(EventoLegenda e) {
        return e != null
            && e.isDialogo()
            && e.temTexto()
            && e.texto() != null && !e.texto().isBlank()
            && classificador.categoria(e.estilo(), e.texto()) == CategoriaConteudo.DIALOGO;
    }

    private static LinhaAlvoContextual mapear(EventoLegenda e) {
        return new LinhaAlvoContextual(e.indice(), e.estilo(), e.texto());
    }
}
