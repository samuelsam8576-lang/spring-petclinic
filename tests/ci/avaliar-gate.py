#!/usr/bin/env python3
"""Evaluate actual reports; missing output and execution errors fail closed."""
from pathlib import Path
import json,sys,xml.etree.ElementTree as ET

out=Path('resultados'); out.mkdir(exist_ok=True)
checks=[]
def check(name,ok,detail): checks.append({'criterio':name,'atendido':bool(ok),'resultado':detail})
suites=[]
for p in Path('target/surefire-reports').glob('TEST-*.xml'):
    suites.append(ET.parse(p).getroot())
counts={k:sum(int(s.get(k,0)) for s in suites) for k in ('tests','failures','errors','skipped')}
e2e=sum(int(s.get('tests',0)) for s in suites if '.e2e.' in s.get('name',''))
check('Relatorios de testes presentes',counts['tests']>0 and e2e>0,f"{counts}; E2E={e2e}")
check('Nenhuma falha ou erro',counts['failures']==0 and counts['errors']==0,counts)
coverage={}
p=Path('target/site/jacoco/jacoco.xml')
if p.exists():
    root=ET.parse(p).getroot()
    for name,el in [('total',root)]+[('owner',x) for x in root.findall('package') if x.get('name','').endswith('/owner')]:
        c=next((x for x in el.findall('counter') if x.get('type')=='BRANCH'),None)
        if c is not None:
            hit=int(c.get('covered'));total=hit+int(c.get('missed'));coverage[name]=hit/total if total else None
for name,limit in [('total',.75),('owner',.85)]:
    value=coverage.get(name);check('Cobertura de desvios '+name,value is not None and value>=limit,{'medido':value,'minimo':limit})
p=Path('target/pmd.xml'); violations=[]; errors=[]
if p.exists():
    root=ET.parse(p).getroot()
    violations=[{'regra':x.get('rule'),'prioridade':int(x.get('priority','5')),'linha':x.get('beginline'),'mensagem':(x.text or '').strip()} for x in root.iter() if x.tag.split('}')[-1]=='violation']
    errors=[(x.text or '').strip() for x in root.iter() if x.tag.split('}')[-1] in ('error','configerror')]
critical=[x for x in violations if x['prioridade']<=3]
check('PMD executado sem erro de analise',p.exists() and not errors,{'arquivo':p.exists(),'erros':errors})
check('PMD sem violacao de prioridade 1 a 3',p.exists() and not critical,{'total':len(violations),'prioridades_1_a_3':len(critical)})
for phase in ['maven','pmd','selenium','newman','k6']:
    p=out/(phase+'.exitcode'); code=int(p.read_text().strip()) if p.exists() else None
    check('Execucao '+phase,code==0,code)
summary={'status':'APROVADO' if all(c['atendido'] for c in checks) else 'REPROVADO','testes':counts,'e2e':e2e,'cobertura':coverage,'pmd':{'violacoes':violations,'erros':errors},'criterios':checks}
(out/'quality-gate.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
lines=['# Quality gate da execucao real','',f"Resultado: **{summary['status']}**",'','| Criterio | Atendido | Resultado |','| --- | --- | --- |']
for c in checks:lines.append('| '+c['criterio']+' | '+('Sim' if c['atendido'] else 'Nao')+' | '+str(c['resultado']).replace('|','/')+' |')
(out/'QUALITY_GATE.md').write_text('\n'.join(lines))
print('\n'.join(lines))
sys.exit(0 if summary['status']=='APROVADO' else 1)
