package com.br.auction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Faz /docs e /docs/ servirem a pagina Scalar (static/docs/index.html) via
 * forward interno, sem expor o /index.html na URL.
 */
@Configuration
public class DocsViewConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/docs").setViewName("forward:/docs/index.html");
		registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
	}
}
