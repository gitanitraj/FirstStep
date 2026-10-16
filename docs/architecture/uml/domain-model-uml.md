# Domain Model

## Shared Domain UML

``` text
Community
--------------------------
id
name
city
state
zipCodes : List<String>
active

        1
        |
        | contains
        v

             <<abstract>>
              CivicContent
----------------------------------------
id
title
summary
verified
tags
contentSource
createdDate
updatedDate

        ^
        |
 +------+-------+--------+----------+
 |      |       |        |          |
Resource NewsItem Flyer  FAQ  ExpertAnswer

ContentSource
-------------
id
name
type
url
retrieved

Citation
Media
Location
Phone
Website
Contact
```
