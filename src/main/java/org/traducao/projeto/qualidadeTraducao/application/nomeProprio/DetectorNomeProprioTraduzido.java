package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: descobre que o modelo traduziu um nome próprio da obra — {@code Aoshima}
 * virando outra coisa — quando NÃO existe lore declarada para proibir isso.
 *
 * <h2>O QUE ESTE DETECTOR NÃO PEGA, medido antes de ser ligado</h2>
 * Ele só enxerga nome que <b>nenhum</b> dos dicionários conhece. Nome próprio que também é palavra
 * comum passa ileso, e isso foi conferido palavra a palavra em 13/08/2026:
 * <pre>
 *   Blackwood  pt=não  en=não       de=não   -&gt; ACUSA
 *   Sky        pt=não  en=CONHECE   de=não   -&gt; escapa
 *   Heinz      pt=não  en=CONHECE   de=CONHECE -&gt; escapa (17 falas do Magnetic Rose)
 *   Rose       pt=CONHECE ...                -&gt; escapa
 * </pre>
 * O {@code Sky -> Céu} do Javadoc de {@code ContextoSemLore} está entre os que escapam, e não é
 * defeito de implementação: é irredutível. Para o dicionário, {@code Sky} e {@code Never} são a
 * mesma coisa — inglês comum capitalizado. Acusar um obriga a acusar o outro, e acusar
 * {@code Never} é literalmente o falso positivo de 57,7% que derrubou a versão anterior. Separar
 * os dois exige saber que a obra tem um personagem chamado Sky, que é precisamente o que a lore
 * declara e o {@code sem_lore} não tem.
 *
 * <p>Portanto o número que este detector produz é <b>PISO, nunca teto</b>: nome achado é nome
 * traduzido de fato, mas silêncio dele não prova que nenhum nome se perdeu.
 *
 * <h2>O buraco que este detector cobre</h2>
 * Numa obra com lore, quem protege nome próprio é {@code correcoesTerminologia()}, que restaura a
 * grafia oficial sem depender do modelo. Em {@code ContextoSemLore} esse mapa é VAZIO por
 * definição — declarar termo de uma obra que ninguém estudou seria inventar vocabulário. O
 * resultado, declarado no Javadoc do próprio contexto, é que a única barreira contra nome próprio
 * traduzido passa a ser a instrução em prosa do prompt, e prosa não é mecanismo.
 *
 * <h2>Por que o dicionário resolve o que a heurística sozinha não resolvia</h2>
 * A regra "palavra capitalizada tem de sobreviver" foi medida e descartada no projeto: 323 de 560
 * pendências eram falso positivo. A causa era não distinguir {@code Sky} — que não é palavra de
 * idioma nenhum — de {@code Never}, que é inglês comum capitalizado por ênfase. Os quatro
 * dicionários fazem exatamente essa distinção: só {@link VeredictoPalavra#DESCONHECIDA} — nem
 * português, nem inglês, nem alemão, nem japonês — vira candidata a nome da obra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Não corrige nada.</b> Devolve diagnóstico para o relatório. Reescrever fala a partir de
 *       heurística de nome é o caminho conhecido para o falso positivo em massa voltar.</li>
 *   <li><b>Uma consulta ao dicionário por execução</b>, nunca por fala. Consultar por fala já custou
 *       28m16s -&gt; 68m01s num episódio do Unicorn.</li>
 *   <li><b>Falha fechada:</b> dicionário indisponível devolve
 *       {@link VeredictoNomeProprio#naoVerificado()}, jamais um resultado limpo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nunca lança. Qualquer exceção do dicionário vira NÃO VERIFICADO.
 */
@ApplicationScoped
public class DetectorNomeProprioTraduzido {

    private final CorretorOrtograficoLegenda dicionario;

    @Inject
    public DetectorNomeProprioTraduzido(CorretorOrtograficoLegenda dicionario) {
        this.dicionario = dicionario;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: examina a execução inteira de uma vez e aponta as falas em que um nome
     * próprio do original não chegou à tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o dicionário é consultado UMA vez, com a união das candidatas de
     * todas as falas. Falas com tradução em branco (pendentes) são ignoradas — ausência ali é
     * pendência, não nome traduzido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link VeredictoNomeProprio#naoVerificado()} quando o
     * dicionário não responde, quando o mapa é nulo ou quando qualquer exceção escapa.
     *
     * @param traducoes original em inglês -&gt; tradução em português
     */
    public VeredictoNomeProprio verificarLote(Map<String, String> traducoes) {
        if (traducoes == null || traducoes.isEmpty()) {
            return VeredictoNomeProprio.limpo(0);
        }
        try {
            Map<String, Set<String>> candidatasPorFala = extrairCandidatas(traducoes);
            Set<String> uniao = unir(candidatasPorFala);
            if (uniao.isEmpty()) {
                return VeredictoNomeProprio.limpo(0);
            }

            Set<String> nomesDaObra = filtrarDesconhecidas(uniao);
            if (nomesDaObra == null) {
                return VeredictoNomeProprio.naoVerificado();
            }
            if (nomesDaObra.isEmpty()) {
                return VeredictoNomeProprio.limpo(uniao.size());
            }
            return compararComTraducao(traducoes, candidatasPorFala, nomesDaObra, uniao.size());
        } catch (RuntimeException e) {
            return VeredictoNomeProprio.naoVerificado();
        }
    }

    private static Map<String, Set<String>> extrairCandidatas(Map<String, String> traducoes) {
        Map<String, Set<String>> porFala = new LinkedHashMap<>();
        for (Map.Entry<String, String> par : traducoes.entrySet()) {
            String traduzido = par.getValue();
            if (traduzido == null || traduzido.isBlank()) {
                continue;
            }
            Set<String> c = ExtratorCandidatosNomeProprio.candidatas(par.getKey());
            if (!c.isEmpty()) {
                porFala.put(par.getKey(), c);
            }
        }
        return porFala;
    }

    private static Set<String> unir(Map<String, Set<String>> porFala) {
        Set<String> uniao = new LinkedHashSet<>();
        porFala.values().forEach(uniao::addAll);
        return uniao;
    }

    /**
     * Devolve só as palavras que nenhum dos quatro idiomas reconhece, ou {@code null} quando o
     * dicionário não respondeu — o {@code null} é o que distingue "não há nome" de "não olhei".
     */
    private Set<String> filtrarDesconhecidas(Set<String> uniao) {
        Map<String, VeredictoPalavra> vereditos = dicionario.classificar(uniao);
        if (vereditos == null || vereditos.isEmpty()) {
            return null;
        }
        Set<String> desconhecidas = new LinkedHashSet<>();
        for (String palavra : uniao) {
            VeredictoPalavra v = vereditos.get(palavra);
            if (v == null || v == VeredictoPalavra.NAO_VERIFICADO) {
                // Uma única palavra sem veredito já invalida o lote: dizer "nenhum nome perdido"
                // tendo deixado de examinar palavra é o falso verde que a regra de três estados
                // existe para impedir.
                return null;
            }
            // ROMAJI entra JUNTO com DESCONHECIDA, e isso não é descuido.
            //
            // O dicionário ja_ROMAJI vem do IPADIC, que é morfológico e cheio de nome próprio —
            // ele reconhece "Aoshima", personagem do Memories, e o arquivo abre em "aarajima",
            // "aatsukawa". Se ROMAJI significasse "palavra conhecida, logo não é nome da obra", o
            // detector ficaria cego justamente no caso mais comum em anime: nome japonês.
            //
            // E há a razão positiva: romaji que some da tradução é defeito do mesmo jeito. Romaji
            // não se traduz — é a regra do karaokê do projeto, e vale para o diálogo também.
            if (v == VeredictoPalavra.DESCONHECIDA || v == VeredictoPalavra.ROMAJI) {
                desconhecidas.add(palavra);
            }
        }
        return desconhecidas;
    }

    private static VeredictoNomeProprio compararComTraducao(Map<String, String> traducoes,
                                                            Map<String, Set<String>> candidatasPorFala,
                                                            Set<String> nomesDaObra,
                                                            int examinadas) {
        Map<String, List<String>> perdidos = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> fala : candidatasPorFala.entrySet()) {
            String traduzido = traducoes.get(fala.getKey());
            List<String> ausentes = new ArrayList<>();
            for (String candidata : fala.getValue()) {
                if (nomesDaObra.contains(candidata) && !apareceNoTexto(traduzido, candidata)) {
                    ausentes.add(candidata);
                }
            }
            if (!ausentes.isEmpty()) {
                perdidos.put(fala.getKey(), List.copyOf(ausentes));
            }
        }
        return perdidos.isEmpty()
            ? VeredictoNomeProprio.limpo(examinadas)
            : new VeredictoNomeProprio(perdidos, true, examinadas);
    }

    /**
     * Busca por palavra inteira e sem distinguir caixa. Sem distinguir caixa de propósito: o
     * modelo às vezes muda a capitalização do nome, e isso é outro defeito — bem menor — que não
     * pode contaminar esta medição.
     */
    private static boolean apareceNoTexto(String texto, String palavra) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String alvo = texto.toLowerCase(Locale.ROOT);
        String agulha = palavra.toLowerCase(Locale.ROOT);
        int i = alvo.indexOf(agulha);
        while (i >= 0) {
            boolean antesLivre = i == 0 || !Character.isLetter(alvo.charAt(i - 1));
            int fim = i + agulha.length();
            boolean depoisLivre = fim >= alvo.length() || !Character.isLetter(alvo.charAt(fim));
            if (antesLivre && depoisLivre) {
                return true;
            }
            i = alvo.indexOf(agulha, i + 1);
        }
        return false;
    }
}
