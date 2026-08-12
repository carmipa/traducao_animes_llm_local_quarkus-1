package org.traducao.projeto.qualidadeTraducao.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe acentos em palavras que o LLM às vezes devolve sem acento
 * (visto no 08th: {@code infancia}, {@code ate}). Age SÓ sobre formas cuja grafia sem acento
 * NUNCA é uma palavra válida em português — assim a correção é determinística e não muda
 * sentido. Complementa a revisão gramatical do modelo com uma rede determinística barata.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Dicionário CURADO e conservador: só entram formas cuja versão sem acento não existe em
 *       PT ({@code nao}, {@code voce}, {@code tambem}, {@code ate}, {@code infancia}, ...). São
 *       DELIBERADAMENTE excluídos homógrafos e casos que dependem de sentido — {@code esta}
 *       (this)≠{@code está} (is), {@code e} (and)≠{@code é} (is), {@code as vezes} (the times)≠
 *       {@code às vezes} (sometimes) — para nunca introduzir erro.</li>
 *   <li>Substituição por fronteira de palavra (não casa {@code ate} dentro de {@code Kate}),
 *       preservando a caixa do achado (Nao→Não, NAO→NÃO, nao→não).</li>
 *   <li>Classe sem estado; só JDK + Spring. Não conhece cache, LLM nem legenda.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto {@code null}/vazio ou sem nenhuma forma do dicionário é devolvido intacto; nunca lança.
 */
@Component
public class NormalizadorAcentosComuns {

