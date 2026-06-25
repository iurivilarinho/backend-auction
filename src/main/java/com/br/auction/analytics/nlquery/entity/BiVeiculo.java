package com.br.auction.analytics.nlquery.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Relacao curada de veiculos (lotes) de leilao com dados do leilao e a margem de
 * mercado (FIPE - lance), exposta via {@link Subselect}. Somente leitura.
 */
@Entity
@Immutable
@Subselect("""
        SELECT i.id                  AS veiculo_id,
               i.lotId               AS lote_id,
               i.lotNumber           AS lote_numero,
               i.lotType             AS tipo_lote,
               i.vehicleDescription  AS descricao,
               i.brand               AS marca,
               i.model               AS modelo,
               i.vehicleYear         AS ano,
               i.currentBidValue     AS lance_atual,
               i.fipeValue           AS valor_fipe,
               (i.fipeValue - i.currentBidValue) AS margem_fipe,
               CASE WHEN i.currentBidValue > 0
                    THEN (i.fipeValue - i.currentBidValue) / i.currentBidValue * 100
                    ELSE NULL END     AS margem_percentual,
               a.id                  AS leilao_id,
               a.auctionNoticeNumber AS edital,
               a.city                AS cidade,
               a.auctioneer          AS leiloeiro,
               a.status              AS status_leilao,
               a.stateCode           AS uf,
               a.providerCode        AS provedor,
               a.closingDate         AS data_encerramento,
               a.sourceUrl           AS url_leilao
        FROM tbAuctionItem i
        LEFT JOIN tbAuction a ON a.id = i.fk_Id_Auction
        """)
@Synchronize({ "tbAuctionItem", "tbAuction" })
public class BiVeiculo {

    @Id
    @Column(name = "veiculo_id")
    private Long veiculoId;
    @Column(name = "lote_id")
    private String loteId;
    @Column(name = "lote_numero")
    private String loteNumero;
    @Column(name = "tipo_lote")
    private String tipoLote;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "marca")
    private String marca;
    @Column(name = "modelo")
    private String modelo;
    @Column(name = "ano")
    private String ano;
    @Column(name = "lance_atual")
    private BigDecimal lanceAtual;
    @Column(name = "valor_fipe")
    private BigDecimal valorFipe;
    @Column(name = "margem_fipe")
    private BigDecimal margemFipe;
    @Column(name = "margem_percentual")
    private BigDecimal margemPercentual;
    @Column(name = "leilao_id")
    private Long leilaoId;
    @Column(name = "edital")
    private String edital;
    @Column(name = "cidade")
    private String cidade;
    @Column(name = "leiloeiro")
    private String leiloeiro;
    @Column(name = "status_leilao")
    private String statusLeilao;
    @Column(name = "uf")
    private String uf;
    @Column(name = "provedor")
    private String provedor;
    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;
    @Column(name = "url_leilao")
    private String urlLeilao;
}
