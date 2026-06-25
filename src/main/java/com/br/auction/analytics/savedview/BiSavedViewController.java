package com.br.auction.analytics.savedview;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.auction.analytics.savedview.BiSavedViewDtos.SavedViewRequest;
import com.br.auction.analytics.savedview.BiSavedViewDtos.SavedViewResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Visoes salvas do B.I. (consultas geradas no chat: salvar, carregar, favoritar). */
@Tag(name = "BI - Visoes salvas", description = "Consultas/visoes salvas do assistente de B.I.")
@RestController
@RequestMapping("/api/bi/views")
@Validated
public class BiSavedViewController {

    private final BiSavedViewService service;

    public BiSavedViewController(BiSavedViewService service) {
        this.service = service;
    }

    @Operation(summary = "Lista as visoes salvas")
    @GetMapping
    public List<SavedViewResponse> list() {
        return service.list().stream().map(SavedViewResponse::of).toList();
    }

    @Operation(summary = "Carrega uma visao salva")
    @GetMapping("/{id}")
    public SavedViewResponse get(@PathVariable Long id) {
        return SavedViewResponse.of(service.get(id));
    }

    @Operation(summary = "Cria uma visao salva")
    @ApiResponse(responseCode = "201", description = "Visao criada")
    @PostMapping
    public ResponseEntity<SavedViewResponse> create(@Valid @RequestBody SavedViewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedViewResponse.of(service.create(request)));
    }

    @Operation(summary = "Atualiza uma visao salva")
    @PutMapping("/{id}")
    public SavedViewResponse update(@PathVariable Long id, @Valid @RequestBody SavedViewRequest request) {
        return SavedViewResponse.of(service.update(id, request));
    }

    @Operation(summary = "Define a visao como padrao")
    @PutMapping("/{id}/default")
    public SavedViewResponse setDefault(@PathVariable Long id) {
        return SavedViewResponse.of(service.setDefault(id));
    }

    @Operation(summary = "Alterna o favorito de uma visao")
    @PutMapping("/{id}/favorite")
    public SavedViewResponse toggleFavorite(@PathVariable Long id) {
        return SavedViewResponse.of(service.toggleFavorite(id));
    }

    @Operation(summary = "Duplica uma visao")
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<SavedViewResponse> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SavedViewResponse.of(service.duplicate(id)));
    }

    @Operation(summary = "Remove uma visao salva")
    @ApiResponse(responseCode = "204", description = "Visao removida")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
