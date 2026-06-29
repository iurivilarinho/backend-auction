package com.br.auction.repository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.models.DistanceSetting;

@Repository
public interface DistanceSettingRepository extends JpaRepository<DistanceSetting, Long> {
}
