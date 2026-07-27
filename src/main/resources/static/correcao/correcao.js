import { logNoConsole } from '../js/app.js';

export function initCorrecao() {
    const btnLimpar = document.getElementById('btn-limpar-cache');
    const btnScraping = document.getElementById('btn-scraping-google');
    const btnRevisarCache = document.getElementById('btn-revisar-cache');
    const btnTermoEnsaio = document.getElementById('btn-reforcar-terminologia-ensaio');
    const btnTermoAplicar = document.getElementById('btn-reforcar-terminologia-aplicar');

    /**
     * PROPÓSITO DE NEGÓCIO: envia aos três modos o mesmo alvo e o contexto de
     * fallback necessário para caches antigos sem proveniência.
     * INVARIANTES DO DOMÍNIO: contexto vazio nunca substitui a proveniência de
     * caches novos; o backend decide a lore arquivo por arquivo.
     * COMPORTAMENTO EM CASO DE FALHA: devolve objeto válido com strings vazias.
     */
    const montarRequisicao = () => ({
        entrada: document.getElementById('correcao-entrada')?.value.trim() || '',
        contextoId: document.getElementById('correcao-contexto')?.value || ''
    });

    /**
     * PROPÓSITO DE NEGÓCIO: evita cliques repetidos enquanto a manutenção aceita
     * ainda está na fila ou em execução.
     * INVARIANTES DO DOMÍNIO: somente os três botões operacionais são bloqueados.
     * COMPORTAMENTO EM CASO DE FALHA: reabilita os botões no bloco finally.
     */
    const acompanharConclusao = async () => {
        [btnLimpar, btnScraping, btnRevisarCache, btnTermoEnsaio, btnTermoAplicar]
            .filter(Boolean).forEach(btn => { btn.disabled = true; });
        try {
            for (;;) {
                const resposta = await fetch('/api/pipeline/status', { cache: 'no-store' });
                if (!resposta.ok) break;
                const dados = await resposta.json();
                if (dados.mensagem === 'livre') break;
                await new Promise(resolve => setTimeout(resolve, 1000));
            }
        } catch (erro) {
            logNoConsole('console-correcao', `Não foi possível acompanhar o estado da fila: ${erro.message}`, 'aviso');
        } finally {
            [btnLimpar, btnScraping, btnRevisarCache, btnTermoEnsaio, btnTermoAplicar]
                .filter(Boolean).forEach(btn => { btn.disabled = false; });
        }
    };

    /**
     * PROPÓSITO DE NEGÓCIO: dispara o reforço de terminologia sobre o cache já gravado.
     * INVARIANTES DO DOMÍNIO: ensaio e aplicação são ROTAS distintas, não um parâmetro — a ação
     * que reescreve o acervo não pode depender de um booleano invertido por engano. A aplicação
     * ainda pede confirmação explícita, porque age em lote sobre trabalho já pronto.
     * COMPORTAMENTO EM CASO DE FALHA: registra o erro no console da página e reabilita os botões.
     */
    const dispararReforco = async (aplicar) => {
        if (aplicar && !window.confirm(
            'Isto REESCREVE o cache já gravado, aplicando a terminologia oficial de cada obra.\n\n'
            + 'Há backup e escrita atômica por arquivo, mas rode o ENSAIO antes para ver o que muda.\n\n'
            + 'Aplicar agora?')) {
            return;
        }
        const body = montarRequisicao();
        const rota = aplicar ? '/api/reforcar-terminologia-aplicar' : '/api/reforcar-terminologia-ensaio';
        logNoConsole('console-correcao',
            aplicar ? 'Aplicando terminologia oficial ao cache...' : 'Ensaiando reforço de terminologia (nada será escrito)...',
            'info');
        if (body.entrada) logNoConsole('console-correcao', `Pasta de Cache: ${body.entrada}`, 'info');

        try {
            const res = await fetch(rota, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            if (!res.ok) {
                throw new Error(await res.text() || 'Erro no reforço de terminologia');
            }
            const dados = await res.json();
            logNoConsole('console-correcao', dados.mensagem || 'Aceito pela fila.', 'sucesso');
            await acompanharConclusao();
        } catch (erro) {
            logNoConsole('console-correcao', `Falha: ${erro.message}`, 'erro');
        }
    };

    if (btnTermoEnsaio) {
        btnTermoEnsaio.addEventListener('click', () => dispararReforco(false));
    }
    if (btnTermoAplicar) {
        btnTermoAplicar.addEventListener('click', () => dispararReforco(true));
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', async () => {
            const body = montarRequisicao();
            const entrada = body.entrada;
            logNoConsole('console-correcao', 'Disparando limpeza de cache de tradução...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');

            try {
                const res = await fetch('/api/corrigir-cache', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro ao limpar cache');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Limpeza aceita pela fila; acompanhe o resultado real abaixo.', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro na limpeza: ${err.message}`, 'erro');
            }
        });
    }

    if (btnScraping) {
        btnScraping.addEventListener('click', async () => {
            const body = montarRequisicao();
            const entrada = body.entrada;
            logNoConsole('console-correcao', 'Disparando corretor via Scraping Google Tradutor...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');

            try {
                const res = await fetch('/api/corrigir-scraping', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro no scraping de correção');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Processamento de raspagem de correção iniciado!', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro no scraping: ${err.message}`, 'erro');
            }
        });
    }

    if (btnRevisarCache) {
        btnRevisarCache.addEventListener('click', async () => {
            const body = montarRequisicao();
            const entrada = body.entrada;
            const contextoId = body.contextoId;
            logNoConsole('console-correcao', 'Disparando revisão de concordância PT-BR no cache...', 'info');
            if (entrada) logNoConsole('console-correcao', `Pasta de Cache: ${entrada}`, 'info');
            if (contextoId) logNoConsole('console-correcao', `Contexto: ${contextoId}`, 'info');

            try {
                const res = await fetch('/api/revisar-cache', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });

                if (!res.ok) {
                    const erro = await res.text();
                    throw new Error(erro || 'Erro na revisão de concordância do cache');
                }

                const data = await res.json();
                logNoConsole('console-correcao', 'Revisão de concordância do cache iniciada!', 'sucesso');
                if (data.mensagem) {
                    logNoConsole('console-correcao', data.mensagem, 'info');
                }
                await acompanharConclusao();
            } catch (err) {
                logNoConsole('console-correcao', `Erro na revisão do cache: ${err.message}`, 'erro');
            }
        });
    }
}
