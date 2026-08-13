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
 * @param modeloHerdado modelo cujas traduções foram REAPROVEITADAS por este cache, quando o reuso
 *                      entre modelos foi autorizado; {@code null} no caso normal
 */
// A omissão do campo nulo na serialização é decidida na INFRAESTRUTURA (mixin em
// CacheTraducaoService), não aqui: este pacote é puro por contrato de arquitetura e não conhece
// Jackson. Duas catracas guiaram este desenho — a de compatibilidade exigiu que o JSON regravado
// siga estruturalmente idêntico ao legado, e a de fronteira recusou a anotação no domínio.
public record ProvenienciaCache(
    int schemaVersion,
    String contextoId,
    String contextoHash,
    String modeloLlm,
    String idiomaOrigem,
    String idiomaDestino,
    String modeloHerdado
) {
    public static final int SCHEMA_ATUAL = 1;

    /**
     * PROPÓSITO DE NEGÓCIO: a forma de SEMPRE — proveniência sem herança, que é o caso normal.
     *
     * <p>INVARIANTES DO DOMÍNIO: existe para que acrescentar {@code modeloHerdado} não obrigue os
     * chamadores a passar {@code null} em toda construção, e para que o acervo já gravado — 94.721
     * falas em 12/08/2026 — continue desserializando sem o campo novo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida; é delegação pura.
     */
    public ProvenienciaCache(int schemaVersion, String contextoId, String contextoHash,
            String modeloLlm, String idiomaOrigem, String idiomaDestino) {
        this(schemaVersion, contextoId, contextoHash, modeloLlm, idiomaOrigem, idiomaDestino, null);
    }

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
        // modeloHerdado NÃO entra na comparação, e isso é deliberado: ele registra COMO o cache foi
        // formado, não PARA QUE ele serve. Compará-lo invalidaria todo o acervo já gravado (que o
        // traz nulo) no instante em que o campo nasceu — e o cache de um experimento herdado
        // continua reaproveitável na execução seguinte do mesmo modelo, que é o desejado.
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se duas gerações divergem SOMENTE no modelo — mesma lore, mesmo
     * prompt, mesmo par de idiomas, mesmo schema. É a condição que autoriza um modelo a reaproveitar
     * o trabalho de outro quando o operador pede isso explicitamente.
     *
     * <h2>Por que isto NÃO é o comportamento padrão</h2>
     * Trocar o modelo e reusar o cache faz a proveniência afirmar que a aya traduziu o que o
     * mistral traduziu, e é a proveniência que sustenta toda comparação entre modelos — foi ela que
     * permitiu medir mistral × aya no Unicorn em 12/08. Um reuso silencioso destruiria essa
     * capacidade de forma invisível e irreversível, porque o cache é regravado.
     *
     * <p>O caso legítimo é o EXPERIMENTO: rodar 50 episódios do Zeta para exercitar 6 falas
     * pendentes custa 17.090 traduções desperdiçadas. Aí o reuso é o que torna o teste viável — e o
     * preço é carimbar {@code modeloHerdado}, para o cache dizer a verdade sobre o que ele é.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Exige igualdade dos CINCO outros campos. Lore diferente continua invalidando: a
     *       tradução muda com o prompt, e reaproveitá-la seria servir texto de outra obra.</li>
     *   <li>Modelos IGUAIS devolvem {@code false} — o caso já é coberto por
     *       {@link #mesmaProveniencia} e confundir os dois faria a herança ser carimbada em
     *       reuso comum.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * {@code null} devolve {@code false}, como em {@link #mesmaProveniencia}.
     */
    public boolean divergeSomenteNoModelo(ProvenienciaCache outra) {
        if (outra == null) {
            return false;
        }
        return schemaVersion == outra.schemaVersion()
            && Objects.equals(contextoId, outra.contextoId)
            && Objects.equals(contextoHash, outra.contextoHash)
            && Objects.equals(idiomaOrigem, outra.idiomaOrigem)
            && Objects.equals(idiomaDestino, outra.idiomaDestino)
            && !Objects.equals(modeloLlm, outra.modeloLlm);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve esta proveniência declarando de quem ela herdou traduções.
     *
     * <p>INVARIANTES DO DOMÍNIO: não altera nenhum dos seis campos que decidem reuso — só acrescenta
     * a origem herdada, de modo que o cache resultante continue batendo consigo mesmo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: modelo nulo/branco devolve a proveniência inalterada, em
     * vez de gravar uma herança vazia que pareceria informação.
     */
    public ProvenienciaCache herdandoDe(String modeloAnterior) {
        if (modeloAnterior == null || modeloAnterior.isBlank()) {
            return this;
        }
        return new ProvenienciaCache(schemaVersion, contextoId, contextoHash, modeloLlm,
            idiomaOrigem, idiomaDestino, modeloAnterior);
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
