# UML — Information Flow

*The Collect → Generate metadata → Normalize → Enrich → Deliver pipeline.*
*See [../02-information-flow.md](../02-information-flow.md).*

```mermaid
flowchart LR
    %% TODO: Expand each stage with real inputs/outputs and where provenance
    %% (ContentSource) is attached and later surfaced (Citation) at Deliver.
    S[Sources] --> C[Collect]
    C --> M[Generate metadata]
    M --> N[Normalize]
    N --> E[Enrich]
    E --> D[Deliver]
    D --> CH[Channels: Web / Mobile / Search / AI / Newsletter / Social / APIs]
```

> TODO: Replace the stub with the full flow diagram and add a caption.

