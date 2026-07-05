# UML — Domain Model

*Entities and their relationships. See [../01-domain-model.md](../01-domain-model.md).*

> NOTE: The shared-characteristics concept (`KnowledgeObject`) is **conceptual** —
> do not draw it as a committed superclass. Represent shared characteristics
> without presuming inheritance until that decision is made.

```mermaid
classDiagram
    %% TODO: Model Core Knowledge (Resource, NewsItem, Flyer, FAQ, ExpertAnswer),
    %% organizing entities (Community, Category, Tag), and Supporting objects
    %% (ContentSource, Citation, Location, Contact, Media).
    %% Show communityId as a partition on Core Knowledge and the ContentSource link.
    class Community
    class Resource
    class ContentSource
    Resource --> Community : communityId
    Resource --> ContentSource : provenance
```

> TODO: Replace the stub with the full class diagram and add a caption.
