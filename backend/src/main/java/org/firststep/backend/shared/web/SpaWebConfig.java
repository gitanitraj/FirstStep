package org.firststep.backend.shared.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the React SPA under /app-next, including client-side routes.
 *
 * The bare /app-next and /app-next/ paths forward to index.html (view
 * controllers). Everything deeper is handled by a resource resolver that serves
 * the real static file when it exists (JS/CSS/assets) and otherwise falls back
 * to index.html — the standard SPA deep-link pattern, so React Router routes and
 * hard refreshes on them (e.g. /app-next/category/housing-assistance) load the
 * app at any path depth. Widened from the two exact forwards in Step 3 now that
 * real client routes exist (Slice A).
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String APP_NEXT_LOCATION = "classpath:/static/app-next/";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app-next").setViewName("forward:/app-next/index.html");
        registry.addViewController("/app-next/").setViewName("forward:/app-next/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app-next/**")
                .addResourceLocations(APP_NEXT_LOCATION)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Unknown path with no matching file → a client-side route.
                        return location.createRelative("index.html");
                    }
                });
    }
}
