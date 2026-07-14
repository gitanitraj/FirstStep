package org.firststep.backend.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app-next").setViewName("forward:/app-next/index.html");
        registry.addViewController("/app-next/").setViewName("forward:/app-next/index.html");
    }
}
