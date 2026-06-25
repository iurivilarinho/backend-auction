package com.br.auction.integration.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servico de criptografia simetrica (AES-GCM) usado para proteger segredos de
 * credenciais em repouso. A chave vem da configuracao e nunca fica no codigo.
 */
@Service
public class CryptoService {

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String PREFIX = "ENC:";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH_BIT = 128;

	private final SecretKeySpec keySpec;
	private final SecureRandom secureRandom;

	public CryptoService(@Value("${integration.crypto.key}") String base64Key) {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != 32) {
			throw new IllegalStateException(
					"integration.crypto.key deve ser base64 de 32 bytes (256 bits). Obtido: " + keyBytes.length);
		}
		this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
		this.secureRandom = new SecureRandom();
	}

	public String encrypt(String plaintext) {
		if (plaintext == null) {
			return null;
		}
		if (isEncrypted(plaintext)) {
			return plaintext;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
			buffer.put(iv);
			buffer.put(cipherBytes);
			return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
		} catch (Exception ex) {
			throw new IllegalStateException("Falha ao criptografar valor", ex);
		}
	}

	public String decrypt(String stored) {
		if (stored == null) {
			return null;
		}
		if (!isEncrypted(stored)) {
			return stored;
		}
		try {
			byte[] decoded = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
			ByteBuffer buffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] cipherBytes = new byte[buffer.remaining()];
			buffer.get(cipherBytes);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			byte[] plainBytes = cipher.doFinal(cipherBytes);
			return new String(plainBytes, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			throw new IllegalStateException("Falha ao descriptografar valor", ex);
		}
	}

	public boolean isEncrypted(String value) {
		return value != null && value.startsWith(PREFIX);
	}
}
