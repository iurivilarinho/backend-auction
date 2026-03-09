package com.br.leilao.service;

import org.springframework.stereotype.Service;

import com.br.leilao.response.VehicleInfo;

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

			String year = parts[parts.length - 1];
			info.setYear(year);

			String brandModel = parts[0];

			if (!brandModel.contains("/")) {
				return info;
			}

			String[] brandModelParts = brandModel.split("/");

			String brand = brandModelParts[0];
			String firstModelPart = brandModelParts[1];

			info.setBrand(brand);

			StringBuilder modelBuilder = new StringBuilder(firstModelPart);

			for (int i = 1; i < parts.length - 1; i++) {
				modelBuilder.append(" ").append(parts[i]);
			}

			info.setModel(modelBuilder.toString());

		} catch (Exception ignored) {
		}

		return info;
	}
}