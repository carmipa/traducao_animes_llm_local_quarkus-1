package org.traducao.projeto.cachetraducao.domain;

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
 * <p>INVARIANTES DO DOMÍNIO: duas proveniências só são "a mesma" se os SEIS campos
 * baterem por igualdade exata — schemaVersion, contextoId, contextoHash, modeloLlm,
 * idiomaOrigem e idiomaDestino. O hash é derivado do prompt de sistema ativo
 * (SHA-256), então qualquer mudança de lore/regra muda o hash. A versão de schema
 * NÃO é normalizada: um objeto sem o campo é materializado com {@code 0} e, como
 * {@code 0 != SCHEMA_ATUAL}, é tratado como incompatível — nunca reutilizado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #hashDe(String)} nunca lança — se o
 * algoritmo SHA-256 faltar (não deve, é padrão da JVM), cai para o hashCode em
 * hexadecimal como último recurso. {@link #mesmaProveniencia} trata nulo como
 * "diferente"; versão ausente/{@code 0} ou divergente de {@code SCHEMA_ATUAL}
 * reprova a compatibilidade e leva ao arquivamento da geração anterior.
 *
 * @param schemaVersion versão do schema do documento de cache persistido
 * @param contextoId identificador do lore/contexto usado na geração
 * @param contextoHash hash SHA-256 do prompt de sistema ativo
 * @param modeloLlm identificador do modelo LLM que gerou as traduções
 * @param idiomaOrigem código do idioma de origem
 * @param idiomaDestino código do idioma de destino
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
        return schemaVersion == outra.schemaVersion()
            && Objects.equals(contextoId, outra.contextoId)
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
