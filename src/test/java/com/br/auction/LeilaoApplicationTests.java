package com.br.auction;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = LeilaoApplication.class, properties = {
		"spring.datasource.url=jdbc:h2:mem:auction-test;MODE=PostgreSQL;DATABASE_TO_LOWER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.h2.console.enabled=false",
		"integration.seed.enabled=false",
		"integration.scheduler.enabled=false",
		"auction.scheduler.enabled=false" })
class LeilaoApplicationTests {

	@Test
	void contextLoads() {
	}
}
