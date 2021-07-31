package me.neznamy.tab.shared.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.shared.ITabPlayer;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.cpu.TabFeature;
import me.neznamy.tab.shared.cpu.UsageType;
import me.neznamy.tab.shared.features.types.Feature;

/**
 * Permission group refresher
 */
public class GroupRefresher implements Feature {

	public static final String DEFAULT_GROUP = "NONE";

	private TAB tab;
	private boolean groupsByPermissions;
	private boolean usePrimaryGroup;
	private List<String> primaryGroupFindingList;
	
	public GroupRefresher(TAB tab) {
		this.tab = tab;
		usePrimaryGroup = tab.getConfiguration().getConfig().getBoolean("use-primary-group", true);
		groupsByPermissions = tab.getConfiguration().getConfig().getBoolean("assign-groups-by-permissions", false);
		primaryGroupFindingList = new ArrayList<>();
		for (Object group : tab.getConfiguration().getConfig().getStringList("primary-group-finding-list", Arrays.asList("Owner", "Admin", "Helper", "default"))){
			primaryGroupFindingList.add(group.toString());
		}
		tab.getCPUManager().startRepeatingMeasuredTask(1000, "refreshing permission groups", getFeatureType(), UsageType.REPEATING_TASK, () -> {

			for (TabPlayer p : tab.getPlayers()) {
				((ITabPlayer) p).setAdditionalGroups(detectAdditionalPermissionGroups(p), false);
				((ITabPlayer) p).setGroup(detectPermissionGroup(p), true);
			}
		});
	}

	public String detectPermissionGroup(TabPlayer p) {
		if (isGroupsByPermissions()) {
			return getByPermission(p);
		}
		if (isUsePrimaryGroup()) {
			return getByPrimary(p);
		}
		return getFromList(p);
	}

	public List<String> detectAdditionalPermissionGroups(TabPlayer p) {
		if (groupsByPermissions) {
			return getAdditionalByPermission(p);
		}
		return new ArrayList<>();
	}

	public String getByPrimary(TabPlayer p) {
		try {
			return tab.getPermissionPlugin().getPrimaryGroup(p);
		} catch (Exception e) {
			return tab.getErrorManager().printError(DEFAULT_GROUP, "Failed to get permission group of " + p.getName() + " using " + tab.getPermissionPlugin().getName() + " v" + tab.getPermissionPlugin().getVersion(), e);
		}
	}

	public String getFromList(TabPlayer p) {
		try {
			String[] playerGroups = tab.getPermissionPlugin().getAllGroups(p);
			if (playerGroups != null && playerGroups.length > 0) {
				for (String groupFromList : primaryGroupFindingList) {
					for (String playerGroup : playerGroups) {
						if (playerGroup.equalsIgnoreCase(groupFromList)) {
							return playerGroup;
						}
					}
				}
				return playerGroups[0];
			} else {
				return DEFAULT_GROUP;
			}
		} catch (Exception e) {
			return tab.getErrorManager().printError(DEFAULT_GROUP, "Failed to get permission groups of " + p.getName() + " using " + tab.getPermissionPlugin().getName() + " v" + tab.getPermissionPlugin().getVersion(), e);
		}
	}

	public String getByPermission(TabPlayer p) {
		for (Object group : primaryGroupFindingList) {
			if (p.hasPermission("tab.group." + group)) {
				return String.valueOf(group);
			}
		}
		tab.getErrorManager().oneTimeConsoleError("Player " + p.getName() + " does not have any group permission while assign-groups-by-permissions is enabled! Did you forget to add his group to primary-group-finding-list?");
		return DEFAULT_GROUP;
	}

	public List<String> getAdditionalByPermission(TabPlayer p) {
		List<String> result = new ArrayList<>();
		boolean skippedPrimary = false;
		for (Object group : primaryGroupFindingList) {
			if (p.hasPermission("tab.group." + group)) {
				if (skippedPrimary) {
					result.add(String.valueOf(group));
				} else {
					skippedPrimary = true;
				}
			}
		}
		return result;
	}

	@Override
	public TabFeature getFeatureType() {
		return TabFeature.GROUP_REFRESHING;
	}

	public boolean isGroupsByPermissions() {
		return groupsByPermissions;
	}

	public boolean isUsePrimaryGroup() {
		return usePrimaryGroup;
	}
}