#!/usr/bin/env python3
"""One-second Linux /proc samples for the Java process and the runner host."""
import argparse,csv,os,signal,time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--pid',type=int,required=True);p.add_argument('--output',required=True);p.add_argument('--interval',type=float,default=1);a=p.parse_args()
running=True
def stop(*_):
    global running
    running=False
signal.signal(signal.SIGTERM,stop);signal.signal(signal.SIGINT,stop)
hz=os.sysconf(os.sysconf_names['SC_CLK_TCK']); ncpu=os.cpu_count() or 1
def sample():
    # The executable name can contain spaces: fields start after the last ')'.
    fields=Path(f'/proc/{a.pid}/stat').read_text().rsplit(')',1)[1].split()
    ticks=int(fields[11])+int(fields[12])
    status=dict(line.split(':',1) for line in Path(f'/proc/{a.pid}/status').read_text().splitlines() if ':' in line)
    host=list(map(int,Path('/proc/stat').read_text().splitlines()[0].split()[1:])); total=sum(host[:8]);idle=host[3]+host[4]
    mem={k:int(v.strip().split()[0]) for k,v in (line.split(':',1) for line in Path('/proc/meminfo').read_text().splitlines())}
    return ticks,total,idle,int(status['VmRSS'].split()[0])/1024,int(status['Threads'].strip()),100*(1-mem['MemAvailable']/mem['MemTotal'])
Path(a.output).parent.mkdir(parents=True,exist_ok=True)
with open(a.output,'w',newline='') as f:
    w=csv.writer(f);w.writerow(['elapsed_s','java_cpu_pct_one_core','java_cpu_pct_host_capacity','host_cpu_pct','java_rss_mib','threads','host_memory_used_pct'])
    before=sample();start=last=time.monotonic()
    while running:
        time.sleep(a.interval)
        try:cur=sample()
        except (FileNotFoundError,ProcessLookupError):break
        now=time.monotonic();dt=now-last;cpu=(cur[0]-before[0])/hz/dt*100;hostcpu=100*(1-(cur[2]-before[2])/max(1,cur[1]-before[1]))
        w.writerow([round(now-start,3),round(cpu,3),round(cpu/ncpu,3),round(hostcpu,3),round(cur[3],3),cur[4],round(cur[5],3)]);f.flush();before=cur;last=now
