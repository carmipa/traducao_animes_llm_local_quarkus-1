/**
 * ==========================================================================
 * KRONOS CORE - ORQUESTRADOR GLOBAL FRONTEND (SPA & SSE STREAM LOGS)
 * ==========================================================================
 */

import { initAnalise } from '../analise/analise.js?v=4.0';
import { initExtracao } from '../extracao/extracao.js?v=3.0';
import { initAuditorConteudo } from '../auditorConteudoLegendas/auditorConteudoLegendas.js?v=3.9';
import { initTraducao } from '../traducao/traducao.js?v=3.0';
import { initCorrecao } from '../correcao/correcao.js?v=3.0';
import { initRevisao } from '../revisao/revisao.js?v=3.2';
import { initCura } from '../cura/cura.js?v=3.0';
import { initRevisaoLore } from '../revisaoLore/revisaoLore.js?v=3.1';
import { initTrocaTipoLegenda } from '../trocaTipoLegenda/trocaTipoLegenda.js?v=3.0';
import { initRemuxer } from '../remuxer/remuxer.js?v=3.1';
import { initMapa } from '../mapa/mapa.js?v=5.0';
import { initTelemetria } from '../telemetria/telemetria.js?v=3.1';
import { initDocumentacao } from '../documentacao/documentacao.js?v=3.0';
import { initSobre } from '../sobre/sobre.js?v=3.0';
import { initRenomearArquivos } from '../renomearArquivos/renomearArquivos.js?v=3.0';
import { initNovoKaraoke } from '../novoKaraoke/novoKaraoke.js?v=1.0';
import { initTraducaoKaraoke } from '../traducaoKaraoke/traducaoKaraoke.js?v=1.1';
import { initInicio } from '../inicio/inicio.js?v=1.0';
import { inicializarI18n } from '../i18n/i18n.js?v=2.1';

// Definições de Títulos e Subtítulos por seção do menu
const CONFIG_SECOES = {
    inicio: {
        titulo: "Painel Inicial",
        subtitulo: "Orquestrador automatizado e pipeline industrial de processamento de animes"
    },
    analise: {
        titulo: "1. Análise de Mídia",
        subtitulo: "Auditoria técnica de codecs, sincronia e taxas de bits de vídeos"
    },
    extracao: {
        titulo: "2. Extração de Legendas",
        subtitulo: "Extração industrial de faixas de legendas embutidas in vídeos (MKV, MP4 e outros)"
    },
    "auditor-conteudo": {
        titulo: "3. Análise de Legenda",
        subtitulo: "Auditoria de legendas .ass traduzidas: vazamento de efeitos, alucinações de IA e metadados"
    },
    traducao: {
        titulo: "4. Tradução Local via LLM",
        subtitulo: "Traduzir legendas originais em inglês usando inteligência artificial local"
    },
    correcao: {
        titulo: "5. Correção do Cache de Tradução",
        subtitulo: "Limpeza de inconsistências e preenchimento via raspagem do Google Tradutor"
    },
    revisao: {
        titulo: "6. Revisão de Legendas",
        subtitulo: "Concordância PT-BR via LLM local e correção de inglês residual via Google"
    },
    "revisao-lore": {
        titulo: "7. Revisão de Lore",
        subtitulo: "Padronização de nomes, locais e termos de mundo nas legendas via LLM e lore oficial"
    },
    "troca-tipo-legenda": {
        titulo: "8. Troca Tipo Legenda",
        subtitulo: "Auditoria e substituição em lote de fontes vietnamitas ou ANSI legadas por fontes Unicode seguras"
    },
    "novo-karaoke": {
        titulo: "9. Karaokê Simples",
        subtitulo: "Conversão de karaokê KFX em legendas simples e limpas, preservando o tempo original"
    },
    "traducao-karaoke": {
        titulo: "10. Tradução de Karaokê",
        subtitulo: "Letras de música em PT-BR com o japonês/romaji original preservado junto na tela"
    },
    cura: {
        titulo: "11. Correção de Legendas",
        subtitulo: "Corrige a legenda PT-BR usando a original como referência imutável"
    },
    remuxer: {
        titulo: "12. Remuxer Industrial",
        subtitulo: "Junção de vídeos originais e novas legendas traduzidas em novos MKVs"
    },
    "renomear-arquivos": {
        titulo: "13. Renomear Arquivos de Vídeo",
        subtitulo: "Limpeza de nomes de arquivo usando regex e metadados S01E01"
    },
    mapa: {
        titulo: "Mapeamento do Projeto",
        subtitulo: "Auditoria de taxonomia e visualização da árvore de estrutura do código"
    },
    telemetria: {
        titulo: "Telemetria KRONOS",
        subtitulo: "Observabilidade da traducao, cache local e historico operacional"
    },
    documentacao: {
        titulo: "Documentação",
        subtitulo: "Arquitetura, módulos, API REST e diagramas do KRONOS CORE"
    },
    sobre: {
        titulo: "Sobre o Autor",
        subtitulo: "Paulo André Carminati, contatos, formação e repositórios"
    }
};

let logsEventSource = null;
let logsReconnectTimer = null;
let seletorCaminhoEmAndamento = false;
const logsPendentesPorConsole = new Map();
const LIMITE_LOGS_PENDENTES = 1000;

document.addEventListener('traducao-karaoke:painel-carregado', () => {
    descarregarLogsPendentes('console-traducao-karaoke');
});

document.addEventListener('DOMContentLoaded', async () => {
    inicializarI18n();
    inicializarNavegacao();
    inicializarGruposMenu();
    await inicializarModulos();
    atualizarStatusConexao();
    buscarContadoresGlobais();
    conectarFluxoLugsSSE();

    inicializarMetadadosDinamicos();
    inicializarBotoesLimpezaFormularios();
    inicializarControlesConsole();
    inicializarBotoesProcurarCaminho();
    inicializarBotaoSair();
});

/**
 * Controla a troca de abas/painéis na sidebar e atualiza os títulos
 */
