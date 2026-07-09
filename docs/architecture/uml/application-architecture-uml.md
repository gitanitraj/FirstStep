# Application Architecture

## Package UML

``` text
org.firststep
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
|          ApiResponse<T>
|          PageResponse<T>
|
+-- resource
|    controller service dto model
+-- news
|    controller service dto model
+-- flyer
|    controller service dto model
+-- expert
|    controller service dto model
+-- search
+-- ai
+-- pipeline
     collect
     extract-metadata
     normalize
     enrich
     deliver
```
