# IM-05 inspect-first gap map

Inspection on copyofdev: initial HEAD 3b8c3d6d4496c8321a4cb5f5c067661b1533c103; remote-only documentation fast-forward to 26ad4cd6b3e339a58b010dbfd1dbfdb8bf4bd3ca. Twenty-five dirty/untracked IM-04 files protected with hashes.

| Existing seam / behavior | Demonstrated gap | Bounded change |
|---|---|---|
| IM-04 helpFor(key, topic, communityId) | Not consumed by resident Topic page | Reuse unchanged; no second admission rule |
| UI /category/:key/:topic and matching API; taxonomy topicSlug resolver | Topic API returns broad resources/Flyers/Topic Experts; counts differ from help | Same Topic URL now help-only, invalid parent remains404 |
| /category/:key stable | No All Help destination/path | Reserve static /category/:key/help in existing route pattern and API |
| Navigation groups from navigation.json | Broad all-content counts on links to narrowed help | Category page recomputes help-only Topic counts, retains authored group labels/order; generic NavigationService unchanged |
| ContentCollection four-item disclosure | Topic page rendered every result at once | Reuse four-item preview + expansion to all records; no server cap |
| ContentCard existing original URL | Flyer media fallback missing; HA-012 has phone but no website | Original media fallback for Flyer; help browse uses explicitly stored phone when Resource has no website; no fabricated detail route |
| Community API parameter exists | UI drops it in browsing links/requests | Preserve explicit communityId query through Category, Topic and All Help |
| Generic fetch errors | Missing Topic and network error look alike | Preserve errorCode via ApiError; distinct accessible messages |
| Existing theme tokens / heading primary token | Screenshot shows dark heading on high-contrast black | Use text-primary, which the current theme already redefines |

Real Housing: 37 raw Housing Assistance records, 36 current Help records owned by Wilmington; HA-004 belongs to newport-de, explaining the difference without an expiry inference. All nine Topics audited. Eviction Prevention formerly counted2 broad items but has0 Help. Food has0 Wilmington Help,12 unscoped Help. No live Flyer has an approved helpAccess flag. Authority: New v2 matrix §10 and IM-05 work order; no selector, taxonomy rewrite or full Category redesign required.
