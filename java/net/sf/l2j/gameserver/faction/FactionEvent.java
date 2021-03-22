package net.sf.l2j.gameserver.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author An4rchy
 *
 */
public abstract class FactionEvent implements Runnable
{
	protected final CLogger LOGGER = new CLogger(FactionEvent.class.getName());
	
	private int _id;
	private String _name;
	private int _duration;
	protected EventState _state;
	protected int _teamOneScore, _teamTwoScore;
	protected ArrayList<EventPlayerData> _participants;
	private HashMap<String, ScheduledFuture<?>> _eventTasks;
	protected ArrayList<Npc> _spawns;
	
	public FactionEvent(int id, String name, int duration)
	{
		_id = id;
		_name = name;
		_duration = duration;
		_state = EventState.INACTIVE;
		_participants = new ArrayList<>();
		_eventTasks = new HashMap<>();
		_spawns = new ArrayList<>();
	}

	@Override
	public void run()
	{
		if (getEventPlayerInfo() != null)
			registerTask("Info", () -> sendInformation(), 1000, 1000);
		announceEvent();
	}

	protected void sendInformation()
	{
		ExShowScreenMessage msg = new ExShowScreenMessage(getEventPlayerInfo(), 1000, ExShowScreenMessage.SMPOS.BOTTOM_RIGHT, false);
		for (EventPlayerData player : _participants)
			player.getActingPlayer().sendPacket(msg);
	}

	protected String getEventPlayerInfo()
	{
		return null;
	}

	protected void announceEvent()
	{
		World.announceToOnlinePlayers("Next Event - "+getName());
	}
	
	protected void endEvent()
	{
		_state = EventState.INACTIVE;

		World.announceToOnlinePlayers(getName()+" Event is over!");

		// TODO mini
		_participants.clear();
		EventManager.getInstance().clearFactionChanges();
		for (Npc npc : _spawns)
		{
			Spawn npcSpawn = npc.getSpawn();
			if (npcSpawn != null)
			{
				npcSpawn.setRespawnState(false);
				SpawnTable.getInstance().deleteSpawn(npcSpawn, false);
			}
			npc.deleteMe();
		}
		_spawns.clear();
		_eventTasks.values().forEach(t -> t.cancel(false));
		_eventTasks.clear();
		_teamOneScore = 0;
		_teamTwoScore = 0;
		
		EventManager.getInstance().beginEvent(null); // TODO mini
	}
	
	protected Npc spawnNpc(int npcId, Location loc, int respawnDelay)
	{
		NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		
		if (template == null)
		{
			LOGGER.warn("{} tried to spawn non-existing npc with id: {}", getClass().getName(), npcId);
			return null;
		}
		
		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setLoc(loc.getX(), loc.getY(), loc.getZ(), 0);
			if (respawnDelay > 0)
			{
				spawn.setRespawnDelay(respawnDelay);
				spawn.setRespawnState(true);
			}
			else
			{
				spawn.setRespawnDelay(1);
				spawn.setRespawnState(false);
			}
			
			SpawnTable.getInstance().addSpawn(spawn, false);
			Npc npc = spawn.doSpawn(false);
			_spawns.add(npc);
			return npc;
		}
		catch (Exception e)
		{
			LOGGER.error("Error spawning npc with id {}", e, npcId);
		}
		
		return null;
	}

	protected void cancelTask(String name)
	{
		ScheduledFuture<?> task = _eventTasks.get(name);
		if (task != null)
			task.cancel(false);
	}

	protected void registerTask(String name, Runnable r, long delay)
	{
		registerTask(name, r, delay, 0);
	}
	
	protected void registerTask(String name, Runnable r, long initial, long repeat)
	{
		if (repeat > 0)
			_eventTasks.put(name, ThreadPool.scheduleAtFixedRate(r, initial, repeat));
		else
			_eventTasks.put(name, ThreadPool.schedule(r, initial));
	}
	
	public synchronized void addParticipant(Player player)
	{
		_participants.add(new EventPlayerData(player));
	}
	
	public synchronized void removeParticipant(Player player)
	{
		EventPlayerData participant = getParticipant(player);
		if (participant != null)
		{
			player.sendMessage("You have been removed from the event.");
			_participants.remove(participant);
		}
	}
	
	public EventPlayerData getParticipant(Player player)
	{
		for (EventPlayerData participant : _participants)
			if (participant.getActingPlayer().getObjectId() == player.getObjectId())
				return participant;
		
		return null;
	}

	public boolean isEventNpc(Npc npc)
	{
		return _spawns.contains(npc);
	}

	public int getId()
	{
		return _id;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getDuration()
	{
		return _duration;
	}
	
	public EventState getState()
	{
		return _state;
	}

	public void onRespawnButton(Player player)
	{ }

	public boolean allowRecall()
	{
		return true;
	}

	public void onRecall(Player player)
	{
		removeParticipant(player);
	}

	public void onRespawn(Player player)
	{ }

	public void onDeath(Player victim, Playable killer)
	{ }

	public void onInterract(Player player, Npc npc)
	{ }

	public void onCast(Player player, Creature target, L2Skill skill)
	{ }

	public void onCastStop(Player player, L2Skill skill)
	{ }

	public boolean canAttack(Playable attacker, Playable victim)
	{
		return true;
	}

	public boolean showRespawnButton()
	{
		return true;
	}

	public boolean showToTownButton()
	{
		return true;
	}

	public abstract EventType getType();
}
