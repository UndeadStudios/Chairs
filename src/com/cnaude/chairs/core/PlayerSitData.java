package com.cnaude.chairs.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.cnaude.chairs.api.PlayerChairSitEvent;
import com.cnaude.chairs.api.PlayerChairUnsitEvent;

public class PlayerSitData {

	protected final Chairs plugin;

	protected final Set<UUID> sitDisabled = new HashSet<>();
	protected final HashMap<Player, SitData> sittingPlayers = new HashMap<>();
	protected final HashMap<Block, Player> occupiedBlocks = new HashMap<>();

	public PlayerSitData(Chairs plugin) {
		this.plugin = plugin;
	}

	public void disableSitting(UUID playerUUID) {
		sitDisabled.add(playerUUID);
	}

	public void enableSitting(UUID playerUUID) {
		sitDisabled.remove(playerUUID);
	}

	public boolean isSittingDisabled(UUID playerUUID) {
		return sitDisabled.contains(playerUUID);
	}

	public boolean isSitting(Player player) {
		return sittingPlayers.containsKey(player) && sittingPlayers.get(player).sitting;
	}

	public boolean isBlockOccupied(Block block) {
		return occupiedBlocks.containsKey(block);
	}

	public Player getPlayerOnChair(Block chair) {
		return occupiedBlocks.get(chair);
	}

	public boolean sitPlayer(final Player player,  Block blocktooccupy, Location sitlocation) {
		PlayerChairSitEvent playersitevent = new PlayerChairSitEvent(player, sitlocation.clone());
		Bukkit.getPluginManager().callEvent(playersitevent);
		if (playersitevent.isCancelled()) {
			return false;
		}
		sitlocation = playersitevent.getSitLocation().clone();
		if (plugin.getChairsConfig().msgEnabled) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getChairsConfig().msgSitEnter));
		}
		Entity arrow = Chairs.spawnChairsArrow(sitlocation);
		SitData sitdata = new SitData(
			arrow, player.getLocation(), blocktooccupy,
			Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> resitPlayer(player), 1000, 1000)
		);
		player.teleport(sitlocation);
		arrow.addPassenger(player);
		sittingPlayers.put(player, sitdata);
		occupiedBlocks.put(blocktooccupy, player);
		sitdata.sitting = true;
		return true;
	}

	public void resitPlayer(final Player player) {
		SitData sitdata = sittingPlayers.get(player);
		sitdata.sitting = false;
		Entity prevArrow = sitdata.arrow;
		Entity newArrow = Chairs.spawnChairsArrow(prevArrow.getLocation());
		newArrow.addPassenger(player);
		sitdata.arrow = newArrow;
		prevArrow.remove();
		sitdata.sitting = true;
	}

	public boolean unsitPlayer(Player player) {
		return unsitPlayer(player, true, true);
	}

	public void unsitPlayerForce(Player player, boolean teleport) {
		unsitPlayer(player, false, teleport);
	}

	private boolean unsitPlayer(final Player player, boolean canCancel, boolean teleport) {
		SitData sitdata = sittingPlayers.get(player);
		final PlayerChairUnsitEvent playerunsitevent = new PlayerChairUnsitEvent(player, sitdata.teleportBackLocation.clone(), canCancel);
		Bukkit.getPluginManager().callEvent(playerunsitevent);
		if (playerunsitevent.isCancelled() && playerunsitevent.canBeCancelled()) {
			return false;
		}
		sitdata.sitting = false;
		player.leaveVehicle();
		sitdata.arrow.remove();
		player.setSneaking(false);
		occupiedBlocks.remove(sitdata.occupiedBlock);
		Bukkit.getScheduler().cancelTask(sitdata.resitTaskId);
		sittingPlayers.remove(player);
		if (teleport) {
			player.teleport(playerunsitevent.getTeleportLocation().clone());
		}
		if (plugin.getChairsConfig().msgEnabled) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getChairsConfig().msgSitLeave));
		}
		return true;
	}

	protected static class SitData {

		protected final Location teleportBackLocation;
		protected final Block occupiedBlock;
		protected final int resitTaskId;

		protected boolean sitting;
		protected Entity arrow;

		public SitData(Entity arrow, Location teleportLocation, Block block, int resitTaskId) {
			this.arrow = arrow;
			this.teleportBackLocation = teleportLocation;
			this.occupiedBlock = block;
			this.resitTaskId = resitTaskId;
		}

	}

}
