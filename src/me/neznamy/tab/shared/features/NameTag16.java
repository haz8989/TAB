package me.neznamy.tab.shared.features;

import java.util.Set;

import me.neznamy.tab.shared.Configs;
import me.neznamy.tab.shared.ITabPlayer;
import me.neznamy.tab.shared.PluginHooks;
import me.neznamy.tab.shared.ProtocolVersion;
import me.neznamy.tab.shared.Shared;
import me.neznamy.tab.shared.cpu.CPUFeature;
import me.neznamy.tab.shared.features.interfaces.JoinEventListener;
import me.neznamy.tab.shared.features.interfaces.Loadable;
import me.neznamy.tab.shared.features.interfaces.QuitEventListener;
import me.neznamy.tab.shared.features.interfaces.Refreshable;
import me.neznamy.tab.shared.features.interfaces.WorldChangeListener;

public class NameTag16 implements Loadable, JoinEventListener, QuitEventListener, WorldChangeListener, Refreshable{

	private Set<String> usedPlaceholders;

	public NameTag16() {
		usedPlaceholders = Configs.config.getUsedPlaceholderIdentifiersRecursive("tagprefix", "tagsuffix");
	}
	@Override
	public void load(){
		for (ITabPlayer p : Shared.getPlayers()) {
			p.properties.get("tagprefix").update();
			p.properties.get("tagsuffix").update();
			if (!p.disabledNametag) p.registerTeam();
		}
		//fixing a 1.8.x client-sided vanilla bug on bukkit mode
		if (ProtocolVersion.SERVER_VERSION.getMinorVersion() == 8 || PluginHooks.viaversion || PluginHooks.protocolsupport) {
			for (ITabPlayer p : Shared.getPlayers()) {
				p.nameTagVisible = !p.hasInvisibility();
			}
			Shared.featureCpu.startRepeatingMeasuredTask(200, "refreshing nametag visibility", CPUFeature.NAMETAG_INVISFIX, new Runnable() {
				public void run() {
					for (ITabPlayer p : Shared.getPlayers()) {
						boolean visible = !p.hasInvisibility();
						if (p.nameTagVisible != visible) {
							p.nameTagVisible = visible;
							p.updateTeam();
						}
					}
				}
			});
		}
	}
	@Override
	public void unload() {
		for (ITabPlayer p : Shared.getPlayers()) {
			if (!p.disabledNametag) p.unregisterTeam();
		}
	}
	@Override
	public void onJoin(ITabPlayer connectedPlayer) {
		connectedPlayer.properties.get("tagprefix").update();
		connectedPlayer.properties.get("tagsuffix").update();
		if (connectedPlayer.disabledNametag) return;
		connectedPlayer.registerTeam();
		for (ITabPlayer all : Shared.getPlayers()) {
			if (all == connectedPlayer) continue; //already registered 2 lines above
			if (!all.disabledNametag) all.registerTeam(connectedPlayer);
		}
	}
	@Override
	public void onQuit(ITabPlayer disconnectedPlayer) {
		if (!disconnectedPlayer.disabledNametag) disconnectedPlayer.unregisterTeam();
	}
	@Override
	public void onWorldChange(ITabPlayer p, String from, String to) {
		if (p.disabledNametag && !p.isDisabledWorld(Configs.disabledNametag, from)) {
			p.unregisterTeam();
		} else if (!p.disabledNametag && p.isDisabledWorld(Configs.disabledNametag, from)) {
			p.registerTeam();
		} else {
			p.updateTeam();
		}
	}
	@Override
	public void refresh(ITabPlayer refreshed, boolean force) {
		if (refreshed.disabledNametag) return;
		boolean prefix = refreshed.properties.get("tagprefix").update();
		boolean suffix = refreshed.properties.get("tagsuffix").update();
		if (prefix || suffix || force) refreshed.updateTeam();
	}
	@Override
	public Set<String> getUsedPlaceholders() {
		return usedPlaceholders;
	}
	@Override
	public CPUFeature getRefreshCPU() {
		return CPUFeature.NAMETAG;
	}
}