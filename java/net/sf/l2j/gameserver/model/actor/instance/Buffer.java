package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.skills.AbstractEffect;
import net.sf.l2j.gameserver.skills.L2Skill;

/**
 * @author An4rchy
 *
 */
public class Buffer extends Folk
{
	private static final CLogger LOGGER = new CLogger(Buffer.class.getName());
	
	public Buffer(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("buff"))
		{
			String[] buffData = command.substring(5).split(" ");
			int buffId = Integer.parseInt(buffData[0]);
			L2Skill buff = SkillTable.getInstance().getInfo(buffId, buffData.length > 1 ? Integer.parseInt(buffData[1]) : SkillTable.getInstance().getMaxLevel(buffId));
			
			if (buff == null || !Config.BUFFER_BUFF_IDS.contains(buffId))
			{
				LOGGER.warn("Player {} tried to buff skill id {}", player.getName(), buffId);
				player.sendMessage("An unexpected error occured, please contact a GM.");
				return;
			}
			
			if (player.getBufferTarget() == 2 && player.getSummon() == null)
			{
				player.sendMessage("You don't have a pet summoned.");
				return;
			}
			
			buff.getEffects(this, player.getBufferTarget() == 1 ? player : player.getSummon());
		}
		else if (command.equals("heal"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("You may not heal in combat mode.");
				return;
			}
			if (player.getBufferTarget() == 2 && player.getSummon() == null)
			{
				player.sendMessage("You don't have a pet summoned.");
				return;
			}
			
			if (player.getBufferTarget() == 1)
				player.getStatus().setCpHpMp(player.getStatus().getMaxCp(), player.getStatus().getMaxHp(), player.getStatus().getMaxMp());
			else
				player.getSummon().getStatus().setHpMp(player.getSummon().getStatus().getMaxHp(), player.getSummon().getStatus().getMaxMp());
		}
		else if (command.equals("cancel"))
		{
			if (player.getBufferTarget() == 2 && player.getSummon() == null)
			{
				player.sendMessage("You don't have a pet summoned.");
				return;
			}
			
			for (AbstractEffect effect : player.getBufferTarget() == 1 ? player.getAllEffects() : player.getSummon().getAllEffects())
				if (Config.BUFFER_BUFF_IDS.contains(effect.getSkill().getId()))
					effect.exit();
			showChatWindow(player, 0);
		}
		else if (command.equals("target"))
		{
			player.setBufferTarget(player.getBufferTarget() == 1 ? 2 : 1);
			player.sendMessage("Buffer target changed to: "+(player.getBufferTarget() == 1 ? "Self" : "Pet"));
			showChatWindow(player, 0);
		}
		else if (command.equals("schemes"))
		{
			StringBuilder sb = new StringBuilder();
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement ps = con.prepareStatement("SELECT * FROM character_schemes WHERE charObjectId="+player.getObjectId());
				ResultSet rs = ps.executeQuery())
			{
				int i = 1;
				while (rs.next())
				{
					sb.append("<tr>");
					sb.append("<td width=\"18\">"+(i++)+"</td>");
					sb.append("<td width=\"90\">"+rs.getString("schemeName")+"</td>");
					sb.append("<td width=\"50\">"+(rs.getInt("schemeTarget") == 1 ? "Self" : "Pet")+"</td>");
					sb.append("<td width=\"30\"><a action=\"bypass npc_"+getObjectId()+"_use "+rs.getInt("schemeTarget")+" "+rs.getString("schemeBuffs")+"\">Use</a></td>");
					sb.append("<td width=\"34\"><a action=\"bypass -h npc_"+getObjectId()+"_delete "+rs.getString("schemeName")+"\">Delete</a></td>");
					sb.append("</tr>");
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't load schemes for player {}", e, player.getName());
				player.sendMessage("An unexpected error occured, please contact a GM.");
				return;
			}
			
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/custom/buffer/"+getNpcId()+"-schemes.htm");
			html.replace("%objectId%", getObjectId());
			html.replace("%target%", player.getBufferTarget() == 1 ? "Self" : "Pet");
			html.replace("%currentSchemes%", getCurrentSchemes(player));
			html.replace("%schemes%", sb.toString());
			player.sendPacket(html);
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (command.startsWith("use"))
		{
			String[] schemeData = command.substring(4).split(" ");
			int schemeTarget = Integer.parseInt(schemeData[0]);
			
			if (schemeTarget == 2 && player.getSummon() == null)
			{
				player.sendMessage("You don't have a pet summoned.");
				return;
			}
			
			for (String skill : schemeData[1].split(","))
			{
				int skillId = Integer.parseInt(skill);
				if (!Config.BUFFER_BUFF_IDS.contains(skillId))
					continue;
				
				SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(this, schemeTarget == 1 ? player : player.getSummon());
			}
		}
		else if (command.startsWith("delete"))
		{
			String schemeName = command.substring(7);
			
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement stm = con.prepareStatement("DELETE FROM character_schemes WHERE charObjectId=? AND schemeName=?"))
			{
				stm.setInt(1, player.getObjectId());
				stm.setString(2, schemeName);
				
				stm.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't delete scheme {} for player {}", e, schemeName, player.getName());
				player.sendMessage("An unexpected error occured, please contact a GM.");
				return;
			}
			
			player.sendMessage("Scheme "+schemeName+" has been deleted.");
			onBypassFeedback(player, "schemes"); // show schemes window again
		}
		else if (command.startsWith("save"))
		{
			String schemeName = command.substring(4).trim();
			
			if (schemeName.length() < 3 || schemeName.length() > 17 || !StringUtil.isAlphaNumeric(schemeName))
			{
				player.sendMessage("Please enter a name of letters and numbers between 3 and 16 characters.");
				onBypassFeedback(player, "schemes"); // show schemes window again
				return;
			}
			
			if (getCurrentSchemes(player) >= 5)
			{
				player.sendMessage("You cannot have more than 5 schemes.");
				onBypassFeedback(player, "schemes"); // show schemes window again
				return;
			}
			if (player.getBufferTarget() == 2 && player.getSummon() == null)
			{
				player.sendMessage("You don't have a pet summoned.");
				onBypassFeedback(player, "schemes"); // show schemes window again
				return;
			}
			
			ArrayList<Integer> playerBuffs = new ArrayList<>();
			for (AbstractEffect effect : player.getBufferTarget() == 1 ? player.getAllEffects() : player.getSummon().getAllEffects())
				if (Config.BUFFER_BUFF_IDS.contains(effect.getSkill().getId()))
					playerBuffs.add(effect.getSkill().getId());
			
			if (playerBuffs.isEmpty())
			{
				player.sendMessage("There are no buffs to be saved.");
				onBypassFeedback(player, "schemes"); // show schemes window again
				return;
			}
			
			StringBuilder sb = new StringBuilder();
			for (int buffId : playerBuffs)
				sb.append(buffId+",");
			
			try (Connection con = ConnectionPool.getConnection();
				PreparedStatement stm = con.prepareStatement("INSERT INTO character_schemes VALUES (?,?,?,?)"))
			{
				stm.setInt(1, player.getObjectId());
				stm.setString(2, schemeName);
				stm.setInt(3, player.getBufferTarget());
				stm.setString(4, sb.toString());
				
				stm.execute();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't save scheme for player {}", e, player.getName());
				player.sendMessage("An unexpected error occured, please contact a GM.");
				return;
			}
			
			player.sendMessage("Your scheme has been saved.");
			onBypassFeedback(player, "schemes"); // show schemes window again
		}
		else
			super.onBypassFeedback(player, command);
	}
	
	private static int getCurrentSchemes(Player player)
	{
		int i = 0;
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM character_schemes WHERE charObjectId="+player.getObjectId());
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
				i++;
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't load schemes for player {}", e, player.getName());
			player.sendMessage("An unexpected error occured, please contact a GM.");
			return 0;
		}
		
		return i;
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		String fileName = getHtmlPath(getNpcId(), val);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(fileName);
		html.replace("%objectId%", getObjectId());
		html.replace("%target%", player.getBufferTarget() == 1 ? "Self" : "Pet");
		html.replace("%maxBuffs%", player.getMaxBuffCount());
		html.replace("%currentBuffs%", player.getCurrentBuffCount());
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename;
		
		if (val == 0)
			filename = "data/html/custom/buffer/" + npcId + ".htm";
		else
			filename = "data/html/custom/buffer/"+npcId+"-"+val+".htm";
		
		if (HtmCache.getInstance().isLoadable(filename))
			return filename;
		
		return "<html>HTML not found, please contact a GM.</html>";
	}
}
