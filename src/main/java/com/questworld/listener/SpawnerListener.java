package com.questworld.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.questworld.api.QuestWorld;
import com.questworld.util.AutoListener;

public class SpawnerListener extends AutoListener {

	public SpawnerListener(Plugin plugin) {
		register(plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent e) {
		if (e.getSpawnReason().equals(SpawnReason.SPAWNER))
			e.getEntity().setMetadata("spawned_by_spawner",
					new FixedMetadataValue(QuestWorld.getPlugin(), "QuestWorld"));
	}
}