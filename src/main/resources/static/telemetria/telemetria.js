import { mostrarAlerta } from '../js/app.js';

let jvmCpuHistory = [];
let jvmHeapHistory = [];
let jvmLabelsHistory = [];

// Estado da tabela de Historico Operacional (busca + filtro + paginacao)
const ITENS_POR_PAGINA = 12;
let historicoCompleto = [];
let filtroOperacaoAtivo = 'todas';
let termoBuscaTabela = '';
let paginaAtualTabela = 1;

const CORES_ORIGEM = {
    LLM: '#8b5cf6',
    LORE: '#ec4899',
    GOOGLE: '#3b82f6',
    CACHE: '#06b6d4',
    SISTEMA: '#8b97ad'
};

const NOME_OPERACAO_LORE = 'Revisao de Lore (.ass LLM)';

// Configuração de tema dark global do Chart.js
function configurarTemaDarkChart() {
    if (typeof Chart === 'undefined') return;

    Chart.defaults.color = '#8b97ad';
    Chart.defaults.font.family = "'Outfit', 'Inter', sans-serif";
    Chart.defaults.font.size = 10;
    Chart.defaults.plugins.tooltip.backgroundColor = '#0c1422';
    Chart.defaults.plugins.tooltip.borderColor = 'rgba(255, 255, 255, 0.08)';
    Chart.defaults.plugins.tooltip.borderWidth = 1;
    Chart.defaults.plugins.tooltip.cornerRadius = 6;
    Chart.defaults.plugins.tooltip.titleColor = '#eef2f8';
    Chart.defaults.plugins.tooltip.bodyColor = '#8b97ad';
    Chart.defaults.plugins.tooltip.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.pointStyle = 'circle';
    Chart.defaults.plugins.legend.labels.color = '#8b97ad';
    Chart.defaults.scale.grid.color = 'rgba(255, 255, 255, 0.04)';
    Chart.defaults.scale.border = { display: false };
    Chart.defaults.scale.ticks.color = '#8b97ad';
}

