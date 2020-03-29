package com.cnaude.chairs.sitaddons;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.cnaude.chairs.core.Chairs;
import com.cnaude.chairs.core.ChairsConfig;
import com.cnaude.chairs.core.PlayerSitData;

public class CommandRestrict implements Listener {

	protected final Chairs plugin;
	protected final ChairsConfig config;
	protected final PlayerSitData sitdata;
	public CommandRestrict(Chairs plugin) {
		this.plugin = plugin;
		this.config = plugin.getChairsConfig();
		this.sitdata = plugin.getPlayerSitData();
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		String playercommand = event.getMessage().toLowerCase();
		if (plugin.getPlayerSitData().isSitting(player)) {
			if (config.restrictionsDisableAllCommands) {
				event.setCancelled(true);
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.msgSitCommandRestricted));
				return;
			}
			for (String disabledCommand : config.restrictionsDisabledCommands) {
				if (disabledCommand.startsWith(playercommand)) {
					String therest = playercommand.replace(disabledCommand, "");
					if (therest.isEmpty() || therest.startsWith(" ")) {
						event.setCancelled(true);
						player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.msgSitCommandRestricted));
						return;
					}
				}
			}
		}
	}

}
