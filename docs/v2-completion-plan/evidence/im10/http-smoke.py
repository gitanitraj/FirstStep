"""Read-only real-catalog packaged API proof, separate from fixed-clock synthetic tests."""
import json
from pathlib import Path
from urllib.request import urlopen
from urllib.error import HTTPError
from datetime import datetime, timezone
out=Path(__file__).resolve().parent
paths=['/api/community-notices/events?communityId=wilmington-de',
 '/api/community-notices/flyers?communityId=wilmington-de', '/api/community-notices?communityId=wilmington-de',
 '/api/community-notices/events?communityId=synthetic-west', '/api/flyers/FL-008', '/api/flyers',
 '/api/category/community-support?communityId=wilmington-de','/api/originals/OR-003', '/api/originals/OR-001',
 '/api/originals/not-an-article', '/api/news?communityId=wilmington-de']
private={'diagnostic','diagnostics','occurrenceAssessment','status','helpAccess','verified','createdDate','updatedDate','expirationDate','editorialReview','retrieved','authoredCategoryTags','authoredSubcategory'}
def check(node, article_revision=False):
 if isinstance(node,dict):
  forbidden=private - ({'updatedDate'} if article_revision else set())
  assert not forbidden.intersection(node), forbidden.intersection(node)
  for v in node.values():check(v, article_revision)
 elif isinstance(node,list):
  for v in node:check(v, article_revision)
results=[]
for path in paths:
 try:
  with urlopen('http://127.0.0.1:8110'+path) as response:status=response.status;body=json.load(response)
 except HTTPError as error:status=error.code;body=json.load(error)
 assert status==(404 if path in paths[8:10] else 200),(path,status)
 check(body, path.startswith('/api/originals/')); results.append(dict(path=path,status=status,body=body))
events=results[0]['body']['data'];flyers=results[1]['body']['data'];overview=results[2]['body']['data']
assert events['counts']['EVENTS']==len(events['items'])
assert events['counts']==flyers['counts']==overview['counts']
assert results[3]['body']['data']['items']==[]
assert 'FL-008' in [i['id'] for i in flyers['items']]
row=results[4]['body']['data']
assert row['contentType']=='FLYER' and row['event_date']=='2026-10-17'
assert row['startsAt'] is None and row['endsAt'] is None
assert row['communityId']=='wilmington-de' and row['contentSource']['id']=='westside-grows-together'
assert row['imageUrl']=='/images/seasonal/United%20Neighbors%202026.jpg'
assert len(results[5]['body']['data'])==8
with urlopen('http://127.0.0.1:8110'+row['imageUrl']) as response:
 image=response.read(); assert response.status==200 and image[:2]==b'\xff\xd8'
(out/'packaged-http.json').write_text(json.dumps(dict(status='PASS',clock=datetime.now(timezone.utc).isoformat(),
 limitation='Runtime real catalog; exact concluded boundaries are independently fixed-clock tested. No remote feed refresh.',
 imageBytes=len(image),eventIds=[i['id'] for i in events['items']],flyerIds=[i['id'] for i in flyers['items']],responses=results),indent=2)+'\n')
print('PASS: 11 packaged API requests plus original JPEG; independent Flyers, Community/counts, Article guards and privacy')