export function initTelemetria() {
    configurarTemaDarkChart();

    const btnRefresh = document.getElementById('btn-refresh-telemetria');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', carregarDadosTelemetria);
    }

    const btnExportar = document.getElementById('btn-exportar-telemetria');
    if (btnExportar) {
        btnExportar.addEventListener('click', () => {
            window.open('/api/telemetria/exportar', '_blank');
        });
    }

    // Publica a telemetria sanitizada como dataset público no repositório Git
    // dedicado (kronos-anime-translation-telemetry-dataset): snapshot + commit + push.
    const btnPublicar = document.getElementById('btn-publicar-dataset');
    if (btnPublicar) {
        const rotulo = btnPublicar.querySelector('span:last-child');
        btnPublicar.addEventListener('click', async () => {
            btnPublicar.disabled = true;
            const textoOriginal = rotulo ? rotulo.textContent : '';
            if (rotulo) rotulo.textContent = 'Publicando...';
            try {
                const res = await fetch('/api/telemetria/publicar-dataset', { method: 'POST' });
                const dados = await res.json();
                if (res.ok && dados.pushOk) {
                    mostrarAlerta(dados.mensagem, 'sucesso');
                } else {
                    mostrarAlerta(dados.mensagem || 'Falha ao publicar o dataset.', res.ok ? 'aviso' : 'erro');
                }
            } catch (e) {
                mostrarAlerta('Erro de conexão ao publicar o dataset: ' + e.message, 'erro');
            } finally {
                btnPublicar.disabled = false;
                if (rotulo) rotulo.textContent = textoOriginal;
            }
        });
    }

    const campoBusca = document.getElementById('t-table-search');
    if (campoBusca) {
        campoBusca.addEventListener('input', () => {
            termoBuscaTabela = campoBusca.value.trim().toLowerCase();
            paginaAtualTabela = 1;
            renderizarTabelaHistorico();
        });
    }

    const btnPaginaAnterior = document.getElementById('t-page-prev');
    if (btnPaginaAnterior) {
        btnPaginaAnterior.addEventListener('click', () => {
            if (paginaAtualTabela > 1) {
                paginaAtualTabela -= 1;
                renderizarTabelaHistorico();
            }
        });
    }

    const btnPaginaProxima = document.getElementById('t-page-next');
    if (btnPaginaProxima) {
        btnPaginaProxima.addEventListener('click', () => {
            const totalPaginas = Math.max(1, Math.ceil(filtrarHistorico().length / ITENS_POR_PAGINA));
            if (paginaAtualTabela < totalPaginas) {
                paginaAtualTabela += 1;
                renderizarTabelaHistorico();
            }
        });
    }

    const btnFiltrarLore = document.getElementById('btn-filtrar-lore-historico');
    if (btnFiltrarLore) {
        btnFiltrarLore.addEventListener('click', () => {
            filtroOperacaoAtivo = NOME_OPERACAO_LORE;
            paginaAtualTabela = 1;
            const campoBusca = document.getElementById('t-table-search');
            if (campoBusca) campoBusca.value = '';
            termoBuscaTabela = '';
            renderizarChipsFiltro();
            renderizarTabelaHistorico();
            document.querySelector('.telemetry-table-card')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
    }

    // Atualizações em tempo real via SSE (o servidor emite a cada registro de
    // telemetria e num heartbeat de 5s com CPU/heap da JVM).
    iniciarStreamTelemetria();

    // Fallback: se o stream SSE cair/não conectar, mantém o polling de 10s.
    setInterval(() => {
        const panel = document.getElementById('panel-telemetria');
        if (!sseAtivo && panel && panel.classList.contains('active')) {
            carregarDadosTelemetria();
        }
    }, 10000);
}

let sseAtivo = false;

function iniciarStreamTelemetria() {
    if (typeof EventSource === 'undefined') return;
    try {
        const stream = new EventSource('/api/telemetria/stream');
        stream.addEventListener('telemetria', (evento) => {
            sseAtivo = true;
            const panel = document.getElementById('panel-telemetria');
            if (!panel || !panel.classList.contains('active')) return;
            try {
                aplicarDadosTelemetria(JSON.parse(evento.data), null);
            } catch (err) {
                console.error('Erro ao aplicar telemetria SSE:', err);
            }
        });
        stream.onerror = () => {
            // O navegador reconecta sozinho; enquanto isso o polling assume.
            sseAtivo = false;
        };
    } catch (err) {
        console.error('Não foi possível abrir o stream SSE de telemetria:', err);
        sseAtivo = false;
    }
}

async function carregarDadosTelemetria() {
    const tableBody = document.getElementById('telemetria-table-body');
    try {
        const inicio = performance.now();
        const res = await fetch('/api/telemetria');
        if (!res.ok) throw new Error('Não foi possível obter dados de telemetria');

        const data = await res.json();
        const latenciaMs = Math.round(performance.now() - inicio);
        aplicarDadosTelemetria(data, latenciaMs);
    } catch (err) {
        console.error('Erro ao atualizar telemetria:', err);
        if (tableBody && tableBody.children.length === 0) {
            renderizarLinhaVazia(tableBody, 'Nao foi possivel carregar a telemetria agora.');
        }
    }
}

function aplicarDadosTelemetria(data, latenciaMs) {
    const episodiosVal = document.getElementById('t-episodios');
    const linhasVal = document.getElementById('t-linhas');
    const tempoLinhaVal = document.getElementById('t-tempo-linha');
    const cacheHitsVal = document.getElementById('t-cache-hits');
    const cacheFilesVal = document.getElementById('t-cache-files');
    const cacheRateVal = document.getElementById('t-cache-rate');
    const cacheStatusVal = document.getElementById('t-cache-status');
    const cacheMeterVal = document.getElementById('t-cache-meter');
    const latenciaVal = document.getElementById('t-latencia');
    const cacheSavedVal = document.getElementById('t-cache-saved');
    const operacoesVal = document.getElementById('t-operacoes');
    const auditLineVal = document.getElementById('t-audit-line');
    const errosVal = document.getElementById('t-erros');
    const chamadasLlmVal = document.getElementById('t-chamadas-llm');
    const arquivosSanitizadosVal = document.getElementById('t-arquivos-sanitizados');

    const totalLinhas = Number(data.totalLinhas ?? 0);
        const cacheHits = Number(data.totalCacheHits ?? 0);
        const cacheRate = totalLinhas > 0 ? Math.min(100, (cacheHits / totalLinhas) * 100) : 0;
        const historico = Array.isArray(data.historicoOperacoes) ? data.historicoOperacoes : [];
        const statusCache = cacheRate >= 50 ? 'otimo' : cacheRate > 0 ? 'aquecendo' : 'estavel';

        // Atualiza os cards estatísticos
        if (episodiosVal) episodiosVal.textContent = formatarNumero(data.totalEpisodios ?? 0);
        if (linhasVal) linhasVal.textContent = formatarNumero(totalLinhas);
        if (tempoLinhaVal) tempoLinhaVal.textContent = `${data.tempoMedioPorLinhaMs ?? 0} ms`;
        if (cacheHitsVal) cacheHitsVal.textContent = formatarNumero(cacheHits);
        if (cacheFilesVal) cacheFilesVal.textContent = formatarNumero(data.cacheCount ?? 0);
        if (cacheRateVal) cacheRateVal.textContent = `${cacheRate.toFixed(1)}%`;
        if (cacheStatusVal) cacheStatusVal.textContent = statusCache;
        if (cacheMeterVal) cacheMeterVal.style.width = `${cacheRate}%`;
        // No caminho SSE não há latência de request para medir.
        if (latenciaVal && latenciaMs !== null && latenciaMs !== undefined) {
            latenciaVal.textContent = `${latenciaMs} ms`;
        }
        if (cacheSavedVal) cacheSavedVal.textContent = `${formatarNumero(cacheHits)} chamadas`;
        if (operacoesVal) operacoesVal.textContent = formatarNumero(historico.length);
        if (errosVal) errosVal.textContent = formatarNumero(data.totalErros ?? 0);
        if (arquivosSanitizadosVal) arquivosSanitizadosVal.textContent = formatarNumero(data.arquivosSanitizados ?? 0);
        if (chamadasLlmVal) {
            const totalChamadasLlm = historico.filter(op => op.origem === 'LLM').length;
            chamadasLlmVal.textContent = formatarNumero(totalChamadasLlm);
        }
        if (auditLineVal) {
            const hora = new Date().toLocaleTimeString('pt-BR', { timeZone: 'UTC' });
            auditLineVal.textContent = `[${hora} UTC] ${formatarNumero(historico.length)} operacoes, ${formatarNumero(totalLinhas)} falas e ${cacheRate.toFixed(1)}% de cache registrados.`;
        }

        // Atualiza estatísticas detalhadas do hardware JVM
        const cpuVal = numeroSeguro(data.jvmCpuUso ?? data.jvmCpu, 0);
        const thrVal = numeroSeguro(data.jvmThreadsAtivas ?? data.jvmThreads, 0);
        const heapUsadoBytes = numeroSeguro(data.jvmHeapUsadoBytes ?? data.heapUsed, 0);
        const heapMaxBytes = Math.max(1, numeroSeguro(data.jvmHeapMaxBytes ?? data.heapMax, 1));
        const heapUsedMb = heapUsadoBytes / 1024 / 1024;
        const heapMaxMb = heapMaxBytes / 1024 / 1024;
        const heapPercent = Math.min(100, Math.max(0, (heapUsadoBytes / heapMaxBytes) * 100));

        const cpuEl = document.getElementById('t-jvm-cpu');
        const thrEl = document.getElementById('t-jvm-threads');
        const heapEl = document.getElementById('t-jvm-heap');

        if (cpuEl) cpuEl.textContent = `${cpuVal.toFixed(1)}%`;
        if (thrEl) thrEl.textContent = formatarNumero(thrVal);
        if (heapEl) heapEl.textContent = `${Math.round(heapUsedMb)} MB / ${Math.round(heapMaxMb)} MB`;

        renderizarGraficosTelemetria(data, totalLinhas, cacheHits, cpuVal, heapPercent);
        renderizarRevisaoLore(data.revisaoLore);

        // Atualiza o Historico Operacional (busca + filtros + paginacao)
        historicoCompleto = historico;
        renderizarChipsFiltro();
        paginaAtualTabela = Math.min(paginaAtualTabela, Math.max(1, Math.ceil(filtrarHistorico().length / ITENS_POR_PAGINA)));
        renderizarTabelaHistorico();
}

function rotuloExibicaoOperacao(nome) {
    if (!nome) return 'Desconhecida';
    if (nome === NOME_OPERACAO_LORE || nome.includes('Revisao de Lore')) {
        return 'Revisão de Lore';
    }
    return nome;
}

function renderizarRevisaoLore(resumo) {
    const sessoesEl = document.getElementById('t-lore-sessoes');
    const arquivosEl = document.getElementById('t-lore-arquivos');
    const sinalizadasEl = document.getElementById('t-lore-sinalizadas');
    const corrigidasEl = document.getElementById('t-lore-corrigidas');
    const taxaEl = document.getElementById('t-lore-taxa');
    const hintEl = document.getElementById('t-lore-hint');
    const btnFiltrar = document.getElementById('btn-filtrar-lore-historico');

    const sessoes = Number(resumo?.totalSessoes ?? 0);
    const arquivos = Number(resumo?.totalArquivosProcessados ?? 0);
    const sinalizadas = Number(resumo?.totalFalasSinalizadas ?? 0);
    const corrigidas = Number(resumo?.totalFalasCorrigidas ?? 0);
    const taxa = resumo?.taxaCorrecaoPercent;

    if (sessoesEl) sessoesEl.textContent = formatarNumero(sessoes);
    if (arquivosEl) arquivosEl.textContent = formatarNumero(arquivos);
    if (sinalizadasEl) sinalizadasEl.textContent = formatarNumero(sinalizadas);
    if (corrigidasEl) corrigidasEl.textContent = formatarNumero(corrigidas);
    if (taxaEl) {
        taxaEl.textContent = taxa === null || taxa === undefined ? '—' : `${taxa}%`;
    }
    if (hintEl) {
        hintEl.textContent = sessoes > 0
            ? `${formatarNumero(sessoes)} sessão(ões) registrada(s) no histórico do pipeline.`
            : 'Nenhuma sessão de revisão de lore registrada ainda.';
    }
    if (btnFiltrar) {
        btnFiltrar.hidden = sessoes <= 0;
    }
}

function formatarNumero(valor) {
    return Number(valor ?? 0).toLocaleString('pt-BR');
}

function numeroSeguro(valor, padrao = 0) {
    const numero = Number(valor);
    return Number.isFinite(numero) ? numero : padrao;
}

// ---- Historico Operacional: filtros, busca, chips e paginacao ----

function filtrarHistorico() {
    return historicoCompleto.filter(op => {
        if (filtroOperacaoAtivo !== 'todas' && (op.nomeOperacao ?? '') !== filtroOperacaoAtivo) {
            return false;
        }
        if (!termoBuscaTabela) return true;
        const alvo = `${op.nomeOperacao ?? ''} ${op.detalheOperacao ?? ''}`.toLowerCase();
        return alvo.includes(termoBuscaTabela);
    });
}

function renderizarChipsFiltro() {
    const container = document.getElementById('t-filter-chips');
    if (!container) return;

    const contagemPorTipo = new Map();
    historicoCompleto.forEach(op => {
        const nome = op.nomeOperacao ?? 'Desconhecida';
        if (!contagemPorTipo.has(nome)) {
            contagemPorTipo.set(nome, { total: 0, origem: op.origem ?? 'SISTEMA' });
        }
        contagemPorTipo.get(nome).total += 1;
    });

    // Se o filtro ativo deixou de existir no dataset atual, volta para "Todas"
    if (filtroOperacaoAtivo !== 'todas' && !contagemPorTipo.has(filtroOperacaoAtivo)) {
        filtroOperacaoAtivo = 'todas';
    }

    const chips = [criarChip('todas', 'Todas', historicoCompleto.length, CORES_ORIGEM.SISTEMA)];
    contagemPorTipo.forEach((info, nome) => {
        chips.push(criarChip(nome, rotuloExibicaoOperacao(nome), info.total, CORES_ORIGEM[info.origem] ?? CORES_ORIGEM.SISTEMA));
    });

    container.innerHTML = '';
    chips.forEach(chip => container.appendChild(chip));
}

function criarChip(valor, label, contagem, cor) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'filter-chip' + (filtroOperacaoAtivo === valor ? ' active' : '');
    btn.dataset.filtro = valor;
    btn.innerHTML = `
        <span class="chip-dot" style="background:${cor}"></span>
        <span class="chip-label">${label}</span>
        <span class="chip-count">${formatarNumero(contagem)}</span>
    `;
    btn.addEventListener('click', () => {
        filtroOperacaoAtivo = valor;
        paginaAtualTabela = 1;
        renderizarChipsFiltro();
        renderizarTabelaHistorico();
    });
    return btn;
}

