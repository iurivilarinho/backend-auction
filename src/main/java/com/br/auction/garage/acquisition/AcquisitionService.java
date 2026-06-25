package com.br.auction.garage.acquisition;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
		Acquisition acquisition = new Acquisition();
		AuctionItem item = null;
		if (request.getAuctionItemId() != null) {
			item = auctionItemRepository.findById(request.getAuctionItemId()).orElseThrow(
					() -> new EntityNotFoundException("Veiculo nao encontrado: " + request.getAuctionItemId()));
			acquisition.setAuctionItem(item);
		} else if (request.getVehicleDescription() == null || request.getVehicleDescription().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o veiculo (auctionItemId) ou a descricao do veiculo");
		}
		acquisition.setVehicleDescription(request.getVehicleDescription());
		acquisition.setLotReference(request.getLotReference());
		acquisition.setSourceUrl(request.getSourceUrl());
		acquisition.setStatus(request.getStatus() == null ? AcquisitionStatus.ARREMATADO : request.getStatus());
		acquisition.setAcquisitionValue(request.getAcquisitionValue() != null ? request.getAcquisitionValue()
				: (item != null ? item.getCurrentBidValue() : null));
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
				quote.setUrl(quoteRequest.getUrl());
				quote.setNotes(quoteRequest.getNotes());
				quote.setSelected(Boolean.FALSE);
				expense.addQuote(quote);
			}
		} else {
			if (request.getValue() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o valor ou ao menos um orcamento");
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
		quote.setUrl(request.getUrl());
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

	/**
	 * Sincroniza os documentos (Nota, Carta, Alvara) de um veiculo adquirido baixando-os do painel
	 * e guardando-os no banco. Casa a aquisicao com o arremate do painel pela referencia do lote.
	 */
	@Transactional
	public DetranPanelService.SyncResult syncDocuments(Long acquisitionId) {
		Acquisition acquisition = findById(acquisitionId);
		DetranPanelService.PanelResult result = detranPanelService.fetchArremates(true);
		if (!result.success()) {
			return new DetranPanelService.SyncResult(false, result.message(), 0);
		}
		DetranPanelService.PanelArremate match = matchArremate(acquisition, result.arremates());
		if (match == null) {
			return new DetranPanelService.SyncResult(false,
					"Nao encontrei este veiculo entre os seus arremates no painel.", 0);
		}
		int added = attachPanelDocuments(acquisition, match);
		if (added > 0) {
			repository.save(acquisition);
		}
		return new DetranPanelService.SyncResult(true,
				added > 0 ? added + " documento(s) sincronizado(s) do painel."
						: "Documentos ja estavam sincronizados.",
				added);
	}

	/** Casa a aquisicao com um arremate do painel pela referencia "Leilao X / Lote Y". */
	private DetranPanelService.PanelArremate matchArremate(Acquisition acquisition,
			List<DetranPanelService.PanelArremate> arremates) {
		String reference = acquisition.getLotReference();
		for (DetranPanelService.PanelArremate a : arremates) {
			if (reference != null && reference.equalsIgnoreCase(a.reference())) {
				return a;
			}
		}
		// Sem referencia (aquisicao manual): tenta casar pela descricao do veiculo.
		String description = acquisition.resolveVehicleDescription();
		if (description != null) {
			for (DetranPanelService.PanelArremate a : arremates) {
				if (description.equalsIgnoreCase(a.descricao())) {
					return a;
				}
			}
		}
		return null;
	}

	/** Anexa os documentos do painel que ainda nao existem na aquisicao; retorna quantos adicionou. */
	private int attachPanelDocuments(Acquisition acquisition, DetranPanelService.PanelArremate arremate) {
		Set<DocumentType> present = new HashSet<>();
		for (AcquisitionDocument existing : acquisition.getDocuments()) {
			present.add(existing.getType());
		}
		int added = 0;
		for (DetranPanelService.PanelDoc doc : arremate.documents()) {
			if (present.contains(doc.type())) {
				continue;
			}
			AcquisitionDocument document = new AcquisitionDocument();
			document.setType(doc.type());
			document.setFileName(doc.fileName());
			document.setContentType(doc.contentType());
			document.setBytes(doc.bytes());
			acquisition.addDocument(document);
			present.add(doc.type());
			added++;
		}
		return added;
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

	private static final Pattern MONEY = Pattern.compile("R\\$\\s*[\\d.]+,\\d{2}");

	/**
	 * Importa os arremates a partir do HTML da pagina /arremates (copiada enquanto o usuario
	 * esta logado no painel). O login do painel e SAML/SSO e nao pode ser automatizado de
	 * forma confiavel, por isso a importacao trabalha sobre o HTML fornecido.
	 */
	@Transactional
	public ArrematesImportResult importArremates(String html) {
		if (html == null || html.isBlank()) {
			return new ArrematesImportResult(0, 0,
					"Cole o HTML da pagina de arremates (logado em https://leilao.detran.mg.gov.br/arremates) para importar.");
		}
		Document document = Jsoup.parse(html);

		// Conjunto de referencias de lote ja importadas (idempotencia entre execucoes).
		Set<String> existing = new HashSet<>();
		for (Acquisition acquisition : repository.findAll()) {
			if (acquisition.getLotReference() != null) {
				existing.add(acquisition.getLotReference().toLowerCase());
			}
		}

		// 1) Tenta o parser preciso da tabela do painel (Leilao, Lote, Descricao, Valor Arremate...).
		Element table = findArrematesTable(document);
		if (table != null) {
			return importFromArrematesTable(table, existing);
		}

		// 2) Fallback generico (HTML colado de layout diferente): detecta dinheiro por linha.
		Elements rows = new Elements();
		rows.addAll(document.select("table tbody tr"));
		rows.addAll(document.select(".card, .arremate, .lote"));
		if (rows.isEmpty()) {
			rows.addAll(document.select("li"));
		}

		Set<String> seen = new HashSet<>();
		int imported = 0;
		int skipped = 0;
		for (Element row : rows) {
			String text = row.text().trim();
			Matcher money = MONEY.matcher(text);
			if (!money.find()) {
				continue;
			}
			String description = extractDescription(row, text);
			if (description == null || description.isBlank()) {
				continue;
			}
			if (!seen.add(description.toLowerCase())) {
				skipped++;
				continue;
			}
			Acquisition acquisition = new Acquisition();
			acquisition.setVehicleDescription(description);
			acquisition.setStatus(AcquisitionStatus.ARREMATADO);
			acquisition.setAcquisitionValue(parseMoney(money.group()));
			Element link = row.selectFirst("a[href]");
			if (link != null) {
				acquisition.setSourceUrl(link.absUrl("href"));
			}
			repository.save(acquisition);
			imported++;
		}

		String message = imported > 0
				? imported + " arremate(s) importado(s)" + (skipped > 0 ? " (" + skipped + " duplicado(s))." : ".")
				: "Nenhum arremate reconhecido no HTML. Confirme que copiou a pagina /arremates estando logado.";
		return new ArrematesImportResult(imported, skipped, message);
	}

	/** Localiza a tabela de arremates pelo cabecalho (precisa ter as colunas Lote e Valor Arremate). */
	private Element findArrematesTable(Document document) {
		for (Element table : document.select("table")) {
			String header = table.select("th").text().toLowerCase();
			if (header.contains("lote") && header.contains("valor")) {
				return table;
			}
		}
		return null;
	}

	/**
	 * Parser preciso da tabela do painel de arremates. Mapeia as colunas pelo cabecalho e cria uma
	 * aquisicao por linha, usando a referencia "Leilao X / Lote Y" para evitar duplicar em re-importacoes.
	 */
	private ArrematesImportResult importFromArrematesTable(Element table, Set<String> existing) {
		List<String> headers = new ArrayList<>();
		for (Element th : table.select("tr").first().select("th, td")) {
			headers.add(norm(th.text()));
		}
		int idxLeilao = indexOfHeader(headers, "leilao");
		int idxLote = indexOfHeader(headers, "lote");
		int idxDescricao = indexOfHeader(headers, "descricao");
		int idxValor = indexOfHeader(headers, "valor");
		int idxCondicao = indexOfHeader(headers, "condicao");
		int idxStatus = indexOfHeader(headers, "status");

		int imported = 0;
		int skipped = 0;
		Set<String> seen = new HashSet<>();
		Elements bodyRows = table.select("tbody tr");
		if (bodyRows.isEmpty()) {
			bodyRows = table.select("tr");
		}
		for (Element row : bodyRows) {
			Elements cells = row.select("td");
			if (cells.isEmpty()) {
				continue; // linha de cabecalho
			}
			String descricao = cell(cells, idxDescricao);
			if (descricao == null || descricao.isBlank()) {
				continue;
			}
			String leilao = cell(cells, idxLeilao);
			String lote = cell(cells, idxLote);
			String reference = ("Leilao " + (leilao == null ? "?" : leilao) + " / Lote "
					+ (lote == null ? "?" : lote)).trim();
			String dedupeKey = reference.toLowerCase();
			if (existing.contains(dedupeKey) || !seen.add(dedupeKey)) {
				skipped++;
				continue;
			}

			Acquisition acquisition = new Acquisition();
			acquisition.setVehicleDescription(truncate(descricao));
			acquisition.setLotReference(reference);
			acquisition.setStatus(AcquisitionStatus.ARREMATADO);
			acquisition.setAcquisitionValue(parseMoney(cell(cells, idxValor)));
			String condicao = cell(cells, idxCondicao);
			String status = cell(cells, idxStatus);
			StringBuilder notes = new StringBuilder();
			if (condicao != null && !condicao.isBlank()) {
				notes.append("Condicao: ").append(condicao);
			}
			if (status != null && !status.isBlank()) {
				if (notes.length() > 0) {
					notes.append(" | ");
				}
				notes.append("Situacao no painel: ").append(status);
			}
			if (notes.length() > 0) {
				acquisition.setNotes(notes.toString());
			}
			repository.save(acquisition);
			imported++;
		}

		String message = imported > 0
				? imported + " arremate(s) importado(s) do painel"
						+ (skipped > 0 ? " (" + skipped + " ja existente(s))." : ".")
				: (skipped > 0 ? "Nenhum arremate novo: " + skipped + " ja estavam importados."
						: "Nenhum arremate encontrado na pagina do painel.");
		return new ArrematesImportResult(imported, skipped, message);
	}

	private int indexOfHeader(List<String> headers, String keyword) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i).contains(keyword)) {
				return i;
			}
		}
		return -1;
	}

	private String cell(Elements cells, int index) {
		if (index < 0 || index >= cells.size()) {
			return null;
		}
		String text = cells.get(index).text().trim();
		return text.isEmpty() ? null : text;
	}

	/** Normaliza texto para comparacao de cabecalho (minusculo, sem acento). */
	private String norm(String value) {
		if (value == null) {
			return "";
		}
		String lower = value.toLowerCase().trim();
		return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
	}

	/**
	 * Importa os arremates automaticamente: faz login no painel com o perfil salvo (arrematante),
	 * baixa o HTML da pagina /arremates e reaproveita o mesmo parser do fluxo manual. Se o login
	 * automatico falhar, retorna a mensagem do painel para que o usuario possa colar o HTML.
	 */
	@Transactional
	public ArrematesImportResult importArrematesAutomatically() {
		DetranPanelService.PanelResult panel = detranPanelService.fetchArremates(true);
		if (!panel.success()) {
			return new ArrematesImportResult(0, 0, panel.message());
		}
		if (panel.arremates().isEmpty()) {
			return new ArrematesImportResult(0, 0, "Nenhum veiculo arrematado encontrado no painel.");
		}

		// Indexa as aquisicoes existentes por referencia de lote (idempotencia + backfill de documentos).
		Map<String, Acquisition> byReference = new HashMap<>();
		for (Acquisition acquisition : repository.findAll()) {
			if (acquisition.getLotReference() != null) {
				byReference.put(acquisition.getLotReference().toLowerCase(), acquisition);
			}
		}

		int imported = 0;
		int skipped = 0;
		int documents = 0;
		for (DetranPanelService.PanelArremate arremate : panel.arremates()) {
			String reference = arremate.reference();
			Acquisition existing = byReference.get(reference.toLowerCase());
			if (existing != null) {
				// Ja importado: completa os documentos que faltarem (ex.: liberados depois).
				int added = attachPanelDocuments(existing, arremate);
				if (added > 0) {
					repository.save(existing);
					documents += added;
				}
				skipped++;
				continue;
			}

			Acquisition acquisition = new Acquisition();
			acquisition.setVehicleDescription(truncate(arremate.descricao()));
			acquisition.setLotReference(reference);
			acquisition.setStatus(AcquisitionStatus.ARREMATADO);
			acquisition.setAcquisitionValue(parseMoney(arremate.valor()));
			StringBuilder notes = new StringBuilder();
			if (arremate.condicao() != null) {
				notes.append("Condicao: ").append(arremate.condicao());
			}
			if (arremate.status() != null) {
				if (notes.length() > 0) {
					notes.append(" | ");
				}
				notes.append("Situacao no painel: ").append(arremate.status());
			}
			if (notes.length() > 0) {
				acquisition.setNotes(notes.toString());
			}
			documents += attachPanelDocuments(acquisition, arremate);
			repository.save(acquisition);
			imported++;
		}

		StringBuilder message = new StringBuilder();
		if (imported > 0) {
			message.append(imported).append(" arremate(s) importado(s) do painel");
		} else {
			message.append("Nenhum arremate novo");
		}
		if (skipped > 0) {
			message.append(imported > 0 ? " (" : ": ").append(skipped).append(" ja existente(s)")
					.append(imported > 0 ? ")" : "");
		}
		message.append(documents > 0 ? "; " + documents + " documento(s) baixado(s)." : ".");
		return new ArrematesImportResult(imported, skipped, message.toString());
	}

	private String extractDescription(Element row, String fallback) {
		for (String selector : List.of("b", "strong", ".titulo", ".descricao", "td", "h5", "h6")) {
			Element element = row.selectFirst(selector);
			if (element != null && !element.text().isBlank() && element.text().length() >= 4) {
				return truncate(element.text().trim());
			}
		}
		return truncate(fallback);
	}

	private String truncate(String value) {
		return value.length() > 280 ? value.substring(0, 280) : value;
	}

	private BigDecimal parseMoney(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String normalized = raw.replace("R$", "").replace(".", "").replace(",", ".").replaceAll("[^0-9.]", "").trim();
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			return null;
		}
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
