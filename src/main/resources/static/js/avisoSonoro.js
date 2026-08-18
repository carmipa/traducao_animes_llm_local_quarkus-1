/**
 * AVISO SONORO DE FIM DE LOTE — compartilhado entre as telas que rodam trabalho longo.
 *
 * PROPÓSITO DE NEGÓCIO: pedido de Paulo (2026-08-14) para a Tradução Local, e estendido à
 * Revisão de Lore em 17/08/2026: o lote leva de minutos a horas, quem dispara vai fazer outra
 * coisa, e três toques no fim chamam de volta.
 *
 * POR QUE MORA AQUI, e não copiado em cada tela: invariante 10 do projeto — componente repetido
 * vira compartilhado. Duas cópias do mesmo áudio divergiriam no dia em que uma ganhasse ajuste
 * (volume, número de toques) e a outra não, e ninguém perceberia até o lote errado avisar
 * diferente.
 *
 * POR QUE O CANAL NASCE NO CLIQUE, e não na hora de tocar: o navegador bloqueia áudio que a
 * página inicia sozinha. Um AudioContext criado fora de um gesto do usuário nasce 'suspended', e
 * `oscillator.start()` não emite nada — sem erro, sem log. O clique que dispara o trabalho É o
 * gesto; é ali que ele é criado.
 *
 * INVARIANTES: um único AudioContext por página, reaproveitado entre as telas; `armar` devolve
 * TRÊS estados, nunca dois.
 *
 * COMPORTAMENTO EM CASO DE FALHA: navegador sem AudioContext devolve 'indisponivel' e nunca
 * lança — o alerta visual da tela é a rede que não depende de permissão nenhuma.
 */

let contextoAudio = null;

export const AVISO_TOQUES = 3;
const AVISO_INTERVALO_S = 0.55;
const AVISO_FREQUENCIA_HZ = 880;

/**
 * Prepara o canal de áudio e devolve o estado REAL, em três valores — 'armado',
 * 'nao-confirmado' e 'indisponivel'.
 *
 * Dois valores não bastariam: "o navegador ainda não liberou" e "este navegador não faz som"
 * levam a ações diferentes, e tratá-los como o mesmo "não" esconderia justamente o caso
 * recuperável. É a mesma regra dos três estados das guardas do projeto.
 */
export function armarAvisoSonoro() {
    const Construtor = window.AudioContext || window.webkitAudioContext;
    if (!Construtor) return 'indisponivel';

    try {
        if (!contextoAudio) contextoAudio = new Construtor();
        // resume() e assincrono: o estado logo depois ainda pode ser 'suspended'. Por isso o
        // veredito abaixo LE o estado atual em vez de presumir sucesso.
        if (contextoAudio.state === 'suspended') contextoAudio.resume().catch(() => {});
        return contextoAudio.state === 'running' ? 'armado' : 'nao-confirmado';
    } catch (e) {
        return 'indisponivel';
    }
}

/**
 * Toca os três avisos. Devolve `false` quando não havia canal de áudio — o chamador ainda mostra
 * o alerta visual, que é a rede que nunca depende de permissão do navegador.
 */
export function tocarAvisoSonoro() {
    if (!contextoAudio) return false;
    if (contextoAudio.state === 'suspended') contextoAudio.resume().catch(() => {});

    const inicio = contextoAudio.currentTime;
    for (let i = 0; i < AVISO_TOQUES; i++) {
        const t = inicio + i * AVISO_INTERVALO_S;
        const oscilador = contextoAudio.createOscillator();
        const ganho = contextoAudio.createGain();
        oscilador.type = 'sine';
        oscilador.frequency.value = AVISO_FREQUENCIA_HZ;
        // Envelope curto: subir e descer o volume evita o estalo do corte seco.
        ganho.gain.setValueAtTime(0.0001, t);
        ganho.gain.exponentialRampToValueAtTime(0.25, t + 0.02);
        ganho.gain.exponentialRampToValueAtTime(0.0001, t + 0.35);
        oscilador.connect(ganho).connect(contextoAudio.destination);
        oscilador.start(t);
        oscilador.stop(t + 0.4);
    }
    return true;
}

/**
 * A frase que a tela escreve no console sobre o estado do aviso. Compartilhada para que as duas
 * telas digam a MESMA coisa — quem vai sair de perto precisa saber ANTES se pode confiar no som;
 * descobrir que ele não tocaria só depois de perder o fim do lote é o pior desfecho.
 */
export function mensagemDoAviso(estado) {
    return {
        'armado': `Aviso sonoro ARMADO: ${AVISO_TOQUES} toques ao fim do lote.`,
        'nao-confirmado': 'Aviso sonoro NÃO CONFIRMADO: o navegador ainda não liberou o áudio. O alerta na tela vale de qualquer forma.',
        'indisponivel': 'Aviso sonoro INDISPONÍVEL neste navegador. Só haverá alerta na tela.'
    }[estado];
}
