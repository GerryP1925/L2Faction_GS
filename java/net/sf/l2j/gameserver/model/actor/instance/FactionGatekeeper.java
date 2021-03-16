package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.faction.EventManager;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author An4rchy
 *
 */
public class FactionGatekeeper extends Folk
{
	public FactionGatekeeper(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player.getFaction() == 0)
		{
			player.sendMessage("You must join a faction before participating in an event.");
			return;
		}
		
		if (command.startsWith("joinEvent"))
			EventManager.getInstance().getActiveEvent().addParticipant(player);
		else if (command.startsWith("teleport"))
		{
			if (player.getFaction() == 0)
			{
				player.sendMessage("You must first choose a faction.");
				return;
			}
			if (EventManager.getInstance().getActiveEvent().getParticipant(player) != null)
			{
				player.sendMessage("You cannot teleport while participating in an event.");
				return;
			}
			
			String[] tpLocs = command.substring(9).split(" ");
			Location tpLoc = new Location(Integer.parseInt(tpLocs[0]), Integer.parseInt(tpLocs[1]), Integer.parseInt(tpLocs[2]));
			
			player.teleportTo(tpLoc, 30);
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile("data/html/custom/factiongatekeeper/"+getNpcId()+".htm");
		html.replace("%objectId%", getObjectId());
		html.replace("%currentEvent%", EventManager.getInstance().getActiveEvent().getName());
		html.replace("%eventRemaining%", EventManager.getInstance().getEventRemainingMins());
		
		player.sendPacket(html);
	}
}
