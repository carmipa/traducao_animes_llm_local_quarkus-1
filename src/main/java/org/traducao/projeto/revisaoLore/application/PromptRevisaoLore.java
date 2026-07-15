package org.traducao.projeto.revisaoLore.application;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: monta os prompts de revisão terminológica e mantém a
 * lore da obra separável das instruções operacionais.
 * <p>INVARIANTES DO DOMÍNIO: a fonte canônica recebida integra o prompt sem
 * alteração e pode ser recuperada pelos delimitadores estáveis da classe.
 * <p>COMPORTAMENTO EM CASO DE FALHA: lore ausente usa marcador explícito e a
 * extração de prompt inválido devolve texto vazio.
 */
public final class PromptRevisaoLore {

    private static final String INICIO_LORE = "Use a lore abaixo como fonte canonica de grafia e padrao:\n";
    private static final String FIM_LORE = "\n\nRegras:";

    private PromptRevisaoLore() {
    }

    public static String montarPromptSistema(String loreObra) {
        String lore = loreObra != null && !loreObra.isBlank() ? loreObra.strip() : "(sem lore adicional)";
        return """
            Voce e revisor especializado em legendas de anime/filme, focado em TERMINOLOGIA E LORE.
            Corrija APENAS nomes proprios, locais, organizacoes, mechas, titulos, apelidos e termos de mundo
            que estejam fora do padrao oficial da obra. NAO reescreva a fala inteira nem mude concordancia de genero
            a menos que um nome proprio exija artigo/pronome coerente.

            Use a lore abaixo como fonte canonica de grafia e padrao:
            %s

            Regras:
            - Preserve marcadores [[TAGn]] literalmente (nao traduza nem remova).
            - Trate nomes canonicos como texto protegido: personagens, sobrenomes, apelidos, lugares,
              naves, mechas, armas, operacoes e titulos de obra NAO devem ser traduzidos.
            - Faccoes e organizacoes consagradas que possuem traducao padrao estabelecida para o portugues devem ser traduzidas
              quando o texto em portugues ja estiver nessa convencao. Aceite variantes naturais e corretas em PT-BR:
              "Federation" pode ser "Federacao"; "Earth Federation" pode ser "Federacao Terrestre" ou
              "Federacao da Terra"; "Principality of Zeon" pode ser "Principado de Zeon".
              NAO force uma unica variante se a traducao atual ja estiver natural, consistente e correta.
              Nomes de faccoes especificas sem traducao consagrada (ex.: "08th MS Team", "Londo Bell") devem ser mantidos no original.
            - Quando uma palavra comum fizer parte de um nome oficial protegido, mantenha a palavra no idioma original
              (ex.: Narrative Gundam, Unicorn Gundam, Freedom Gundam, War in the Pocket, The 08th MS Team).
            - Mantenha termos tecnicos da obra em ingles quando a lore assim indicar (mobile suit, Newtype, Handler, etc.).
            - Corrija transliteracoes erradas, nomes anglicizados indevidos, traducao literal de nomes oficiais e localizacoes fora do padrao.
            - Se apenas uma parte do nome foi traduzida, restaure o nome oficial completo conforme a lore.
            - NAO altere verbos, adjetivos, metaforas ou expressoes comuns de dialogo que ja estejam bem traduzidas para o portugues.
              NUNCA introduza termos em ingles de forma desnecessaria para palavras comuns (ex.: NUNCA mude "garotos" para "kids" ou "curar feridas" para "bandagem feridas").
            - NUNCA adicione sobrenomes ou nomes completos se o original em ingles usa apenas o primeiro nome ou apelido
              (ex.: se o original diz apenas "Shiro and Aina", mantenha "Shiro e Aina", NUNCA force "Shiro Amada e Aina Sahalin").
            - Nao use o original em ingles para retraduzir, melhorar estilo, trocar sinonimos ou ajustar fluidez geral.
              Use o original apenas para identificar nomes/termos de lore que estejam factualmente incorretos.
            - NUNCA crie erros gramaticais ou de concordancia em portugues.
            - Se a traducao ja estiver correta segundo a lore ou for um dialogo comum sem termos especificos de lore, devolva-a exatamente como foi fornecida.
            - Nao adicione explicacoes, aspas ou comentarios.

            Responda APENAS com uma unica linha: a fala revisada em portugues do Brasil.
            """.formatted(lore);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recupera somente a fonte canônica anexada ao prompt
     * para que as travas não confundam instruções genéricas com termos da obra.
     * <p>INVARIANTES DO DOMÍNIO: usa os delimitadores produzidos por
     * {@link #montarPromptSistema(String)} e não inclui as regras operacionais.
     * <p>COMPORTAMENTO EM CASO DE FALHA: prompt ausente ou fora do formato
     * conhecido devolve texto vazio, fazendo a validação posterior bloquear.
     */
    public static String extrairLoreCanonica(String promptSistema) {
        if (promptSistema == null || promptSistema.isBlank()) {
            return "";
        }
        int inicio = promptSistema.indexOf(INICIO_LORE);
        if (inicio < 0) {
            return "";
        }
        inicio += INICIO_LORE.length();
        int fim = promptSistema.indexOf(FIM_LORE, inicio);
        return fim >= inicio ? promptSistema.substring(inicio, fim).strip() : "";
    }

    public static String montarPromptUsuario(
        String originalIngles,
        String traducaoPt,
        List<String> problemasDetectados
    ) {
        String listaProblemas = problemasDetectados == null || problemasDetectados.isEmpty()
            ? "(revisao preventiva de nomes/locais/termos)"
            : String.join("\n- ", problemasDetectados);

        return """
            Audite SOMENTE a terminologia de lore da fala em portugues.
            Use o original em ingles apenas como referencia para localizar nomes de personagens, lugares,
            faccoes, mechas, patentes, armas e termos protegidos.
            Nao retraduza a fala, nao melhore estilo, nao troque sinonimos e nao corrija expressoes comuns.
            Se a traducao atual ja estiver aceitavel em PT-BR, devolva exatamente a mesma linha.

            Original (ingles):
            %s

            Traducao atual (portugues):
            %s

            Indicios de problema (heuristica automatica):
            - %s

            Responda com uma unica linha: a traducao revisada conforme a lore oficial.
            """.formatted(originalIngles, traducaoPt, listaProblemas);
    }
}
