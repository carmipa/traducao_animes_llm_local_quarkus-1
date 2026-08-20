package org.traducao.projeto.traducaoKaraoke.application;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.SinaisDeKaraoke;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: decide a classe de TODOS os eventos de um arquivo de legenda de uma vez
 * só, e passa a ser o único lugar que sabe ler do ASS as duas evidências externas de karaokê —
 * o campo {@code Effect} e a existência de camada romaji no mesmo instante.
 *
 * <h2>O desperdício que originou, medido</h2>
 * {@code TraduzirKaraokeUseCase} percorria o documento DUAS vezes e chamava
 * {@code classificar} nas duas — uma no pré-passe que descobre os instantes com romaji, outra no
 * laço que emite os eventos. No acervo isso é <b>3,95 milhões de classificações onde 1,98 milhão
 * basta</b>. Nenhum JIT conserta trabalho feito duas vezes de propósito.
 *
 * <p>E o ganho maior nem é esse: com a decisão isolada aqui, mexer no critério de música deixa de
 * exigir tocar num método que também grava cache e escreve arquivo.
 *
 * <h2>A ORDEM dos dois passes, que não é arbitrária</h2>
 * O pré-passe classifica com <b>apenas</b> o campo {@code Effect} — nunca com a evidência de
 * "romaji no mesmo instante". Se ele usasse, a regra se alimentaria da própria conclusão e uma
 * linha puxaria a vizinha para dentro da música em cascata. Só o passe final recebe as duas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A classe é indexada por POSIÇÃO no documento, não por igualdade de evento: duas linhas
 *       idênticas em instantes diferentes são eventos diferentes, e karaokê é cheio delas — no
 *       86 a mesma frase aparece 650 vezes.</li>
 *   <li>Evento que não é diálogo ou não tem texto é {@link ClasseLinhaKaraoke#FORA_DE_MUSICA},
 *       sem consultar o classificador.</li>
 *   <li>É imutável depois de montado. Quem consulta não pode mudar a decisão.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Índice fora da faixa devolve {@code FORA_DE_MUSICA} — o lado que não traduz e não altera.
 * Prefixo malformado devolve evidência ausente, que é o lado restritivo. Nunca lança.
 */
public final class PlanoDeClassificacao {

    private final List<ClasseLinhaKaraoke> classePorPosicao;
    private final Set<String> instantesComOriginalPreservada;

    private PlanoDeClassificacao(List<ClasseLinhaKaraoke> classePorPosicao,
                                 Set<String> instantesComOriginalPreservada) {
        this.classePorPosicao = List.copyOf(classePorPosicao);
        this.instantesComOriginalPreservada = Set.copyOf(instantesComOriginalPreservada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o plano do arquivo inteiro — dois passes, uma decisão por
     * evento.
     *
     * <p>INVARIANTES DO DOMÍNIO: o pré-passe usa SÓ o campo {@code Effect}; o passe final usa
     * {@code Effect} mais o pareamento por instante. Ver a nota de ORDEM no topo da classe.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: documento nulo devolve um plano vazio, que responde
     * {@code FORA_DE_MUSICA} para tudo.
     */
    public static PlanoDeClassificacao montar(DocumentoLegenda documento,
                                              ClassificadorLetraKaraokeService classificador) {
        if (documento == null || documento.eventos() == null) {
            return new PlanoDeClassificacao(List.of(), Set.of());
        }
        List<EventoLegenda> eventos = documento.eventos();

        Set<String> comRomaji = new HashSet<>();
        for (EventoLegenda ev : eventos) {
            if (!classificavel(ev)) {
                continue;
            }
            ClasseLinhaKaraoke previa = classificador.classificar(
                ev.estilo(), ev.texto(), new SinaisDeKaraoke(campoEfeitoDe(ev), false));
            if (previa == ClasseLinhaKaraoke.ORIGINAL_JAPONES) {
                comRomaji.add(instanteDe(ev));
            }
        }

        List<ClasseLinhaKaraoke> classes = new ArrayList<>(eventos.size());
        for (EventoLegenda ev : eventos) {
            if (!classificavel(ev)) {
                classes.add(ClasseLinhaKaraoke.FORA_DE_MUSICA);
                continue;
            }
            classes.add(classificador.classificar(ev.estilo(), ev.texto(),
                new SinaisDeKaraoke(campoEfeitoDe(ev), comRomaji.contains(instanteDe(ev)))));
        }
        return new PlanoDeClassificacao(classes, comRomaji);
    }

    /** A decisão já tomada para o evento naquela posição do documento. */
    public ClasseLinhaKaraoke classeNaPosicao(int posicao) {
        if (posicao < 0 || posicao >= classePorPosicao.size()) {
            return ClasseLinhaKaraoke.FORA_DE_MUSICA;
        }
        return classePorPosicao.get(posicao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: já existe uma camada com a letra ORIGINAL neste instante?
     *
     * <p>É a pergunta que decide se a tradução precisa empilhar a original com {@code \N}. O
     * prejuízo de errar está medido: nos episódios 13-22 do Unicorn o encerramento tem UMA
     * camada só, e traduzir sem empilhar apagava a letra da tela — 22 de 23 linhas, em 10
     * episódios.
     */
    public boolean temOriginalPreservadaNoInstante(EventoLegenda evento) {
        return evento != null && instantesComOriginalPreservada.contains(instanteDe(evento));
    }

    /** Quantos eventos o plano decidiu — para o resumo por arquivo. */
    public int total() {
        return classePorPosicao.size();
    }

    private static boolean classificavel(EventoLegenda ev) {
        return ev != null && ev.isDialogo() && ev.temTexto();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code inicio,fim} do prefixo — a chave que identifica DUAS camadas
     * simultâneas como sendo do mesmo momento da música.
     *
     * <p>INVARIANTES DO DOMÍNIO: NÃO usar o prefixo inteiro. Ele carrega o ESTILO, e camadas
     * irmãs têm estilos diferentes por definição ({@code OP - Romaji} e {@code OP - English}) —
     * comparar o prefixo faria toda camada parecer solitária.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo ou curto devolve string vazia, e o evento é
     * tratado como sem irmã — o lado que PRESERVA a original.
     */
    static String instanteDe(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null) {
            return "";
        }
        String[] campos = evento.prefixo().split(",");
        return campos.length >= 3 ? campos[1] + "," + campos[2] : "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o campo {@code Effect} da linha {@code Dialogue:} — o carimbo que o
     * Kara Templater do Aegisub deixa nas linhas que ELE gera.
     *
     * <p>INVARIANTES DO DOMÍNIO: o formato é
     * {@code Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect}, e o campo é o NONO. O
     * {@code split} usa limite negativo porque campo vazio no meio é o caso NORMAL — sem ele o
     * Java descarta os vazios do fim e o índice escorrega.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo ou com menos de nove campos devolve
     * {@code null}, que {@code SinaisDeKaraoke} lê como "sem evidência".
     */
    static String campoEfeitoDe(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null) {
            return null;
        }
        String[] campos = evento.prefixo().split(",", -1);
        return campos.length >= 9 ? campos[8].trim() : null;
    }
}
