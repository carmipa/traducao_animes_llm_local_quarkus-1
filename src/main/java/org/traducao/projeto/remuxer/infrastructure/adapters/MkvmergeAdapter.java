package org.traducao.projeto.remuxer.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.util.ProcessoExternoUtil;
import org.traducao.projeto.remuxer.domain.RemuxTarefa;
import org.traducao.projeto.remuxer.domain.RemuxerException;
import org.traducao.projeto.remuxer.domain.SaidaRemuxJaExisteException;
import org.traducao.projeto.remuxer.infrastructure.config.RemuxerProperties;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * PROPÓSITO DE NEGÓCIO: executa o mkvmerge sem reencodar, valida o container
 * produzido e publica o MKV final sem arriscar um destino já existente.
 *
 * <p>INVARIANTES DO DOMÍNIO: mkvmerge escreve somente em temporário; o nome final
 * nasce por move sem {@code REPLACE_EXISTING}; falha/cancelamento remove
 * apenas o temporário desta execução.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: destino existente gera exceção específica;
 * timeout, interrupção, saída inválida ou I/O geram {@link RemuxerException} e
 * preservam qualquer MKV final anterior.
 */
@Component
public class MkvmergeAdapter {
    private static final Logger log = LoggerFactory.getLogger(MkvmergeAdapter.class);
    private static final Duration TIMEOUT_REMUX = Duration.ofMinutes(30);
    private static final Duration TIMEOUT_VALIDACAO = Duration.ofSeconds(30);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String mkvmergePath;
    private final ProcessoRunner processoRunner;

