package org.traducao.projeto.contexto.lore.semlore;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: traduzir uma obra que AINDA NÃO TEM LORE declarada — o anime acabou de
 * ser baixado e a intenção é ver como fica, testar o pipeline ou resolver na pressa. Sem este
 * contexto, a única saída seria escolher a lore de OUTRA obra, que é exatamente o defeito que a
 * {@code GuardaContextoObraTraducao} existe para impedir.
 *
 * <h2>O que este contexto NÃO tem, e por quê</h2>
 * Termos protegidos, mapa de terminologia e pares inconfundíveis ficam VAZIOS — herdados dos
 * defaults da interface. Não é omissão: declarar qualquer termo aqui seria inventar vocabulário
 * de uma obra que ninguém estudou. As consequências são reais e o operador precisa conhecê-las:
 * <ul>
 *   <li>Nome próprio PODE ser traduzido pelo modelo. Sem lista de termos protegidos, nada além
 *       da instrução em prosa impede {@code Sky} virar {@code Céu}.</li>
 *   <li>{@code EnforcadorTermosLore} não tem o que reforçar.</li>
 *   <li>{@code RestauradorFalaIdenticaSemItalico} fica inerte — ele exige que a fala seja feita
 *       de termos protegidos, e não há nenhum.</li>
 * </ul>
 * Continuam ATIVAS todas as blindagens que não dependem de lore: estrutura de tags, detecção de
 * alucinação e meta-resposta, identificador numérico, resíduo em inglês e eco.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #reconhecePasta} é SEMPRE {@code false}. Este contexto não reivindica obra
 *       nenhuma, e é isso que faz a guarda se comportar certo sozinha: pasta desconhecida vira
 *       {@code INDETERMINADO} (avisa e segue), e pasta de obra COM lore vira {@code DIVERGENTE}
 *       (bloqueia, dizendo qual contexto usar).</li>
 *   <li>{@link #apareceNaListaDeObras} é {@code false}: fica fora dos combos das outras telas.</li>
 *   <li>O id {@code sem_lore} carimba a proveniência do cache, então uma tradução crua NUNCA é
 *       reaproveitada depois que a lore de verdade for escrita — a proveniência diverge e o
 *       pipeline retraduz.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem I/O e sem estado; nenhum método lança.
 */
@Component
public class ContextoSemLore implements ProvedorContexto {

    /** Id reservado. Também é o valor que libera a trava de lore na UI. */
    public static final String ID = "sem_lore";

    private static final String LORE = """
        - NAO ha lore declarada para esta obra. Voce nao conhece o universo, os personagens
          nem a terminologia dela, e NAO deve fingir que conhece.
        - Trate TODA palavra com inicial maiuscula no meio da frase como nome proprio e
          MANTENHA como esta: nomes de pessoa, mecha, nave, faccao, cidade, organizacao,
          tecnica e codinome. Na duvida entre traduzir e manter, MANTENHA.
        - NAO deduza significado de nome proprio pela raiz da palavra. Um personagem chamado
          "Sky" continua "Sky", nunca "Ceu"; "Blackwood" continua "Blackwood".
        - NAO padronize grafia, NAO corrija o que parecer erro de romanizacao e NAO troque um
          nome por outro parecido: sem lore, voce nao tem como saber qual e o certo.
        - Traduza normalmente o restante da fala: dialogo, narracao e descricao.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "obra sem lore declarada (traducao crua)", LORE);

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getNomeExibicao() {
        return "Sem lore — tradução crua";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: este contexto não reivindica NENHUMA pasta.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code false} incondicional — não delega à
     * {@code IdentidadeObra}, que casaria pelo nome de exibição e faria "sem lore" reivindicar
     * qualquer pasta que contivesse essas palavras.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; ignora o argumento.
     */
    @Override
    public boolean reconhecePasta(String nomeDaPasta) {
        return false;
    }

    /** Fora dos combos de obra: tem tela própria. */
    @Override
    public boolean apareceNaListaDeObras() {
        return false;
    }
}
