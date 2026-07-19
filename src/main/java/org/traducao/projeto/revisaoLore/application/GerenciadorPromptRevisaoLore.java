package org.traducao.projeto.revisaoLore.application;

import jakarta.enterprise.inject.Instance;
import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GerenciadorPromptRevisaoLore {

    private final List<ProvedorPromptRevisaoLore> provedores;

    public GerenciadorPromptRevisaoLore(Instance<ProvedorPromptRevisaoLore> provedores) {
        this.provedores = provedores.stream()
            .sorted(Comparator.comparing(ProvedorPromptRevisaoLore::getNomeExibicao, String.CASE_INSENSITIVE_ORDER))
            .toList();
        validarIdsUnicos(this.provedores);
    }

    public List<ProvedorPromptRevisaoLore> getProvedores() {
        return provedores;
    }

    public boolean existePrompt(String id) {
        return id != null && provedores.stream().anyMatch(p -> p.getId().equals(id));
    }

    public ProvedorPromptRevisaoLore obterPrompt(String id) {
        if (id == null || id.isBlank()) {
            throw new RevisaoLoreException("Prompt de revisao de lore obrigatorio.");
        }
        return provedores.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new RevisaoLoreException(
                "Prompt de revisao de lore desconhecido: \"" + id + "\". Prompts disponiveis: "
                    + provedores.stream().map(ProvedorPromptRevisaoLore::getId).collect(Collectors.joining(", "))));
    }

    public String obterNome(String id) {
        return obterPrompt(id).getNomeExibicao();
    }

    public String obterPromptSistema(String id) {
        return obterPrompt(id).obterPromptSistema();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega o mapa de terminologia canônica da obra ativa para o
     * reforço determinístico da revisão de lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega ao provedor do {@code id}; chave = forma-ruim PT,
     * valor = canônico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code id} nulo/desconhecido lança
     * {@link RevisaoLoreException} (mesma política de {@link #obterPrompt(String)}).
     */
    public Map<String, String> correcoesTerminologia(String id) {
        return obterPrompt(id).correcoesTerminologia();
    }

    private void validarIdsUnicos(List<ProvedorPromptRevisaoLore> provedores) {
        Map<String, Long> contagemPorId = provedores.stream()
            .map(ProvedorPromptRevisaoLore::getId)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<String> duplicados = contagemPorId.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        if (!duplicados.isEmpty()) {
            throw new IllegalStateException("IDs de prompt de revisao de lore duplicados: " + duplicados);
        }
    }
}