function inicializarNavegacao() {
    const botoesMenu = document.querySelectorAll('.nav-item');
    const paineis = document.querySelectorAll('.panel');
    const tituloPagina = document.getElementById('page-title');
    const subtituloPagina = document.getElementById('page-subtitle');

    botoesMenu.forEach(botao => {
        // "Sair" é uma ação, não uma aba — não participa da troca de painéis
        // (ver inicializarBotaoSair()).
        if (botao.id === 'btn-menu-sair') return;
        botao.addEventListener('click', () => {
            const target = botao.getAttribute('data-target');
            
            // 1. Atualizar classe ativa dos botões do menu
            botoesMenu.forEach(b => b.classList.remove('active'));
            botao.classList.add('active');

            // 2. Exibir painel correto
            paineis.forEach(painel => {
                painel.classList.remove('active');
                if (painel.id === `panel-${target}`) {
                    painel.classList.add('active');
                }
            });

            // 3. Atualizar títulos no cabeçalho
            if (CONFIG_SECOES[target]) {
                tituloPagina.textContent = CONFIG_SECOES[target].titulo;
                subtituloPagina.textContent = CONFIG_SECOES[target].subtitulo;
            }

            // Ações extras ao abrir painéis específicos
            if (target === 'telemetria') {
                document.getElementById('btn-refresh-telemetria').click();
            }
        });
    });
}

/**
 * Acordeão dos grupos do menu lateral: alterna abrir/fechar cada grupo e
 * persiste a escolha no localStorage, restaurando na próxima visita.
 */
function inicializarGruposMenu() {
    const CHAVE_ESTADO = 'kronos.menuGruposFechados';
    // Sem estado salvo, "Preparação" inicia colapsada; depois vale a escolha do usuário.
    let fechados;
    try {
        fechados = new Set(JSON.parse(localStorage.getItem(CHAVE_ESTADO) || '["preparacao"]'));
    } catch (e) {
        fechados = new Set(['preparacao']);
    }

    document.querySelectorAll('.nav-group').forEach(grupo => {
        const id = grupo.getAttribute('data-grupo');
        const header = grupo.querySelector('.nav-group-header');
        if (!header) return;

        const aplicar = () => {
            const fechado = fechados.has(id);
            grupo.classList.toggle('fechado', fechado);
            header.setAttribute('aria-expanded', String(!fechado));
        };
        aplicar();

        header.addEventListener('click', () => {
            if (fechados.has(id)) {
                fechados.delete(id);
            } else {
                fechados.add(id);
            }
            try {
                localStorage.setItem(CHAVE_ESTADO, JSON.stringify([...fechados]));
            } catch (e) {
                // armazenamento indisponível: estado vive só nesta sessão
            }
            aplicar();
        });
    });
}

/**
 * Inicializa cada um dos módulos JavaScript específicos das pastas
 */
async function inicializarModulos() {
    initInicio();
    initAnalise();
    initExtracao();
    await initAuditorConteudo();
    await initNovoKaraoke();
    await initTraducaoKaraoke();
    initTraducao();
    initCorrecao();
    initRevisao();
    initCura();
    await initRevisaoLore();
    await initTrocaTipoLegenda();
    initRemuxer();
    await initRenomearArquivos();
    initMapa();
    initTelemetria();
    initDocumentacao();
    await initSobre();
}

/**
 * PROPÓSITO DE NEGÓCIO: conecta a interface ao fluxo SSE e encaminha cada etapa
 * operacional ao console visual do módulo que iniciou o processamento.
 *
 * INVARIANTES DO DOMÍNIO: cada canal deve apontar para um único console; mensagens
 * recebidas antes da montagem do painel devem permanecer disponíveis; só pode haver
 * uma conexão SSE ativa por página.
 *
 * COMPORTAMENTO EM CASO DE FALHA: fecha a conexão defeituosa e agenda uma nova
 * tentativa em cinco segundos, preservando no buffer as linhas já recebidas.
 */
function conectarFluxoLugsSSE() {
    if (logsEventSource && logsEventSource.readyState !== EventSource.CLOSED) {
        return;
    }
    if (logsReconnectTimer) {
        clearTimeout(logsReconnectTimer);
        logsReconnectTimer = null;
    }

    console.log('Iniciando escuta de Server-Sent Events (SSE) para logs...');
    const eventSource = new EventSource('/api/logs/stream');
    logsEventSource = eventSource;

    // O backend publica cada operação em segundo plano sob um canal SSE com
    // o próprio nome (ver LogStreamService#definirCanalAtual no servidor),
    // então a rota para o console certo é direta — não depende de qual aba
    // está aberta no navegador no momento em que a linha de log chega.
    const consoleMap = {
        'analise': 'console-analise',
        'extracao': 'console-extracao',
        'auditor-conteudo': 'console-auditor-conteudo',
        'traducao': 'console-traducao',
        'correcao': 'console-correcao',
        'revisao': 'console-revisao',
        'revisao-lore': 'console-revisao-lore',
        'troca-tipo-legenda': 'console-troca-tipo-legenda',
        'correcao-legendas': 'console-cura',
        'cura': 'console-cura',
        'remuxer': 'console-remuxer',
        'renomear-arquivos': 'console-renomear-arquivos',
        'novo-karaoke': 'console-novo-karaoke',
        'traducao-karaoke': 'console-traducao-karaoke'
    };

    for (const [canal, consoleId] of Object.entries(consoleMap)) {
        eventSource.addEventListener(canal, (event) => {
            logNoConsoleFormatado(consoleId, event.data);
            verificarAlertaSSE(event.data);
        });
    }

    // Canal genérico de fallback, para qualquer log que não pertença a uma
    // operação específica das abas acima.
    eventSource.addEventListener('console', (event) => {
        const activeNav = document.querySelector('.nav-item.active');
        if (!activeNav) return;

        const target = activeNav.getAttribute('data-target');
        const consoleId = consoleMap[target];
        if (consoleId) {
            logNoConsoleFormatado(consoleId, event.data);
            verificarAlertaSSE(event.data);
        }
    });

    // Resultado ESTRUTURADO (JSON) da Análise de Mídia: o backend publica o
    // resultado num único evento e aqui o console é substituído por cartões +
    // tabela de legendas (a análise não grava mais relatório em disco).
    eventSource.addEventListener('analise-relatorio', (event) => {
        renderizarAnaliseMidia('console-analise', event.data);
    });

    eventSource.addEventListener('sistema', (event) => {
        console.log('SSE Sistema:', event.data);
    });

    eventSource.onerror = (err) => {
        console.warn('Erro na conexão de stream SSE, tentando reconectar em 5s...', err);
        eventSource.close();
        if (logsEventSource === eventSource) {
            logsEventSource = null;
        }
        if (!logsReconnectTimer) {
            logsReconnectTimer = setTimeout(() => {
                logsReconnectTimer = null;
                conectarFluxoLugsSSE();
            }, 5000);
        }
    };
}

