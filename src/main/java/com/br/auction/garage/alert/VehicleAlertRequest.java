package com.br.auction.garage.alert;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados de um alerta de veiculo")
public class VehicleAlertRequest {

	@NotBlank(message = "O nome e obrigatorio")
	@Schema(description = "Nome do alerta")
	private String name;

	@Schema(description = "Palavra-chave (marca/modelo/descricao)")
	private String keyword;

	@Schema(description = "Cidade desejada")
	private String city;

	@Schema(description = "Tipo de lote (CONSERVADO/SUCATA)")
	private String lotType;

	@Schema(description = "Lance maximo desejado")
	private BigDecimal maxBid;

	@Schema(description = "Alerta ativo")
	private Boolean active;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
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

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
