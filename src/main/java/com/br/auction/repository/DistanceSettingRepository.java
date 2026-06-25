package com.br.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.models.DistanceSetting;

public interface DistanceSettingRepository extends JpaRepository<DistanceSetting, Long> {
}
