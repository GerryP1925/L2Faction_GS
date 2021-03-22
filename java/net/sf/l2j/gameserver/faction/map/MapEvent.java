package net.sf.l2j.gameserver.faction.map;

import java.util.ArrayList;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.faction.*;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

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

	/**
	 * Sets state to ATTACKING and registers 5 tasks ('RES' task, 'END' task, 'WARN' task, 'REWARD' task).
	 *
	 */
	@Override
	public void run()
	{
		super.run();

		_state = EventState.ATTACKING;

		if (_resTask != null)
			registerTask("Res", _resTask, 10 * 1000, 10 * 1000);
		registerTask("End", () -> endEvent(), getDuration() * 1000 * 60);
		registerTask("Warn", () -> World.announceToOnlinePlayers("5 minutes left for "+getName()+" Event to end!"), (getDuration() - 5) * 1000 * 60);
		registerTask("Reward", () -> endBattle(), (getDuration() * 1000 * 60) - (10 * 1000));
	}

	protected void endBattle()
	{
		_state = EventState.REWARDING;
		cancelTask("Info");
		_participants.forEach(p -> p.getActingPlayer().abortAll(true));
	}

	public Location getRandomSpawn(int team)
	{
		if (team == 1)
			return _teamOneSpawns.get(Rnd.get(_teamOneSpawns.size()));
		
		return _teamTwoSpawns.get(Rnd.get(_teamTwoSpawns.size()));
	}

	@Override
	protected void announceEvent()
	{
		super.announceEvent();
		World.announceToOnlinePlayers("Event Duration - "+getDuration()+" minutes");
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

	@Override
	public boolean canAttack(Playable attacker, Playable victim)
	{
		return _state == EventState.ATTACKING;
	}

	@Override
	public boolean showRespawnButton()
	{
		return _state == EventState.ATTACKING;
	}

	@Override
	public void onRespawnButton(Player player)
	{
		if (_resTask != null)
			_resTask.addPlayer(player);
	}

	@Override
	public void onRespawn(Player player)
	{
		if (_radar != null)
			player.getRadarList().addMarker(_radar);
	}

	@Override
	public EventType getType()
	{
		return EventType.MAP;
	}
}
