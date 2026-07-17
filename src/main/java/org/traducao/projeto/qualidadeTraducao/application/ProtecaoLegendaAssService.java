package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: blindagens compartilhadas para linhas ASS/SSA antes e depois
 * de chamadas a IA/serviços externos. Centraliza os casos perigosos encontrados em
 * fansubs — clips vetoriais longos, letras soltas pós-template e preâmbulos alucinados
 * — para que typesetting pesado não seja enviado ao LLM nem sobrescrito por uma
 * resposta que destruiria o efeito visual original.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só o texto VISÍVEL decide: blocos {@code {...}} de override/comentário ASS são
 *       removidos para a inspeção e o texto recebido nunca é modificado.</li>
 *   <li>Bloqueio antes do LLM exige conjunção de sinais (tags pesadas + alta densidade
 *       de tags + texto curto): uma fala normal com duas camadas de estilo nunca é
 *       bloqueada só por ter tags.</li>
 *   <li>Serviço stateless — só constantes {@code static final}. Qualquer instância é
 *       intercambiável, o que sustenta tanto a injeção Spring quanto o reuso estático
 *       pelos chamadores.</li>
 *   <li>Contrato público do peer são os métodos de INSTÂNCIA; os estáticos permanecem
 *       package-private como detalhe interno de implementação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança: entrada {@code null} degrada para o lado seguro — {@code false}
 * nas heurísticas de suspeita/bloqueio (não intervir) e {@code ""} na extração de texto
 * visível. Texto sem conteúdo visível é bloqueado antes do LLM, evitando gastar chamada
 * em linha puramente decorativa.
 */
@Service
public class ProtecaoLegendaAssService {

