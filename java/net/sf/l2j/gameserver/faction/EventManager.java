package net.sf.l2j.gameserver.faction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.faction.map.PvPMapEvent;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * @author An4rchy
 *
 */
public final class EventManager implements IXmlReader
{
	private HashMap<Integer, FactionEvent> _events;
	@SuppressWarnings("unused")
	private int _lastEventId;
	private long _eventStartTime;
	private FactionEvent _currentEvent;
	private HashMap<Integer, Integer> _factionChanges;
	private boolean _engineOn;
	
	public EventManager()
	{
		_events = new HashMap<>();
		_lastEventId = 0;
		_factionChanges = new HashMap<>();
		_engineOn = true;
		
		load();
		beginEvent(null);
	}
	
	public void beginEvent(ArrayList<EventPlayerData> participants)
	{
		if (!_engineOn) // admin has stopped the events
			return;
		
		EventType type = EventType.MAP;
		/*
		if (_lastEventId != 0)
		{
			switch (_events.get(_lastEventId).getType())
			{
				case MAP:
				{
					if (participants != null && !participants.isEmpty())
						type = EventType.MINI;
					else
						type = EventType.FULL;
					break;
				}
				case MINI:
				{
					type = EventType.FULL;
					break;
				}
				case FULL:
				{
					type = EventType.MAP;
					break;
				}
			}
		}*/
		
		for (FactionEvent event : _events.values())
		{
			if (event.getType() == type)
			{
				_currentEvent = event;
				_lastEventId = event.getId();
				_eventStartTime = System.currentTimeMillis();
				ThreadPool.execute(_currentEvent);
			}
		}
	}
	
	public void stopEvents(Player admin)
	{
		_engineOn = false;
		admin.sendMessage("The event engine has been stopped. Next event will begin with //startevents.");
	}
	
	public void startEvents(Player admin)
	{
		if (_engineOn)
		{
			admin.sendMessage("The event engine isn't stopped.");
			return;
		}
		
		_engineOn = true;
		beginEvent(null);
		admin.sendMessage("The event engine has started.");
	}
	
	public String getEventRemainingMins()
	{
		long remainingMillis = _eventStartTime + (_currentEvent.getDuration() * 1000 * 60) - System.currentTimeMillis();
		String minutes = ""+TimeUnit.MILLISECONDS.toMinutes(remainingMillis);
		int secondsNum = (int) (TimeUnit.MILLISECONDS.toSeconds(remainingMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingMillis)));
		String seconds = secondsNum < 10 ? "0"+secondsNum : ""+secondsNum;
		return String.format("%s min %s sec", minutes, seconds);
	}
	
	public FactionEvent getActiveEvent()
	{
		return _currentEvent;
	}
	
	public void checkForFactionChange(Player player)
	{
		if (_factionChanges.containsKey(player.getObjectId()))
		{
			player.setFaction(_factionChanges.get(player.getObjectId()));
			_factionChanges.remove(player.getObjectId());
		}
	}
	
	public void clearFactionChanges()
	{
		for (Entry<Integer, Integer> entry : _factionChanges.entrySet())
		{
			Player player = World.getInstance().getPlayer(entry.getKey());
			if (player == null || !player.isOnline() || player.getClient().isDetached()) // only online players in order to store correctly
				continue;
			
			player.setFaction(entry.getValue());
			_factionChanges.remove(entry.getKey());
		}
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/factionmaps.xml");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "event", eventNode ->
		{
			NamedNodeMap attrs = eventNode.getAttributes();
			int id = parseInteger(attrs, "id");
			String name = parseString(attrs, "name");
			String type = parseString(attrs, "type");
			String subType = parseString(attrs, "subtype");
			if (type.equals("MAP"))
			{
				int duration = parseInteger(attrs, "duration");
				ArrayList<Location> team1Spawns = new ArrayList<>(),
					team2Spawns = new ArrayList<>(),
					flags = new ArrayList<>();
				Location radar = new Location(0, 0, 0);
				StatSet boss = new StatSet();
				forEach(eventNode, "teamSpawns", spawnNode ->
				{
					forEach(spawnNode, "spawn", spawn ->
					{
						NamedNodeMap spawnAttrs = spawn.getAttributes();
						Location spawnLoc = new Location(parseInteger(spawnAttrs, "x"), parseInteger(spawnAttrs, "y"), parseInteger(spawnAttrs, "z"));
						if (parseInteger(spawnAttrs, "team") == 1)
							team1Spawns.add(spawnLoc);
						else
							team2Spawns.add(spawnLoc);
					});
				});
				forEach(eventNode, "flag", flagNode ->
				{
					NamedNodeMap flagAttrs = flagNode.getAttributes();
					flags.add(new Location(parseInteger(flagAttrs, "x"), parseInteger(flagAttrs, "y"), parseInteger(flagAttrs, "z")));
				});
				forEach(eventNode, "radar", radarNode ->
				{
					NamedNodeMap radarAttrs = radarNode.getAttributes();
					radar.set(parseInteger(radarAttrs, "x"), parseInteger(radarAttrs, "y"), parseInteger(radarAttrs, "z"));
				});
				forEach(eventNode, "boss", bossNode ->
				{
					NamedNodeMap bossAttrs = bossNode.getAttributes();
					boss.put("bossId", parseInteger(bossAttrs, "id"));
					boss.put("bossX", parseInteger(bossAttrs, "x"));
					boss.put("bossY", parseInteger(bossAttrs, "y"));
					boss.put("bossZ", parseInteger(bossAttrs, "z"));
					boss.put("bossMins", parseInteger(bossAttrs, "time"));
				});
				if (subType.equals("PVP"))
				{
					PvPMapEvent event = new PvPMapEvent(id, name, duration, team1Spawns, team2Spawns, radar.getX() == 0 ? null : radar, (int)boss.get("bossId"), new Location((int)boss.get("bossX"), (int)boss.get("bossY"), (int)boss.get("bossZ")), (int)boss.get("bossMins"));
					_events.put(id, event);
				}
				else if (subType.equals("DOMINION"))
				{
					
				}
				else if (subType.equals("SIEGEF"))
				{
					
				}
			}
			else if (type.equals("MINI"))
			{
				if (subType.equals("SIMONSAYS"))
				{
					
				}
				else if (subType.equals("LUCKYCHESTS"))
				{
					
				}
			}
			else if (type.equals("FULL"))
			{
				//int duration = this.parseInteger(attrs, "duration");
				//int register = this.parseInteger(attrs, "register");
				if (subType.equals("TVT"))
				{
					
				}
				else if (subType.equals("DM"))
				{
					
				}
			}
		}));
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		private static final EventManager _instance = new EventManager();
	}
}