    /**
     * Fronteira de início do termo, com a quebra {@code \N} do ASS tratada como separador.
     *
     * <p>{@code \N} ocupa DOIS caracteres — contrabarra e a letra {@code N} — e o {@code N} é letra
     * para {@code \p{L}}. Sem a alternativa, {@code "voce"} colado na quebra vira sufixo de um
     * {@code "Nvoce"} inexistente e o acento nunca é reposto.
     *
     * <p><b>MEDIDO no cache do acervo em 2026-08-04</b> (60.891 falas traduzidas, 24,6% com a
     * quebra): das 12 formas do dicionário, <b>0</b> sobreviveram SOLTAS e <b>11</b> sobreviveram
     * COLADAS na quebra. Como este normalizador roda em {@code ProcessarArquivoUseCase} ANTES de
     * gravar, o cache é experimento natural: forma solta foi SEMPRE corrigida, forma colada NUNCA
     * foi. As 11 são "para\Nvoce", "voz,\Nnao vai ganhar", "se\Nvoce era um Cyber-Newtype".
     *
     * <p>A reposição usa {@code appendReplacement} sobre o grupo, e o lookbehind é de largura zero
     * — a quebra não é consumida nem reescrita.
     */
    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    // Forma-sem-acento (nunca palavra válida em PT) -> forma acentuada canônica.
    private static final Map<String, String> CORRECOES = Map.ofEntries(
        Map.entry("nao", "não"),
        Map.entry("voce", "você"),
        Map.entry("voces", "vocês"),
        Map.entry("tambem", "também"),
        Map.entry("ate", "até"),
        Map.entry("alem", "além"),
        Map.entry("apos", "após"),
        Map.entry("atras", "atrás"),
        Map.entry("infancia", "infância"),
        Map.entry("ninguem", "ninguém"),
        Map.entry("alguem", "alguém"),
        Map.entry("porem", "porém"),

        // ---------------------------------------------------------------
        // AMPLIAÇÃO DE 07/08/2026 — as seis primeiras foram MEDIDAS.
        //
        // Auditadas 10.918 falas das duas obras traduzidas na noite de 06/08
        // (Gundam 0083 Stardust Memory, 15 eps; Gundam 08th MS Team, 13 eps):
        // 74 falas (0,68%) saíram com a forma sem acento. A contagem, com a
        // fronteira que trata a quebra \N do ASS:
        //
        //   35x capitao   26x entao   7x estao   3x razao
        //    1x pressao    1x esquadrao
        //
        // "sera" apareceu 1x e ficou de FORA de propósito: pode ser nome
        // próprio, e a invariante deste mapa exige forma que nunca seja palavra
        // válida. Uma ocorrência não paga o risco de estragar um nome.
        //
        // O resto é a mesma família -ão/-ção, toda com a mesma propriedade: a
        // forma sem til não existe em português. Entram porque o defeito é do
        // modelo com a terminação, não com a palavra específica — e esperar
        // cada uma aparecer numa auditoria seria pagar o erro antes de corrigir.
        // ---------------------------------------------------------------
        Map.entry("capitao", "capitão"),
        Map.entry("capitaes", "capitães"),
        Map.entry("entao", "então"),
        Map.entry("estao", "estão"),
        Map.entry("razao", "razão"),
        Map.entry("razoes", "razões"),
        Map.entry("pressao", "pressão"),
        Map.entry("esquadrao", "esquadrão"),
        Map.entry("batalhao", "batalhão"),

        Map.entry("irmao", "irmão"),
        Map.entry("irmaos", "irmãos"),
        Map.entry("mae", "mãe"),
        Map.entry("maes", "mães"),
        Map.entry("coracao", "coração"),
        Map.entry("tres", "três"),
        Map.entry("comeco", "começo"),

        Map.entry("missao", "missão"),
        Map.entry("missoes", "missões"),
        Map.entry("posicao", "posição"),
        Map.entry("posicoes", "posições"),
        Map.entry("atencao", "atenção"),
        Map.entry("decisao", "decisão"),
        Map.entry("decisoes", "decisões"),
        Map.entry("licao", "lição"),
        Map.entry("nacao", "nação"),
        Map.entry("uniao", "união"),
        Map.entry("visao", "visão"),
        Map.entry("tensao", "tensão"),
        Map.entry("invasao", "invasão"),
        Map.entry("explosao", "explosão"),
        Map.entry("operacao", "operação"),
        Map.entry("operacoes", "operações"),
        Map.entry("situacao", "situação"),
        Map.entry("informacao", "informação"),
        Map.entry("informacoes", "informações"),
        Map.entry("explicacao", "explicação"),
        Map.entry("reacao", "reação"),
        Map.entry("direcao", "direção"),
        Map.entry("condicao", "condição"),
        Map.entry("condicoes", "condições"),
        Map.entry("destruicao", "destruição"),
        Map.entry("protecao", "proteção"),
        Map.entry("confirmacao", "confirmação"),
        Map.entry("autorizacao", "autorização"),

        // ---------------------------------------------------------------
        // AMPLIAÇÃO DE 12/08/2026 — MEDIDA no Gundam Unicorn, 22 episódios.
        //
        // MedicaoAcentuacaoFaltanteIT comparou as três traduções da MESMA obra e achou o
        // primeiro eixo em que os modelos divergem para o lado do mistral:
        //
        //   mistral   23 de 5.676 falas (0,4%)
        //   aya      197 de 5.458 falas (3,6%)   <-- 9x mais
        //
        // O dicionário acima é todo da família -ão/-ção e não cobria NADA do que a aya erra.
        // As mais frequentes, contadas: familia 22 · proxima 21 · unica 11 · federacao 10 ·
        // ultima 9 · maldicao 8 · fundacao 5 · unico 5 · facil 4 · historia 4 · possivel 4.
        //
        // Por que isto importa mais que parecer detalhe: erro de acento é corrigível por
        // MÁQUINA e erro de sentido não é. Com esta rede, o eixo em que a aya perdia deixa de
        // existir, e o eixo em que ela ganha — inverter pergunta e negação, 10x menos que o
        // mistral — continua valendo. É o que torna a escolha do titular uma decisão só.
        //
        // Mesmo critério de sempre: entra apenas forma cuja grafia sem acento NÃO é palavra
        // portuguesa. Ficaram DE FORA, mesmo tendo aparecido, todas as que também são verbo:
        // "publica" (ele publica), "medica" (ele medica), "critica", "pratica", "duvida",
        // "fabrica", "continua". Trocar essas introduziria erro onde não havia.
        // ---------------------------------------------------------------
        Map.entry("familia", "família"),
        Map.entry("familias", "famílias"),
        Map.entry("proxima", "próxima"),
        Map.entry("proximo", "próximo"),
        Map.entry("proximas", "próximas"),
        Map.entry("proximos", "próximos"),
        Map.entry("unica", "única"),
        Map.entry("unico", "único"),
        Map.entry("unicas", "únicas"),
        Map.entry("unicos", "únicos"),
        Map.entry("ultima", "última"),
        Map.entry("ultimo", "último"),
        Map.entry("ultimas", "últimas"),
        Map.entry("ultimos", "últimos"),
        Map.entry("historia", "história"),
        Map.entry("historias", "histórias"),
        Map.entry("memoria", "memória"),
        Map.entry("memorias", "memórias"),
        Map.entry("vitoria", "vitória"),
        Map.entry("gloria", "glória"),
        Map.entry("possivel", "possível"),
        Map.entry("impossivel", "impossível"),
        Map.entry("responsavel", "responsável"),
        Map.entry("responsaveis", "responsáveis"),
        Map.entry("disponivel", "disponível"),
        Map.entry("terrivel", "terrível"),
        Map.entry("incrivel", "incrível"),
        Map.entry("horrivel", "horrível"),
        Map.entry("nivel", "nível"),
        Map.entry("niveis", "níveis"),
        Map.entry("facil", "fácil"),
        Map.entry("dificil", "difícil"),
        Map.entry("faceis", "fáceis"),
        Map.entry("dificeis", "difíceis"),
        Map.entry("rapido", "rápido"),
        Map.entry("rapida", "rápida"),
        Map.entry("proprio", "próprio"),
        Map.entry("propria", "própria"),
        Map.entry("proprios", "próprios"),
        Map.entry("proprias", "próprias"),
        Map.entry("otimo", "ótimo"),
        Map.entry("otima", "ótima"),
        Map.entry("seculo", "século"),
        Map.entry("seculos", "séculos"),
        Map.entry("exercito", "exército"),
        Map.entry("veiculo", "veículo"),
        Map.entry("veiculos", "veículos"),
        Map.entry("servico", "serviço"),
        Map.entry("servicos", "serviços"),
        Map.entry("inicio", "início"),
        Map.entry("proposito", "propósito"),
        Map.entry("logica", "lógica"),
        Map.entry("logico", "lógico"),
        Map.entry("musica", "música"),
        Map.entry("musicas", "músicas"),
        Map.entry("importancia", "importância"),
        Map.entry("circunstancia", "circunstância"),
        Map.entry("circunstancias", "circunstâncias"),
        Map.entry("experiencia", "experiência"),
        Map.entry("experiencias", "experiências"),
        Map.entry("ciencia", "ciência"),
        Map.entry("consciencia", "consciência"),
        Map.entry("distancia", "distância"),
        Map.entry("emergencia", "emergência"),
        Map.entry("federacao", "federação"),
        Map.entry("federacoes", "federações"),
        Map.entry("maldicao", "maldição"),
        Map.entry("fundacao", "fundação"),
        Map.entry("ativacao", "ativação"),
        Map.entry("investigacao", "investigação"),
        Map.entry("localizacao", "localização"),
        Map.entry("geracao", "geração"),
        Map.entry("geracoes", "gerações"),
        Map.entry("populacao", "população"),
        Map.entry("estacao", "estação"),
        Map.entry("ligacao", "ligação"),
        Map.entry("traicao", "traição"),
        Map.entry("solucao", "solução"),
        Map.entry("evacuacao", "evacuação"),
        Map.entry("formacao", "formação"),
        Map.entry("comunicacao", "comunicação"),
        Map.entry("transmissao", "transmissão"),
        Map.entry("expressao", "expressão"),
        Map.entry("compreensao", "compreensão"),
        Map.entry("intencao", "intenção"),
        Map.entry("excecao", "exceção"),
        Map.entry("opcao", "opção"),
        Map.entry("opcoes", "opções"),
        Map.entry("acao", "ação"),
        Map.entry("acoes", "ações"),
        Map.entry("nacoes", "nações"),
        Map.entry("ilusao", "ilusão"),
        Map.entry("confusao", "confusão"),
        Map.entry("conclusao", "conclusão"),
        Map.entry("ocasiao", "ocasião"),
        Map.entry("regiao", "região"),
        Map.entry("regioes", "regiões"),
        Map.entry("rebeliao", "rebelião"),
        Map.entry("questao", "questão"),
        Map.entry("questoes", "questões"),
        Map.entry("sugestao", "sugestão"),
        Map.entry("dimensao", "dimensão"),
        Map.entry("dimensoes", "dimensões")
    );

    private static final Pattern PALAVRA;
    static {
        // Alternância ordenada da mais longa para a mais curta (voces antes de voce).
        String alternancia = CORRECOES.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .reduce((a, b) -> a + "|" + b).orElse("");
        PALAVRA = Pattern.compile(
            INICIO_DE_TERMO + "(" + alternancia + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o texto com os acentos repostos nas formas do dicionário.
     * <p>INVARIANTES DO DOMÍNIO: só troca formas do dicionário curado, por fronteira de palavra,
     * preservando a caixa; nada mais é tocado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null}/vazio devolve a si mesmo.
     */
    public String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        Matcher m = PALAVRA.matcher(texto);
        StringBuilder sb = new StringBuilder(texto.length());
        while (m.find()) {
            String achado = m.group(1);
            String base = CORRECOES.get(achado.toLowerCase(Locale.ROOT));
            m.appendReplacement(sb, Matcher.quoteReplacement(aplicarCaixa(achado, base)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String aplicarCaixa(String achado, String base) {
        if (achado.length() > 1 && achado.chars().allMatch(Character::isUpperCase)) {
            return base.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(achado.charAt(0))) {
            return Character.toUpperCase(base.charAt(0)) + base.substring(1);
        }
        return base;
    }
}
