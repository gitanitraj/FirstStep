# UML — Information Sources

*Taxonomy of source actor types that feed the **Collect** stage.*
*See [../02-information-flow.md](../02-information-flow.md).*

Actor types: **Government**, **Nonprofit**, **Community Organization**,
**Grassroots Organizer**, **Resident** (future), **Expert**.

```mermaid
flowchart TD
    %% TODO: Refine relationships — which actor types produce structured vs.
    %% unstructured sources, and how each maps into Collect.
    subgraph Actors[Source Actor Types]
        G[Government]
        NP[Nonprofit]
        CO[Community Organization]
        GO[Grassroots Organizer]
        R[Resident - future]
        EX[Expert]
    end
    Actors --> Collect
```

> TODO: Replace the stub with the full sources diagram and add a caption.
