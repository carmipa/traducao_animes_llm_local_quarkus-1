package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.contexto.domain.SnapshotContexto;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final CorrecaoCacheAuditoria auditoria;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as quatro peças da operação — leitura/escrita de cache,
     * resolução da lore do arquivo, o reforço determinístico e a auditoria.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas. Não há telemetria injetada:
     * a fatia alcançaria {@code telemetria.TelemetriaService} por dependência direta, e essa
     * aresta é dívida congelada que a FASE 2 do Plano-Mestre remove com uma porta. Abrir uma
     * aresta nova aqui reprovaria — corretamente — o {@code FronteiraCorretorCacheArchTest}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     */
    public ReforcarTerminologiaCacheUseCase(
        CacheManutencaoService cacheService,
        ContextoManutencaoCacheService contextoService,
        EnforcadorTermosLore enforcador,
        CorrecaoCacheAuditoria auditoria
    ) {
        this.cacheService = cacheService;
        this.contextoService = contextoService;
        this.enforcador = enforcador;
        this.auditoria = auditoria;
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
        Acumulador acc = new Acumulador(aplicar);
        if (!Files.isDirectory(raizCache)) {
            acc.falhas++;
            log.error("Pasta de cache não encontrada: {}", raizCache);
            return acc.resultado();
        }
        CacheManutencaoService.Sessao sessao =
            aplicar ? cacheService.iniciarSessao(raizCache, OPERACAO) : null;
        try {
            for (Path arquivo : cacheService.listarCachesTraducaoBase(raizCache)) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                processarArquivo(arquivo, sessao, contextoFallback, acc);
            }
        } catch (IOException e) {
            acc.falhas++;
            log.error("Falha ao varrer a pasta de cache {}", raizCache, e);
        }
        return acc.resultado();
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
            SnapshotContexto contexto = contextoService.ativarSnapshot(doc, contextoFallback);
            Map<String, String> mapa = contexto.correcoesTerminologia();
            if (mapa.isEmpty()) {
                return;
            }
            // Do mais LONGO para o mais curto: é o que permite atribuir cada trecho ao termo
            // certo quando um canônico contém o outro ("Beam Sabers" contém "Beam Saber").
            List<String> canonicos = mapa.values().stream()
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
            int alteradas = 0;
            for (JsonNode no : doc.entradas()) {
                if (no instanceof ObjectNode entrada
                    && reforcarEntrada(entrada, mapa, canonicos, arquivo, doc, contexto.id(), acc)) {
                    alteradas++;
                }
            }
            if (alteradas > 0 && acc.aplicar) {
                cacheService.salvarAtomico(doc, sessao);
                acc.arquivosAlterados++;
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
     * PROPÓSITO DE NEGÓCIO: reforça a terminologia de UMA fala e credita o que foi restaurado.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto novo só é gravado no nó quando {@code aplicar} é
     * verdadeiro — em ensaio a contagem é idêntica e o documento fica intocado, que é o que
     * torna o ensaio uma previsão fiel e não uma aproximação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fala sem original ou sem mudança devolve {@code false}.
     *
     * @return {@code true} quando a fala mudou
     */
    private boolean reforcarEntrada(
        ObjectNode entrada, Map<String, String> mapa, List<String> canonicos, Path arquivo,
        CacheManutencaoService.DocumentoEditavel doc, String contextoId, Acumulador acc
    ) {
        String original = ClassificadorEntradaCacheService.texto(entrada, "original");
        String antes = ClassificadorEntradaCacheService.texto(entrada, "traduzido");
        if (original.isBlank() || antes.isBlank()) {
            return false;
        }
        String depois = enforcador.reforcar(original, antes, mapa);
        if (depois.equals(antes)) {
            return false;
        }
        creditarTermos(antes, depois, canonicos, acc);
        acc.falasAlteradas++;
        if (acc.aplicar) {
            entrada.put("traduzido", depois);
        }
        auditar(arquivo, entrada, doc.proveniencia(), contextoId, antes, depois);
        return true;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: credita a cada termo canônico quantas vezes ele passou a aparecer.
     *
     * <p>INVARIANTES DO DOMÍNIO: conta OCORRÊNCIA LITERAL do canônico, sem fronteira de palavra.
     * É deliberado: o enforcer insere o canônico literalmente, então a diferença antes→depois já
     * é exatamente o número de inserções. Recompilar aqui a regex de fronteira do enforcer seria
     * uma segunda cópia da regra — e cópia de regra foi o que produziu a divergência que esta
     * fase acabou de eliminar. Contar o literal não depende de conhecer a regra de fronteira.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: delta zero ou negativo não credita nada.
     */
    private void creditarTermos(String antes, String depois, List<String> canonicos, Acumulador acc) {
        Map<String, Integer> naSaida = contarPorCanonico(depois, canonicos);
        Map<String, Integer> naEntrada = contarPorCanonico(antes, canonicos);
        for (Map.Entry<String, Integer> par : naSaida.entrySet()) {
            int delta = par.getValue() - naEntrada.getOrDefault(par.getKey(), 0);
            if (delta > 0) {
                acc.porTermo.merge(par.getKey(), delta, Integer::sum);
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta cada termo canônico no texto atribuindo cada trecho a UM
     * único termo — o mais longo que o cobre.
     *
     * <p>INVARIANTES DO DOMÍNIO: os canônicos chegam ordenados do mais LONGO para o mais curto e
     * cada ocorrência encontrada é consumida (trocada por um sentinela) antes de o próximo termo
     * ser procurado. Sem isso o contador mente por construção: "Beam Sabers" contém literalmente
     * "Beam Saber", e os mapas de lore são cheios de pares singular/plural — "Mobile Suit"/
     * "Mobile Suits", "Newtype"/"Newtypes" —, então o singular seria creditado toda vez que o
     * plural fosse restaurado. Num relatório cujo propósito é ser um número honesto, inflar o
     * termo mais comum é o pior defeito possível.
     *
     * <p>O sentinela é {@code U+0000}, que não ocorre em legenda nem em termo de lore; ele só
     * existe dentro deste método e nunca chega ao texto gravado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo nulo/vazio é ignorado; texto sem nenhum canônico
     * devolve mapa vazio.
     */
    private static Map<String, Integer> contarPorCanonico(String texto, List<String> canonicos) {
        Map<String, Integer> contagem = new HashMap<>();
        String restante = texto;
        for (String canonico : canonicos) {
            int total = contarLiteral(restante, canonico);
            if (total > 0) {
                contagem.put(canonico, total);
                restante = restante.replace(canonico, " ");
            }
        }
        return contagem;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta ocorrências literais e NÃO sobrepostas de um trecho.
     * <p>INVARIANTES DO DOMÍNIO: avança o cursor pelo comprimento do alvo; sem regex.
     * <p>COMPORTAMENTO EM CASO DE FALHA: alvo vazio devolveria laço infinito, por isso o
     * chamador o descarta antes.
     */
    private static int contarLiteral(String texto, String alvo) {
        int total = 0;
        for (int i = texto.indexOf(alvo); i >= 0; i = texto.indexOf(alvo, i + alvo.length())) {
            total++;
        }
        return total;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra a troca com a proveniência do arquivo, para o operador poder
     * conferir cada restauração — inclusive as do ensaio, que é onde ele decide se autoriza.
     * <p>INVARIANTES DO DOMÍNIO: antes/depois são exatamente os textos observados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: a auditoria absorve erro de I/O sem parar a varredura.
     */
    private void auditar(
        Path arquivo, ObjectNode entrada, ProvenienciaCache prov, String contextoId,
        String antes, String depois
    ) {
        auditoria.registrar(new EntradaAuditoriaCorrecaoCache(
            Instant.now().toString(), OPERACAO, arquivo.toAbsolutePath().toString(),
            entrada.path("indice").asInt(-1),
            ClassificadorEntradaCacheService.texto(entrada, "estilo"), contextoId,
            prov != null ? prov.contextoHash() : null, prov != null ? prov.modeloLlm() : null,
            "TERMINOLOGIA_REFORCADA", "mapa de terminologia da obra",
            ClassificadorEntradaCacheService.texto(entrada, "original"), antes, depois, null));
    }

    /** Estado mutável restrito a uma execução síncrona na fila única. */
    private static final class Acumulador {
        private final boolean aplicar;
        private final Map<String, Integer> porTermo = new HashMap<>();
        private int arquivosAnalisados;
        private int arquivosAlterados;
        private int falasAlteradas;
        private int falhas;

        Acumulador(boolean aplicar) {
            this.aplicar = aplicar;
        }

        ResultadoReforcoTerminologia resultado() {
            return new ResultadoReforcoTerminologia(arquivosAnalisados, arquivosAlterados,
                falasAlteradas, porTermo, falhas, aplicar);
        }
    }
}
