package org.traducao.projeto.traducao.application.contextocena;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: deriva a ASSINATURA CONTEXTUAL de uma fala — uma chave determinística
 * calculada SÓ a partir da fonte (índice, texto original, estilo, vizinhas originais e versão
 * da política), para o cache da correção por contexto de cena distinguir a MESMA fala em
 * cenas diferentes (ex.: "Thank you." em duas cenas com falantes distintos), que hoje
 * colapsam numa tradução só. É o que impede o colapso de identidade sem depender de nenhum
 * "palpite" de gênero (a chave nunca é probabilística — evita circularidade).
 *
 * <p>INVARIANTES DO DOMÍNIO: função PURA e determinística — mesmas entradas ⇒ mesma
 * assinatura; qualquer diferença de índice, original, estilo, vizinhas ou versão de política
 * muda a assinatura; NADA da tradução ou do gênero inferido entra na chave (só fonte). A
 * assinatura é o hex de um SHA-256 (64 caracteres), estável entre execuções e máquinas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos nulos são tratados como vazios (nunca lança);
 * se o algoritmo SHA-256 faltar (não deve, é padrão da JVM) cai para um hash hexadecimal do
 * conteúdo canônico como último recurso.
 */
@Service
public class ChaveadorContextual {

    // Separadores de controle para o texto canônico — evitam colisão por concatenação
    // ambígua (ex.: "ab"+"c" vs "a"+"bc"). Unit Separator (0x1F) entre campos,
    // Record Separator (0x1E) entre vizinhas. Definidos por código numérico para não
    // dependerem de caracteres invisíveis no fonte.
    private static final String SEP_CAMPO = String.valueOf((char) 0x1F);
    private static final String SEP_VIZINHA = String.valueOf((char) 0x1E);

    /**
     * PROPÓSITO DE NEGÓCIO: calcula a assinatura contextual só-fonte de uma fala-alvo.
     * <p>INVARIANTES DO DOMÍNIO: determinística; sensível a índice, original, estilo,
     * vizinhas e versão da política; insensível a qualquer coisa da tradução.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos viram vazio; nunca lança.
     *
     * @param indice posição ordinal da fala na legenda
     * @param original texto original da fala-alvo
     * @param estilo estilo/categoria da fala-alvo
     * @param vizinhasOriginais textos originais das falas vizinhas (contexto), em ordem
     * @param politicaVersao versão da política contextual (formato de mensagem/janela)
     * @return assinatura hex SHA-256 (64 caracteres)
     */
    public String assinatura(int indice, String original, String estilo,
            List<String> vizinhasOriginais, String politicaVersao) {
        StringBuilder canonico = new StringBuilder();
        canonico.append(indice).append(SEP_CAMPO)
            .append(nvl(estilo)).append(SEP_CAMPO)
            .append(nvl(original)).append(SEP_CAMPO);
        if (vizinhasOriginais != null) {
            for (int i = 0; i < vizinhasOriginais.size(); i++) {
                if (i > 0) {
                    canonico.append(SEP_VIZINHA);
                }
                canonico.append(nvl(vizinhasOriginais.get(i)));
            }
        }
        canonico.append(SEP_CAMPO).append(nvl(politicaVersao));
        return sha256Hex(canonico.toString());
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(String conteudo) {
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
