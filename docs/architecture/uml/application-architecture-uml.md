# Application Architecture

## Package UML

``` text
org.firststep.backend
|
+-- shared
|    +-- model
|    |     CivicContent (abstract)
|    |     Community
|    |     ContentSource
|    |     Citation
|    |     Media
|    |     Location
|    |     Phone
|    |     Website
|    |     Contact
|    +-- dto
|    |     ApiResponse<T>
|    |     PageResponse<T>  (scaffolded, not wired into any endpoint)
|    +-- exception
|    |     NotFoundException
|    +-- web
|          GlobalExceptionHandler
|
+-- resource
|    controller  service  repository  model      (no dto — ApiResponse<T> wraps Resource directly)
+-- news
|    controller  service  repository  model      (no dto — ApiResponse<T> wraps NewsItem directly)
+-- ai
|    controller  service  dto                    (DecisionRequest/Response/Step — ai-specific, not shared)
+-- flyer    (scaffolding only — package-info.java, no sub-packages yet)
+-- expert   (scaffolding only — package-info.java, no sub-packages yet)
+-- search   (scaffolding only — package-info.java, no sub-packages yet)
+-- pipeline (scaffolding only — marker interfaces, not wired into resource/news ingestion)
     collect            Collector<T>
     extractmetadata    MetadataExtractor<T>   ("extract-metadata" stage — no hyphen in Java package names)
     normalize          Normalizer<T, R>
     enrich             Enricher<T>
     deliver            Deliverer<T, R>
```
