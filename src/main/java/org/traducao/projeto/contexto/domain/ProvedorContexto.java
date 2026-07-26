package org.traducao.projeto.contexto.domain;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de um provedor de contexto/lore de tradução — cada
 * obra (Gundam, Macross, Danmachi...) implementa esta interface para fornecer o
 * prompt de sistema do LLM, o rótulo de UI e os termos que não devem ser traduzidos.
 * É o ponto de extensão do módulo compartilhado {@code contexto}: novas obras entram
 * apenas adicionando implementações {@code @Component}, sem tocar em quem consome.
 *
 * <p>INVARIANTES DO DOMÍNIO: interface pura (só depende do JDK); {@link #getId()} é o
 * identificador único e estável usado para seleção e para carimbar a proveniência do
 * cache; {@link #obterPromptSistema()} devolve o prompt completo já montado; termos
 * protegidos são um conjunto imutável (por padrão vazio). Nenhum método realiza I/O.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: os métodos não lançam por contrato; um provedor
 * mal formado (id nulo/duplicado) é rejeitado por quem agrega os provedores
 * ({@code GerenciadorContexto}), não por esta interface.
 */
public interface ProvedorContexto {
    /**
     * Retorna o ID único para seleção via UI.
     */
    String getId();

    /**
     * Retorna o nome amigável para exibição no combo box da UI.
     */
    String getNomeExibicao();

    /**
     * Retorna o prompt de sistema completo para o LLM, com regras gerais e lore especifico da midia.
     */
    String obterPromptSistema();

    /**
     * Termos desta obra que NÃO devem ser traduzidos (nomes próprios, facções,
     * patentes, lugares, mecha). Por padrão vazio; cada contexto pode
     * sobrescrever para que o detector de "tradução idêntica" proteja o lore
     * selecionado, em vez de depender só da lista global fixa no detector.
     */
    default Set<String> termosProtegidos() {
        return Set.of();
    }

    /**
     * Mapa forma-ruim → termo canônico para REFORÇO DETERMINÍSTICO de terminologia
     * pós-tradução: quando o LLM traduz um termo de mundo que a lore manda manter no
     * original (ex.: {@code "Legião" → "Legion"}), este mapa permite restaurar a grafia
     * oficial SEM depender do modelo. Por padrão vazio; cada obra sobrescreve com as
     * traduções indevidas observadas. A chave é a forma-ruim (em PT); o valor é o termo
     * canônico a restaurar. A restauração só ocorre quando o ORIGINAL contém o termo
     * canônico — nunca altera uma tradução legítima que não veio dele.
     */
    default Map<String, String> correcoesTerminologia() {
        return Map.of();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nomes ALTERNATIVOS e abreviações pelos quais ESTA obra também é
     * reconhecível na pasta em que os arquivos de legenda moram (ex.: {@code "Stardust Memory"},
     * {@code "Z Gundam"}, {@code "86"}). É o COMPLEMENTO da identidade canônica: o id e o nome
     * de exibição já identificam a obra automaticamente (ver {@link IdentidadeObra#de}), e este
     * conjunto cobre o que eles não alcançam — o título abreviado, o nome do arco/subtítulo, a
     * grafia que o grupo de fansub usa.
     *
     * <p>INVARIANTES DO DOMÍNIO: por padrão VAZIO — e vazio NÃO significa mais "obra sem
     * identidade": desde a identidade canônica derivada, toda obra do catálogo se reconhece
     * pelo id e pelo nome de exibição sem declarar nada. Cada apelido é comparado por
     * SEQUÊNCIA DE PALAVRAS INTEIRAS, então apelidos curtos e numéricos ({@code "86"},
     * {@code "0083"}) são seguros: não casam dentro de outra palavra nem dentro de
     * {@code "(1986)"}. Um apelido idêntico ao de OUTRO provedor é erro de configuração e
     * impede o startup — declarar {@code "Break Blade"} nos seis filmes, por exemplo, é
     * proibido; cada entrada precisa de um nome que só ela reivindique.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; o conjunto é imutável e sem I/O.
     *
     * @return apelidos de pasta desta obra, possivelmente vazio
     */
    default Set<String> apelidosPasta() {
        return Set.of();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde se o nome de uma pasta de obra identifica ESTA lore.
     * Cada obra sabe se reconhecer; quem orquestra apenas pergunta, sem heurística própria.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega à {@link IdentidadeObra} derivada deste provedor —
     * FONTE ÚNICA do reconhecimento. Nenhuma comparação de nome de obra é reimplementada aqui:
     * duas cópias da regra divergiriam e o catálogo passaria a discordar da guarda sobre o que
     * é a mesma obra. O casamento exige sequência de palavras INTEIRAS e nunca é um palpite por
     * similaridade, prefixo ou distância.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome nulo/em branco devolve {@code false}; nunca
     * lança.
     *
     * @param nomeDaPasta nome da pasta da obra, cru como veio do sistema de arquivos
     * @return {@code true} somente quando um nome canônico casa por palavras inteiras
     */
    default boolean reconhecePasta(String nomeDaPasta) {
        return IdentidadeObra.de(this).reconhece(nomeDaPasta);
    }
}
