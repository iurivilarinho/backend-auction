package com.br.auction.analytics.savedview;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.br.auction.analytics.savedview.BiSavedViewDtos.SavedViewRequest;

/** Regras das visoes salvas do B.I. (app single-user: visoes globais, sem dono). */
@Service
public class BiSavedViewService {

    private final BiSavedViewRepository repository;

    public BiSavedViewService(BiSavedViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BiSavedView> list() {
        return repository.findAllByOrderByFavoriteDescCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public BiSavedView get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visao nao encontrada."));
    }

    @Transactional
    public BiSavedView create(SavedViewRequest request) {
        BiSavedView view = new BiSavedView();
        apply(view, request);
        return repository.save(view);
    }

    @Transactional
    public BiSavedView update(Long id, SavedViewRequest request) {
        BiSavedView view = get(id);
        apply(view, request);
        return repository.save(view);
    }

    @Transactional
    public BiSavedView setDefault(Long id) {
        BiSavedView target = get(id);
        for (BiSavedView other : repository.findAll()) {
            if (other.isDefault() && !other.getId().equals(target.getId())) {
                other.setDefault(false);
                repository.save(other);
            }
        }
        target.setDefault(true);
        return repository.save(target);
    }

    @Transactional
    public BiSavedView setArchived(Long id, boolean archived) {
        BiSavedView view = get(id);
        view.setArchived(archived);
        return repository.save(view);
    }

    @Transactional
    public BiSavedView toggleArchived(Long id) {
        BiSavedView view = get(id);
        view.setArchived(!view.isArchived());
        return repository.save(view);
    }

    @Transactional
    public BiSavedView toggleFavorite(Long id) {
        BiSavedView view = get(id);
        view.setFavorite(!view.isFavorite());
        return repository.save(view);
    }

    @Transactional
    public BiSavedView duplicate(Long id) {
        BiSavedView source = get(id);
        BiSavedView copy = new BiSavedView();
        copy.setName(source.getName() + " (copia)");
        copy.setScope(source.getScope());
        copy.setPayload(source.getPayload());
        copy.setShared(source.isShared());
        return repository.save(copy);
    }

    @Transactional
    public void delete(Long id) {
        BiSavedView view = get(id);
        repository.delete(view);
    }

    private void apply(BiSavedView view, SavedViewRequest request) {
        view.setName(request.name().trim());
        view.setScope(request.scope());
        view.setPayload(request.payload());
        view.setShared(Boolean.TRUE.equals(request.shared()));
        if (request.favorite() != null) {
            view.setFavorite(request.favorite());
        }
    }
}
