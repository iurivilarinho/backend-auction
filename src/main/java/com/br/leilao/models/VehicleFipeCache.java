package com.br.leilao.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "tbVehicleFipeCache")
@Schema(description = "Cache de valores FIPE já consultados")
public class VehicleFipeCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Identificador do cache")
    private Long id;

    @Column
    @Schema(description = "Marca do veículo")
    private String brand;

    @Column
    @Schema(description = "Modelo do veículo")
    private String model;

    @Column
    @Schema(description = "Ano do veículo")
    private String year;

    @Column
    @Schema(description = "Tipo do veículo na API FIPE")
    private String vehicleType;

    @Column
    @Schema(description = "Valor FIPE")
    private BigDecimal fipeValue;

    @Column
    @Schema(description = "Data da consulta")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public BigDecimal getFipeValue() {
        return fipeValue;
    }

    public void setFipeValue(BigDecimal fipeValue) {
        this.fipeValue = fipeValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}