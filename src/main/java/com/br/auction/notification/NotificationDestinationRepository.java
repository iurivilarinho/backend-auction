package com.br.auction.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

	List<NotificationDestination> findAllByOrderByCreatedAtAsc();

	List<NotificationDestination> findByEnabledTrue();
}
