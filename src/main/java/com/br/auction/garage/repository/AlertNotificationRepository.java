package com.br.auction.garage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.br.auction.garage.models.AlertNotification;

public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

	boolean existsByAlertIdAndTriggerKey(Long alertId, String triggerKey);

	boolean existsByAlertIdAndAuctionItemId(Long alertId, Long auctionItemId);

	void deleteByAlertId(Long alertId);
}
