package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.faction.EventManager;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

import java.awt.*;

/**
 * @author An4rchy
 *
 */
public class AdminEvents implements IAdminCommandHandler
{
	private ExServerPrimitive d = null;

	@Override
	public void useAdminCommand(String command, Player player)
	{
		if (command.equals("admin_stopevents"))
			EventManager.getInstance().stopEvents(player);
		else if (command.equals("admin_startevents"))
			EventManager.getInstance().startEvents(player);
		else if (command.equals("admin_dede"))
		{
			WorldObject o = player.getTarget();

			if (o == null || !(o instanceof Door))
				return;

			player.sendMessage(""+((Door)o).getDoorId());
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return new String[] { "admin_stopevents", "admin_startevents", "admin_dede" };
	}
}
