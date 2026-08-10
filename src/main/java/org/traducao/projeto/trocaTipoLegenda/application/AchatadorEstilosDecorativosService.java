package org.traducao.projeto.trocaTipoLegenda.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.CarimboCabecalhoLegenda;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;
import org.traducao.projeto.trocaTipoLegenda.domain.ClassificacaoCamadas;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.ClassificadorCamadaMusicalPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: remove "frescuras visuais" de legendas .ass — as fontes
 * decorativas e o posicionamento animado que aberturas/encerramentos e placas
 * (estilos como {@code OPL2}, {@code ED}, {@code Sign}) carregam do fansub
 * original. Reatribui as falas desses estilos ao estilo de diálogo principal
 * ({@code Default}) e descarta o bloco de override inline ({@code \pos}, {@code \fad},
 * {@code \bord}, {@code \c}...) do início da fala, transformando a letra da música
 * numa legenda branca legível igual ao diálogo. É o passo que faltava: a cura de
 * tags PRESERVA a formatação e a troca de fontes só conserta fontes ANSI quebradas,
 * então nenhum dos dois removia o estilo decorativo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O estilo BASE é o {@code Default} quando existe no cabeçalho; na sua
 *       ausência, o estilo usado pela maior parte das falas {@code Dialogue}. Se
 *       nenhum estilo base com fonte conhecida puder ser determinado, o documento
 *       volta INALTERADO (viés de preservação).</li>
 *   <li>Uma fala só é achatada quando é {@code Dialogue}, seu estilo NÃO é o base,
 *       NÃO é um estilo protegido (a saída "Karaoke Simples" do próprio pipeline) e
 *       sua fonte declarada DIFERE da fonte do estilo base. Diálogo comum (mesma
 *       fonte do base) permanece byte a byte intacto — inclusive suas tags inline.</li>
 *   <li>O achatamento reescreve APENAS a coluna Style dentro do prefixo estrutural e
 *       remove o(s) bloco(s) {@code \{...\}} do INÍCIO do texto; texto visível,
 *       tempos, camadas e demais colunas são preservados. O cabeçalho
 *       ({@code [V4+ Styles]}) não é alterado — os estilos decorativos ficam inertes
 *       por deixarem de ser referenciados.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Documento, cabeçalho ou lista de eventos nulos, ausência da coluna Style na seção
 * {@code [Events]} ou impossibilidade de determinar a fonte base fazem o serviço
 * devolver o documento original sem exceção — nunca grava uma legenda pior do que a
 * de entrada. Prefixos fora do formato esperado são deixados intactos individualmente.
 */
@Service
public class AchatadorEstilosDecorativosService {

    /**
     * Estilos que o próprio pipeline emite já limpos (ex.: a saída do Karaokê
     * Simples, em Arial e alinhada ao topo). Achatá-los para o Default reintroduziria
     * sobreposição com o diálogo — por isso são protegidos mesmo com fonte diferente.
     */
    private static final Set<String> ESTILOS_PROTEGIDOS = Set.of("karaoke simples");

    private static final String ESTILO_BASE_PREFERIDO = "Default";

    /**
     * Um ou mais blocos de override ASS colados no começo da fala.
     *
     * <p>DUPLICAÇÃO DECLARADA: {@code core.texto.TextoSemTags.BORDA_INICIO} tem esta mesma
     * regex desde 07/08/2026. Não foram unificadas de propósito — unificar criaria aresta entre
     * {@code trocaTipoLegenda} e {@code traducao}, e o projeto prefere duplicação consciente a
     * acoplamento. As intenções são OPOSTAS e podem evoluir separadas: aqui o bloco líder é
     * DESCARTADO (é a frescura visual que esta fase existe para remover); lá ele é PRESERVADO,
     * para vestir de volta a tradução que voltou do LLM sem tag nenhuma.
     */
    private static final Pattern OVERRIDE_LIDER = Pattern.compile("^(?:\\{[^}]*\\})+");

    private final AuditoriaFontesService auditoriaFontes;
    private final ClassificadorCamadaMusicalPort classificadorCamadas;

