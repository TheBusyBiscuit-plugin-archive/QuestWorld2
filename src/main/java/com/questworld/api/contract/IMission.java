package com.questworld.api.contract;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.questworld.api.MissionType;
import com.questworld.api.annotation.NoImpl;

@NoImpl
public interface IMission extends DataObject {
	int getAmount();

	int getCustomInt();

	String getCustomString();

	boolean getDeathReset();

	String getDescription();

	List<String> getDialogue();

	String getDisplayName();

	EntityType getEntity();

	ItemStack getItem();

	Location getLocation();

	boolean getSpawnerSupport();

	int getTimeframe();

	MissionType getType();

	ItemStack getDisplayItem();

	int getIndex();

	IQuest getQuest();

	UUID getUniqueId();

	IMissionState getState();

	String getText();
}
