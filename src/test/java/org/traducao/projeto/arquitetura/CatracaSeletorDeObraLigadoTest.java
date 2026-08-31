package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o seletor de obra é o que liga a tela à API de informações do anime —
 * capa, sinopse, ficha. Ele só funciona se estiver ligado em <b>quatro</b> pontos do
 * {@code app.js}, e cada ponto faltando produz um defeito DIFERENTE e silencioso.
 *
 * <h2>Os quatro pontos, e o que cada ausência causa</h2>
 * <table>
 *   <tr><th>ponto</th><th>se faltar</th></tr>
 *   <tr><td>lista de {@code carregarContextosAuxiliares}</td>
 *       <td>o {@code <select>} fica em <b>"Carregando contextos..."</b> para sempre</td></tr>
 *   <tr><td>{@code todosSelects}</td>
 *       <td>a tela não acompanha a troca de obra feita em outra tela</td></tr>
 *   <tr><td>{@code mapeamentoFormularios}</td>
 *       <td>o banner existe no HTML e <b>nunca preenche</b> — capa em branco, sem erro</td></tr>
 *   <tr><td>{@code ehAuxiliar}</td>
 *       <td>a tela não ganha a opção "Sem obra" e a trava prende o operador</td></tr>
 * </table>
 *
 * <p>Nenhum deles quebra o carregamento da página: a tela abre, parece certa, e falha só quando
 * alguém tenta usar. É o tipo de defeito que só aparece com o operador na frente.
 *
 * <h2>O prejuízo que originou (27/08/2026)</h2>
 * Três telas com campos de pasta — <b>1.2 Extração</b>, <b>1.4 Análise de Legenda</b> e
 * <b>5.1 Remuxer</b> — não tinham seletor de obra nenhum, enquanto outras onze tinham. Quem
 * extraía uma faixa não via de que anime era; quem remuxava idem. Ao ligá-las foi preciso tocar
 * os quatro pontos, e a facilidade de esquecer um deles é a razão desta catraca.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo {@code <select id="X-contexto">} de qualquer HTML estático está nos três lugares.</li>
 *   <li>Todo {@code bannerId} do mapa existe como id em algum HTML — mapa apontando para banner
 *       inexistente é código morto que parece vivo.</li>
 *   <li>Todo {@code meta-banner-*} do HTML está no mapa — banner órfão nunca preenche.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia a tela e QUAL dos pontos falta. Alvo vazio REPROVA: zero seletores encontrados seria o
 * padrão do HTML ter mudado, e zero violações sairia como aprovação.
 */
@DisplayName("seletor de obra: ligado nos quatro pontos, em toda tela que o tem")
class CatracaSeletorDeObraLigadoTest {

    private static final Path ESTATICOS = Path.of("src/main/resources/static");
    private static final Path APP_JS = Path.of("src/main/resources/static/js/app.js");

