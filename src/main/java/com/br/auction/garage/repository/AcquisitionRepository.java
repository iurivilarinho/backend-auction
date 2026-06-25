package com.br.auction.garage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.garage.models.Acquisition;

public interface AcquisitionRepository extends JpaRepository<Acquisition, Long> {

	List<Acquisition> findAllByOrderByCreatedAtDesc();

	boolean existsByAuctionItemId(Long auctionItemId);

	long countByStatus(AcquisitionStatus status);
}
