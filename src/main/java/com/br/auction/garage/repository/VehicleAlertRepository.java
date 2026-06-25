package com.br.auction.garage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.VehicleAlert;

public interface VehicleAlertRepository extends JpaRepository<VehicleAlert, Long> {

	List<VehicleAlert> findAllByOrderByCreatedAtDesc();
}
