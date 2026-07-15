package org.traducao.projeto.mapaProjeto.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MapeadorDiretorioUseCase {

    private static final Logger log = LoggerFactory.getLogger(MapeadorDiretorioUseCase.class);
    private static final String ARQUIVO_SAIDA = "relatorio_diretorio_vps.txt";

    public void executar(Path pastaRaiz) {
        log.info("Iniciando Mapeador de Diretório para a raiz: {}", pastaRaiz.toAbsolutePath());
        
        List<ArquivoMapeado> arquivos = escanearArquivos(pastaRaiz);
        List<String> linhas = new ArrayList<>();

        String nomeProjeto = pastaRaiz.getFileName().toString();
        String dataHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Cabeçalho Técnico
        linhas.add("=".repeat(100));
        linhas.add("RELATÓRIO DE MAPEAMENTO COMPLETO DE DIRETÓRIO E TAXONOMIA DE REPOSITÓRIO");
        linhas.add("Projeto: " + nomeProjeto);
        linhas.add("Data/Hora de Execução: " + dataHora);
        linhas.add("=".repeat(100) + "\n");

        // PARTE 1: CAMINHO ABSOLUTO COMPLETO NO SISTEMA LOCAL
        linhas.add("PARTE 1: MAPEAMENTO DE DIRETÓRIO LOCAL (CAMINHO ABSOLUTO COMPLETO)");
        linhas.add("-".repeat(100));

        if (arquivos.isEmpty()) {
            linhas.add("[AVISO] Nenhum arquivo encontrado no diretório atual.");
        } else {
            // Ordena pelo caminho absoluto
            List<ArquivoMapeado> ordenadoPorCaminho = new ArrayList<>(arquivos);
            ordenadoPorCaminho.sort(Comparator.comparing(a -> a.caminhoAbsoluto));

            for (ArquivoMapeado arq : ordenadoPorCaminho) {
                linhas.add(String.format(" Arquivo: %-35s | Caminho: %s", arq.nome, arq.caminhoAbsoluto));
            }
        }

        linhas.add("\n" + "=".repeat(100) + "\n");

        // PARTE 2: QUADRO DE ALOCAÇÃO NA ESTRUTURA DO REPOSITÓRIO ONLINE
        linhas.add("PARTE 2: ESTRUTURA DE DESTINO SUGERIDA PARA O REPOSITÓRIO ONLINE");
        linhas.add("-".repeat(100));

        if (!arquivos.isEmpty()) {
            linhas.add(String.format(" %-35s | %-45s", "NOME DO ARQUIVO", "PASTA ALVO NO REPOSITÓRIO ONLINE"));
            linhas.add(String.format(" %-35s | %-45s", "-".repeat(35), "-".repeat(45)));

            // Ordena pelo nome do arquivo
            List<ArquivoMapeado> ordenadoPorNome = new ArrayList<>(arquivos);
            ordenadoPorNome.sort(Comparator.comparing(a -> a.nome));

            for (ArquivoMapeado arq : ordenadoPorNome) {
                String target = sugerirPastaDestino(arq);
                linhas.add(String.format("  %-34s | %-44s", arq.nome, target));
            }
        }

        linhas.add("\n" + "=".repeat(100));
        linhas.add("FIM DO RELATÓRIO - PRONTO PARA DOWNLOAD DA JANELA DE CONTEXTO");

        // Salvar em arquivo
        Path destino = pastaRaiz.resolve(ARQUIVO_SAIDA);
        try {
            Files.write(destino, linhas);
            log.info("Relatório de mapeamento de diretório salvo com sucesso em: {}", destino.toAbsolutePath());
        } catch (IOException e) {
            log.error("Erro ao salvar relatório de mapeamento em {}: {}", destino, e.getMessage(), e);
        }
    }

    private List<ArquivoMapeado> escanearArquivos(Path raiz) {
        List<ArquivoMapeado> lista = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(raiz)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> !p.toAbsolutePath().toString().contains(Path.of(".git").toString())) // Ignora .git
                .forEach(p -> {
                    String nome = p.getFileName().toString();
                    String caminhoAbs = p.toAbsolutePath().toString();
                    String caminhoRel = raiz.toAbsolutePath().relativize(p.toAbsolutePath()).toString();
                    lista.add(new ArquivoMapeado(nome, caminhoAbs, caminhoRel));
                });
        } catch (IOException e) {
            log.error("Erro ao varrer diretório de mapeamento: {}", e.getMessage(), e);
        }
        return lista;
    }

    private String sugerirPastaDestino(ArquivoMapeado arq) {
        String nomeLower = arq.nome.toLowerCase();
        int dotIdx = nomeLower.lastIndexOf('.');
        String ext = dotIdx > 0 ? nomeLower.substring(dotIdx) : "";

        if (".srt".equals(ext)) {
            return "legendas/traduzidas/";
        }
        if (".md".equals(ext)) {
            return "obsidian_vault/notas/";
        }
        if (".json".equals(ext)) {
            return "config/json_data/";
        }
        if (".log".equals(ext) || ".txt".equals(ext)) {
            return "telemetria/logs_erro/";
        }
        if (".py".equals(ext) || ".sh".equals(ext)) {
            if (arq.caminhoRelativo.toLowerCase().contains("mistral")) {
                return "src/mistral_engine/";
            } else {
                return "src/scripts_automacao/";
            }
        }

        // Outras extensões: tenta manter a estrutura de pastas relativas
        Path pRel = Path.of(arq.caminhoRelativo);
        Path pai = pRel.getParent();
        if (pai != null) {
            // Corrige separadores de caminho no Windows para barras normais (para consistencia no online)
            return pai.toString().replace('\\', '/') + "/";
        }
        
        return "raiz/";
    }

    private static class ArquivoMapeado {
        final String nome;
        final String caminhoAbsoluto;
        final String caminhoRelativo;

        ArquivoMapeado(String nome, String caminhoAbsoluto, String caminhoRelativo) {
            this.nome = nome;
            this.caminhoAbsoluto = caminhoAbsoluto;
            this.caminhoRelativo = caminhoRelativo;
        }
    }
}
