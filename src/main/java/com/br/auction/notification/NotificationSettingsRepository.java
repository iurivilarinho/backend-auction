package com.br.auction.notification;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
}
