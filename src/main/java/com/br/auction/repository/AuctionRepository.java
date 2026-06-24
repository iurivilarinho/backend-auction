package com.br.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import com.br.auction.models.Auction;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

	boolean existsByDetranAuctionId(String detranAuctionId);

	boolean existsByProviderCodeAndDetranAuctionId(String providerCode, String detranAuctionId);

	Optional<Auction> findByProviderCodeAndDetranAuctionId(String providerCode, String detranAuctionId);

	@Override
	@EntityGraph(attributePaths = "items")
	Optional<Auction> findById(Long id);

}
