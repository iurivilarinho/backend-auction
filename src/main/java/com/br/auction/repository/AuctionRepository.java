package com.br.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.br.auction.models.Auction;

public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

	boolean existsByDetranAuctionId(String detranAuctionId);

	boolean existsByProviderCodeAndDetranAuctionId(String providerCode, String detranAuctionId);

	Optional<Auction> findByProviderCodeAndDetranAuctionId(String providerCode, String detranAuctionId);

	@Override
	@EntityGraph(attributePaths = "items")
	Optional<Auction> findById(Long id);

	@Query("""
			SELECT DISTINCT a.city, a.stateCode FROM Auction a
			WHERE a.city IS NOT NULL AND a.city <> ''
			AND (:auctionId IS NULL OR a.id = :auctionId)
			AND (:providerCode IS NULL OR a.providerCode = :providerCode)
			AND (:stateCode IS NULL OR a.stateCode = :stateCode)
			""")
	List<Object[]> findDistinctCities(@Param("auctionId") Long auctionId, @Param("providerCode") String providerCode,
			@Param("stateCode") String stateCode);

}
