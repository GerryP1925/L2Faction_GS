package net.sf.l2j.gameserver.faction.map;

import java.util.ArrayList;

import net.sf.l2j.gameserver.faction.EventManager;
import net.sf.l2j.gameserver.faction.EventPlayerData;
import net.sf.l2j.gameserver.faction.EventState;
import net.sf.l2j.gameserver.faction.EventType;
import net.sf.l2j.gameserver.faction.RessurectionTask;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

/**
 * @author An4rchy
 *
 */
public class PvPMapEvent extends MapEvent
{
	private int _bossId;
	private Location _bossLoc;
	private int _bossMins;
	
	public PvPMapEvent(int id, String name, int duration, ArrayList<Location> teamOneSpawns, ArrayList<Location> teamTwoSpawns, Location radar, int bossId, Location bossLoc, int bossMins)
	{
		super(id, name, duration, teamOneSpawns, teamTwoSpawns, radar);
		
		_bossId = bossId;
		_bossLoc = bossLoc;
		_bossMins = bossMins;
		_resTask = new RessurectionTask(getRandomSpawn(1), getRandomSpawn(2));
	}
	
	@Override
	public void run()
	{
		_state = EventState.ATTACKING;
		registerTask(() -> sendInformation(), 1000, 1000);
		registerTask(_resTask, 10 * 1000, 10 * 1000);
		registerTask(() -> endRewards(), (getDuration() * 1000 * 60) - (10 * 1000));
		registerTask(() -> endEvent(), getDuration() * 1000 * 60);
		registerTask(() -> World.announceToOnlinePlayers("5 minutes left for "+getName()+" Event to end!"), (getDuration() - 5) * 1000 * 60);
		registerTask(() -> 
		{
			Npc boss = spawnNpc(_bossId, _bossLoc, 0);
			if (boss != null)
				World.announceToOnlinePlayers("Boss "+boss.getName()+" has been spawned!");
		}, _bossMins * 1000 * 60);
		announceEvent();
	}
	
	@Override
	protected void sendInformation()
	{
		ExShowScreenMessage msg = new ExShowScreenMessage("Anakim: "+_teamOneScore+" Lilith: "+_teamTwoScore+" Time Left: "+EventManager.getInstance().getEventRemainingMins().replace(" min ", ":").replace(" sec", ""), 1000, SMPOS.BOTTOM_RIGHT, false);
		for (EventPlayerData player : _participants)
			player.getActingPlayer().sendPacket(msg);
	}
	
	@Override
	public void onKill(Playable killer, Player victim)
	{
		EventPlayerData killerP = getParticipant(killer.getActingPlayer());
		EventPlayerData victimP = getParticipant(victim);
		
		if (killerP == null || victimP == null)
			return;
		
		killerP.setScore(killerP.getScore() + 1);
		if (killer.getActingPlayer().getFaction() == 1)
			_teamOneScore++;
		else
			_teamTwoScore++;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.MAP;
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
	public boolean showToTownButton()
	{
		return true;
	}
	
	@Override
	public void onRespawnButton(Player player)
	{
		_resTask.addPlayer(player);
	}
	
	@Override
	protected void endRewards()
	{
		_state = EventState.REWARDING;
		World.announceToOnlinePlayers(getName()+" Event is over!");
	}
}
