package com.br.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.models.CityGeocode;

public interface CityGeocodeRepository extends JpaRepository<CityGeocode, Long> {

	Optional<CityGeocode> findByCityIgnoreCaseAndStateIgnoreCase(String city, String state);
}
