package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.faction.EventManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author An4rchy
 *
 */
public class AdminEvents implements IAdminCommandHandler
{
	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.equals("admin_stopevents"))
			EventManager.getInstance().stopEvents(player);
		else if (command.equals("admin_startevents"))
			EventManager.getInstance().startEvents(player);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return new String[] { "admin_stopevents", "admin_startevents" };
	}
}
