"""IM13 J5/J6: actual external JSON authoring, validator, executable reload and read-only CLI.
Only temporary complete catalog copies are edited; the production dataset is never written.
"""
import copy
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import time
from pathlib import Path
from urllib.request import urlopen
from urllib.error import URLError

OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[3]
OUT=Path(os.environ.get("IM13_OUTPUT_DIR", str(OUT)))
JAVA='/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java'
JAR=ROOT/'backend/target/firststep-backend-0.1.0.jar'
BASE='http://127.0.0.1:8114'
ID='SD-913'

def rows(data):return data if isinstance(data,list) else next(v for v in data.values() if isinstance(v,list))
def read(path):
    with urlopen(BASE+path,timeout=10) as response:return json.load(response)['data']
def stop(p):
    if p.poll() is None:
        p.terminate()
        try:p.wait(timeout=10)
        except subprocess.TimeoutExpired:p.kill();p.wait()
def boot(data,phase):
    log=(OUT/f'editor-{phase}-runtime.log').open('w')
    p=subprocess.Popen([JAVA,'-jar',str(JAR),'--server.address=127.0.0.1','--server.port=8114','--news.rss.initial-delay=86400000','--app.data.dir='+str(data)],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
    for _ in range(100):
        if p.poll() is not None:raise AssertionError('Editor fixture startup failed: '+phase)
        try:
            with urlopen(BASE+'/api/resources/'+ID,timeout=1) as response:
                if response.status==200:return p,log
        except (URLError,TimeoutError):pass
        time.sleep(.1)
    stop(p);raise AssertionError('Editor fixture readiness timeout')

def validate(work,script,name,expected=0,args=(),diagnostic=None):
    cmd=['python3',str(ROOT/'data-cleaning/scripts'/script),*args]
    result=subprocess.run(cmd,cwd=work,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (OUT/(name+'.txt')).write_text(result.stdout)
    assert result.returncode==expected,(name,result.returncode,result.stdout)
    if diagnostic:assert diagnostic in result.stdout,result.stdout
    return {'command':cmd,'cwd':'isolated temporary root/app/data','exitCode':result.returncode,'exactDiagnostic':diagnostic}

original={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'app/data').rglob('*') if p.is_file()}
result={'synthetic':True,'canonicalId':ID,'authoringBoundary':'Manual authorized JSON edit plus validator and process reload; no Admin UI or hot reload is claimed','steps':[]}
with tempfile.TemporaryDirectory(prefix='im13-editor-') as directory:
    work=Path(directory);data=work/'app/data';shutil.copytree(ROOT/'app/data',data)
    seed=next(r for r in rows(json.loads((data/'resources.json').read_text())) if r['id']=='HA-001')
    fixture=copy.deepcopy(seed);fixture.update(id=ID,communityId='bear-de',organization='IM13 synthetic offering',organization_id='ORG-001',category_tags=['Housing'],subcategory='Emergency Shelter',summary='Synthetic creation for manual authoring verification.')
    fixture['locations'][0]['city']='Wilmington'
    path=data/'resources.communities.json';document=json.loads(path.read_text());rows(document).append(fixture)
    def save():path.write_text(json.dumps(document,indent=2)+'\n')
    save();(OUT/'editor-created-fixture.json').write_text(json.dumps(fixture,indent=2)+'\n')
    result['steps'].append(validate(work,'validate_organizations.py','editor-create-provider-validator'))
    result['steps'].append(validate(work,'validate_schema.py','editor-create-schema-validator',args=['--input','app/data/resources.communities.json']))
    process,log=boot(data,'created')
    try:
        item=read('/api/resources/'+ID+'?communityId=bear-de');assert item['communityId']=='bear-de' and item['locations'][0]['city']=='Wilmington'
        assert item['organization_id']=='ORG-001' and item['category_tags']==['Housing'] and item['subcategory']=='Emergency Shelter'
        assert item['contentSource']['id']=='dscyf-directory'
        provider=read('/api/organization/american-red-cross?communityId=bear-de');assert ID in [r['id'] for r in provider['resources']]
        help_page=read('/api/category/housing/emergency-shelter?communityId=bear-de');assert ID in [r['id'] for r in help_page['items']]
        result['createdRead']={'resource':item,'provider':provider,'topicIds':[r['id'] for r in help_page['items']]}
    finally:stop(process);log.close()
    fixture.pop('organization_id');fixture.update(category='Food Program',category_tags=['Food'],subcategory='Food Pantry',summary='Synthetic update; housing words cannot override authored Food.')
    # rows(document) holds the same object, matching a manual edit of this existing record.
    save();(OUT/'editor-updated-fixture.json').write_text(json.dumps(fixture,indent=2)+'\n')
    result['steps'].append(validate(work,'validate_organizations.py','editor-update-provider-validator'))
    result['steps'].append(validate(work,'validate_schema.py','editor-update-schema-validator',args=['--input','app/data/resources.communities.json']))
    process,log=boot(data,'updated')
    try:
        updated=read('/api/resources/'+ID+'?communityId=bear-de');assert updated['id']==ID and updated['organization_id'] is None and updated['category_tags']==['Food'] and updated['subcategory']=='Food Pantry'
        assert updated['contentSource']==result['createdRead']['resource']['contentSource']
        food=read('/api/category/food/food-pantry?communityId=bear-de');assert ID in [r['id'] for r in food['items']]
        old=read('/api/category/housing/emergency-shelter?communityId=bear-de');assert ID not in [r['id'] for r in old['items']]
        provider=read('/api/organization/american-red-cross?communityId=bear-de');assert ID not in [r['id'] for r in provider['resources']]
        result['updatedRead']={'resource':updated,'foodIds':[r['id'] for r in food['items']],'previousTopicIds':[r['id'] for r in old['items']],'providerIds':[r['id'] for r in provider['resources']]}
    finally:stop(process);log.close()
    fixture['organization_id']='ORG-999';save()
    result['steps'].append(validate(work,'validate_organizations.py','editor-invalid-provider',expected=1,diagnostic="resources.communities SD-913 references unknown organization_id 'ORG-999'."))
    fixture.pop('organization_id');fixture['subcategory']='Not a controlled Topic';save()
    result['steps'].append(validate(work,'validate_schema.py','editor-invalid-topic',expected=1,args=['--input','app/data/resources.communities.json'],diagnostic="Invalid subcategory: 'Not a controlled Topic'"))

for phase,args in [('all',[]),('bear',['--community=bear-de'])]:
    command=[JAVA,'-Dloader.main=org.firststep.maintenance.EditorialExpiryReportCommand','-cp',str(JAR),'org.springframework.boot.loader.launch.PropertiesLauncher','--at=2026-10-17T16:00:00Z','--app.data.dir='+str(ROOT/'app/data'),*args]
    run=subprocess.run(command,cwd=ROOT,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (OUT/f'expiry-packaged-{phase}.txt').write_text(run.stdout);assert run.returncode==0
    if phase=='all':assert 'NP-003' in run.stdout and 'APPROACHING_EXPIRY' in run.stdout and 'NP-001' in run.stdout and 'EXPIRED' in run.stdout
    if phase=='bear':assert 'No approaching-expiry or expired records in this scope.' in run.stdout and 'NP-003' not in run.stdout
assert all(hashlib.sha256(Path(p).read_bytes()).hexdigest()==h for p,h in original.items())
result['status']='PASS';result['productionCatalogsUnchanged']=True
(OUT/'editor-workflow.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: synthetic create/validate/read/edit/reload; explicit owner/provider and Topic; optional provider omission; exact validator negatives; packaged all/Bear expiry CLI')
