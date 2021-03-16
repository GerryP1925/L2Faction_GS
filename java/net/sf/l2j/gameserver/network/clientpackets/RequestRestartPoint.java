package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.faction.EventListeners;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.location.Location;

public final class RequestRestartPoint extends L2GameClientPacket
{
	protected static final Location JAIL_LOCATION = new Location(-114356, -249645, -2984);
	
	protected int _requestType;
	
	@Override
	protected void readImpl()
	{
		_requestType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		// TODO Needed? Possible?
		if (player.isFakeDeath())
		{
			player.stopFakeDeath(true);
			return;
		}
		
		if (!player.isDead())
			return;
		
		// Schedule a respawn delay if player is part of a clan registered in an active siege.
		if (player.getClan() != null)
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(player);
			if (siege != null && siege.checkSide(player.getClan(), SiegeSide.ATTACKER))
			{
				ThreadPool.schedule(() -> portPlayer(player), Config.ATTACKERS_RESPAWN_DELAY);
				return;
			}
		}
		
		portPlayer(player);
	}
	
	/**
	 * Teleport the {@link Player} to the associated {@link Location}, based on _requestType.
	 * @param player : The player set as parameter.
	 */
	private void portPlayer(Player player)
	{
		// Enforce type.
		if (player.isInJail())
			_requestType = 27;
		
		if (_requestType == 0) // respawn button
			EventListeners.onRespawnButton(player);
		else if (_requestType == 1) // clan hall
		{
			
		}
		else if (_requestType == 2) // castle
		{
			
		}
		else if (_requestType == 3) // to town
		{
			if (player.isDead())
				player.doRevive();
			
			player.teleportTo(MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN), 20);
			EventListeners.onRecall(player);
		}
		else if (_requestType == 4) // fixed
		{
			if (!player.isGM())
				return;
			
			player.teleportTo(player.getPosition(), 0);
		}
		else if (_requestType == 27) // jail
			player.teleportTo(JAIL_LOCATION, 20);
	}
}