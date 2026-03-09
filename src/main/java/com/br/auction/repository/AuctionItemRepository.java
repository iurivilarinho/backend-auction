package com.br.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.br.auction.models.AuctionItem;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long>, JpaSpecificationExecutor<AuctionItem> {

	boolean existsByLotId(String lotId);
}