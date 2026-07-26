package org.traducao.projeto.contexto.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ContextoNaoEncontradoException;
import org.traducao.projeto.contexto.domain.IdentidadeObra;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.contexto.domain.SnapshotContexto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PROPÓSITO DE NEGÓCIO: agrega todos os provedores de contexto/lore descobertos por
 * CDI e mantém qual está ATIVO, servindo o prompt de sistema, a lore crua, o id de
 * proveniência e os termos protegidos para a tradução em curso. É o ponto único pelo
 * qual as fatias funcionais (tradução, correção, revisão, karaokê) selecionam e
 * consultam a obra ativa — agora residente no módulo compartilhado {@code contexto}
 * (peer), consumível por qualquer fatia sem acoplamento reverso.
 *
 * <p>INVARIANTES DO DOMÍNIO: os provedores são ordenados por nome de exibição
 * (case-insensitive) e seus ids são únicos (falha na construção se houver duplicata); a
 * IDENTIDADE CANÔNICA de cada obra é derivada UMA vez na construção e nenhum nome canônico
 * pode pertencer a duas obras (colisão também falha na construção — ver
 * {@link #validarIdentidadesSemColisao(List)});
 * o contexto padrão é {@code danmachi} (ou o primeiro, se ausente); {@code provedorAtivo}
 * nunca cai silenciosamente no padrão quando um id explícito não existe. O campo
 * {@code provedorAtivo} é {@code volatile} para visibilidade entre a thread do executor
 * de background e a leitura ao montar o prompt — não é uma alegação de isolamento por job.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #definirContextoAtivo(String)} lança
 * {@link ContextoNaoEncontradoException} para um id não vazio desconhecido (impede
 * traduzir com a lore errada silenciosamente); ids nulos/vazios mantêm o ativo atual;
 * ids duplicados no registro lançam {@link IllegalStateException} na construção;
 * {@link #obterPromptAtivo()} devolve um prompt genérico quando não há ativo.
 */
@Component
public class GerenciadorContexto {

    private static final String ID_CONTEXTO_PADRAO = "danmachi";

    private final List<ProvedorContexto> provedores;
    private final List<IdentidadeObra> identidades;
    private final ProvedorContexto provedorPadrao;

    private volatile ProvedorContexto provedorAtivo;

    public GerenciadorContexto(List<ProvedorContexto> provedores) {
        this.provedores = provedores.stream()
                .sorted(Comparator.comparing(ProvedorContexto::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .toList();
        validarIdsUnicos(this.provedores);
        this.identidades = this.provedores.stream().map(IdentidadeObra::de).toList();
        validarIdentidadesSemColisao(this.identidades);
        this.provedorPadrao = encontrarProvedorPadrao();
        this.provedorAtivo = provedorPadrao;
    }

    public List<ProvedorContexto> getProvedores() {
        return provedores;
    }

    /**
     * Id do contexto usado quando nenhuma seleção explícita é feita (ex.: primeira
     * carga da UI). Usado pelo frontend para pré-selecionar a opção correta no
     * combo box, em vez de depender da ordem alfabética da lista.
     */
    public String getIdContextoPadrao() {
        return provedorPadrao != null ? provedorPadrao.getId() : null;
    }

    public boolean existeContexto(String id) {
        return id != null && provedores.stream().anyMatch(p -> p.getId().equals(id));
    }

    /**
     * Define o contexto ativo a partir do id selecionado na UI antes de cada
     * tradução. Um id não vazio que não corresponda a nenhum provedor é um erro:
     * cair silenciosamente no contexto padrão esconderia o problema e faria o
     * anime ser traduzido com a lore errada sem nenhum aviso.
     */
    public ProvedorContexto definirContextoAtivo(String id) {
        if (id == null || id.isBlank()) {
            return this.provedorAtivo;
        }
        ProvedorContexto encontrado = provedores.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ContextoNaoEncontradoException(
                        "Contexto de tradução desconhecido: \"" + id + "\". Contextos disponíveis: "
                                + idsDisponiveis()));
        this.provedorAtivo = encontrado;
        return this.provedorAtivo;
    }

    public String obterPromptAtivo() {
        if (this.provedorAtivo == null) {
            return SnapshotContexto.PROMPT_NEUTRO;
        }
        return this.provedorAtivo.obterPromptSistema();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega o contexto ativo CONGELADO em um valor imutável, para
     * que uma execução longa (traduzir um episódio inteiro) leia prompt, lore, termos
     * protegidos e id de proveniência de uma fotografia coerente, em vez de reconsultar
     * este gerenciador — que é global e pode ser reescrito por outra rota no meio do
     * caminho, produzindo um arquivo com prompt de uma obra e proveniência de outra.
     *
     * <p>INVARIANTES DO DOMÍNIO: lê {@code provedorAtivo} UMA única vez (variável local),
     * de modo que todos os campos do snapshot venham do MESMO provedor mesmo que a
     * troca aconteça durante a montagem. O snapshot devolvido não observa mudanças
     * posteriores do contexto ativo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem contexto ativo devolve
     * {@link SnapshotContexto#NEUTRO} — que reproduz exatamente o que os getters legados
     * deste gerenciador devolvem nesse estado (id nulo, nome {@code "Padrao"}, prompt
     * genérico, sem termos e sem correções) — em vez de lançar.
     *
     * @return fotografia imutável do contexto ativo; nunca {@code null}
     */
    public SnapshotContexto snapshotAtivo() {
        ProvedorContexto ativo = this.provedorAtivo;
        return ativo == null ? SnapshotContexto.NEUTRO : SnapshotContexto.de(ativo);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela o contexto PEDIDO EXPLICITAMENTE por um job, sem passar
     * pelo estado global. É esta a porta que a tradução em lote usa: o operador escolheu um
     * id na UI e é EXATAMENTE aquela obra que deve valer do primeiro ao último arquivo do
     * lote. Ler o ativo global no lugar disso reabria a janela do incidente — outra rota
     * (correção, revisão, karaokê) troca o ativo enquanto o lote roda e os arquivos seguintes
     * saem com outra lore, sem nenhum sinal.
     *
     * <p>INVARIANTES DO DOMÍNIO: resolve o provedor pelo id no registro imutável e devolve a
     * fotografia dele; NÃO lê e NÃO escreve {@code provedorAtivo}. Chamar este método não tem
     * efeito colateral algum sobre o contexto ativo — quem quiser trocar o ativo chama
     * {@link #definirContextoAtivo(String)} de propósito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: id nulo, em branco ou desconhecido lança
     * {@link ContextoNaoEncontradoException}, pela mesma razão de
     * {@link #definirContextoAtivo(String)}: cair no contexto padrão (ou no ativo anterior)
     * traduziria o lote inteiro com a lore errada em silêncio. Nunca devolve
     * {@link SnapshotContexto#NEUTRO} como consolo.
     *
     * @param id identificador do contexto escolhido para o job
     * @return fotografia imutável do contexto pedido; nunca {@code null}
     * @throws ContextoNaoEncontradoException se o id for nulo, em branco ou não registrado
     */
    public SnapshotContexto snapshotPorId(String id) {
        if (id == null || id.isBlank()) {
            throw new ContextoNaoEncontradoException(
                    "Contexto de tradução obrigatório: nenhum id informado para congelar o job. "
                            + "Contextos disponíveis: " + idsDisponiveis());
        }
        ProvedorContexto encontrado = provedores.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ContextoNaoEncontradoException(
                        "Contexto de tradução desconhecido: \"" + id + "\". Contextos disponíveis: "
                                + idsDisponiveis()));
        return SnapshotContexto.de(encontrado);
    }

    private String idsDisponiveis() {
        return provedores.stream().map(ProvedorContexto::getId).collect(Collectors.joining(", "));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve uma pasta de obra na(s) obra(s) do catálogo que ela
     * identifica. É a base factual da guarda que impede traduzir Gundam 0083 com a lore de
     * Guilty Crown por causa de um clique errado no combo: o caminho do arquivo passa a
     * ter voz, em vez de a seleção da UI ser a única palavra.
     *
     * <p>INVARIANTES DO DOMÍNIO: mede a ESPECIFICIDADE de cada identidade canônica sobre o
     * nome ({@link IdentidadeObra#especificidadeEm(String)}) e devolve APENAS os ids que
     * empatam no valor máximo. Isso é o que faz {@code "Macross 7 Encore"} resolver para
     * {@code macross_7_encore} e não empatar com {@code macross_7}: a entrada mais precisa
     * vence a mais genérica por uma regra de ordem total, não por similaridade. Um resultado
     * com MAIS DE UM id significa empate real — duas obras distintas reivindicando a pasta
     * com a mesma precisão — e é assim que a ambiguidade chega ao validador. O gerenciador
     * não desempata além disso e não adivinha: o conjunto é ordenado por id para que log e
     * mensagem de bloqueio sejam determinísticos entre execuções.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo/em branco ou nenhuma identidade casando
     * devolve conjunto VAZIO — o sinal de "não reconhecido", que os consumidores tratam como
     * aviso, nunca como divergência.
     *
     * @param nomeDaObra nome da pasta da obra, cru como veio do sistema de arquivos
     * @return ids das obras mais específicas que reconhecem a pasta, ordenados; possivelmente vazio
     */
    public Set<String> idsQueReconhecem(String nomeDaObra) {
        if (nomeDaObra == null || nomeDaObra.isBlank()) {
            return Set.of();
        }
        int melhor = 0;
        Set<String> vencedores = new TreeSet<>();
        for (IdentidadeObra identidade : identidades) {
            int peso = identidade.especificidadeEm(nomeDaObra);
            if (peso == 0 || peso < melhor) {
                continue;
            }
            if (peso > melhor) {
                melhor = peso;
                vencedores.clear();
            }
            vencedores.add(identidade.contextoId());
        }
        return vencedores;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe a identidade canônica derivada de cada obra registrada, para
     * que testes de catálogo e diagnóstico possam auditar QUAIS nomes o sistema reconhece —
     * sem reimplementar a derivação e correr o risco de auditar uma regra diferente da que roda
     * em produção.
     *
     * <p>INVARIANTES DO DOMÍNIO: mesma ordem de {@link #getProvedores()}; lista e identidades
     * imutáveis; é a MESMA instância usada por {@link #idsQueReconhecem(String)}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; catálogo vazio devolve lista vazia.
     *
     * @return identidades canônicas das obras registradas, na ordem do catálogo
     */
    public List<IdentidadeObra> identidades() {
        return identidades;
    }

    /**
     * Retorna apenas a lore/terminologia do contexto ativo, sem o restante do
     * prompt de traducao (prioridades, regras de concordancia, regras de
     * saida). Usado por revisoes pontuais (ex.: concordancia PT-BR) que nao
     * devem reenviar o prompt de traducao inteiro ao LLM como se fosse lore.
     */
    public String obterLoreAtiva() {
        return ContextoPrompt.obterLore(obterPromptAtivo());
    }

    public String obterNomeContextoAtivo() {
        return this.provedorAtivo != null ? this.provedorAtivo.getNomeExibicao() : SnapshotContexto.NOME_NEUTRO;
    }

    /**
     * Id do contexto ativo (não o nome de exibição). Usado para carimbar a
     * proveniência do cache de tradução, de modo que uma legenda em cache saiba
     * com qual lore foi produzida. Retorna {@code null} se não houver contexto ativo.
     */
    public String obterIdContextoAtivo() {
        return this.provedorAtivo != null ? this.provedorAtivo.getId() : null;
    }

    /**
     * Termos protegidos (não traduzir) do lore atualmente ativo. Usado pelo
     * detector de tradução idêntica para acompanhar o lore selecionado. Vazio
     * quando não há contexto ativo ou o contexto não declara termos.
     */
    public java.util.Set<String> termosProtegidosAtivos() {
        return this.provedorAtivo != null ? this.provedorAtivo.termosProtegidos() : java.util.Set.of();
    }

    // correcoesTerminologiaAtiva() foi REMOVIDO: seu único consumidor (o reforço
    // determinístico de terminologia em ProcessarArquivoUseCase) passou a ler o mapa do
    // SnapshotContexto congelado da execução. Ler o mapa daqui, no fim do arquivo, era
    // justamente uma das janelas pelas quais uma troca de lore no meio do episódio entrava.
    // Quem precisar do mapa fora de uma execução usa snapshotAtivo().correcoesTerminologia().

    private ProvedorContexto encontrarProvedorPadrao() {
        return provedores.stream()
                .filter(p -> ID_CONTEXTO_PADRAO.equals(p.getId()))
                .findFirst()
                .orElse(provedores.isEmpty() ? null : provedores.get(0));
    }

    private void validarIdsUnicos(List<ProvedorContexto> provedores) {
        Map<String, Long> contagemPorId = provedores.stream()
                .map(ProvedorContexto::getId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<String> duplicados = contagemPorId.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!duplicados.isEmpty()) {
            throw new IllegalStateException("IDs de contexto duplicados: " + duplicados);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede o startup quando DUAS obras distintas reivindicam
     * EXATAMENTE o mesmo nome canônico (ex.: os seis filmes de Break Blade declarando todos o
     * apelido {@code "Break Blade"}). Nesse estado, nenhuma pasta com esse nome jamais
     * resolveria para uma obra única: toda tradução da obra viraria bloqueio por ambiguidade,
     * em runtime, arquivo a arquivo. Isso é ERRO DE CONFIGURAÇÃO do catálogo de lores, não erro
     * dos dados que chegam — e erro de configuração se paga no boot, alto e cedo, não em uma
     * mensagem por episódio depois que o operador já mandou o lote rodar.
     *
     * <p>INVARIANTES DO DOMÍNIO: a colisão é por nome canônico EXATO (já normalizado) entre
     * provedores DIFERENTES. Nomes que apenas se contêm ({@code "Macross 7"} dentro de
     * {@code "Macross 7 Encore"}) NÃO são colisão — são hierarquia legítima, resolvida em
     * runtime por especificidade. Repetir o mesmo nome dentro do MESMO provedor (um apelido
     * igual ao id, por exemplo) também não é colisão: a identidade é um conjunto. A mensagem
     * lista nome e ids em ordem determinística para o conserto ser óbvio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link IllegalStateException}, que aborta a
     * construção do bean e derruba a aplicação no boot. Falhar aberto aqui (só logar) deixaria
     * o catálogo permanentemente incapaz de identificar aquela obra.
     *
     * @param identidades identidades canônicas derivadas dos provedores registrados
     */
    private void validarIdentidadesSemColisao(List<IdentidadeObra> identidades) {
        Map<String, Set<String>> idsPorNome = new LinkedHashMap<>();
        for (IdentidadeObra identidade : identidades) {
            for (String nome : identidade.nomesCanonicos()) {
                idsPorNome.computeIfAbsent(nome, chave -> new TreeSet<>()).add(identidade.contextoId());
            }
        }

        Map<String, Set<String>> colisoes = new TreeMap<>();
        idsPorNome.forEach((nome, ids) -> {
            if (ids.size() > 1) {
                colisoes.put(nome, ids);
            }
        });

        if (!colisoes.isEmpty()) {
            List<String> detalhes = new ArrayList<>();
            colisoes.forEach((nome, ids) -> detalhes.add("\"" + nome + "\" -> " + ids));
            throw new IllegalStateException(
                    "Colisão de identidade canônica entre contextos (erro de CONFIGURAÇÃO do catálogo de lores, "
                            + "não dos dados): o mesmo nome identifica obras diferentes, então nenhuma pasta com "
                            + "esse nome poderia resolver para uma obra única. Torne o apelido de pasta específico "
                            + "de cada obra. Colisões: " + String.join("; ", detalhes));
        }
    }
}
