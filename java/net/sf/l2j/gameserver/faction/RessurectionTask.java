package net.sf.l2j.gameserver.faction;

import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.gameserver.faction.map.MapEvent;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author An4rchy
 *
 */
public class RessurectionTask implements Runnable
{
	private CopyOnWriteArrayList<Player> _players;
	private Location _location1, _location2;
	
	public RessurectionTask(Location location1, Location location2)
	{
		_players = new CopyOnWriteArrayList<>();
		_location1 = location1;
		_location2 = location2;
	}
	
	@Override
	public void run()
	{
		for (Player player : _players)
		{
			player.doRevive();
			player.getStatus().setCpHpMp(player.getStatus().getMaxCp(), player.getStatus().getMaxHp(), player.getStatus().getMaxMp());
			if (player.getFaction() == 1)
				player.teleportTo(_location1, 50);
			else
				player.teleportTo(_location2, 50);
		}
		
		_players.clear();
		_location1 = ((MapEvent)EventManager.getInstance().getActiveEvent()).getRandomSpawn(1);
		_location2 = ((MapEvent)EventManager.getInstance().getActiveEvent()).getRandomSpawn(2);
	}
	
	public void addPlayer(Player player)
	{
		player.sendMessage("You have been added to the ressurection task.");
		_players.add(player);
	}
}
