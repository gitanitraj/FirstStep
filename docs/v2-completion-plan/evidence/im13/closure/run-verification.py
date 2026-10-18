"""Bounded IM-13 rerun; records exact commands/exits and input/distribution hashes."""
import datetime,hashlib,json,os,subprocess,time,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[5]
OUT=Path(__file__).resolve().parent
EVIDENCE=OUT.parent
ledger=[]
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,value):(OUT/name).write_text(json.dumps(value,indent=2)+'\n')
def inputs():
 paths=[]
 for base in ['backend/src','app/data','frontend/src','data-cleaning/scripts','data-cleaning/tests']:
  paths.extend(p for p in (ROOT/base).rglob('*') if p.is_file() and '__pycache__' not in p.parts and 'app-next' not in p.parts)
 for n in ['backend/pom.xml','backend/Dockerfile','.dockerignore','frontend/package.json','frontend/package-lock.json','frontend/index.html','frontend/tsconfig.json','frontend/tsconfig.node.json','frontend/vite.config.ts','frontend/vitest.config.ts']:
  if (ROOT/n).is_file():paths.append(ROOT/n)
 paths+=list(EVIDENCE.glob('*.py'))+list(EVIDENCE.glob('*.mjs'))+[Path(__file__),OUT/'http-baseline.json',OUT/'inventory.json']
 return {str(p.relative_to(ROOT)):digest(p) for p in sorted(set(paths))}
def run(name,args,cwd=ROOT,env=None):
 record={'name':name,'command':args,'cwd':str(cwd),'environmentOverrides':env or {},'startedUtc':now(),'log':name+'.txt'}
 with (OUT/(name+'.txt')).open('w') as log:
  proc=subprocess.run(args,cwd=cwd,env={**os.environ,**(env or {})},stdout=log,stderr=subprocess.STDOUT)
 record.update(endedUtc=now(),exitCode=proc.returncode,outputSha256=digest(OUT/(name+'.txt')))
 ledger.append(record);write('commands.json',ledger);print(name,proc.returncode,flush=True)
 if proc.returncode:raise RuntimeError(name+' failed; see log')
 return (OUT/(name+'.txt')).read_text()
before=inputs();write('inputs-before.json',{'capturedUtc':now(),'sha256':before})
env={'IM13_OUTPUT_DIR':str(OUT),'IM13_ORIGIN':'http://127.0.0.1:8133'}
container='firststep-im13-closure-'+str(os.getpid())
started=False
try:
 run('backend-verify',['/Users/composedmedia/tools/apache-maven-3.9.16/bin/mvn','-B','-Dim12.evidence.dir='+str(OUT),'-Dim12.phase=closure','verify'],ROOT/'backend')
 run('frontend-tests',['npm','test'],ROOT/'frontend',env={'NODE_OPTIONS':'--no-experimental-webstorage'})
 run('frontend-typecheck',['./node_modules/.bin/tsc','--noEmit'],ROOT/'frontend')
 run('frontend-build',['npm','run','build'],ROOT/'frontend')
 run('python-tests',['python3','-m','unittest','discover','-s','data-cleaning/tests','-v'])
 for name in ['content_sources','homepage','navigation','news','organizations','schema']:
  run('validator-'+name,['python3','data-cleaning/scripts/validate_'+name+'.py'])
 run('docker-build',['docker','build','-f','backend/Dockerfile','-t','firststep-im13:closure','.'])
 run('docker-image',['docker','image','inspect','firststep-im13:closure'])
 run('docker-start',['docker','run','--detach','--name',container,'--publish','127.0.0.1:8133:8080','firststep-im13:closure','--news.rss.initial-delay=86400000']);started=True
 # Readiness remains local and read-only; record process exits for each probe.
 for attempt in range(60):
  probe=subprocess.run(['curl','--fail','--silent','http://127.0.0.1:8133/api/categories'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
  if probe.returncode==0:break
  time.sleep(1)
 else:raise RuntimeError('Packaged readiness timeout')
 run('docker-copy-distribution',['docker','cp',container+':/app/app.jar',str(OUT/'distribution.jar')])
 run('http-release',['python3',str(EVIDENCE/'http-release.py')],env=env)
 run('packaged-manifests',['python3',str(EVIDENCE/'packaged-check.py')],env=env)
 run('editor-and-expiry',['python3',str(EVIDENCE/'editor-workflow.py')],env=env)
 run('browser-release',['node',str(EVIDENCE/'browser-check.mjs')],env=env)
 run('browser-probe',['node',str(EVIDENCE/'browser-probe.mjs'),'after'],env=env)
 run('docker-runtime',['docker','logs',container])
 # Match served production assets to the freshly copied packaged jar.
 from urllib.request import urlopen
 assets={}
 with zipfile.ZipFile(OUT/'distribution.jar') as jar:
  for name in jar.namelist():
   if name.startswith('BOOT-INF/classes/static/app-next/') and not name.endswith('/'):
    url='http://127.0.0.1:8133/'+name.removeprefix('BOOT-INF/classes/static/')
    expected=hashlib.sha256(jar.read(name)).hexdigest()
    with urlopen(url,timeout=10) as response:actual=hashlib.sha256(response.read()).hexdigest()
    assert expected==actual,(url,'served asset mismatch');assets[name]=expected
 write('distribution-hashes.json',{'capturedUtc':now(),'localExecutableJar':digest(ROOT/'backend/target/firststep-backend-0.1.0.jar'),'dockerExecutableJar':digest(OUT/'distribution.jar'),'frontendDist':{str(p.relative_to(ROOT/'frontend/dist')):digest(p) for p in (ROOT/'frontend/dist').rglob('*') if p.is_file()},'packagedAssetsMatchLiveHttp':assets})
finally:
 if started:
  try:run('docker-stop',['docker','stop',container])
  except Exception as error:print(error,flush=True)
 after=inputs();write('inputs-after.json',{'capturedUtc':now(),'sha256':after})
 changes=[k for k in sorted(set(before)|set(after)) if before.get(k)!=after.get(k)]
 write('binding-result.json',{'status':'PASS' if not changes else 'FAIL','changedInputs':changes,'inputCount':len(before),'note':'Overall gate success also requires all commands exit 0 and completed distribution hashes.'})
 assert not changes,changes
