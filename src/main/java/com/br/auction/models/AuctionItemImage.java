package com.br.auction.models;

import java.util.Base64;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Imagem de um veiculo armazenada no proprio banco da aplicacao. Os bytes sao baixados da
 * fonte no momento da integracao para que as imagens continuem disponiveis mesmo que o
 * site de origem (provedor) fique fora do ar.
 *
 * <p>O conteudo binario e persistido como texto Base64 (LOB), formato portavel entre H2,
 * PostgreSQL e SQL Server sem depender do tipo BLOB/bytea especifico de cada banco.</p>
 */
@Entity
@Table(name = "tbAuctionItemImage")
@Schema(description = "Imagem de veiculo persistida no banco da aplicacao")
public class AuctionItemImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Schema(description = "Identificador interno da imagem")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fk_Id_AuctionItem", foreignKey = @ForeignKey(name = "FK_FROM_TBAUCTIONITEMIMAGE_FOR_TBAUCTIONITEM"))
	@Schema(description = "Item de leilao ao qual a imagem pertence")
	private AuctionItem auctionItem;

	@Column(length = 1000)
	@Schema(description = "URL de origem da imagem no provedor")
	private String sourceUrl;

	@Column(length = 100)
	@Schema(description = "Content-type da imagem (ex.: image/jpeg)")
	private String contentType;

	@Lob
	@Column(name = "imageData")
	@Schema(description = "Conteudo da imagem em Base64 (quando armazenada no banco; nulo quando servida via URL)")
	private String data;

	@Column(name = "imagePosition")
	@Schema(description = "Ordem da imagem dentro do item")
	private Integer position;

	public AuctionItemImage() {
	}

	public AuctionItemImage(String sourceUrl, String contentType, byte[] data) {
		this.sourceUrl = sourceUrl;
		this.contentType = contentType;
		this.data = data == null ? null : Base64.getEncoder().encodeToString(data);
	}

	public Long getId() {
		return id;
	}

	public AuctionItem getAuctionItem() {
		return auctionItem;
	}

	public void setAuctionItem(AuctionItem auctionItem) {
		this.auctionItem = auctionItem;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public byte[] getBytes() {
		return data == null ? new byte[0] : Base64.getDecoder().decode(data);
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}
}
