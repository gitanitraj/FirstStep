# FIRST STEP
A decision aid that turns scattered public data into guidance residents can actually act on.

# The problem
Most civic and government information already exists — it just isn't easily found. Residents looking for housing assistance, food resources or local policy updates must piece together answers from scattered websites, social media posts and word of mouth. The information is available, but finding trustworthy, up-to-date guidance can be difficult and time-consuming.

The challenge is not access to information. The challenge is transforming information into action. The goal is to create a scalable civic guidance platform that can evolve as community needs change. 

# What this project does
First Step reorganizes public information into structured, readable, actionable guidance for residents of Wilmington, Delaware.

Rather than functioning as a traditional resource directory, First Step acts as a decision aid. Resources are organized into meaningful categories and presented in a way that helps users identify practical next steps.

Current categories include:

Housing Help
Free / Low-Cost Essentials
Seasonal Resources
Weekly Updates

The application demonstrates how thoughtful information architecture can reduce friction between public information and real-world need.

# Technical Architecture
Building civic tools for low-resource communities requires more than technical skill. It requires judgment about what information to surface, when and how. This project demonstrates that capacity: taking existing public data but channeling it into an application that is genuinely useful, without noise or unnecessary complexity.

   # Backend
   
   Spring Boot REST API
   Three API endpoints
   Java model classes aligned with a defined schema
   Startup validation logging
   
   # Building the Backend

   The backend targets **Java 17**. The build uses a Maven toolchain to compile
   and test on JDK 17 regardless of the JVM Maven itself runs under (e.g.
   Homebrew's Maven bundles a newer JDK). This requires a one-time, machine-local
   `~/.m2/toolchains.xml` registering a JDK 17 install, for example:

   ```xml
   <toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0">
     <toolchain>
       <type>jdk</type>
       <provides><version>17</version></provides>
       <configuration>
         <jdkHome>/path/to/your/jdk-17</jdkHome>
       </configuration>
     </toolchain>
   </toolchains>
   ```

   Find your JDK 17 path with `/usr/libexec/java_home -v 17` (macOS). Without a
   matching toolchain entry, the build fails with "No toolchain found".

   # Running with Docker

   The whole app runs as a self-contained Docker Compose stack — the Spring
   backend plus a local Ollama AI service — so you don't need Java, Maven, or a
   toolchain installed. From the repo root:

   ```
   docker compose up --build
   ```

   Then open http://localhost:8080.

   Two services start together:
   * **app** — the Spring backend and static UI on port 8080.
   * **ollama** — a local Ollama server. On first run it pulls the `gemma2:2b`
     model (a few minutes); the model is cached in a named volume, so later
     starts are fast. While the model is still downloading, the AI decision
     endpoint returns a graceful fallback instead of erroring.

   Runtime data (`app/data/` and the seasonal images) is baked into the image, so
   updating that data means rebuilding (`docker compose up --build`). The stack
   needs roughly 4 GB of RAM available for the AI model.

   # Frontend
   Lightweight static frontend
   Mobile-first interface
   Screen-based navigation
   Resource filtering and detail views
   
   # Data Pipeline
   Python preprocessing scripts
   Schema validation
   Data transformation and cleaning
   JSON generation for application consumption
   
# Future Enhancements
The project was intentionally designed to support future growth.

Planned enhancements include:
* Spanish-language support
* Enhanced accessibility options
* Reusable JSON loading utilities
* Stronger filtering and ranking logic
* Expanded civic data sources
* Additional guidance workflows
* Improved abstraction and generic-based architecture
* Enhanced navigation patterns

"Messy public data can become usable decisions. This prototype shows how." 

1. Open demo:
   http://localhost:8080

