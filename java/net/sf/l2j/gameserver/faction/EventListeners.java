package net.sf.l2j.gameserver.faction;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.faction.map.FortressSiegeEvent;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author An4rchy
 *
 */
public class EventListeners
{
	public static void onCastStop(Player player, L2Skill skill)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return;

		EventManager.getInstance().getActiveEvent().onCastStop(player, skill);
	}

	public static void onCast(Player player, Creature target, L2Skill skill)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return;

		EventManager.getInstance().getActiveEvent().onCast(player, target, skill);
	}

	public static void onInterract(Player player, Npc npc)
	{
		if (!EventManager.getInstance().getActiveEvent().isEventNpc(npc) || EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return;

		EventManager.getInstance().getActiveEvent().onInterract(player, npc);
	}

	public static void onDeath(Player victim, Creature killer)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(victim) == null) // victim isn't even participant, nothing custom
			return;

		if (killer != null && killer instanceof Playable && EventManager.getInstance().getActiveEvent().getParticipant(killer.getActingPlayer()) != null && victim.getFaction() != killer.getActingPlayer().getFaction()) // killer is a player/pet that is in event and players are different factions
			EventManager.getInstance().getActiveEvent().onDeath(victim, (Playable)killer);
		else // killer is null or not participant or mob
			EventManager.getInstance().getActiveEvent().onDeath(victim, null);
	}

	public static boolean canAttackDoor(Playable attacker, int doorId)
	{
		if (EventManager.getInstance().getActiveEvent() instanceof FortressSiegeEvent)
		{
			FortressSiegeEvent event = (FortressSiegeEvent) EventManager.getInstance().getActiveEvent();
			if (event.isEventDoor(doorId) && event.getState() == EventState.ATTACKING && event.getParticipant(attacker.getActingPlayer()) != null)
				return true;
		}

		return false;
	}

	public static boolean canRecall(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) != null)
			return EventManager.getInstance().getActiveEvent().allowRecall();

		return true;
	}

	public static void onRecall(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return;

		EventManager.getInstance().getActiveEvent().onRecall(player);
	}
	
	public static void onRespawnButton(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null || !EventManager.getInstance().getActiveEvent().showRespawnButton())
		{
			player.doRevive();
			player.teleportTo(MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN), 20);
			return;
		}
		
		EventManager.getInstance().getActiveEvent().onRespawnButton(player);
	}
	
	public static boolean showToTownButton(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return true;
		
		return EventManager.getInstance().getActiveEvent().showToTownButton();
	}
	
	public static boolean showRespawnButton(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return false;
		
		return EventManager.getInstance().getActiveEvent().showRespawnButton();
	}
	
	public static boolean canAttack(Playable attacker, Playable victim)
	{
		if ((EventManager.getInstance().getActiveEvent().getParticipant(attacker.getActingPlayer()) == null && EventManager.getInstance().getActiveEvent().getParticipant(victim.getActingPlayer()) != null) // 1 in event and 1 not in event -> can't attack
			|| (EventManager.getInstance().getActiveEvent().getParticipant(attacker.getActingPlayer()) != null && EventManager.getInstance().getActiveEvent().getParticipant(victim.getActingPlayer()) == null))
			return false;
		else if (EventManager.getInstance().getActiveEvent().getParticipant(attacker.getActingPlayer()) == null && EventManager.getInstance().getActiveEvent().getParticipant(victim.getActingPlayer()) == null) // 2 not in event -> can attack
			return true;
		
		return EventManager.getInstance().getActiveEvent().canAttack(attacker, victim); // 2 in event, calculate based on event
	}
}
