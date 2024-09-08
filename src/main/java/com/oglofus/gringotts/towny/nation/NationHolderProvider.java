package com.oglofus.gringotts.towny.nation;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.gestern.gringotts.AccountChest;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.Gringotts;
import org.gestern.gringotts.GringottsAccount;
import org.gestern.gringotts.accountholder.AccountHolder;
import org.gestern.gringotts.accountholder.AccountHolderProvider;
import org.gestern.gringotts.event.CalculateStartBalanceEvent;

import com.oglofus.gringotts.towny.TownyConfiguration;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.RenameNationEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;

/**
 * The type Nation holder provider.
 */
public class NationHolderProvider implements AccountHolderProvider, Listener {
    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param id id of account holder
     * @return account holder for id
     */
    @Override
    public @Nullable AccountHolder getAccountHolder(@Nonnull String id) {
        try {
            UUID targetUuid = UUID.fromString(id);

            return getAccountHolder(targetUuid);
        } catch (IllegalArgumentException ignored) {}

        String vaultPrefix = NationAccountHolder.ACCOUNT_TYPE + "-";

        Nation nation;

        if (id.startsWith(vaultPrefix)) {
            nation = TownyUniverse.getInstance().getNation(id.substring(vaultPrefix.length()));
        } else {
            nation = TownyUniverse.getInstance().getNation(id);
        }

        return getAccountHolder(nation);
    }

    /**
     * Get the AccountHolder object mapped to the given id for this provider.
     *
     * @param uuid id of account holder
     * @return account holder for id
     */
    @Override
    public @Nullable AccountHolder getAccountHolder(@Nonnull UUID uuid) {
        Nation nation = TownyUniverse.getInstance().getNation(uuid);

        return getAccountHolder(nation);
    }

    /**
     * Get a TownyAccountHolder for the nation of which player is a resident, if
     * any.
     *
     * @param player player to get nation for
     * @return TownyAccountHolder for the nation of which player is a resident, if
     * any. null otherwise.
     */
    @Override
    public @Nullable AccountHolder getAccountHolder(@Nonnull OfflinePlayer player) {
        try {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

            if (resident == null) {
                return null;
            }

            Town   town   = resident.getTown();
            Nation nation = town.getNation();

            return getAccountHolder(nation);
        } catch (NotRegisteredException ignored) {}

        return null;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    @Override
    public @Nonnull String getType() {
        return NationAccountHolder.ACCOUNT_TYPE;
    }

    /**
     * Gets account names.
     *
     * @return the account names
     */
    @Override
    public @Nonnull Set<String> getAccountNames() {
        return TownyUniverse.getInstance().getNations().stream().map(TownyObject::getName).collect(Collectors.toSet());
    }

    /**
     * Gets account holder.
     *
     * @param nation the nation
     * @return the account holder
     */
    public @Nullable AccountHolder getAccountHolder(@Nullable Nation nation) {
        if (nation == null) {
            return null;
        }

        return new NationAccountHolder(nation);
    }

    /**
     * Rename nation.
     *
     * @param event the event
     */
    @EventHandler
    public void renameNation(RenameNationEvent event) {
        Nation nation = event.getNation();

        AccountHolder holder = this.getAccountHolder(nation);

        if (holder == null) {
            return;
        }

        GringottsAccount account = Gringotts.instance.getAccounting().getAccount(holder);

        if (account == null) {
            return;
        }

        Gringotts.instance.getDao().retrieveChests(account).forEach(AccountChest::updateSign);
    }

    /**
     * Calculate start balance.
     *
     * @param event the event
     */
    @EventHandler
    public void calculateStartBalance(CalculateStartBalanceEvent event) {
        if (!event.holder.getType().equals(getType())) {
            return;
        }

        event.startValue = Configuration.CONF.getCurrency().getCentValue(TownyConfiguration.CONF.nationStartBalance);
    }
}
