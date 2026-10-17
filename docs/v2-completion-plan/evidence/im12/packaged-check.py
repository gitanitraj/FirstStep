"""Isolated real executable-jar deployment: three full catalog copies, read-only HTTP, built SPA.
No original catalog edits, remote providers or feed refresh. Terminates every child on exit.
"""
import json
import shutil
import subprocess
import tempfile
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import urlopen, Request
from urllib.error import HTTPError, URLError

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[3]
JAVA = '/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java'
JAR = ROOT / 'backend/target/firststep-backend-0.1.0.jar'
PRIVATE = {'verified','status','expirationDate','createdDate','editorialReview','helpAccess','retrieved','authoredCategoryTags','authoredSubcategory','diagnostics','tieStage','generatedBy'}
def check(node):
    if isinstance(node,dict):
        assert not PRIVATE.intersection(node),PRIVATE.intersection(node)
        for value in node.values(): check(value)
    elif isinstance(node,list):
        for value in node: check(value)
def request(path, data=None):
    try:
        req = Request('http://127.0.0.1:8112'+path,data=None if data is None else json.dumps(data).encode(),headers={'Content-Type':'application/json'})
        with urlopen(req,timeout=10) as r: return r.status,json.load(r)
    except HTTPError as e: return e.code,json.load(e)
def stop(p):
    if p.poll() is None:
        p.terminate()
        try:p.wait(timeout=10)
        except subprocess.TimeoutExpired:p.kill();p.wait()

def wait_ready(p):
    for _ in range(100):
        if p.poll() is not None:return False
        try:
            with urlopen('http://127.0.0.1:8112/api/health',timeout=1) as r:
                if r.status==200:return True
        except (URLError,TimeoutError): pass
        time.sleep(.2)
    raise AssertionError('Packaged startup timeout')

result={'clock':datetime.now(timezone.utc).isoformat(),'scenarios':{},'responses':{}}
with tempfile.TemporaryDirectory(prefix='im12-packaged-') as temporary:
    for scenario in ['positive','missing','malformed']:
        data=Path(temporary)/scenario;shutil.copytree(ROOT/'app/data',data)
        manifest=data/'first-rate-news-membership.json'
        if scenario=='missing':manifest.unlink()
        if scenario=='malformed':manifest.write_text('[invalid')
        command=[JAVA,'-jar',str(JAR),'--server.address=127.0.0.1','--server.port=8112','--news.rss.initial-delay=86400000','--app.data.dir='+str(data)]
        with (OUT/f'packaged-{scenario}.log').open('w') as log:
            process=subprocess.Popen(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            try:
                ready=wait_ready(process)
                if scenario=='malformed':
                    assert not ready and process.returncode!=0
                    assert 'Cannot read First Rate News admissions' in (OUT/f'packaged-{scenario}.log').read_text()
                    result['scenarios'][scenario]={'ready':False,'exitCode':process.returncode,'residentServing':False};continue
                assert ready,scenario
                status,page=request('/api/category/housing?communityId=wilmington-de');assert status==200;check(page)
                ids=next(c['items'] for c in page['data']['collections'] if c['key']=='first-rate-news')
                ids=[i['id'] for i in ids]
                assert ids==(['OR-003','FAQ-002','EA-001','EA-005'] if scenario=='positive' else ['OR-003']),ids
                result['scenarios'][scenario]={'ready':True,'firstRateNewsIds':ids,'dataSelection':'explicit absolute app.data.dir copied full catalog'}
                if scenario=='missing':continue
                paths=['/api/resources','/api/resources/HA-001','/api/faqs','/api/faqs/FAQ-002','/api/expert-answers','/api/expert-answers/EA-001','/api/flyers/FL-008','/api/search?q=housing','/api/categories','/api/originals/OR-003','/api/news','/api/news/rss','/api/community-notices/events','/api/community-notices/flyers','/api/updates','/api/find-help','/api/home']
                for path in paths:
                    status,body=request(path);assert status==200,(path,status);check(body);result['responses'][path]={'status':status,'body':body}
                inventory=json.loads((OUT/'inventory.json').read_text())
                for owner,counts in inventory['communities'].items():
                    path='/api/resources?communityId='+owner;status,body=request(path);check(body)
                    assert status==200 and len(body['data'])==counts['Resource'];assert all(r['communityId']==owner for r in body['data'])
                    result['responses'][path]={'status':status,'ids':[r['id'] for r in body['data']]}
                for path in ['/api/resources','/api/faqs','/api/expert-answers','/api/flyers']:
                    status,body=request(path+'?communityId=unknown');assert status==200 and body['data']==[]
                for path in ['/api/resources/HA-001','/api/faqs/FAQ-002','/api/expert-answers/EA-001','/api/flyers/FL-008','/api/originals/OR-003']:
                    status,body=request(path+'?communityId=bear-de');assert status==404;check(body)
                for record in ['OR-001','OR-002','missing']:
                    status,body=request('/api/originals/'+record);assert status==404 and body['errorMessage']=='Article not found: '+record;check(body)
                status,body=request('/api/decide?communityId=bear-de',{'userQuery':'food'});assert status==200;check(body)
                assert body['data']['notes']=='AI call failed. Please try again later.';result['responses']['POST /api/decide?communityId=bear-de']={'status':status,'body':body}
                with (OUT/'built-server.log').open('w') as frontend_log:
                    frontend=subprocess.Popen(['python3',str(OUT/'serve-built.py')],cwd=ROOT,stdout=frontend_log,stderr=subprocess.STDOUT)
                    try:
                        for _ in range(50):
                            try:
                                with urlopen('http://127.0.0.1:4182/app-next/') as r:break
                            except URLError:time.sleep(.1)
                        with (OUT/'browser-check.txt').open('w') as browser_log:
                            run=subprocess.run(['node',str(OUT/'browser-check.mjs')],cwd=ROOT,stdout=browser_log,stderr=subprocess.STDOUT,timeout=90)
                        assert run.returncode==0,(OUT/'browser-check.txt').read_text()
                    finally:stop(frontend)
            finally:stop(process)
result['status']='PASS'
(OUT/'packaged-http.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: packaged positive/missing/malformed manifests, privacy, eight Community partitions, guarded details, built browser provider/Food navigation')
