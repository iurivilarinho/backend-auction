package com.br.auction.response;

import java.math.BigDecimal;
import java.util.List;

import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;
import com.br.auction.models.AuctionItem;
import com.br.auction.service.AuctionItemLinks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de item de leilao")
public class AuctionItemResponse {

	@Schema(description = "Identificador do item")
	private Long id;

	@Schema(description = "Identificador do lote no portal do provedor")
	private String lotId;

	@Schema(description = "Numero do lote")
	private String lotNumber;

	@Schema(description = "Tipo textual do lote")
	private String lotType;

	@Schema(description = "Tipo normalizado do lote")
	private LotType normalizedLotType;

	@Schema(description = "Descricao do veiculo")
	private String vehicleDescription;

	@Schema(description = "Marca extraida da descricao")
	private String brand;

	@Schema(description = "Modelo extraido da descricao")
	private String model;

	@Schema(description = "Ano extraido da descricao")
	private String vehicleYear;

	@Schema(description = "Valor atual do lance")
	private BigDecimal currentBidValue;

	@Schema(description = "Valor da tabela FIPE")
	private BigDecimal fipeValue;

	@Schema(description = "Distancia em km entre a origem configurada e a cidade do leilao (null quando ainda nao geocodificada)")
	private Double distanceKm;

	@Schema(description = "Indica se os lances do veiculo ja foram encerrados (leilao finalizado)")
	private boolean closed;

	@Schema(description = "URL que abre o leilao ja posicionado neste veiculo (ancora #lotId); cai na URL do leilao quando nao ha lote")
	private String lotUrl;

	@Schema(description = "Imagens do veiculo (servidas pelo backend)")
	private List<AuctionItemImageResponse> images;

	@Schema(description = "Leilao ao qual o veiculo pertence")
	private AuctionListResponse auction;

	public AuctionItemResponse(AuctionItem item) {
		this.id = item.getId();
		this.lotId = item.getLotId();
		this.lotNumber = item.getLotNumber();
		this.lotType = item.getLotType();
		this.normalizedLotType = LotType.fromSource(item.getLotType());
		this.vehicleDescription = item.getVehicleDescription();
		this.brand = item.getBrand();
		this.model = item.getModel();
		this.vehicleYear = item.getVehicleYear();
		this.currentBidValue = item.getCurrentBidValue();
		this.fipeValue = item.getFipeValue();
		this.closed = item.getAuction() != null
				&& AuctionStatus.fromSource(item.getAuction().getStatus()) == AuctionStatus.FINALIZADO;
		this.lotUrl = AuctionItemLinks.lotUrl(item);
		this.images = item.getImages() == null ? List.of()
				: item.getImages().stream().map(AuctionItemImageResponse::new).toList();
		this.auction = item.getAuction() != null ? new AuctionListResponse(item.getAuction()) : null;
	}

	public Long getId() {
		return id;
	}

	public String getLotId() {
		return lotId;
	}

	public String getLotNumber() {
		return lotNumber;
	}

	public String getLotType() {
		return lotType;
	}

	public LotType getNormalizedLotType() {
		return normalizedLotType;
	}

	public String getVehicleDescription() {
		return vehicleDescription;
	}

	public String getBrand() {
		return brand;
	}

	public String getModel() {
		return model;
	}

	public String getVehicleYear() {
		return vehicleYear;
	}

	public BigDecimal getCurrentBidValue() {
		return currentBidValue;
	}

	public BigDecimal getFipeValue() {
		return fipeValue;
	}

	public Double getDistanceKm() {
		return distanceKm;
	}

	public void setDistanceKm(Double distanceKm) {
		this.distanceKm = distanceKm;
	}

	public boolean isClosed() {
		return closed;
	}

	public String getLotUrl() {
		return lotUrl;
	}

	public List<AuctionItemImageResponse> getImages() {
		return images;
	}

	public AuctionListResponse getAuction() {
		return auction;
	}
}
