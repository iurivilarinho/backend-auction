package com.br.auction.garage.acquisition;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.garage.enums.DocumentType;
import com.br.auction.garage.enums.ExpenseStatus;
import com.br.auction.garage.models.Acquisition;
import com.br.auction.garage.models.AcquisitionDocument;
import com.br.auction.garage.models.AcquisitionExpense;
import com.br.auction.garage.models.ExpenseQuote;
import com.br.auction.garage.repository.AcquisitionDocumentRepository;
import com.br.auction.garage.repository.AcquisitionExpenseRepository;
import com.br.auction.garage.repository.AcquisitionRepository;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AcquisitionService {

	private final AcquisitionRepository repository;
	private final AcquisitionExpenseRepository expenseRepository;
	private final AcquisitionDocumentRepository documentRepository;
	private final AuctionItemRepository auctionItemRepository;
	private final DetranPanelService detranPanelService;

	public AcquisitionService(AcquisitionRepository repository, AcquisitionExpenseRepository expenseRepository,
			AcquisitionDocumentRepository documentRepository, AuctionItemRepository auctionItemRepository,
			DetranPanelService detranPanelService) {
		this.repository = repository;
		this.expenseRepository = expenseRepository;
		this.documentRepository = documentRepository;
		this.auctionItemRepository = auctionItemRepository;
		this.detranPanelService = detranPanelService;
	}

	public List<Acquisition> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	public Acquisition findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veiculo adquirido nao encontrado: " + id));
	}

	@Transactional
	public Acquisition create(AcquisitionRequest request) {
		if (request.getAuctionItemId() == null) {
			throw new IllegalArgumentException("O veiculo (auctionItemId) e obrigatorio");
		}
		AuctionItem item = auctionItemRepository.findById(request.getAuctionItemId())
				.orElseThrow(() -> new EntityNotFoundException("Veiculo nao encontrado: " + request.getAuctionItemId()));
		Acquisition acquisition = new Acquisition();
		acquisition.setAuctionItem(item);
		acquisition.setStatus(request.getStatus() == null ? AcquisitionStatus.ARREMATADO : request.getStatus());
		acquisition.setAcquisitionValue(
				request.getAcquisitionValue() != null ? request.getAcquisitionValue() : item.getCurrentBidValue());
		acquisition.setSaleValue(request.getSaleValue());
		acquisition.setAcquiredAt(request.getAcquiredAt());
		acquisition.setInspectionDeadline(request.getInspectionDeadline());
		acquisition.setSoldAt(request.getSoldAt());
		acquisition.setNotes(request.getNotes());
		return repository.save(acquisition);
	}

	@Transactional
	public Acquisition update(Long id, AcquisitionRequest request) {
		Acquisition acquisition = findById(id);
		if (request.getStatus() != null) {
			acquisition.setStatus(request.getStatus());
		}
		acquisition.setAcquisitionValue(request.getAcquisitionValue());
		acquisition.setSaleValue(request.getSaleValue());
		acquisition.setAcquiredAt(request.getAcquiredAt());
		acquisition.setInspectionDeadline(request.getInspectionDeadline());
		acquisition.setSoldAt(request.getSoldAt());
		acquisition.setNotes(request.getNotes());
		return repository.save(acquisition);
	}

	@Transactional
	public Acquisition updateStatus(Long id, AcquisitionStatus status) {
		Acquisition acquisition = findById(id);
		acquisition.setStatus(status);
		return repository.save(acquisition);
	}

	@Transactional
	public void delete(Long id) {
		repository.delete(findById(id));
	}

	@Transactional
	public Acquisition addExpense(Long acquisitionId, AcquisitionExpenseRequest request) {
		Acquisition acquisition = findById(acquisitionId);
		AcquisitionExpense expense = new AcquisitionExpense();
		expense.setType(request.getType());
		expense.setDescription(request.getDescription());
		expense.setIncurredAt(request.getIncurredAt());

		boolean hasQuotes = request.getQuotes() != null && !request.getQuotes().isEmpty();
		if (hasQuotes) {
			expense.setStatus(ExpenseStatus.COTACAO);
			expense.setValue(null);
			for (ExpenseQuoteRequest quoteRequest : request.getQuotes()) {
				ExpenseQuote quote = new ExpenseQuote();
				quote.setPlace(quoteRequest.getPlace());
				quote.setValue(quoteRequest.getValue());
				quote.setNotes(quoteRequest.getNotes());
				quote.setSelected(Boolean.FALSE);
				expense.addQuote(quote);
			}
		} else {
			if (request.getValue() == null) {
				throw new IllegalArgumentException("Informe o valor ou ao menos um orcamento");
			}
			expense.setStatus(ExpenseStatus.COMPRADO);
			expense.setValue(request.getValue());
			expense.setPlace(request.getPlace());
		}
		acquisition.addExpense(expense);
		return repository.save(acquisition);
	}

	@Transactional
	public Acquisition addQuote(Long acquisitionId, Long expenseId, ExpenseQuoteRequest request) {
		Acquisition acquisition = findById(acquisitionId);
		AcquisitionExpense expense = resolveExpense(acquisition, expenseId);
		ExpenseQuote quote = new ExpenseQuote();
		quote.setPlace(request.getPlace());
		quote.setValue(request.getValue());
		quote.setNotes(request.getNotes());
		quote.setSelected(Boolean.FALSE);
		expense.addQuote(quote);
		repository.save(acquisition);
		return acquisition;
	}

	@Transactional
	public Acquisition selectQuote(Long acquisitionId, Long expenseId, Long quoteId) {
		Acquisition acquisition = findById(acquisitionId);
		AcquisitionExpense expense = resolveExpense(acquisition, expenseId);
		ExpenseQuote chosen = null;
		for (ExpenseQuote quote : expense.getQuotes()) {
			boolean isChosen = quote.getId() != null && quote.getId().equals(quoteId);
			quote.setSelected(isChosen);
			if (isChosen) {
				chosen = quote;
			}
		}
		if (chosen == null) {
			throw new EntityNotFoundException("Orcamento nao encontrado: " + quoteId);
		}
		expense.setStatus(ExpenseStatus.COMPRADO);
		expense.setValue(chosen.getValue());
		expense.setPlace(chosen.getPlace());
		repository.save(acquisition);
		return acquisition;
	}

	@Transactional
	public Acquisition deleteExpense(Long acquisitionId, Long expenseId) {
		Acquisition acquisition = findById(acquisitionId);
		AcquisitionExpense expense = resolveExpense(acquisition, expenseId);
		acquisition.getExpenses().remove(expense);
		repository.save(acquisition);
		return acquisition;
	}

	@Transactional
	public DetranPanelService.SyncResult syncDocuments(Long acquisitionId) {
		Acquisition acquisition = findById(acquisitionId);
		return detranPanelService.syncDocuments(acquisition);
	}

	@Transactional
	public Acquisition attachDocument(Long acquisitionId, DocumentType type, MultipartFile file) throws IOException {
		Acquisition acquisition = findById(acquisitionId);
		AcquisitionDocument document = new AcquisitionDocument();
		document.setType(type);
		document.setFileName(file.getOriginalFilename());
		document.setContentType(file.getContentType());
		document.setBytes(file.getBytes());
		acquisition.addDocument(document);
		return repository.save(acquisition);
	}

	public AcquisitionDocument findDocument(Long acquisitionId, Long documentId) {
		AcquisitionDocument document = documentRepository.findById(documentId)
				.orElseThrow(() -> new EntityNotFoundException("Documento nao encontrado: " + documentId));
		if (document.getAcquisition() == null || !document.getAcquisition().getId().equals(acquisitionId)) {
			throw new EntityNotFoundException("Documento nao pertence ao veiculo informado");
		}
		return document;
	}

	public AcquisitionDashboardResponse dashboard() {
		return new AcquisitionDashboardResponse(findAll());
	}

	private AcquisitionExpense resolveExpense(Acquisition acquisition, Long expenseId) {
		return acquisition.getExpenses().stream()
				.filter(expense -> expense.getId() != null && expense.getId().equals(expenseId))
				.findFirst()
				.orElseThrow(() -> new EntityNotFoundException("Gasto nao encontrado: " + expenseId));
	}
}
