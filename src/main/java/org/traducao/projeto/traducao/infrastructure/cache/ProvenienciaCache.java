package org.traducao.projeto.traducao.infrastructure.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * PROPÓSITO DE NEGÓCIO: Carimba cada cache de tradução com a origem que o
 * produziu — qual lore/contexto, qual hash do prompt de sistema, qual modelo e
 * qual par de idiomas. É o que permite provar com o que uma tradução em cache
 * foi feita e impedir que uma melhoria de lore reuse silenciosamente traduções
 * antigas.
 *
 * <p>INVARIANTES DO DOMÍNIO: duas proveniências só são "a mesma" se contextoId,
 * contextoHash, modeloLlm e os dois idiomas baterem. O hash é derivado do prompt
 * de sistema ativo (SHA-256), então qualquer mudança de lore/regra muda o hash.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #hashDe(String)} nunca lança — se o
 * algoritmo SHA-256 faltar (não deve, é padrão da JVM), cai para o hashCode em
 * hexadecimal como último recurso. {@link #mesmaProveniencia} trata nulo como
 * "diferente".
 */
public record ProvenienciaCache(
    int schemaVersion,
    String contextoId,
    String contextoHash,
    String modeloLlm,
    String idiomaOrigem,
    String idiomaDestino
) {
    public static final int SCHEMA_ATUAL = 1;

    public boolean mesmaProveniencia(ProvenienciaCache outra) {
        if (outra == null) {
            return false;
        }
        return Objects.equals(contextoId, outra.contextoId)
            && Objects.equals(contextoHash, outra.contextoHash)
            && Objects.equals(modeloLlm, outra.modeloLlm)
            && Objects.equals(idiomaOrigem, outra.idiomaOrigem)
            && Objects.equals(idiomaDestino, outra.idiomaDestino);
    }

    public static String hashDe(String conteudo) {
        if (conteudo == null) {
            return "0";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(conteudo.hashCode());
        }
    }
}
