package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: corrige localmente contradições linguísticas inequívocas
 * antes de consultar um LLM, preservando tom, lore e restante da fala.
 *
 * <p>INVARIANTES DO DOMÍNIO: somente relações explícitas no original, expressões
 * canônicas e incidentes já comprovados recebem substituição determinística;
 * contexto ambíguo nunca é reescrito.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada ausente ou regra não comprovada
 * devolve {@link Optional#empty()} e mantém a tradução atual.
 */
@Service
public class CorretorDeterministicoConcordanciaService {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;
    private static final Pattern GRACAS_AO_DEUS = Pattern.compile("\\bgraças ao deus\\b", FLAGS);
    private static final Pattern FILHO_DA_MAE = Pattern.compile("\\bfilho da mãe\\b", FLAGS);
    private static final Pattern PORRA_ISOLADA = Pattern.compile("^\\s*porra!\\s*$", FLAGS);
    private static final Pattern INSULTO_FORTE_EN = Pattern.compile(
        "\\bson of a (?:bitch|hitch)\\b|\\bson of a\\s*\\.\\.\\.", FLAGS);
    private static final Pattern INSULTO_INTERROMPIDO_EN = Pattern.compile(
        "\\b(?:you\\s+)?son of a\\s*\\.\\.\\.", FLAGS);
    private static final Pattern ARTIGO_MOBILE_SUIT = Pattern.compile(
        "(?<![\\p{L}\\p{N}])a(?=\\s+mobile\\s+(?:suit|armor)\\b)", FLAGS);
    private static final Pattern POSSESSIVO_FEM_COM_PARENTE_MASC = Pattern.compile(
        "\\b(minha|sua|nossa)\\s+(pai|filho|irmão)\\b", FLAGS);
    private static final Pattern POSSESSIVO_MASC_COM_PARENTE_FEM = Pattern.compile(
        "\\b(meu|seu|nosso)\\s+(mãe|filha|irmã)\\b", FLAGS);

    private static final List<RegraParentesco> REGRAS_PARENTESCO = List.of(
        regra("father|dad|daddy", "mother|mom|mommy|mum|mummy", "mãe|mae|mamãe|mamae", "pai"),
        regra("mother|mom|mommy|mum|mummy", "father|dad|daddy", "pai|papai", "mãe"),
        regra("son", "daughter", "filha", "filho"),
        regra("daughter", "son", "filho", "filha"),
        regra("brother", "sister", "irmã|irma", "irmão"),
        regra("sister", "brother", "irmão|irmao", "irmã")
    );

    /**
     * PROPÓSITO DE NEGÓCIO: produz uma proposta mínima para erros objetivos que
     * não justificam custo nem liberdade criativa de um modelo generativo.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags ASS e palavras não relacionadas permanecem
     * byte a byte; substituições preservam a capitalização inicial encontrada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nenhuma alteração comprovada devolve
     * vazio; a validação final continua sob responsabilidade do caso de uso.
     */
    public Optional<String> corrigir(String originalIngles, String traducaoAtual) {
        if (traducaoAtual == null || traducaoAtual.isBlank()) {
            return Optional.empty();
        }

        String corrigida = substituirPreservandoInicial(traducaoAtual, GRACAS_AO_DEUS, "graças a Deus");
        if (originalIngles != null && !originalIngles.isBlank()) {
            if (ARTIGO_MOBILE_SUIT.matcher(originalIngles).find()) {
                corrigida = substituirPreservandoInicial(corrigida, ARTIGO_MOBILE_SUIT, "um");
            }
            for (RegraParentesco regra : REGRAS_PARENTESCO) {
                if (regra.esperadaEn().matcher(originalIngles).find()
                    && !regra.opostaEn().matcher(originalIngles).find()) {
                    corrigida = substituirPreservandoInicial(corrigida, regra.incorretaPt(), regra.corretaPt());
                }
            }
            if (INSULTO_FORTE_EN.matcher(originalIngles).find()) {
                corrigida = substituirPreservandoInicial(corrigida, FILHO_DA_MAE, "filho da puta");
            }
            if (INSULTO_INTERROMPIDO_EN.matcher(originalIngles).find()
                && PORRA_ISOLADA.matcher(corrigida).matches()) {
                corrigida = "Seu filho da puta!";
            }
        }
        corrigida = ajustarPossessivosDeParentesco(corrigida);

        return corrigida.equals(traducaoAtual) ? Optional.empty() : Optional.of(corrigida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recompõe o possessivo que acompanha um parentesco
     * trocado, produzindo `meu pai` em vez de `minha pai`.
     * <p>INVARIANTES DO DOMÍNIO: somente possessivos imediatamente anteriores a
     * pai/mãe, filho/filha ou irmão/irmã são ajustados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: frase sem essa combinação permanece
     * byte a byte igual.
     */
    private String ajustarPossessivosDeParentesco(String texto) {
        String ajustado = POSSESSIVO_FEM_COM_PARENTE_MASC.matcher(texto).replaceAll(resultado -> {
            String possessivo = switch (resultado.group(1).toLowerCase()) {
                case "minha" -> "meu";
                case "sua" -> "seu";
                default -> "nosso";
            };
            return capitalizarComo(resultado.group(1), possessivo) + " " + resultado.group(2);
        });
        return POSSESSIVO_MASC_COM_PARENTE_FEM.matcher(ajustado).replaceAll(resultado -> {
            String possessivo = switch (resultado.group(1).toLowerCase()) {
                case "meu" -> "minha";
                case "seu" -> "sua";
                default -> "nossa";
            };
            return capitalizarComo(resultado.group(1), possessivo) + " " + resultado.group(2);
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a capitalização inicial ao trocar uma
     * palavra gramatical no começo da fala.
     * <p>INVARIANTES DO DOMÍNIO: somente o primeiro caractere é ajustado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: referência vazia devolve a substituta.
     */
    private String capitalizarComo(String referencia, String substituta) {
        if (referencia == null || referencia.isEmpty()
            || !Character.isUpperCase(referencia.charAt(0))) {
            return substituta;
        }
        return Character.toUpperCase(substituta.charAt(0)) + substituta.substring(1);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria uma regra bilateral de parentesco usada tanto
     * para exigir evidência inglesa quanto para limitar a palavra alterada no PT.
     * <p>INVARIANTES DO DOMÍNIO: padrões usam fronteiras de palavra e comparação
     * Unicode sem diferenciar maiúsculas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: expressão regular inválida interrompe a
     * inicialização, impedindo execução com regra parcialmente construída.
     */
    private static RegraParentesco regra(
        String esperadaEn,
        String opostaEn,
        String incorretaPt,
        String corretaPt
    ) {
        return new RegraParentesco(
            Pattern.compile("\\b(?:" + esperadaEn + ")\\b", FLAGS),
            Pattern.compile("\\b(?:" + opostaEn + ")\\b", FLAGS),
            Pattern.compile("\\b(?:" + incorretaPt + ")\\b", FLAGS),
            corretaPt);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca apenas o termo comprovadamente incorreto sem
     * transformar início de frase em palavra minúscula.
     * <p>INVARIANTES DO DOMÍNIO: todas as ocorrências casadas recebem o mesmo
     * termo e nenhuma parte fora do casamento é normalizada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de casamento devolve o texto
     * original sem alocação semântica adicional.
     */
    private String substituirPreservandoInicial(String texto, Pattern padrao, String substituta) {
        Matcher matcher = padrao.matcher(texto);
        return matcher.replaceAll(resultado -> {
            String encontrada = resultado.group();
            if (!encontrada.isEmpty() && Character.isUpperCase(encontrada.charAt(0))) {
                return Character.toUpperCase(substituta.charAt(0)) + substituta.substring(1);
            }
            return substituta;
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: representa uma equivalência familiar segura entre
     * a referência inglesa e a forma brasileira esperada.
     * <p>INVARIANTES DO DOMÍNIO: todos os campos são imutáveis e não nulos.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o record não executa I/O nem aplica
     * substituições por conta própria.
     */
    private record RegraParentesco(
        Pattern esperadaEn,
        Pattern opostaEn,
        Pattern incorretaPt,
        String corretaPt
    ) {}
}
