package com.br.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.br.auction.models.CityCentroid;

@Repository
public interface CityCentroidRepository extends JpaRepository<CityCentroid, Long> {

	Optional<CityCentroid> findFirstByNormalizedNameAndUf(String normalizedName, String uf);
}
