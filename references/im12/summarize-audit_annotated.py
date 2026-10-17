# Annotated reference: docs/v2-completion-plan/evidence/im12/summarize-audit.py
# WHAT: Reproduces the read-only packaged/browser or deterministic audit evidence; environment paths and cleanup are explicit.
# WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
# INTERACTIONS: Consumes captured test evidence or temporary copied catalogs; local browser/HTTP processes are terminated after verification.
# ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
# BEGIN MIRRORED SOURCE
"""Read-only, deterministic IM-12 inventory. Run after Im12CatalogAuditTest with evidence properties."""
import collections
import json
from pathlib import Path

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[3]
def read(name):
    return json.loads((OUT / name).read_text())
def write(name, value):
    (OUT / name).write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')
def records(path):
    data = json.loads(path.read_text())
    return data if isinstance(data, list) else next(v for v in data.values() if isinstance(v, list))

audit = read('catalog-after.json')
before = read('catalog-before.json')
assert before == audit, 'Raw/current/projection read models changed'
rows = audit['rows']
by_id = {r['id']:r for r in rows}
raw = {}
for filename in ['resources.json','resources.communities.json','news.json','flyers.json','articles.json','faq.json','expert-answers.json']:
    data = records(ROOT / 'app/data' / filename)
    raw[filename] = {'rows':len(data),'missingStoredCommunityId':[r.get('id') for r in data if not r.get('communityId')],
                     'missingStoredSourceId':[r.get('id') for r in data if not r.get('source_id') and not r.get('contentSource')],
                     'ids':[r.get('id') for r in data]}
ids = [r['id'] for r in rows]
summary = {'clock':audit['clock'],'communities':audit['communities'],'kindCounts':dict(collections.Counter(r['kind'] for r in rows)),
           'contentTypes':sorted({r['contentType'] for r in rows}),'supportedTypes':['RESOURCE','NEWS','FLYER','LAW','EXPERT'],
           'rawStoredFiles':raw,'normalizedMissingOwner':[r['id'] for r in rows if not r['communityId']],
           'duplicates':[k for k,v in collections.Counter(ids).items() if v>1],
           'sourceMissing':[r['id'] for r in rows if not r['source']],'sectorUnknown':[r['id'] for r in rows if not r['producerSector']],
           'states':dict(collections.Counter(r['editorial']['state'] for r in rows)),
           'providerKnown':sum(bool(r.get('organizationId')) for r in rows if r['kind']=='Resource'),
           'providerMissing':[r['id'] for r in rows if r['kind']=='Resource' and not r.get('organizationId')],
           'parentDisplayOnly':[r['id'] for r in rows if r.get('parentDisplayOnly')],
           'rawCurrentProjectionBeforeAfterEqual':True,
           'housingResourcesAllOwners':sum(r['kind']=='Resource' and 'Housing' in r['categoryTags'] for r in rows),
           'flyerHelpAccess':{r['id']:r.get('helpAccess') for r in rows if r['kind']=='Flyer'}}
write('inventory.json',summary)
traces = audit['providerTraces']
write('named-relationships.json',[t for t in traces if t['resourceId'] in ['HA-001','HA-002','HA-006','HA-004','CI-001','SD-032','SD-042','SD-048']])
write('producer-and-authored-provenance.json',[{k:r.get(k) for k in ['id','kind','contentType','communityId','source','producerSector','categoryTags','topic','authoredCategoriesTopics','authoredCategoryTags','authoredSubcategory','articleApproved']} for r in rows])
prohibited = {'verified','status','expirationDate','createdDate','editorialReview','helpAccess','retrieved','authoredCategoryTags','authoredSubcategory','diagnostics','tieStage','generatedBy'}
findings = {}
for phase in ['before','after']:
    findings[phase] = {}
    for path, response in read('http-'+phase+'.json').items():
        leaks = [f for f in response.get('fieldPaths',[]) if f.rsplit('.',1)[-1] in prohibited]
        findings[phase][path] = {'httpStatus':response['status'],'leaks':leaks,'concreteFieldAllowlist':response.get('fieldPaths',[])}
assert all(not r['leaks'] for r in findings['after'].values())
write('endpoint-field-audit.json',findings)
write('lifecycle-occurrence.json',[{k:r.get(k) for k in ['id','communityId','kind','editorial','publicStatus','published','eventDate','startsAt','endsAt','occurrence','helpAccess']} for r in rows])
print(json.dumps({k:v for k,v in summary.items() if k in ['kindCounts','communities','states','providerKnown','housingResourcesAllOwners','rawCurrentProjectionBeforeAfterEqual']},indent=2))
