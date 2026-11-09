#!/usr/bin/env python3
"""Gera, em Markdown, o resumo da rodada 2 a partir dos arquivos produzidos pelo workflow.

Ele só LÊ os arquivos de saída das ferramentas (Maven, JaCoCo, k6, ZAP, coletor de segurança).
Nada é estimado: o que não existir aparece como "não encontrado".
Uso, na raiz do repositório:  python3 tests/relatorio/resumo.py
"""
import csv
import glob
import json
import os
import statistics
import subprocess
import xml.etree.ElementTree as ET


def cmd(comando):
    try:
        r = subprocess.run(comando, shell=True, capture_output=True, text=True, timeout=30)
        saida = r.stdout.strip().splitlines()
        return saida[0] if r.returncode == 0 and saida else "não disponível"
    except Exception:
        return "não disponível"


def pct(cobertas, perdidas):
    total = cobertas + perdidas
    return "sem itens" if total == 0 else f"{100 * cobertas / total:.1f}%".replace(".", ",")


def num(x, casas=2):
    return f"{x:.{casas}f}".replace(".", ",") if isinstance(x, (int, float)) else "n/d"


L = ["# Resumo da rodada 2", "",
     "Estes números vêm direto dos arquivos gerados pelas ferramentas. Copie-os para o relatório e para a planilha.", ""]

# ------------------------------------------------------------------ ambiente
mem = "n/d"
try:
    for linha in open("/proc/meminfo"):
        if linha.startswith("MemTotal"):
            mem = f"{int(linha.split()[1]) / 1024 / 1024:.1f} GiB".replace(".", ",")
            break
except OSError:
    pass
L += ["## Ambiente de execução (para o Quadro 7 do relatório)", "",
      "| Item | Valor |", "| --- | --- |",
      f"| Commit testado | {cmd('git rev-parse --short HEAD')} |",
      f"| Sistema operacional | {cmd('. /etc/os-release && echo $PRETTY_NAME')} (kernel {cmd('uname -r')}) |",
      f"| Processador | {cmd("lscpu | grep -m1 'Model name' | cut -d: -f2 | xargs")} |",
      f"| Núcleos lógicos | {cmd('nproc')} |", f"| Memória RAM | {mem} |",
      f"| Java | {cmd('java -version 2>&1 | head -1')} |",
      f"| Navegador | {cmd('google-chrome --version')} |", ""]

# ------------------------------------------------------------------ testes (Surefire)
grupos = {"Maven (unitários e integração)": [0, 0, 0, 0], "Selenium (testes de navegador)": [0, 0, 0, 0]}
problemas = []
for arq in sorted(glob.glob("target/surefire-reports/TEST-*.xml")):
    try:
        raiz = ET.parse(arq).getroot()
    except ET.ParseError:
        continue
    nome = raiz.get("name", "")
    g = grupos["Selenium (testes de navegador)" if nome.endswith("E2E") else "Maven (unitários e integração)"]
    g[0] += int(raiz.get("tests", 0)); g[1] += int(raiz.get("failures", 0))
    g[2] += int(raiz.get("errors", 0)); g[3] += int(raiz.get("skipped", 0))
    for caso in raiz.iter("testcase"):
        for tipo, rot in (("failure", "FALHOU"), ("error", "ERRO"), ("skipped", "IGNORADO")):
            el = caso.find(tipo)
            if el is not None:
                msg = (el.get("message") or "").replace("\n", " ")[:110]
                problemas.append(f"| {rot} | `{caso.get('classname', '').split('.')[-1]}.{caso.get('name')}` | {msg} |")
L += ["## Testes automatizados (Surefire)", "", "| Grupo | Total | Aprovados | Falhas | Erros | Ignorados |", "| --- | --- | --- | --- | --- | --- |"]
if any(sum(v) for v in grupos.values()):
    for nome, (t, f, e, s) in grupos.items():
        L.append(f"| {nome} | {t} | {t - f - e - s} | {f} | {e} | {s} |")
    if problemas:
        L += ["", "Testes com falha, erro ou ignorados:", "", "| Situação | Teste | Mensagem |", "| --- | --- | --- |"] + problemas
else:
    L.append("| relatórios do Surefire não encontrados | | | | | |")
L.append("")

# ------------------------------------------------------------------ cobertura (JaCoCo)
L += ["## Cobertura de código (JaCoCo)", ""]
csv_jacoco = "target/site/jacoco/jacoco.csv"
if os.path.exists(csv_jacoco):
    acum = {}
    with open(csv_jacoco, newline="", encoding="utf-8") as f:
        for linha in csv.DictReader(f):
            pacote = linha["PACKAGE"].split(".")[-1]
            for chave in (pacote, "TOTAL DO PROJETO"):
                a = acum.setdefault(chave, [0, 0, 0, 0])
                a[0] += int(linha["LINE_COVERED"]); a[1] += int(linha["LINE_MISSED"])
                a[2] += int(linha["BRANCH_COVERED"]); a[3] += int(linha["BRANCH_MISSED"])
    L += ["| Pacote | Linhas | Cobertura de linhas | Desvios | Cobertura de desvios |", "| --- | --- | --- | --- | --- |"]
    for pacote in sorted(acum, key=lambda p: (p == "TOTAL DO PROJETO", p)):
        lc, lm, bc, bm = acum[pacote]
        L.append(f"| {pacote} | {lc}/{lc + lm} | {pct(lc, lm)} | {bc}/{bc + bm} | {pct(bc, bm)} |")
