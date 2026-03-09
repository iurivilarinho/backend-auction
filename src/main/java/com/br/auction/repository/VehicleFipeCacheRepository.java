package com.br.auction.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.auction.models.VehicleFipeCache;

@Repository
public interface VehicleFipeCacheRepository extends JpaRepository<VehicleFipeCache, Long> {

	Optional<VehicleFipeCache> findTopByBrandAndModelAndCreatedAtAfter(String brand, String model, LocalDateTime date);

	Optional<VehicleFipeCache> findByBrandAndModelAndYear(String brand, String model, String year);

}