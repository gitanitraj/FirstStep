# Editorial regression cases

Real article drafts that failed review, preserved as test cases.

**Each case serves two separate systems, and the findings must not be merged:**

| | question | owner |
| --- | --- | --- |
| **Generation** | How do we stop the writing model producing this? | the article-writing LLM's prompt and constraints |
| **Review** | How does First Step detect and disposition it when it happens anyway? | Decision 048 |

A case belongs here because it demonstrates BOTH. A generation fix that made a
failure rarer would not remove the need for review; a review that catches it does
not fix the generator.

> **An editorial finding is not an architectural requirement.** These cases record
> what the standard must catch. They do not, on their own, justify a
> source-verification subsystem, mandatory manual URL checking, or new mechanical
> validators. Where a case genuinely exposes a schema or workflow gap, that is
> recorded as a decision — not inferred from the fact that an article was wrong.

---

## Case 001 — ChristianaCare land acquisition

**Provenance:** draft submitted for publication, 2026-08-21.
**Status:** held, `flagged`. Article record `OR-002`.

### What the evidence established
A land purchase closed on a stated date. A spokesperson said the company was
evaluating potential uses.

### What the article claimed
That the company "plans to use the property for various purposes, including
potential future development of healthcare facilities and community amenities" —
two paragraphs after correctly stating that **no plans had been announced**. It
closed by asserting the acquisition "is expected to have a positive impact on the
local community", and carried a WILMINGTON dateline for a property near Newark.

### The failure
A **silent tier-2 → tier-1 promotion**: a source's forward-looking statement
became First Step's own assertion. Nobody decided to do that — the draft simply
dropped the attribution.

Self-contradiction within one article is the diagnostic worth remembering: the
sourced sentence and the invented sentence were both present, four paragraphs
apart.

---

## Case 002 — Wilmington Voluntary Rent Escrow Program

**Provenance:** produced by a separate LLM being trained to write First Step
Original articles, then submitted to this project as a live test case. It is
**primarily a test artifact for the article-generation system**, and secondarily
a test of Decision 048's review standard.
**Status:** held, `flagged`. Article record `OR-001`.
**Source:** City of Wilmington program page, published 2026-07-22.

### What the source establishes

- the escrow **mechanism** — eligible tenants **may** temporarily deposit rent
  into escrow when specified essential services are not restored after the
  notice required under Delaware law
- eligibility context and the essential-service conditions
- program administration, and the roles of FSCAA and L&I
- the release conditions, under City Code § 34-151(e)

### What it does NOT establish

- that the program **"ensures"** tenants will avoid eviction
- that it **stabilizes the rental market**
- that it **prevents economic hardship**

### The failure

**The model took a source's description of a mechanism and a purpose, and
generated broader consequences the source does not support.**

Two distinct drifts, in one sentence:

> "it **ensures** that tenants can continue to pay rent **without facing
> eviction** while essential services are being restored"

1. **Modal drift** — the source's *may* became *ensures*.
2. **Scope drift** — a rent-payment mechanism became an eviction outcome.

And in the next sentence, invented significance at a scale the source never
addresses:

> "helps to **stabilize the rental market** and **prevent further economic
> hardship** for residents"

### What the draft got RIGHT — this matters for the fix

The procedural sections — Administration, Eligibility, Enrollment, Escrow Fund
Management, Release of Escrow Funds, Contact — are faithful, specific and useful.
Statutory and code citations are given inline (`25 Del. C. § 5308`,
`§ 34-151(e)`), which is attribution, and a reviewer who flags them is wrong.
The model also correctly set `verified: false` and marked the URL as needing
checking.

**The generalization: the model is reliable when RESTATING and unreliable when
INTERPRETING.** Every failure sits in an evaluative or summarizing section; no
failure sits in a procedural one. That is where to aim the constraints.

### Generation behaviors that need to change

**1. "Why It Matters" must not manufacture significance because the section asks
for it.**

This is the primary finding. A section headed *why does this matter* pressures
the model to answer at the largest scale available, and when the source supports
no broad claim the model supplies one.

> Explain practical significance using **evidence-supported implications**. Where
> the source establishes only a mechanism or a resident-facing action, explain
> **what the information lets a resident understand or do** — do not invent
> economic, legal, or community-wide outcomes.

**2. Preserve the source's modality.**
Permissive stays permissive. *May* must not become *ensures*, *will*, or
*guarantees*. An enabling mechanism must never be rendered as a guaranteed
outcome.

**3. DECIDED — articles do not carry a "Conclusion" section.**
The template produced one, and a conclusion about a civic program becomes an
endorsement almost by default — here, "designed to protect… ensure their
financial stability". **Both** regression cases fail in their closing paragraph,
which is the pattern that settled it.

With a "Why It Matters" section already present, a conclusion is redundant as
well as risky: significance has been stated once, and restating it is what
creates room to overstate it. **The reader can draw their own conclusion.** Where
a closing section is genuinely useful, "What to do next" serves a resident better
and is far harder to editorialize into.

**4. Never invent a date.**
The draft carried `Published: 2023-04-01` for a program announced 2026-07-22 —
three years off, and incompatible with its own reference to Mayor Carney. Where a
date is not established, leave it unset.

**5. Target the Article schema, not news.json.**
The submitted metadata block used `Source ID`, `Source URL`, `Author`, `Urgency`,
`Active`, `Geography` and `Resource Tags` — news-item fields. An Article carries
`title`, `summary`, `whyItMatters`, `body`, `category_tags`, `subcategory`,
`tags`, `contentSource`, `generatedBy`, `publishDate`, `verified`.
(A `byline` field is approved but not yet implemented.)
`Category Tags: Local News` is not a taxonomy category and fails validation.

### Review behaviors this case confirms

Flags Decision 048 should raise, and did:

| passage | issue |
| --- | --- |
| "ensures … without facing eviction" | `certainty-drift` |
| "stabilize the rental market … prevent further economic hardship" | `advocacy` |
| "ensure their financial stability during repairs" | `advocacy` |

A flag the reviewer raised **wrongly**, preserved here as a false positive:

| passage | wrongly flagged as | why it was wrong |
| --- | --- | --- |
| "in accordance with City Code Section 34-151(e)" | `unattributed-claim` | The inline code citation **is** the attribution. Restating an ordinance while naming the section is not First Step asserting law on its own authority. |

That false positive is the reason flag dispositions must distinguish
**withdrawn** from **overridden** — and it is now recorded as such on the article
record itself, with its actor, date and reason, rather than deleted.

### The distinction this case demonstrates most cleanly

**The source is reachable, and a claim in the article is still overstated.**

A resolving URL proves nothing about the sentences that cite it. This is why
review is editorial and not mechanical:

| mechanical — eventually promotable to a validator | editorial — never mechanical |
| --- | --- |
| does the URL resolve? | does the source support *this claim*? |
| is a required field present? | is the cited law correctly characterized? |
| is the review status in vocabulary? | is the phone number current? |

Rent Escrow would pass every mechanical check available and still needed review.
