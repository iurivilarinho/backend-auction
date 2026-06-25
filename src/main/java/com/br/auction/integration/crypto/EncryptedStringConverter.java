package com.br.auction.integration.crypto;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter JPA que criptografa/descriptografa transparentemente os campos anotados
 * com {@code @Convert(converter = EncryptedStringConverter.class)}.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String>, ApplicationContextAware {

	private static CryptoService cryptoService;

	@Override
	public void setApplicationContext(ApplicationContext context) {
		ConfigurableListableBeanFactory factory = ((ConfigurableApplicationContext) context).getBeanFactory();
		cryptoService = factory.getBean(CryptoService.class);
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return cryptoService == null ? attribute : cryptoService.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return cryptoService == null ? dbData : cryptoService.decrypt(dbData);
	}
}
