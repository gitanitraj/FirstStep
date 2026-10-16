"""IM-04 packaged API assertions. Start jar with --app.data.dir=/tmp/im04-fixture-data.
Run from root: python3 docs/v2-completion-plan/evidence/im04/packaged-check.py
"""
import json
import urllib.request
from pathlib import Path


def get(path):
    with urllib.request.urlopen('http://localhost:8096' + path) as response:
        return json.load(response)['data']


page = get('/api/category/housing?communityId=synthetic-community')
collections = {c['key']: c['items'] for c in page['collections']}
help_items = collections['get-help']
flyers = collections['flyers']
assert [i['id'] for i in help_items] == ['SYN-access', 'SYN-near']
assert {i['id'] for i in flyers} == {'SYN-access', 'SYN-near', 'SYN-ordinary', 'SYN-invalid'}
for item in help_items:
    assert item['contentType'] == 'FLYER'
    assert item == next(f for f in flyers if f['id'] == item['id'])
    assert item['imageUrl'] == '/images/seasonal/Eviction%20Help.png'
    assert 'helpAccess' not in item
raw = get('/api/flyers/SYN-access')
assert raw['id'] == 'SYN-access' and raw['communityId'] == 'synthetic-community'
assert raw['contentSource']['id'] == 'community-legal-aid'
assert raw['organization'] == 'Synthetic host — no provider role asserted'
assert 'helpAccess' not in raw
assert get('/api/flyers/SYN-expired')['id'] == 'SYN-expired'
assert 'SYN-expired' not in {i['id'] for i in get('/api/flyers')}
assert get('/api/category/housing?communityId=unrelated-community')['collections'] == []
with urllib.request.urlopen('http://localhost:8096/images/seasonal/Eviction%20Help.png') as response:
    assert response.status == 200 and len(response.read()) > 0
result = {'helpIds': [i['id'] for i in help_items], 'flyerIds': [i['id'] for i in flyers],
          'detail': raw, 'assertions': 'all passed; synthetic fixtures only'}
Path('docs/v2-completion-plan/evidence/im04/packaged-final.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
