package com.br.auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.models.AuctionItemImage;

public interface AuctionItemImageRepository extends JpaRepository<AuctionItemImage, Long> {

	List<AuctionItemImage> findByAuctionItemIdOrderByPositionAsc(Long auctionItemId);
}
