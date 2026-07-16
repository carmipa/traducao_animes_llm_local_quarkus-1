package org.traducao.projeto.auditorConteudoLegendas.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaArquivoUnico;
import org.traducao.projeto.auditorConteudoLegendas.domain.RegraAuditoriaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaSrt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: audita legendas em três escopos — só o original (EN), só
 * o traduzido (PT-BR) ou os dois em comparação — produzindo um relatório didático
 * com formato, integridade e anomalias.
 * <p>INVARIANTES DO DOMÍNIO: somente arquivos regulares ASS, SSA ou SRT entram na
 * auditoria; o modo comparativo executa as regras de par (original ↔ traduzido) e
 * os modos de arquivo único executam as regras estruturais/temporais isoladas.
 * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ausente, formato não suportado ou
 * erro de leitura gera {@link AuditoriaException} sem relatório parcial.
 */
@ApplicationScoped
public class AuditorConteudoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditorConteudoUseCase.class);

    private final LeitorLegendaAss leitorLegendaAss;
    private final LeitorLegendaSrt leitorLegendaSrt;
    private final Instance<RegraAuditoriaConteudo> regras;
    private final Instance<RegraAuditoriaArquivoUnico> regrasArquivoUnico;
    private final ValidadorParsingLegenda validadorParsing;
    private final TelemetriaAuditoriaService telemetria;

    /**
     * PROPÓSITO DE NEGÓCIO: reúne leitores, regras (comparativas e de arquivo
     * único) e telemetria necessários para analisar os formatos suportados.
     * <p>INVARIANTES DO DOMÍNIO: dependências são obrigatórias; os leitores ASS/SSA
     * e SRT e os dois conjuntos de regras permanecem separados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: o contêiner de injeção interrompe a
     * inicialização se alguma dependência não estiver disponível.
     */
    @Inject
    public AuditorConteudoUseCase(
        LeitorLegendaAss leitorLegendaAss,
        LeitorLegendaSrt leitorLegendaSrt,
        Instance<RegraAuditoriaConteudo> regras,
        Instance<RegraAuditoriaArquivoUnico> regrasArquivoUnico,
        ValidadorParsingLegenda validadorParsing,
        TelemetriaAuditoriaService telemetria
    ) {
        this.leitorLegendaAss = leitorLegendaAss;
        this.leitorLegendaSrt = leitorLegendaSrt;
        this.regras = regras;
        this.regrasArquivoUnico = regrasArquivoUnico;
        this.validadorParsing = validadorParsing;
        this.telemetria = telemetria;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a assinatura histórica que compara original e
     * traduzido, usada por testes e integrações existentes.
     * <p>INVARIANTES DO DOMÍNIO: equivale a chamar o modo {@link ModoAuditoria#AMBAS}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga as mesmas exceções do modo
     * comparativo.
     */
    public RelatorioAuditoriaConteudo auditar(Path caminhoOriginal, Path caminhoTraduzido) {
        return auditarComparativo(caminhoOriginal, caminhoTraduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ponto de entrada por modo escolhido nas abas do painel.
     * <p>INVARIANTES DO DOMÍNIO: {@link ModoAuditoria#AMBAS} exige os dois caminhos;
     * {@link ModoAuditoria#ORIGINAL} usa {@code caminhoOriginal};
     * {@link ModoAuditoria#TRADUZIDO} usa {@code caminhoTraduzido}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho exigido nulo gera
     * {@link AuditoriaException}; demais falhas seguem o modo específico.
     */
    public RelatorioAuditoriaConteudo auditar(ModoAuditoria modo, Path caminhoOriginal, Path caminhoTraduzido) {
        return switch (modo) {
            case AMBAS -> auditarComparativo(caminhoOriginal, caminhoTraduzido);
            case ORIGINAL -> auditarArquivoUnico(caminhoOriginal, ModoAuditoria.ORIGINAL);
            case TRADUZIDO -> auditarArquivoUnico(caminhoTraduzido, ModoAuditoria.TRADUZIDO);
        };
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a análise comparativa (original ↔ traduzido) e
     * devolve todos os dados necessários para tela, exportação e telemetria.
     * <p>INVARIANTES DO DOMÍNIO: o formato informado no relatório é obtido do
     * próprio caminho validado e determina o leitor usado; todas as regras
     * comparativas são executadas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: encapsula falhas inesperadas em
     * {@link AuditoriaException}; falhas de domínio preservam sua mensagem.
     */
    private RelatorioAuditoriaConteudo auditarComparativo(Path caminhoOriginal, Path caminhoTraduzido) {
        if (caminhoOriginal == null || caminhoTraduzido == null) {
            throw new AuditoriaException("Modo comparativo exige os arquivos original e traduzido.");
        }
        String formatoOriginal = validarArquivo(caminhoOriginal, "original");
        String formatoTraduzido = validarArquivo(caminhoTraduzido, "traduzido");

        long inicioMs = System.currentTimeMillis();
        log.info("Iniciando auditoria de conteudo: {} vs {}", caminhoOriginal, caminhoTraduzido);
        System.out.println("\n=== Auditoria de Conteudo de Legendas ===");
        System.out.println("Original : " + caminhoOriginal.toAbsolutePath());
        System.out.println("Formato original: " + formatoOriginal);
        System.out.println("Traduzido: " + caminhoTraduzido.toAbsolutePath());
        System.out.println("Formato traduzido: " + formatoTraduzido);

        try {
            RelatorioAuditoriaConteudo relatorio = new RelatorioAuditoriaConteudo(
                caminhoOriginal.getFileName().toString(),
                caminhoTraduzido.getFileName().toString(),
                formatoOriginal,
                formatoTraduzido,
                ModoAuditoria.AMBAS
            );

            DocumentoLegenda docOriginal = lerLegenda(caminhoOriginal, formatoOriginal);
            DocumentoLegenda docTraduzido = lerLegenda(caminhoTraduzido, formatoTraduzido);

            List<AnomaliaConteudo> anomalias = new ArrayList<>();
            int regrasExecutadas = 0;

            // Bug 5 — integridade de parsing dos dois arquivos brutos.
            anomalias.addAll(validadorParsing.validar(caminhoOriginal, formatoOriginal, "original"));
            for (AnomaliaConteudo a : validadorParsing.validar(caminhoTraduzido, formatoTraduzido, "traduzido")) {
                anomalias.add(reposicionarParaTraduzido(a));
            }

            // Bug 3 — o modo AMBAS também roda as regras estruturais sobre CADA lado.
            for (RegraAuditoriaArquivoUnico regra : regrasArquivoUnico) {
                regrasExecutadas += 2;
                anomalias.addAll(regra.auditar(docOriginal));
                for (AnomaliaConteudo a : regra.auditar(docTraduzido)) {
                    anomalias.add(reposicionarParaTraduzido(a));
                }
            }

            // Bug 2 — índice ASS (posicional) ≠ índice SRT (número do bloco): comparar
            // por índice entre formatos diferentes associa falas erradas. Bloqueamos.
            if (formatosComparaveis(formatoOriginal, formatoTraduzido)) {
                for (RegraAuditoriaConteudo regra : regras) {
                    regrasExecutadas++;
                    List<AnomaliaConteudo> encontradas = regra.auditar(docOriginal, docTraduzido);
                    log.debug("Regra '{}' encontrou {} anomalia(s)", regra.getNome(), encontradas.size());
                    anomalias.addAll(encontradas);
                }
            } else {
                anomalias.add(new AnomaliaConteudo(
                    AnomaliaConteudo.TipoSeveridade.CRITICAL,
                    "Formatos Incompatíveis para Comparação",
                    "Comparação por índice entre formatos diferentes (" + formatoOriginal + " ↔ "
                        + formatoTraduzido + ") não é confiável: o índice ASS é posicional e o SRT é o "
                        + "número do bloco. As regras comparativas foram puladas.",
                    null, null,
                    "Usar o mesmo formato nos dois lados, ou auditar cada arquivo no modo de arquivo único."));
            }

            for (AnomaliaConteudo a : deduplicar(anomalias)) {
                relatorio.adicionarAnomalia(a);
            }

            long duracaoMs = System.currentTimeMillis() - inicioMs;
            String caminhoJson = telemetria.registrar(relatorio, caminhoTraduzido, duracaoMs);
            relatorio.definirMetadados(duracaoMs, caminhoJson, regrasExecutadas);

            log.info("Auditoria de conteudo concluida em {} ms — {} anomalia(s), {} regra(s)",
                duracaoMs, relatorio.getAnomalias().size(), regrasExecutadas);
            System.out.println("Regras executadas: " + regrasExecutadas);
            System.out.println("Anomalias: " + relatorio.getAnomalias().size());
            System.out.println("Duracao: " + duracaoMs + " ms");

            return relatorio;
        } catch (AuditoriaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na auditoria de conteudo ({} vs {}): {}",
                caminhoOriginal, caminhoTraduzido, e.getMessage(), e);
            throw new AuditoriaException("Falha ao auditar os arquivos: " + e.getMessage(), e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: audita um único arquivo (só original ou só traduzido)
     * aplicando as regras estruturais e temporais que não dependem de referência.
     * <p>INVARIANTES DO DOMÍNIO: apenas o lado auditado carrega nome e formato no
     * relatório; anomalias do modo TRADUZIDO são reposicionadas para o lado
     * traduzido para que a tela rotule o evento corretamente.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho nulo, arquivo ausente ou formato
     * não suportado gera {@link AuditoriaException}; falhas inesperadas são
     * encapsuladas na mesma exceção.
     */
    private RelatorioAuditoriaConteudo auditarArquivoUnico(Path caminho, ModoAuditoria modo) {
        if (caminho == null) {
            throw new AuditoriaException("Modo de arquivo único exige o caminho do arquivo a auditar.");
        }
        String papel = modo == ModoAuditoria.ORIGINAL ? "original" : "traduzido";
        String formato = validarArquivo(caminho, papel);

        long inicioMs = System.currentTimeMillis();
        log.info("Iniciando auditoria de arquivo unico ({}): {}", papel, caminho);
        System.out.println("\n=== Auditoria de Conteudo de Legendas (arquivo unico: " + papel + ") ===");
        System.out.println("Arquivo : " + caminho.toAbsolutePath());
        System.out.println("Formato : " + formato);

        try {
            boolean ehOriginal = modo == ModoAuditoria.ORIGINAL;
            RelatorioAuditoriaConteudo relatorio = new RelatorioAuditoriaConteudo(
                ehOriginal ? caminho.getFileName().toString() : null,
                ehOriginal ? null : caminho.getFileName().toString(),
                ehOriginal ? formato : null,
                ehOriginal ? null : formato,
                modo
            );

            DocumentoLegenda documento = lerLegenda(caminho, formato);

            List<AnomaliaConteudo> anomalias = new ArrayList<>();
            int regrasExecutadas = 0;

            // Bug 5 — integridade de parsing do arquivo bruto.
            for (AnomaliaConteudo a : validadorParsing.validar(caminho, formato, papel)) {
                anomalias.add(ehOriginal ? a : reposicionarParaTraduzido(a));
            }

            for (RegraAuditoriaArquivoUnico regra : regrasArquivoUnico) {
                regrasExecutadas++;
                for (AnomaliaConteudo a : regra.auditar(documento)) {
                    anomalias.add(ehOriginal ? a : reposicionarParaTraduzido(a));
                }
            }

            for (AnomaliaConteudo a : deduplicar(anomalias)) {
                relatorio.adicionarAnomalia(a);
            }

            long duracaoMs = System.currentTimeMillis() - inicioMs;
            String caminhoJson = telemetria.registrar(relatorio, caminho, duracaoMs);
            relatorio.definirMetadados(duracaoMs, caminhoJson, regrasExecutadas);

            log.info("Auditoria de arquivo unico ({}) concluida em {} ms — {} anomalia(s), {} regra(s)",
                papel, duracaoMs, relatorio.getAnomalias().size(), regrasExecutadas);
            System.out.println("Regras executadas: " + regrasExecutadas);
            System.out.println("Anomalias: " + relatorio.getAnomalias().size());
            System.out.println("Duracao: " + duracaoMs + " ms");

            return relatorio;
        } catch (AuditoriaException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na auditoria de arquivo unico ({}): {}", caminho, e.getMessage(), e);
            throw new AuditoriaException("Falha ao auditar o arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: move o evento analisado do slot "original" para o slot
     * "traduzido" quando a auditoria é do arquivo traduzido, para que a tela e o
     * markdown rotulem a linha como traduzida.
     * <p>INVARIANTES DO DOMÍNIO: as regras de arquivo único sempre preenchem
     * {@code eventoOriginal}; os demais campos da anomalia são preservados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não executa I/O e nunca lança.
     */
    private AnomaliaConteudo reposicionarParaTraduzido(AnomaliaConteudo anomalia) {
        return new AnomaliaConteudo(
            anomalia.severidade(),
            anomalia.regra(),
            anomalia.descricao(),
            null,
            anomalia.eventoOriginal(),
            anomalia.sugestaoCorrecao()
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se dois formatos podem ser comparados por índice.
     * <p>INVARIANTES DO DOMÍNIO: ASS e SSA são a mesma família (índice posicional);
     * SRT é família própria (índice = número do bloco). Famílias diferentes não são
     * comparáveis por índice.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato desconhecido cai na família ASS.
     */
    private boolean formatosComparaveis(String formatoA, String formatoB) {
        return familiaFormato(formatoA).equals(familiaFormato(formatoB));
    }

    private String familiaFormato(String formato) {
        return "SRT".equals(formato) ? "SRT" : "ASS";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remove anomalias equivalentes antes de apresentar, já
     * que AMBAS combina regras estruturais e comparativas que podem apontar o mesmo
     * defeito na mesma linha.
     * <p>INVARIANTES DO DOMÍNIO: a chave considera regra, severidade, índices dos
     * eventos e descrição; a ordem de primeira ocorrência é preservada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não executa I/O e nunca lança.
     */
    private List<AnomaliaConteudo> deduplicar(List<AnomaliaConteudo> anomalias) {
        Set<String> vistos = new LinkedHashSet<>();
        List<AnomaliaConteudo> unicas = new ArrayList<>();
        for (AnomaliaConteudo a : anomalias) {
            String chave = a.regra() + "|" + a.severidade() + "|"
                + (a.eventoOriginal() != null ? a.eventoOriginal().indice() : "-") + "|"
                + (a.eventoTraduzido() != null ? a.eventoTraduzido().indice() : "-") + "|"
                + a.descricao();
            if (vistos.add(chave)) {
                unicas.add(a);
            }
        }
        return unicas;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida a entrada e devolve o rótulo de formato que
     * aparecerá no relatório.
     * <p>INVARIANTES DO DOMÍNIO: somente ASS, SSA e SRT são aceitos; o rótulo é
     * sempre retornado em maiúsculas.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link AuditoriaException} com o
     * papel do arquivo e a lista dos formatos suportados.
     */
    private String validarArquivo(Path caminho, String papel) {
        if (!Files.isRegularFile(caminho)) {
            throw new AuditoriaException("Arquivo " + papel + " nao encontrado: " + caminho);
        }
        String nome = caminho.getFileName().toString().toLowerCase();
        if (nome.endsWith(".ass")) return "ASS";
        if (nome.endsWith(".ssa")) return "SSA";
        if (nome.endsWith(".srt")) return "SRT";
        throw new AuditoriaException("Formato do arquivo " + papel
            + " nao suportado. Use ASS, SSA ou SRT: " + caminho);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: direciona cada artefato ao parser compatível para
     * que a auditoria use o mesmo modelo de documento interno.
     * <p>INVARIANTES DO DOMÍNIO: SRT usa o leitor SubRip; ASS e SSA usam o
     * leitor Advanced SubStation Alpha.
     * <p>COMPORTAMENTO EM CASO DE FALHA: formato fora do conjunto validado gera
     * {@link AuditoriaException} e nenhuma regra é executada.
     */
    private DocumentoLegenda lerLegenda(Path caminho, String formato) {
        return switch (formato) {
            case "SRT" -> leitorLegendaSrt.ler(caminho);
            case "ASS", "SSA" -> leitorLegendaAss.ler(caminho);
            default -> throw new AuditoriaException("Formato nao suportado: " + formato);
        };
    }
}
