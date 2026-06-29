package com.br.auction.repository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.auction.models.AuctionItem;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long>, JpaSpecificationExecutor<AuctionItem> {

	boolean existsByLotId(String lotId);

	boolean existsByAuctionIdAndLotId(Long auctionId, String lotId);

	Optional<AuctionItem> findByAuctionIdAndLotId(Long auctionId, String lotId);

	List<AuctionItem> findByAuctionId(Long auctionId);

	/**
	 * Itens de um provedor específico, com o leilão pai já carregado (evita N+1 ao montar o feed
	 * ao-vivo, que lê {@code item.getAuction().getDetranAuctionId()}). Usado para escopar o
	 * {@code /api/feed/lots-live} ao provedor, evitando itens de outro provedor no feed do DETRAN.
	 */
	@EntityGraph(attributePaths = "auction")
	List<AuctionItem> findByAuctionProviderCode(String providerCode);

	List<AuctionItem> findByBrandIsNullAndVehicleDescriptionIsNotNull();

	@Query("""
			SELECT DISTINCT i.brand FROM AuctionItem i
			WHERE i.brand IS NOT NULL AND i.brand <> ''
			AND (:auctionId IS NULL OR i.auction.id = :auctionId)
			AND (:allProviders = TRUE OR i.auction.providerCode IN :providerCodes)
			AND (:stateCode IS NULL OR i.auction.stateCode = :stateCode)
			ORDER BY i.brand
			""")
	List<String> findDistinctBrands(@Param("auctionId") Long auctionId, @Param("allProviders") boolean allProviders,
			@Param("providerCodes") List<String> providerCodes, @Param("stateCode") String stateCode);

	@Query("""
			SELECT DISTINCT i.vehicleYear FROM AuctionItem i
			WHERE i.vehicleYear IS NOT NULL AND i.vehicleYear <> ''
			AND (:auctionId IS NULL OR i.auction.id = :auctionId)
			AND (:allProviders = TRUE OR i.auction.providerCode IN :providerCodes)
			AND (:stateCode IS NULL OR i.auction.stateCode = :stateCode)
			ORDER BY i.vehicleYear DESC
			""")
	List<String> findDistinctYears(@Param("auctionId") Long auctionId, @Param("allProviders") boolean allProviders,
			@Param("providerCodes") List<String> providerCodes, @Param("stateCode") String stateCode);
}
