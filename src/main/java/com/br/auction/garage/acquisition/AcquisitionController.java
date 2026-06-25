package com.br.auction.garage.acquisition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.br.auction.garage.enums.AcquisitionStatus;
import com.br.auction.garage.enums.DocumentType;
import com.br.auction.garage.enums.ExpenseType;
import com.br.auction.garage.models.Acquisition;
import com.br.auction.garage.models.AcquisitionDocument;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/garage/acquisitions")
@Tag(name = "Garagem - Adquiridos", description = "Gestao dos veiculos adquiridos: gastos, cotacoes, status, documentos e indicadores")
public class AcquisitionController {

	private final AcquisitionService service;

	public AcquisitionController(AcquisitionService service) {
		this.service = service;
	}

	@Operation(summary = "Listar veiculos adquiridos")
	@GetMapping
	public ResponseEntity<List<AcquisitionResponse>> findAll() {
		return ResponseEntity.ok(service.findAll().stream().map(AcquisitionResponse::new).toList());
	}

	@Operation(summary = "Indicadores da garagem")
	@GetMapping("/dashboard")
	public ResponseEntity<AcquisitionDashboardResponse> dashboard() {
		return ResponseEntity.ok(service.dashboard());
	}

	@Operation(summary = "Opcoes (status, tipos de gasto, tipos de documento)")
	@GetMapping("/options")
	public ResponseEntity<Map<String, Object>> options() {
		Map<String, Object> options = Map.of(
				"statuses", labels(AcquisitionStatus.values(), AcquisitionStatus::name, AcquisitionStatus::getDescription),
				"expenseTypes", labels(ExpenseType.values(), ExpenseType::name, ExpenseType::getDescription),
				"documentTypes", labels(DocumentType.values(), DocumentType::name, DocumentType::getDescription));
		return ResponseEntity.ok(options);
	}

	@Operation(summary = "Buscar veiculo adquirido por ID")
	@ApiResponse(responseCode = "404", description = "Nao encontrado")
	@GetMapping("/{id}")
	public ResponseEntity<AcquisitionResponse> findById(@PathVariable Long id) {
		return ResponseEntity.ok(new AcquisitionResponse(service.findById(id)));
	}

	@Operation(summary = "Registrar veiculo adquirido")
	@ApiResponse(responseCode = "201", description = "Criado")
	@PostMapping
	public ResponseEntity<AcquisitionResponse> create(@Valid @RequestBody AcquisitionRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(new AcquisitionResponse(service.create(request)));
	}

	@Operation(summary = "Atualizar veiculo adquirido")
	@PutMapping("/{id}")
	public ResponseEntity<AcquisitionResponse> update(@PathVariable Long id, @Valid @RequestBody AcquisitionRequest request) {
		return ResponseEntity.ok(new AcquisitionResponse(service.update(id, request)));
	}

	@Operation(summary = "Alterar status")
	@PatchMapping("/{id}/status")
	public ResponseEntity<AcquisitionResponse> updateStatus(@PathVariable Long id,
			@Valid @RequestBody AcquisitionStatusUpdateRequest request) {
		return ResponseEntity.ok(new AcquisitionResponse(service.updateStatus(id, request.getStatus())));
	}

	@Operation(summary = "Remover veiculo adquirido")
	@ApiResponse(responseCode = "204", description = "Removido")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Adicionar gasto (direto ou em cotacao)")
	@PostMapping("/{id}/expenses")
	public ResponseEntity<AcquisitionResponse> addExpense(@PathVariable Long id,
			@Valid @RequestBody AcquisitionExpenseRequest request) {
		return ResponseEntity.ok(new AcquisitionResponse(service.addExpense(id, request)));
	}

	@Operation(summary = "Remover gasto")
	@DeleteMapping("/{id}/expenses/{expenseId}")
	public ResponseEntity<AcquisitionResponse> deleteExpense(@PathVariable Long id, @PathVariable Long expenseId) {
		return ResponseEntity.ok(new AcquisitionResponse(service.deleteExpense(id, expenseId)));
	}

	@Operation(summary = "Adicionar orcamento a um gasto em cotacao")
	@PostMapping("/{id}/expenses/{expenseId}/quotes")
	public ResponseEntity<AcquisitionResponse> addQuote(@PathVariable Long id, @PathVariable Long expenseId,
			@Valid @RequestBody ExpenseQuoteRequest request) {
		return ResponseEntity.ok(new AcquisitionResponse(service.addQuote(id, expenseId, request)));
	}

	@Operation(summary = "Marcar orcamento como comprado (vira despesa)")
	@PostMapping("/{id}/expenses/{expenseId}/quotes/{quoteId}/select")
	public ResponseEntity<AcquisitionResponse> selectQuote(@PathVariable Long id, @PathVariable Long expenseId,
			@PathVariable Long quoteId) {
		return ResponseEntity.ok(new AcquisitionResponse(service.selectQuote(id, expenseId, quoteId)));
	}

	@Operation(summary = "Sincronizar documentos do painel do provedor (best-effort)")
	@PostMapping("/{id}/documents/sync")
	public ResponseEntity<DetranPanelService.SyncResult> syncDocuments(@PathVariable Long id) {
		return ResponseEntity.ok(service.syncDocuments(id));
	}

	@Operation(summary = "Anexar documento manualmente")
	@PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AcquisitionResponse> uploadDocument(@PathVariable Long id,
			@RequestParam DocumentType type, @RequestParam MultipartFile file) throws IOException {
		return ResponseEntity.ok(new AcquisitionResponse(service.attachDocument(id, type, file)));
	}

	@Operation(summary = "Baixar documento")
	@GetMapping("/{id}/documents/{documentId}/download")
	public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
		AcquisitionDocument document = service.findDocument(id, documentId);
		MediaType mediaType = document.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(document.getContentType());
		String fileName = document.getFileName() == null ? "documento" : document.getFileName();
		return ResponseEntity.ok()
				.contentType(mediaType)
				.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
				.body(document.getBytes());
	}

	private <T> List<Map<String, String>> labels(T[] values, java.util.function.Function<T, String> name,
			java.util.function.Function<T, String> label) {
		List<Map<String, String>> result = new ArrayList<>();
		for (T value : values) {
			result.add(Map.of("value", name.apply(value), "label", label.apply(value)));
		}
		return result;
	}
}