function classeDuracao(duracaoMs) {
    if (duracaoMs === null || duracaoMs === undefined) return '';
    if (duracaoMs < 5000) return 'duration-fast';
    if (duracaoMs < 60000) return 'duration-mid';
    return 'duration-slow';
}

function origemBadgeClass(origem) {
    switch (origem) {
        case 'LLM': return 'origin-llm';
        case 'LORE': return 'origin-lore';
        case 'GOOGLE': return 'origin-google';
        case 'CACHE': return 'origin-cache';
        default: return 'origin-sistema';
    }
}

function renderizarTabelaHistorico() {
    const tableBody = document.getElementById('telemetria-table-body');
    const contadorTexto = document.getElementById('t-table-count');
    const paginacaoInfo = document.getElementById('t-pagination-info');
    const btnAnterior = document.getElementById('t-page-prev');
    const btnProxima = document.getElementById('t-page-next');
    if (!tableBody) return;

    const filtrados = filtrarHistorico();
    const totalPaginas = Math.max(1, Math.ceil(filtrados.length / ITENS_POR_PAGINA));
    paginaAtualTabela = Math.min(Math.max(1, paginaAtualTabela), totalPaginas);

    const inicio = (paginaAtualTabela - 1) * ITENS_POR_PAGINA;
    const pagina = filtrados.slice(inicio, inicio + ITENS_POR_PAGINA);

    tableBody.innerHTML = '';
    if (pagina.length === 0) {
        renderizarLinhaVazia(tableBody, historicoCompleto.length === 0
            ? 'Aguardando dados de processamento...'
            : 'Nenhuma operacao corresponde a busca/filtro atual.');
    } else {
        pagina.forEach(op => {
            const row = document.createElement('tr');
            const origem = op.origem ?? 'SISTEMA';
            const cor = CORES_ORIGEM[origem] ?? CORES_ORIGEM.SISTEMA;

            // registradoEm chega em UTC (ISO-8601); o Date converte para o
            // fuso local do navegador na exibição.
            const tdData = document.createElement('td');
            tdData.className = 'op-datetime';
            tdData.textContent = op.registradoEm
                ? new Date(op.registradoEm).toLocaleString('pt-BR', { timeZone: 'UTC' }) + ' UTC'
                : '-';

            const tdOperacao = document.createElement('td');
            tdOperacao.innerHTML = `<span class="op-name"><span class="chip-dot" style="background:${cor}"></span>${rotuloExibicaoOperacao(op.nomeOperacao)}</span>`;

            const tdOrigem = document.createElement('td');
            tdOrigem.innerHTML = `<span class="origin-badge ${origemBadgeClass(origem)}">${origem}</span>`;

            const tdDetalhe = document.createElement('td');
            tdDetalhe.className = 'op-key';
            tdDetalhe.title = op.detalheOperacao || '-';
            tdDetalhe.textContent = op.detalheOperacao || '-';

            const tdDuracao = document.createElement('td');
            tdDuracao.innerHTML = `<span class="op-duration ${classeDuracao(op.duracaoMs)}">${op.duracaoFormatada || '-'}</span>`;

            const tdTaxa = document.createElement('td');
            tdTaxa.textContent = op.taxaSucesso === null || op.taxaSucesso === undefined ? '-' : `${op.taxaSucesso}%`;

            row.append(tdData, tdOperacao, tdOrigem, tdDetalhe, tdDuracao, tdTaxa);
            tableBody.appendChild(row);
        });
    }

    if (contadorTexto) {
        const total = historicoCompleto.length;
        const totalFiltrado = filtrados.length;
        const fimIntervalo = Math.min(inicio + ITENS_POR_PAGINA, totalFiltrado);
        const intervaloTexto = totalFiltrado === 0 ? '0-0' : `${inicio + 1}-${fimIntervalo}`;
        contadorTexto.textContent = totalFiltrado === total
            ? `${formatarNumero(total)} operacoes registradas · exibindo ${intervaloTexto}`
            : `${formatarNumero(totalFiltrado)} de ${formatarNumero(total)} operacoes · exibindo ${intervaloTexto}`;
    }
    if (paginacaoInfo) paginacaoInfo.textContent = `Pagina ${paginaAtualTabela} de ${totalPaginas}`;
    if (btnAnterior) btnAnterior.disabled = paginaAtualTabela <= 1;
    if (btnProxima) btnProxima.disabled = paginaAtualTabela >= totalPaginas;
}

