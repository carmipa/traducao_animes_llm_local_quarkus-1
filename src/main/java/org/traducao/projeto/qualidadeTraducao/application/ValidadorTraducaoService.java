package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: impede que textos parcialmente traduzidos, respostas
 * rotuladas ou conteúdo em idioma indevido cheguem às legendas e ao cache.
 * <p>INVARIANTES DO DOMÍNIO: comentários ASS não visíveis são ignorados, nomes
 * próprios conhecidos não viram falso positivo e resíduos visíveis inequívocos
 * sempre bloqueiam a proposta.
 * <p>COMPORTAMENTO EM CASO DE FALHA: lança
 * {@link AlucinacaoDetectadaException} com diagnóstico e o chamador preserva a
 * tradução anterior.
 */
@Service
public class ValidadorTraducaoService {
    
    // Regras robustas importadas do pipeline Python, ampliadas após observar em
    // produção o Mistral Nemo deixar fragmentos como "exactly the same" sem
    // traduzir mesmo traduzindo o resto da fala corretamente.
    //
    // UNICODE_CHARACTER_CLASS e necessario aqui: sem ela, \b no Java so reconhece
    // [a-zA-Z0-9_] como caractere de palavra, entao letras acentuadas (ç, ã, é...)
    // contam como "fronteira", e palavras em portugues como "força" ou "esforço"
    // batem com "\bfor\b" e disparam falso positivo de "resíduo em inglês".
    private static final Pattern PADRAO_RESIDUO = Pattern.compile(
        "\\b(you|they|without|very|where|what|when|why|who|this|that|these|those|"
            + "and|the|is|are|was|were|have|has|with|from|exactly|same|not|but|"
            + "except|because|could|would|should|will|shall|might|must|cannot|"
            + "their|yours|hers|ours|theirs|whom|whose|which|how|however|whenever|wherever|"
            + "about|above|across|after|against|along|among|around|before|behind|below|beneath|"
            + "beside|between|beyond|down|during|into|outside|over|past|since|through|"
            + "throughout|till|toward|under|underneath|until|upon|within|although|unless|"
            + "does|did|been|had|went|gone|got|make|made|take|took|saw|seen|thought|transform|"
            + "rather|unknown|ensign|always|never|sometimes|often|usually|really|also|even|"
            + "every|please|thank|thanks|sorry|feds)\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Contrações inglesas: resíduo inequívoco (nenhuma colide com PT-BR) que a
    // lista de palavras soltas não pega — caso real do 86: "Se você terminou sua
    // missão, it's seu dever me dar um relatório." passava sem disparar nada.
    // Aceita apóstrofo ASCII (') e tipográfico (’).
    private static final Pattern PADRAO_CONTRACAO_INGLES = Pattern.compile(
        "\\b(?:it|that|there|what|here|he|she|let)['’]s\\b"
            + "|\\b(?:don|can|won|ain|didn|doesn|isn|aren|wasn|weren|couldn|wouldn|shouldn|hasn|haven)['’]t\\b"
            + "|\\b(?:i|you|we|they)['’](?:m|ll|ve|re|d)\\b"
            + "|\\b(?:gonna|gotta|wanna)\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Palavras inequívocas de francês (sem colisão com vocabulário PT-BR) que pegam
    // o LLM local "vazando" pra francês em vez de inglês — caso observado em
    // produção: "WITH SHINING BLUE FIRE" foi "corrigido" para "AURA BLEU BRILLANTE"
    // e passou batido pelo PADRAO_RESIDUO, que só cobre inglês. Espanhol não entra
    // aqui: nunca foi observado como idioma de origem/vazamento neste projeto.
    private static final Pattern PADRAO_OUTRO_IDIOMA = Pattern.compile(
        "\\b(bleu|rouge|noir|blanc|jaune|monde|cœur|coeur|amour|bonjour|merci|"
            + "toujours|déjà|deja|être|avoir|avec|chez|sans|vous|nous|elles|"
            + "très|tres|où|quelque|aujourd'hui)\\b",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Evita os falsos positivos de "Abaixo a tirania" bloqueando apenas preâmbulos óbvios.
    // "tradução:"/"saída:" no início cobre o LLM rotulando a resposta (casos reais:
    // linha de karaokê do Gundam Narrative entregue como "Tradução: {\r\pos(488,23)...}ep"
    // e efeito visual devolvido como "Saída: {=68}{\pos(...)}").
    private static final Pattern PADRAO_PREAMBULO = Pattern.compile(
        "^(esta [ée] a tradu|abaixo seguem|aqui está a|tradução solicitada|a tradução seria|"
            + "tradu[çc][ãa]o\\s*:|sa[ií]da\\s*:|resposta\\s*:)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Recusa/meta-resposta do LLM: em vez de traduzir, o modelo devolve uma
    // desculpa, um pedido de mais contexto ou fala SOBRE a tarefa de tradução, e
    // esse texto ia direto para a legenda (bug histórico do software antigo:
    // "Eh?" -> "Desculpe, mas não recebi nenhuma linha para traduzir. Por favor,
    // forneça-me as linhas...", "No, sir!" -> "Sem tradução.", "Dunno" -> "não
    // tenho contexto para traduzir a linha..."). Ancorado na META-referência à
    // tradução — NUNCA em "por favor"/"desculpe"/"não posso" sozinhos, que são
    // diálogo legítimo comum em anime (validado contra 88 recusas reais x 125
    // falas legítimas com "por favor" no cache do Gundam 08th MS Team).
    // Inclui também a RECITAÇÃO DE CAPACIDADE/PAPEL: ao ver "About who I am and what
    // I'm capable of." (08th ep1), o modelo recitou o prompt de sistema -> "Sou capaz
    // de traduzir legendas de anime ... preservando ... intenção emocional". Como está
    // em PT e preserva o ASS, escapava da validação. Ancorado no vocabulário de
    // tarefa/prompt ("traduzir legendas", "legendas de anime", "intenção emocional") —
    // NUNCA em "sou capaz"/"preservando"/"emocional" sozinhos, que são fala legítima.
    private static final Pattern PADRAO_RECUSA_META = Pattern.compile(
        "(?:sem\\s+tradu[çc][ãa]o\\b"
            + "|n[ãa]o\\s+(?:recebi|tenho\\s+acesso|tenho\\s+como\\s+saber|tenho\\s+contexto|tenho\\s+informa)"
            + "|forne\\S*\\s+.{0,40}?(?:linha|contexto|informa|texto|t[íi]tulo)"
            + "|preciso\\s+de\\s+mais\\s+(?:contexto|informa)"
            + "|(?:linha|texto|frase)s?\\s+(?:para\\s+|que\\s+.{0,20}?)?traduzir"
            + "|traduzir\\s+(?:a\\s+linha|a\\s+frase|o\\s+texto|isso|este)"
            + "|nenhuma\\s+tradu[çc][ãa]o\\s+encontrada"
            + "|--\\s*tradu[çc][ãa]o|^traduzir\\s*:"
            + "|n[ãa]o\\s+precisa\\s+ser\\s+retrabalhada|a\\s+tradu[çc][ãa]o\\s+(?:atual\\s+)?[ée]\\s+correta"
            + "|as\\s+an\\s+ai|i\\s+(?:cannot|can'?t|am\\s+unable)\\b"
            + "|provide\\s+more\\s+(?:context|info)|don'?t\\s+have\\s+.{0,20}context"
            + "|traduzir\\s+legendas?|legendas?\\s+de\\s+anime|inten[çc][ãa]o\\s+emocional)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Palavras do PADRAO_RESIDUO que também são nomes próprios comuns em
    // anime/mangá (ex.: o personagem "Will"). Uma ocorrência Capitalizada
    // dessas palavras numa frase PT-BR é quase sempre nome, não resíduo —
    // sem esta exceção a fala inteira era rejeitada e mantida sem tradução.
    private static final java.util.Set<String> RESIDUOS_TAMBEM_NOMES = java.util.Set.of("will");

    // Asterisco no texto visível: o LLM às vezes CENSURA palavrão ("You bastards!" ->
    // "uns *****es!") ou vaza MARKDOWN de ênfase ("Damn it!" -> "Merd**a**!"). Casos reais
    // em ZZ/08th/Zeta/86/Narrative. Neste projeto a fonte NUNCA traz '*' e as legendas usam
    // parênteses para ação — então qualquer '*' visível é artefato do modelo e a fala deve
    // ficar pendente (retraduz) em vez de ir censurada/pontilhada para a legenda. Bônus:
    // entradas de cache com asterisco deixam de ser reaproveitadas e retraduzem no próximo run.
    private static final Pattern PADRAO_ASTERISCO = Pattern.compile("\\*");

    // Blocos {...} do ASS (tags de override e comentários de fansub) não são
    // texto exibido: valida-los como fala gerava falso positivo (comentário
    // legítimo em inglês tipo "{Yes, ma'am}" disparava resíduo e queimava uma
    // chamada de LLM à toa) e falso negativo (preâmbulo depois de tag, como
    // "{\i1}Tradução: ...", escapava da âncora ^ do padrão de preâmbulo).
    private static final Pattern PADRAO_BLOCO_ASS = Pattern.compile("\\{[^}]*\\}");

    // Marcadores do pipeline ([[TAG0]]) não são texto exibido: entram e saem da fala sem
    // alterar o que o espectador lê. Precisam sair antes de QUALQUER medida de comprimento,
    // senão uma fala curta cheia de marcadores parece longa e a desproporção não dispara.
    private static final Pattern PADRAO_MARCADOR = Pattern.compile("\\[\\[TAG\\d+\\]\\]");

    // ---------------------------------------------------------------------------------
    // LIMIARES DA VALIDAÇÃO DE PAR — todos MEDIDOS, nenhum estimado.
    //
    // Corpus: as 5.794 entradas não-vazias do run completo de Guilty Crown (23 episódios,
    // mistral-nemo), com os 27 defeitos publicados nos .ass confirmados um a um contra a
    // legenda entregue. A varredura de limiares está registrada na discussão com o Codex
    // (RESPOSTA_CODEX_PARA_CLAUDE_KRONOS_2026-07-26.md).
    //
    // Resultado desta combinação: 26 dos 27 defeitos capturados, ZERO falso positivo em
    // 5.794. Afrouxar o fator para 2,0 ou o mínimo para 18 caracteres introduz falso
    // positivo imediato em fala legítima curta ("Huh?" -> "O que foi?"); apertar para 3,5
    // perde dois defeitos reais. Estes três números são um ponto ótimo medido, não uma
    // preferência — alterá-los exige repetir a medição sobre o corpus.
    // ---------------------------------------------------------------------------------

    /** Acima deste comprimento o original tem conteúdo suficiente para justificar expansão. */
    private static final int COMPRIMENTO_MAXIMO_ORIGINAL_CURTO = 30;

    /** Abaixo disto a tradução é curta demais para que a razão signifique alguma coisa. */
    private static final int COMPRIMENTO_MINIMO_PARA_DESPROPORCAO = 20;

    /** Razão tradução/original a partir da qual o excedente deixa de ser expansão natural do PT-BR. */
    private static final double FATOR_DESPROPORCAO = 2.5;

    // Prefixo "<algo>:" no início da fala — o formato que o modelo usa ao narrar quem fala
    // ("Inori: \"Você me ama, Shu?\"") em vez de traduzir. O limite de 30 caracteres antes dos
    // dois-pontos separa nome de personagem de uma oração inteira que legitimamente termina
    // em dois-pontos. O grupo 2 guarda o que vem depois, usado para distinguir invenção de
    // simples troca de pontuação.
    private static final Pattern PADRAO_PREFIXO_LOCUTOR = Pattern.compile("^([^:]{1,30}):(.*)$", Pattern.DOTALL);

    // Conectivos de discurso que legitimamente abrem uma fala com dois-pontos em PT-BR.
    // Sem esta allowlist, "I repeat:" traduzido como "Repito:" seria acusado de locutor
    // inventado — caso real e correto, medido no corpus.
    private static final Pattern PADRAO_CONECTIVO_DISCURSO = Pattern.compile(
        "^(?:repito|aten[çc][ãa]o|aviso|nota|observa[çc][ãa]o|ou seja|isto|isso|ent[ãa]o|bem|sim|"
            + "n[ãa]o|mas|ah|oh|ei|olha|escuta|escute|veja|ora)\\s*:",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // Meta-resposta que só é reconhecível COM o original em mãos: o modelo comenta a tarefa
    // de traduzir em vez de executá-la. Difere do PADRAO_RECUSA_META porque não depende de
    // recusa explícita — "Minha tradução para a sua linha é: ..." é afirmativo e escapava.
    // Ancorado no vocabulário de TAREFA, nunca em "desculpe"/"por favor" isolados, que são
    // fala legítima (50 dos 56 primeiros flags do corpus eram diálogo real).
    private static final Pattern PADRAO_META_DE_PAR = Pattern.compile(
        "minha\\s+tradu[çc][ãa]o"
            + "|tradu[çc][ãa]o\\s+(?:para|de)\\s+(?:a\\s+)?(?:sua\\s+)?linha"
            + "|para\\s+poder\\s+traduzir"
            + "|preciso\\s+(?:de|da|dessa)\\s+.{0,20}(?:linha|contexto|informa)"
            + "|(?:envi\\S+|mand\\S+)\\s+.{0,15}novamente"
            + "|pode\\s+me\\s+enviar",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    /**
     * PROPÓSITO DE NEGÓCIO: valida o texto visível de uma fala antes de sua
     * persistência, detectando resíduos e respostas fora do contrato PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: tags e comentários ASS são retirados somente
     * para a inspeção; o texto recebido nunca é modificado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto vazio é aceito; violação lança
     * {@link AlucinacaoDetectadaException} com a fala original no diagnóstico.
     */
    public void validarFala(String textoTraduzido) {
        if (textoTraduzido == null || textoTraduzido.trim().isEmpty()) {
            return;
        }

        String visivel = PADRAO_BLOCO_ASS.matcher(textoTraduzido).replaceAll("").trim();
        if (visivel.isEmpty()) {
            return;
        }

        if (temResiduoRelevante(visivel)) {
            throw new AlucinacaoDetectadaException("Resíduo gringo detectado: " + textoTraduzido);
        }

        if (PADRAO_CONTRACAO_INGLES.matcher(visivel).find()) {
            throw new AlucinacaoDetectadaException("Resíduo gringo detectado (contração): " + textoTraduzido);
        }

        if (PADRAO_OUTRO_IDIOMA.matcher(visivel).find()) {
            throw new AlucinacaoDetectadaException("Idioma incorreto detectado (não é PT-BR): " + textoTraduzido);
        }

        if (PADRAO_PREAMBULO.matcher(visivel).find()) {
            throw new AlucinacaoDetectadaException("Preâmbulo detectado: " + textoTraduzido);
        }

        if (PADRAO_RECUSA_META.matcher(visivel).find()) {
            throw new AlucinacaoDetectadaException("Recusa/meta-resposta do LLM detectada: " + textoTraduzido);
        }

        // Marcador de falha do pipeline Python antigo encontrado em legendas
        // legadas (ex.: "[ERRO_TRADUCAO: The Garanden!]" na G-Reconguista).
        // Garante retradução na revisão mesmo quando o conteúdo não dispara
        // o padrão de resíduo em inglês.
        if (visivel.contains("ERRO_TRADUCAO")) {
            throw new AlucinacaoDetectadaException("Marcador de erro de tradução detectado: " + textoTraduzido);
        }

        if (PADRAO_ASTERISCO.matcher(visivel).find()) {
            throw new AlucinacaoDetectadaException("Censura/markdown com asterisco detectado: " + textoTraduzido);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida a fala CONTRA O ORIGINAL que a gerou, detectando o defeito
     * que {@link #validarFala(String)} é estruturalmente incapaz de ver — texto em PT-BR
     * impecável, sem resíduo, sem preâmbulo e sem vocabulário de tarefa, mas que <b>não está
     * ancorado no original</b>. É a classe medida em produção no run de Guilty Crown: 27
     * entradas publicadas nas legendas, entre elas {@code "No."} traduzido como
     * {@code "Shu: Eu não posso... usar o King's Power..."} e {@code "Souta!"} como uma ficha
     * inteira do roster. Nenhuma delas dispara qualquer regra que olhe só a saída.
     *
     * <p><b>Invariantes do domínio</b>
     * <ul>
     *   <li>O invariante violado é ANCORAGEM, não comprimento: a tradução carrega material que
     *       o original não sustenta. Comprimento é sintoma, e por isso a desproporção é apenas
     *       uma das três evidências — sozinha ela deixaria 8 dos 27 casos passar.</li>
     *   <li>Os três limiares saíram de MEDIÇÃO sobre as 5.794 entradas reais do run, não de
     *       estimativa: juntos capturam 26 dos 27 defeitos com ZERO falso positivo. Mexer
     *       neles sem repetir a medição troca precisão comprovada por chute.</li>
     *   <li>Prefixo de locutor só é defeito quando NÃO vem do original: {@code "Gai—"}
     *       traduzido como {@code "Gai:"} é troca de pontuação, não invenção de falante —
     *       exigir conteúdo após os dois-pontos OU prefixo não ancorado elimina esses três
     *       falsos positivos sem perder {@code "Done!" -> "Linha 1:"}.</li>
     *   <li>Dois-pontos legítimo herdado do original nunca acusa, e conectivos de discurso
     *       ({@code "Repito:"}, {@code "Atenção:"}) estão na allowlist — {@code "I repeat:"}
     *       traduzido como {@code "Repito:"} é tradução correta.</li>
     * </ul>
     *
     * <p><b>Comportamento em caso de falha</b>
     * Texto vazio, original ausente ou fala que sobra vazia após limpar tags são aceitos: sem
     * um par comparável não há ancoragem a medir, e reprovar aí trocaria uma checagem inútil
     * por uma pendência inventada. Violação lança {@link AlucinacaoDetectadaException} com o
     * diagnóstico, e o chamador preserva o original.
     *
     * @param textoOriginal fala de origem que produziu a tradução
     * @param textoTraduzido fala devolvida pelo tradutor (LLM ou fallback de máquina)
     */
    public void validarPar(String textoOriginal, String textoTraduzido) {
        if (textoOriginal == null || textoTraduzido == null) {
            return;
        }
        String original = visivel(textoOriginal);
        String traduzido = visivel(textoTraduzido);
        if (original.isEmpty() || traduzido.isEmpty()) {
            return;
        }

        if (PADRAO_META_DE_PAR.matcher(traduzido).find()) {
            throw new AlucinacaoDetectadaException(
                "Meta-resposta sobre a tarefa de traduzir: \"" + traduzido + "\" (original: \"" + original + "\")");
        }

        if (temLocutorInventado(original, traduzido)) {
            throw new AlucinacaoDetectadaException(
                "Locutor/narração inventado, ausente no original: \"" + traduzido
                    + "\" (original: \"" + original + "\")");
        }

        if (traduzido.length() >= COMPRIMENTO_MINIMO_PARA_DESPROPORCAO
            && original.length() <= COMPRIMENTO_MAXIMO_ORIGINAL_CURTO
            && traduzido.length() >= original.length() * FATOR_DESPROPORCAO) {
            throw new AlucinacaoDetectadaException(
                "Tradução desproporcional ao original (" + original.length() + " -> "
                    + traduzido.length() + " caracteres), conteúdo não ancorado: \"" + traduzido
                    + "\" (original: \"" + original + "\")");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: isola o texto EXIBIDO, sem tags ASS nem marcadores do pipeline, que
     * é o único material comparável entre original e tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove blocos {@code {...}} e marcadores {@code [[TAGn]]} e
     * colapsa a quebra {@code \N} em espaço; o texto recebido nunca é modificado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula vira string vazia.
     */
    private String visivel(String texto) {
        if (texto == null) {
            return "";
        }
        String semTags = PADRAO_BLOCO_ASS.matcher(texto).replaceAll("");
        return PADRAO_MARCADOR.matcher(semTags).replaceAll("").replace("\\N", " ").trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta o prefixo {@code "Nome:"} que o modelo cria quando decide
     * narrar quem fala em vez de traduzir a fala.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige que o original NÃO traga o mesmo recurso; poupa
     * conectivos de discurso legítimos; e só acusa quando há conteúdo após os dois-pontos ou
     * quando o próprio prefixo não aparece no original — é essa segunda condição que separa
     * {@code "Gai—" -> "Gai:"} (legítimo) de {@code "Done!" -> "Linha 1:"} (inventado).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer texto sem dois-pontos devolve falso.
     */
    private boolean temLocutorInventado(String original, String traduzido) {
        java.util.regex.Matcher m = PADRAO_PREFIXO_LOCUTOR.matcher(traduzido);
        if (!m.matches()) {
            return false;
        }
        if (PADRAO_CONECTIVO_DISCURSO.matcher(traduzido).find()) {
            return false;
        }
        if (PADRAO_PREFIXO_LOCUTOR.matcher(original).matches()) {
            return false;
        }
        boolean temConteudoDepois = !m.group(2).trim().isEmpty();
        return temConteudoDepois || !prefixoVemDoOriginal(m.group(1), original);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se o prefixo antes dos dois-pontos foi tirado do original
     * ou inventado, comparando palavra a palavra sem acento e sem caixa.
     *
     * <p>INVARIANTES DO DOMÍNIO: TODAS as palavras do prefixo precisam existir no original como
     * palavra inteira; uma só palavra estranha basta para caracterizar invenção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo que normaliza para vazio conta como ancorado,
     * porque não há evidência de invenção.
     */
    private boolean prefixoVemDoOriginal(String prefixo, String original) {
        String originalCercado = " " + normalizarParaAncoragem(original) + " ";
        for (String palavra : normalizarParaAncoragem(prefixo).split("\\s+")) {
            if (!palavra.isEmpty() && !originalCercado.contains(" " + palavra + " ")) {
                return false;
            }
        }
        return true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: forma canônica de comparação entre original e tradução — sem
     * acento, sem caixa e sem pontuação, para que "Inori" e "inori," sejam a mesma palavra.
     *
     * <p>INVARIANTES DO DOMÍNIO: só normaliza para COMPARAR; nada normalizado é persistido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve string vazia.
     */
    private String normalizarParaAncoragem(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = java.text.Normalizer
            .normalize(texto, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diferencia resíduo inglês visível de palavra que é
     * também nome próprio de personagem.
     * <p>INVARIANTES DO DOMÍNIO: a exceção vale somente para ocorrência
     * capitalizada cadastrada; outro token inglês na mesma fala continua
     * produzindo bloqueio, como em {@code Will transform}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de correspondência retorna
     * falso; qualquer resíduo inequívoco retorna verdadeiro.
     */
    private boolean temResiduoRelevante(String texto) {
        var matcher = PADRAO_RESIDUO.matcher(texto);
        while (matcher.find()) {
            String palavra = matcher.group(1);
            boolean capitalizada = palavra.length() > 1
                && Character.isUpperCase(palavra.charAt(0))
                && !palavra.equals(palavra.toUpperCase());
            boolean nomeProprioProvavel = capitalizada
                && RESIDUOS_TAMBEM_NOMES.contains(palavra.toLowerCase());
            if (!nomeProprioProvavel) {
                return true;
            }
        }
        return false;
    }
}
