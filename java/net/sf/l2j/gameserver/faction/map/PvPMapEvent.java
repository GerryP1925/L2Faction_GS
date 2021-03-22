package net.sf.l2j.gameserver.faction.map;

import java.util.ArrayList;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.faction.EventManager;
import net.sf.l2j.gameserver.faction.EventPlayerData;
import net.sf.l2j.gameserver.faction.RessurectionTask;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

/**
 * @author An4rchy
 *
 */
public class PvPMapEvent extends MapEvent
{
	private StatSet _bossTemplate;
	
	public PvPMapEvent(int id, String name, int duration, ArrayList<Location> teamOneSpawns, ArrayList<Location> teamTwoSpawns, Location radar, StatSet bossTemplate)
	{
		super(id, name, duration, teamOneSpawns, teamTwoSpawns, radar);

		_bossTemplate = bossTemplate;
		_resTask = new RessurectionTask(getRandomSpawn(1), getRandomSpawn(2));
	}
	
	@Override
	public void run()
	{
		super.run();

		registerTask("BossWarn", () ->
		{
			Npc boss = spawnNpc(_bossTemplate.getInteger("id"), new Location(_bossTemplate.getInteger("x"), _bossTemplate.getInteger("y"), _bossTemplate.getInteger("z")), 0);
			if (boss != null)
				World.announceToOnlinePlayers("Boss "+boss.getName()+" has been spawned!");
		}, _bossTemplate.getInteger("time") * 1000 * 60);
	}

	@Override
	protected String getEventPlayerInfo()
	{
		return "Anakim: "+_teamOneScore+" Lilith: "+_teamTwoScore+" Time Left: "+EventManager.getInstance().getEventRemainingMins().replace(" min ", ":").replace(" sec", "");
	}
	
	@Override
	public void onDeath(Player victim, Playable killer)
	{
		if (killer == null) // we must have killer to add score to
			return;

		EventPlayerData killerP = getParticipant(killer.getActingPlayer());
		killerP.setScore(killerP.getScore() + 1);
		if (killer.getActingPlayer().getFaction() == 1)
			_teamOneScore++;
		else
			_teamTwoScore++;
	}
}
