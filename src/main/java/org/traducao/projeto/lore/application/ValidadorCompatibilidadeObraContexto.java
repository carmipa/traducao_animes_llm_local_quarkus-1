package org.traducao.projeto.lore.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.lore.domain.IdentidadeObra;
import org.traducao.projeto.lore.domain.VeredictoObraContexto;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: julga se o arquivo prestes a ser traduzido pertence à obra cuja
 * lore foi selecionada, e redige a mensagem que o operador vê quando não pertence. É a
 * blindagem que faltava no incidente medido nesta árvore: 15 caches de Gundam 0083
 * gravados com {@code contextoId = "guilty_crown"} e ~4.442 entradas traduzidas sob a lore
 * errada — nenhuma guarda comparava a obra do ARQUIVO com o contexto ATIVO, e a
 * proveniência carimbava fielmente a lore errada, deixando o cache coerentemente errado.
 *
 * <p>Mora no peer {@code contexto} porque a pergunta que responde — "este arquivo é da obra
 * cujo contexto está selecionado?" — é sobre IDENTIDADE DE OBRA, e identidade de obra é
 * propriedade deste peer. O peer {@code qualidadeTraducao} é dono da validação do TEXTO
 * produzido; a fatia {@code traducao} apenas consome este veredicto e o traduz em efeito.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Decisão puramente determinística sobre os dados recebidos: mesmos argumentos,
 *       mesmo veredicto. Sem I/O, sem estado, sem relógio, sem aleatoriedade.</li>
 *   <li>{@link VeredictoObraContexto#DIVERGENTE} só sai com PROVA POSITIVA — a pasta resolveu
 *       para UMA obra do catálogo e o contexto ativo não é ela. Ausência de reconhecimento
 *       nunca vira divergência.</li>
 *   <li>{@link VeredictoObraContexto#AMBIGUO} sai quando a pasta é reivindicada por MAIS DE
 *       UMA obra com a mesma especificidade. Bloqueia mesmo que o contexto ativo esteja entre
 *       elas: se a identidade não resolve para uma obra única, deixar passar seria aceitar a
 *       seleção da UI como prova — e é justamente isso que produziu o incidente.</li>
 *   <li>Recebe o resultado do reconhecimento como DADO ({@code idsQueReconhecem}), sem
 *       consultar o {@code GerenciadorContexto}: assim {@code contexto.application} não
 *       depende de {@code contexto.infrastructure} e o julgamento permanece testável sem
 *       container. O conjunto recebido JÁ vem resolvido por especificidade pelo catálogo —
 *       aqui, mais de um id significa empate real, não sobreposição entre uma entrada de
 *       franquia e sua temporada.</li>
 *   <li>Este serviço APENAS julga e redige; quem decide bloquear ou seguir é a fatia que o
 *       consome ({@code traducao.application.GuardaContextoObraTraducao}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nenhum método lança, nem para argumento nulo: obra em branco, conjunto nulo/vazio de
 * reconhecedores ou contexto ativo ausente resultam em
 * {@link VeredictoObraContexto#INDETERMINADO} — o desfecho seguro, que deixa a execução
 * seguir com aviso. Falhar fechado aqui pararia traduções legítimas de toda obra que ainda
 * não declarou vocabulário de pasta.
 */
@Component
public class ValidadorCompatibilidadeObraContexto {

    /**
     * Rótulos ESTRUTURAIS de pasta: nomes que descrevem a posição do arquivo na organização
     * do acervo, e não a obra. Casam o nome INTEIRO já normalizado — "guilty crown ova" não
     * casa, "ova" casa. O número é opcional porque tanto {@code "Season 5"} quanto
     * {@code "Movie"} são igualmente incapazes de identificar obra.
     */
    private static final Pattern NOME_ESTRUTURAL = Pattern.compile(
        "(?:season|temporada|temp|s|movie|movies|filme|filmes|ova|ovas|oad|oads|special|specials"
        + "|especial|especiais|extra|extras|bonus|part|parte|disc|disco|cd|vol|volume)\\s*\\d*");

    /**
     * PROPÓSITO DE NEGÓCIO: compara a obra reconhecida no caminho do arquivo com o contexto
     * selecionado para a execução e devolve o desfecho.
     *
     * <p>INVARIANTES DO DOMÍNIO: mais de um id que reconhece é AMBÍGUO (a identidade não
     * resolve); com um id único, casa quando ele é o ativo e diverge quando não é; é
     * indeterminado em todos os demais casos (ninguém reconheceu, obra sem nome, execução sem
     * contexto). A checagem de ambiguidade vem DEPOIS das checagens de ausência: sem obra e
     * sem contexto ativo não há tradução a proteger, e reprovar aí só trocaria um aviso útil
     * por um bloqueio inútil.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer argumento nulo/em branco degrada para
     * {@link VeredictoObraContexto#INDETERMINADO} em vez de lançar.
     *
     * @param obraDoArquivo nome da pasta da obra derivado do caminho do arquivo
     * @param contextoAtivoId id do contexto/lore selecionado para esta execução
     * @param idsQueReconhecem ids dos contextos que reconhecem {@code obraDoArquivo}, já
     *        resolvidos por especificidade pelo catálogo
     * @return o desfecho da comparação; nunca {@code null}
     */
    public VeredictoObraContexto avaliar(String obraDoArquivo, String contextoAtivoId, Set<String> idsQueReconhecem) {
        if (obraDoArquivo == null || obraDoArquivo.isBlank()) {
            return VeredictoObraContexto.INDETERMINADO;
        }
        if (idsQueReconhecem == null || idsQueReconhecem.isEmpty()) {
            return VeredictoObraContexto.INDETERMINADO;
        }
        if (contextoAtivoId == null || contextoAtivoId.isBlank()) {
            return VeredictoObraContexto.INDETERMINADO;
        }
        if (idsQueReconhecem.size() > 1) {
            return VeredictoObraContexto.AMBIGUO;
        }
        return idsQueReconhecem.contains(contextoAtivoId)
            ? VeredictoObraContexto.CASA
            : VeredictoObraContexto.DIVERGENTE;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: redige a mensagem de bloqueio com os três fatos que o operador
     * precisa para se corrigir em um clique — qual arquivo, qual obra o caminho declara e
     * qual contexto deveria ter sido escolhido em vez do que está ativo.
     *
     * <p>INVARIANTES DO DOMÍNIO: cita o id ATIVO e os ids RECONHECIDOS explicitamente; não
     * sugere "corrigir automaticamente" nem troca o contexto por conta própria — a decisão
     * é do operador.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita nulos e os imprime como estão; a mensagem é
     * diagnóstica e nunca deve, ela própria, quebrar o fluxo de erro.
     *
     * @param caminhoArquivo caminho do arquivo bloqueado
     * @param obraDoArquivo obra reconhecida no caminho
     * @param contextoAtivoId id do contexto ativo que não bate com a obra
     * @param idsQueReconhecem ids dos contextos que reconhecem a obra
     * @return mensagem de bloqueio pronta para log e para a saída dinâmica
     */
    public String mensagemDeBloqueio(String caminhoArquivo, String obraDoArquivo,
                                     String contextoAtivoId, Set<String> idsQueReconhecem) {
        return "Obra do arquivo NÃO confere com o contexto selecionado — execução BLOQUEADA para "
            + "não gravar cache com a lore errada. Arquivo: " + caminhoArquivo
            + " | obra reconhecida no caminho: \"" + obraDoArquivo + "\""
            + " | contexto esperado: " + idsQueReconhecem
            + " | contexto ATIVO: \"" + contextoAtivoId + "\"."
            + " Selecione o contexto correto na UI e execute de novo.";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: redige o bloqueio quando o contexto veio do CARIMBO gravado no
     * arquivo, e não da seleção do operador. É outra mensagem porque é outro conserto.
     *
     * <p>{@link #mensagemDeBloqueio} manda "selecione o contexto correto na UI e execute de novo",
     * o que é correto para a tradução — lá o contexto vem mesmo da tela. Para um cache VERSIONADO
     * é uma instrução impossível: o carimbo tem prioridade sobre a seleção por construção, então
     * trocar a obra na UI produz a mensagem byte a byte idêntica e o operador fica preso num laço
     * sem saída. Ela também chamava o carimbo de "contexto ATIVO", o que é factualmente falso no
     * instante do bloqueio: a lore reprovada nunca chega a ser ativada.
     *
     * <p>INVARIANTES DO DOMÍNIO: nomeia o valor pelo que ele é — o carimbo da proveniência — e
     * instrui os dois consertos que de fato funcionam: corrigir o {@code contextoId} do cache, ou
     * mover o arquivo para a pasta da obra certa. Não sugere trocar a seleção, e não corrige nada
     * por conta própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita nulos e os imprime como estão.
     *
     * @param caminhoArquivo caminho do arquivo bloqueado
     * @param obraDoArquivo obra reconhecida no caminho
     * @param contextoCarimbado id gravado na proveniência do cache
     * @param idsQueReconhecem ids dos contextos que reconhecem a obra
     * @return mensagem de bloqueio pronta para log e para a saída dinâmica
     */
    public String mensagemDeBloqueioCarimbo(String caminhoArquivo, String obraDoArquivo,
                                            String contextoCarimbado, Set<String> idsQueReconhecem) {
        return "Obra do arquivo NÃO confere com a proveniência gravada no cache — operação "
            + "BLOQUEADA para não reinterpretar o cache com a lore errada. Arquivo: " + caminhoArquivo
            + " | obra reconhecida no caminho: \"" + obraDoArquivo + "\""
            + " | contexto esperado: " + idsQueReconhecem
            + " | contexto CARIMBADO na proveniência: \"" + contextoCarimbado + "\"."
            + " A seleção da UI é IGNORADA para cache versionado: corrija o \"contextoId\" da"
            + " proveniência deste arquivo, ou mova-o para a pasta da obra correta.";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: redige a mensagem do bloqueio por AMBIGUIDADE — a pasta é
     * reivindicada por mais de uma obra com a mesma precisão, então o catálogo não consegue
     * nomear uma única obra esperada. A mensagem é endereçada a quem mantém as lores: o
     * conserto é dar a uma das obras um nome canônico mais específico (apelido de pasta), não
     * escolher uma delas no combo e torcer.
     *
     * <p>INVARIANTES DO DOMÍNIO: lista os ids empatados e diz explicitamente que o problema é
     * de IDENTIDADE, não de seleção; não sugere desempate automático.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita nulos e os imprime como estão.
     *
     * @param caminhoArquivo caminho do arquivo bloqueado
     * @param obraDoArquivo obra reconhecida no caminho
     * @param contextoAtivoId id do contexto ativo da execução
     * @param idsQueReconhecem ids empatados que reivindicam a pasta
     * @return mensagem de bloqueio pronta para log e para a saída dinâmica
     */
    public String mensagemDeAmbiguidade(String caminhoArquivo, String obraDoArquivo,
                                        String contextoAtivoId, Set<String> idsQueReconhecem) {
        return "Identidade da obra AMBÍGUA — execução BLOQUEADA porque o catálogo não consegue "
            + "provar de qual obra é este arquivo. Arquivo: " + caminhoArquivo
            + " | obra reconhecida no caminho: \"" + obraDoArquivo + "\""
            + " | reivindicada com a MESMA especificidade por: " + idsQueReconhecem
            + " | contexto ATIVO: \"" + contextoAtivoId + "\"."
            + " Isto é defeito de IDENTIDADE, não de seleção: declare um apelido de pasta mais "
            + "específico em uma dessas lores para desempatar.";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: redige o aviso do caso indeterminado — a obra do caminho não é
     * reconhecida por nenhum contexto, então a guarda não tem base para julgar e a tradução
     * segue. Torna VISÍVEL que o arquivo passou sem verificação, em vez de dar a falsa
     * impressão de que foi conferido.
     *
     * <p>INVARIANTES DO DOMÍNIO: é AVISO, não bloqueio; deixa explícito que a checagem foi
     * pulada e que o operador é quem responde pela seleção do combo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita nulos e os imprime como estão.
     *
     * @param obraDoArquivo obra derivada do caminho, não reconhecida por nenhum contexto
     * @param contextoAtivoId id do contexto ativo da execução
     * @return mensagem de aviso pronta para log e para a saída dinâmica
     */
    public String mensagemDeIndeterminacao(String obraDoArquivo, String contextoAtivoId) {
        return "Obra \"" + obraDoArquivo + "\" não é reconhecida por nenhum contexto registrado: "
            + "a checagem obra×contexto foi PULADA e a tradução segue com o contexto ativo \""
            + contextoAtivoId + "\". Confirme que é a lore certa — a guarda não adivinha a obra "
            + "a partir de um caminho desconhecido.";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece que o nome da pasta é GENÉRICO — {@code "Season 05"},
     * {@code "Movie"}, {@code "OVA"} —, isto é, um rótulo que QUALQUER obra do acervo pode ter
     * e que portanto não identifica obra nenhuma. Existe para separar dois casos que hoje
     * produzem o mesmo aviso e pedem consertos OPOSTOS: "obra ainda sem lore cadastrada"
     * (cadastre a lore) e "a pasta não diz que obra é esta" (renomeie a pasta).
     *
     * <h2>O prejuízo que originou</h2>
     * Medido em 07/08/2026: metade das pastas de legenda do acervo tinha um nível de temporada
     * entre a obra e o arquivo, e a obra derivada do caminho saía {@code "Season 05"}. Disso
     * dependem DUAS coisas — o portão obra×contexto, que se declarava cego e seguia, e o
     * DIRETÓRIO DE CACHE, que virava {@code cache/Season 5/}, o mesmo já ocupado por 22
     * arquivos do Gundam Unicorn. Duas obras gravando no mesmo cache é corrupção silenciosa,
     * e o aviso genérico não dizia nada sobre isso.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Casa o nome INTEIRO normalizado, nunca um pedaço: {@code "Guilty Crown OVA"} e
     *       {@code "DanMachi Season 05"} são nomes legítimos de obra e NÃO podem casar aqui.
     *       A diferença é entre a pasta <em>se chamar</em> {@code OVA} e a pasta <em>conter</em>
     *       a palavra.</li>
     *   <li>Usa {@link IdentidadeObra#normalizar(String)} — a MESMA normalização do
     *       reconhecimento. Se as duas divergissem, uma pasta poderia ser genérica para este
     *       método e específica para o catálogo, ou o contrário.</li>
     *   <li>NÃO altera o veredicto: pasta genérica continua
     *       {@link VeredictoObraContexto#INDETERMINADO} e a tradução SEGUE. Só a mensagem muda.
     *       Bloquear aqui pararia a tradução de quem organiza o acervo assim, e a guarda existe
     *       para impedir lore errada, não para impor convenção de pastas.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Nulo ou em branco devolve {@code false}: sem nome não há como afirmar que é genérico, e
     * o aviso comum de indeterminação já cobre o caso. Nunca lança.
     *
     * @param obraDoArquivo nome da pasta derivado do caminho do arquivo
     * @return {@code true} quando o nome inteiro é rótulo estrutural sem identidade de obra
     */
    public boolean pastaGenerica(String obraDoArquivo) {
        if (obraDoArquivo == null || obraDoArquivo.isBlank()) {
            return false;
        }
        return NOME_ESTRUTURAL.matcher(IdentidadeObra.normalizar(obraDoArquivo)).matches();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: redige o aviso da pasta genérica. É outra mensagem porque é OUTRO
     * CONSERTO: {@link #mensagemDeIndeterminacao} manda confirmar a lore, o que para
     * {@code "Season 05"} é instrução impossível — não existe e nunca vai existir uma lore
     * chamada "Season 05". O conserto é renomear a pasta.
     *
     * <p>INVARIANTES DO DOMÍNIO: nomeia o diretório de cache que a pasta genérica produz,
     * porque a colisão de cache é o dano REAL e silencioso — o aviso de lore o operador lê no
     * console, o cache compartilhado entre duas obras ele não vê até o resultado sair errado.
     * Traz um exemplo concreto de renomeação, porque aviso sem conserto vira ruído ignorado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceita nulos e os imprime como estão; a mensagem é
     * diagnóstica e nunca deve, ela própria, quebrar o fluxo.
     *
     * @param obraDoArquivo nome genérico derivado do caminho
     * @param contextoAtivoId id do contexto ativo da execução
     * @return mensagem de aviso pronta para log e para a saída dinâmica
     */
    public String mensagemDePastaGenerica(String obraDoArquivo, String contextoAtivoId) {
        return "Pasta \"" + obraDoArquivo + "\" tem nome GENÉRICO e não identifica obra alguma —"
            + " qualquer anime do acervo pode ter uma pasta com esse nome. A checagem obra×contexto"
            + " foi PULADA e a tradução segue com o contexto ativo \"" + contextoAtivoId + "\"."
            + " ATENÇÃO: o cache desta execução vai para \"cache/" + obraDoArquivo + "/\", que"
            + " qualquer outra obra com a mesma estrutura de pastas também usaria. Renomeie a pasta"
            + " incluindo o nome da obra (ex.: \"DanMachi Season 05\" em vez de \"Season 05\").";
    }
}