    private static String todosOsHtml() throws IOException {
        StringBuilder tudo = new StringBuilder();
        try (Stream<Path> s = Files.walk(ESTATICOS)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".html")).toList()) {
                tudo.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
            }
        }
        return tudo.toString();
    }

    private static Set<String> achar(String texto, String padrao) {
        Set<String> fora = new LinkedHashSet<>();
        Matcher m = Pattern.compile(padrao).matcher(texto);
        while (m.find()) {
            fora.add(m.group(1));
        }
        return fora;
    }

    /** O trecho entre o marcador e o primeiro {@code ]} — onde vivem as listas de ids. */
    private static String listaApos(String app, String marcador) {
        int i = app.indexOf(marcador);
        if (i < 0) {
            return "";
        }
        int j = app.indexOf(']', i);
        return j < 0 ? "" : app.substring(i, j);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) dos leitores — eles acham o que existe e
     * calam no que não existe.
     *
     * <p>Sem isto, um {@code app.js} reestruturado devolveria listas vazias, nenhuma violação e
     * build verde: a guarda aprovaria por não enxergar nada.
     */
    @Test
    @DisplayName("CONTROLE: os leitores acham o que existe e calam no que nao existe")
    void leitoresCalibrados() {
        String htmlDeControle = "<select id=\"teste-contexto\"> <div id=\"meta-banner-teste\">";
        assertEquals(Set.of("teste-contexto"),
            achar(htmlDeControle, "<select[^>]*id=\"([a-z0-9-]+-contexto)\""));
        assertEquals(Set.of("meta-banner-teste"),
            achar(htmlDeControle, "id=\"(meta-banner-[a-z0-9-]+)\""));
        assertTrue(achar("<div>nada aqui</div>", "<select[^>]*id=\"([a-z0-9-]+-contexto)\"")
            .isEmpty(), "o leitor inventou seletor onde nao ha");

        assertTrue(listaApos("nao tem marcador", "carregarContextosAuxiliares([").isEmpty(),
            "o leitor de lista devolveu conteudo sem achar o marcador");
    }

    @Test
    @DisplayName("todo seletor de obra esta nas duas listas E no mapa de metadados")
    void seletorLigadoNosTresLugares() throws IOException {
        String html = todosOsHtml();
        String app = Files.readString(APP_JS, StandardCharsets.UTF_8);

        Set<String> seletores = achar(html, "<select[^>]*id=\"([a-z0-9-]+-contexto)\"");
        assertTrue(seletores.size() >= 10,
            "NAO VERIFICADO: achei " + seletores.size() + " seletores de obra no HTML. Sao mais "
                + "de dez; isso e o padrao do id ter mudado, e nao as telas terem sumido.");

        Set<String> naCarga = achar(listaApos(app, "carregarContextosAuxiliares(["),
            "'([a-z0-9-]+-contexto)'");
        Set<String> emTodos = achar(listaApos(app, "const todosSelects = ["),
            "'([a-z0-9-]+-contexto)'");
        Set<String> noMapa = achar(app, "selectId: '([a-z0-9-]+-contexto)'");
        assertTrue(!naCarga.isEmpty() && !emTodos.isEmpty() && !noMapa.isEmpty(),
            "NAO VERIFICADO: uma das tres listas do app.js veio vazia — carga=" + naCarga.size()
                + " todos=" + emTodos.size() + " mapa=" + noMapa.size());

        List<String> problemas = new ArrayList<>();
        for (String s : seletores) {
            List<String> falta = new ArrayList<>();
            if (!naCarga.contains(s)) {
                falta.add("lista de carga (o select fica em 'Carregando contextos...' para sempre)");
            }
            if (!emTodos.contains(s)) {
                falta.add("todosSelects (nao acompanha a troca de obra feita em outra tela)");
            }
            if (!noMapa.contains(s)) {
                falta.add("mapa de metadados (o banner nunca preenche)");
            }
            if (!falta.isEmpty()) {
                problemas.add(s + " -> falta em: " + String.join(" · ", falta));
            }
        }
        assertTrue(problemas.isEmpty(),
            "SELETOR DE OBRA MAL LIGADO:\n  " + String.join("\n  ", problemas));
    }

    @Test
    @DisplayName("banner e mapa se cobrem nos dois sentidos")
    void bannerEmapaSeCobrem() throws IOException {
        String html = todosOsHtml();
        String app = Files.readString(APP_JS, StandardCharsets.UTF_8);

        Set<String> bannersNoHtml = achar(html, "id=\"(meta-banner-[a-z0-9-]+)\"");
        assertTrue(bannersNoHtml.size() >= 10,
            "NAO VERIFICADO: achei " + bannersNoHtml.size() + " banners no HTML — padrao mudou");

        Map<String, String> mapa = new LinkedHashMap<>();
        Matcher m = Pattern.compile("selectId: '([^']+)', bannerId: '([^']+)'").matcher(app);
        while (m.find()) {
            mapa.put(m.group(1), m.group(2));
        }
        assertTrue(!mapa.isEmpty(), "NAO VERIFICADO: o mapa de metadados veio vazio");

        List<String> apontamMalOrfaos = new ArrayList<>();
        mapa.forEach((sel, ban) -> {
            if (!bannersNoHtml.contains(ban)) {
                apontamMalOrfaos.add(sel + " aponta para o banner '" + ban
                    + "', que nao existe em HTML nenhum");
            }
        });
        Set<String> orfaos = new LinkedHashSet<>(bannersNoHtml);
        orfaos.removeAll(mapa.values());
        orfaos.forEach(b -> apontamMalOrfaos.add("o banner '" + b
            + "' esta no HTML e NAO esta no mapa — ele nunca vai preencher"));

        assertTrue(apontamMalOrfaos.isEmpty(),
            "BANNER DA API DE ANIME DESLIGADO:\n  " + String.join("\n  ", apontamMalOrfaos));
    }
}
