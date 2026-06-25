package com.br.auction.garage.acquisition;

import java.time.LocalDateTime;

import com.br.auction.garage.enums.DocumentType;
import com.br.auction.garage.models.AcquisitionDocument;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Documento do veiculo adquirido")
public class AcquisitionDocumentResponse {

	private final Long id;
	private final DocumentType type;
	private final String typeLabel;
	private final String fileName;
	private final String contentType;
	private final boolean available;
	private final String url;
	private final String sourceUrl;
	private final LocalDateTime createdAt;

	public AcquisitionDocumentResponse(AcquisitionDocument document, Long acquisitionId) {
		this.id = document.getId();
		this.type = document.getType();
		this.typeLabel = document.getType() == null ? null : document.getType().getDescription();
		this.fileName = document.getFileName();
		this.contentType = document.getContentType();
		this.available = document.hasContent();
		this.url = document.hasContent()
				? "/api/garage/acquisitions/" + acquisitionId + "/documents/" + document.getId() + "/download"
				: null;
		this.sourceUrl = document.getSourceUrl();
		this.createdAt = document.getCreatedAt();
	}

	public Long getId() {
		return id;
	}

	public DocumentType getType() {
		return type;
	}

	public String getTypeLabel() {
		return typeLabel;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public boolean isAvailable() {
		return available;
	}

	public String getUrl() {
		return url;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
