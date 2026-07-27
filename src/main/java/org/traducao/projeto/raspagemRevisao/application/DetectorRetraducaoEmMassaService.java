package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DiagnosticoRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: descobre se a legenda apresentada à revisão é, na verdade, um arquivo que
 * NÃO foi traduzido — ou que regrediu quase inteiro ao inglês. É a proteção que impede a Opção 6 de
 * assumir em silêncio o trabalho da Tradução Local: revisar 1.200 falas inglesas uma a uma gastaria
 * a GPU refazendo o pipeline errado e produziria uma tradução pior que a da etapa própria.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Conta apenas falas AUDITÁVEIS: diálogo, com texto, não filtradas pelo
 *       {@link FiltroAuditoriaLinha} e com original comparável. Placas e karaokê ficam de fora —
 *       incluí-los inflaria o denominador e faria a proteção deixar de disparar.</li>
 *   <li>Uma fala idêntica ao inglês NÃO é falha quando é composta só de termos canônicos da lore.
 *       "Axis" continua "Axis" em português, e sem esta exceção uma obra com muitos nomes próprios
 *       seria bloqueada por estar correta.</li>
 *   <li>Só MEDE. Não escreve, não imprime, não decide o que fazer com o arquivo — quem reage ao
 *       diagnóstico é o caso de uso, porque a reação envolve gravar o que o cache já recuperou.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Documento nulo ou sem base comparável produz diagnóstico vazio e NÃO bloqueia: na ausência de
 * dados não se recusa o arquivo.
 */
@Service
public class DetectorRetraducaoEmMassaService {

    private final FiltroAuditoriaLinha filtroAuditoria;
    private final ProtetorTermosLoreService protetorLore;
    private final AuditorProblemasLegendaService auditor;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne o filtro de linhas, a guarda de lore e o auditor.
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do detector.
     */
    public DetectorRetraducaoEmMassaService(
            FiltroAuditoriaLinha filtroAuditoria,
            ProtetorTermosLoreService protetorLore,
            AuditorProblemasLegendaService auditor) {
        this.filtroAuditoria = filtroAuditoria;
        this.protetorLore = protetorLore;
        this.auditor = auditor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mede quantas falas comparáveis continuam em inglês e diz se isso passa
     * do limite seguro.
     *
     * <p>INVARIANTES DO DOMÍNIO: o documento recebido já passou pela recuperação via cache — medir
     * antes dela bloquearia arquivos que a sincronização acabou de consertar.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas ausentes devolvem diagnóstico zerado, que não
     * bloqueia; não lança.
     *
     * @param documento a legenda já sincronizada
     * @param originaisPorIndice o original inglês de cada fala, por índice
     * @param contexto lore ativa da obra
     * @return as métricas e o veredicto do limiar
     */
    public DiagnosticoRetraducao diagnosticar(
        DocumentoLegenda documento,
        Map<Integer, String> originaisPorIndice,
        ContextoRevisao contexto
    ) {
        if (documento == null || originaisPorIndice == null || originaisPorIndice.isEmpty()) {
            return new DiagnosticoRetraducao(0, 0, false);
        }
        int auditaveis = 0;
        int naoTraduzidas = 0;
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || evento.texto() == null || evento.texto().isBlank()
                || filtroAuditoria.deveIgnorar(evento, evento.texto())) {
                continue;
            }
            String original = originaisPorIndice.get(evento.indice());
            if (original == null || original.isBlank()) {
                continue;
            }
            auditaveis++;
            if (!mesmoTexto(original, evento.texto())) {
                continue;
            }
            if (protetorLore.contemSomenteTermosCanonicos(
                original, contexto.lore(), contexto.termosProtegidos())) {
                continue;
            }
            ResultadoDeteccaoConcordancia resultado = auditor.auditar(original, evento.texto());
            if (resultado.motivos().stream()
                .anyMatch(m -> m.contains(PoliticaRetraducao.NAO_TRADUZIDA))) {
                naoTraduzidas++;
            }
        }
        return new DiagnosticoRetraducao(auditaveis, naoTraduzidas,
            PoliticaRetraducao.excedeLimiarRetraducaoEmMassa(auditaveis, naoTraduzidas));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compara duas falas ignorando diferenças de espaçamento, que não mudam o
     * que o espectador lê.
     *
     * <p>INVARIANTES DO DOMÍNIO: é detalhe DESTA comparação, não um normalizador compartilhado. O
     * caso de uso tem um helper equivalente para outros fins; dar um lar comum a uma função de uma
     * linha custaria mais do que a cópia — foi a mesma conclusão do item (c) desta fase.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos degradam para vazio; não lança.
     */
    private static boolean mesmoTexto(String a, String b) {
        return normalizar(a).equals(normalizar(b));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }
}
