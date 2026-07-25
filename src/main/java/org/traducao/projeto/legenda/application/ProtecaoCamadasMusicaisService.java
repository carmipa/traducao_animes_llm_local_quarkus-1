package org.traducao.projeto.legenda.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Camada;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Janela;
import org.traducao.projeto.legenda.domain.PareamentoCamadasMusicais.Par;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: responde, UMA VEZ POR ARQUIVO, quais linhas são a camada ORIGINAL do
 * karaokê e portanto não podem ser traduzidas nem corrigidas. É o pré-passe que transforma a regra
 * pura de pareamento em uma resposta consultável pelas fatias — o proprietário único da pergunta
 * "isto é romaji?", para que tradutor, achatador e conversor parem de decidir cada um por si.
 *
 * <p>Por que uma vez por arquivo: o pareamento é uma propriedade do DOCUMENTO (duas linhas no mesmo
 * tempo), não do evento isolado. Segue o mesmo molde do cálculo de frequência de texto que a
 * Tradução Local já faz antes de percorrer os eventos.
 *
 * <p>INVARIANTES DO DOMÍNIO: só entram no pareamento eventos com indicador de música e com janela de
 * tempo legível no prefixo; diálogo comum nunca entra. A decisão de qual lado é o original é do
 * domínio ({@code PareamentoCamadasMusicais}) e pares indecisos NÃO protegem ninguém — mantêm o
 * comportamento anterior, que é o viés conservador do projeto.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: documento nulo, sem eventos ou sem pares devolve
 * {@link ProtecaoCamadas#VAZIA}, que não protege nada e deixa o pipeline idêntico ao de antes.
 * Nenhum método lança.
 */
@Service
public class ProtecaoCamadasMusicaisService {

    private final DetectorEfeitoKaraokeService detector;

    public ProtecaoCamadasMusicaisService(DetectorEfeitoKaraokeService detector) {
        this.detector = detector;
    }

    /**
     * Resposta do pré-passe: os índices de evento a preservar e o que foi observado no caminho.
     *
     * @param indicesPreservados índices de {@link EventoLegenda#indice()} da camada original
     * @param paresEncontrados quantos pares romaji × tradução foram reconhecidos
     * @param paresIndecisos pares reconhecidos em que os dois lados ficaram indistinguíveis
     */
    public record ProtecaoCamadas(Set<Integer> indicesPreservados, int paresEncontrados, int paresIndecisos) {

        /** Não protege nada — o pipeline se comporta exatamente como antes do pré-passe. */
        public static final ProtecaoCamadas VAZIA = new ProtecaoCamadas(Set.of(), 0, 0);

        public ProtecaoCamadas {
            indicesPreservados = Set.copyOf(indicesPreservados);
        }

        /** O evento deste índice é camada original de um par e não pode ser traduzido. */
        public boolean protege(int indice) {
            return indicesPreservados.contains(indice);
        }

        /** Nada foi pareado — útil para log e telemetria distinguirem "não achou" de "desligado". */
        public boolean vazia() {
            return indicesPreservados.isEmpty();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula a proteção do documento inteiro.
     *
     * <p>INVARIANTES DO DOMÍNIO: não altera o documento e não depende de ordem de chamada — é uma
     * função pura do conteúdo. Eventos que não são {@code Dialogue}, sem texto, sem indicador de
     * música ou sem janela de tempo legível são ignorados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer entrada degenerada devolve
     * {@link ProtecaoCamadas#VAZIA}; nunca lança.
     */
    public ProtecaoCamadas calcular(DocumentoLegenda documento) {
        if (documento == null || documento.eventos() == null) {
            return ProtecaoCamadas.VAZIA;
        }
        List<Camada> candidatas = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            camadaDe(evento).ifPresent(candidatas::add);
        }
        if (candidatas.size() < 2) {
            return ProtecaoCamadas.VAZIA;
        }

        List<Par> pares = PareamentoCamadasMusicais.parear(candidatas);
        Set<Integer> preservados = new HashSet<>();
        int indecisos = 0;
        for (Par par : pares) {
            Optional<Camada> original = par.camadaPreservar();
            if (original.isPresent()) {
                preservados.add(original.get().indice());
            } else {
                indecisos++;
            }
        }
        if (preservados.isEmpty() && pares.isEmpty()) {
            return ProtecaoCamadas.VAZIA;
        }
        return new ProtecaoCamadas(preservados, pares.size(), indecisos);
    }

    private Optional<Camada> camadaDe(EventoLegenda evento) {
        if (evento == null || !evento.temTexto() || evento.texto().isBlank()) {
            return Optional.empty();
        }
        // Critério LARGO de propósito: para reconhecer o par é preciso enxergar os dois lados, e a
        // camada traduzida do OP/ED usa nomes como ED_S2/OP_S2/OP2. Ser candidata não protege nada.
        if (!detector.podeSerCamadaMusical(evento.estilo(), evento.texto())) {
            return Optional.empty();
        }
        return PareamentoCamadasMusicais.janelaDoPrefixo(evento.prefixo())
            .map(janela -> montar(evento, janela));
    }

    private Camada montar(EventoLegenda evento, Janela janela) {
        return new Camada(
            evento.indice(),
            evento.estilo(),
            evento.texto(),
            janela.inicioCs(),
            janela.fimCs(),
            detector.eEstiloDeRomaji(evento.estilo()),
            detector.proporcaoRomaji(evento.texto()));
    }
}
