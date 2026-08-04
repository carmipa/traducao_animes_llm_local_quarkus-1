package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducaoCorrige.domain.ContextoDoCache;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ModoReforcoTerminologia;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;
import org.traducao.projeto.traducaoCorrige.domain.ports.AuditoriaCorrecaoCachePort;
import org.traducao.projeto.traducaoCorrige.domain.ports.TelemetriaCorrecaoPort;
import org.traducao.projeto.traducaoCorrige.infrastructure.config.CorrecaoCacheProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: aplica a terminologia oficial da obra ao cache JÁ GRAVADO, sem
 * retraduzir. Fecha a lacuna que motivou o Plano-Mestre do corretor: as correções
 * determinísticas de terminologia agiam apenas no momento da tradução, então cache antigo
 * carregava para sempre o defeito com que nasceu, e aplicar uma regra nova ao acervo custava
 * ~4 horas de GPU por obra — para algo que uma varredura resolve em segundos.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Ensaio por padrão.</b> {@code aplicar=false} percorre tudo e conta, sem tocar um byte.
 *       Escrever no acervo é uma decisão explícita do operador, nunca o comportamento default.</li>
 *   <li><b>Nenhuma regra de terminologia mora aqui.</b> O julgamento é do
 *       {@link EnforcadorTermosLore} (peer {@code qualidadeTraducao}) e o mapa é do peer
 *       {@code contexto}. Esta classe orquestra e conta — reimplementar qualquer parte
 *       recriaria a divergência que a unificação do enforcer acabou de eliminar.</li>
 *   <li><b>A lore é a do arquivo, não a da tela.</b> O contexto vem de
 *       {@link ContextoManutencaoCacheService#ativarSnapshot}, que aplica a guarda obra×contexto
 *       antes de entregar o mapa. Um cache carimbado com a lore errada é RECUSADO, não
 *       "corrigido" com a terminologia de outra obra.</li>
 *   <li><b>Nunca degrada.</b> O enforcer só restaura quando o original inglês sustenta o termo;
 *       arquivo só é reescrito se algo mudou; backup precede a troca atômica.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha em um arquivo é contabilizada e auditada, o original permanece intacto no disco e a
 * varredura segue para o próximo — o lote termina com {@code CONCLUIDO_COM_FALHAS}.
 */
@Service
public class ReforcarTerminologiaCacheUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReforcarTerminologiaCacheUseCase.class);
    private static final String OPERACAO = "reforco-terminologia";

    private final CacheManutencaoService cacheService;
    private final ContextoManutencaoCacheService contextoService;
    private final EnforcadorTermosLore enforcador;
    private final ValidadorTraducaoService validador;
    private final AuditoriaCorrecaoCachePort auditoria;
    private final TelemetriaCorrecaoPort telemetria;
    private final CorrecaoCacheProperties propriedades;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as peças da operação — leitura/escrita de cache, resolução da
     * lore do arquivo, o reforço determinístico de terminologia, o reparo de troca de entidade
     * e a auditoria.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas. As duas saídas — auditoria e
     * telemetria — entram por PORTA, não pelo tipo concreto: é o que permite testar esta classe
     * sem disco e sem a fatia {@code telemetria}, e é a forma que a FASE 2 do Plano-Mestre
     * prescreve para as quatro fatias da correção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     */
    public ReforcarTerminologiaCacheUseCase(
        CacheManutencaoService cacheService,
        ContextoManutencaoCacheService contextoService,
        EnforcadorTermosLore enforcador,
        ValidadorTraducaoService validador,
        AuditoriaCorrecaoCachePort auditoria,
        TelemetriaCorrecaoPort telemetria,
        CorrecaoCacheProperties propriedades
    ) {
        this.cacheService = cacheService;
        this.contextoService = contextoService;
        this.enforcador = enforcador;
        this.validador = validador;
        this.auditoria = auditoria;
        this.telemetria = telemetria;
        this.propriedades = propriedades;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: declara no BOOT se a escrita no acervo está autorizada. A receita de
     * fatia nova do projeto exige capacidade nova com flag OFF e LOG DE ESTADO NO INÍCIO — sem o
     * log, uma flag desligada vira uma operação que "não funciona" sem ninguém saber por quê, e
     * uma flag ligada por engano vira escrita silenciosa sobre trabalho pronto.
     *
     * <p>INVARIANTES DO DOMÍNIO: roda uma vez, no start; não decide nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: só loga; nunca impede o boot.
     */
    @PostConstruct
    void registrarEstadoInicial() {
        log.info("[ REFORCO-TERMINOLOGIA ] escrita no acervo {} "
                + "(correcao-cache.reforco-terminologia-aplicar-habilitado={}). O ensaio roda sempre.",
            propriedades.reforcoTerminologiaAplicarHabilitado() ? "AUTORIZADA" : "BLOQUEADA",
            propriedades.reforcoTerminologiaAplicarHabilitado());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se a escrita no acervo está autorizada, para que a borda HTTP
     * possa RECUSAR o pedido em vez de enfileirar um job que já nasce condenado.
     *
     * <p>INVARIANTES DO DOMÍNIO: é a MESMA fonte que {@link #executar} consulta. Expor a capacidade
     * — e não a propriedade — mantém a configuração dentro desta fatia: quem chama pergunta "posso
     * escrever?", não "qual o valor da flag?".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: leitura simples; nunca lança.
     */
    public boolean escritaNoAcervoAutorizada() {
        return propriedades.reforcoTerminologiaAplicarHabilitado();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ensaia o reforço sobre a pasta de cache sem escrever nada — a forma
     * segura de descobrir o que a regra nova mudaria no acervo antes de autorizar a mudança.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhum arquivo é aberto para escrita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve resultado com {@code falhas > 0}.
     */
    public ResultadoReforcoTerminologia ensaiar(Path raizCache, String contextoFallback) {
        return executar(raizCache, contextoFallback, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: percorre a pasta de cache aplicando (ou apenas medindo) a
     * terminologia oficial de cada obra sobre as traduções já gravadas.
     *
     * <p>INVARIANTES DO DOMÍNIO: um arquivo por vez, na fila serial; arquivo sem mapa de
     * terminologia é pulado sem custo; a sessão de backup só existe quando há escrita.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inexistente ou varredura quebrada conta falha e
     * devolve o resultado; nunca propaga exceção ao chamador.
     *
     * @param raizCache raiz da pasta de cache
     * @param contextoFallback contexto da UI, usado só para cache legado sem proveniência
     * @param aplicar {@code false} para ensaiar; {@code true} para escrever no disco
     */
    public ResultadoReforcoTerminologia executar(Path raizCache, String contextoFallback, boolean aplicar) {
        long inicioMs = System.currentTimeMillis();
        if (aplicar && !propriedades.reforcoTerminologiaAplicarHabilitado()) {
            // Recusa ANTES de abrir qualquer arquivo. Escrever sobre trabalho pronto exige
            // autorização declarada em configuração, não só um clique — e a recusa é ruidosa,
            // porque uma operação que "não faz nada" em silêncio é indistinguível de um defeito.
            //
            // O desfecho é RECUSADO_POR_CONFIG, não "ensaio com uma falha". Até 2026-07-28 esta
            // guarda construía o acumulador com aplicar=false e somava falhas++, e o resultado foi
            // medido em produção: o operador clicou Aplicar duas vezes e leu, nas duas,
            // "ENSAIO (nada foi escrito) — CONCLUIDO_COM_FALHAS — arquivos: 0 — falhas: 1". A razão
            // real só existia neste log, que não chega à tela. Ver ModoReforcoTerminologia.
            //
            // A rota HTTP recusa antes de enfileirar; esta guarda é a defesa de dentro, para
            // chamador que não venha pela web.
            Acumulador bloqueado = new Acumulador(ModoReforcoTerminologia.RECUSADO_POR_CONFIG);
            log.error("[ REFORCO-TERMINOLOGIA ] APLICAÇÃO RECUSADA: a escrita no acervo está "
                + "desligada. Ligue correcao-cache.reforco-terminologia-aplicar-habilitado=true "
                + "para autorizar. O ensaio continua disponível.");
            return publicar(raizCache, inicioMs, bloqueado);
        }
        Acumulador acc = new Acumulador(
            aplicar ? ModoReforcoTerminologia.APLICADO : ModoReforcoTerminologia.ENSAIO);
        if (!Files.isDirectory(raizCache)) {
            acc.falhas++;
            log.error("Pasta de cache não encontrada: {}", raizCache);
            return publicar(raizCache, inicioMs, acc);
        }
        CacheManutencaoService.Sessao sessao =
            aplicar ? cacheService.iniciarSessao(raizCache, OPERACAO) : null;
        try {
            for (Path arquivo : cacheService.listarCachesDaObra(raizCache, contextoFallback)) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                processarArquivo(arquivo, sessao, contextoFallback, acc);
            }
        } catch (IOException e) {
            acc.falhas++;
            log.error("Falha ao varrer a pasta de cache {}", raizCache, e);
        }
        return publicar(raizCache, inicioMs, acc);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha a execução publicando o resultado em log, telemetria e relatório
     * persistido. Uma operação que reescreve o acervo e não aparece em lugar nenhum deixa o
     * processo CEGO — não dá para comparar execuções, nem saber quando um termo parou de aparecer.
     *
     * <p>INVARIANTES DO DOMÍNIO: o ENSAIO também é publicado, e rotulado como tal. Publicar só a
     * aplicação esconderia justamente a medição que serve para decidir; omitir o rótulo faria um
     * ensaio ser lido depois como se o acervo tivesse mudado. {@code itensCorrigidos} é ZERO em
     * ensaio, porque nada foi corrigido — o que ele mediria está em {@code itensDetectados}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a porta absorve erro de I/O; o resultado é devolvido ao
     * chamador de qualquer forma.
     */
    private ResultadoReforcoTerminologia publicar(Path raizCache, long inicioMs, Acumulador acc) {
        ResultadoReforcoTerminologia r = acc.resultado();
        String rotulo = switch (r.modo()) {
            case APLICADO -> "Reforço de Terminologia";
            case ENSAIO -> "Reforço de Terminologia (ensaio)";
            // A telemetria é histórico: uma recusa arquivada como "(ensaio)" faria a série
            // temporal afirmar que alguém mediu o acervo quando ninguém abriu um arquivo.
            case RECUSADO_POR_CONFIG -> "Reforço de Terminologia (RECUSADO)";
        };
        log.info("[{}] status={} arquivos={} alterados={} pulados={} falhas={} falas={} restaurações={}",
            rotulo, r.status(), r.arquivosAnalisados(), r.arquivosAlterados(),
            r.arquivosNaoVerificaveis(), r.falhas(), r.falasAlteradas(), r.totalRestauracoes());
        r.porFrequencia().forEach((termo, n) -> log.info("[{}]   {}x {}", rotulo, n, termo));

        telemetria.registrar(rotulo,
            "status=" + r.status() + "; pulados=" + r.arquivosNaoVerificaveis()
                + "; falhas=" + r.falhas(),
            OPERACAO.replace('-', '_'), raizCache,
            System.currentTimeMillis() - inicioMs,
            r.arquivosAnalisados(), r.falasAlteradas(),
            r.aplicado() ? r.falasAlteradas() : 0,
            relatorioDe(r, raizCache));
        return r;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o relatório legível que fica no disco, com o contador POR TERMO
     * — que é a razão de esta operação existir e o que separa "o modelo acertou" de "o enforcer
     * consertou".
     * <p>INVARIANTES DO DOMÍNIO: o modo (ensaio/aplicado) vem ANTES dos números; termos ordenados
     * do mais restaurado para o menos, com empate por nome, para o arquivo ser comparável entre
     * execuções.
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; sem I/O.
     */
    private static String relatorioDe(ResultadoReforcoTerminologia r, Path raizCache) {
        StringBuilder sb = new StringBuilder();
        sb.append("REFORÇO DE TERMINOLOGIA SOBRE CACHE JÁ GRAVADO\n")
            .append("==============================================\n")
            .append("Modo: ").append(r.rotuloModo())
            .append('\n')
            .append("Pasta: ").append(raizCache.toAbsolutePath()).append('\n')
            .append("Status: ").append(r.status()).append('\n')
            .append("Arquivos analisados: ").append(r.arquivosAnalisados()).append('\n')
            .append("Arquivos alterados: ").append(r.arquivosAlterados()).append('\n')
            .append("Arquivos pulados (obra não verificável): ").append(r.arquivosNaoVerificaveis())
            .append('\n')
            .append("Falhas: ").append(r.falhas()).append('\n')
            .append("Falas alteradas: ").append(r.falasAlteradas()).append('\n')
            .append("Restaurações: ").append(r.totalRestauracoes()).append('\n')
            .append("\nPor termo canônico:\n");
        if (r.restauracoesPorTermo().isEmpty()) {
            sb.append("  (nenhuma)\n");
        } else {
            r.porFrequencia().forEach((termo, n) -> sb.append("  ").append(n).append("x  ")
                .append(termo).append('\n'));
        }
        return sb.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforça a terminologia de um arquivo sob a lore que a guarda aprovou
     * para ele.
     *
     * <p>INVARIANTES DO DOMÍNIO: a guarda obra×contexto roda antes de qualquer leitura de fala;
     * obra sem mapa de terminologia sai cedo; a escrita acontece UMA vez por arquivo, e só
     * quando houve mudança e o operador autorizou.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: contabiliza e audita o arquivo; o original permanece.
     */
    private void processarArquivo(
        Path arquivo, CacheManutencaoService.Sessao sessao, String contextoFallback, Acumulador acc
    ) {
        acc.arquivosAnalisados++;
        try {
            CacheManutencaoService.DocumentoEditavel doc = cacheService.carregar(arquivo);
            ContextoDoCache contexto = contextoService.ativarSnapshot(doc, contextoFallback);
            if (!contexto.identidadeVerificavel()) {
                // Cache legado, em pasta que nenhuma lore reconhece, com a obra escolhida à mão:
                // nem o carimbo nem o caminho sustentam a lore. A guarda deixa passar de propósito
                // (falhar fechado ali pararia obra sem vocabulário declarado), mas reescrever
                // terminologia sobre um palpite é justamente o dano que a guarda existe para
                // impedir — só que em lote e sobre trabalho já pronto.
                acc.arquivosNaoVerificaveis++;
                log.warn("[ PULADO ] Identidade da obra não verificável (cache sem carimbo em pasta "
                    + "não reconhecida): {} — terminologia NÃO aplicada.", arquivo);
                return;
            }
            Map<String, String> mapa = contexto.contexto().correcoesTerminologia();
            // Pares inconfundíveis vêm da MESMA obra resolvida pelo carimbo, nunca da lore ativa
            // global: este fluxo percorre obra por obra e a lore ativa pode ser outra (ou nenhuma).
            Set<List<String>> pares = contexto.contexto().paresInconfundiveis();
            if (mapa.isEmpty() && pares.isEmpty()) {
                return;
            }
            // Tudo deste arquivo fica PENDENTE até a gravação voltar. Creditar antes seria
            // publicar números que podem nunca chegar ao disco: medido, um salvarAtomico que
            // lançava deixava o relatório anunciando "Beam Saber 2x, Axis 1x" com o arquivo
            // byte a byte intacto — e três linhas de auditoria TERMINOLOGIA_REFORCADA para
            // falas que não mudaram. Em ensaio não há gravação, então o pendente é confirmado
            // no fim do arquivo: é isso que mantém o ensaio uma previsão fiel.
            Pendente pendente = new Pendente();
            for (JsonNode no : doc.entradas()) {
                if (no instanceof ObjectNode entrada) {
                    reforcarEntrada(entrada, mapa, pares, arquivo, doc, contexto.id(), acc.aplicar, pendente);
                }
            }
            if (pendente.falasAlteradas > 0 && acc.aplicar) {
                cacheService.salvarAtomico(doc, sessao);
                acc.arquivosAlterados++;
            }
            acc.confirmar(pendente);
            // A auditoria é o DATASET: cada linha afirma um fato sobre o acervo. Publicar
            // TERMINOLOGIA_REFORCADA num ensaio faria o histórico atestar uma aplicação que nunca
            // aconteceu — o arquivo continua com a forma-ruim e o JSONL diz que foi reforçado.
            // Corrompe justamente o registro que existe para se poder confiar no que foi feito.
            if (acc.aplicar) {
                pendente.auditorias.forEach(auditoria::registrar);
            }
        } catch (Exception e) {
            acc.falhas++;
            log.error("Falha ao reforçar terminologia em {}: {}", arquivo, e.getMessage());
            auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
                Instant.now().toString(), OPERACAO, arquivo.toAbsolutePath().toString(), -1, null,
                contextoFallback, null, null, "FALHA_ARQUIVO", e.getMessage(), null, null, null,
                e.toString()));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reforça a terminologia de UMA fala, registrando no pendente do arquivo
     * o que o enforcer efetivamente trocou.
     *
     * <p>INVARIANTES DO DOMÍNIO: a contagem vem do PRÓPRIO enforcer
     * ({@link EnforcadorTermosLore#reforcarContando}), nunca de comparar o texto antes e depois.
     * Deduzir o número de fora é reimplementar a regra: a versão anterior comparava ocorrências do
     * canônico e não creditava nada quando a forma-ruim CONTÉM o canônico — {@code "Plee"→"Ple"},
     * {@code "Gouf Customizado"→"Gouf Custom"}, {@code "Gundam Alexandre"→"Gundam Alex"}, em três
     * obras do catálogo —, e o relatório saía vazio tendo reescrito o arquivo.
     *
     * <p>O texto novo só é gravado no nó quando {@code aplicar} é verdadeiro; em ensaio a contagem
     * é idêntica e o documento fica intocado, que é o que torna o ensaio previsão e não estimativa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fala sem original ou sem mudança não registra nada.
     */
    private void reforcarEntrada(
        ObjectNode entrada, Map<String, String> mapa, Set<List<String>> pares, Path arquivo,
        CacheManutencaoService.DocumentoEditavel doc, String contextoId, boolean aplicar,
        Pendente pendente
    ) {
        String original = ClassificadorEntradaCacheService.texto(entrada, "original");
        String antes = ClassificadorEntradaCacheService.texto(entrada, "traduzido");
        if (original.isBlank() || antes.isBlank()) {
            return;
        }
        EnforcadorTermosLore.Reforco reforco = enforcador.reforcarContando(original, antes, mapa);
        String depois = reforco.texto();
        // TROCA DE ENTIDADE: o mapa de terminologia conserta GRAFIA ("Plee" -> "Ple"); não alcança
        // um nome trocado por OUTRO nome válido da mesma obra. Medido no cache do Gundam ZZ em
        // 2026-08-03: das 16.491 falas, 22 têm troca de entidade — 11 "Nahel Argama" -> "Argama"
        // (naves diferentes), 10 "Zeta Gundam" -> "ZZ Gundam" (mechas diferentes) e 1
        // "Argama" -> "ZZ Gundam". Nenhuma delas é alcançada pelo mapa, porque a forma escrita
        // está correta — errada é a ENTIDADE. Antes disto, corrigi-las exigia retraduzir a obra
        // inteira; o reparo é determinístico e reaproveita a regra já usada no portão do LLM.
        String reparado = validador.repararTrocaDeEntidade(original, depois, pares);
        String termoRestaurado = null;
        if (reparado != null) {
            termoRestaurado = termoQueVoltou(depois, reparado, pares);
            depois = reparado;
        }
        if (depois.equals(antes)) {
            return;
        }
        pendente.falasAlteradas++;
        reforco.restauracoesPorTermo().forEach((termo, n) -> pendente.porTermo.merge(termo, n, Integer::sum));
        // O contador POR TERMO é a razão de a operação existir: sem creditar a entidade
        // restaurada, o relatório anuncia só os termos do mapa e cala sobre a maior parte das
        // falas que mudaram — o operador leria "2 termos" para 26 alterações.
        if (termoRestaurado != null) {
            pendente.porTermo.merge(termoRestaurado, 1, Integer::sum);
        }
        if (aplicar) {
            entrada.put("traduzido", depois);
        }
        pendente.auditorias.add(
            entradaAuditoria(arquivo, entrada, doc.proveniencia(), contextoId, antes, depois));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: descobre QUAL entidade o reparo restaurou, para o contador por termo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o termo restaurado é o que passou a existir no texto reparado
     * sem existir no anterior. Compara por ocorrência, não por posição — o reparo pode trocar
     * várias menções na mesma fala.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nenhum termo identificável devolve {@code null} e o
     * contador simplesmente não credita nada; a fala já foi contada em {@code falasAlteradas}.
     */
    private static String termoQueVoltou(String antes, String depois, Set<List<String>> pares) {
        for (List<String> par : pares) {
            if (par == null || par.size() != 2) {
                continue;
            }
            for (String termo : par) {
                if (depois.contains(termo) && !antes.contains(termo)) {
                    return termo;
                }
            }
        }
        return null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o registro de auditoria da troca, SEM publicá-lo. A publicação
     * espera a gravação: {@code TERMINOLOGIA_REFORCADA} tem de significar "chegou ao disco".
     * <p>INVARIANTES DO DOMÍNIO: antes/depois são exatamente os textos observados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; não escreve nada.
     */
    private EntradaAuditoriaCorrecaoCache entradaAuditoria(
        Path arquivo, ObjectNode entrada, ProvenienciaCache prov, String contextoId,
        String antes, String depois
    ) {
        return new EntradaAuditoriaCorrecaoCache(
            Instant.now().toString(), OPERACAO, arquivo.toAbsolutePath().toString(),
            entrada.path("indice").asInt(-1),
            ClassificadorEntradaCacheService.texto(entrada, "estilo"), contextoId,
            prov != null ? prov.contextoHash() : null, prov != null ? prov.modeloLlm() : null,
            "TERMINOLOGIA_REFORCADA", "mapa de terminologia da obra",
            ClassificadorEntradaCacheService.texto(entrada, "original"), antes, depois, null);
    }

    /**
     * O que UM arquivo produziu, ainda não publicado. Existe para que uma gravação que falha não
     * deixe números no relatório: sem esta separação, um {@code salvarAtomico} que lançava fazia o
     * relatório anunciar restaurações com o arquivo intacto no disco.
     */
    private static final class Pendente {
        private final Map<String, Integer> porTermo = new HashMap<>();
        private final List<EntradaAuditoriaCorrecaoCache> auditorias = new ArrayList<>();
        private int falasAlteradas;
    }

    /** Estado mutável restrito a uma execução síncrona na fila única. */
    private static final class Acumulador {
        private final ModoReforcoTerminologia modo;
        private final boolean aplicar;
        private final Map<String, Integer> porTermo = new HashMap<>();
        private int arquivosAnalisados;
        private int arquivosAlterados;
        private int falasAlteradas;
        private int arquivosNaoVerificaveis;
        private int falhas;

        Acumulador(ModoReforcoTerminologia modo) {
            this.modo = modo;
            // Só APLICADO escreve. Recusa e ensaio compartilham o comportamento de não tocar o
            // disco, e é justamente por isso que precisam de rótulos diferentes no relatório.
            this.aplicar = modo == ModoReforcoTerminologia.APLICADO;
        }

        /**
         * PROPÓSITO DE NEGÓCIO: incorpora ao total o que UM arquivo produziu, depois de ele ter
         * sido efetivamente gravado (ou de o ensaio ter terminado, onde não há o que gravar).
         * <p>INVARIANTES DO DOMÍNIO: chamado UMA vez por arquivo bem-sucedido; nunca no catch.
         * <p>COMPORTAMENTO EM CASO DE FALHA: pendente vazio não altera nada.
         */
        void confirmar(Pendente pendente) {
            falasAlteradas += pendente.falasAlteradas;
            pendente.porTermo.forEach((termo, n) -> porTermo.merge(termo, n, Integer::sum));
        }

        ResultadoReforcoTerminologia resultado() {
            return new ResultadoReforcoTerminologia(arquivosAnalisados, arquivosAlterados,
                falasAlteradas, porTermo, arquivosNaoVerificaveis, falhas, modo);
        }
    }
}
