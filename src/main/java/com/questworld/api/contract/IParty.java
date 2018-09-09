package com.questworld.api.contract;

import java.util.Set;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface IParty {
	enum LeaveReason {
		ABANDON,
		DISCONNECT,
		KICKED,
	}

	Set<OfflinePlayer> getFullGroup();

	Set<OfflinePlayer> getMembers();

	OfflinePlayer getLeader();

	boolean isLeader(OfflinePlayer player);

	int getSize();

	boolean hasInvited(OfflinePlayer player);

	void playerLeave(OfflinePlayer traitor, LeaveReason reason);

	void invitePlayer(Player p);

	void playerJoin(Player p);
}
