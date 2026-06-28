package com.br.auction.service;

import org.springframework.stereotype.Service;

import com.br.auction.response.VehicleInfo;

@Service
public class VehicleParserService {

	public VehicleInfo parse(String description) {

		VehicleInfo info = new VehicleInfo();

		if (description == null || description.isBlank()) {
			return info;
		}

		String normalized = description.trim().toUpperCase();

		try {

			String[] parts = normalized.split(" ");

			// So considera o ultimo token como ano quando ele REALMENTE parece um ano (4 digitos, ou
			// "AAAA/AA"/"AAAA/AAAA"). Evita capturar lixo da descricao como ano (ex.: "CVT", "4P", "(4P)").
			int modelEnd = parts.length;
			String last = parts[parts.length - 1];
			if (last.matches("\\d{4}(/\\d{2,4})?")) {
				info.setYear(last);
				modelEnd = parts.length - 1;
			}

			String brandModel = parts[0];

			if (!brandModel.contains("/")) {
				return info;
			}

			String[] brandModelParts = brandModel.split("/");

			String brand = brandModelParts[0];
			String firstModelPart = brandModelParts[1];

			info.setBrand(brand);

			StringBuilder modelBuilder = new StringBuilder(firstModelPart);

			for (int i = 1; i < modelEnd; i++) {
				modelBuilder.append(" ").append(parts[i]);
			}

			info.setModel(modelBuilder.toString());

		} catch (Exception ignored) {
		}

		return info;
	}
}