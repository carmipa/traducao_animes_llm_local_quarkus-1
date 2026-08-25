package org.traducao.projeto.revisaoConcordancia.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ContagemCorretor;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: revisa a CONCORDÂNCIA DE GÊNERO de legendas que só existem em português
 * (o {@code .ass} PT-BR, sem inglês e sem cache), aplicando o corretor determinístico. Atende o
 * relato do Paulo de ver erros de gênero sobrando no fim dos animes: é o motor do menu "Revisão
 * de Concordância", que roda separado da revisão de lore e da tradução.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só correções determinísticas de gênero inequívoco (via {@link CorretorConcordanciaGeneroService});
 *       não usa inglês nem LLM.</li>
 *   <li><b>Estilo musical é veto ABSOLUTO.</b> Música e karaokê pertencem à fatia
 *       {@code traducaoKaraoke}, que sabe lidar com KFX, camadas e tempo por sílaba. Aqui não é
 *       "não consigo revisar", é "não é meu trabalho" — a mesma regra de escopo que
 *       {@code traducao.SeletorEventosTraduziveis} e {@code raspagemRevisao.FiltroAuditoriaLinha}
 *       já aplicam. Medido em 2026-08-16 no 86, ANTES desta guarda existir: esta tela via
 *       <b>22.568 de 26.524</b> eventos na Part 1 (85,1%) e <b>49.458 de 53.175</b> na Part 2
 *       (93,0%) — quase tudo sílaba solta de karaokê, e a tela mexe em GÊNERO, que é onde a
 *       heurística mais erra.</li>
 *   <li>NUNCA sobrescreve sem backup: cada arquivo alterado é copiado para subpasta timestampada
 *       antes de regravar; {@code aplicar=false} é dry-run (não escreve).</li>
 *   <li>Só reescreve arquivos que mudaram; eventos sem texto passam intactos; a estrutura do
 *       documento é preservada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Pasta inexistente devolve resultado zerado; erro por arquivo é logado e o arquivo é pulado;
 * falha ao criar backup aborta a gravação daquele arquivo (preserva o original).
 */
@Service
public class RevisarConcordanciaUseCase {

    private static final Logger log = LoggerFactory.getLogger(RevisarConcordanciaUseCase.class);
    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final String PASTA_BACKUP = "backup_revisao_concordancia";

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final CorretorConcordanciaGeneroService corretor;

    /**
     * O segundo corretor da tela, e ele fecha a classe que o primeiro nunca veria: o substantivo
     * escrito como forma verbal ({@code a milicia}, {@code de noticias}, {@code em orbita}).
     * Entrou em 23/08/2026, depois de o Macross II sair com 12,5% das falas defeituosas e esta
     * tela devolver, com razao, "NADA A CORRIGIR" — porque o defeito nao era de genero.
     */
    private final CorretorAcentoQueColideComVerboService corretorAcento;

    /**
     * O TERCEIRO corretor: os padrões curados para o acento que o revisor gramatical tem regra e
     * não dispara — {@code "Isso e tudo"}, {@code "Você esta falando"}. Medido em 24/08/2026:
     * 1.008 ocorrências no acervo, amostra lida uma a uma.
     */
    private final CorretorAcentoPorPadraoService corretorPadrao;

    /**
     * O QUARTO: o acento da palavra que o dicionário REJEITA ({@code territorio}, {@code serao}),
     * pelo corretor de produção, com os nomes próprios da própria fala como intocáveis.
     */
    private final CorretorAcentoDeDicionarioNaFalaService corretorDicionario;
    private final CorretorCaractereForaDoPortuguesService corretorCaractere;

    private final TelemetriaService telemetriaService;

    /**
     * O MESMO juiz de estilo musical que as outras telas usam. É peer ({@code legenda.domain}), e
     * consultá-lo é o oposto de reescrever a regra aqui: a forma do nome musical tem UM dono desde
     * 07/08/2026, e uma segunda lista divergiria em silêncio no dia em que um estilo novo entrasse.
     */
    private final PoliticaEstiloMusical politicaEstiloMusical;

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a fala é música/karaokê — e portanto NÃO é trabalho desta
     * tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: pergunta ao dono da regra ({@link PoliticaEstiloMusical}), que é
     * peer, em vez de reimplementar a forma do nome musical. Uma segunda lista aqui divergiria em
     * silêncio no dia em que um estilo novo entrasse no acervo — e o sinal disso só apareceria numa
     * legenda estragada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: estilo {@code null} devolve {@code false} e a fala segue
     * para o corretor, como sempre seguiu — a guarda não inventa veto onde não há nome de estilo.
     */
    private boolean eMusica(EventoLegenda evento) {
        return evento.estilo() != null && politicaEstiloMusical.estiloIgnorado(evento.estilo());
    }

