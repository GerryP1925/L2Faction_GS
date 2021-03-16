package net.sf.l2j.gameserver.faction;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;

/**
 * @author An4rchy
 *
 */
public abstract class FactionEvent implements Runnable
{
	private static final CLogger LOGGER = new CLogger(FactionEvent.class.getName());
	
	private int _id;
	private String _name;
	private int _duration;
	protected EventState _state;
	protected int _teamOneScore, _teamTwoScore;
	protected ArrayList<EventPlayerData> _participants;
	protected ArrayList<ScheduledFuture<?>> _eventTasks;
	protected ArrayList<Npc> _spawns;
	
	public FactionEvent(int id, String name, int duration)
	{
		_id = id;
		_name = name;
		_duration = duration;
		_state = EventState.INACTIVE;
		_participants = new ArrayList<>();
		_eventTasks = new ArrayList<>();
		_spawns = new ArrayList<>();
	}
	
	protected void announceEvent()
	{
		World.announceToOnlinePlayers("Next Event - "+getName());
		World.announceToOnlinePlayers("Event Duration - "+getDuration()+" minutes");
	}
	
	protected void endEvent()
	{
		_state = EventState.INACTIVE;
		
		// TODO mini
		for (EventPlayerData player : _participants)
			player.getActingPlayer().teleportTo(MapRegionData.getInstance().getLocationToTeleport(player.getActingPlayer(), TeleportType.TOWN), 20);
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
		for (ScheduledFuture<?> task : _eventTasks)
			task.cancel(true);
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
	
	protected void sendInformation()
	{
		
	}
	
	protected void registerTask(Runnable r, long delay)
	{
		registerTask(r, delay, 0);
	}
	
	protected void registerTask(Runnable r, long initial, long repeat)
	{
		if (repeat > 0)
			_eventTasks.add(ThreadPool.scheduleAtFixedRate(r, initial, repeat));
		else
			_eventTasks.add(ThreadPool.schedule(r, initial));
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
	
	public abstract EventType getType();
	public abstract boolean canAttack(Playable attacker, Playable victim);
	public abstract boolean showRespawnButton();
	public abstract boolean showToTownButton();
	public abstract void onRespawnButton(Player player);
	protected abstract void endRewards();
}
