package net.sf.l2j.gameserver.faction.map;

import java.util.ArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.faction.FactionEvent;
import net.sf.l2j.gameserver.faction.RessurectionTask;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author An4rchy
 *
 */
public abstract class MapEvent extends FactionEvent
{
	protected ArrayList<Location> _teamOneSpawns, _teamTwoSpawns;
	protected RessurectionTask _resTask;
	private Location _radar;
	
	public MapEvent(int id, String name, int duration, ArrayList<Location> teamOneSpawns, ArrayList<Location> teamTwoSpawns, Location radar)
	{
		super(id, name, duration);
		
		_teamOneSpawns = teamOneSpawns;
		_teamTwoSpawns = teamTwoSpawns;
		_radar = radar;
	}
	
	public Location getRandomSpawn(int team)
	{
		if (team == 1)
			return _teamOneSpawns.get(Rnd.get(_teamOneSpawns.size()));
		
		return _teamTwoSpawns.get(Rnd.get(_teamTwoSpawns.size()));
	}
	
	@Override
	public synchronized void addParticipant(Player player)
	{
		super.addParticipant(player);
		
		player.teleportTo(player.getFaction() == 1 ? getRandomSpawn(1) : getRandomSpawn(2), 50);
		if (_radar != null)
			player.getRadarList().addMarker(_radar);
	}
	
	@Override
	public synchronized void removeParticipant(Player player)
	{
		player.getRadarList().removeAllMarkers();
		super.removeParticipant(player);
	}
	
	public abstract void onKill(Playable killer, Player victim);
}
