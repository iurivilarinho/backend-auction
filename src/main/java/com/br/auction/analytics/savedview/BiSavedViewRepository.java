package com.br.auction.analytics.savedview;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio das visoes salvas do B.I. */
@Repository
public interface BiSavedViewRepository extends JpaRepository<BiSavedView, Long> {

    List<BiSavedView> findAllByOrderByFavoriteDescCreatedAtDesc();
}
