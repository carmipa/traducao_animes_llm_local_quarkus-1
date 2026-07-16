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
 * NÃO é normalizada: quando comparada à proveniência atual do pipeline, carimbada
 * com {@code SCHEMA_ATUAL}, uma versão ausente/{@code 0} ou divergente reprova a
 * compatibilidade e nunca é reutilizada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #hashDe(String)} nunca lança — se o
 * algoritmo SHA-256 faltar (não deve, é padrão da JVM), cai para o hashCode em
 * hexadecimal como último recurso. {@link #mesmaProveniencia} trata nulo como
 * "diferente"; no fluxo automático, versão ausente/{@code 0} materializada no cache
 * diverge de {@code SCHEMA_ATUAL} e leva ao arquivamento da geração anterior.
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

    /**
     * PROPÓSITO DE NEGÓCIO: determina se a proveniência armazenada coincide
     * exatamente com a proveniência atual fornecida pelo pipeline, autorizando (ou
     * não) o reaproveitamento das traduções em cache sem rechamar o LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara exatamente os seis campos (schemaVersion,
     * contextoId, contextoHash, modeloLlm, idiomaOrigem, idiomaDestino); o chamador
     * do fluxo automático deve fornecer a proveniência atual carimbada com
     * {@link #SCHEMA_ATUAL}; nenhuma normalização de schema é realizada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} retorna {@code false}; no fluxo
     * automático, um schema ausente/{@code 0} materializado no cache diverge de
     * {@link #SCHEMA_ATUAL} e causa o arquivamento pelo {@code CacheTraducaoService}.
     */
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
