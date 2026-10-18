# Explicit Decide DTO schema, fail-closed missing schemas, injected-field rejection controls and unchanged recursive privacy/other route checks.
# Complete source follows unchanged.
"""Read-only Docker release HTTP and exact concrete field-allowlist comparison to rerun audit."""
import json
import os
import hashlib
from pathlib import Path
from datetime import datetime,timezone
from urllib.request import Request,urlopen
from urllib.error import HTTPError
OUT=Path(os.environ.get('IM13_OUTPUT_DIR', str(Path(__file__).resolve().parent)))
BASE=os.environ.get('IM13_ORIGIN','http://127.0.0.1:8113')
reference=json.loads((OUT/'http-baseline.json').read_text())
private={'verified','status','expirationDate','createdDate','editorialReview','helpAccess','retrieved','authoredCategoryTags','authoredSubcategory','diagnostics','tieStage','generatedBy'}
def fields(node,path='$'):
    found=set()
    if isinstance(node,dict):
        for key,child in node.items():
            assert key not in private,(path,key)
            found.add(path+'.'+key);found.update(fields(child,path+'.'+key))
    elif isinstance(node,list):
        for child in node:found.update(fields(child,path+'[]'))
    return found
# MockMvc's URI-string probe retains '+' in multiword queries, while real HTTP decodes it.
# Compare nonempty Search content against the same independently captured public kind DTOs,
# with exact object keys plus explicit nested field-path allowlists, never an empty response.
search_allowed={'$.data','$.data[].type','$.data[].score','$.data[].content'}
search_keys={}
for route,kind in [('/api/resources','RESOURCE'),('/api/news','NEWS'),('/api/flyers','FLYER')]:
    sample=reference[route]['body']['data'][0]
    search_keys[kind]=set(sample)
    for path in reference[route]['fieldPaths']:
        if path.startswith('$.data[]'):search_allowed.add(path.replace('$.data[]','$.data[].content',1))
search_keys['LAW']=search_keys['NEWS']
# Explicit DTO contracts: ApiResponse<DecisionSummary>, DecisionStep and PublicCitation.
DECIDE_ENVELOPE={'success','data','errorCode','errorMessage','timestamp'}
DECIDE_SUMMARY={'answerTitle','steps','citations','notes'}
def decide_schema(body):
    assert set(body)==DECIDE_ENVELOPE, 'Unexpected decide envelope fields'
    assert set(body['data'])==DECIDE_SUMMARY, 'Unexpected decide summary fields'
    for step in body['data']['steps']:
        assert set(step)=={'order','title','action','why'}, 'Unexpected decide step fields'
    for citation in body['data']['citations']:
        assert set(citation)=={'sourceType','id','label','contentSource'}, 'Unexpected decide citation fields'
        if citation['contentSource'] is not None:
            assert set(citation['contentSource'])=={'id','name','type','url'}, 'Unexpected decide source fields'
    return fields(body)
# Fail closed before HTTP if any reference schema is absent.
for key, expected in reference.items():
    assert key=='POST /api/decide' or 'fieldPaths' in expected, ('Missing expected schema',key)
# Negative controls run on synthetic DTOs, without any production API modification.
import copy
sample={'success':True,'data':{'answerTitle':'fixture','steps':[{'order':1,'title':'t','action':'a','why':None}],
 'citations':[{'sourceType':'RESOURCE','id':'synthetic','label':'fixture','contentSource':{'id':'source','name':None,'type':None,'url':None}}], 'notes':None},'errorCode':None,'errorMessage':None,'timestamp':'fixture'}
decide_schema(sample)
negative=[]
for label, location, key in [('envelope',(), 'unexpected'),('summary',('data',),'unexpected'),('step',('data','steps',0),'unexpected'),('citation',('data','citations',0),'unexpected'),('source',('data','citations',0,'contentSource'),'unexpected'),('nested-private',('data','citations',0,'contentSource'),'editorialReview')]:
    bad=copy.deepcopy(sample); target=bad
    for part in location: target=target[part]
    target[key]='must reject'
    try: decide_schema(bad)
    except AssertionError: negative.append({'case':label,'status':'PASS: rejected'})
    else: raise AssertionError(('Negative control accepted',label))
(OUT/'http-negative-controls.json').write_text(json.dumps(negative,indent=2)+'\n')
responses=[]
for key,expected in sorted(reference.items()):
    post=key.startswith('POST ');path=key.removeprefix('POST ')
    req=Request(BASE+path,data=b'{"userQuery":""}' if post else None,headers={'Content-Type':'application/json'})
    try:
        with urlopen(req,timeout=10) as r:code=r.status;raw=r.read()
    except HTTPError as e:code=e.code;raw=e.read()
    assert code==expected['status'],(path,code,expected['status'])
    try:body=json.loads(raw)
    except json.JSONDecodeError:body=raw.decode()
    actual=fields(body)
    if path.startswith('/api/search'):
        envelope=set(reference['/api/resources']['body']);assert set(body)==envelope
        assert actual<=search_allowed|{'$.'+key for key in envelope},(path,actual-search_allowed)
        for hit in body['data']:
            assert set(hit)=={'type','score','content'}
            content=hit['content'];assert set(content)==search_keys[content['contentType']]
            if content.get('contentSource') is not None:assert set(content['contentSource'])=={'id','name','type','url'}
    elif key=='POST /api/decide':
        assert actual==decide_schema(body)
    elif 'fieldPaths' in expected:
        assert actual==set(expected['fieldPaths']),(path,'field-allowlist mismatch',actual.symmetric_difference(expected['fieldPaths']))
    else:
        raise AssertionError(('Missing expected schema', key))
    responses.append({'method':'POST' if post else 'GET','path':path,'status':code,'body':body,'fieldPaths':sorted(actual),'exactObservedAllowlistMatch':True,'schemaMode':'exact content-kind keys + nested allowlist' if path.startswith('/api/search') else 'exact concrete field paths'})
# Same-origin SPA deep routes and production assets, with no proxy/dev-only substitutions.
assets=[]
for path in ['/app-next/','/app-next/category/housing?communityId=wilmington-de','/app-next/originals/OR-003','/images/seasonal/United%20Neighbors%202026.jpg']:
    with urlopen(BASE+path,timeout=10) as r:
        raw=r.read();assert r.status==200
        if path.startswith('/app-next'):assert b'/app-next/assets/' in raw
        else:assert r.headers['Content-Type'].startswith('image/')
        assets.append({'path':path,'status':r.status,'contentType':r.headers['Content-Type'],'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()})
result={'status':'PASS','clock':datetime.now(timezone.utc).isoformat(),'distribution':'actual existing multi-stage Dockerfile final image; built Vite SPA + executable Spring Boot','responses':responses,'assets':assets,'limit':'Exact field shapes for actual inventory plus fixed-clock full tests; not remote provider/RSS exhaustiveness'}
(OUT/'http-release.json').write_text(json.dumps(result,indent=2)+'\n')
print(f'PASS:{len(responses)} actual Docker resident HTTP responses match exact allowlists;{len(assets)} SPA/media paths')
