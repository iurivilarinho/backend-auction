package com.br.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.auction.models.AuctionItem;

public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long>, JpaSpecificationExecutor<AuctionItem> {

	boolean existsByLotId(String lotId);

	boolean existsByAuctionIdAndLotId(Long auctionId, String lotId);

	Optional<AuctionItem> findByAuctionIdAndLotId(Long auctionId, String lotId);

	List<AuctionItem> findByAuctionId(Long auctionId);

	List<AuctionItem> findByBrandIsNullAndVehicleDescriptionIsNotNull();

	@Query("""
			SELECT DISTINCT i.brand FROM AuctionItem i
			WHERE i.brand IS NOT NULL AND i.brand <> ''
			AND (:auctionId IS NULL OR i.auction.id = :auctionId)
			AND (:providerCode IS NULL OR i.auction.providerCode = :providerCode)
			AND (:stateCode IS NULL OR i.auction.stateCode = :stateCode)
			ORDER BY i.brand
			""")
	List<String> findDistinctBrands(@Param("auctionId") Long auctionId, @Param("providerCode") String providerCode,
			@Param("stateCode") String stateCode);

	@Query("""
			SELECT DISTINCT i.vehicleYear FROM AuctionItem i
			WHERE i.vehicleYear IS NOT NULL AND i.vehicleYear <> ''
			AND (:auctionId IS NULL OR i.auction.id = :auctionId)
			AND (:providerCode IS NULL OR i.auction.providerCode = :providerCode)
			AND (:stateCode IS NULL OR i.auction.stateCode = :stateCode)
			ORDER BY i.vehicleYear DESC
			""")
	List<String> findDistinctYears(@Param("auctionId") Long auctionId, @Param("providerCode") String providerCode,
			@Param("stateCode") String stateCode);
}
