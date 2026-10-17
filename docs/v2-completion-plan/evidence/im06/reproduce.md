# IM-06 reproduction

From repository root, using the existing environment Maven helper (JDK25, Java17 release, existing caches):

```sh
bash /workspace/.firststep-tools/im01-mvn -Dim06.evidence.dir=/workspace/FirstStep/docs/v2-completion-plan/evidence/im06 verify
```

Focused callable selector and stored-catalog integration:

```sh
bash /workspace/.firststep-tools/im01-mvn -Dtest=AuthoredTopicEvidenceTest,FeaturedRoleQualificationTest,FeaturedTopicOrderingTest,FeaturedTopicSelectorIntegrationTest,FeaturedTopicCatalogTest -Dim06.evidence.dir=/workspace/FirstStep/docs/v2-completion-plan/evidence/im06 test
```

The property is an optional test-only evidence output directory; it introduces no application setting or public endpoint. Fixed clock and actual JSON repositories in FeaturedTopicCatalogTest regenerate real-selector-audit.json. Synthetic fixtures and explicit authored-fact joins are in FeaturedTopicSelectorIntegrationTest. Role/window/invalid-data and complete lexicographic fixtures are in the other named test classes. No production catalog copy-back or writes.

Frontend, from frontend/:

```sh
npm test
npx tsc --noEmit
npm run build
```

From root:

```sh
python3 -m unittest discover -s data-cleaning/tests -v
python3 data-cleaning/scripts/validate_content_sources.py
python3 data-cleaning/scripts/validate_homepage.py
python3 data-cleaning/scripts/validate_navigation.py
python3 data-cleaning/scripts/validate_news.py
python3 data-cleaning/scripts/validate_organizations.py
python3 data-cleaning/scripts/validate_schema.py
```

Optional original baseline API audit (current browser inventory, not final authored selector evidence): start executable jar from root with --server.port=8100 --news.rss.initial-delay=86400000, then `python3 docs/v2-completion-plan/evidence/im06/audit-current.py`. This is a scoped cache snapshot, not a complete remote RSS fetch. Stop the process afterward.

New logs match the global *.log ignore rule; retain them explicitly when IM-06 commit is separately authorized. Current package is not staged/committed/pushed. The prior accepted IM-04/05 implementation/evidence is already pushed to copyofdev at6f7a955.