function renderizarGraficosTelemetria(data, totalLinhas, cacheHits, cpuVal, heapPercent) {
    const brutas = Math.round(totalLinhas * 1.15) || 0;
    const unicas = totalLinhas || 0;
    const cache = cacheHits || 0;
    const llm = Math.max(0, totalLinhas - cacheHits);

    renderizarFunil([
        { label: 'Brutas', valor: brutas, cor: '#64748b' },
        { label: 'Únicas', valor: unicas, cor: '#818cf8' },
        { label: 'Cache', valor: cache, cor: '#2dd4bf' },
        { label: 'LLM', valor: llm, cor: '#6366f1' }
    ]);

    const contagemModelos = {};
    const listaTraducoes = Array.isArray(data.traducoesLlm) ? data.traducoesLlm : [];
    listaTraducoes.forEach(t => {
        const modelo = t.modeloLlm || 'Desconhecido';
        contagemModelos[modelo] = (contagemModelos[modelo] || 0) + 1;
    });
    renderizarDonut(contagemModelos);

    const now = new Date();
    const timeLabel = now.getHours().toString().padStart(2, '0') + ':' +
        now.getMinutes().toString().padStart(2, '0') + ':' +
        now.getSeconds().toString().padStart(2, '0');

    jvmCpuHistory.push(cpuVal);
    jvmHeapHistory.push(heapPercent);
    jvmLabelsHistory.push(timeLabel);

    if (jvmCpuHistory.length > 15) {
        jvmCpuHistory.shift();
        jvmHeapHistory.shift();
        jvmLabelsHistory.shift();
    }
    renderizarLinhasJvm(jvmCpuHistory, jvmHeapHistory);
}

