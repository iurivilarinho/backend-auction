package com.br.auction.garage.repository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.AcquisitionExpense;

@Repository
public interface AcquisitionExpenseRepository extends JpaRepository<AcquisitionExpense, Long> {
}
