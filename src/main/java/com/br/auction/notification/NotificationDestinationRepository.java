package com.br.auction.notification;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

	List<NotificationDestination> findAllByOrderByCreatedAtAsc();

	List<NotificationDestination> findByEnabledTrue();
}
