package com.songoda.core.hooks.economies;

import net.tnemc.core.Reserve;
import net.tnemc.core.economy.EconomyAPI;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public class ReserveEconomy extends Economy {

    EconomyAPI economyAPI;

    public ReserveEconomy() {
        if (Reserve.instance().economyProvided())
            economyAPI = Reserve.instance().economy();
    }

    @Override
    public boolean isEnabled() {
        return Reserve.instance().isEnabled();
    }

    @Override
    public String getName() {
        return "Reserve";
    }

    @Override
    public String formatEconomy(double amt) {
        return economyAPI.format(BigDecimal.valueOf(amt));
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return economyAPI.getBankHoldings(player.getUniqueId()).doubleValue();
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        return economyAPI.hasHoldings(player.getUniqueId(), new BigDecimal(cost));
    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        return economyAPI.removeHoldings(player.getUniqueId(), new BigDecimal(cost));
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return economyAPI.addHoldings(player.getUniqueId(), new BigDecimal(amount));
    }
}
