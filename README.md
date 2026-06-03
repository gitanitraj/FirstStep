# FIRST STEP
A decision aid that turns scattered public data into guidance residents can actually act on.

# The problem
Most civic and government information already exists — it just isn't easily found. Residents looking for housing assistance, food resources or local policy updates must piece together answers from scattered websites, social media posts and word of mouth. The information is there; the trustworthiness and stability of the sources is not.

# What this project does
This prototype reorganizes messy public data into structured, readable, actionable guidance for a single local area: Wilmington. The goal is to reduce friction between public information and real-world need — and to show what becomes possible when information architecture is designed to serve people, not just present data.

It also demonstrates that a developer who understands why people seek information can design tools that everyday residents will actually use.

# Why it matters
Building civic tools for low-resource communities requires more than technical skill. It requires judgment about what information to surface, when and how. This project demonstrates that capacity: taking existing public data but channeling it into an application that is genuinely useful, without noise or unnecessary complexity.

"Messy public data can become usable decisions. This prototype shows how." 

1. Validate data:
   python data-cleaning/validate_schema.py

2. Build backend:
   cd backend
   mvn -U clean package

3. Run backend:
   mvn spring-boot:run

4. Open demo:
   http://localhost:8080
