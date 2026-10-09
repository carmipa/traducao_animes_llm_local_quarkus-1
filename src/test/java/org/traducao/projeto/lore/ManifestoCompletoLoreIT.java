package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: linha de base COMPLETA da lore antes de ela sair do {@code lore.yaml} para
 * SQLite (FASE 1 do plano de 09/10/2026). Congela, por obra, por lado (tradução e revisão) e por
 * campo, o hash do valor que a produção entrega pelo CDI. A migração está certa quando este arquivo
 * continua idêntico depois da troca de fonte — sem regenerar nada.
 *
 * <h2>A lacuna que ele fecha</h2>
 * O que existia antes congelava metade:
 * <pre>
 *   manifesto-lore.properties ....... tradução: id + nome + prompt + termosProtegidos (um hash só)
 *   baseline-terminologia-lore.tsv .. correcoesTerminologia (catraca de um lado só)
 *   baseline-campos-lore.tsv ........ apelidos, pares, visibilidade (catraca de um lado só)
 *   prompt de REVISÃO ............... NADA
 *   equivalenciasAceitas ............ NADA
 *   traducoesObrigatorias ........... NADA
 * </pre>
 * Uma migração que trocasse um byte do prompt de revisão, ou perdesse uma equivalência, passaria
 * por todas as guardas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Igualdade nos dois sentidos</b>, e não catraca: campo PERDIDO, NOVO ou MUDADO reprova.
 *       A migração não acrescenta nem tira nada; mudança deliberada de lore regenera este arquivo no
 *       mesmo commit, com o motivo.</li>
 *   <li><b>O prompt é hasheado CRU</b>, por {@link ProvenienciaCache#hashDe(String)} — o mesmo
 *       algoritmo do {@code contextoHash} do cache. Nada de remover {@code \r}: um {@code \r} a mais
 *       muda o hash do cache e joga fora a tradução da obra, e é exatamente isso que tem de reprovar.</li>
 *   <li><b>Ordem conta onde o consumidor a enxerga.</b> Conjuntos e mapas são ordenados antes do hash
 *       (a iteração de {@code Set.copyOf} muda a cada JVM). Pares inconfundíveis e as listas de
 *       equivalência guardam a ordem declarada: é a que o carregador novo tem de reproduzir.</li>
 *   <li>O gabarito é o catálogo vivo de hoje, lido pelo leitor de produção (A3). Ele não sai do
 *       carregador novo.</li>
 *   <li>Os controles moram aqui dentro: o instrumento é visto reprovando um byte, um termo e uma
 *       troca de ordem de par, e aceitando a troca de ordem de um conjunto (A1).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova listando cada divergência por obra, lado e campo. Manifesto ausente, vazio, truncado
 * ({@code #!total=}) ou com linha malformada reprova como defeito, nunca como aprovação. Com
 * {@code -Dkronos.lore.manifesto.regravar=true} grava o arquivo versionado a partir do vivo e
 * retorna — regenerar é rodar o teste, nunca digitar hash.
 */
@QuarkusTest
@DisplayName("FASE 1 (lore em SQLite): manifesto completo — obra x lado x campo")
class ManifestoCompletoLoreIT {

    static final Path VERSIONADO = Path.of("src", "test", "resources", "lore", "manifesto-lore-completo.tsv");
    static final Path SNAPSHOT_VIVO = Path.of("build", "tmp", "manifesto-lore-completo.gerado.tsv");
    static final String FLAG_REGRAVAR = "kronos.lore.manifesto.regravar";
    static final String DIRETIVA_TOTAL = "#!total=";
    static final String TRADUCAO = "traducao";
    static final String REVISAO = "revisao";

    @Inject
    List<ProvedorContexto> catalogoTraducao;

    @Inject
    List<ProvedorPromptRevisaoLore> catalogoRevisao;

    /**
     * PROPÓSITO DE NEGÓCIO: um campo de uma obra num lado, reduzido ao hash do valor canônico.
     * <p>INVARIANTES DO DOMÍNIO: {@code itens} é informativo (caracteres de texto, elementos de
     * coleção) e entra na comparação junto com o hash.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    record Linha(String obra, String lado, String campo, int itens, String hash) {
        String chave() {
            return obra + "\t" + lado + "\t" + campo;
        }

        String tsv() {
            return chave() + "\t" + itens + "\t" + hash;
        }
    }

    @Test
    @DisplayName("o catálogo vivo bate campo a campo com o manifesto completo versionado")
    void catalogoBateComOManifesto() throws IOException {
        assertFalse(catalogoTraducao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorContexto no CDI");
        assertFalse(catalogoRevisao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorPromptRevisaoLore no CDI");

        Map<String, Linha> vivo = linhasDoCatalogo(catalogoTraducao, catalogoRevisao);
        String texto = serializar(vivo);
        Files.createDirectories(SNAPSHOT_VIVO.getParent());
        Files.writeString(SNAPSHOT_VIVO, texto, StandardCharsets.UTF_8);

        if (Boolean.getBoolean(FLAG_REGRAVAR)) {
            Files.writeString(VERSIONADO, texto, StandardCharsets.UTF_8);
            System.out.println("[manifesto-lore-completo] regravado a partir do catálogo vivo: " + VERSIONADO
                + " (" + vivo.size() + " linhas)");
            return;
        }

        assertTrue(Files.exists(VERSIONADO), () -> "NÃO VERIFICADO: " + VERSIONADO + " não existe. Gere com "
            + "gradlew test --tests \"*ManifestoCompletoLoreIT*\" -D" + FLAG_REGRAVAR + "=true");
        Map<String, Linha> esperado = ler(Files.readString(VERSIONADO, StandardCharsets.UTF_8));

        List<String> divergencias = divergencias(esperado, vivo);
        assertTrue(divergencias.isEmpty(), () -> "A lore viva diverge do manifesto completo ("
            + divergencias.size() + " campo(s)). Se a mudança foi DELIBERADA, regenere com -D" + FLAG_REGRAVAR
            + "=true no mesmo commit e diga o porquê. Vivo gravado em " + SNAPSHOT_VIVO + ".\n  "
            + String.join("\n  ", divergencias));
    }

    /**
     * Controle do instrumento, sem depender do arquivo versionado: um byte a mais no prompt de
     * revisão de UMA obra tem de produzir UMA divergência, nomeando obra, lado e campo — e nada no
     * lado da tradução da mesma obra.
     */
    @Test
    @DisplayName("controle: um byte no prompt de revisão reprova nomeando obra, lado e campo")
    void controleUmByteNoPromptDeRevisao() {
        Map<String, Linha> base = linhasDoCatalogo(catalogoTraducao, catalogoRevisao);
        ProvedorPromptRevisaoLore alvo = catalogoRevisao.getFirst();

        List<ProvedorPromptRevisaoLore> mutado = new ArrayList<>(catalogoRevisao);
        mutado.set(0, revisaoCom(alvo, alvo.obterPromptSistema() + " ", alvo.termosProtegidos()));

        List<String> d = divergencias(base, linhasDoCatalogo(catalogoTraducao, mutado));
        assertEquals(1, d.size(), () -> "esperada exatamente uma divergência, vieram: " + d);
        assertTrue(d.getFirst().startsWith("MUDOU " + alvo.getId() + "\t" + REVISAO + "\tprompt"),
            () -> "a divergência não nomeou obra/lado/campo: " + d.getFirst());
    }

    /**
     * Controle de FRONTEIRA (A1): os dois casos carregam o mesmo sinal — a ordem de iteração mudou.
     * Num conjunto (termos protegidos) a ordem não é conteúdo e NÃO pode reprovar; num par
     * inconfundível a ordem é a declarada e TEM de reprovar. Um termo a menos reprova sempre.
     */
    @Test
    @DisplayName("controle: ordem de conjunto passa, ordem de par e termo perdido reprovam")
    void controleOrdemDeConjuntoParETermoPerdido() {
        Map<String, Linha> base = linhasDoCatalogo(catalogoTraducao, catalogoRevisao);

        ProvedorContexto comTermos = catalogoTraducao.stream()
            .filter(p -> p.termosProtegidos().size() >= 2).findFirst()
            .orElseThrow(() -> new AssertionError("NÃO VERIFICADO: nenhuma obra com 2+ termos protegidos"));
        List<String> invertidos = new ArrayList<>(new TreeSet<>(comTermos.termosProtegidos()).descendingSet());
        Map<String, Linha> reordenado = linhasDoCatalogo(
            trocar(catalogoTraducao, traducaoCom(comTermos, new LinkedHashSet<>(invertidos), comTermos.paresInconfundiveis())),
            catalogoRevisao);
        assertEquals(List.of(), divergencias(base, reordenado),
            "reordenar um CONJUNTO reprovou — o instrumento acusaria a iteração do Set.copyOf como defeito");

        Set<String> menosUm = new LinkedHashSet<>(invertidos.subList(1, invertidos.size()));
        List<String> perdeu = divergencias(base, linhasDoCatalogo(
            trocar(catalogoTraducao, traducaoCom(comTermos, menosUm, comTermos.paresInconfundiveis())), catalogoRevisao));
        assertEquals(1, perdeu.size(), () -> "termo perdido devia dar uma divergência: " + perdeu);
        assertTrue(perdeu.getFirst().startsWith("MUDOU " + comTermos.getId() + "\t" + TRADUCAO + "\ttermosProtegidos"),
            () -> "termo perdido não nomeou o campo: " + perdeu.getFirst());

        ProvedorContexto comPares = catalogoTraducao.stream()
            .filter(p -> p.paresInconfundiveis().size() >= 2).findFirst()
            .orElseThrow(() -> new AssertionError("NÃO VERIFICADO: nenhuma obra com 2+ pares inconfundíveis"));
        List<List<String>> pares = new ArrayList<>(comPares.paresInconfundiveis());
        java.util.Collections.reverse(pares);
        List<String> parTrocado = divergencias(base, linhasDoCatalogo(
            trocar(catalogoTraducao, traducaoCom(comPares, comPares.termosProtegidos(), new LinkedHashSet<>(pares))),
            catalogoRevisao));
        assertEquals(1, parTrocado.size(), () -> "ordem de par trocada devia dar uma divergência: " + parTrocado);
        assertTrue(parTrocado.getFirst().startsWith("MUDOU " + comPares.getId() + "\t" + TRADUCAO + "\tparesInconfundiveis"),
            () -> "a troca de ordem de par não nomeou o campo: " + parTrocado.getFirst());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reduz os dois catálogos às linhas do manifesto.
     * <p>INVARIANTES DO DOMÍNIO: chave única por obra/lado/campo — id repetido num lado reprova, porque
     * a segunda obra apagaria a primeira em silêncio no mapa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code fail} nomeando o id repetido.
     */
    static Map<String, Linha> linhasDoCatalogo(List<ProvedorContexto> traducao, List<ProvedorPromptRevisaoLore> revisao) {
        Map<String, Linha> linhas = new TreeMap<>();
        Set<String> vistosTraducao = new TreeSet<>();
        for (ProvedorContexto p : traducao) {
            String id = p.getId();
            if (!vistosTraducao.add(id)) {
                fail("id repetido no lado da tradução: " + id);
            }
            por(linhas, id, TRADUCAO, "nome", p.getNomeExibicao().length(), ProvenienciaCache.hashDe(p.getNomeExibicao()));
            por(linhas, id, TRADUCAO, "prompt", p.obterPromptSistema().length(), ProvenienciaCache.hashDe(p.obterPromptSistema()));
            por(linhas, id, TRADUCAO, "termosProtegidos", p.termosProtegidos().size(), hash(conjunto(p.termosProtegidos())));
            por(linhas, id, TRADUCAO, "correcoesTerminologia", p.correcoesTerminologia().size(), hash(mapa(p.correcoesTerminologia())));
            por(linhas, id, TRADUCAO, "traducoesObrigatorias", p.traducoesObrigatorias().size(), hash(mapa(p.traducoesObrigatorias())));
            por(linhas, id, TRADUCAO, "paresInconfundiveis", p.paresInconfundiveis().size(), hash(pares(p.paresInconfundiveis())));
            por(linhas, id, TRADUCAO, "apelidosPasta", p.apelidosPasta().size(), hash(conjunto(p.apelidosPasta())));
            String visivel = String.valueOf(p.apareceNaListaDeObras());
            por(linhas, id, TRADUCAO, "apareceNaListaDeObras", visivel.length(), hash(visivel));
        }
        Set<String> vistosRevisao = new TreeSet<>();
        for (ProvedorPromptRevisaoLore r : revisao) {
            String id = r.getId();
            if (!vistosRevisao.add(id)) {
                fail("id repetido no lado da revisão: " + id);
            }
            por(linhas, id, REVISAO, "nome", r.getNomeExibicao().length(), ProvenienciaCache.hashDe(r.getNomeExibicao()));
            por(linhas, id, REVISAO, "prompt", r.obterPromptSistema().length(), ProvenienciaCache.hashDe(r.obterPromptSistema()));
            por(linhas, id, REVISAO, "termosProtegidos", r.termosProtegidos().size(), hash(conjunto(r.termosProtegidos())));
            por(linhas, id, REVISAO, "correcoesTerminologia", r.correcoesTerminologia().size(), hash(mapa(r.correcoesTerminologia())));
            por(linhas, id, REVISAO, "equivalenciasAceitas", r.equivalenciasAceitas().size(), hash(equivalencias(r.equivalenciasAceitas())));
        }
        return linhas;
    }

    private static void por(Map<String, Linha> linhas, String obra, String lado, String campo, int itens, String hash) {
        Linha l = new Linha(obra, lado, campo, itens, hash);
        linhas.put(l.chave(), l);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compara manifesto e vivo nos dois sentidos.
     * <p>INVARIANTES DO DOMÍNIO: cada divergência começa por {@code PERDIDO}, {@code NOVO} ou
     * {@code MUDOU}, seguida da chave — é o que os controles conferem.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; devolve a lista.
     */
    static List<String> divergencias(Map<String, Linha> esperado, Map<String, Linha> vivo) {
        List<String> d = new ArrayList<>();
        for (Linha e : esperado.values()) {
            Linha v = vivo.get(e.chave());
            if (v == null) {
                d.add("PERDIDO " + e.chave() + " (tinha " + e.itens() + " itens)");
            } else if (!v.hash().equals(e.hash()) || v.itens() != e.itens()) {
                d.add("MUDOU " + e.chave() + " (itens " + e.itens() + " -> " + v.itens() + ")");
            }
        }
        for (Linha v : vivo.values()) {
            if (!esperado.containsKey(v.chave())) {
                d.add("NOVO " + v.chave() + " (" + v.itens() + " itens)");
            }
        }
        return d;
    }

    static String serializar(Map<String, Linha> linhas) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MANIFESTO COMPLETO DA LORE — FASE 1 do plano da lore em SQLite (09/10/2026).\n");
        sb.append("# Formato: <obra>\\t<traducao|revisao>\\t<campo>\\t<itens>\\t<sha256 do valor canonico>\n");
        sb.append("# Gerado por ManifestoCompletoLoreIT a partir do catalogo vivo do CDI. Nao editar a mao:\n");
        sb.append("# regenerar com -D").append(FLAG_REGRAVAR).append("=true, no mesmo commit da mudanca deliberada.\n");
        sb.append(DIRETIVA_TOTAL).append(linhas.size()).append('\n');
        for (Linha l : linhas.values()) {
            sb.append(l.tsv()).append('\n');
        }
        return sb.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê o manifesto versionado.
     * <p>INVARIANTES DO DOMÍNIO: cinco campos por TAB; {@code #!total=} conferido; linha que não se
     * entende é defeito, nunca descarte.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code fail} nomeando a linha.
     */
    static Map<String, Linha> ler(String texto) {
        Map<String, Linha> linhas = new TreeMap<>();
        Integer total = null;
        int numero = 0;
        for (String linha : texto.lines().toList()) {
            numero++;
            if (linha.startsWith(DIRETIVA_TOTAL)) {
                total = Integer.parseInt(linha.substring(DIRETIVA_TOTAL.length()).trim());
                continue;
            }
            if (linha.isBlank() || linha.startsWith("#")) {
                continue;
            }
            String[] c = linha.split("\t", -1);
            if (c.length != 5 || !c[4].matches("[0-9a-f]{64}") || !c[3].matches("\\d+")
                || !(TRADUCAO.equals(c[1]) || REVISAO.equals(c[1]))) {
                fail("manifesto malformado na linha " + numero + ": " + linha);
            }
            Linha l = new Linha(c[0], c[1], c[2], Integer.parseInt(c[3]), c[4]);
            if (linhas.put(l.chave(), l) != null) {
                fail("manifesto com chave repetida na linha " + numero + ": " + l.chave());
            }
        }
        if (linhas.isEmpty()) {
            fail("NÃO VERIFICADO: manifesto sem nenhuma linha de dado");
        }
        if (total == null || total != linhas.size()) {
            fail("manifesto declara " + DIRETIVA_TOTAL + total + " e tem " + linhas.size()
                + " linhas — truncado ou editado à mão");
        }
        return linhas;
    }

    // ---- forma canônica: cada valor vira "<comprimento>:<texto>", o que torna a junção inequívoca.

    private static String item(String s) {
        return s.length() + ":" + s + ";";
    }

    private static String conjunto(Collection<String> valores) {
        StringBuilder sb = new StringBuilder();
        new TreeSet<>(valores).forEach(v -> sb.append(item(v)));
        return sb.toString();
    }

    private static String mapa(Map<String, String> valores) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(valores).forEach((k, v) -> sb.append(item(k)).append('=').append(item(v)));
        return sb.toString();
    }

    private static String pares(Set<List<String>> valores) {
        StringBuilder sb = new StringBuilder();
        for (List<String> par : valores) {
            sb.append('[');
            par.forEach(v -> sb.append(item(v)));
            sb.append(']');
        }
        return sb.toString();
    }

    private static String equivalencias(Map<String, List<String>> valores) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(valores).forEach((k, formas) -> {
            sb.append(item(k)).append("=[");
            formas.forEach(f -> sb.append(item(f)));
            sb.append(']');
        });
        return sb.toString();
    }

    private static String hash(String canonico) {
        return ProvenienciaCache.hashDe(canonico);
    }

    // ---- cópias para os controles: delegam tudo ao original, trocando só o campo do caso.

    private static List<ProvedorContexto> trocar(List<ProvedorContexto> todos, ProvedorContexto novo) {
        List<ProvedorContexto> copia = new ArrayList<>();
        for (ProvedorContexto p : todos) {
            copia.add(p.getId().equals(novo.getId()) ? novo : p);
        }
        return copia;
    }

    private static ProvedorContexto traducaoCom(ProvedorContexto o, Set<String> termos, Set<List<String>> pares) {
        return new ProvedorContexto() {
            @Override public String getId() { return o.getId(); }
            @Override public String getNomeExibicao() { return o.getNomeExibicao(); }
            @Override public String obterPromptSistema() { return o.obterPromptSistema(); }
            @Override public Set<String> termosProtegidos() { return termos; }
            @Override public Map<String, String> correcoesTerminologia() { return o.correcoesTerminologia(); }
            @Override public Map<String, String> traducoesObrigatorias() { return o.traducoesObrigatorias(); }
            @Override public Set<List<String>> paresInconfundiveis() { return pares; }
            @Override public Set<String> apelidosPasta() { return o.apelidosPasta(); }
            @Override public boolean apareceNaListaDeObras() { return o.apareceNaListaDeObras(); }
        };
    }

    private static ProvedorPromptRevisaoLore revisaoCom(ProvedorPromptRevisaoLore o, String prompt, Set<String> termos) {
        return new ProvedorPromptRevisaoLore() {
            @Override public String getId() { return o.getId(); }
            @Override public String getNomeExibicao() { return o.getNomeExibicao(); }
            @Override public String obterPromptSistema() { return prompt; }
            @Override public Set<String> termosProtegidos() { return termos; }
            @Override public Map<String, List<String>> equivalenciasAceitas() { return o.equivalenciasAceitas(); }
            @Override public Map<String, String> correcoesTerminologia() { return o.correcoesTerminologia(); }
        };
    }
}
