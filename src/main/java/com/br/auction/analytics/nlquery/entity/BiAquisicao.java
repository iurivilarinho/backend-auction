package com.br.auction.analytics.nlquery.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Relacao curada de veiculos adquiridos (garagem) com gastos consolidados, total
 * investido e lucro realizado, exposta via {@link Subselect}. Somente leitura.
 */
@Entity
@Immutable
@Subselect("""
        SELECT ac.id AS aquisicao_id,
               COALESCE(it.vehicleDescription, ac.vehicleDescription) AS descricao,
               it.brand       AS marca,
               it.model       AS modelo,
               it.vehicleYear AS ano,
               ac.status      AS status,
               ac.acquisitionValue AS valor_arremate,
               ac.saleValue        AS valor_venda,
               COALESCE((SELECT SUM(e.amount) FROM tbAcquisitionExpense e
                         WHERE e.fk_Id_Acquisition = ac.id AND e.status = 'COMPRADO'), 0) AS total_gastos,
               (COALESCE(ac.acquisitionValue, 0)
                 + COALESCE((SELECT SUM(e.amount) FROM tbAcquisitionExpense e
                             WHERE e.fk_Id_Acquisition = ac.id AND e.status = 'COMPRADO'), 0)) AS total_investido,
               CASE WHEN ac.saleValue IS NOT NULL
                    THEN ac.saleValue - COALESCE(ac.acquisitionValue, 0)
                         - COALESCE((SELECT SUM(e.amount) FROM tbAcquisitionExpense e
                                     WHERE e.fk_Id_Acquisition = ac.id AND e.status = 'COMPRADO'), 0)
                    ELSE NULL END AS lucro,
               ac.acquiredAt        AS data_arremate,
               ac.soldAt            AS data_venda,
               ac.inspectionDeadline AS prazo_vistoria,
               ac.lotReference      AS referencia_lote
        FROM tbAcquisition ac
        LEFT JOIN tbAuctionItem it ON it.id = ac.fk_Id_AuctionItem
        """)
@Synchronize({ "tbAcquisition", "tbAcquisitionExpense", "tbAuctionItem" })
public class BiAquisicao {

    @Id
    @Column(name = "aquisicao_id")
    private Long aquisicaoId;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "marca")
    private String marca;
    @Column(name = "modelo")
    private String modelo;
    @Column(name = "ano")
    private String ano;
    @Column(name = "status")
    private String status;
    @Column(name = "valor_arremate")
    private BigDecimal valorArremate;
    @Column(name = "valor_venda")
    private BigDecimal valorVenda;
    @Column(name = "total_gastos")
    private BigDecimal totalGastos;
    @Column(name = "total_investido")
    private BigDecimal totalInvestido;
    @Column(name = "lucro")
    private BigDecimal lucro;
    @Column(name = "data_arremate")
    private LocalDate dataArremate;
    @Column(name = "data_venda")
    private LocalDate dataVenda;
    @Column(name = "prazo_vistoria")
    private LocalDate prazoVistoria;
    @Column(name = "referencia_lote")
    private String referenciaLote;
}