    public RevisarConcordanciaUseCase(
        LeitorLegendaAss leitor, EscritorLegendaAss escritor,
        CorretorConcordanciaGeneroService corretor,
        CorretorAcentoQueColideComVerboService corretorAcento,
        CorretorAcentoPorPadraoService corretorPadrao,
        CorretorAcentoDeDicionarioNaFalaService corretorDicionario,
        CorretorCaractereForaDoPortuguesService corretorCaractere,
        TelemetriaService telemetriaService,
        PoliticaEstiloMusical politicaEstiloMusical) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.corretor = corretor;
        this.corretorAcento = corretorAcento;
        this.corretorPadrao = corretorPadrao;
        this.corretorDicionario = corretorDicionario;
        this.corretorCaractere = corretorCaractere;
        this.telemetriaService = telemetriaService;
        this.politicaEstiloMusical = politicaEstiloMusical;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: revisa a concordância de gênero de todos os {@code .ass}/{@code .ssa}
     * de uma pasta PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: com {@code aplicar=false} nada é gravado; com {@code true} cada
     * arquivo alterado é copiado para backup antes de ser regravado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente devolve zero; erro por arquivo é pulado.
     *
     * @param pasta pasta com as legendas PT-BR
     * @param aplicar false = dry-run (só relatório); true = grava com backup
     * @return {@link ResultadoConcordancia} com contagens e backups
     */
    public ResultadoConcordancia revisarPasta(Path pasta, boolean aplicar) {
        long inicioMs = System.currentTimeMillis();
        if (pasta == null || !Files.isDirectory(pasta)) {
            return new ResultadoConcordancia(0, 0, 0, List.of(), aplicar);
        }
        List<Path> arquivos;
        try (Stream<Path> stream = Files.walk(pasta)) {
            arquivos = stream.filter(Files::isRegularFile).filter(this::temExtensaoSuportada).sorted().toList();
        } catch (IOException e) {
            log.warn("Falha ao varrer {} para revisão de concordância: {}", pasta, e.getMessage());
            return new ResultadoConcordancia(0, 0, 0, List.of(), aplicar);
        }

        int analisados = 0;
        int alterados = 0;
        int falasCorrigidas = 0;
        int foraDoAlcance = 0;
        // Contados SEPARADO por corretor: um numero so nao diria de onde veio o ganho, e a ordem
        // permanente deste projeto e contador do que agiu E do que se absteve.
        int porGeneroTotal = 0;
        int porAcentoTotal = 0;
        // Um placar por corretor, zerado a CADA passada: o total de uma pasta nao pode vazar para
        // a proxima, senao o relatorio de uma obra pequena herda o numero da anterior.
        // Quatro posicoes: agiu, absteve-se, falhou e NANOS. O relogio entrou em 24/08/2026,
        // quando uma passada sobre seis arquivos levou cinco minutos e o unico jeito de saber
        // qual elo gastava o tempo era desmontar a cadeia na mao.
        long[] placarGenero = new long[4];
        long[] placarPos = new long[4];
        long[] placarPadrao = new long[4];
        long[] placarDicionario = new long[4];
        long[] placarCaractere = new long[4];

        // As guardas do elo do dicionario contam o que BARRARAM tendo o que corrigir. Zerar aqui
        // e obrigatorio: contador de servico vivo herda o placar da passada anterior, e o numero
        // da pasta de agora sairia somado com o da pasta de antes.
        corretorDicionario.zerarPlacarDasGuardas();
        List<Path> backups = new ArrayList<>();

        for (Path arquivo : arquivos) {
            if (eParcial(arquivo)) {
                foraDoAlcance++;
                imprimir(AnsiCores.DIM + "  [Ignorado] " + arquivo.getFileName()
                    + " (.parcial — tradução incompleta, não é entrega)" + AnsiCores.RESET);
                continue;
            }
            analisados++;
            try {
                DocumentoLegenda documento = leitor.ler(arquivo);
                List<EventoLegenda> novos = new ArrayList<>(documento.eventos().size());
                int corrigidasArq = 0;

                // AQUECIMENTO, uma vez por arquivo. Sem ele o elo do dicionario custava 80 ms POR
                // FALA — um processo externo para cada fala que trouxesse uma palavra inedita —
                // contra 0,04 ms dos outros tres. Nao muda uma virgula do resultado: aquecer so
                // povoa a memoria do dicionario. So o relogio muda.
                //
                // O universo aquecido e o mesmo que sera corrigido: musica e fala sem texto ficam
                // de fora, porque perguntar por elas gastaria o processo externo com palavra que
                // ninguem vai consultar.
                List<String> falasDoArquivo = documento.eventos().stream()
                    .filter(e -> e.temTexto() && !eMusica(e))
                    .map(EventoLegenda::texto)
                    .toList();
                if (!corretorDicionario.aquecerCom(falasDoArquivo)) {
                    log.warn("Dicionario nao respondeu ao aquecer {} — a passada continua, mas o "
                        + "elo do dicionario fica lento e pode nao verificar nada.",
                        arquivo.getFileName());
                }
                for (EventoLegenda evento : documento.eventos()) {
                    if (!evento.temTexto() || eMusica(evento) || eComentario(evento)) {
                        novos.add(evento);
                        continue;
                    }
                    // QUATRO corretores em cadeia. A ordem importa, e esta e a razao de cada
                    // posicao: genero primeiro, porque decide por determinante e nao se importa
                    // com acento; depois os tres de acento, do mais informado (POS tagger) para o
                    // mais mecanico (dicionario), para que quem sabe mais tenha a primeira palavra
                    // sobre a mesma fala.
                    //
                    // Cada elo passa pelo `aplicar`, que CONTA: quantas mudou, quantas deixou
                    // intactas e quantas explodiram. Fala que lanca excecao nao some — ela e
                    // contada e a passada segue, porque erro engolido em silencio some junto com
                    // a fala e ninguem descobre.
                    String antes = evento.texto();
                    // O CARACTERE VEM PRIMEIRO, e a ordem aqui e causal, nao estetica: o espaco
                    // invisivel quebra a fronteira de palavra e o macron desfigura a forma que o
                    // dicionario e o POS tagger procuram. Limpar depois deles seria limpar tarde —
                    // os quatro elos seguintes ja teriam olhado para uma palavra que nao existe.
                    String limpo = aplicar(placarCaractere, "caractere fora do portugues", antes,
                        corretorCaractere::corrigir, arquivo);
                    String porGenero = aplicar(placarGenero, "genero", limpo, corretor::corrigir, arquivo);
                    String comPos = aplicar(placarPos, "acento por POS tagger", porGenero, corretorAcento::corrigir, arquivo);
                    String comPadrao = aplicar(placarPadrao, "acento por padrao", comPos, corretorPadrao::corrigir, arquivo);
                    String depois = aplicar(placarDicionario, "acento por dicionario", comPadrao,
                        corretorDicionario::corrigir, arquivo);
                    if (!porGenero.equals(limpo)) {
                        porGeneroTotal++;
                    }
                    if (!depois.equals(porGenero)) {
                        porAcentoTotal++;
                    }
                    if (!depois.equals(antes)) {
                        corrigidasArq++;
                        novos.add(evento.comTexto(depois));
                    } else {
                        novos.add(evento);
                    }
                }
                if (corrigidasArq == 0) {
                    imprimir(AnsiCores.DIM + "  [OK]       " + arquivo.getFileName()
                        + " (concordancia conforme)" + AnsiCores.RESET);
                    continue;
                }
                if (aplicar) {
                    // As contagens sobem DEPOIS da gravação, não antes. Falha ao criar backup ou
                    // ao escrever cai no catch e o arquivo fica intacto — se os contadores já
                    // tivessem subido, o banner final diria "corrigidas" para uma fala que
                    // continua errada no disco. Estado-alvo não é estado-atual.
                    Path backup = criarBackup(pasta, arquivo);
                    DocumentoLegenda revisado = new DocumentoLegenda(
                        documento.cabecalho(), novos, documento.quebraDeLinha(), documento.comBom());
                    escritor.escrever(arquivo, revisado);
                    backups.add(backup);
                    falasCorrigidas += corrigidasArq;
                    alterados++;
                    imprimir(AnsiCores.GREEN + "  [Revisado] " + arquivo.getFileName()
                        + " (" + corrigidasArq + " fala(s) corrigida(s))" + AnsiCores.RESET);
                    imprimir(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
                } else {
                    falasCorrigidas += corrigidasArq;
                    alterados++;
                    imprimir(AnsiCores.YELLOW + "  [Pendente] " + arquivo.getFileName()
                        + " (" + corrigidasArq + " fala(s) mudariam — nada gravado, simulacao)"
                        + AnsiCores.RESET);
                }
            } catch (IOException | RuntimeException e) {
                imprimir(AnsiCores.RED + "  [Erro]     " + arquivo.getFileName()
                    + " — " + e.getMessage() + AnsiCores.RESET);
                log.warn("Revisão de concordância pulou {} por erro: {}", arquivo, e.getMessage());
            }
        }
        // A disponibilidade do revisor viaja com o resultado porque ZERO correcao de acento tem
        // duas causas possiveis — texto limpo, ou motor que nao subiu — e as duas nao podem sair
        // com a mesma cara na tela (invariante 12).
        // O placar de cada elo da cadeia, para o relatorio dizer DE ONDE veio cada correcao e
        // — o que importa mais — para "nao achou nada" nunca sair igual a "nem rodou".
        List<ContagemCorretor> porCorretor = List.of(
            contagem("caractere fora do portugues", placarCaractere,
                corretorCaractere.disponivel()),
            contagem("genero (determinante)", placarGenero, true),
            contagem("acento por POS tagger", placarPos, corretorAcento.disponivel()),
            contagem("acento por padrao", placarPadrao, true),
            contagem("acento por dicionario", placarDicionario, corretorDicionario.disponivel()));

        long falhasTotais = placarCaractere[2] + placarGenero[2] + placarPos[2]
            + placarPadrao[2] + placarDicionario[2];
        if (falhasTotais > 0) {
            log.warn("Revisao de concordancia em {}: {} fala(s) lancaram excecao e ficaram como "
                + "estavam. O detalhe por corretor esta no banner e na telemetria.",
                pasta.getFileName(), falhasTotais);
        }
        log.info("Revisao de concordancia em {} — {} arquivo(s), {} fala(s) corrigida(s). Placar: {}",
            pasta.getFileName(), analisados, falasCorrigidas,
            porCorretor.stream().map(c -> c.nome() + "=" + c.agiu()).toList());

        ResultadoConcordancia resultado = new ResultadoConcordancia(
            analisados, alterados, falasCorrigidas, List.copyOf(backups), aplicar, foraDoAlcance,
            porGeneroTotal, porAcentoTotal,
            corretorAcento.disponivel(), corretorAcento.motivoDaIndisponibilidade(),
            porCorretor,
            corretorDicionario.barradasPorMaiuscula(), corretorDicionario.barradasPorIdioma());
        telemetriaService.registrarOperacao(new OperacaoTelemetria(
            "Revisão de Concordância",
            // O DETALHE carrega o placar por corretor ate o CSV. Sem ele, o dataset guarda um
            // total unico e a analise de amanha nao consegue responder de qual elo veio o ganho —
            // que e exatamente a pergunta que se faz quando um corretor novo entra na cadeia.
            "Pasta: " + pasta.getFileName() + (aplicar ? " (aplicado)" : " (simulado)")
                + " | " + porCorretor.stream()
                    .map(c -> c.nome() + "=" + (c.disponivel() ? String.valueOf(c.agiu()) : "NV")
                        + (c.falhou() > 0 ? "/" + c.falhou() + "f" : ""))
                    .collect(java.util.stream.Collectors.joining(" ")),
            System.currentTimeMillis() - inicioMs,
            analisados,
            falasCorrigidas,
            falasCorrigidas,
            Instant.now().toString()));
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escreve UMA linha por arquivo no console do operador, seguindo a
     * mesma regra de cores das telas 3.1 e 3.2 — verde só para o que foi gravado, amarelo para o
     * que ficou por resolver, cinza para o arquivo que realmente não tinha nada, vermelho só
     * para erro.
     *
     * <h2>Por que uma linha por ARQUIVO, e nunca por fala</h2>
     * Medido na 3.2 em 18/08/2026: imprimindo por fala, 10.013 das 10.563 linhas do console
     * (94,8%) eram "auditando" e "limpo" — ruído que empurra o sinal para fora da tela. Aqui o
     * risco seria pior: esta fatia percorre 332.545 falas do acervo, e nenhuma delas interessa ao
     * operador enquanto não muda nada.
     *
     * <p>INVARIANTES DO DOMÍNIO: escreve em {@code System.out} (que o console web captura) e no
     * log do servidor SEM os códigos ANSI — cor é para o olho, não para o arquivo de log.
     *
     * <h2>Por que aqui NÃO há batimento de progresso, e isso está medido</h2>
     * A 3.2 precisou de um: {@code System.out} alimenta o mesmo log que o
     * {@code pode-compilar.ps1} lê, e ele trata <b>90s sem linha</b> como "job terminado" —
     * liberando uma compilação que dispara live reload e mata o job em curso (o acidente do
     * episódio Stink Bomb, 14/08/2026). Lá o silêncio é real porque cada fala pode virar uma
     * chamada ao LLM com retentativa.
     *
     * <p>Aqui não há LLM nem rede. Medido em 18/08/2026 sobre o acervo inteiro: a pasta mais
     * lenta ({@code DanMachi}, 1.614.552 eventos em 260 arquivos) levou <b>12,8s no total</b> —
     * 7x abaixo do limite do portão. E como sai uma linha por ARQUIVO, o silêncio máximo é o de
     * um arquivo só, na casa das dezenas de milissegundos. Copiar o batimento da 3.2 para cá
     * seria guarda que não protege nada — e guarda que não protege nada ensina a ignorar as que
     * protegem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; console indisponível não pode derrubar uma
     * revisão em andamento.
     */
    private void imprimir(String linhaColorida) {
        System.out.println(linhaColorida);
        log.info(linhaColorida.replaceAll((char) 27 + "\\[[0-9;]*m", "").strip());
    }

    private Path criarBackup(Path pasta, Path arquivo) throws IOException {
        Path dirBackup = pasta.resolve(PASTA_BACKUP);
        Files.createDirectories(dirBackup);
        String nome = arquivo.getFileName().toString();
        Path backup = dirBackup.resolve(nome + "." + LocalDateTime.now().format(TS) + ".bak");
        Files.copy(arquivo, backup, StandardCopyOption.COPY_ATTRIBUTES);
        return backup;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se o arquivo é uma tradução INCOMPLETA, que o pipeline isolou com
     * o sufixo {@code .parcial} — e que por isso não é entrega e não se reescreve.
     *
     * <h2>Por que o sufixo é critério confiável aqui</h2>
     * Ele não é convenção de quem nomeia arquivo à mão: quem o coloca é o próprio pipeline
     * ({@code traducao.ResolvedorSaidaLegenda}), quando a tradução terminou com pendências. O
     * arquivo existe justamente para NÃO ser confundido com o PT-BR final usado no remux.
     *
     * <p>A constante é local de propósito. O dono da regra é outra FATIA, e fatia não fala com
     * fatia: duplicar oito caracteres conscientemente custa menos que a dependência — é a mesma
     * escolha que o projeto já faz em toda parte. Se o pipeline mudar o sufixo, esta linha muda
     * junto.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara em minúsculas, porque o disco do Windows não distingue
     * caixa e o acervo tem as duas formas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome vazio devolve {@code false} — na dúvida o arquivo
     * segue para a revisão normal, que tem backup e veto de música.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: aplica UM corretor a UMA fala e registra o desfecho — mudou, deixou
     * intacta, ou explodiu.
     *
     * <h2>Por que a exceção é CONTADA e não propagada</h2>
     * Um corretor que lança numa fala derrubaria o arquivo inteiro, e o operador veria "falha
     * inesperada" sem saber em qual das 2.000 falas. Aqui a fala volta como estava, o contador
     * sobe, o log nomeia o arquivo e o corretor, e a passada continua. <b>O que não pode
     * acontecer é a fala sumir em silêncio</b> — por isso o número aparece no relatório final,
     * e não só no log.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca devolve {@code null}; em qualquer falha devolve o texto
     * recebido, byte a byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conta em {@code placar[2]}, loga em nível de aviso com
     * arquivo e corretor, e devolve o texto original.
     *
     * @param placar   três posições: agiu, absteve-se, falhou
     * @param nome     o corretor, para o log
     * @param texto    a fala como está
     * @param correcao o corretor a chamar
     * @param arquivo  de onde veio a fala, para o log dizer ONDE
     */
    private String aplicar(long[] placar, String nome, String texto,
                           java.util.function.Function<String, Optional<String>> correcao,
                           Path arquivo) {
        long comeco = System.nanoTime();
        try {
            Optional<String> nova = correcao.apply(texto);
            if (nova.isPresent() && !nova.get().equals(texto)) {
                placar[0]++;
                if (log.isDebugEnabled()) {
                    log.debug("[{}] {} :: '{}' -> '{}'", nome, arquivo.getFileName(), texto, nova.get());
                }
                return nova.get();
            }
            placar[1]++;
            return texto;
        } catch (RuntimeException e) {
            placar[2]++;
            log.warn("Corretor '{}' falhou numa fala de {} — a fala fica como esta e a passada "
                + "continua. Fala: '{}'. Causa: {}", nome, arquivo.getFileName(), texto, e.toString());
            return texto;
        } finally {
            // O relógio soma em TODAS as saídas, inclusive na que lança: elo que falha devagar
            // também custa tempo, e escondê-lo faria o placar mentir justo no caso ruim.
            placar[3] += System.nanoTime() - comeco;
        }
    }

    /**
     * Monta a contagem de um elo a partir do placar cru.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida; o placar vem do próprio laço.
     */
    private static ContagemCorretor contagem(String nome, long[] placar, boolean disponivel) {
        return new ContagemCorretor(nome, (int) placar[0], (int) placar[1], (int) placar[2],
            disponivel, placar[3]);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se a linha é {@code Comment} — a que o reprodutor NÃO mostra.
     *
     * <h2>A fala que encontrou este buraco</h2>
     * Em 25/08/2026, lendo os 98 pares que o acervo produziria antes de gravar, apareceu
     * {@code place → placê} em duas falas do DanMachi. A procura pelo texto no acervo não achou
     * nada — porque a fala não é {@code Dialogue}, é {@code Comment}:
     *
     * <pre>
     *   Comment: ...,English,...,{\bord0\shad0\an5\pos(940,1020)...}Is the place where you aim,
     * </pre>
     *
     * <h2>Por que não se corrige o que ninguém lê</h2>
     * <ul>
     *   <li><b>Ganho zero.</b> A régua deste projeto é <i>atrapalha ler?</i>. Linha que o
     *       reprodutor não desenha não atrapalha ler coisa nenhuma.</li>
     *   <li><b>Risco real.</b> {@code Comment} é onde o Kara Templater guarda o MOLDE do karaokê;
     *       mexer no texto do molde mexe no que ele gera. O acervo tem 1.937 dessas linhas, e
     *       {@code Flower} (136), {@code EVERYTHING} (91) e {@code English} (79) não são pegas
     *       pelo veto de música — passavam direto.</li>
     *   <li><b>Número inflado.</b> Contar como "fala corrigida" o que ninguém vê faz a tela
     *       reportar trabalho que não existe.</li>
     * </ul>
     *
     * <p>O escritor devolve o tipo como veio ({@code prefixo = tipo + ": " + ...}), então nunca
     * houve o risco pior — {@code Comment} virar {@code Dialogue} e aparecer na tela. Isso foi
     * conferido antes de escrever esta guarda, e não presumido.
     *
     * <p>INVARIANTES DO DOMÍNIO: só olha o tipo da linha; não decide por estilo nem por conteúdo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tipo nulo NÃO é comentário — na dúvida a fala segue para
     * a correção, que é o comportamento de sempre.
     */
    private boolean eComentario(EventoLegenda evento) {
        return "Comment".equals(evento.tipoLinha());
    }

    private boolean eParcial(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().contains(".parcial.");
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }
}
