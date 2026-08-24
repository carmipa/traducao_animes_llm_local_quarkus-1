package org.traducao.projeto.revisaoConcordancia.domain;

/**
 * PROPÓSITO DE NEGÓCIO: o placar de UM corretor numa passada — quantas falas ele mudou, quantas
 * viu e deixou como estavam, quantas explodiram na cara dele, e se ele sequer pôde rodar.
 *
 * <h2>Por que existe, e por que tem quatro números e não um</h2>
 * A tela 3.3 passou a ter quatro corretores em cadeia. Um total único diria <i>"78 falas
 * corrigidas"</i> sem dizer de onde veio — e a próxima medição teria de adivinhar se o ganho foi
 * da lista de gênero, do POS tagger, dos padrões curados ou do dicionário.
 *
 * <p>A ordem permanente deste projeto é <b>contador do que AGIU e contador do que se ABSTEVE</b>.
 * Sem o segundo, "este corretor não achou nada" e "este corretor nem rodou" saem iguais — e essa
 * confusão já custou caro aqui mais de uma vez.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code agiu + absteve + falhou} é o total de falas que passaram por este corretor.</li>
 *   <li><b>{@code disponivel} não é enfeite:</b> {@code agiu=0} com {@code disponivel=false} é
 *       <i>NÃO VERIFIQUEI</i>, e nunca <i>está limpo</i>. É a invariante 12 do projeto — saída
 *       vazia ambígua é bug — aplicada ao placar.</li>
 *   <li>{@code falhou} conta a fala que lançou exceção. Ela NÃO some: a passada continua, e o
 *       número aparece no relatório. Erro engolido em silêncio é o que esta contagem impede.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida e não lança.
 *
 * @param nome        como o corretor aparece no relatório e na telemetria
 * @param agiu        falas que ele mudou
 * @param absteve     falas que ele viu e deixou intactas
 * @param falhou      falas em que ele lançou exceção — contadas, nunca escondidas
 * @param disponivel  se ele pôde rodar; {@code false} torna qualquer zero acima NÃO VERIFICADO
 */
public record ContagemCorretor(
    String nome,
    int agiu,
    int absteve,
    int falhou,
    boolean disponivel
) {

    /** Total de falas que passaram por este corretor. */
    public int vistas() {
        return agiu + absteve + falhou;
    }

    /**
     * A linha do relatório, já com o vocabulário certo: quando o corretor não pôde rodar, o zero
     * vira NÃO VERIFICADO em vez de se passar por limpeza.
     */
    public String linhaDeRelatorio() {
        if (!disponivel) {
            return String.format("%-26s NAO VERIFICADO (nao pode rodar)", nome);
        }
        String base = String.format("%-26s %5d agiu · %6d intactas", nome, agiu, absteve);
        return falhou == 0 ? base : base + String.format(" · %d FALHARAM", falhou);
    }
}