/**
 * Verifica se o servidor Spring Boot está respondendo
 */
async function atualizarStatusConexao() {
    const indicador = document.querySelector('.status-indicator');
    const statusText = document.querySelector('.status-text');
    
    try {
        const res = await fetch('/api/status', { method: 'GET' });
        if (res.ok) {
            indicador.className = 'status-indicator online';
            statusText.textContent = 'Backend Online';
        } else {
            throw new Error();
        }
    } catch (e) {
        indicador.className = 'status-indicator offline';
        statusText.textContent = 'Backend Offline';
    }

    // Repete a verificação a cada 10 segundos
    setTimeout(atualizarStatusConexao, 10000);
}

/**
 * Carrega estatísticas rápidas no cabeçalho
 */
async function buscarContadoresGlobais() {
    try {
        const res = await fetch('/api/telemetria');
        if (res.ok) {
            const dados = await res.json();
            
            // Atualiza cabeçalho global
            const cacheCount = document.getElementById('stat-cache-count');
            if (cacheCount && dados.cacheCount !== undefined) {
                cacheCount.textContent = `${dados.cacheCount} Arquivos`;
            }
            
            // Atualiza widget da home (Painel Inicial)
            const dashCacheCount = document.getElementById('dashboard-cache-count');
            if (dashCacheCount && dados.cacheCount !== undefined) {
                dashCacheCount.textContent = `${dados.cacheCount} Arquivos`;
            }
        }
    } catch (e) {
        console.warn('Não foi possível obter os contadores globais da telemetria.');
        const cacheCount = document.getElementById('stat-cache-count');
        if (cacheCount) cacheCount.textContent = 'Indisponível';
        
        const dashCacheCount = document.getElementById('dashboard-cache-count');
        if (dashCacheCount) dashCacheCount.textContent = 'Indisponível';
    }
}

/**
 * Escapa caracteres especiais de HTML para impedir que conteúdo vindo do
 * backend (nomes de arquivo, mensagens de erro, texto raspado do Google
 * Translate) seja interpretado como markup/script ao cair no innerHTML.
 */
