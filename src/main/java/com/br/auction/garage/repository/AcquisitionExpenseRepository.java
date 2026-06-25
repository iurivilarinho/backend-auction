package com.br.auction.garage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.AcquisitionExpense;

public interface AcquisitionExpenseRepository extends JpaRepository<AcquisitionExpense, Long> {
}
