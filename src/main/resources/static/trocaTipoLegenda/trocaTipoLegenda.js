import { logNoConsole, mostrarAlerta } from '../js/app.js';

const PAINEL_HTML = 'trocaTipoLegenda/trocaTipoLegenda.html?v=3.1';

function escapeHtml(texto) {
    return String(texto ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

async function carregarPainelHtml() {
    const painel = document.getElementById('panel-troca-tipo-legenda');
    if (!painel || painel.dataset.moduloCarregado === 'true') {
        return painel;
    }

    const resposta = await fetch(PAINEL_HTML);
    if (!resposta.ok) {
        throw new Error(`Falha ao carregar ${PAINEL_HTML}`);
    }

    painel.innerHTML = await resposta.text();
    painel.dataset.moduloCarregado = 'true';
    return painel;
}

function vincularEventos() {
    const btnEscanear = document.getElementById('btn-escanear-fontes');
    const btnAplicar = document.getElementById('btn-aplicar-substituicoes');
    const btnLimpar = document.querySelector('.btn-clear-form[data-form="form-troca-tipo-legenda"]');
    const inputEntrada = document.getElementById('troca-tipo-legenda-entrada');
    
    const areaResultado = document.getElementById('area-auditoria-resultado');
    const cardCorrecao = document.getElementById('card-correcao-desbloqueado');
    const tabelaFontesCorpo = document.querySelector('#tabela-fontes tbody');
    
    const badgeTotal = document.getElementById('badge-total-arquivos');
    const badgeProblemas = document.getElementById('badge-arquivos-problemas');
    const btnToggleOk = document.getElementById('btn-toggle-arquivos-ok');

    if (!btnEscanear || !inputEntrada || !tabelaFontesCorpo) return;

    let ultimoResultadoAuditoria = null;
    let mostrarArquivosOk = false;

    const renderizarTabelaAuditoria = (data) => {
        tabelaFontesCorpo.innerHTML = '';

        if (!data.arquivos || data.arquivos.length === 0) {
            tabelaFontesCorpo.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--text-muted);">Nenhum arquivo de legenda encontrado na pasta.</td></tr>`;
            if (btnToggleOk) btnToggleOk.classList.add('hidden');
            return;
        }

        const arquivosComProblemas = data.arquivos.filter(arq =>
            (arq.fontes || []).some(fonteInfo => fonteInfo.problematica)
        );
        const arquivosOk = data.arquivos.filter(arq =>
            !(arq.fontes || []).some(fonteInfo => fonteInfo.problematica)
        );
        const arquivosVisiveis = mostrarArquivosOk
            ? [...arquivosComProblemas, ...arquivosOk]
            : arquivosComProblemas;

        if (btnToggleOk) {
            if (arquivosOk.length > 0) {
                btnToggleOk.classList.remove('hidden');
                btnToggleOk.textContent = mostrarArquivosOk
                    ? `Ocultar ${arquivosOk.length} OK`
                    : `Mostrar ${arquivosOk.length} OK`;
            } else {
                btnToggleOk.classList.add('hidden');
            }
        }

        if (arquivosVisiveis.length === 0) {
            tabelaFontesCorpo.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align:center; color: var(--accent-green); font-weight: 700;">
                        Nenhuma legenda precisa de alteração. ${arquivosOk.length} arquivo(s) estão OK.
                    </td>
                </tr>
            `;
            return;
        }

        arquivosVisiveis.forEach(arq => {
            const nomeArq = arq.arquivo;
            const nomeArqSeguro = escapeHtml(nomeArq);
            const fontesProblematicas = (arq.fontes || []).filter(fonteInfo => fonteInfo.problematica);
            const temProblema = fontesProblematicas.length > 0;
            const tr = document.createElement('tr');
            if (temProblema) {
                tr.className = 'linha-auditoria-problema';
            }

            const diagnosticoHtml = temProblema
                ? '<span class="status-badge pulse-red">Problema</span>'
                : '<span class="status-badge pulse-green">OK</span>';

            const fontesHtml = temProblema
                ? fontesProblematicas.map(fonteInfo => {
                    const estiloSeguro = escapeHtml(fonteInfo.estilo);
                    const fonteAtualSegura = escapeHtml(fonteInfo.fonteAtual);
                    const fonteSugeridaSegura = escapeHtml(fonteInfo.fonteSugerida);
                    return `<code>${estiloSeguro}: ${fonteAtualSegura} -> ${fonteSugeridaSegura}</code>`;
                }).join('<br>')
                : '<span style="color: var(--text-muted);">Nenhuma fonte legacy detectada</span>';

            const acaoHtml = temProblema
                ? '<strong style="color: var(--accent-green);">Substituir por Arial</strong>'
                : '<span style="color: var(--text-muted);">Manter arquivo</span>';

            const decisaoHtml = temProblema
                ? '<span class="status-badge pulse-red">Alterar se aplicar lote</span>'
                : '<span class="status-badge pulse-green">Não alterar</span>';

            tr.innerHTML = `
                <td class="td-arquivo-legenda" title="${nomeArqSeguro}"><strong>${nomeArqSeguro}</strong></td>
                <td>${diagnosticoHtml}</td>
                <td>${fontesHtml}</td>
                <td>${acaoHtml}</td>
                <td>${decisaoHtml}</td>
            `;
            tabelaFontesCorpo.appendChild(tr);
        });
    };

    if (btnToggleOk) {
        btnToggleOk.addEventListener('click', () => {
            if (!ultimoResultadoAuditoria) return;
            mostrarArquivosOk = !mostrarArquivosOk;
            renderizarTabelaAuditoria(ultimoResultadoAuditoria);
        });
    }

    // Ação do Botão: Escanear Fontes
    btnEscanear.addEventListener('click', async () => {
        const caminho = inputEntrada.value.trim();
        if (!caminho) {
            mostrarAlerta('Informe a pasta com as legendas a serem auditadas!', 'erro');
            return;
        }

        logNoConsole('console-troca-tipo-legenda', `Iniciando escaneamento de fontes em: ${caminho}`, 'info');
        btnEscanear.disabled = true;

        // Oculta cards antigos
        areaResultado.classList.add('hidden');
        cardCorrecao.classList.add('hidden');
        if (btnAplicar) {
            btnAplicar.disabled = false;
        }
        if (btnToggleOk) {
            btnToggleOk.classList.add('hidden');
        }
        ultimoResultadoAuditoria = null;
        mostrarArquivosOk = false;
        tabelaFontesCorpo.innerHTML = '';

        try {
            const res = await fetch('/api/troca-legenda/escanear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ diretorioLegendas: caminho })
            });

            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                throw new Error(data.erro || 'Falha ao realizar auditoria de fontes');
            }

            // Exibir a área de resultados
            areaResultado.classList.remove('hidden');
            badgeTotal.textContent = `${data.totalArquivosAnalisados} arquivos`;
            badgeProblemas.textContent = `${data.totalComProblemas} com problemas`;
            
            if (data.totalComProblemas > 0) {
                badgeProblemas.className = 'meta-badge score'; // Destaca em vermelho/rosa
            } else {
                badgeProblemas.className = 'meta-badge genre'; // Destaca em verde
            }

            ultimoResultadoAuditoria = data;
            renderizarTabelaAuditoria(data);

            // Desbloqueia a área de alteração se houver problemas
            if (data.totalComProblemas > 0) {
                cardCorrecao.classList.remove('hidden');
                if (btnAplicar) {
                    btnAplicar.disabled = false;
                }
                logNoConsole('console-troca-tipo-legenda', `Auditoria concluída: ${data.totalComProblemas} de ${data.totalArquivosAnalisados} arquivos possuem fontes vietnamitas legadas de alto risco. Área de substituição liberada!`, 'aviso');
                mostrarAlerta('Auditoria concluída! Fontes legadas problemáticas foram detectadas.', 'aviso');
            } else {
                cardCorrecao.classList.add('hidden');
                logNoConsole('console-troca-tipo-legenda', `Auditoria concluída: Todos os ${data.totalArquivosAnalisados} arquivos estão com fontes Unicode seguras. Nenhuma ação necessária!`, 'sucesso');
                mostrarAlerta('Parabéns! Todas as fontes analisadas são Unicode seguras.', 'sucesso');
            }

        } catch (err) {
            logNoConsole('console-troca-tipo-legenda', `Erro: ${err.message}`, 'erro');
            mostrarAlerta(err.message, 'erro');
        } finally {
            btnEscanear.disabled = false;
        }
    });

    // Ação do Botão: Aplicar Substituições
    if (btnAplicar) {
        btnAplicar.addEventListener('click', async () => {
            const caminho = inputEntrada.value.trim();
            if (!caminho) return;

            logNoConsole('console-troca-tipo-legenda', `Solicitando substituição de fontes em lote no pipeline...`, 'info');
            btnAplicar.disabled = true;

            try {
                const res = await fetch('/api/troca-legenda/aplicar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ diretorioLegendas: caminho })
                });

                const data = await res.json().catch(() => ({}));

                if (!res.ok) {
                    throw new Error(data.erro || 'Falha ao iniciar substituição de fontes');
                }

                logNoConsole('console-troca-tipo-legenda', data.mensagem || 'Substituição iniciada.', 'sucesso');
                mostrarAlerta('Processo de substituição de fontes iniciado! Acompanhe os logs.', 'sucesso');
                
                // Oculta a área de substituição para evitar duplo clique
                cardCorrecao.classList.add('hidden');
            } catch (err) {
                logNoConsole('console-troca-tipo-legenda', `Erro: ${err.message}`, 'erro');
                mostrarAlerta(err.message, 'erro');
                btnAplicar.disabled = false;
            }
        });
    }

    // Ação de Limpeza do Formulário
    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            areaResultado.classList.add('hidden');
            cardCorrecao.classList.add('hidden');
            tabelaFontesCorpo.innerHTML = '';
            ultimoResultadoAuditoria = null;
            mostrarArquivosOk = false;
            if (btnToggleOk) {
                btnToggleOk.classList.add('hidden');
            }
            logNoConsole('console-troca-tipo-legenda', 'Formulário e resultados limpos.', 'info');
        });
    }
}

export async function initTrocaTipoLegenda() {
    try {
        await carregarPainelHtml();
        vincularEventos();
        document.dispatchEvent(new CustomEvent('troca-tipo-legenda:painel-carregado'));
    } catch (err) {
        console.error('[Menu Troca Tipo Legenda] Erro ao inicializar módulo:', err);
        const painel = document.getElementById('panel-troca-tipo-legenda');
        if (painel) {
            painel.innerHTML = '<div class="glass-card"><p class="card-desc">Não foi possível carregar a interface de Troca de Tipo de Legenda.</p></div>';
        }
    }
}
