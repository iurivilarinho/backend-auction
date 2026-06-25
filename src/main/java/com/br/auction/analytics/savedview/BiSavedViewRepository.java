package com.br.auction.analytics.savedview;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio das visoes salvas do B.I. */
@Repository
public interface BiSavedViewRepository extends JpaRepository<BiSavedView, Long> {

    List<BiSavedView> findAllByOrderByFavoriteDescCreatedAtDesc();
}
