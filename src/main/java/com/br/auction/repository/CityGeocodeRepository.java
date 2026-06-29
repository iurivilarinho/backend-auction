package com.br.auction.repository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.models.CityGeocode;

@Repository
public interface CityGeocodeRepository extends JpaRepository<CityGeocode, Long> {

	Optional<CityGeocode> findByCityIgnoreCaseAndStateIgnoreCase(String city, String state);
}
