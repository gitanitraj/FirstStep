"""Inspect-only IM-06 audit using current APIs plus raw files for storage comparison.
Start baseline executable jar on8100 with news.rss.initial-delay=86400000.
"""
import json
from pathlib import Path
import urllib.request

OUT = Path('docs/v2-completion-plan/evidence/im06')
TAXONOMY = json.loads(Path('app/data/taxonomy.json').read_text())['categories']
def get(path):
    with urllib.request.urlopen('http://localhost:8100' + path) as response:
        return json.load(response)['data']
current = []
for endpoint in ['resources', 'flyers', 'news', 'news/rss', 'faqs', 'expert-answers']:
    current.extend(get('/api/' + endpoint))
# Article bodies/private internals are not needed: category collections identify approved Originals.
# Load stored Article metadata only for the audit's raw comparison, never resident candidate admission.
raw = {}
for filename in ['resources.json', 'resources.communities.json', 'flyers.json', 'news.json', 'faq.json', 'expert-answers.json', 'articles.json']:
    root = json.loads(Path('app/data', filename).read_text())
    records = root if isinstance(root, list) else next(value for value in root.values() if isinstance(value, list))
    raw[filename] = {'storedCount': len(records), 'ids': [record['id'] for record in records]}
result = {'rawFiles': raw, 'rssScope': 'relevance-admitted current in-memory cache only; no remote fetch requested', 'categories': {}}
for key in ['housing', 'food']:
    category = next(category for category in TAXONOMY if category['key'] == key)
    for community in ['wilmington-de', None]:
        query = '?communityId=' + community if community else ''
        help_page = get('/api/category/' + key + '/help' + query)
        rows = []
        for topic in help_page['topics']:
            members = {item['id']: item for item in current if (not community or item.get('communityId') == community)
                       and category['label'] in (item.get('category_tags') or [])
                       and item.get('subcategory') == topic['name']}
            help_items = get('/api/category/' + key + '/' + topic['slug'] + query)['items']
            rows.append({'name': topic['name'], 'key': topic['slug'], 'currentDistinctExplicitTopicCount': len(members),
                         'currentDistinctTopicIds': sorted(members), 'actionableCount': len(help_items),
                         'actionableIds': sorted(item['id'] for item in help_items),
                         'lawPolicyContextIds': sorted(item['id'] for item in members.values() if item.get('contentType') == 'LAW' or item.get('type') == 'policy-update'),
                         'visualFlyerIds': sorted(item['id'] for item in members.values() if item.get('contentType') == 'FLYER' and item.get('image'))})
        result['categories'][key + '/' + (community or 'unscoped-existing-browse')] = {'currentHelpTotal': len(help_page['items']), 'topics': rows}
OUT.joinpath('baseline-candidate-audit.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result['categories'], indent=2))