    public AchatadorEstilosDecorativosService(
        AuditoriaFontesService auditoriaFontes,
        ClassificadorCamadaMusicalPort classificadorCamadas
    ) {
        this.auditoriaFontes = auditoriaFontes;
        this.classificadorCamadas = classificadorCamadas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o achatamento a um documento de legenda inteiro,
     * devolvendo a versão sem frescura visual e o resumo do que foi alterado.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva a instância de entrada (não a muta); a lista
     * de eventos de saída tem o MESMO tamanho e ordem da de entrada; contadores são
     * não negativos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada insuficiente para decidir o base
     * devolve {@code new Resultado(documento, 0, List.of())}, sinalizando "nada a
     * achatar" sem lançar.
     */
    public Resultado achatar(DocumentoLegenda documento) {
        if (documento == null || documento.cabecalho() == null || documento.eventos() == null) {
            return new Resultado(documento, 0, List.of(), 0);
        }

        Map<String, String> fontesPorEstilo = mapaFontes(documento.cabecalho());
        String estiloBase = determinarEstiloBase(documento, fontesPorEstilo);
        String fonteBase = estiloBase == null ? null : fontesPorEstilo.get(estiloBase.toLowerCase(Locale.ROOT));
        int indiceColunaStyle = indiceColuna(documento.cabecalho(), "Style");
        if (estiloBase == null || fonteBase == null || indiceColunaStyle < 0) {
            return new Resultado(documento, 0, List.of(), 0);
        }

        List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
        Set<String> decorativosAchatados = new LinkedHashSet<>();
        int falasAchatadas = 0;
        int silabasDescartadas = 0;
        // Uma pergunta só, por documento, atravessando a porta: quais linhas ficam intactas
        // (camada original do karaokê) e quais são sílaba de timing.
        ClassificacaoCamadas camadas = classificadorCamadas == null
            ? ClassificacaoCamadas.VAZIA
            : classificadorCamadas.classificar(documento);

        for (EventoLegenda evento : documento.eventos()) {
            // Sílaba de timing é DESCARTADA, não achatada. Achatar transformava cada pedaço
            // ("Do", "you", "feel", "a", "lone") numa legenda branca própria: no Unicorn ep.1
            // seriam 138 linhas piscando uma palavra por vez sobre o vídeo, no lugar da legenda
            // normal — pior, na TV, que a tag animada que se queria remover. As 17 linhas com a
            // frase inteira permanecem e são elas que viram a legenda legível da abertura.
            if (evento != null && camadas.ehSilabaDeTiming(evento.indice())) {
                silabasDescartadas++;
                continue;
            }
            if (ehDecorativo(evento, estiloBase, fonteBase, fontesPorEstilo, camadas)) {
                String novoPrefixo = reescreverColunaEstilo(
                    evento.prefixo(), evento.tipoLinha(), indiceColunaStyle, estiloBase);
                String novoTexto = removerOverridesLideres(evento.texto());
                novos.add(new EventoLegenda(
                    evento.indice(), evento.tipoLinha(), estiloBase, novoPrefixo, novoTexto));
                decorativosAchatados.add(evento.estilo());
                falasAchatadas++;
            } else {
                novos.add(evento);
            }
        }

        if (falasAchatadas == 0 && silabasDescartadas == 0) {
            return new Resultado(documento, 0, List.of(), 0);
        }
        String cabecalho = carimbar(documento.cabecalho(), estiloBase,
            decorativosAchatados, falasAchatadas, silabasDescartadas);
        DocumentoLegenda saida = new DocumentoLegenda(
            cabecalho, novos, documento.quebraDeLinha(), documento.comBom());
        return new Resultado(saida, falasAchatadas, List.copyOf(decorativosAchatados), silabasDescartadas);
    }


    /**
     * PROPÓSITO DE NEGÓCIO: registra NO PRÓPRIO ARQUIVO o que o achatamento
     * colapsou, para que quem abrir a legenda meses depois saiba que ela foi
     * achatada e o que existia antes.
     *
     * <h2>O prejuízo que originou</h2>
     * Auditado o acervo em 07/08/2026: no Gundam 0080 a origem tinha
     * {@code Default 226 · Signs 150 · OP 21} e a saída tem {@code Default 397}.
     * Depois de achatar, <b>nada no arquivo distingue diálogo de letreiro ou de
     * música</b> — nem para auditar, nem para uma tradução futura. O Zeta está
     * nesse estado em 50 de 50 episódios.
     *
     * <p>O dado não estava perdido: o achatamento sempre gravou snapshot em
     * {@code backups/}. Perdida estava a DESCOBERTA — o arquivo achatado não
     * apontava para lugar nenhum, e quem o encontrasse sozinho não teria como
     * saber que houve achatamento. Foi assim que eu mesmo, nesta auditoria,
     * classifiquei 18.431 falas como resíduo de tradução quando eram letra de
     * música cujo estilo tinha sido apagado.
     *
     * <h2>INVARIANTES DO DOMÍNIO</h2>
     * <ul>
     *   <li>Escreve como COMENTÁRIO do formato ({@code ;}) dentro de
     *       {@code [Script Info]}. Player e libass ignoram; o Aegisub já usa a
     *       mesma convenção no cabeçalho que ele gera.</li>
     *   <li><b>Idempotente:</b> um carimbo anterior é substituído, nunca
     *       empilhado. Achatar duas vezes não pode produzir um cabeçalho que
     *       cresce a cada execução.</li>
     *   <li>Não registra data nem caminho de backup — data envelhece no arquivo e
     *       caminho absoluto vazaria a máquina de quem rodou. O que fica é o que
     *       não muda: quais estilos existiam e quanto foi descartado.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cabeçalho sem {@code [Script Info]}
     * recebe o carimbo no início; nunca lança e nunca devolve nulo.
     */
    private String carimbar(String cabecalho, String estiloBase,
                            Set<String> decorativos, int falas, int silabas) {
        StringBuilder primeira = new StringBuilder("achatou: ")
            .append(falas).append(" fala(s) para o estilo \"").append(estiloBase).append('"');
        if (silabas > 0) {
            primeira.append(", ").append(silabas).append(" silaba(s) de timing descartada(s)");
        }

        List<String> linhas = new ArrayList<>();
        linhas.add(primeira.toString());
        if (!decorativos.isEmpty()) {
            linhas.add("estilos originais: " + String.join(", ", decorativos));
        }
        linhas.add("original preservado em backups/ desta execucao");

        return CarimboCabecalhoLegenda.aplicar(cabecalho, linhas);
    }

    // A remocao do carimbo anterior mora agora em CarimboCabecalhoLegenda, no peer legenda:
    // e mecanica de cabecalho ASS, e a traducao passou a precisar dela tambem. Manter a copia
    // aqui criaria a segunda implementacao da mesma regra.

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma fala carrega estilo decorativo a achatar.
     *
     * <p>INVARIANTES DO DOMÍNIO: só {@code Dialogue}; estilo diferente do base
     * (case-insensitive); estilo não protegido; fonte declarada conhecida e diferente
     * da fonte base. Acima de tudo isso, o que o peer {@code legenda} reconhece como
     * LÍNGUA ORIGINAL (romaji) nunca é achatado — nem quando a fonte difere, nem quando o nome do
     * estilo é desconhecido. Achatar romaji o joga no estilo do diálogo, ou seja, no rodapé e em
     * cima da fala, além de dissolver a pista que separa as duas camadas do karaokê.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: estilo nulo ou fonte desconhecida devolve
     * {@code false} (não achata), preservando a fala.
     */
    private boolean ehDecorativo(EventoLegenda evento, String estiloBase, String fonteBase,
                                 Map<String, String> fontesPorEstilo, ClassificacaoCamadas camadas) {
        if (evento == null || !evento.isDialogo() || evento.estilo() == null) {
            return false;
        }
        // Proteção por LÍNGUA, não por fonte nem por nome: a razão de existir desta fase.
        // Quem decide é o adaptador atrás da porta; aqui só se consulta o veredito.
        if (camadas != null && camadas.devePreservar(evento.indice())) {
            return false;
        }
        String estilo = evento.estilo();
        String estiloNorm = estilo.toLowerCase(Locale.ROOT);
        if (estiloNorm.equals(estiloBase.toLowerCase(Locale.ROOT)) || ESTILOS_PROTEGIDOS.contains(estiloNorm)) {
            return false;
        }
        String fonteEstilo = fontesPorEstilo.get(estiloNorm);
        return fonteEstilo != null && !fonteEstilo.equalsIgnoreCase(fonteBase);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: constrói o mapa (nome do estilo em minúsculas -> fonte)
     * reaproveitando o parser de cabeçalho já existente.
     *
     * <p>INVARIANTES DO DOMÍNIO: em nomes de estilo duplicados, mantém a PRIMEIRA
     * fonte vista; chaves em minúsculas para comparação case-insensitive.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cabeçalho sem seção de estilos devolve mapa
     * vazio, o que leva {@link #achatar} a não alterar nada.
     */
    private Map<String, String> mapaFontes(String cabecalho) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (AuditoriaFonteInfo info : auditoriaFontes.analisarCabecalho(cabecalho)) {
            mapa.putIfAbsent(info.estilo().toLowerCase(Locale.ROOT), info.fonteAtual());
        }
        return mapa;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: elege o estilo de diálogo principal (a "fonte da verdade"
     * de legibilidade) usado como alvo do achatamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: prefere {@code Default} quando presente no cabeçalho.
     * Na ausência dele, vence o estilo com MAIOR TEMPO DE TELA entre as falas
     * {@code Dialogue} com fonte declarada — nunca o mais numeroso. Contagem de eventos
     * só decide quando nenhuma duração pôde ser lida (legenda sem colunas Start/End ou
     * com tempos ilegíveis), e aí o critério antigo volta como último recurso.
     *
     * <h2>O prejuízo que originou</h2>
     * O critério anterior era "estilo mais frequente" e elegia a DECORAÇÃO como base.
     * O logo de abertura do Zeta Gundam é animado quadro a quadro: 297 eventos do estilo
     * {@code Zeta Episode Title}, de 0,04 s cada — 6 segundos de tela no total. Nos
     * episódios 1, 8 e 14 esses 297 quadros superavam as 205/291/275 falas de diálogo do
     * episódio, então a decoração ganhava a votação e o achatamento rodava AO CONTRÁRIO:
     * o diálogo inteiro era jogado no estilo do letreiro (corpo 100, contorno 0, sombra 0,
     * cinza claro, margem lateral 10), ficando ilegível sobre cena clara. Nos outros 47
     * episódios o defeito não apareceu por um fio — no episódio 2 foram 298 falas contra
     * 297 quadros, UMA linha de diferença. Por tempo de tela a separação é de duas ordens
     * de grandeza (6 s de logo contra ~10 min de diálogo), e não depende de sorte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nenhum candidato com fonte conhecida devolve
     * {@code null}, sinalizando a {@link #achatar} para preservar o documento.
     */
    private String determinarEstiloBase(DocumentoLegenda documento, Map<String, String> fontesPorEstilo) {
        for (AuditoriaFonteInfo info : auditoriaFontes.analisarCabecalho(documento.cabecalho())) {
            if (info.estilo().equalsIgnoreCase(ESTILO_BASE_PREFERIDO)) {
                return info.estilo();
            }
        }
        int colunaInicio = indiceColuna(documento.cabecalho(), "Start");
        int colunaFim = indiceColuna(documento.cabecalho(), "End");

        Map<String, Long> tempoDeTela = new LinkedHashMap<>();
        Map<String, Integer> frequencia = new LinkedHashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || evento.estilo() == null
                || !fontesPorEstilo.containsKey(evento.estilo().toLowerCase(Locale.ROOT))) {
                continue;
            }
            frequencia.merge(evento.estilo(), 1, Integer::sum);
            tempoDeTela.merge(evento.estilo(), duracaoEmCentesimos(evento, colunaInicio, colunaFim), Long::sum);
        }

        String porTempoDeTela = tempoDeTela.entrySet().stream()
            .filter(entrada -> entrada.getValue() > 0L)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        if (porTempoDeTela != null) {
            return porTempoDeTela;
        }
        return frequencia.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mede quanto tempo uma fala fica no ar, para que a eleição do
     * estilo base pese presença na tela em vez de número de linhas.
     *
     * <p>INVARIANTES DO DOMÍNIO: resultado em centésimos de segundo e nunca negativo —
     * fim anterior ao início vale {@code 0}. Não interpreta o conteúdo da fala nem altera
     * o evento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: coluna ausente, prefixo fora do formato ou tempo
     * ilegível devolvem {@code 0}, o que faz o estilo apenas não somar tempo — nunca lança
     * e nunca derruba a eleição inteira por causa de uma linha malformada.
     */
    private long duracaoEmCentesimos(EventoLegenda evento, int colunaInicio, int colunaFim) {
        if (colunaInicio < 0 || colunaFim < 0) {
            return 0L;
        }
        String[] campos = camposDoPrefixo(evento);
        if (campos == null || colunaInicio >= campos.length || colunaFim >= campos.length) {
            return 0L;
        }
        long inicio = emCentesimos(campos[colunaInicio]);
        long fim = emCentesimos(campos[colunaFim]);
        if (inicio < 0L || fim < 0L) {
            return 0L;
        }
        return Math.max(0L, fim - inicio);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o carimbo de tempo do formato ASS ({@code H:MM:SS.CC})
     * para centésimos de segundo, unidade em que as durações são somadas.
     *
     * <p>INVARIANTES DO DOMÍNIO: aceita horas com um ou mais dígitos e centésimos com um ou
     * dois; espaços em volta são tolerados. Não faz aritmética de fuso nem de quadro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code -1} para nulo, formato inesperado ou
     * número não parseável, sinalizando "não medi" — que o chamador traduz em duração zero.
     */
    private static long emCentesimos(String tempo) {
        if (tempo == null) {
            return -1L;
        }
        String t = tempo.trim();
        int primeiroDoisPontos = t.indexOf(':');
        int segundoDoisPontos = primeiroDoisPontos < 0 ? -1 : t.indexOf(':', primeiroDoisPontos + 1);
        int ponto = t.lastIndexOf('.');
        if (primeiroDoisPontos < 0 || segundoDoisPontos < 0 || ponto < segundoDoisPontos) {
            return -1L;
        }
        try {
            long horas = Long.parseLong(t.substring(0, primeiroDoisPontos));
            long minutos = Long.parseLong(t.substring(primeiroDoisPontos + 1, segundoDoisPontos));
            long segundos = Long.parseLong(t.substring(segundoDoisPontos + 1, ponto));
            String centesimosTexto = t.substring(ponto + 1);
            long centesimos = Long.parseLong(centesimosTexto);
            if (centesimosTexto.length() == 1) {
                centesimos *= 10L;
            }
            return ((horas * 60L + minutos) * 60L + segundos) * 100L + centesimos;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quebra o prefixo estrutural de um evento nas suas colunas, para
     * que estilo e tempos sejam lidos pelo índice declarado no {@code Format:} do cabeçalho.
     *
     * <p>INVARIANTES DO DOMÍNIO: não descarta colunas vazias (usa limite negativo no split),
     * de modo que o índice lido do cabeçalho continua válido. Não modifica o evento.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo, sem o cabeçalho {@code tipo + ": "} ou
     * que não termine em vírgula devolve {@code null} — o chamador trata como "não medi".
     */
    private String[] camposDoPrefixo(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null || evento.tipoLinha() == null) {
            return null;
        }
        String cabeca = evento.tipoLinha() + ": ";
        String prefixo = evento.prefixo();
        if (!prefixo.startsWith(cabeca) || !prefixo.endsWith(",")) {
            return null;
        }
        return prefixo.substring(cabeca.length(), prefixo.length() - 1).split(",", -1);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza a posição (0-based) de uma coluna declarada na seção
     * {@code [Events]} — {@code Style} para reescrever o estilo dentro do prefixo bruto,
     * {@code Start}/{@code End} para medir o tempo de tela na eleição do estilo base.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a primeira linha {@code Format:} após {@code [Events]};
     * comparação de nome de coluna case-insensitive e sem espaços. A ordem das colunas é
     * lida do arquivo, nunca presumida — legenda com {@code Format:} fora do padrão continua
     * sendo tratada corretamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: seção/coluna ausente devolve {@code -1}. Para
     * {@code Style} isso aborta o achatamento sem alterar o documento; para os tempos, apenas
     * faz a eleição cair no critério de contagem.
     */
    private int indiceColuna(String cabecalho, String nomeColuna) {
        String[] linhas = cabecalho.split("\r\n|\n", -1);
        boolean emEvents = false;
        for (String linha : linhas) {
            String t = linha.trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                emEvents = t.equalsIgnoreCase("[Events]");
                continue;
            }
            if (emEvents && t.regionMatches(true, 0, "Format:", 0, "Format:".length())) {
                String[] colunas = t.substring(t.indexOf(':') + 1).split(",");
                for (int i = 0; i < colunas.length; i++) {
                    if (colunas[i].trim().equalsIgnoreCase(nomeColuna)) {
                        return i;
                    }
                }
                return -1;
            }
        }
        return -1;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca o nome do estilo dentro do prefixo estrutural
     * (ex.: {@code "Dialogue: 0,...,OPL2,,0,0,0,fx,"} -> {@code "...,Default,,0,0,0,fx,"}).
     *
     * <p>INVARIANTES DO DOMÍNIO: só a coluna de índice {@code indiceColunaStyle} muda;
     * as demais colunas e a vírgula final são mantidas exatamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo que não começa com {@code tipo + ": "},
     * não termina em vírgula ou tem menos colunas que o índice esperado é devolvido
     * inalterado — melhor manter o estilo antigo do que corromper a linha.
     */
    private String reescreverColunaEstilo(String prefixo, String tipoLinha, int indiceColunaStyle, String novoEstilo) {
        if (prefixo == null || tipoLinha == null) {
            return prefixo;
        }
        String cabeca = tipoLinha + ": ";
        if (!prefixo.startsWith(cabeca) || !prefixo.endsWith(",")) {
            return prefixo;
        }
        String meio = prefixo.substring(cabeca.length(), prefixo.length() - 1);
        String[] campos = meio.split(",", -1);
        if (indiceColunaStyle >= campos.length) {
            return prefixo;
        }
        campos[indiceColunaStyle] = novoEstilo;
        return cabeca + String.join(",", campos) + ",";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove o bloco de override inline colado no início da fala
     * — o {@code \fad}/{@code \pos}/{@code \bord}/{@code \c} que produz a animação e o
     * posicionamento decorativos.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove apenas blocos {@code \{...\}} CONSECUTIVOS do
     * começo; tags no meio da linha e o texto visível são preservados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve {@code null}; texto sem
     * override no início é devolvido igual.
     */
    private String removerOverridesLideres(String texto) {
        if (texto == null) {
            return null;
        }
        return OVERRIDE_LIDER.matcher(texto).replaceFirst("");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o documento achatado e o resumo de auditoria
     * (quantas falas mudaram, quais estilos decorativos foram neutralizados e quantas
     * sílabas de timing saíram) para o caso de uso e a telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: os contadores são não negativos; {@code falasAchatadas}
     * é o total de eventos reescritos e {@code silabasDescartadas} o de eventos
     * REMOVIDOS; {@code estilosDecorativos} é imutável e sem repetição.
     *
     * <p>ATENÇÃO: com {@code silabasDescartadas > 0} a legenda de saída tem MENOS eventos
     * que a de entrada. Até 2026-07-29 valia "mesmo tamanho e ordem", e a mudança é
     * deliberada — camada de timing de karaokê não é legenda, é efeito, e mantê-la
     * achatada produzia uma linha branca por sílaba sobre o vídeo. O original continua
     * recuperável pelo backup que {@code AchatarEstilosUseCase} grava antes de gravar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: quando nada muda, {@code documento} é o de
     * entrada, os contadores são {@code 0} e a lista é vazia.
     */
    public record Resultado(DocumentoLegenda documento, int falasAchatadas,
                            List<String> estilosDecorativos, int silabasDescartadas) {
        public Resultado {
            estilosDecorativos = estilosDecorativos == null ? List.of() : List.copyOf(estilosDecorativos);
        }

        public boolean houveAchatamento() {
            return falasAchatadas > 0 || silabasDescartadas > 0;
        }
    }
}
