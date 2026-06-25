package com.br.auction.garage.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tbVehicleAlert")
@Schema(description = "Alerta de veiculo: notifica quando surgir um lote que combine com o criterio")
public class VehicleAlert {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	@Schema(description = "Nome do alerta")
	private String name;

	@Column(length = 300)
	@Schema(description = "Palavra-chave (marca/modelo/descricao) procurada no veiculo")
	private String keyword;

	@Column(length = 60)
	@Schema(description = "Cidade desejada (opcional)")
	private String city;

	@Column(length = 30)
	@Schema(description = "Tipo de lote desejado: CONSERVADO ou SUCATA (opcional)")
	private String lotType;

	@Column(precision = 15, scale = 2)
	@Schema(description = "Lance maximo desejado")
	private BigDecimal maxBid;

	@Column(nullable = false)
	@Schema(description = "Indica se o alerta esta ativo")
	private Boolean active = Boolean.TRUE;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.active == null) {
			this.active = Boolean.TRUE;
		}
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
