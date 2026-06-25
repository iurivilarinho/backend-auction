package com.br.auction.garage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.AcquisitionDocument;

public interface AcquisitionDocumentRepository extends JpaRepository<AcquisitionDocument, Long> {
}
