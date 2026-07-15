import { logNoConsole } from '../js/app.js';

export function initCorrecao() {
    const btnLimpar = document.getElementById('btn-limpar-cache');
    const btnScraping = document.getElementById('btn-scraping-google');
    const btnRevisarCache = document.getElementById('btn-revisar-cache');

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
        [btnLimpar, btnScraping, btnRevisarCache].filter(Boolean).forEach(btn => { btn.disabled = true; });
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
            [btnLimpar, btnScraping, btnRevisarCache].filter(Boolean).forEach(btn => { btn.disabled = false; });
        }
    };

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
