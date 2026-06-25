package com.br.auction.garage.saved;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.auction.garage.models.SavedVehicle;
import com.br.auction.garage.repository.SavedVehicleRepository;
import com.br.auction.models.AuctionItem;
import com.br.auction.repository.AuctionItemRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SavedVehicleService {

	private final SavedVehicleRepository repository;
	private final AuctionItemRepository auctionItemRepository;

	public SavedVehicleService(SavedVehicleRepository repository, AuctionItemRepository auctionItemRepository) {
		this.repository = repository;
		this.auctionItemRepository = auctionItemRepository;
	}

	public List<SavedVehicle> findAll() {
		return repository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public SavedVehicle save(SavedVehicleRequest request) {
		SavedVehicle existing = repository.findByAuctionItemId(request.getAuctionItemId()).orElse(null);
		if (existing != null) {
			existing.setNotes(request.getNotes());
			return repository.save(existing);
		}
		AuctionItem item = auctionItemRepository.findById(request.getAuctionItemId())
				.orElseThrow(() -> new EntityNotFoundException("Veiculo nao encontrado: " + request.getAuctionItemId()));
		SavedVehicle saved = new SavedVehicle();
		saved.setAuctionItem(item);
		saved.setNotes(request.getNotes());
		return repository.save(saved);
	}

	@Transactional
	public void delete(Long id) {
		SavedVehicle saved = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Veiculo salvo nao encontrado: " + id));
		repository.delete(saved);
	}
}