    /**
     * PROPÓSITO DE NEGÓCIO: cria o adaptador de produção com resolvedor do
     * executável e executor externo seguro.
     *
     * <p>INVARIANTES DO DOMÍNIO: caminho configurado válido tem prioridade sobre
     * caminhos padrão e PATH.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: localização final é validada no início
     * do lote com timeout.
     */
    @Inject
    public MkvmergeAdapter(RemuxerProperties properties) {
        this(properties, ProcessoExternoUtil::executar);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: oferece seam controlado para testar falhas do processo
     * sem depender de MKVToolNix instalado na suíte.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa exatamente as mesmas regras de arquivo do
     * construtor de produção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceções do runner seguem o tratamento
     * normal do adaptador.
     */
    MkvmergeAdapter(RemuxerProperties properties, ProcessoRunner processoRunner) {
        this.mkvmergePath = localizarMkvmerge(properties.resolverMkvmergePath());
        this.processoRunner = processoRunner;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza o executável configurado, padrão Windows ou
     * disponível no PATH.
     *
     * <p>INVARIANTES DO DOMÍNIO: caminho absoluto só é aceito quando existe.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code mkvmerge}; a validação de
     * infraestrutura produzirá erro didático se o PATH não resolver.
     */
    private String localizarMkvmerge(String caminhoConfigurado) {
        if (!"mkvmerge".equals(caminhoConfigurado) && Files.isRegularFile(Path.of(caminhoConfigurado))) {
            return caminhoConfigurado;
        }
        for (String caminho : List.of(
            "C:\\Program Files\\MKVToolNix\\mkvmerge.exe",
            "C:\\Program Files (x86)\\MKVToolNix\\mkvmerge.exe")) {
            if (Files.isRegularFile(Path.of(caminho))) {
                log.info("mkvmerge detectado em: {}", caminho);
                return caminho;
            }
        }
        return "mkvmerge";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que o MKVToolNix responde antes de planejar
     * um lote potencialmente grande.
     *
     * <p>INVARIANTES DO DOMÍNIO: validação possui timeout e código zero.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: interrupção é restaurada; demais falhas
     * viram {@link RemuxerException} sem tocar nas mídias.
     */
    public void validarInfraestrutura() {
        try {
            ProcessoExternoUtil.Resultado resultado = processoRunner.executar(
                List.of(mkvmergePath, "--version"), TIMEOUT_VALIDACAO, true);
            if (resultado.codigoSaida() != 0) {
                throw new RemuxerException("mkvmerge respondeu com código " + resultado.codigoSaida());
            }
            log.info("mkvmerge validado: {}", new String(resultado.stdout()).trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemuxerException("Validação do mkvmerge interrompida.", e);
        } catch (IOException | TimeoutException e) {
            throw new RemuxerException("Falha ao validar mkvmerge em " + mkvmergePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa remux com sincronismo zero e substituição das
     * legendas originais, preservando compatibilidade com chamadas existentes.
     *
     * <p>INVARIANTES DO DOMÍNIO: destino existente nunca é alterado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga exceção de domínio após limpar
     * temporário próprio.
     */
    public void executarRemux(RemuxTarefa tarefa) {
        executarRemux(tarefa, 0, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa remux com offset e substituição das legendas
     * originais, preservando API histórica.
     *
     * <p>INVARIANTES DO DOMÍNIO: PT-BR é marcada como padrão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: destino final anterior é preservado.
     */
    public void executarRemux(RemuxTarefa tarefa, long sincronismoMs) {
        executarRemux(tarefa, sincronismoMs, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: remuxa uma tarefa em temporário, opcionalmente
     * preservando legendas originais, e publica somente após inspeção do MKV.
     *
     * <p>INVARIANTES DO DOMÍNIO: saída final não preexistente; temporário fica na
     * mesma pasta; container final contém vídeo, áudio e legenda PT-BR.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: temporário é apagado no {@code finally};
     * destino preexistente recebe exceção específica e nunca é removido.
     */
    public void executarRemux(RemuxTarefa tarefa, long sincronismoMs, boolean preservarLegendasOriginais) {
        Path destinoFinal = tarefa.caminhoSaida().toAbsolutePath().normalize();
        if (Files.exists(destinoFinal)) {
            throw new SaidaRemuxJaExisteException("MKV final já existe e foi preservado: " + destinoFinal);
        }
        Path temporario = criarCaminhoTemporario(destinoFinal);
        try {
            List<Integer> legendasOriginais = preservarLegendasOriginais
                ? identificarIdsLegendas(tarefa.caminhoVideo()) : List.of();
            List<String> comando = montarComando(
                tarefa, temporario, sincronismoMs, preservarLegendasOriginais, legendasOriginais);
            ProcessoExternoUtil.Resultado resultado = processoRunner.executar(comando, TIMEOUT_REMUX, true);
            if (resultado.codigoSaida() != 0) {
                throw new RemuxerException("mkvmerge falhou com código " + resultado.codigoSaida()
                    + ": " + new String(resultado.stdout()));
            }
            validarSaidaTemporaria(temporario);
            publicarSemSobrescrever(temporario, destinoFinal);
        } catch (RemuxerException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemuxerException("Remux interrompido; saída parcial descartada: " + tarefa.nomeVideo(), e);
        } catch (TimeoutException e) {
            throw new RemuxerException("Tempo limite excedido; saída parcial descartada: " + tarefa.nomeVideo(), e);
        } catch (IOException e) {
            throw new RemuxerException("Falha de I/O no remux de " + tarefa.nomeVideo() + ": " + e.getMessage(), e);
        } finally {
            excluirTemporario(temporario);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta argumentos sem shell, com metadata regional
     * PT-BR genérica e opção explícita sobre legendas originais.
     *
     * <p>INVARIANTES DO DOMÍNIO: vídeo/áudio nunca são recodificados; offset atua
     * somente na faixa externa; nome de faixa não cita modelo de IA.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista é apenas construída; validação
     * ocorre na execução.
     */
    List<String> montarComando(RemuxTarefa tarefa, Path saidaTemporaria, long sincronismoMs,
                               boolean preservarLegendasOriginais, List<Integer> legendasOriginais) {
        List<String> comando = new ArrayList<>();
        comando.add(mkvmergePath);
        comando.add("-o");
        comando.add(saidaTemporaria.toString());
        if (!preservarLegendasOriginais) {
            comando.add("--no-subtitles");
        } else {
            for (Integer idLegenda : legendasOriginais) {
                comando.add("--default-track-flag");
                comando.add(idLegenda + ":0");
            }
        }
        comando.add(tarefa.caminhoVideo().toString());
        comando.add("--language");
        comando.add("0:pt-BR");
        comando.add("--track-name");
        comando.add("0:Português (Brasil)");
        comando.add("--default-track-flag");
        comando.add("0:1");
        if (sincronismoMs != 0) {
            comando.add("--sync");
            comando.add("0:" + sincronismoMs);
        }
        comando.add(tarefa.caminhoLegenda().toString());
        return comando;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: descobre as faixas de legenda já presentes no vídeo
     * para mantê-las como alternativas sem concorrer com a nova PT-BR padrão.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente IDs cujo tipo informado pelo mkvmerge é
     * {@code subtitles} são devolvidos; nenhuma faixa de áudio/vídeo é alterada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: identificação inválida ou código não
     * zero aborta o remux, preserva a origem e não publica destino incompleto.
     */
    private List<Integer> identificarIdsLegendas(Path video)
            throws IOException, InterruptedException, TimeoutException {
        ProcessoExternoUtil.Resultado identificacao = processoRunner.executar(
            List.of(mkvmergePath, "-J", video.toString()), TIMEOUT_VALIDACAO, true);
        if (identificacao.codigoSaida() != 0) {
            throw new RemuxerException("Não foi possível identificar as legendas originais de: " + video);
        }
        List<Integer> ids = new ArrayList<>();
        JsonNode raiz = JSON.readTree(identificacao.stdout());
        for (JsonNode faixa : raiz.path("tracks")) {
            if ("subtitles".equals(faixa.path("type").asText()) && faixa.has("id")) {
                ids.add(faixa.path("id").asInt());
            }
        }
        return List.copyOf(ids);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria nome temporário único mantendo extensão MKV para
     * o MKVToolNix reconhecer o container.
     *
     * <p>INVARIANTES DO DOMÍNIO: temporário fica ao lado do destino e termina em
     * {@code .mkv}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho é apenas calculado; I/O ocorrerá
     * na execução.
     */
    private Path criarCaminhoTemporario(Path destinoFinal) {
        String nome = destinoFinal.getFileName().toString();
        String base = nome.toLowerCase(Locale.ROOT).endsWith(".mkv")
            ? nome.substring(0, nome.length() - 4) : nome;
        return destinoFinal.resolveSibling(base + ".part-" + UUID.randomUUID() + ".mkv");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inspeciona o temporário com o próprio mkvmerge antes
     * de promovê-lo a arquivo final.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige arquivo não vazio, container reconhecido,
     * vídeo, áudio e uma legenda marcada como português/PT-BR.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança exceção e deixa o {@code finally}
     * remover o temporário.
     */
    private void validarSaidaTemporaria(Path temporario)
            throws IOException, InterruptedException, TimeoutException {
        if (!Files.isRegularFile(temporario) || Files.size(temporario) == 0) {
            throw new RemuxerException("mkvmerge não produziu um arquivo temporário válido: " + temporario);
        }
        ProcessoExternoUtil.Resultado identificacao = processoRunner.executar(
            List.of(mkvmergePath, "-J", temporario.toString()), TIMEOUT_VALIDACAO, true);
        if (identificacao.codigoSaida() != 0) {
            throw new RemuxerException("MKV temporário não pôde ser identificado: " + temporario);
        }
        JsonNode raiz = JSON.readTree(identificacao.stdout());
        boolean video = false;
        boolean audio = false;
        boolean legendaPt = false;
        for (JsonNode faixa : raiz.path("tracks")) {
            String tipo = faixa.path("type").asText();
            video |= "video".equals(tipo);
            audio |= "audio".equals(tipo);
            if ("subtitles".equals(tipo)) {
                JsonNode propriedades = faixa.path("properties");
                String idioma = propriedades.path("language").asText();
                String idiomaIetf = propriedades.path("language_ietf").asText();
                legendaPt |= "por".equalsIgnoreCase(idioma)
                    || idiomaIetf.toLowerCase(Locale.ROOT).startsWith("pt");
            }
        }
        if (!video || !audio || !legendaPt) {
            throw new RemuxerException("Validação do MKV temporário falhou: vídeo=" + video
                + ", áudio=" + audio + ", legendaPT=" + legendaPt + ".");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: promove o temporário validado ao nome final sem
     * substituir arquivo que tenha surgido durante a execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: não usa {@code REPLACE_EXISTING}; o move simples
     * na mesma pasta preserva a semântica de falhar se o destino existir.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: colisão vira exceção específica e o
     * temporário é removido pelo chamador.
     */
    private void publicarSemSobrescrever(Path temporario, Path destinoFinal) throws IOException {
        try {
            Files.move(temporario, destinoFinal);
        } catch (FileAlreadyExistsException e) {
            throw new SaidaRemuxJaExisteException("Destino surgiu durante o remux e foi preservado: " + destinoFinal);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: elimina somente o parcial exclusivo desta execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca recebe nem remove o destino final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso para limpeza manual sem
     * mascarar a falha original.
     */
    private void excluirTemporario(Path temporario) {
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException e) {
            log.warn("Não foi possível remover parcial exclusivo {}: {}", temporario, e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: abstrai a fronteira com processos externos para testes
     * determinísticos de timeout, falha e publicação.
     *
     * <p>INVARIANTES DO DOMÍNIO: devolve código e streams completos do processo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga I/O, interrupção ou timeout ao
     * adaptador.
     */
    @FunctionalInterface
    interface ProcessoRunner {
        ProcessoExternoUtil.Resultado executar(List<String> comando, Duration timeout, boolean mesclarErro)
            throws IOException, InterruptedException, TimeoutException;
    }
}
