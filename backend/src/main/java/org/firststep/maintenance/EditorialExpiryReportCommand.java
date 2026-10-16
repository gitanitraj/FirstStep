package org.firststep.maintenance;

import java.io.PrintStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ConfigurableApplicationContext;
import org.firststep.backend.resource.repository.ResourceRepository;
import org.firststep.backend.news.repository.NewsRepository;
import org.firststep.backend.flyer.repository.FlyerRepository;
import org.firststep.backend.expert.repository.FaqRepository;
import org.firststep.backend.expert.repository.ExpertAnswerRepository;
import org.firststep.backend.originals.repository.ArticleRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.firststep.backend.category.service.TaxonomyService;
import org.firststep.backend.expert.repository.JsonExpertAnswerRepository;
import org.firststep.backend.expert.repository.JsonFaqRepository;
import org.firststep.backend.flyer.repository.JsonFlyerRepository;
import org.firststep.backend.news.repository.JsonNewsRepository;
import org.firststep.backend.news.service.RssFeedService;
import org.firststep.backend.originals.repository.JsonArticleRepository;
import org.firststep.backend.resource.repository.JsonResourceRepository;
import org.firststep.backend.shared.classification.CategoryClassifier;
import org.firststep.backend.shared.classification.CivicContentClassifier;
import org.firststep.backend.shared.classification.SourceMappingService;
import org.firststep.backend.shared.classification.TagClassifier;
import org.firststep.backend.shared.lifecycle.EditorialExpiryConfiguration;
import org.firststep.backend.shared.lifecycle.EditorialExpiryDiagnostics;
import org.firststep.backend.shared.lifecycle.EditorialExpiryReport;
import org.firststep.backend.shared.lifecycle.EditorialLifecycle;
import org.firststep.backend.shared.service.ContentSourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.Banner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Bounded, non-web maintenance entry point. Outside the backend component scan;
 * imports only stored-data collaborators, without scheduling or remote feed fetches.
 */
public final class EditorialExpiryReportCommand {
    private EditorialExpiryReportCommand() {}

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties
    @Import({EditorialExpiryConfiguration.class, EditorialLifecycle.class, EditorialExpiryDiagnostics.class,
            JsonResourceRepository.class, JsonNewsRepository.class, JsonFlyerRepository.class,
            JsonFaqRepository.class, JsonExpertAnswerRepository.class, JsonArticleRepository.class,
            RssFeedService.class, TaxonomyService.class, ContentSourceService.class,
            CivicContentClassifier.class, CategoryClassifier.class, SourceMappingService.class, TagClassifier.class})
    public static class ReportConfiguration {
        @Bean
        @Primary
        Clock diagnosticClock(@Value("${at:}") String at) {
            return Clock.fixed(at.isBlank() ? Instant.now() : Instant.parse(at), ZoneOffset.UTC);
        }
    }

    public static void main(String[] args) {
        System.exit(execute(args, System.out));
    }

    /** Exit 0: complete, valid report; 1: isolated metadata problems; 2: invocation/setup error. */
    public static int execute(String[] args, PrintStream output) {
        try {
            var arguments = new DefaultApplicationArguments(args);
            if (arguments.containsOption("help")) {
                output.println("Editorial expiry report: [--at=2026-10-15T16:00:00Z] [--community=wilmington-de] [--app.data.dir=app/data] [--app.editorial-expiry.<setting>=value]");
                return 0;
            }
            if (!arguments.getNonOptionArgs().isEmpty()) throw new IllegalArgumentException("Use named --option=value arguments.");
            for (String key : arguments.getOptionNames()) {
                if (!List.of("at", "community", "app.data.dir").contains(key) && !key.startsWith("app.editorial-expiry.")) {
                    throw new IllegalArgumentException("Unknown report option: " + key);
                }
                List<String> values = arguments.getOptionValues(key);
                if (values == null || values.size() != 1 || values.get(0).isBlank()) {
                    throw new IllegalArgumentException("Supply exactly one nonblank value for --" + key);
                }
            }
            if (arguments.containsOption("at")) {
                try { Instant.parse(arguments.getOptionValues("at").get(0)); }
                catch (java.time.format.DateTimeParseException invalidInstant) {
                    throw new IllegalArgumentException("Invalid --at: use an ISO-8601 instant, for example 2026-10-15T16:00:00Z.");
                }
            }
            SpringApplication application = new SpringApplication(ReportConfiguration.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setBannerMode(Banner.Mode.OFF);
            application.setLogStartupInfo(false);
            application.setDefaultProperties(Map.of("logging.level.root", "OFF"));
            // run() returns after ApplicationReadyEvent: repository loading has completed.
            try (var context = application.run(args)) {
                String dataDir = context.getEnvironment().getProperty("app.data.dir", "app/data");
                verifyLoadedCatalog(context, Path.of(dataDir));
                var report = context.getBean(EditorialExpiryDiagnostics.class).collect(
                        arguments.containsOption("community") ? arguments.getOptionValues("community").get(0) : null);
                output.print(EditorialExpiryReport.render(report));
                return report.problems().isEmpty() ? 0 : 1;
            }
        } catch (IllegalArgumentException invalidRequest) {
            output.println("Report could not run: " + invalidRequest.getMessage());
            return 2;
        } catch (RuntimeException setupFailure) {
            output.println("Report could not run: check catalog loading and editorial-expiry configuration. No report completeness is claimed.");
            return 2;
        }
    }
    /** Repositories log load failures rather than throwing; never label a partial load complete. */
    private static void verifyLoadedCatalog(ConfigurableApplicationContext context, Path dir) {
        int resources = recordCount(dir.resolve("resources.json"));
        if (Files.exists(dir.resolve("resources.communities.json"))) {
            resources += recordCount(dir.resolve("resources.communities.json"));
        }
        if (context.getBean(ResourceRepository.class).findAll().size() != resources
                || context.getBean(NewsRepository.class).findAll().size() != recordCount(dir.resolve("news.json"))
                || context.getBean(FlyerRepository.class).findAll().size() != recordCount(dir.resolve("flyers.json"))
                || context.getBean(FaqRepository.class).findAll().size() != recordCount(dir.resolve("faq.json"))
                || context.getBean(ExpertAnswerRepository.class).findAll().size() != recordCount(dir.resolve("expert-answers.json"))
                || context.getBean(ArticleRepository.class).findAll().size() != recordCount(dir.resolve("articles.json"))) {
            throw new IllegalArgumentException("Stored catalog loading was incomplete; inspect repository loading messages. No complete report was produced.");
        }
    }

    private static int recordCount(Path file) {
        try {
            var root = new ObjectMapper().readTree(file.toFile());
            var records = root.isArray() ? root : root.has("records") ? root.get("records") : root.get("resources");
            if (records != null && records.isArray()) return records.size();
            if (root.isObject() && root.has("id")) return 1;
            throw new IllegalArgumentException("Unrecognized stored catalog structure: " + file);
        } catch (IOException invalidCatalog) {
            throw new IllegalArgumentException("Cannot read valid catalog JSON: " + file);
        }
    }

}