else:
    L.append("Arquivo `jacoco.csv` não encontrado.")
L.append("")

# ------------------------------------------------------------------ carga (k6)
def metricas_k6(caminho):
    try:
        m = json.load(open(caminho, encoding="utf-8")).get("metrics", {})
    except (OSError, ValueError):
        return None
    d, r, f = m.get("http_req_duration", {}), m.get("http_reqs", {}), m.get("http_req_failed", {})
    falhas = f.get("value")
    if falhas is None and (f.get("passes", 0) + f.get("fails", 0)):
        falhas = f["passes"] / (f["passes"] + f["fails"])
    return {"media": d.get("avg"), "p95": d.get("p(95)"), "p99": d.get("p(99)"), "reqs": r.get("count"),
            "taxa": r.get("rate"), "falhas": falhas}


L += ["## Teste de carga (k6)", ""]
carga = [(os.path.basename(p), metricas_k6(p)) for p in sorted(glob.glob("resultados/k6-carga-*.json"))]
carga = [(n, m) for n, m in carga if m]
estresse = metricas_k6("resultados/k6-estresse.json")
if carga or estresse:
    L += ["| Execução | Requisições | Req/s | Média (ms) | p95 (ms) | p99 (ms) | Falhas |", "| --- | --- | --- | --- | --- | --- | --- |"]
    for n, m in carga + ([("estresse (até 200 usuários)", estresse)] if estresse else []):
        falhas = f"{100 * m['falhas']:.2f}%".replace(".", ",") if m["falhas"] is not None else "n/d"
        L.append(f"| {n.replace('.json', '')} | {m['reqs']} | {num(m['taxa'], 1)} | {num(m['media'])} | {num(m['p95'])} | {num(m['p99'])} | {falhas} |")
    p95s = [m["p95"] for _, m in carga if m["p95"] is not None]
    if len(p95s) >= 2:
        L += ["", f"**Mediana do p95 das {len(p95s)} execuções de carga: {num(statistics.median(p95s))} ms.**"]
else:
    L.append("Resumos do k6 não encontrados.")
L.append("")

# ------------------------------------------------------------------ CT09 e CT10 (coletor)
L += ["## Segurança: CT09 e CT10 (coletor)", ""]
tabelas = sorted(glob.glob("evidencias/seguranca-*/resumo.csv"))
if tabelas:
    for t in tabelas:
        L += [f"Pasta `{os.path.basename(os.path.dirname(t))}`:", "", "| Caso | Rota | Status HTTP |", "| --- | --- | --- |"]
        for linha in open(t, encoding="utf-8").read().splitlines()[1:]:
            c = linha.split(";")
            if len(c) == 3:
                L.append(f"| {c[0]} | `{c[1]}` | {c[2]} |")
        L.append("")
else:
    L += ["Arquivos do coletor não encontrados.", ""]

# ------------------------------------------------------------------ ZAP
RISCO = {"3": "Alto", "2": "Médio", "1": "Baixo", "0": "Informativo"}
L += ["## Varredura OWASP ZAP", ""]
achou = False
for rotulo, caminho in (("Baseline (passiva)", "zap/zap-baseline.json"), ("Full scan (ativa)", "zap/zap-full.json")):
    try:
        dados = json.load(open(caminho, encoding="utf-8"))
    except (OSError, ValueError):
        continue
    achou = True
    sites = dados.get("site", [])
    sites = sites if isinstance(sites, list) else [sites]
    alertas = [a for s in sites for a in s.get("alerts", [])]
    cont = {k: 0 for k in RISCO}
    for a in alertas:
        cont[str(a.get("riskcode", "0"))] = cont.get(str(a.get("riskcode", "0")), 0) + 1
    L += [f"**{rotulo}:** " + ", ".join(f"{RISCO[k]} {cont[k]}" for k in ("3", "2", "1", "0")), "",
          "| Risco | Alerta | Ocorrências |", "| --- | --- | --- |"]
    for a in sorted(alertas, key=lambda x: -int(x.get("riskcode", 0))):
        L.append(f"| {RISCO.get(str(a.get('riskcode')), '?')} | {a.get('name', a.get('alert', ''))} | {a.get('count', '')} |")
    L.append("")
if not achou:
    L += ["Relatórios JSON do ZAP não encontrados.", ""]

# ------------------------------------------------------------------ prints
L += ["## Prints e arquivos de evidência", ""]
prints = sorted(glob.glob("target/evidencias/*.png"))
L += [f"- Prints do Selenium: {', '.join(os.path.basename(p) for p in prints)}" if prints else "- Prints do Selenium: nenhum encontrado (os testes de navegador não geraram prints)."]
L += ["- Relatórios do ZAP em HTML: `zap-baseline.html` e `zap-full.html`, na pasta `zap/` do pacote para download.",
      "- Relatório de cobertura: pasta `jacoco/`, abra o `index.html`.",
      "- Logs completos: pasta `evidencias/` (`maven-verify.log`, `selenium.log`, `newman.log`, `k6-*.log`, `app-*.log`)."]
print("\n".join(L))
