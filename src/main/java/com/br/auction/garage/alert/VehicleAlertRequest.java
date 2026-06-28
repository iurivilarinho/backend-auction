package com.br.auction.garage.alert;

import java.math.BigDecimal;

import com.br.auction.garage.enums.AlertType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de um alerta de veiculo")
public class VehicleAlertRequest {

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome do alerta")
	private String name;

	@Schema(description = "Tipo do alerta (gatilho). Default NEW_MATCH.")
	private AlertType type;

	@Schema(description = "Palavra-chave (marca/modelo/descricao)")
	private String keyword;

	@Schema(description = "Marca desejada")
	private String brand;

	@Schema(description = "Modelo desejado")
	private String model;

	@Schema(description = "Cidade desejada")
	private String city;

	@Schema(description = "Tipo de lote (CONSERVADO/SUCATA)")
	private String lotType;

	@Schema(description = "Lance maximo desejado (filtro de selecao)")
	private BigDecimal maxBid;

	@Schema(description = "Raio maximo em km a partir do ponto de origem configurado")
	private Double radiusKm;

	@Schema(description = "Ano minimo do veiculo (ex.: 2012)")
	private Integer minYear;

	@Schema(description = "Valor de gatilho: teto (PRICE_ABOVE) ou alvo (SOLD_BELOW)")
	private BigDecimal thresholdValue;

	@Schema(description = "Percentual da FIPE para barganha (FIPE_DEAL), ex.: 70")
	private Integer fipePercent;

	@Schema(description = "Antecedencia em minutos para o aviso de encerramento (CLOSING_SOON ou notifyClosingSoon)")
	private Integer leadTimeMinutes;

	@Schema(description = "Tambem avisar quando faltar pouco para encerrar os lances (usa leadTimeMinutes, padrao 60)")
	private Boolean notifyClosingSoon;

	@Schema(description = "Tambem avisar quando o leilao abrir para lances (status passa a EM_ANDAMENTO)")
	private Boolean notifyOnStart;

	@Schema(description = "Numero de WhatsApp (E.164 sem +) que sobrescreve o destinatario global")
	private String recipientPhone;

	@Schema(description = "Alerta ativo")
	private Boolean active;

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
}
