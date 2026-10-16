# IM-03 editorial Category assignments

**Decision source:** user's explicit conditional approval in this task, October 16,
2026. The user approved the proposed Category-only approach using existing controlled
Categories, subject to verification of each record's actual question, title and
substantive answer before writing. Ambiguous/unsupported records were to remain
unassigned for a separate editorial decision. No Topics or taxonomy additions were
authorized; runtime keyword inference was explicitly prohibited as a substitute.

**Verification performed:** read all twelve stored records in `app/data/faq.json`
and `app/data/expert-answers.json`, including titles, questions and full answers.
Each proposed assignment is supported by the substantive subject described below.
No ambiguous assignment was found. This is an execution of the user's editorial
decision after content verification, not automated prose classification. It does
not certify the current factual accuracy of the advice or renew an editorial date.

| ID | Approved Category | Substantive content verified before assignment |
|---|---|---|
| FAQ-001 | Food | SNAP application channels through Delaware ASSIST/DSS, and application documentation for food benefits. |
| FAQ-002 | Housing | Tenant eviction/tenancy notice periods, reasons for notice, and the court's role in eviction. |
| FAQ-003 | Legal | How to obtain income-qualified free civil legal assistance from DVLS/CLASI; the answer discusses access to legal services across several subjects. |
| FAQ-004 | Utilities | LIHEAP heating-bill assistance eligibility, household income, seasonal applications and limited funds. |
| FAQ-005 | Health | Documentation required to apply for Medicaid health coverage through ASSIST. |
| FAQ-006 | Employment | Filing an unemployment insurance claim after job loss, work history and claim timing. |
| EA-001 | Housing | Landlord security-deposit return/deductions, tenant demand letter and remedies after moving out. |
| EA-002 | Food | SNAP application channels, household expenses and expedited food-benefit processing. |
| EA-003 | Employment | At-will dismissal, unlawful employment termination/retaliation, final pay and Department of Labor complaints. The legal producer does not determine the Category. |
| EA-004 | Health | Medicaid coverage eligibility based on household income and application timing. Mention of unemployment does not change the substantive healthcare subject. |
| EA-005 | Housing | Landlord notice for nonpayment, violations or ending tenancy, court orders and prohibited lockouts. |
| EA-006 | Utilities | LIHEAP heating-cost grants, payment to utilities, income criteria and application windows. |

**Applied change:** append only the approved `category_tags` field to each record.
All twelve previously lacked that field. No `subcategory` was added or retained
from in-memory inference. Identity, Community, source, expert/provider fields,
question/answer text, descriptive tags, dates, statuses and cross-record links are
unchanged. Semantic before/after comparison removes only the added Category field
and proves equality for every other stored field. Controlled taxonomy is unchanged.

**Unresolved editorial assignments:** none. Before/after record inventories and the
full IM-03 verification/disposition are in the IM-03 execution report and its evidence
folder. Future editorial changes remain subject to the approved authoring process.
