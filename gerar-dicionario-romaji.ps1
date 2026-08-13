<#
.SYNOPSIS
    Gera um dicionário hunspell de ROMAJI a partir do IPADIC que veio com o MeCab.

.DESCRIPTION
    Não existe dicionário hunspell de romaji pronto — e ele faz falta. Medido em 13/08/2026:
    palavras em romaji são reprovadas por português, inglês E alemão (6 de 7 em PT, 7 de 7 em EN),
    ou seja, apareceriam TODAS como erro ortográfico. Em anime isso não é caso de borda: letra de
    música em romaji é a regra, e nomes próprios japoneses estão em toda parte.

    O IPADIC traz a leitura em katakana de cada palavra, e katakana -> romaji é conversão
    determinística. Então o dicionário se gera, em vez de se procurar.

    Saída: C:\Hunspell\ja_ROMAJI.dic + .aff, no formato que o hunspell lê como qualquer idioma.

.NOTES
    Pré-requisito: MeCab instalado (o IPADIC vem junto). Sem ele o script para e diz por quê.
    Romanização Hepburn simplificada — a mesma que fansub usa em karaokê.
#>
[CmdletBinding()]
param(
    [string]$Ipadic = 'C:\Program Files (x86)\MeCab\dic\ipadic',
    [string]$Destino = 'C:\Hunspell',
    # Categorias que valem: nome próprio e substantivo cobrem nome de personagem, lugar e o
    # vocabulário de letra de música. Verbo conjugado inflaria o arquivo sem ganho.
    [string[]]$Arquivos = @('Noun.proper.csv', 'Noun.csv', 'Noun.name.csv', 'Noun.place.csv')
)

if (-not (Test-Path -LiteralPath $Ipadic)) {
    Write-Host "IPADIC nao encontrado em $Ipadic — instale o MeCab primeiro." -ForegroundColor Red
    exit 1
}

# Tabela Hepburn. Dígrafos ANTES dos simples: senão "キャ" vira "kiya" em vez de "kya".
$digrafos = [ordered]@{
    'キャ'='kya';'キュ'='kyu';'キョ'='kyo';'シャ'='sha';'シュ'='shu';'ショ'='sho'
    'チャ'='cha';'チュ'='chu';'チョ'='cho';'ニャ'='nya';'ニュ'='nyu';'ニョ'='nyo'
    'ヒャ'='hya';'ヒュ'='hyu';'ヒョ'='hyo';'ミャ'='mya';'ミュ'='myu';'ミョ'='myo'
    'リャ'='rya';'リュ'='ryu';'リョ'='ryo';'ギャ'='gya';'ギュ'='gyu';'ギョ'='gyo'
    'ジャ'='ja';'ジュ'='ju';'ジョ'='jo';'ビャ'='bya';'ビュ'='byu';'ビョ'='byo'
    'ピャ'='pya';'ピュ'='pyu';'ピョ'='pyo';'ファ'='fa';'フィ'='fi';'フェ'='fe';'フォ'='fo'
    'ウィ'='wi';'ウェ'='we';'ウォ'='wo';'ヴァ'='va';'ヴィ'='vi';'ヴェ'='ve';'ヴォ'='vo'
    'ティ'='ti';'ディ'='di';'トゥ'='tu';'ドゥ'='du';'シェ'='she';'ジェ'='je';'チェ'='che'
}
$simples = [ordered]@{
    'ア'='a';'イ'='i';'ウ'='u';'エ'='e';'オ'='o'
    'カ'='ka';'キ'='ki';'ク'='ku';'ケ'='ke';'コ'='ko'
    'サ'='sa';'シ'='shi';'ス'='su';'セ'='se';'ソ'='so'
    'タ'='ta';'チ'='chi';'ツ'='tsu';'テ'='te';'ト'='to'
    'ナ'='na';'ニ'='ni';'ヌ'='nu';'ネ'='ne';'ノ'='no'
    'ハ'='ha';'ヒ'='hi';'フ'='fu';'ヘ'='he';'ホ'='ho'
    'マ'='ma';'ミ'='mi';'ム'='mu';'メ'='me';'モ'='mo'
    'ヤ'='ya';'ユ'='yu';'ヨ'='yo'
    'ラ'='ra';'リ'='ri';'ル'='ru';'レ'='re';'ロ'='ro'
    'ワ'='wa';'ヲ'='o';'ン'='n'
    'ガ'='ga';'ギ'='gi';'グ'='gu';'ゲ'='ge';'ゴ'='go'
    'ザ'='za';'ジ'='ji';'ズ'='zu';'ゼ'='ze';'ゾ'='zo'
    'ダ'='da';'ヂ'='ji';'ヅ'='zu';'デ'='de';'ド'='do'
    'バ'='ba';'ビ'='bi';'ブ'='bu';'ベ'='be';'ボ'='bo'
    'パ'='pa';'ピ'='pi';'プ'='pu';'ペ'='pe';'ポ'='po'
    'ヴ'='vu';'ァ'='a';'ィ'='i';'ゥ'='u';'ェ'='e';'ォ'='o';'ャ'='ya';'ュ'='yu';'ョ'='yo'
}

