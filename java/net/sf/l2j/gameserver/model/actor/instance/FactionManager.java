package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * @author An4rchy
 *
 */
public class FactionManager extends Folk
{
	public FactionManager(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("joinFaction"))
		{
			int faction = Integer.parseInt(command.substring(12));
			
			player.setFaction(faction);
			showChatWindow(player, "data/html/custom/factionmanager/"+getNpcId()+"-joined.htm"); // show join htm
		}
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		if ((player.getFaction() == 1 && getNpcId() == Config.FACTION_ONE_NPC) || (player.getFaction() == 2 && getNpcId() == Config.FACTION_TWO_NPC))
			showChatWindow(player, "data/html/custom/factionmanager/"+getNpcId()+"-friend.htm");
		else if ((player.getFaction() == 1 && getNpcId() == Config.FACTION_TWO_NPC) || (player.getFaction() == 2 && getNpcId() == Config.FACTION_ONE_NPC))
			showChatWindow(player, "data/html/custom/factionmanager/"+getNpcId()+"-enemy.htm");
		else
			showChatWindow(player, "data/html/custom/factionmanager/"+getNpcId()+".htm");
	}
}
