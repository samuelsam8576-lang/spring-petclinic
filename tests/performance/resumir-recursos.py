#!/usr/bin/env python3
"""Summarize actual Linux /proc samples and an actual k6 JSON."""
from pathlib import Path
import argparse,csv,json,statistics
p=argparse.ArgumentParser()
p.add_argument('--input',required=True);p.add_argument('--k6',required=True);p.add_argument('--output',required=True)
a=p.parse_args()
with open(a.input,newline='') as f:rows=list(csv.DictReader(f))
if not rows:raise SystemExit('CSV sem amostras de recursos')
s={'amostras':len(rows),'duracao_s':float(rows[-1]['elapsed_s']),'recursos':{}}
for col in ['java_cpu_pct_one_core','java_cpu_pct_host_capacity','host_cpu_pct','java_rss_mib','host_memory_used_pct']:
 v=[float(r[col]) for r in rows];s['recursos'][col]={'media':statistics.mean(v),'minimo':min(v),'maximo':max(v)}
m=json.loads(Path(a.k6).read_text())['metrics']
s['carga']={'p95_ms':m['http_req_duration']['p(95)'],'requisicoes':m['http_reqs']['count'],'taxa_erro':m['http_req_failed']['value']}
Path(a.output).write_text(json.dumps(s,ensure_ascii=False,indent=2));print(json.dumps(s,ensure_ascii=False,indent=2))
