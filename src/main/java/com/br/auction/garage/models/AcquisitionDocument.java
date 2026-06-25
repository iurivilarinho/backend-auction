package com.br.auction.garage.models;

import java.time.LocalDateTime;
import java.util.Base64;

import com.br.auction.garage.enums.DocumentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbAcquisitionDocument")
@Schema(description = "Documento do veiculo adquirido (carta de arrematacao, edital, alvara) armazenado no banco")
public class AcquisitionDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fk_Id_Acquisition", foreignKey = @ForeignKey(name = "FK_FROM_TBACQUISITIONDOCUMENT_FOR_TBACQUISITION"))
	private Acquisition acquisition;

	@Enumerated(EnumType.STRING)
	// VARCHAR (e nao ENUM nativo): permite novos tipos de documento sem quebrar o schema.
	@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
	@Column(nullable = false, length = 40)
	@Schema(description = "Tipo do documento")
	private DocumentType type;

	@Column(length = 200)
	@Schema(description = "Nome do arquivo")
	private String fileName;

	@Column(length = 100)
	@Schema(description = "Content-type do documento")
	private String contentType;

	@Column(length = 1000)
	@Schema(description = "URL de origem no painel do provedor")
	private String sourceUrl;

	@Lob
	@Column(name = "documentData")
	@Schema(description = "Conteudo do documento em Base64")
	private String data;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Acquisition getAcquisition() {
		return acquisition;
	}

	public void setAcquisition(Acquisition acquisition) {
		this.acquisition = acquisition;
	}

	public DocumentType getType() {
		return type;
	}

	public void setType(DocumentType type) {
		this.type = type;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setBytes(byte[] bytes) {
		this.data = bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
	}

	public byte[] getBytes() {
		return data == null ? new byte[0] : Base64.getDecoder().decode(data);
	}

	public boolean hasContent() {
		return data != null && !data.isBlank();
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
