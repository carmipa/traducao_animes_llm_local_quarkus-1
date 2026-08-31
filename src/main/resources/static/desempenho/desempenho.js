/**
 * Painel "Desempenho": mostra a ÚLTIMA medição de tempo por operação, gravada pelo
 * MedicaoDesempenhoDoPipelineIT em relatorios/desempenho.json e servida por
 * /api/desempenho (ver DesempenhoController).
 *
 * Esta tela NÃO mede. Medir custa CPU e disco na mesma máquina que traduz, e o número sairia
 * contaminado justamente quando alguém quisesse confiar nele — além de que a medição roda pelo
 * Gradle, e compilar mata tradução em andamento.
 *
 * Três estados, e o terceiro não é o primeiro: "nunca mediram" (404) mostra o comando para medir;
 * "medição vazia" mostra tabela vazia com a data; erro mostra a causa. Um estado só faria
 * "ainda não rodou" e "rodou e não achou nada" chegarem iguais na tela.
 */

const OPERACAO_DESTACADA = /elo ·/;

function formatarSegundos(s) {
    if (s === null || s === undefined) return '—';
    return s < 0.1 ? `${(s * 1000).toFixed(0)} ms` : `${s.toFixed(1)} s`;
}

function formatarPorUnidade(ms) {
    if (ms === null || ms === undefined) return '—';
    if (ms < 0.01) return `${(ms * 1000).toFixed(1)} µs`;
    return `${ms.toFixed(3)} ms`;
}

function formatarData(iso) {
    if (!iso) return 'data desconhecida';
    try {
        return new Date(iso).toLocaleString('pt-BR');
    } catch {
        return iso;
    }
}

export function initDesempenho() {
    const painel = document.getElementById('panel-desempenho');
    if (!painel) return;

    const elMeta    = document.getElementById('desempenho-meta');
    const elVazio   = document.getElementById('desempenho-vazio');
    const elErro    = document.getElementById('desempenho-erro');
    const elErroMsg = document.getElementById('desempenho-erro-msg');
    const elWrapper = document.getElementById('desempenho-tabela-wrapper');
    const elCorpo   = document.getElementById('desempenho-corpo');

    function mostrar(estado) {
        elVazio.style.display   = estado === 'vazio'  ? '' : 'none';
        elErro.style.display    = estado === 'erro'   ? '' : 'none';
        elWrapper.style.display = estado === 'tabela' ? '' : 'none';
    }

    let jaCarregou = false;

    async function carregar() {
        try {
            const resposta = await fetch('/api/desempenho');
            if (resposta.status === 404) {
                elMeta.textContent = '';
                mostrar('vazio');
                return;
            }
            if (!resposta.ok) {
                elErroMsg.textContent = `O servidor respondeu HTTP ${resposta.status}.`;
                mostrar('erro');
                return;
            }
            const dados = await resposta.json();
            const medidas = Array.isArray(dados.medidas) ? dados.medidas : [];

            elMeta.textContent =
                `Medido em ${formatarData(dados.medidoEm)} · máquina ${dados.maquina || '?'}`
                + ` · Java ${dados.java || '?'} · ${dados.processadores || '?'} processadores`;

            elCorpo.innerHTML = '';
            for (const m of medidas) {
                const tr = document.createElement('tr');
                if (OPERACAO_DESTACADA.test(m.operacao)) tr.classList.add('linha-secundaria');
                const celulas = [
                    m.operacao,
                    String(m.unidades ?? '—'),
                    formatarSegundos(m.segundos),
                    formatarPorUnidade(m.msPorUnidade),
                    m.observacao || ''
                ];
                celulas.forEach((valor, i) => {
                    const td = document.createElement('td');
                    // textContent, nunca innerHTML: a observação vem de um arquivo em disco, e
                    // conteúdo de arquivo não se injeta como HTML numa página.
                    td.textContent = valor;
                    if (i === 1 || i === 2 || i === 3) td.classList.add('num');
                    tr.appendChild(td);
                });
                elCorpo.appendChild(tr);
            }
            mostrar('tabela');
        } catch (e) {
            elErroMsg.textContent = `Não foi possível carregar a medição: ${e.message}`;
            mostrar('erro');
        }
    }

    // Carrega quando o painel abre pela primeira vez, e não no boot: a página inteira sobe mais
    // rápido, e quem nunca abre esta tela não paga a requisição.
    const botao = document.getElementById('btn-menu-desempenho');
    if (botao) {
        botao.addEventListener('click', () => {
            if (!jaCarregou) {
                jaCarregou = true;
                carregar();
            }
        });
    }
}