function ParaRomaji($katakana) {
    if ([string]::IsNullOrWhiteSpace($katakana)) { return $null }
    $s = $katakana
    foreach ($k in $digrafos.Keys) { $s = $s.Replace($k, $digrafos[$k]) }
    # ッ dobra a consoante seguinte (sokuon): ガッツ -> gattsu
    $s = [regex]::Replace($s, 'ッ(.)', { param($m)
        $prox = $m.Groups[1].Value
        $r = if ($simples.Contains($prox)) { $simples[$prox] } else { $prox }
        if ($r -match '^[a-z]') { $r.Substring(0,1) + $r } else { $r } })
    foreach ($k in $simples.Keys) { $s = $s.Replace($k, $simples[$k]) }
    # ー alonga a vogal anterior; em fansub costuma ser simplesmente omitido.
    $s = $s -replace 'ー',''
    # Só sobreviveu romaji puro? Se restou kana, a conversão não é confiável.
    if ($s -match '[^\p{IsBasicLatin}]') { return $null }
    return $s.ToLower()
}

Write-Host "lendo o IPADIC..." -ForegroundColor Cyan
$romaji = [System.Collections.Generic.HashSet[string]]::new()
$lidos = 0
foreach ($nome in $Arquivos) {
    $arq = Join-Path $Ipadic $nome
    if (-not (Test-Path -LiteralPath $arq)) { continue }
    # Shift-JIS (code page 932), e NAO UTF-8: escolher UTF-8 na instalacao converte o dicionario
    # COMPILADO, mas os .csv fonte do IPADIC continuam em SJIS. Lidos como UTF-8 saem 100% em
    # mojibake e o script gera ZERO formas — foi o que aconteceu na primeira execucao.
    foreach ($linha in [System.IO.File]::ReadAllLines($arq, [Text.Encoding]::GetEncoding(932))) {
        $lidos++
        $c = $linha.Split(',')
        if ($c.Count -lt 12) { continue }
        $r = ParaRomaji $c[11]        # coluna da leitura (yomi) em katakana
        if ($r -and $r.Length -ge 3 -and $r -match '^[a-z]+$') { [void]$romaji.Add($r) }
    }
    Write-Host ("  {0,-22} acumulado: {1} formas" -f $nome, $romaji.Count)
}

if ($romaji.Count -eq 0) {
    Write-Host "nenhuma forma gerada — confira o encoding do IPADIC (deve ser UTF-8)." -ForegroundColor Red
    exit 2
}

New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$dic = Join-Path $Destino 'ja_ROMAJI.dic'
$aff = Join-Path $Destino 'ja_ROMAJI.aff'
$ordenado = $romaji | Sort-Object
Set-Content -LiteralPath $dic -Value (@($ordenado.Count) + $ordenado) -Encoding UTF8
Set-Content -LiteralPath $aff -Value @('SET UTF-8', 'TRY aiueokstnhmyrwgzdbpcjf') -Encoding UTF8

Write-Host "`n  gerado: $dic" -ForegroundColor Green
Write-Host ("  {0} formas romaji, de {1} linhas do IPADIC" -f $ordenado.Count, $lidos)

# PROVA DE VIDA com caso-controle: romaji real tem de passar, e invencao tem de reprovar.
$teste = @('^sasageyo','^kimi','^sora','^yakusoku','^zzzqqqinventado')
$saida = ($teste -join "`n") | & hunspell -a -d ja_ROMAJI -i UTF-8 2>&1
$ruins = @($saida | Where-Object { $_ -match '^[&#]\s+(\S+)' } | ForEach-Object { $matches[1] })
Write-Host "`n  prova de vida:" -ForegroundColor Cyan
foreach ($p in @('sasageyo','kimi','sora','yakusoku','zzzqqqinventado')) {
    $ok = -not ($ruins -contains $p)
    Write-Host ("    {0,-18} {1}" -f $p, $(if ($ok) { 'reconhecida' } else { 'NAO reconhecida' }))
}
if ($ruins -notcontains 'zzzqqqinventado') {
    Write-Host "`n  ATENCAO: o caso-controle NAO foi reprovado — o dicionario nao esta sendo lido." -ForegroundColor Red
    exit 2
}
exit 0
