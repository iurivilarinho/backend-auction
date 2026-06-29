package com.br.auction.garage.repository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.AcquisitionDocument;

@Repository
public interface AcquisitionDocumentRepository extends JpaRepository<AcquisitionDocument, Long> {
}
