"""Read-only packaged API smoke; run while the final jar listens on localhost8109."""
import json
from pathlib import Path
from urllib.request import urlopen
from urllib.error import HTTPError
OUT = Path(__file__).resolve().parent
BASE = 'http://127.0.0.1:8109'
paths = ['/api/category/housing?communityId=wilmington-de', '/api/news?communityId=wilmington-de',
         '/api/news/rss?communityId=not-a-community', '/api/originals/OR-003',
         '/api/originals/OR-001', '/api/originals/OR-002', '/api/originals/not-an-article',
         '/api/category/food?communityId=bear-de', '/api/category/housing/eviction-prevention?communityId=wilmington-de']
results = []
private = {'editorialReview', 'generatedBy', 'reviewer', 'flags', 'helpAccess', 'diagnostics', 'limitations',
           'verified', 'status', 'createdDate', 'expirationDate', 'authoredCategoryTags', 'authoredSubcategory'}
def check(value):
    if isinstance(value, dict):
        assert not (private & value.keys()), private & value.keys()
        for child in value.values(): check(child)
    elif isinstance(value, list):
        for child in value: check(child)
for path in paths:
    try:
        with urlopen(BASE + path) as response: status = response.status; body = json.load(response)
    except HTTPError as error: status = error.code; body = json.load(error)
    expected = 404 if path in paths[4:7] else 200
    assert status == expected, (path, status)
    check(body)
    results.append({'path': path, 'status': status, 'body': body})
category = results[0]['body']['data']
keys = ['policy-info', 'law', 'flyers', 'first-rate-news']
actual = {key: [item['id'] for c in category['collections'] if c['key'] == key for item in c['items']] for key in keys}
assert actual == {'policy-info': ['NP-004', 'NP-005'], 'law': [], 'flyers': ['FL-002'], 'first-rate-news': ['OR-003', 'FAQ-002', 'EA-001', 'EA-005']}, actual
assert results[2]['body']['data'] == []
assert results[-1]['body']['data']['items'] == []
(OUT / 'packaged-http.json').write_text(json.dumps({'status': 'PASS', 'clock': 'runtime clock, separate from fixed-clock catalog test', 'collections': actual, 'responses': results}, indent=2) + '\n')
print('PASS: 9 packaged HTTP requests, four current collection memberships, public privacy and Article404 guards')
