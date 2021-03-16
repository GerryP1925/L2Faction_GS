package net.sf.l2j.gameserver.faction;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author An4rchy
 *
 */
public class EventListeners
{
	public static void onRecall(Player player)
	{
		if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null)
			return;
		
		EventManager.getInstance().getActiveEvent().removeParticipant(player);
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
