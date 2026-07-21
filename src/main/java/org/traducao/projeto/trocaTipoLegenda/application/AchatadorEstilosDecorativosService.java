package org.traducao.projeto.trocaTipoLegenda.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;

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

    /** Um ou mais blocos de override ASS colados no começo da fala. */
    private static final Pattern OVERRIDE_LIDER = Pattern.compile("^(?:\\{[^}]*\\})+");

    private final AuditoriaFontesService auditoriaFontes;

    public AchatadorEstilosDecorativosService(AuditoriaFontesService auditoriaFontes) {
        this.auditoriaFontes = auditoriaFontes;
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
            return new Resultado(documento, 0, List.of());
        }

        Map<String, String> fontesPorEstilo = mapaFontes(documento.cabecalho());
        String estiloBase = determinarEstiloBase(documento, fontesPorEstilo);
        String fonteBase = estiloBase == null ? null : fontesPorEstilo.get(estiloBase.toLowerCase(Locale.ROOT));
        int indiceColunaStyle = indiceColunaStyle(documento.cabecalho());
        if (estiloBase == null || fonteBase == null || indiceColunaStyle < 0) {
            return new Resultado(documento, 0, List.of());
        }

        List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
        Set<String> decorativosAchatados = new LinkedHashSet<>();
        int falasAchatadas = 0;

        for (EventoLegenda evento : documento.eventos()) {
            if (ehDecorativo(evento, estiloBase, fonteBase, fontesPorEstilo)) {
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

        if (falasAchatadas == 0) {
            return new Resultado(documento, 0, List.of());
        }
        DocumentoLegenda saida = new DocumentoLegenda(
            documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
        return new Resultado(saida, falasAchatadas, List.copyOf(decorativosAchatados));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma fala carrega estilo decorativo a achatar.
     *
     * <p>INVARIANTES DO DOMÍNIO: só {@code Dialogue}; estilo diferente do base
     * (case-insensitive); estilo não protegido; fonte declarada conhecida e diferente
     * da fonte base.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: estilo nulo ou fonte desconhecida devolve
     * {@code false} (não achata), preservando a fala.
     */
    private boolean ehDecorativo(EventoLegenda evento, String estiloBase, String fonteBase,
                                 Map<String, String> fontesPorEstilo) {
        if (evento == null || !evento.isDialogo() || evento.estilo() == null) {
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
     * <p>INVARIANTES DO DOMÍNIO: prefere {@code Default} quando presente no cabeçalho;
     * senão, o estilo mais frequente entre as falas {@code Dialogue} que tenha fonte
     * declarada.
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
        Map<String, Integer> frequencia = new LinkedHashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (evento.isDialogo() && evento.estilo() != null
                && fontesPorEstilo.containsKey(evento.estilo().toLowerCase(Locale.ROOT))) {
                frequencia.merge(evento.estilo(), 1, Integer::sum);
            }
        }
        return frequencia.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza a posição (0-based) da coluna Style na seção
     * {@code [Events]}, necessária para reescrever o estilo dentro do prefixo bruto.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a primeira linha {@code Format:} após {@code [Events]};
     * comparação de nome de coluna case-insensitive e sem espaços.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: seção/coluna ausente devolve {@code -1}, o que
     * aborta o achatamento sem alterar o documento.
     */
    private int indiceColunaStyle(String cabecalho) {
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
                    if (colunas[i].trim().equalsIgnoreCase("Style")) {
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
     * (quantas falas mudaram e quais estilos decorativos foram neutralizados) para o
     * caso de uso e a telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code falasAchatadas} é não negativo e igual ao total
     * de eventos reescritos; {@code estilosDecorativos} é imutável e sem repetição.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: quando nada é achatado, {@code documento} é o
     * de entrada, {@code falasAchatadas} é {@code 0} e a lista é vazia.
     */
    public record Resultado(DocumentoLegenda documento, int falasAchatadas, List<String> estilosDecorativos) {
        public Resultado {
            estilosDecorativos = estilosDecorativos == null ? List.of() : List.copyOf(estilosDecorativos);
        }

        public boolean houveAchatamento() {
            return falasAchatadas > 0;
        }
    }
}
