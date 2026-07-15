package org.traducao.projeto.mapaProjeto.presentation;

import org.traducao.projeto.config.ExecucaoCli;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.traducao.projeto.mapaProjeto.application.GeradorMapaProjetoUseCase;
import org.traducao.projeto.mapaProjeto.application.MapeadorDiretorioUseCase;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.modo", havingValue = "MAPEAR")
public class MapaProjetoCLI implements ExecucaoCli {

    private final MapeadorDiretorioUseCase mapeadorDiretorioUseCase;
    private final GeradorMapaProjetoUseCase geradorMapaProjetoUseCase;
    private final TradutorProperties propriedades;

    public MapaProjetoCLI(MapeadorDiretorioUseCase mapeadorDiretorioUseCase,
                           GeradorMapaProjetoUseCase geradorMapaProjetoUseCase,
                           TradutorProperties propriedades) {
        this.mapeadorDiretorioUseCase = mapeadorDiretorioUseCase;
        this.geradorMapaProjetoUseCase = geradorMapaProjetoUseCase;
        this.propriedades = propriedades;
    }

    @Override
    public void executar() {
        String divisor = AnsiCores.colorir("=".repeat(80), AnsiCores.MAGENTA);
        System.out.println("\n" + divisor);
        System.out.println(AnsiCores.colorir("  >>> ESTEIRA DE MAPEAMENTO E ESTRUTURAÇÃO DO PROJETO <<<", AnsiCores.WHITE, true));
        System.out.println(divisor);
        System.out.flush();

        // Determina a raiz a ser mapeada
        String entrada = propriedades.diretorioEntrada();
        if (entrada == null || entrada.isBlank()) {
            entrada = System.getProperty("user.dir");
        }

        Path raiz = Path.of(entrada).toAbsolutePath();
        System.out.println(AnsiCores.colorir("  Mapeando a raiz: ", AnsiCores.CYAN) + AnsiCores.colorir(raiz.toString(), AnsiCores.WHITE));
        System.out.flush();

        try {
            // 1. Executa mapeamento e taxonomia de diretório
            System.out.println(AnsiCores.colorir("  [1/2] Gerando relatório de taxonomia de arquivos...", AnsiCores.CYAN));
            System.out.flush();
            mapeadorDiretorioUseCase.executar(raiz);

            // 2. Executa geração do mapa estrutural do projeto
            System.out.println(AnsiCores.colorir("  [2/2] Gerando mapa estrutural de Javadocs/Docstrings...", AnsiCores.CYAN));
            System.out.flush();
            geradorMapaProjetoUseCase.executar(raiz);

            System.out.println(divisor);
            System.out.println(AnsiCores.colorir("  [OK] Mapeamento concluído com sucesso!", AnsiCores.GREEN, true));
            System.out.println(AnsiCores.colorir("  -> Relatório salvo em: relatorio_diretorio_vps.txt", AnsiCores.WHITE));
            System.out.println(AnsiCores.colorir("  -> Mapa do projeto salvo em: mapa_projeto.md", AnsiCores.WHITE));
            System.out.println(divisor + "\n");
            System.out.flush();

        } catch (Exception e) {
            System.err.println(AnsiCores.colorir("  [ERRO] Ocorreu uma falha no mapeamento: " + e.getMessage(), AnsiCores.RED, true));
            e.printStackTrace();
        }
    }
}
