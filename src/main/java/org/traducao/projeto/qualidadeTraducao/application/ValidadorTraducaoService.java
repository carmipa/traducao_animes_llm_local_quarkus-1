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

    // Palavras do PADRAO_RESIDUO que também são nomes próprios comuns em
    // anime/mangá (ex.: o personagem "Will"). Uma ocorrência Capitalizada
    // dessas palavras numa frase PT-BR é quase sempre nome, não resíduo —
    // sem esta exceção a fala inteira era rejeitada e mantida sem tradução.
    private static final java.util.Set<String> RESIDUOS_TAMBEM_NOMES = java.util.Set.of("will");

    // Blocos {...} do ASS (tags de override e comentários de fansub) não são
    // texto exibido: valida-los como fala gerava falso positivo (comentário
    // legítimo em inglês tipo "{Yes, ma'am}" disparava resíduo e queimava uma
    // chamada de LLM à toa) e falso negativo (preâmbulo depois de tag, como
    // "{\i1}Tradução: ...", escapava da âncora ^ do padrão de preâmbulo).
    private static final Pattern PADRAO_BLOCO_ASS = Pattern.compile("\\{[^}]*\\}");

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

        // Marcador de falha do pipeline Python antigo encontrado em legendas
        // legadas (ex.: "[ERRO_TRADUCAO: The Garanden!]" na G-Reconguista).
        // Garante retradução na revisão mesmo quando o conteúdo não dispara
        // o padrão de resíduo em inglês.
        if (visivel.contains("ERRO_TRADUCAO")) {
            throw new AlucinacaoDetectadaException("Marcador de erro de tradução detectado: " + textoTraduzido);
        }
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
