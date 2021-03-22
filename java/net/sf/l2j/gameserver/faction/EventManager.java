package net.sf.l2j.gameserver.faction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.faction.map.DominionMapEvent;
import net.sf.l2j.gameserver.faction.map.FortressSiegeEvent;
import net.sf.l2j.gameserver.faction.map.PvPMapEvent;
import net.sf.l2j.gameserver.faction.mini.LuckyChestsMini;
import net.sf.l2j.gameserver.faction.mini.SimonSaysMini;
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
		
		EventType type = EventType.MINI;
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
			if (event.getType() == type && event.getId() == 5)
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
		if (_currentEvent.getDuration() == 0)
			return "---";

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
					team2Spawns = new ArrayList<>();
				Location radar = new Location(0, 0, 0);
				StatSet boss = new StatSet();
				ArrayList<StatSet> flags = new ArrayList<>();
				ArrayList<Integer> doorIds = new ArrayList<>();
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
					StatSet flag = new StatSet();
					flag.set("id", parseInteger(flagAttrs, "id"));
					flag.set("x", parseInteger(flagAttrs, "x"));
					flag.set("y", parseInteger(flagAttrs, "y"));
					flag.set("z", parseInteger(flagAttrs, "z"));
					flag.set("r", parseInteger(flagAttrs, "r"));
					flags.add(flag);
				});
				forEach(eventNode, "radar", radarNode ->
				{
					NamedNodeMap radarAttrs = radarNode.getAttributes();
					radar.set(parseInteger(radarAttrs, "x"), parseInteger(radarAttrs, "y"), parseInteger(radarAttrs, "z"));
				});
				forEach(eventNode, "boss", bossNode ->
				{
					NamedNodeMap bossAttrs = bossNode.getAttributes();
					boss.put("id", parseInteger(bossAttrs, "id"));
					boss.put("x", parseInteger(bossAttrs, "x"));
					boss.put("y", parseInteger(bossAttrs, "y"));
					boss.put("z", parseInteger(bossAttrs, "z"));
					boss.put("time", parseInteger(bossAttrs, "time"));
				});
				forEach(eventNode, "door", doorNode ->
				{
					NamedNodeMap doorAttrs = doorNode.getAttributes();
					doorIds.add(parseInteger(doorAttrs, "id"));
				});
				if (subType.equals("PVP"))
				{
					PvPMapEvent event = new PvPMapEvent(id, name, duration, team1Spawns, team2Spawns, radar.getX() == 0 ? null : radar, boss);
					_events.put(id, event);
				}
				else if (subType.equals("DOMINION"))
				{
					DominionMapEvent event = new DominionMapEvent(id, name, duration, team1Spawns, team2Spawns, radar.getX() == 0 ? null : radar, flags);
					_events.put(id, event);
				}
				else if (subType.equals("SIEGEF"))
				{
					FortressSiegeEvent event = new FortressSiegeEvent(id, name, duration, team1Spawns, team2Spawns, radar.getX() == 0 ? null : radar, doorIds, flags.get(0));
					_events.put(id, event);
				}
			}
			else if (type.equals("MINI"))
			{
				Location spawnLoc = new Location(0, 0, 0);
				forEach(eventNode, "eventSpawn", spawnNode ->
				{
					NamedNodeMap spawnAttrs = spawnNode.getAttributes();
					spawnLoc.setX(parseInteger(spawnAttrs, "x"));
					spawnLoc.setY(parseInteger(spawnAttrs, "y"));
					spawnLoc.setZ(parseInteger(spawnAttrs, "z"));
				});
				if (subType.equals("SIMONSAYS"))
				{
					StatSet simonTemplate = new StatSet();
					forEach(eventNode, "simon", simonNode ->
					{
						NamedNodeMap simonAttrs = simonNode.getAttributes();
						simonTemplate.set("id", parseInteger(simonAttrs, "id"));
						simonTemplate.set("x", parseInteger(simonAttrs, "x"));
						simonTemplate.set("y", parseInteger(simonAttrs, "y"));
						simonTemplate.set("z", parseInteger(simonAttrs, "z"));
					});
					SimonSaysMini event = new SimonSaysMini(id, name, new ArrayList<>(), simonTemplate, spawnLoc);
					_events.put(id, event);
				}
				else if (subType.equals("LUCKYCHESTS"))
				{
					int duration = parseInteger(attrs, "duration");
					StatSet chestInfo = new StatSet();
					forEach(eventNode, "chest", chestNode ->
					{
						NamedNodeMap chestAttrs = chestNode.getAttributes();
						chestInfo.set("id", parseInteger(chestAttrs, "id"));
						chestInfo.set("p1x", parseInteger(chestAttrs, "p1x"));
						chestInfo.set("p1y", parseInteger(chestAttrs, "p1y"));
						chestInfo.set("p2x", parseInteger(chestAttrs, "p2x"));
						chestInfo.set("p2y", parseInteger(chestAttrs, "p2y"));
						chestInfo.set("p3x", parseInteger(chestAttrs, "p3x"));
						chestInfo.set("p3y", parseInteger(chestAttrs, "p3y"));
						chestInfo.set("p4x", parseInteger(chestAttrs, "p4x"));
						chestInfo.set("p4y", parseInteger(chestAttrs, "p4y"));
						chestInfo.set("z", parseInteger(chestAttrs, "z"));
						chestInfo.set("amount", parseInteger(chestAttrs, "amount"));
					});
					LuckyChestsMini event = new LuckyChestsMini(id, name, duration, new ArrayList<>(), chestInfo, spawnLoc);
					_events.put(id, event);
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
