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
 * Relacao curada de gastos/cotacoes lancados nas aquisicoes da garagem, exposta
 * via {@link Subselect}. Somente leitura.
 */
@Entity
@Immutable
@Subselect("""
        SELECT e.id                 AS gasto_id,
               e.fk_Id_Acquisition  AS aquisicao_id,
               e.type               AS tipo,
               e.status             AS status,
               e.description        AS descricao,
               e.amount             AS valor,
               e.place              AS local,
               e.incurredAt         AS data
        FROM tbAcquisitionExpense e
        """)
@Synchronize({ "tbAcquisitionExpense" })
public class BiGasto {

    @Id
    @Column(name = "gasto_id")
    private Long gastoId;
    @Column(name = "aquisicao_id")
    private Long aquisicaoId;
    @Column(name = "tipo")
    private String tipo;
    @Column(name = "status")
    private String status;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "valor")
    private BigDecimal valor;
    @Column(name = "local")
    private String local;
    @Column(name = "data")
    private LocalDate data;
}