    private static final Pattern PADRAO_DESENHO_VETORIAL = Pattern.compile("\\\\p[1-9]\\d*");
    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]+}");
    private static final Pattern PADRAO_TAG_ASS_PESADA = Pattern.compile(
        "\\\\(?:t\\(|pos\\(|move\\(|i?clip\\(|org\\(|fad\\(|fr[xyz]|blur|bord|[13]c&)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PADRAO_CLIP_LONGO = Pattern.compile(
        "\\\\i?clip\\([^)]{300,}\\)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern PADRAO_ESTILO_TECNICO = Pattern.compile(
        "(?i)\\b(signs?|title|ep\\s*title|next\\s*ep|opening|ending|op|ed|song|karaoke|lyrics?|credits?)\\b"
    );
    private static final Pattern PADRAO_CAMINHO_TRADUZIDO = Pattern.compile(
        "(?i)(?:legendas[_-]?ptbr|traducao[_-]?ptbr|traduzidas|revisao|_pt-?br\\b|\\bpt-?br_)"
    );
    private static final Pattern PADRAO_PREAMBULO_LLM = Pattern.compile(
        "^(?:sa[ií]da|resposta|tradu[çc][ãa]o|translation|output)\\s*:",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final int LIMIAR_TEXTO_CURTO_EFEITO = 8;

    /**
     * PROPÓSITO DE NEGÓCIO: identifica a linha que carrega um desenho vetorial do
     * Aegisub ({@code \p1}, {@code \p2}...), onde o "texto" são coordenadas de forma
     * e traduzir significaria destruir o desenho.
     * <p>INVARIANTES DO DOMÍNIO: basta uma ocorrência do modo de desenho em qualquer
     * posição da linha para caracterizar conteúdo vetorial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna {@code false},
     * deixando a decisão para as demais blindagens.
     */
    public boolean temDesenhoVetorial(String texto) {
        return texto != null && PADRAO_DESENHO_VETORIAL.matcher(texto).find();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se a IA deve ficar fora desta linha em fluxos que
     * não sabem quantas vezes o texto visível se repete no documento.
     * <p>INVARIANTES DO DOMÍNIO: assume repetição 2 — o limiar mínimo que caracteriza
     * letreiro animado — mantendo o mesmo critério conjunto de {@code deveBloquearAntesDoLlm}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna {@code false}
     * (não bloqueia); texto sem parte visível retorna {@code true}.
     */
    public boolean deveIgnorarIntervencaoIa(String estilo, String texto) {
        return deveBloquearAntesDoLlm(estilo, texto, 2);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta a resposta em que o LLM inflou uma linha de
     * typesetting pesado com texto curto, sinal de que inventou conteúdo em vez de
     * traduzir o pouco que havia.
     * <p>INVARIANTES DO DOMÍNIO: só linhas simultaneamente com tags pesadas, alta
     * densidade de tags e texto original curto são candidatas; fora dessa conjunção
     * nenhuma resposta é considerada suspeita.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer argumento {@code null} ou tradução
     * sem texto visível retorna {@code false}, preservando a resposta.
     */
    public boolean respostaSuspeita(String original, String traduzido) {
        return respostaAssPesadaSuspeita(original, traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que a linha chegue ao LLM quando é typesetting de
     * alto risco, poupando chamada e evitando dano ao efeito visual.
     * <p>INVARIANTES DO DOMÍNIO: a repetição do texto visível é fornecida pelo chamador
     * e só pesa junto de clip longo e texto curto; estilo técnico substitui a repetição
     * como evidência.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna {@code false};
     * texto sem parte visível retorna {@code true}.
     */
    public boolean deveBloquearLinhaAntesDoLlm(String estilo, String texto, long repeticoesTextoVisivel) {
        return deveBloquearAntesDoLlm(estilo, texto, repeticoesTextoVisivel);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe o texto que o espectador realmente lê, sem tags de
     * override nem quebras/espaços rígidos do ASS.
     * <p>INVARIANTES DO DOMÍNIO: a limpeza é apenas para inspeção e comparação; o texto
     * recebido nunca é modificado nem persistido nesta forma.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna string vazia.
     */
    public String textoVisivel(String texto) {
        return extrairTextoVisivelAss(texto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: evita retraduzir uma legenda que já é saída PT-BR do próprio
     * pipeline, reconhecendo o caminho do arquivo.
     * <p>INVARIANTES DO DOMÍNIO: a decisão é só pelo caminho — separadores e espaços são
     * normalizados para que a mesma pasta case em Windows e Linux.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho {@code null} retorna {@code false},
     * tratando a entrada como ainda não traduzida.
     */
    public boolean caminhoPareceTraduzido(Path arquivoEntrada) {
        return caminhoPareceLegendaTraduzida(arquivoEntrada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo da suspeita de resposta inflada sobre typesetting
     * pesado, compartilhado pelo contrato público de instância.
     * <p>INVARIANTES DO DOMÍNIO: exige a conjunção tags pesadas + alta densidade + texto
     * original curto; só então preâmbulo ou crescimento desproporcional acusam.
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumentos {@code null} ou tradução sem texto
     * visível retornam {@code false}.
     */
    static boolean respostaAssPesadaSuspeita(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return false;
        }
        String visivelOriginal = extrairTextoVisivelAss(original);
        String visivelTraduzido = extrairTextoVisivelAss(traduzido);
        if (visivelTraduzido.isBlank()) {
            return false;
        }

        boolean originalComTagsPesadas = PADRAO_TAG_ASS_PESADA.matcher(original).find();
        boolean altaDensidadeTags = original.length() > 40
            && Math.max(1, visivelOriginal.length()) * 3 < original.length();
        boolean textoOriginalCurto = visivelOriginal.length() <= LIMIAR_TEXTO_CURTO_EFEITO;
        if (!originalComTagsPesadas || !altaDensidadeTags || !textoOriginalCurto) {
            return false;
        }

        if (PADRAO_PREAMBULO_LLM.matcher(visivelTraduzido).find()) {
            return true;
        }
        if (visivelOriginal.length() <= 3 && visivelTraduzido.length() > visivelOriginal.length() + 8) {
            return true;
        }
        return visivelTraduzido.length() > Math.max(20, visivelOriginal.length() * 3);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo da extração do texto lido pelo espectador,
     * compartilhado pelo contrato público de instância.
     * <p>INVARIANTES DO DOMÍNIO: remove blocos de override e converte {@code \N},
     * {@code \n} e {@code \h} em espaço, de modo que a comparação de tamanho reflita
     * a fala e não a marcação.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna string vazia.
     */
    static String extrairTextoVisivelAss(String texto) {
        if (texto == null) {
            return "";
        }
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto)
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo do bloqueio pré-LLM de typesetting de alto risco,
     * compartilhado pelo contrato público de instância.
     * <p>INVARIANTES DO DOMÍNIO: sem tags pesadas ou sem alta densidade a linha segue
     * para tradução; texto visível de até 3 caracteres sob tags pesadas é sempre
     * bloqueado; acima disso exige clip longo somado a repetição ou estilo técnico.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null} retorna {@code false};
     * texto sem parte visível retorna {@code true}.
     */
    static boolean deveBloquearAntesDoLlm(String estilo, String texto, long repeticoesTextoVisivel) {
        if (texto == null) {
            return false;
        }
        String visivel = extrairTextoVisivelAss(texto);
        if (visivel.isEmpty()) {
            return true;
        }

        boolean temTagsPesadas = PADRAO_TAG_ASS_PESADA.matcher(texto).find();
        if (!temTagsPesadas) {
            return false;
        }

        boolean altaDensidadeTags = texto.length() > 40
            && Math.max(1, visivel.length()) * 3 < texto.length();
        if (!altaDensidadeTags) {
            return false;
        }

        if (visivel.length() <= 3) {
            return true;
        }

        boolean clipLongo = PADRAO_CLIP_LONGO.matcher(texto).find();
        boolean estiloTecnico = estilo != null && PADRAO_ESTILO_TECNICO.matcher(estilo).find();
        return clipLongo && visivel.length() <= 30 && (repeticoesTextoVisivel >= 2 || estiloTecnico);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: núcleo do reconhecimento de caminho de legenda já traduzida,
     * compartilhado pelo contrato público de instância.
     * <p>INVARIANTES DO DOMÍNIO: normaliza {@code \} para {@code /} e espaço para
     * {@code _} antes de casar, para que o mesmo diretório seja reconhecido
     * independentemente do sistema de arquivos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho {@code null} retorna {@code false}.
     */
    static boolean caminhoPareceLegendaTraduzida(Path arquivoEntrada) {
        if (arquivoEntrada == null) {
            return false;
        }
        String normalizado = arquivoEntrada.toString()
            .replace('\\', '/')
            .replace(' ', '_');
        return PADRAO_CAMINHO_TRADUZIDO.matcher(normalizado).find();
    }
}
