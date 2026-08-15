package org.traducao.projeto.lore.domain;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: a IDENTIDADE CANÔNICA de uma obra — o conjunto de nomes pelos quais
 * ela é reconhecível na pasta onde os arquivos de legenda moram. Existe para que TODAS as
 * obras do catálogo (e não só as duas que declararam apelidos à mão) possam provar, de forma
 * determinística, que o arquivo prestes a ser traduzido pertence — ou não — à lore selecionada
 * no combo da UI. É a peça que generaliza a guarda nascida do incidente medido nesta árvore:
 * 15 caches de Gundam 0083 gravados com {@code contextoId = "guilty_crown"}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A identidade é DERIVADA, não declarada: sai de {@link ProvedorContexto#getId()} e de
 *       {@link ProvedorContexto#getNomeExibicao()}, normalizados. Toda obra do catálogo passa
 *       a ter identidade sem tocar em nenhuma lore. Os {@link ProvedorContexto#apelidosPasta()}
 *       são COMPLEMENTO — nomes alternativos e abreviações que o id e o rótulo de UI não
 *       cobrem (ex.: {@code "86"}, {@code "Z Gundam"}, {@code "Stardust Memory"}).</li>
 *   <li>Comparação DETERMINÍSTICA e nunca fuzzy: um nome canônico só casa quando aparece no
 *       nome da pasta como SEQUÊNCIA DE PALAVRAS INTEIRAS. Não há {@code contains} permissivo,
 *       prefixo, distância de edição nem similaridade — {@code "Crown"} jamais identifica
 *       Guilty Crown e {@code "86"} jamais casa dentro de {@code "(1986)"}.</li>
 *   <li>NÚMERO, ANO E SIGLA SÃO IDENTIDADE, não ruído: {@code 0083}, {@code 0080}, {@code 86},
 *       {@code ZZ}, {@code F91}, {@code NT} distinguem obras inteiras. Por isso a normalização
 *       PRESERVA dígitos e siglas e NÃO existe nenhuma remoção genérica de "ruído de release"
 *       (ano, resolução, grupo de fansub): remover {@code "0083"} por parecer um número
 *       apagaria a própria identidade da obra. O que neutraliza o ruído é a exigência de
 *       palavras inteiras, não uma lista de descarte.</li>
 *   <li>ESPECIFICIDADE resolve sobreposição legítima entre entradas do mesmo franchise
 *       ({@code "Macross 7"} × {@code "Macross 7 Encore"}): vence o nome canônico com MAIS
 *       PALAVRAS e, no empate, o mais longo em caracteres. É uma regra de ordem total sobre os
 *       nomes que de fato casaram — determinística, não uma pontuação de similaridade.</li>
 *   <li>Valor imutável e puro: só JDK, sem I/O, sem estado, sem relógio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança. Provedor nulo, id/nome nulos ou conjunto de apelidos nulo degradam para
 * identidade vazia, e identidade vazia nunca reconhece nada — o desfecho que os consumidores
 * tratam como INDETERMINADO (avisa e segue), jamais como divergência.
 *
 * @param contextoId id do contexto dono desta identidade (o mesmo carimbado na proveniência)
 * @param nomesCanonicos nomes normalizados que identificam a obra; possivelmente vazio
 */
public record IdentidadeObra(String contextoId, Set<String> nomesCanonicos) {

    /**
     * Peso de UMA palavra na medida de especificidade. Como os nomes canônicos são títulos de
     * obra (dezenas de caracteres, nunca milhares), multiplicar a contagem de palavras por este
     * fator e somar o comprimento produz uma ordem lexicográfica (palavras, caracteres) em um
     * único inteiro, sem risco de o desempate por caracteres ultrapassar uma palavra inteira.
     */
    private static final int PESO_PALAVRA = 1_000;

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a identidade seja de fato imutável, mesmo recebendo um
     * conjunto mutável de quem a monta.
     *
     * <p>INVARIANTES DO DOMÍNIO: o conjunto é copiado para versão imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto nulo vira conjunto vazio em vez de lançar.
     */
    public IdentidadeObra {
        nomesCanonicos = nomesCanonicos == null ? Set.of() : Set.copyOf(nomesCanonicos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: deriva a identidade canônica de um provedor de lore, unindo o que
     * ele já declara por obrigação de contrato (id e nome de exibição) ao que declara por
     * escolha (apelidos de pasta). É o que dá cobertura ao catálogo inteiro de uma vez.
     *
     * <p>INVARIANTES DO DOMÍNIO: as três fontes passam pela MESMA normalização
     * ({@link #normalizar(String)}); nomes que normalizam para vazio são descartados; a
     * deduplicação é natural (conjunto), então um apelido igual ao id não conta duas vezes. A
     * derivação não inventa variações — não quebra o título em pedaços, não gera acrônimos e
     * não remove palavras: só normaliza o que o provedor já diz.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: provedor nulo devolve identidade vazia; apelidos nulos
     * são ignorados. Nunca lança.
     *
     * @param provedor provedor de lore cuja identidade se quer derivar
     * @return identidade canônica do provedor; nunca {@code null}
     */
    public static IdentidadeObra de(ProvedorContexto provedor) {
        if (provedor == null) {
            return new IdentidadeObra(null, Set.of());
        }
        Set<String> nomes = new LinkedHashSet<>();
        acrescentar(nomes, provedor.getId());
        acrescentar(nomes, provedor.getNomeExibicao());
        Set<String> apelidos = provedor.apelidosPasta();
        if (apelidos != null) {
            for (String apelido : apelidos) {
                acrescentar(nomes, apelido);
            }
        }
        return new IdentidadeObra(provedor.getId(), nomes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde se o nome de uma pasta identifica esta obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: equivale a {@code especificidadeEm(nomeDaPasta) > 0} — não há
     * um segundo critério de reconhecimento em lugar nenhum do sistema.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo/em branco devolve {@code false}.
     *
     * @param nomeDaPasta nome da pasta da obra, cru como veio do sistema de arquivos
     * @return {@code true} quando algum nome canônico casa por palavras inteiras
     */
    public boolean reconhece(String nomeDaPasta) {
        return especificidadeEm(nomeDaPasta) > 0;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mede QUÃO especificamente esta obra se reconhece na pasta, para que
     * a entrada mais precisa do catálogo vença a mais genérica sem que isso vire ambiguidade.
     * Sem esta medida, a pasta {@code "Macross 7 Encore"} seria reivindicada tanto por
     * {@code macross_7} quanto por {@code macross_7_encore} e a execução seria bloqueada por uma
     * ambiguidade que não existe no mundo real.
     *
     * <p>INVARIANTES DO DOMÍNIO: considera SOMENTE nomes canônicos que casam como sequência de
     * palavras inteiras e devolve o maior peso entre eles, sendo o peso
     * {@code palavras * 1000 + caracteres} — ou seja, mais palavras vence sempre; no empate,
     * vence o mais longo. Zero significa "não reconhece" e é o único valor que não identifica a
     * obra. A medida é uma ordem determinística sobre casamentos EXATOS, nunca uma nota de
     * similaridade: um nome que não casou por palavras inteiras pontua zero, por mais parecido
     * que seja.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo/em branco ou identidade vazia devolvem
     * {@code 0}; nunca lança.
     *
     * @param nomeDaPasta nome da pasta da obra, cru como veio do sistema de arquivos
     * @return peso do casamento mais específico, ou {@code 0} se a obra não se reconhece ali
     */
    public int especificidadeEm(String nomeDaPasta) {
        String alvo = normalizar(nomeDaPasta);
        if (alvo.isEmpty() || nomesCanonicos.isEmpty()) {
            return 0;
        }
        String cercado = " " + alvo + " ";
        int melhor = 0;
        for (String nome : nomesCanonicos) {
            if (cercado.contains(" " + nome + " ")) {
                melhor = Math.max(melhor, pesoDe(nome));
            }
        }
        return melhor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reduz um nome de pasta, id ou apelido à forma canônica de
     * comparação, para que colchetes de fansub, acento, caixa e pontuação não decidam se uma
     * obra foi reconhecida.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove acentos, baixa a caixa em {@link Locale#ROOT} e colapsa
     * toda sequência de caracteres NÃO alfanuméricos em UM espaço, aparando as pontas. Dígitos
     * são preservados intactos — {@code "0083"}, {@code "86"} e {@code "F91"} são identidade, e
     * a normalização nunca os separa nem os descarta. É a ÚNICA normalização de nome de obra do
     * sistema: manter uma segunda cópia faria o catálogo e a guarda discordarem sobre o que é o
     * mesmo nome.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve string vazia em vez de lançar.
     *
     * @param texto nome de pasta, id ou apelido cru
     * @return forma normalizada, com palavras separadas por um único espaço
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static void acrescentar(Set<String> destino, String bruto) {
        String normalizado = normalizar(bruto);
        if (!normalizado.isEmpty()) {
            destino.add(normalizado);
        }
    }

    private static int pesoDe(String nomeCanonico) {
        int palavras = 1;
        for (int i = 0; i < nomeCanonico.length(); i++) {
            if (nomeCanonico.charAt(i) == ' ') {
                palavras++;
            }
        }
        return palavras * PESO_PALAVRA + Math.min(nomeCanonico.length(), PESO_PALAVRA - 1);
    }
}
