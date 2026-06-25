package com.br.auction.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.enums.AuctionProvider;
import com.br.auction.enums.AuctionStatus;
import com.br.auction.enums.LotType;
import com.br.auction.models.Auction;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;
import com.br.auction.repository.AuctionRepository;

import com.br.auction.specification.AuctionItemSpecification;
import com.br.auction.specification.AuctionSpecification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

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
	public Page<Auction> findAll(List<AuctionStatus> status, String search, String providerCode, String stateCode,
			Pageable page) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;

		return auctionRepository.findAll(AuctionSpecification.searchAllFields(search, entityManager)
				.and(AuctionSpecification.statusEquals(status))
				.and(AuctionSpecification.providerCodeEquals(provider.getCode()))
				.and(AuctionSpecification.stateCodeEquals(resolvedStateCode)), page);
	}

	@Transactional(readOnly = true)
	public Auction findById(Long auctionId) {
		return auctionRepository.findById(auctionId)
				.orElseThrow(() -> new EntityNotFoundException("Leilao nao encontrado para ID: " + auctionId));
	}

	@Transactional(readOnly = true)
	public Page<AuctionItem> findAllItems(Long auctionId, List<AuctionStatus> status, List<LotType> type,
			String search, String providerCode, String stateCode, Pageable page) {
		AuctionProvider provider = AuctionProvider.fromCodeOrDefault(providerCode);
		String resolvedStateCode = stateCode == null || stateCode.isBlank() ? provider.getStateCode() : stateCode;

		return auctionItemRepository.findAll(AuctionItemSpecification.searchAllFields(search, entityManager)
				.and(AuctionItemSpecification.auctionIdEquals(auctionId))
				.and(AuctionItemSpecification.typeEquals(type))
				.and(AuctionItemSpecification.auctionStatusEquals(status))
				.and(AuctionItemSpecification.providerCodeEquals(provider.getCode()))
				.and(AuctionItemSpecification.stateCodeEquals(resolvedStateCode)), page);
	}
}
