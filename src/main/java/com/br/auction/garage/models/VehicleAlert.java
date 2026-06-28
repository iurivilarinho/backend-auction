package com.br.auction.garage.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.br.auction.garage.enums.AlertType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbVehicleAlert")
@Schema(description = "Alerta de veiculo: regra configuravel que dispara uma notificacao por WhatsApp")
public class VehicleAlert {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome do alerta")
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	@Schema(description = "Tipo do alerta (gatilho)")
	private AlertType type = AlertType.NEW_MATCH;

	// ---------------- Criterio de selecao dos lotes (comum a todos os tipos) ----------------

	@Column(length = 300)
	@Schema(description = "Palavra-chave (marca/modelo/descricao) procurada no veiculo")
	private String keyword;

	@Column(length = 60)
	@Schema(description = "Marca desejada (opcional)")
	private String brand;

	@Column(length = 60)
	@Schema(description = "Modelo desejado (opcional)")
	private String model;

	@Column(length = 60)
	@Schema(description = "Cidade desejada (opcional)")
	private String city;

	@Column(length = 30)
	@Schema(description = "Tipo de lote desejado: CONSERVADO ou SUCATA (opcional)")
	private String lotType;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Lance maximo desejado (filtro de selecao)")
	private BigDecimal maxBid;

	@Column
	@Schema(description = "Raio maximo em km a partir do ponto de origem configurado (opcional)")
	private Double radiusKm;

	@Column
	@Schema(description = "Ano minimo do veiculo (ex.: 2012 = de 2012 em diante)")
	private Integer minYear;

	// ---------------- Parametros especificos por tipo ----------------

	@Column(precision = 15, scale = 2)
	@Schema(description = "Valor de gatilho: teto (PRICE_ABOVE) ou alvo (SOLD_BELOW)")
	private BigDecimal thresholdValue;

	@Column
	@Schema(description = "Percentual da FIPE para considerar barganha (FIPE_DEAL), ex.: 70 = 70% da FIPE")
	private Integer fipePercent;

	@Column
	@Schema(description = "Antecedencia em minutos para o aviso de encerramento (CLOSING_SOON ou notifyClosingSoon)")
	private Integer leadTimeMinutes;

	// Sem nullable=false: coluna adicionada via ddl-auto=update em tabela ja populada.
	@Column
	@Schema(description = "Tambem avisar quando faltar pouco para encerrar os lances (usa leadTimeMinutes, padrao 60). "
			+ "Permite que um alerta de outro tipo (ex.: NEW_MATCH) emita tambem o lembrete de encerramento.")
	private Boolean notifyClosingSoon = Boolean.FALSE;

	// Sem nullable=false: coluna adicionada via ddl-auto=update em tabela ja populada.
	@Column
	@Schema(description = "Tambem avisar quando o leilao abrir para lances (status passa a EM_ANDAMENTO). Dispara uma vez por lote.")
	private Boolean notifyOnStart = Boolean.FALSE;

	@Column(length = 30)
	@Schema(description = "Numero de WhatsApp (E.164 sem +) que sobrescreve o destinatario global (opcional)")
	private String recipientPhone;

	@Column(nullable = false)
	@Schema(description = "Indica se o alerta esta ativo")
	private Boolean active = Boolean.TRUE;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	// Sem nullable=false: a coluna e adicionada via ddl-auto=update em tabela ja populada;
	// o @PrePersist/@PreUpdate preenchem o valor a partir daqui.
	@Column
	private LocalDateTime updatedAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
		if (this.active == null) {
			this.active = Boolean.TRUE;
		}
		if (this.type == null) {
			this.type = AlertType.NEW_MATCH;
		}
		if (this.notifyClosingSoon == null) {
			this.notifyClosingSoon = Boolean.FALSE;
		}
		if (this.notifyOnStart == null) {
			this.notifyOnStart = Boolean.FALSE;
		}
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AlertType getType() {
		return type;
	}

	public void setType(AlertType type) {
		this.type = type;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getLotType() {
		return lotType;
	}

	public void setLotType(String lotType) {
		this.lotType = lotType;
	}

	public BigDecimal getMaxBid() {
		return maxBid;
	}

	public void setMaxBid(BigDecimal maxBid) {
		this.maxBid = maxBid;
	}

	public Double getRadiusKm() {
		return radiusKm;
	}

	public void setRadiusKm(Double radiusKm) {
		this.radiusKm = radiusKm;
	}

	public Integer getMinYear() {
		return minYear;
	}

	public void setMinYear(Integer minYear) {
		this.minYear = minYear;
	}

	public BigDecimal getThresholdValue() {
		return thresholdValue;
	}

	public void setThresholdValue(BigDecimal thresholdValue) {
		this.thresholdValue = thresholdValue;
	}

	public Integer getFipePercent() {
		return fipePercent;
	}

	public void setFipePercent(Integer fipePercent) {
		this.fipePercent = fipePercent;
	}

	public Integer getLeadTimeMinutes() {
		return leadTimeMinutes;
	}

	public void setLeadTimeMinutes(Integer leadTimeMinutes) {
		this.leadTimeMinutes = leadTimeMinutes;
	}

	public Boolean getNotifyClosingSoon() {
		return notifyClosingSoon;
	}

	public void setNotifyClosingSoon(Boolean notifyClosingSoon) {
		this.notifyClosingSoon = notifyClosingSoon;
	}

	public Boolean getNotifyOnStart() {
		return notifyOnStart;
	}

	public void setNotifyOnStart(Boolean notifyOnStart) {
		this.notifyOnStart = notifyOnStart;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public void setRecipientPhone(String recipientPhone) {
		this.recipientPhone = recipientPhone;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
