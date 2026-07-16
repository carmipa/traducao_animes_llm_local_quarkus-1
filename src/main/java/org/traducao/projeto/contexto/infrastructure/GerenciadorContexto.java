package org.traducao.projeto.contexto.infrastructure;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ContextoNaoEncontradoException;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
 * (case-insensitive) e seus ids são únicos (falha na construção se houver duplicata);
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
    private final ProvedorContexto provedorPadrao;

    private volatile ProvedorContexto provedorAtivo;

    public GerenciadorContexto(List<ProvedorContexto> provedores) {
        this.provedores = provedores.stream()
                .sorted(Comparator.comparing(ProvedorContexto::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
                .toList();
        validarIdsUnicos(this.provedores);
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
                                + provedores.stream().map(ProvedorContexto::getId).collect(Collectors.joining(", "))));
        this.provedorAtivo = encontrado;
        return this.provedorAtivo;
    }

    public String obterPromptAtivo() {
        if (this.provedorAtivo == null) {
            return "Voce e um tradutor especialista. Traduza fielmente.";
        }
        return this.provedorAtivo.obterPromptSistema();
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
        return this.provedorAtivo != null ? this.provedorAtivo.getNomeExibicao() : "Padrao";
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
}
