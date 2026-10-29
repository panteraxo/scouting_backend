package com.scouting.scouting.player.service;

import com.scouting.scouting.player.dto.PlayerStatsResponse;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerStatsService {

    private final PlayerAccessService accessService;
    private final PlayerHistoryEntryRepository historyRepository;
    private final PlayerStatsCalculator calculator = new PlayerStatsCalculator();

    public PlayerStatsService(PlayerAccessService accessService,
                              PlayerHistoryEntryRepository historyRepository) {
        this.accessService = accessService;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse stats(AppUserPrincipal actor, Long playerId) {
        accessService.requireView(actor, playerId);
        return calculator.calculate(playerId,
                historyRepository.findByPlayerProfile_IdOrderByActivityDateDescIdDesc(playerId));
    }
}
