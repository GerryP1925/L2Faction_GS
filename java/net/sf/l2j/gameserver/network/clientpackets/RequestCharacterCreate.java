package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.QuestStatus;
import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.Shortcut;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.holder.ItemTemplateHolder;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateFail;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateOk;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectInfo;
import net.sf.l2j.gameserver.scripting.Quest;

public final class RequestCharacterCreate extends L2GameClientPacket
{
	private String _name;
	private int _race;
	private byte _sex;
	private int _classId;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		readD(); // int
		readD(); // str
		readD(); // con
		readD(); // men
		readD(); // dex
		readD(); // wit
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		// Invalid race.
		if (_race > 4 || _race < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid face.
		if (_face > 2 || _face < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid hair style.
		if (_hairStyle < 0 || (_sex == 0 && _hairStyle > 4) || (_sex != 0 && _hairStyle > 6))
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid hair color.
		if (_hairColor > 3 || _hairColor < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid name typo.
		if (!StringUtil.isValidString(_name, "^[A-Za-z0-9]{1,16}$"))
		{
			sendPacket(CharCreateFail.REASON_INCORRECT_NAME);
			return;
		}
		
		// Your name is already taken by a NPC.
		if (NpcData.getInstance().getTemplateByName(_name) != null)
		{
			sendPacket(CharCreateFail.REASON_INCORRECT_NAME);
			return;
		}
		
		// You already have the maximum amount of characters for this account.
		if (PlayerInfoTable.getInstance().getCharactersInAcc(getClient().getAccountName()) >= 7)
		{
			sendPacket(CharCreateFail.REASON_TOO_MANY_CHARACTERS);
			return;
		}
		
		// The name already exists.
		if (PlayerInfoTable.getInstance().getPlayerObjectId(_name) > 0)
		{
			sendPacket(CharCreateFail.REASON_NAME_ALREADY_EXISTS);
			return;
		}
		
		// The class id related to this template is post-newbie.
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(_classId);
		if (template == null || template.getClassBaseLevel() > 1)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Create the player Object.
		final Player player = Player.create(IdFactory.getInstance().getNextId(), template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, Sex.VALUES[_sex]);
		if (player == null)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Set default values.
		player.getStatus().setMaxHpMp();
		
		// send acknowledgement
		sendPacket(CharCreateOk.STATIC_PACKET);
		
		World.getInstance().addObject(player);
		
		player.getPosition().set(template.getRandomSpawn());
		player.setTitle("");
		
		player.getStatus().setLevel(76);
		player.getStatus().setCpHpMp(player.getStatus().getMaxCp(), player.getStatus().getMaxHp(), player.getStatus().getMaxMp());
		
		// Register shortcuts.
		player.getShortcutList().addShortcut(new Shortcut(0, 0, ShortcutType.ACTION, 2, -1, 1)); // attack shortcut
		
		// Equip or add items, based on template.
		for (ItemTemplateHolder holder : template.getItems())
		{
			final ItemInstance item = player.getInventory().addItem("Init", holder.getId(), holder.getValue(), player, null);
			
			if (item.isEquipable() && holder.isEquipped())
				player.getInventory().equipItemAndRecord(item);
		}
		
		// Add skills.
		for (GeneralSkillNode skill : player.getAvailableAutoGetSkills())
		{
			if (skill.getId() == 9900)
				player.getShortcutList().addShortcut(new Shortcut(9, 1, ShortcutType.SKILL, skill.getId(), 1, 1), false); // auto cp
			if (skill.getId() == 9901)
				player.getShortcutList().addShortcut(new Shortcut(10, 1, ShortcutType.SKILL, skill.getId(), 1, 1), false); // auto hp
			if (skill.getId() == 9902)
				player.getShortcutList().addShortcut(new Shortcut(11, 1, ShortcutType.SKILL, skill.getId(), 1, 1), false); // auto mp
		}
		
		// Tutorial runs here.
		final Quest quest = ScriptData.getInstance().getQuest("Tutorial");
		if (quest != null)
			quest.newQuestState(player).setState(QuestStatus.STARTED);
		
		player.setOnlineStatus(true, false);
		player.deleteMe();
		
		final CharSelectInfo csi = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1);
		sendPacket(csi);
		getClient().setCharSelectSlot(csi.getCharacterSlots());
	}
}