package com.br.auction.analytics.savedview;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Visao salva do B.I.: uma consulta gerada no chat (SQL + grafico) guardada com um
 * nome para recarregar depois. O app de leiloes nao tem usuarios, entao a visao e
 * global (sem dono).
 */
@Schema(name = "BiSavedView", description = "Consulta/visao salva do assistente de B.I.")
@Entity
@Table(name = "tbBiSavedView")
public class BiSavedView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Schema(description = "Escopo da visao (ex.: 'nl-query' para as geradas no chat).")
    @Column(name = "scope", length = 60)
    private String scope;

    @Schema(description = "Conteudo serializado da visao (JSON: pergunta, sql, grafico).")
    @Column(name = "payload", length = 4000)
    private String payload;

    @Column(name = "shared", nullable = false)
    private boolean shared = false;

    @Column(name = "favorite", nullable = false)
    private boolean favorite = false;

    @Column(name = "isDefault", nullable = false)
    private boolean isDefault = false;

    @Schema(description = "Visao arquivada (some da lista principal, vai para a aba Arquivadas).")
    @Column(name = "archived")
    private Boolean archived = Boolean.FALSE;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isArchived() {
        return Boolean.TRUE.equals(archived);
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    /** Sem usuarios no app de leiloes: a visao nao tem dono. */
    public Long getOwnerId() {
        return null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
