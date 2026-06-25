package com.br.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.br.auction.models.AuctionItem;

public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long>, JpaSpecificationExecutor<AuctionItem> {

	boolean existsByLotId(String lotId);

	boolean existsByAuctionIdAndLotId(Long auctionId, String lotId);

	Optional<AuctionItem> findByAuctionIdAndLotId(Long auctionId, String lotId);
}
