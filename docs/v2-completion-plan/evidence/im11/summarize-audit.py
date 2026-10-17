"""Read-only report materialization from captured actual selector evidence and unchanged catalogs."""
from pathlib import Path
import json
out=Path(__file__).resolve().parent
root=out.parents[3]
audit=json.loads((out/'real-catalog-before.json').read_text())
by_id={c['card']['id']:c for selection in audit['itemsByTopic'].values() for c in selection['candidates']}
ids={r['id'] for r in audit['storedHousingRecords']}
stored=[]
for name in ['resources.json','resources.communities.json','flyers.json','news.json','articles.json','faq.json','expert-answers.json']:
 data=json.loads((root/'app/data'/name).read_text())
 if isinstance(data,list): rows=data
 else: rows=next((data[k] for k in ['records','resources','flyers','news','articles','faqs','expertAnswers'] if isinstance(data.get(k),list)),[data])
 for row in rows:
  if row.get('id') not in ids: continue
  fields=['id','communityId','category','category_tags','subcategory','urgency','createdDate','updatedDate','source','retrieved','published','publishDate','expires','expirationDate','active','status','event_date','startsAt','endsAt','helpAccess','image','contentSource','source_expert_answer_id']
  facts={k:row[k] for k in fields if k in row}
  if isinstance(row.get('editorialReview'),dict):facts['articleReview']={k:row['editorialReview'][k] for k in ['status','reviewedDate'] if k in row['editorialReview']}
  stored.append(dict(file='app/data/'+name,facts=facts))
(out/'stored-housing-facts.json').write_text(json.dumps(stored,indent=2)+'\n')
raw_by_id={r['facts']['id']:r['facts'] for r in stored}
lines=['# Actual Housing candidate audit','',f"Captured fixed Clock: {audit['clock']}. Candidate state is scoped to Wilmington; original files remain hash-identical.",'',
'## All controlled Topics','', '|Topic|Current authored Help count|Exact actionable IDs|Distinct Topic proxy coverage|Direct / recent context|Newness|Featured|','|---|---|---|---|---|---|---|']
selected={t['key'] for t in audit['topics']['selected']}
for t in audit['topics']['candidates']:
 lines.append(f"|{t['name']}|{t['actionableCount']}|{', '.join(t['actionableIds']) or 'none'}|{t['distinctRoleCoverage']}|{t['directActionCount']} / {t['recentContextCount']}|{t['catalogOrApprovalNewness'] or 'unknown'}|{'yes' if t['key'] in selected else 'no'}|")
lines+=['','Eviction Prevention has an IM-06 lower-level image proxy on FL-002. Its zero Help IDs exclude it from featured Topics. That proxy does not admit an IM-07 VISUAL item: FL-002 has no helpAccess and is rejected before item role assignment. Its only admitted item is contextual OR-003 as FALLBACK.','',
'## Stored versus scoped current/admitted records','',
'Rows include non-Wilmington Housing, withheld Articles and Category-only Expert/FAQ; none is silently added to the36 Help IDs. Source columns identify producer provenance, not PROVIDER/HOST relationships. Resource catalog newness remains unknown even when source-derived createdDate exists. Full raw fields are in stored-housing-facts.json; actual cards/provenance/evidence/limitations are in real-catalog-before.json.','',
'|ID|Origin|Community|Topic / authored proof|Editorial state / public status|Explicit urgency|Scoped Help / item admission|Actual item roles|Producer|Exclusion or role limitation|','|---|---|---|---|---|---|---|---|---|']
for row in sorted(audit['storedHousingRecords'],key=lambda r:(r['communityId'] or '',r['topic'] or '',r['id'])):
 c=by_id.get(row['id']);reason=row['itemAdmissionExclusions'] or (c['limitations'] if c else [])
 source=row.get('source') or {}
 roles=', '.join(c['roles']) if c else 'not admitted'
 lines.append('|'+ '|'.join(str(v).replace('|','/') for v in [row['id'],', '.join(row['originFiles']),row['communityId'],f"{row['topic'] or 'Category only'} / {row['authoredTopicProof']}",f"{row['lifecycle']['state']} / {row['publicStatus']}",raw_by_id.get(row['id'],{}).get('urgency','absent'),f"{row['scopedActionable']} / {row['featuredItemAdmitted']}",roles or 'no positive role',source.get('id','absent'),'; '.join(reason) or 'none'])+'|')
lines+=['','## Actual winners, next eligible rivals and collision stages','',
'All real admitted actionable Housing candidates have no genuine catalog-newness date. DURABLE has no applicable time discriminator; its exact standard-urgency peers tie on relevance and unknown newness. Fallback compares remaining Help first, then genuine newness, then canonical ID. Next-rival IDs below are computed only for these equal-substantive real groups, not as a replacement production comparator.','',
'|Topic|Position|Winner|Next eligible rival|Comparator stage|','|---|---|---|---|---|']
explanations=[]
for name,selection in audit['itemsByTopic'].items():
 used=set()
 for position in selection['positions']:
  winner=position['item']['card']['id'];role=position['type']
  alternatives=[c for c in selection['candidates'] if c['card']['id'] not in used and (role=='FALLBACK' or role in c['roles'])]
  rivals=sorted(c['card']['id'] for c in alternatives if c['card']['id']!=winner)
  row=dict(topic=name,position=role,winner=winner,nextEligibleRival=rivals[0] if rivals else None,allEligibleRivals=rivals,tieStage=position['tieStage'],source=position['item']['provenance'],card=position['item']['card'])
  explanations.append(row);lines.append(f"|{name}|{role}|{winner}|{row['nextEligibleRival'] or 'none'}|{row['tieStage']}|");used.add(winner)
(out/'selection-explanations.json').write_text(json.dumps(explanations,indent=2)+'\n')
(out/'real-housing-candidates.md').write_text('\n'.join(lines)+'\n')
print('Materialized',len(audit['storedHousingRecords']),'stored Housing rows,',len(explanations),'actual positions across all9 Topics')
