package com.br.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SpringDocConfigurations {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes("bearer-key",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.info(new Info().title("API Leilões Detran").description("API para criação, gerenciamento de leilões.")
						.version("1").contact(new Contact().name("Time Backend").email("iuri-vilarinho@hotmail.com"))
						.license(new License().name("Apache 2.0").url("http://lis/api/licenca")));
	}
}