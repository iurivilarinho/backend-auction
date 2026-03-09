package com.br.leilao.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.leilao.enums.AuctionStatus;
import com.br.leilao.enums.LotType;
import com.br.leilao.models.Auction;
import com.br.leilao.models.AuctionItem;
import com.br.leilao.repository.AuctionItemRepository;
import com.br.leilao.repository.AuctionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import specification.AuctionItemSpecification;
import specification.AuctionSpecification;

@Service
public class AuctionService {
	private final AuctionRepository auctionRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final EntityManager entityManager;

	public AuctionService(AuctionRepository auctionRepository, AuctionItemRepository auctionItemRepository,
			EntityManager entityManager) {

		this.auctionRepository = auctionRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.entityManager = entityManager;
	}

	@Transactional(readOnly = true)
	public Page<Auction> findAll(List<AuctionStatus> status, String search, Pageable page) {
		return auctionRepository.findAll(AuctionSpecification.searchAllFields(search, entityManager)
				.and(AuctionSpecification.statusEquals(status)), page);
	}

	@Transactional(readOnly = true)
	public Auction findById(Long auctionId) {
		return auctionRepository.findById(auctionId)
				.orElseThrow(() -> new EntityNotFoundException("Leilão não encontrado para ID: " + auctionId));
	}

	@Transactional(readOnly = true)
	public Page<AuctionItem> findAllItems(List<AuctionStatus> status, List<LotType> type, String search,
			Pageable page) {
		return auctionItemRepository.findAll(AuctionItemSpecification.searchAllFields(search, entityManager)
				.and(AuctionItemSpecification.typeEquals(type))
				.and(AuctionItemSpecification.auctionStatusEquals(status)), page);
	}

}
