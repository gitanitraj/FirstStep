/* =============================================================================
 * ANNOTATED REFERENCE — backend/.../shared/web/SpaWebConfig.java
 * Slice A of the civic-portal rebuild. See references/decisions.md Decision 021
 * (and 016 for the original narrow version). Keep this mirror in sync.
 * =============================================================================
 *
 * WHAT THIS CLASS DOES
 *   Serves the React SPA (built to classpath:/static/app-next/) under /app-next,
 *   INCLUDING client-side routes like /app-next/category/housing-assistance that
 *   have no corresponding file on disk.
 *
 * THE PROBLEM IT SOLVES
 *   A single-page app owns its own routing in the browser. When a user hard-
 *   refreshes (or deep-links) to /app-next/important-notices, the server gets a
 *   request for a path with no matching file. Default static serving 404s it.
 *   The SPA fix: serve real files when they exist (JS/CSS/assets), otherwise hand
 *   back index.html so the JS boots and React Router renders the right screen.
 *
 * WHY THIS SHAPE (evolution from Decision 016)
 *   Step 3 used two exact view-controller forwards (/app-next and /app-next/).
 *   That only covered the bare entry paths — fine when no client routes existed.
 *   Slice A introduces real routes, and the coming Category→topic→content routes
 *   go 3–4 segments deep, so a per-depth list of view controllers would be
 *   fragile. Instead we use the canonical, DEPTH-AGNOSTIC resource-resolver
 *   fallback below, which handles any path length in one rule.
 * ============================================================================= */

package org.firststep.backend.shared.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String APP_NEXT_LOCATION = "classpath:/static/app-next/";

    // The two BARE entry paths still need an explicit forward — a request for
    // exactly "/app-next" or "/app-next/" has an empty/So-directory resource path
    // that the resolver below shouldn't have to reason about. View controllers
    // are matched before the resource handler (higher precedence), so these win.
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app-next").setViewName("forward:/app-next/index.html");
        registry.addViewController("/app-next/").setViewName("forward:/app-next/index.html");
    }

    // Everything deeper under /app-next/** is handled here. The custom
    // PathResourceResolver is the whole trick:
    //   - requested file EXISTS  → serve it (this is how index-*.js / index-*.css
    //     and any other real asset keep working — assets are served, not masked).
    //   - requested file MISSING → it's a client-side route → return index.html.
    // Because this only matches /app-next/**, it never intercepts /api/** or the
    // root "/" demo app.
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
                        return location.createRelative("index.html");
                    }
                });
    }
}