function renderizarFunil(itens) {
    const alvo = document.getElementById('chart-funil-processamento');
    if (!alvo) return;

    const maior = Math.max(1, ...itens.map(item => item.valor));
    alvo.innerHTML = itens.map(item => {
        const altura = Math.max(6, Math.round((item.valor / maior) * 76));
        return `
            <div class="telemetry-bar-item" style="min-width:0;height:100%;display:grid;grid-template-rows:16px 1fr 18px;justify-items:center;align-items:end;">
                <span class="telemetry-bar-value" style="color:var(--text-muted);font-size:.64rem;line-height:1;">${formatarNumero(item.valor)}</span>
                <span class="telemetry-bar" style="display:block;width:min(28px,58%);min-height:6px;height:${altura}px;background:${item.cor};border-radius:5px 5px 2px 2px;box-shadow:0 0 16px ${hexParaRgba(item.cor, 0.25)};"></span>
                <span class="telemetry-bar-label" style="width:100%;color:var(--text-muted);font-size:.62rem;text-align:center;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${item.label}</span>
            </div>
        `;
    }).join('');
}

function renderizarDonut(contagemModelos) {
    const alvo = document.getElementById('chart-modelos-llm');
    if (!alvo) return;

    const entradas = Object.entries(contagemModelos);
    if (entradas.length === 0) {
        alvo.innerHTML = `
            <div class="telemetry-donut empty" style="width:92px;aspect-ratio:1;border-radius:50%;display:grid;place-items:center;position:relative;background:conic-gradient(rgba(139,151,173,.28) 0% 100%);">
                <span style="position:relative;z-index:1;color:var(--text-primary);font-size:.9rem;font-weight:800;">0</span>
            </div>
            <div class="telemetry-donut-label">Sem chamadas LLM</div>
        `;
        return;
    }

    const cores = ['#2dd4bf', '#818cf8', '#f59e0b', '#f43f5e', '#3b82f6'];
    const total = entradas.reduce((acc, [, valor]) => acc + valor, 0);
    let inicio = 0;
    const fatias = entradas.map(([, valor], idx) => {
        const fim = inicio + (valor / total) * 100;
        const trecho = `${cores[idx % cores.length]} ${inicio}% ${fim}%`;
        inicio = fim;
        return trecho;
    }).join(', ');
    const principal = entradas[0]?.[0] ?? 'LLM';

    alvo.innerHTML = `
        <div class="telemetry-donut" style="width:92px;aspect-ratio:1;border-radius:50%;display:grid;place-items:center;position:relative;background:conic-gradient(${fatias});box-shadow:0 0 22px rgba(45,212,191,.12);">
            <span style="position:relative;z-index:1;color:var(--text-primary);font-size:.9rem;font-weight:800;">${total}</span>
        </div>
        <div class="telemetry-donut-label" title="${principal}">${principal}</div>
    `;
}

