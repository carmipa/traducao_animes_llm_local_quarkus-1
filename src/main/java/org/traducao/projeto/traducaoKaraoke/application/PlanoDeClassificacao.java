package org.traducao.projeto.traducaoKaraoke.application;

import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.SinaisDeKaraoke;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    /**
     * Folga em centésimos nos DOIS lados do casamento de instante entre o pedaço e a frase: o
     * fill de karaokê adianta e atrasa a linha por frações (medido no OPL2 do Unicorn — 0,15s no
     * início, 0,10s no fim). Um só valor para os dois lados, porque a assimetria era o defeito.
     */
    private static final long FOLGA_INSTANTE_CS = 100;

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

        // As sílabas de fill do KFX são calculadas ANTES do conjunto comRomaji, porque uma
        // sílaba solta engana a deteccao de original neste primeiro passe (que ainda nao tem o
        // sinal silabaDeFraseIrma). Medido em 21/09/2026 no OPL2 do Unicorn: o fragmento "I" de
        // "I know that all the lies..." e de "I wonder how long..." (a letra "i", que casa o
        // padrao de silaba japonesa) saia ORIGINAL_JAPONES aqui e marcava o instante da frase
        // como "tem original preservada" — a frase INGLESA no mesmo instante entao NAO empilhava
        // o ingles original, e a abertura saia so em PT (2 de 16 frases). A original de verdade
        // (romaji do ED) e FRASE, nunca fragmento, entao continua contando: o scar das 22/23
        // linhas do ED single-layer segue protegido.
        Set<Integer> silabas = posicoesDeSilaba(eventos);

        Set<String> comRomaji = new HashSet<>();
        for (int i = 0; i < eventos.size(); i++) {
            EventoLegenda ev = eventos.get(i);
            if (!classificavel(ev) || silabas.contains(i)) {
                continue;
            }
            ClasseLinhaKaraoke previa = classificador.classificar(
                ev.estilo(), ev.texto(), new SinaisDeKaraoke(campoEfeitoDe(ev), false));
            if (previa == ClasseLinhaKaraoke.ORIGINAL_JAPONES) {
                comRomaji.add(instanteDe(ev));
            }
        }

        List<ClasseLinhaKaraoke> classes = new ArrayList<>(eventos.size());
        for (int i = 0; i < eventos.size(); i++) {
            EventoLegenda ev = eventos.get(i);
            if (!classificavel(ev)) {
                classes.add(ClasseLinhaKaraoke.FORA_DE_MUSICA);
                continue;
            }
            classes.add(classificador.classificar(ev.estilo(), ev.texto(),
                new SinaisDeKaraoke(campoEfeitoDe(ev), comRomaji.contains(instanteDe(ev)),
                    silabas.contains(i))));
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
     * PROPÓSITO DE NEGÓCIO: acha as linhas que são PEDAÇO de uma frase que também está no
     * arquivo — karaokê pintado sílaba a sílaba com a letra inteira desenhada por cima.
     *
     * <h2>O prejuízo que originou</h2>
     * Ver {@link SinaisDeKaraoke#silabaDeFraseIrma()}: no {@code OPL2} do Unicorn as duas
     * camadas convivem, as duas iam ao LLM, e 78 dos 131 textos distintos traduzidos eram
     * fragmento — {@code cant} virou "Cantar.".
     *
     * <h2>O critério, e as DUAS armadilhas que ele já tropeçou</h2>
     * Os pedaços irmãos, ordenados por instante, têm de CONCATENAR exatamente no texto da frase
     * (normalizado para letra e dígito). {@code Do|you|feel|a|lone} reconstrói
     * {@code doyoufeelalone}; {@code i|to} não reconstrói nada.
     * <ul>
     *   <li><b>Substring não é evidência.</b> A primeira versão só exigia que o fragmento
     *       aparecesse na frase. Medido: trocando o texto pela letra {@code a}, 2.752 dos 2.835
     *       continuavam cobertos — e no 86 Part 2 os 875 "cobertos" eram a letra {@code i}.</li>
     *   <li><b>Cópia de tipografia reconstrói a si mesma.</b> A segunda versão marcou 75,6% do
     *       Zeta, incluindo as frases inteiras de {@code Song JP}, porque a mesma linha desenhada
     *       duas vezes "reconstruía" a frase sozinha. Por isso todo pedaço precisa ter MENOS
     *       palavras que a frase (a cópia da frase inteira tem o MESMO número e é barrada), e
     *       precisa haver pelo menos dois pedaços.</li>
     * </ul>
     *
     * <p><b>Correção de 2026-09-21:</b> o corte era "MENOS de DUAS palavras" e abortava o grupo
     * inteiro ao topar um pedaço de duas palavras — no {@code OPL2} do Unicorn o pedaço
     * {@code "you are"} derrubava os 9 fragmentos de <i>"If you are holding holding onto fear"</i>,
     * que iam ao LLM e viravam lixo ({@code hol}→"Olá", {@code on}→"começando", {@code dnt}→
     * meta-resposta). O corte passou a ser "MENOS palavras que a frase", que barra a cópia da
     * frase inteira (Zeta) e admite o pedaço de duas palavras (Unicorn). A concatenação exata
     * continua sendo a prova.
     *
     * <p>Efeito medido com o critério final: Unicorn 2.562 de 3.280 fragmentos (78,1%), e
     * <b>ZERO</b> no Zeta, no 86 Part 1 e no 86 Part 2 — nenhuma obra saudável é tocada.
     *
     * <p>INVARIANTES DO DOMÍNIO: a comparação é dentro do MESMO estilo. Camadas irmãs têm estilos
     * diferentes por definição, e cruzá-las faria a letra em inglês "reconstruir" o romaji.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo ilegível devolve tempo negativo e o evento fica
     * de fora — o lado que NÃO veta, preservando o viés de traduzir o que não se entendeu.
     */
    private static Set<Integer> posicoesDeSilaba(List<EventoLegenda> eventos) {
        Set<Integer> silabas = new HashSet<>();
        List<Integer> candidatos = new ArrayList<>();
        for (int i = 0; i < eventos.size(); i++) {
            if (classificavel(eventos.get(i)) && !visivelDe(eventos.get(i)).isEmpty()) {
                candidatos.add(i);
            }
        }
        for (int idFrase : candidatos) {
            EventoLegenda frase = eventos.get(idFrase);
            String textoFrase = visivelDe(frase);
            if (palavras(textoFrase) < 2) {
                continue;
            }
            long iniFrase = inicioCs(frase);
            long fimFrase = fimCs(frase);
            if (iniFrase < 0 || fimFrase < 0) {
                continue;
            }
            List<Integer> irmas = new ArrayList<>();
            for (int idIrma : candidatos) {
                if (idIrma == idFrase) {
                    continue;
                }
                EventoLegenda ev = eventos.get(idIrma);
                if (!Objects.equals(ev.estilo(), frase.estilo())) {
                    continue;
                }
                long ini = inicioCs(ev);
                long fim = fimCs(ev);
                // A âncora do pareamento é o FIM, não o início. Todo pedaço do fill de karaokê
                // termina JUNTO com a frase (fica aceso até a linha acabar), então casar pelo fim é
                // o que reconhece o pedaço e, ao mesmo tempo, exclui o fill da frase VIZINHA.
                // Medido em 21/09/2026 no OPL2 do Unicorn: em "We didnt see all its meaning" o
                // primeiro pedaço ("We") começa 0,15s ANTES da frase (39.38 < 39.53) — ancorar no
                // INÍCIO o excluía e os 8 fragmentos vazavam (dnt=>"Nao ha contexto", all=>"Eu sou a
                // Audrey"). Mas ancorar no início com folga puxava o ÚLTIMO pedaço da frase anterior
                // e poluía a reconstrução — medido, o leak subiu de 6 para 32. Pelo fim, "We"
                // termina com a frase (44.79 ≈ 44.69, dentro da folga) e é incluído, enquanto
                // "vive" (fim 39.23, o fill da frase anterior) fica a 5s do fim desta e é excluído.
                // A folga de 1s absorve os centésimos de diferença entre o held e o fill.
                if (ini > fimFrase
                    || fim < fimFrase - FOLGA_INSTANTE_CS || fim > fimFrase + FOLGA_INSTANTE_CS) {
                    continue;
                }
                irmas.add(idIrma);
            }
            if (irmas.size() < 2) {
                continue;
            }
            irmas.sort(Comparator.comparingLong((Integer id) -> inicioCs(eventos.get(id)))
                .thenComparingLong(id -> fimCs(eventos.get(id))));

            int palavrasFrase = palavras(textoFrase);
            StringBuilder reconstruido = new StringBuilder();
            Set<String> jaVistos = new HashSet<>();
            int pedacos = 0;
            boolean algumEhFrase = false;
            for (int id : irmas) {
                EventoLegenda ev = eventos.get(id);
                String visivel = visivelDe(ev);
                // O corte é "MENOS palavras que a frase", não "< 2 palavras". O fansub divide a
                // letra em pedaços que às vezes têm duas palavras ("you are" no OPL2 do Unicorn),
                // e o corte antigo abortava o grupo inteiro ao encontrá-los — os 9 pedaços de
                // "If you are holding holding onto fear" ficavam sem veto e iam ao LLM como
                // fragmento (medido 21/09/2026: hol=>"Olá", on=>"começando", dnt=>meta-resposta).
                // O que o corte precisa barrar é a CÓPIA DE TIPOGRAFIA — a frase inteira desenhada
                // duas vezes, que "reconstruía a si mesma" (Zeta Song JP): essa tem o MESMO número
                // de palavras da frase, então >= palavrasFrase continua abortando-a. A prova forte
                // segue sendo a concatenação exata abaixo, não o número de palavras do pedaço.
                if (palavras(visivel) >= palavrasFrase) {
                    algumEhFrase = true;
                    break;
                }
                String chave = inicioCs(ev) + "|" + fimCs(ev) + "|" + visivel;
                if (!jaVistos.add(chave)) {
                    continue; // cópia de tipografia: mesmo texto, mesmo instante
                }
                reconstruido.append(visivel);
                pedacos++;
            }
            if (algumEhFrase || pedacos < 2) {
                continue;
            }
            if (normalizar(reconstruido.toString()).equals(normalizar(textoFrase))) {
                silabas.addAll(irmas);
            }
        }
        return silabas;
    }

    private static String visivelDe(EventoLegenda ev) {
        String texto = ev.texto();
        if (texto == null) {
            return "";
        }
        return ClassificadorLetraKaraokeService.extrairTextoVisivel(texto).trim();
    }

    private static int palavras(String texto) {
        if (texto.isBlank()) {
            return 0;
        }
        return texto.trim().split("\\s+").length;
    }

    private static String normalizar(String texto) {
        return texto.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    /** {@code h:mm:ss.cc} do prefixo em centésimos; -1 quando ilegível. */
    private static long inicioCs(EventoLegenda ev) {
        return centesimos(campoDoPrefixo(ev, 1));
    }

    private static long fimCs(EventoLegenda ev) {
        return centesimos(campoDoPrefixo(ev, 2));
    }

    private static String campoDoPrefixo(EventoLegenda ev, int indice) {
        if (ev == null || ev.prefixo() == null) {
            return null;
        }
        String[] campos = ev.prefixo().split(",", -1);
        return campos.length > indice ? campos[indice].trim() : null;
    }

    private static long centesimos(String tempo) {
        if (tempo == null || tempo.isBlank()) {
            return -1;
        }
        String[] partes = tempo.split(":");
        if (partes.length != 3) {
            return -1;
        }
        try {
            long h = Long.parseLong(partes[0].trim());
            long m = Long.parseLong(partes[1].trim());
            double s = Double.parseDouble(partes[2].trim().replace(',', '.'));
            return h * 360000L + m * 6000L + Math.round(s * 100);
        } catch (NumberFormatException e) {
            return -1;
        }
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
