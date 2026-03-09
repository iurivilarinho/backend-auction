package com.br.leilao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.br.leilao.models.AuctionItem;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long>, JpaSpecificationExecutor<AuctionItem> {

	boolean existsByLotId(String lotId);
}