package org.traducao.projeto.mapaProjeto.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.mapaProjeto.domain.exceptions.MapaProjetoException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GeradorMapaProjetoUseCase {

    private static final Logger log = LoggerFactory.getLogger(GeradorMapaProjetoUseCase.class);

    // Pastas que não representam arquitetura/código-fonte e por isso são podadas
    // do mapa: controle de versão/IDE, build/gradle/bin, e artefatos OPERACIONAIS
    // volumosos (cache, logs, relatorios, backups) que, se incluídos, dominavam o
    // mapa com centenas de entradas de saída de execução em vez de estrutura.
    private static final Set<String> PASTAS_IGNORAR = Set.of(
        ".git", ".venv", "__pycache__", ".idea", ".cursor", ".claude", "docs", "multiplexar",
        "legendas-traduzidas-ptbr", ".gradle", "build", "bin", "cache", "target", "node_modules",
        "relatorios", "logs", "backups"
    );

    // Prefixos de nomes de diretório/arquivo criados por testes (ex.: os
    // relatorios/junit-<n> gerados por @TempDir do JUnit) — podados onde quer
    // que apareçam, pois são resíduos de execução, não arquitetura.
    private static final List<String> PREFIXOS_IGNORAR = List.of("junit-");

    // Sufixos de arquivos temporários/parciais (escrita atômica, extração
    // interrompida) que não devem poluir a taxonomia do mapa.
    private static final List<String> SUFIXOS_IGNORAR = List.of(".tmp", ".part");

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um nome de pasta/arquivo deve ficar de fora
     * do mapa por ser infraestrutura, saída operacional ou resíduo de teste — e
     * não estrutura de código relevante para navegação por LLMs.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca poda fontes, testes, documentação ou
     * configuração de projeto; a decisão depende apenas do nome simples, não do
     * caminho completo, e cobre os resíduos {@code junit-*} onde quer que surjam.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro sem I/O; entrada nula lança
     * {@link NullPointerException} (nunca é chamado com nome nulo, pois
     * {@code getFileName()} de um filho listado é sempre presente).
     */
    static boolean deveIgnorar(String nome) {
        if (PASTAS_IGNORAR.contains(nome)) {
            return true;
        }
        for (String prefixo : PREFIXOS_IGNORAR) {
            if (nome.startsWith(prefixo)) {
                return true;
            }
        }
        for (String sufixo : SUFIXOS_IGNORAR) {
            if (nome.endsWith(sufixo)) {
                return true;
            }
        }
        return false;
    }

    // Conectores de árvore (estilo `tree` do Unix) — linhas alinhadas em fonte monoespaçada.
    private static final String RAMO = "├── ";
    private static final String RAMO_FINAL = "└── ";
    private static final String TRILHA = "│   ";
    private static final String TRILHA_VAZIA = "    ";
    private static final String LINHA = "================================================================================";
    private static final String SUBLINHA = "--------------------------------------------------------------------------------";

    // Pastas primeiro, depois arquivos; ambos em ordem alfabética (case-insensitive).
    private static final Comparator<Path> ORDEM_ARVORE =
        Comparator.comparingInt((Path p) -> Files.isDirectory(p) ? 0 : 1)
            .thenComparing(p -> p.getFileName().toString().toLowerCase());

    /** Resultado da geracao: relatorio completo, arvore em Markdown "GitHub" e nome do projeto. */
    public record ResultadoMapa(String relatorio, String arvoreGithub, String nomeProjeto) {}

    public ResultadoMapa executar(Path pastaRaiz) {
        log.info("Iniciando Gerador de Mapa do Projeto para: {}", pastaRaiz.toAbsolutePath());

        Path raizAbs = pastaRaiz.toAbsolutePath().normalize();
        String nomeProjeto = raizAbs.getFileName().toString();

        try {
            // 1. Árvore de diretórios do projeto inteiro (pastas ignoradas são podadas).
            List<String> arvore = new ArrayList<>();
            int[] contadores = new int[2]; // [0]=pastas, [1]=arquivos
            construirArvore(raizAbs, "", arvore, contadores);

            // 2. Taxonomia: todos os arquivos-fonte (.java/.py) com seus cabeçalhos.
            List<Path> fontes = coletarArquivosFonte(raizAbs);
            long totalJava = fontes.stream().filter(p -> p.toString().toLowerCase().endsWith(".java")).count();
            long totalPy = fontes.stream().filter(p -> p.toString().toLowerCase().endsWith(".py")).count();

            List<String> linhas = new ArrayList<>();

            // --- Cabeçalho / métricas ---
            linhas.add(LINHA);
            linhas.add(" MAPA ESTRUTURAL DO PROJETO - TRACKER ANIMES");
            linhas.add(LINHA);
            linhas.add(" Raiz do repositorio      : " + nomeProjeto);
            linhas.add(" Pastas mapeadas          : " + contadores[0]);
            linhas.add(" Arquivos (na arvore)     : " + contadores[1]);
            linhas.add(" Arquivos-fonte indexados : " + fontes.size() + "  (.java: " + totalJava + " | .py: " + totalPy + ")");
            linhas.add(" Memoria viva do projeto  : CEREBRO_IA.md (na raiz do repositorio)");
            linhas.add("");
            linhas.add(" Objetivo: mapa de contexto para LLMs navegarem os diretorios e");
            linhas.add(" atualizarem a documentacao oficial. Geracao estatica e automatica.");
            linhas.add(LINHA);
            linhas.add("");

            // --- Seção 1: árvore de diretórios ---
            linhas.add(SUBLINHA);
            linhas.add(" 1. ARVORE DE DIRETORIOS");
            linhas.add(SUBLINHA);
            linhas.add(nomeProjeto + "/");
            linhas.addAll(arvore);
            linhas.add("");

            // --- Seção 2: taxonomia dos arquivos-fonte ---
            linhas.add(SUBLINHA);
            linhas.add(" 2. TAXONOMIA DOS ARQUIVOS-FONTE (.java / .py)");
            linhas.add(SUBLINHA);
            linhas.add("");

            if (fontes.isEmpty()) {
                linhas.add("(Nenhum arquivo .java ou .py encontrado no repositorio.)");
            } else {
                linhas.addAll(montarTaxonomia(raizAbs, fontes));
            }

            linhas.add("");
            linhas.add(LINHA);
            linhas.add(" FIM DO MAPA");
            linhas.add(LINHA);

            // Grava em mapa_projeto.md
            Path destino = raizAbs.resolve("mapa_projeto.md");
            Files.write(destino, linhas);
            log.info("Mapa estrutural do projeto salvo com sucesso em: {}", destino);

            String relatorio = String.join(System.lineSeparator(), linhas);
            String arvoreGithub = montarArvoreGithub(nomeProjeto, arvore);
            return new ResultadoMapa(relatorio, arvoreGithub, nomeProjeto);

        } catch (IOException e) {
            log.error("Erro ao gerar o mapa do projeto: {}", e.getMessage(), e);
            throw new MapaProjetoException("Falha ao gerar o mapa do projeto em: " + pastaRaiz, e);
        }
    }

    /**
     * Renderiza a arvore em Markdown "estilo GitHub": um bloco de codigo que o
     * GitHub exibe em fonte monoespacada, identico a arvore de diretorios.
     */
    private String montarArvoreGithub(String nomeProjeto, List<String> arvore) {
        List<String> md = new ArrayList<>();
        md.add("# Estrutura do Projeto - " + nomeProjeto);
        md.add("");
        md.add("> Arvore de diretorios gerada automaticamente pelo Mapa do Projeto (KRONOS).");
        md.add("");
        md.add("```");
        md.add(nomeProjeto + "/");
        md.addAll(arvore);
        md.add("```");
        md.add("");
        return String.join(System.lineSeparator(), md);
    }

    /**
     * Percorre recursivamente a partir de {@code dir}, acumulando linhas de árvore
     * com conectores. Pastas ignoradas ({@link #PASTAS_IGNORAR}) não são exploradas.
     */
    private void construirArvore(Path dir, String prefixo, List<String> linhas, int[] contadores) {
        List<Path> filhos;
        try (Stream<Path> list = Files.list(dir)) {
            filhos = list
                .filter(p -> !deveIgnorar(p.getFileName().toString()))
                .sorted(ORDEM_ARVORE)
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Falha ao listar {}: {}", dir, e.getMessage());
            return;
        }

        for (int i = 0; i < filhos.size(); i++) {
            Path filho = filhos.get(i);
            boolean ultimo = (i == filhos.size() - 1);
            String conector = ultimo ? RAMO_FINAL : RAMO;
            String nome = filho.getFileName().toString();

            if (Files.isDirectory(filho)) {
                linhas.add(prefixo + conector + nome + "/");
                contadores[0]++;
                construirArvore(filho, prefixo + (ultimo ? TRILHA_VAZIA : TRILHA), linhas, contadores);
            } else {
                linhas.add(prefixo + conector + nome);
                contadores[1]++;
            }
        }
    }

    /**
     * Coleta todos os arquivos {@code .java}/{@code .py} do repositório, podando
     * as pastas ignoradas para não descer em {@code .git}, {@code target}, etc.
     */
    private List<Path> coletarArquivosFonte(Path raiz) throws IOException {
        List<Path> resultado = new ArrayList<>();
        Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(raiz) && deveIgnorar(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String nome = file.getFileName().toString().toLowerCase();
                if (nome.endsWith(".java") || nome.endsWith(".py")) {
                    resultado.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Falha ao acessar {}: {}", file, exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(resultado);
        return resultado;
    }

    /**
     * Agrupa os arquivos por pasta (relativa à raiz) e produz, por arquivo, uma
     * entrada em linhas: nome do arquivo e a descrição extraída do cabeçalho.
     */
    private List<String> montarTaxonomia(Path raiz, List<Path> fontes) {
        // LinkedHashMap preserva a ordem alfabética já ordenada em `fontes`.
        Map<String, List<Path>> porPasta = new LinkedHashMap<>();
        for (Path arq : fontes) {
            Path relPasta = raiz.relativize(arq).getParent();
            String chave = relPasta == null ? "." : relPasta.toString().replace('\\', '/');
            porPasta.computeIfAbsent(chave, k -> new ArrayList<>()).add(arq);
        }

        List<String> linhas = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entrada : porPasta.entrySet()) {
            linhas.add("[PASTA] " + entrada.getKey() + "/");
            for (Path arq : entrada.getValue()) {
                linhas.add("  - " + arq.getFileName());
                String doc = extrairComentarioTopo(arq);
                if (doc != null && !doc.isBlank()) {
                    for (String linhaDoc : doc.split("\\R")) {
                        linhas.add("      " + linhaDoc.strip());
                    }
                } else {
                    linhas.add("      (sem cabecalho explicativo)");
                }
            }
            linhas.add("");
        }
        return linhas;
    }

    private String extrairComentarioTopo(Path arquivo) {
        String nomeLower = arquivo.getFileName().toString().toLowerCase();

        try {
            List<String> linhas = Files.lines(arquivo)
                .limit(40) // Analisa as primeiras 40 linhas
                .collect(Collectors.toList());

            if (nomeLower.endsWith(".py")) {
                return extrairComentarioPython(linhas);
            } else if (nomeLower.endsWith(".java")) {
                return extrairComentarioJava(linhas);
            }
        } catch (IOException e) {
            log.warn("Falha ao ler arquivo {}: {}", arquivo.getFileName(), e.getMessage());
        }
        return null;
    }

    private String extrairComentarioPython(List<String> linhas) {
        List<String> docstring = new ArrayList<>();
        boolean lendoDocstringTripla = false;

        for (String linha : linhas) {
            String linhaStrip = linha.strip();

            // Pula shebang ou encoding
            if (linhaStrip.startsWith("#!") || linhaStrip.contains("coding:")) {
                continue;
            }

            // Trata docstrings triplas
            if (linhaStrip.contains("\"\"\"") || linhaStrip.contains("'''")) {
                if (!lendoDocstringTripla) {
                    lendoDocstringTripla = true;
                    String conteudo = linhaStrip.replace("\"\"\"", "").replace("'''", "").strip();
                    if (!conteudo.isEmpty()) {
                        docstring.add(conteudo);
                    }
                } else {
                    lendoDocstringTripla = false;
                }
                continue;
            }

            if (lendoDocstringTripla) {
                docstring.add(linha.stripTrailing());
                continue;
            }

            // Trata comentários simples (#)
            if (linhaStrip.startsWith("#")) {
                docstring.add(linhaStrip.replaceAll("^#+\\s*", ""));
            } else if (linhaStrip.isEmpty() && docstring.isEmpty()) {
                continue;
            } else if (!linhaStrip.startsWith("#") && !docstring.isEmpty()) {
                // Código normal encontrado após comentários, encerra
                break;
            }
        }

        return docstring.isEmpty() ? null : String.join("\n", docstring).strip();
    }

    private String extrairComentarioJava(List<String> linhas) {
        List<String> docstring = new ArrayList<>();
        boolean lendoBloco = false;

        for (String linha : linhas) {
            String linhaStrip = linha.strip();

            // Detecta e trata Javadocs ou comentários de bloco (/* e /**)
            if (linhaStrip.contains("/*")) {
                lendoBloco = true;
                String conteudo = linhaStrip.substring(linhaStrip.indexOf("/*") + 2).replace("*", "").strip();
                if (!conteudo.isEmpty()) {
                    docstring.add(conteudo);
                }
                if (linhaStrip.contains("*/")) {
                    lendoBloco = false;
                    // Se fechou na mesma linha, limpa e tira o */
                    if (!docstring.isEmpty()) {
                        String ultima = docstring.removeLast();
                        ultima = ultima.replace("*/", "").strip();
                        if (!ultima.isEmpty()) docstring.add(ultima);
                    }
                }
                continue;
            }

            if (lendoBloco) {
                if (linhaStrip.contains("*/")) {
                    lendoBloco = false;
                    String conteudo = linhaStrip.replace("*/", "").replace("*", "").strip();
                    if (!conteudo.isEmpty()) {
                        docstring.add(conteudo);
                    }
                } else {
                    // Remove asteriscos típicos de Javadoc no início da linha
                    String limpa = linhaStrip.replaceAll("^\\*+\\s*", "");
                    docstring.add(limpa);
                }
                continue;
            }

            // Trata comentários simples (//)
            if (linhaStrip.startsWith("//")) {
                docstring.add(linhaStrip.substring(2).strip());
            } else if (linhaStrip.isEmpty() && docstring.isEmpty()) {
                continue;
            } else if (linhaStrip.startsWith("package ") || linhaStrip.startsWith("import ") ||
                       linhaStrip.contains("class ") || linhaStrip.contains("interface ") ||
                       linhaStrip.contains("record ")) {
                // Declarações de código após os comentários, encerra
                if (!docstring.isEmpty()) {
                    break;
                }
            }
        }

        return docstring.isEmpty() ? null : String.join("\n", docstring).strip();
    }
}
