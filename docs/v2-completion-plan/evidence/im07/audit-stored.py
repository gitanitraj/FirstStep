"""Read-only canonical catalog audit. Run from repository root after the catalog test."""
import hashlib
import json
from pathlib import Path

OUT = Path('docs/v2-completion-plan/evidence/im07')
FILES = ['resources.json', 'resources.communities.json', 'flyers.json', 'news.json',
         'articles.json', 'faq.json', 'expert-answers.json']
result = {'scope': 'Stored rows are NOT resident eligibility; join to real-item-audit.json for current selection.', 'files': {}}
for name in FILES:
    path = Path('app/data') / name
    root = json.loads(path.read_text())
    rows = root if isinstance(root, list) else next((root[k] for k in ['records', 'resources', 'flyers', 'news', 'articles', 'faqs', 'expertAnswers'] if isinstance(root.get(k), list)), [root])
    fields = ['id', 'category', 'category_tags', 'subcategory', 'communityId', 'status', 'active',
              'expirationDate', 'expires', 'helpAccess', 'event_date', 'published', 'publishDate',
              'createdDate', 'urgency', 'image', 'source_id', 'source_url']
    records = [{k: row[k] for k in fields if k in row} for row in rows]
    for row, record in zip(rows, records):
        if name == 'flyers.json':
            image = row.get('image')
            candidates = [Path('app/images/seasonal') / image, Path('backend/src/main/resources/static/images/seasonal') / image] if image else []
            record['mediaExistsLocallyAt'] = [str(p) for p in candidates if p.is_file()]
            record['contentSource'] = row.get('contentSource')
    result['files'][name] = {'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'storedCount': len(rows), 'records': records}
(OUT / 'stored-catalog-audit.json').write_text(json.dumps(result, indent=2) + '\n')
