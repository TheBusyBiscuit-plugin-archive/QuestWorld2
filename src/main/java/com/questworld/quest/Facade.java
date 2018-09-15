package com.questworld.quest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import com.questworld.Directories;
import com.questworld.QuestWorldPlugin;
import com.questworld.api.contract.ICategory;
import com.questworld.api.contract.IFacade;
import com.questworld.api.contract.IMission;
import com.questworld.api.contract.IQuest;
import com.questworld.manager.PlayerStatus;
import com.questworld.manager.ProgressTracker;
import com.questworld.util.WeakValueMap;

public class Facade implements IFacade {
	private long lastSave = 0;
	private HashMap<UUID, Category> categoryMap = new HashMap<>();
	private WeakValueMap<Integer, Category> categoryPosition = new WeakValueMap<>();
	private WeakValueMap<UUID, Quest> questMap = new WeakValueMap<>();
	private WeakValueMap<UUID, Mission> missionMap = new WeakValueMap<>();

	@Override
	public Category getCategory(UUID uniqueId) {
		return categoryMap.get(uniqueId);
	}

	@Override
	public Quest getQuest(UUID uniqueId) {
		return questMap.getOrNull(uniqueId);
	}

	@Override
	public Mission getMission(UUID uniqueId) {
		return missionMap.getOrNull(uniqueId);
	}

	@Override
	public Category createCategory(String name, int id) {
		Category c = new Category(name, id, this);
		categoryMap.put(c.getUniqueId(), c);
		categoryPosition.putWeak(id, c);
		return c;
	}

	public Quest createQuest(String name, int id, ICategory category) {
		Quest q = new Quest(name, id, (Category) category);
		questMap.putWeak(q.getUniqueId(), q);
		return q;
	}

	public Mission createMission(int id, IQuest quest) {
		Mission m = new Mission(id, (Quest) quest);
		ProgressTracker.loadDialogue(m);
		missionMap.putWeak(m.getUniqueId(), m);
		return m;
	}

	static File fileFor(ICategory category) {
		return new File(QuestWorldPlugin.getAPI().getDataFolders().questing, category.getID() + ".category");
	}

	static File fileFor(IQuest quest) {
		return new File(QuestWorldPlugin.getAPI().getDataFolders().questing,
				stringOfQuest(quest) + ".quest");
	}

	private static int[] splitQuestString(String in) {
		int result[] = { 0, 0 };

		int len = in.length();
		int mid = in.lastIndexOf('C', len - 2);

		result[0] = Integer.parseInt(in.substring(0, mid - 1));
		result[1] = Integer.parseInt(in.substring(mid + 1, len));

		return result;
	}

	static String stringOfQuest(IQuest quest) {
		if (quest == null)
			return null;

		return quest.getID() + "-C" + quest.getCategory().getID();
	}

	private static class ParseData {
		public ParseData(int id, File file) {
			this.id = id;
			this.file = YamlConfiguration.loadConfiguration(file);
		}

		public final int id;
		public final YamlConfiguration file;
	}

	public void load() {

		ArrayList<ParseData> categoryData = new ArrayList<>();
		HashMap<Integer, ArrayList<ParseData>> questData = new HashMap<>();

		for (File file : Directories.listFiles(QuestWorldPlugin.getAPI().getDataFolders().questing)) {
			String fileName = file.getName();

			if (fileName.endsWith(".quest")) {
				int[] parts = splitQuestString(fileName.substring(0, fileName.length() - 6));

				ArrayList<ParseData> files = questData.get(parts[1]);
				if (files == null) {
					files = new ArrayList<>();
					questData.put(parts[1], files);
				}

				files.add(new ParseData(parts[0], file));
			}
			else if (fileName.endsWith(".category"))
				categoryData.add(new ParseData(Integer.parseInt(fileName.substring(0, fileName.length() - 9)), file));
		}

		ArrayList<Category> categories = new ArrayList<>(categoryData.size());

		for (ParseData cData : categoryData) {
			Category category = new Category(cData.id, cData.file, this);
			categories.add(category);

			ArrayList<ParseData> elements = questData.get(cData.id);

			if (elements != null)
				for (ParseData qData : questData.get(cData.id)) {
					Quest q = new Quest(qData.id, qData.file, category);
					category.directAddQuest(q);
					questMap.putWeak(q.getUniqueId(), q);
				}

			categoryMap.put(category.getUniqueId(), category);
			categoryPosition.putWeak(category.getID(), category);
		}

		for (Category category : categories) {
			category.refreshParent();

			for (Quest quest : category.getQuests())
				quest.refreshParent();
		}

		lastSave = System.currentTimeMillis();
	}

	public void onDiscard() {
		for (Category c : categoryMap.values())
			for (Quest q : c.getQuests())
				for (Mission m : q.getMissions())
					ProgressTracker.loadDialogue(m);

		categoryMap.clear();
		questMap.clear();
		missionMap.clear();
	}

	public void save(boolean force) {
		for (Category c : categoryMap.values()) {
			if (force || lastSave < c.getLastModified())
				c.save();

			for (Quest q : c.getQuests())
				if (force || lastSave < q.getLastModified()) {
					q.save();
					for (Mission m : q.getMissions())
						ProgressTracker.saveDialogue(m);
				}
		}

		lastSave = System.currentTimeMillis();
	}

	public void onReload() {
	}

	@Override
	public long getLastSave() {
		return lastSave;
	}

	@Override
	public Collection<Category> getCategories() {
		return categoryMap.values();
	}

	@Deprecated
	@Override
	public Category getCategory(int id) {
		return categoryPosition.getOrNull(id);
	}

	@Override
	public void deleteCategory(ICategory category) {
		for (IQuest quest : category.getQuests())
			deleteQuest(quest);

		PlayerStatus.clearAllCategoryData(category);

		try {
			Files.deleteIfExists(fileFor(category).toPath());
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		categoryMap.remove(category.getUniqueId());
	}

	@Override
	public void deleteQuest(IQuest quest) {
		for (IMission mission : quest.getMissions())
			deleteMission(mission);

		PlayerStatus.clearAllQuestData(quest);

		try {
			Files.deleteIfExists(fileFor(quest).toPath());
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		questMap.remove(((Quest) quest).getUniqueId());
	}

	@Override
	public void deleteMission(IMission mission) {
		((Mission) mission).setDialogue(Arrays.asList());
		ProgressTracker.saveDialogue(mission);

		PlayerStatus.clearAllMissionData(mission);
		missionMap.remove(((Mission) mission).getUniqueId());
	}

	@Override
	public void clearAllUserData(ICategory category) {
		PlayerStatus.clearAllCategoryData(category);
	}

	@Override
	public void clearAllUserData(IQuest quest) {
		PlayerStatus.clearAllQuestData(quest);
	}
}
