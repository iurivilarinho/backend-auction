package com.br.auction.garage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.SavedVehicle;

public interface SavedVehicleRepository extends JpaRepository<SavedVehicle, Long> {

	List<SavedVehicle> findAllByOrderByCreatedAtDesc();

	boolean existsByAuctionItemId(Long auctionItemId);

	Optional<SavedVehicle> findByAuctionItemId(Long auctionItemId);
}