function escapeHtml(texto) {
    return texto
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * Realiza o parse de códigos ANSI para tags HTML estilizadas
 */
function ansiParaHtml(texto) {
    let html = escapeHtml(texto);

    // Sanitização contra caracteres de controle de cursor
    html = html.replace(/\r?\033\[K/g, '');
    html = html.replace(/\r/g, '');
    
    // Substitui quebras de linha literais se houver
    html = html.replace(/\\n/g, '<br>');

    // Negritos e Resets
    html = html.replace(/\033\[1m/g, '<span style="font-weight: 700;">');
    html = html.replace(/\u001b\[1m/g, '<span style="font-weight: 700;">');
    html = html.replace(/\033\[2m/g, '<span style="opacity: 0.72;">');
    html = html.replace(/\u001b\[2m/g, '<span style="opacity: 0.72;">');
    html = html.replace(/\033\[0m/g, '</span>');
    html = html.replace(/\u001b\[0m/g, '</span>');

    // Mapeamento de Cores ANSI
    const cores = {
        '30': 'var(--text-muted)',
        '31': 'rgba(239, 68, 68, 0.95)', // Vermelho elegante
        '32': 'var(--accent-green)',
        '33': 'var(--accent-yellow)',
        '34': 'var(--accent-blue)',
        '35': 'var(--accent-purple)',
        '36': 'var(--accent-cyan)',
        '37': 'var(--text-primary)',
        '90': 'var(--text-muted)'
    };

    for (let code in cores) {
        const regex1 = new RegExp('\\033\\[' + code + 'm', 'g');
        const regex2 = new RegExp('\\u001b\\[' + code + 'm', 'g');
        const replacement = `<span style="color: ${cores[code]};">`;
        html = html.replace(regex1, replacement);
        html = html.replace(regex2, replacement);
    }

    return html;
}

/**
 * PROPÓSITO DE NEGÓCIO: apresenta no navegador uma linha produzida pelo CLI/backend,
 * conservando horário, cores ANSI e a sequência original do processamento.
 *
 * INVARIANTES DO DOMÍNIO: conteúdo externo nunca é interpretado como HTML; cada
 * console mantém no máximo mil linhas visíveis; uma linha não pode ser descartada
 * apenas porque o painel dinâmico ainda não terminou de carregar.
 *
 * COMPORTAMENTO EM CASO DE FALHA: se o elemento visual ainda não existir, guarda a
 * mensagem em memória para exibição posterior; mensagens excedentes mais antigas
 * são removidas para impedir crescimento ilimitado.
 */
function logNoConsoleFormatado(consoleId, rawMessage) {
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) {
        const pendentes = logsPendentesPorConsole.get(consoleId) || [];
        pendentes.push(rawMessage);
        if (pendentes.length > LIMITE_LOGS_PENDENTES) {
            pendentes.splice(0, pendentes.length - LIMITE_LOGS_PENDENTES);
        }
        logsPendentesPorConsole.set(consoleId, pendentes);
        return;
    }

    // Remove mensagem "Aguardando..." se existir
    const sysMsg = consoleDiv.querySelector('.system-message');
    if (sysMsg) {
        consoleDiv.removeChild(sysMsg);
    }

    const timestamp = new Date().toLocaleTimeString();
    const htmlMensagem = ansiParaHtml(rawMessage);
    
    const linhaLog = document.createElement('div');
    linhaLog.className = 'log-line';
    linhaLog.innerHTML = `<span style="color: var(--text-muted); font-size: 0.75rem;">[${timestamp}]</span> ${htmlMensagem}`;
    
    consoleDiv.appendChild(linhaLog);
    
    // Proteção extrema contra travamentos: Limita a 1000 linhas visíveis no console HTML
    while (consoleDiv.childElementCount > 1000) {
        consoleDiv.removeChild(consoleDiv.firstChild);
    }

    consoleDiv.scrollTop = consoleDiv.scrollHeight;
}

/**
 * PROPÓSITO DE NEGÓCIO: entrega ao terminal visual as linhas produzidas enquanto o
 * fragmento HTML do módulo ainda estava sendo carregado pelo navegador.
 *
 * INVARIANTES DO DOMÍNIO: respeita a ordem de chegada e remove o lote do buffer antes
 * de renderizar para impedir duplicação caso outro evento de montagem seja emitido.
 *
 * COMPORTAMENTO EM CASO DE FALHA: se o console ainda não existir ou não houver linhas
 * pendentes, não altera o estado; uma nova montagem poderá tentar novamente.
 */
function descarregarLogsPendentes(consoleId) {
    if (!document.getElementById(consoleId)) return;

    const pendentes = logsPendentesPorConsole.get(consoleId);
    if (!pendentes || pendentes.length === 0) return;

    logsPendentesPorConsole.delete(consoleId);
    pendentes.forEach(mensagem => logNoConsoleFormatado(consoleId, mensagem));
}

/**
 * Renderiza o resultado ESTRUTURADO da Análise de Mídia (JSON via SSE) em
 * cartões + tabela de legendas em destaque. Guarda o resultado em
 * window.__ultimaAnaliseMidia para a exportação TXT manual.
 */
function renderizarAnaliseMidia(consoleId, jsonText) {
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) return;

    let dados;
    try {
        dados = JSON.parse(jsonText);
    } catch (e) {
        consoleDiv.innerHTML = '<div class="system-message">Falha ao interpretar o resultado da análise.</div>';
        return;
    }
    window.__ultimaAnaliseMidia = dados;

    const resultados = Array.isArray(dados.resultados) ? dados.resultados : [];
    const falhas = Array.isArray(dados.falhas) ? dados.falhas : [];

    const esc = (v) => String(v == null ? '' : v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const kbps = (b) => (b > 0 ? Math.round(b / 1000) + ' kbps' : 'N/A');
    const gib = (bytes) => (bytes > 0 ? (bytes / (1024 ** 3)).toFixed(2) + ' GiB' : 'N/A');
    const dur = (s) => {
        if (!s || s <= 0) return 'N/A';
        const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = Math.floor(s % 60);
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
    };
    const badgeTrad = (leg) => leg.traduzivel
        ? '<span class="badge-trad sim">Traduzível</span>'
        : (leg.exigeOcr ? '<span class="badge-trad ocr">OCR</span>' : '<span class="badge-trad nao">—</span>');
    const flags = (leg) => {
        const f = [];
        if (leg.isDefault) f.push('default');
        if (leg.isForced) f.push('forced');
        if (leg.acessibilidade) f.push('acess.');
        return f.length ? f.join(', ') : '—';
    };
    const lista = (arr, render, limite = 12) => {
        const a = arr || [];
        if (!a.length) return '<li>—</li>';
        return a.slice(0, limite).map(render).join('') + (a.length > limite ? `<li class="ac-dim">+${a.length - limite}…</li>` : '');
    };

    const comTexto = resultados.filter(r => (r.legendas || []).some(l => l.traduzivel)).length;

    let html = '<div class="analise-wrapper">';
    html += '<div class="analise-cabecalho"><span class="material-symbols-outlined">fact_check</span>'
        + '<span>Análise de Mídia</span>'
        + `<span class="analise-resumo">${resultados.length} arquivo(s) · ${comTexto} com legenda de texto · ${falhas.length} falha(s)</span></div>`;

    if (!resultados.length && !falhas.length) {
        html += '<div class="system-message">Nenhum resultado.</div>';
    }

    for (const r of resultados) {
        const c = r.container || {};
        html += '<div class="analise-card">';
        html += `<div class="ac-titulo"><span class="material-symbols-outlined">movie</span>${esc(r.nomeArquivo)}</div>`;
        html += '<div class="ac-grid">';
        html += '<div class="ac-bloco"><h5>Contêiner</h5><ul>'
            + `<li>Formato: ${esc(c.formato)}</li><li>Tamanho: ${gib(c.tamanhoBytes)}</li>`
            + `<li>Duração: ${dur(c.duracaoSegundos)}</li><li>Bitrate: ${kbps(c.bitrateGeral)}</li>`
            + `<li>Escrito por: ${esc(c.aplicacaoEscrita)}</li></ul></div>`;
        html += `<div class="ac-bloco"><h5>Vídeo (${(r.videos || []).length})</h5><ul>`
            + lista(r.videos, v => `<li>${esc(v.codecId)} · ${v.width}x${v.height} · ${v.bitDepth}bit · ${(v.fps || 0).toFixed(3)}fps · ${kbps(v.bitrate)}</li>`) + '</ul></div>';
        html += `<div class="ac-bloco"><h5>Áudio (${(r.audios || []).length})</h5><ul>`
            + lista(r.audios, a => `<li>${esc(a.idioma)} · ${esc(a.format)} · ${a.channels}ch · ${(a.sampleRateKHz || 0).toFixed(1)}kHz · ${kbps(a.bitrate)}</li>`) + '</ul></div>';
        html += `<div class="ac-bloco"><h5>Capítulos (${(r.capitulos || []).length})</h5><ul>`
            + lista(r.capitulos, cap => `<li>${cap.numero}. ${esc(cap.titulo)} <span class="ac-dim">(${dur(cap.inicioSegundos)})</span></li>`) + '</ul></div>';
        html += `<div class="ac-bloco"><h5>Anexos (${(r.anexos || []).length})</h5><ul>`
            + lista(r.anexos, an => `<li>${esc(an.nomeArquivo)} <span class="ac-dim">${esc(an.mimeType)}</span></li>`) + '</ul></div>';
        html += '</div>';

        html += `<div class="ac-legendas"><h5>Legendas (${(r.legendas || []).length})</h5>`;
        if (!(r.legendas || []).length) {
            html += '<p class="ac-sem-legenda">Nenhuma faixa de legenda encontrada — pode ser RAW; hardsub não confirmado por esta análise.</p>';
        } else {
            html += '<table class="ac-tabela"><thead><tr><th>#</th><th>Idioma</th><th>Formato</th><th>Tipo</th><th>Flags</th><th>Traduzível</th></tr></thead><tbody>';
            for (const leg of r.legendas) {
                html += `<tr><td>${(leg.indexRelativo ?? 0) + 1}</td><td>${esc(leg.idioma)}</td><td>${esc(leg.formato)}</td>`
                    + `<td>${esc(leg.tipoCurto)} <span class="ac-dim">(${esc(leg.categoria)})</span></td>`
                    + `<td>${esc(flags(leg))}</td><td>${badgeTrad(leg)}</td></tr>`;
            }
            html += '</tbody></table>';
        }
        html += '</div></div>';
    }

    if (falhas.length) {
        html += '<div class="analise-falhas"><h5>Falhas na análise (' + falhas.length + ')</h5><ul>'
            + falhas.map(f => `<li><strong>${esc(f.arquivo)}</strong>: ${esc(f.erro)}</li>`).join('') + '</ul></div>';
    }

    html += '</div>';
    consoleDiv.innerHTML = html;
    consoleDiv.scrollTop = 0;
}

/**
 * Constrói o TXT (exportação manual) a partir do resultado estruturado da análise.
 */
export function montarTxtAnaliseMidia(dados) {
    if (!dados) return '';
    const resultados = dados.resultados || [];
    const falhas = dados.falhas || [];
    const L = [];
    L.push('RELATORIO DE ANALISE DE MIDIA');
    L.push('Arquivos: ' + resultados.length + ' | Falhas: ' + falhas.length);
    L.push('');
    for (const r of resultados) {
        const c = r.container || {};
        L.push('='.repeat(70));
        L.push('ARQUIVO: ' + (r.nomeArquivo || ''));
        L.push('='.repeat(70));
        L.push('Conteiner: ' + (c.formato || '') + ' | Duracao: ' + (c.duracaoSegundos || 0).toFixed(2) + 's | ' + ((c.tamanhoBytes || 0) / (1024 ** 3)).toFixed(2) + ' GiB');
        L.push('Faixas: video=' + (r.videos || []).length + ' audio=' + (r.audios || []).length
            + ' legenda=' + (r.legendas || []).length + ' capitulos=' + (r.capitulos || []).length + ' anexos=' + (r.anexos || []).length);
        L.push('Legendas:');
        if (!(r.legendas || []).length) {
            L.push('  (nenhuma faixa; hardsub nao confirmado por esta analise)');
        } else {
            for (const leg of r.legendas) {
                L.push('  [' + ((leg.indexRelativo || 0) + 1) + '] ' + (leg.idioma || '') + ' / ' + (leg.tipoCurto || '')
                    + ' (' + (leg.categoria || '') + ') -> ' + (leg.traduzivel ? 'traduzivel' : (leg.exigeOcr ? 'OCR' : '-'))
                    + ' | default=' + !!leg.isDefault + ' forced=' + !!leg.isForced + ' acess=' + !!leg.acessibilidade);
            }
        }
        L.push('');
    }
    if (falhas.length) {
        L.push('FALHAS:');
        for (const f of falhas) L.push('  ' + (f.arquivo || '') + ': ' + (f.erro || ''));
    }
    return L.join('\r\n');
}

/**
 * Método genérico clássico para logs manuais do frontend
 */
export function logNoConsole(consoleId, mensagem, tipo = 'info') {
    let corAnsi = '\u001b[37m'; // Branco padrão
    if (tipo === 'erro') corAnsi = '\u001b[31m';
    if (tipo === 'aviso') corAnsi = '\u001b[33m';
    if (tipo === 'sucesso') corAnsi = '\u001b[32m';
    if (tipo === 'info') corAnsi = '\u001b[36m'; // Ciano para comandos do sistema

    logNoConsoleFormatado(consoleId, `${corAnsi}${mensagem}\u001b[0m`);
}

// Configura funcionalidade de limpar os consoles
document.addEventListener('click', (e) => {
    const btn = e.target.closest('.btn-clear-console');
    if (!btn) return;

    const consoleId = btn.getAttribute('data-target');
    const consoleDiv = document.getElementById(consoleId);
    if (consoleDiv) {
        consoleDiv.innerHTML = '<div class="system-message">Console limpo. Aguardando novos logs...</div>';
    }
});

// Os botões "Parar Execução" foram removidos da UI (decisão 2026-07-09): a
// parada cooperativa nunca interrompia o job de forma perceptível para o
// usuário. O endpoint POST /api/pipeline/parar continua existindo e é usado
// internamente pelo fluxo de encerramento ("Sair").

// Configura funcionalidade de copiar o conteúdo de um console/relatório
document.addEventListener('click', async (e) => {
    const btn = e.target.closest('.btn-copy-console');
    if (!btn) return;

    const consoleId = btn.getAttribute('data-target');
    const consoleDiv = document.getElementById(consoleId);
    if (!consoleDiv) return;

    const texto = consoleDiv.innerText.trim();
    if (!texto) {
        mostrarAlerta('Não há conteúdo para copiar.', 'aviso');
        return;
    }

    try {
        await navigator.clipboard.writeText(texto);
        mostrarAlerta('Conteúdo copiado para a área de transferência!', 'sucesso');
    } catch (err) {
        console.warn('Falha ao copiar via Clipboard API, usando fallback.', err);
        const textarea = document.createElement('textarea');
        textarea.value = texto;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
            document.execCommand('copy');
            mostrarAlerta('Conteúdo copiado para a área de transferência!', 'sucesso');
        } catch (fallbackErr) {
            mostrarAlerta('Não foi possível copiar automaticamente. Selecione o texto manualmente.', 'erro');
        } finally {
            document.body.removeChild(textarea);
        }
    }
});

/**
 * Exibe um alerta flutuante (Toast) na tela
 */
export function mostrarAlerta(mensagem, tipo = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${tipo}`;

    // Ícone do toast em Material Symbols (regra do design: nunca emojis)
    let icon = 'info';
    if (tipo === 'erro') icon = 'error';
    if (tipo === 'sucesso') icon = 'check_circle';
    if (tipo === 'aviso') icon = 'warning';

    toast.innerHTML = `
        <div class="toast-content">
            <span class="material-symbols-outlined toast-icon">${icon}</span> &nbsp; ${escapeHtml(mensagem)}
        </div>
        <button class="toast-close" title="Fechar">&times;</button>
    `;

    container.appendChild(toast);
    
    // Força reflow para animação
    toast.offsetHeight;
    toast.classList.add('show');

    // Auto-destruir após 7 segundos
    const timeout = setTimeout(() => fecharToast(toast), 7000);

    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(timeout);
        fecharToast(toast);
    });
}

function fecharToast(toast) {
    toast.classList.remove('show');
    toast.classList.add('hiding');
    toast.addEventListener('transitionend', () => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    });
}

/**
 * Analisa as mensagens recebidas via SSE e dispara alertas caso sejam de erro fatal ou sucesso
 */
export function verificarAlertaSSE(mensagem) {
    // Remove códigos ANSI limpos para exibir no Toast
    const msgLimpa = mensagem.replace(/\u001B\[[0-9;]*m/g, '').replace(/\033\[[0-9;]*m/g, '').trim();
    
    if (msgLimpa.includes('[ERRO]') || msgLimpa.includes('[FAIL]')) {
        const erroMsg = msgLimpa.replace(/\[ERRO\]|\[FAIL\]/, '').trim();
        mostrarAlerta(erroMsg, 'erro');
    } 
    else if (msgLimpa.includes('PROCESSAMENTO FINALIZADO') || msgLimpa.includes('[SUCESSO]')) {
        mostrarAlerta(msgLimpa, 'sucesso');
        const btnRefresh = document.getElementById('btn-refresh-telemetria');
        if (btnRefresh) btnRefresh.click();
    }
}

/**
 * Monitora inputs de caminho/pasta e selects de contexto para carregar dinamicamente os metadados e capas de animes
 */
function limparTermoMetadata(texto) {
    if (!texto) return '';

    return texto
        .replace(/\s*-\s*Revis[aã]o\s+de\s+Lore\s*$/i, '')
        .replace(/\s+Revis[aã]o\s+de\s+Lore\s*$/i, '')
        .replace(/\s+/g, ' ')
        .trim();
}

function inicializarMetadadosDinamicos() {
    const mapeamentoFormularios = [
        { inputId: 'analise-entrada', selectId: 'analise-contexto', bannerId: 'meta-banner-analise' },
        { inputId: 'traducao-entrada', selectId: 'traducao-contexto', bannerId: 'meta-banner-traducao' },
        { inputId: 'correcao-entrada', selectId: 'correcao-contexto', bannerId: 'meta-banner-correcao' },
        { inputId: 'revisao-entrada', selectId: 'revisao-contexto', bannerId: 'meta-banner-revisao' },
        { inputId: 'cura-entrada-original', selectId: 'cura-contexto', bannerId: 'meta-banner-cura' },
        { inputId: 'revisao-lore-entrada-original', selectId: 'revisao-lore-contexto', bannerId: 'meta-banner-revisao-lore' },
        { inputId: 'troca-tipo-legenda-entrada', selectId: 'troca-tipo-legenda-contexto', bannerId: 'meta-banner-troca-tipo-legenda' },
        { inputId: 'limpanome-entrada', selectId: 'renomear-arquivos-contexto', bannerId: 'meta-banner-limpanome' },
        { inputId: 'novo-karaoke-entrada', selectId: 'novo-karaoke-contexto', bannerId: 'meta-banner-novo-karaoke' },
        { inputId: 'traducao-karaoke-entrada', selectId: 'traducao-karaoke-contexto', bannerId: 'meta-banner-traducao-karaoke' }
    ];

    const atualizarItem = (item) => {
        const input = document.getElementById(item.inputId);
        const select = item.selectId ? document.getElementById(item.selectId) : null;
        let termo = '';

        // Se houver select preenchido com obra válida, usa o contexto como fonte principal de lore
        if (select && select.value && select.selectedIndex >= 0) {
            const option = select.options[select.selectedIndex];
            const optText = option.text;
            if (optText && !optText.includes('Carregando') && !optText.includes('Selecione')) {
                termo = option.dataset.metadataQuery || limparTermoMetadata(optText);
            }
        }

        // Se não houver contexto no select, ou se o usuário digitou um caminho completo de mídia (que não seja termos genéricos de pasta)
        if (!termo && input && input.value.trim().length > 3) {
            const val = input.value.trim();
            if (!['cache', 'logs', 'relatorios'].includes(val.toLowerCase())) {
                termo = val;
            }
        }

        if (termo && termo.length > 2) {
            carregarMetadataAnime(termo, item.bannerId);
        } else {
            const banner = document.getElementById(item.bannerId);
            if (banner) banner.classList.add('hidden');
        }
    };

    // Popula automaticamente todos os selects de contexto dos módulos auxiliares.
    const popularContextos = () => {
        carregarContextosAuxiliares(['analise-contexto', 'traducao-contexto', 'correcao-contexto', 'revisao-contexto', 'cura-contexto', 'revisao-lore-contexto', 'troca-tipo-legenda-contexto', 'renomear-arquivos-contexto', 'novo-karaoke-contexto', 'traducao-karaoke-contexto'], () => {
            mapeamentoFormularios.forEach(atualizarItem);
        });
    };

    popularContextos();
    document.addEventListener('revisao-lore:painel-carregado', popularContextos);
    document.addEventListener('troca-tipo-legenda:painel-carregado', popularContextos);
    document.addEventListener('renomear-arquivos:painel-carregado', popularContextos);
    document.addEventListener('novo-karaoke:painel-carregado', popularContextos);

    mapeamentoFormularios.forEach(item => {
        const input = document.getElementById(item.inputId);
        const select = item.selectId ? document.getElementById(item.selectId) : null;

        if (input) {
            input.addEventListener('input', () => atualizarItem(item));
            input.addEventListener('change', () => atualizarItem(item));
            input.addEventListener('blur', () => atualizarItem(item));
        }

        if (select) {
            select.addEventListener('change', () => atualizarItem(item));
            const observer = new MutationObserver(() => {
                setTimeout(() => atualizarItem(item), 100);
            });
            observer.observe(select, { childList: true });
        }

        // Dispara verificação inicial
        setTimeout(() => atualizarItem(item), 500);
    });

    // Emite carregado ao trocar de aba no menu lateral
    document.querySelectorAll('.nav-menu .nav-item, .sidebar-nav .nav-item').forEach(nav => {
        nav.addEventListener('click', () => {
            setTimeout(() => mapeamentoFormularios.forEach(atualizarItem), 150);
        });
    });
}

async function carregarContextosAuxiliares(idsSelects, onComplete) {
    try {
        const [response, responseRevisaoLore] = await Promise.all([
            fetch('/api/contextos', { cache: 'no-store' }),
            fetch('/api/revisao-lore/contextos', { cache: 'no-store' }).catch(() => null)
        ]);
        if (!response.ok) return;
        const contextos = await response.json();
        if (!Array.isArray(contextos) || contextos.length === 0) return;
        const contextosRevisaoLore = responseRevisaoLore?.ok
            ? await responseRevisaoLore.json()
            : contextos;

        // traducao-karaoke-contexto NÃO é auxiliar: o contexto alimenta o prompt
        // do LLM (lore), então recebe a obra padrão pré-selecionada, como o
        // select da Tradução Local.
        const todosSelects = ['analise-contexto', 'traducao-contexto', 'correcao-contexto', 'revisao-contexto', 'cura-contexto', 'revisao-lore-contexto', 'troca-tipo-legenda-contexto', 'renomear-arquivos-contexto', 'novo-karaoke-contexto', 'traducao-karaoke-contexto'];
        todosSelects.forEach(id => {
            const select = document.getElementById(id);
            if (!select) return;

            const ehAuxiliar = (id === 'analise-contexto' || id === 'correcao-contexto' || id === 'cura-contexto' || id === 'troca-tipo-legenda-contexto' || id === 'renomear-arquivos-contexto' || id === 'novo-karaoke-contexto');
            const ehRevisaoLore = (id === 'revisao-lore-contexto');
            select.innerHTML = '';
            
            if (ehAuxiliar) {
                const optDefault = document.createElement('option');
                optDefault.value = '';
                optDefault.textContent = '-- Selecione uma obra para visualizar --';
                select.appendChild(optDefault);
            }

            if (ehRevisaoLore) {
                const optObrigatorio = document.createElement('option');
                optObrigatorio.value = '';
                optObrigatorio.textContent = '-- Selecione a obra (obrigatório) --';
                optObrigatorio.disabled = true;
                optObrigatorio.selected = true;
                select.appendChild(optObrigatorio);
            }

            const fonteContextos = ehRevisaoLore && Array.isArray(contextosRevisaoLore) && contextosRevisaoLore.length > 0
                ? contextosRevisaoLore
                : contextos;

            fonteContextos.forEach(ctx => {
                const opt = document.createElement('option');
                opt.value = ctx.id;
                opt.textContent = ctx.nome;
                if (ctx.termoMetadata) {
                    opt.dataset.metadataQuery = ctx.termoMetadata;
                }
                if (!ehAuxiliar && !ehRevisaoLore && ctx.padrao) {
                    opt.selected = true;
                }
                select.appendChild(opt);
            });
        });

        if (onComplete && typeof onComplete === 'function') {
            onComplete();
        }
    } catch (e) {
        console.warn('Falha ao carregar contextos auxiliares:', e);
    }
}

async function carregarMetadataAnime(caminho, bannerId) {
    const banner = document.getElementById(bannerId);
    if (!banner) return;

    try {
        const resp = await fetch(`/api/metadata?caminho=${encodeURIComponent(caminho)}`, { cache: 'no-store' });
        if (!resp.ok) {
            banner.classList.add('hidden');
            return;
        }

        const meta = await resp.json();
        renderizarBannerMetadata(banner, meta);
    } catch (e) {
        console.warn('Erro ao carregar metadata:', e);
        banner.classList.add('hidden');
    }
}

function renderizarBannerMetadata(banner, meta) {
    if (!meta || !meta.titulo) {
        banner.classList.add('hidden');
        return;
    }

    const posterHtml = meta.posterUrl 
        ? `<div class="meta-poster-container"><img src="${escapeHtml(meta.posterUrl)}" alt="${escapeHtml(meta.titulo)}" class="meta-poster-img" onerror="this.src='img/kronos_logo.svg'"></div>`
        : '';

    const scoreHtml = meta.score ? `<span class="meta-badge score"><span class="material-symbols-outlined">star</span> ${meta.score}</span>` : '';
    const anoHtml = meta.ano ? `<span class="meta-badge">${meta.ano}</span>` : '';
    const epsHtml = meta.episodios ? `<span class="meta-badge">${meta.episodios} eps</span>` : '';
    const subTitle = meta.tituloJapones || meta.tituloIngles || '';

    let generosHtml = '';
    if (meta.generos && meta.generos.length > 0) {
        generosHtml = meta.generos.slice(0, 3).map(g => `<span class="meta-badge genre">${escapeHtml(g)}</span>`).join('');
    }

    banner.innerHTML = `
        ${posterHtml}
        <div class="meta-info-container">
            <div class="meta-header-titles">
                <div class="meta-title-main">${escapeHtml(meta.titulo)}</div>
                ${subTitle ? `<div class="meta-title-sub">${escapeHtml(subTitle)}</div>` : ''}
            </div>
            <div class="meta-badges-row">
                ${scoreHtml}
                ${anoHtml}
                ${epsHtml}
                ${generosHtml}
            </div>
            ${meta.sinopse ? `<div class="meta-synopsis">${escapeHtml(meta.sinopse)}</div>` : ''}
        </div>
    `;

    banner.classList.remove('hidden');
}

function inicializarBotoesLimpezaFormularios() {
    document.querySelectorAll('.btn-clear-form').forEach(btn => {
        btn.addEventListener('click', () => {
            const formId = btn.getAttribute('data-form');
            const form = formId ? document.getElementById(formId) : btn.closest('form');
            if (!form) return;

            // Reseta todos os inputs de texto e selects do formulário
            form.querySelectorAll('input[type="text"]').forEach(input => input.value = '');
            form.querySelectorAll('select').forEach(select => {
                select.selectedIndex = 0;
            });

            // Oculta o banner de metadados associado se houver
            const cardParent = form.closest('.glass-card');
            if (cardParent) {
                const banner = cardParent.querySelector('.anime-meta-banner');
                if (banner) banner.classList.add('hidden');
            }
        });
    });
}

function inicializarControlesConsole() {
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('.btn-toggle-console');
        if (!btn) return;

        const targetId = btn.getAttribute('data-target');
        const consoleBody = targetId ? document.getElementById(targetId) : null;
        if (!consoleBody) return;

        consoleBody.classList.toggle('expanded');
        if (consoleBody.classList.contains('expanded')) {
            btn.innerHTML = '<span class="material-symbols-outlined console-action-icon">unfold_less</span>Encolher';
        } else {
            btn.innerHTML = '<span class="material-symbols-outlined console-action-icon">unfold_more</span>Expandir';
        }
    });
}

/**
 * PROPÓSITO DE NEGÓCIO: conecta todos os botões "Procurar..." dos formulários
 * ao seletor nativo centralizado e transfere a escolha para o campo correto.
 *
 * INVARIANTES DO DOMÍNIO: apenas um seletor pode ficar aberto em toda a SPA;
 * cliques concorrentes não podem enfileirar janelas que apareceriam depois.
 *
 * COMPORTAMENTO EM CASO DE FALHA: registra o erro no console, restaura o botão
 * e libera o bloqueio global para que uma nova tentativa possa ser feita.
 */
function inicializarBotoesProcurarCaminho() {
    document.body.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-procurar');
        if (!btn) return;
        if (btn.disabled || seletorCaminhoEmAndamento) return;

        const targetId = btn.getAttribute('data-target');
        const tipo = btn.getAttribute('data-type') || 'pasta';
        // Diretório inicial opcional (ex.: pasta "cache" do projeto na Correção de Cache).
        const dirInicial = btn.getAttribute('data-dir-inicial') || '';
        const inputTarget = document.getElementById(targetId);
        if (!inputTarget) return;

        const textoOriginal = btn.innerHTML;
        btn.innerHTML = '⏳ Abrindo...';
        btn.disabled = true;
        seletorCaminhoEmAndamento = true;

        try {
            const endpointBase = tipo === 'arquivo' ? '/api/dialogo/selecionar-arquivo' : '/api/dialogo/selecionar-pasta';
            const endpoint = dirInicial
                ? `${endpointBase}?dirInicial=${encodeURIComponent(dirInicial)}`
                : endpointBase;
            const res = await fetch(endpoint);
            if (res.ok) {
                const data = await res.json();
                if (data.caminho) {
                    inputTarget.value = data.caminho;
                    inputTarget.dispatchEvent(new Event('input', { bubbles: true }));
                    inputTarget.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
        } catch (err) {
            console.error('Erro ao abrir seletor nativo:', err);
        } finally {
            btn.innerHTML = textoOriginal;
            btn.disabled = false;
            seletorCaminhoEmAndamento = false;
        }
    });
}

/**
 * Menu "Sair": encerra a aplicação inteira (servidor + trabalho em execução).
 * Fluxo: abre o modal de confirmação (avisando se a fila do pipeline está
 * ocupada), chama POST /api/sistema/sair e cobre a tela com o aviso final.
 * A parada do job em execução é cooperativa (próximo ponto seguro).
 */
function inicializarBotaoSair() {
    const btnMenu = document.getElementById('btn-menu-sair');
    const modal = document.getElementById('modal-sair');
    const avisoPipeline = document.getElementById('modal-sair-aviso-pipeline');
    const btnCancelar = document.getElementById('btn-sair-cancelar');
    const btnConfirmar = document.getElementById('btn-sair-confirmar');
    const overlayEncerrado = document.getElementById('overlay-encerrado');
    if (!btnMenu || !modal || !btnCancelar || !btnConfirmar || !overlayEncerrado) return;

    btnMenu.addEventListener('click', async () => {
        modal.classList.remove('hidden');
        if (avisoPipeline) {
            avisoPipeline.classList.add('hidden');
            try {
                const res = await fetch('/api/pipeline/status');
                const data = await res.json();
                if (data.mensagem === 'ocupada') {
                    avisoPipeline.classList.remove('hidden');
                }
            } catch (e) {
                // Status indisponível não impede a confirmação de saída.
            }
        }
    });

    btnCancelar.addEventListener('click', () => modal.classList.add('hidden'));
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.classList.add('hidden');
    });

    btnConfirmar.addEventListener('click', async () => {
        btnConfirmar.disabled = true;
        btnCancelar.disabled = true;
        try {
            const res = await fetch('/api/sistema/sair', { method: 'POST' });
            const data = await res.json();
            mostrarAlerta(data.mensagem || 'Encerrando a aplicação...', 'aviso');
            modal.classList.add('hidden');
            overlayEncerrado.classList.remove('hidden');
            // Navegadores só permitem window.close() em janelas abertas por
            // script; se não fechar, o overlay orienta a fechar manualmente.
            setTimeout(() => window.close(), 1500);
        } catch (err) {
            mostrarAlerta(`Erro ao encerrar a aplicação: ${err.message}`, 'erro');
            btnConfirmar.disabled = false;
            btnCancelar.disabled = false;
        }
    });
}

// Metadados e utilitários de formulário são inicializados no DOMContentLoaded principal acima.