function renderizarLinhasJvm(cpuHistory, heapHistory) {
    const alvo = document.getElementById('chart-jvm-performance');
    if (!alvo) return;

    const cpuSeries = cpuHistory.length > 1 ? cpuHistory : [cpuHistory[0] ?? 0, cpuHistory[0] ?? 0];
    const heapSeries = heapHistory.length > 1 ? heapHistory : [heapHistory[0] ?? 0, heapHistory[0] ?? 0];
    const cpuPoints = montarPolyline(cpuSeries);
    const heapPoints = montarPolyline(heapSeries);
    alvo.innerHTML = `
        <svg class="telemetry-line-chart" viewBox="0 0 240 90" preserveAspectRatio="none" aria-hidden="true" style="width:100%;height:100%;display:block;">
            <line x1="0" y1="15" x2="240" y2="15" stroke="rgba(255,255,255,.045)" stroke-width="1"></line>
            <line x1="0" y1="45" x2="240" y2="45" stroke="rgba(255,255,255,.045)" stroke-width="1"></line>
            <line x1="0" y1="75" x2="240" y2="75" stroke="rgba(255,255,255,.045)" stroke-width="1"></line>
            <polyline class="line-heap" points="${heapPoints}" fill="none" stroke="#06b6d4" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"></polyline>
            <polyline class="line-cpu" points="${cpuPoints}" fill="none" stroke="#8b5cf6" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"></polyline>
        </svg>
    `;
}

function montarPolyline(valores) {
    const largura = 240;
    const altura = 82;
    const baseY = 86;
    const divisor = Math.max(1, valores.length - 1);
    return valores.map((valor, idx) => {
        const x = (idx / divisor) * largura;
        const y = baseY - (Math.min(100, Math.max(0, valor)) / 100) * altura;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
}

function hexParaRgba(hex, alpha) {
    const normalizado = hex.replace('#', '');
    const numero = Number.parseInt(normalizado, 16);
    if (!Number.isFinite(numero)) return `rgba(255,255,255,${alpha})`;
    const r = (numero >> 16) & 255;
    const g = (numero >> 8) & 255;
    const b = numero & 255;
    return `rgba(${r},${g},${b},${alpha})`;
}

function renderizarLinhaVazia(tableBody, mensagem) {
    tableBody.innerHTML = '';
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 6;
    cell.className = 'table-empty';
    cell.textContent = mensagem;
    row.appendChild(cell);
    tableBody.appendChild(row);
}
