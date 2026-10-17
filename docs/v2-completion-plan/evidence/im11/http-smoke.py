"""Read-only packaged Category/selector/privacy integration; runtime is distinct from frozen audit."""
from pathlib import Path
from urllib.request import urlopen
from urllib.error import HTTPError
from datetime import datetime,timezone
import json
out=Path(__file__).resolve().parent
paths=['/api/category/housing?communityId=wilmington-de','/api/category/housing?communityId=bear-de',
 '/api/category/food?communityId=wilmington-de','/api/category/food?communityId=bear-de',
 '/api/category/housing/help?communityId=wilmington-de','/api/category/housing/transitional-housing?communityId=wilmington-de',
 '/api/category/housing/eviction-prevention?communityId=wilmington-de',
 '/api/originals/OR-003','/api/originals/OR-001','/api/originals/OR-002','/api/originals/not-an-article',
 '/api/community-notices/events?communityId=wilmington-de','/api/community-notices/flyers?communityId=wilmington-de',
 '/api/flyers/FL-008','/api/news?communityId=wilmington-de']
private={'diagnostics','limitations','tieStage','roles','helpAccess','editorialReview','generatedBy','verified','status',
 'createdDate','expirationDate','authoredCategoryTags','authoredSubcategory','provenance','catalogOrApprovalDate','meaningfulDate'}
def check(node):
 if isinstance(node,dict):
  assert not private.intersection(node),private.intersection(node)
  for child in node.values():check(child)
 elif isinstance(node,list):
  for child in node:check(child)
responses=[]
for path in paths:
 try:
  with urlopen('http://127.0.0.1:8111'+path) as response:status=response.status;body=json.load(response)
 except HTTPError as error:status=error.code;body=json.load(error)
 assert status==(404 if path in paths[8:11] else 200),(path,status)
 check(body);responses.append(dict(path=path,status=status,body=body))
page=responses[0]['body']['data'];featured=page['destination']['featuredTopics']
expected=[['HA-002','HA-003','HA-005'],['HA-016','HA-001','HA-006'],['HA-011','HA-013','HA-021'],['HA-008','HA-023','HA-026']]
assert [t['slug'] for t in featured]==['transitional-housing','emergency-shelter','homeownership','rental-assistance']
assert [[p['item']['id'] for p in t['items']] for t in featured]==expected
assert all([p['role'] for p in t['items']]==['DURABLE','FALLBACK','FALLBACK'] for t in featured)
assert len(responses[4]['body']['data']['items'])==36
assert len(responses[5]['body']['data']['items'])==11
assert responses[6]['body']['data']['items']==[]
assert responses[2]['body']['data']['destination']['featuredTopics']==[]
other=responses[1]['body']['data']['destination'];assert other['communityId']=='bear-de'
assert [t['slug'] for t in other['featuredTopics']]==['sober-living']
assert responses[3]['body']['data']['destination']['communityId']=='bear-de'
collections={c['key']:c for c in page['collections']}
assert [i['id'] for i in collections['first-rate-news']['items']]==['OR-003','FAQ-002','EA-001','EA-005']
assert [i['id'] for i in collections['faqs']['items']]==['FAQ-002']
assert [i['id'] for i in collections['expert-qa']['items']]==['EA-001','EA-005']
assert [i['id'] for i in collections['flyers']['items']]==['FL-002']
assert 'law' not in collections
assert responses[13]['body']['data']['endsAt'] is None
assert 'FL-008' in [i['id'] for i in responses[12]['body']['data']['items']]
(out/'packaged-http.json').write_text(json.dumps(dict(status='PASS',clock=datetime.now(timezone.utc).isoformat(),
 limitation='Real-runtime snapshot; fixed-clock and synthetic edge cases independently tested. No remote RSS coverage claim.',
 featuredIds=expected,roles=['DURABLE','FALLBACK','FALLBACK'],responses=responses),indent=2)+'\n')
print('PASS:15 packaged HTTP requests;12 exact winners/roles, Community/Help, IM09 memberships, IM10 media and privacy guards')
