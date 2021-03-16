package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.faction.EventListeners;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

public class Die extends L2GameServerPacket
{
	private final Creature _creature;
	private final int _objectId;
	private final boolean _fake;
	
	private boolean _sweepable;
	private boolean _allowFixedRes;
	//private Clan _clan;
	
	public Die(Creature creature)
	{
		_creature = creature;
		_objectId = creature.getObjectId();
		_fake = !creature.isDead();
		
		if (creature instanceof Player)
		{
			Player player = (Player) creature;
			_allowFixedRes = player.getAccessLevel().allowFixedRes();
			//_clan = player.getClan();
			
		}
		else if (creature instanceof Monster)
			_sweepable = ((Monster) creature).getSpoilState().isSweepable();
	}
	
	@Override
	protected final void writeImpl()
	{
		if (_fake)
			return;
		
		writeC(0x06);
		writeD(_objectId);
		
		// respawn button
		if (!(_creature instanceof Player) || !EventListeners.showRespawnButton((Player)_creature))
			writeD(0x00);
		else
			writeD(0x01);
		
		writeD(0x00); // clanhall
		writeD(0x00); // castle
		
		// to town button
		if (!(_creature instanceof Player) || !EventListeners.showToTownButton((Player)_creature))
			writeD(0x00);
		else
			writeD(0x01);
		
		writeD((_sweepable) ? 0x01 : 0x00); // sweepable (blue glow)
		writeD((_allowFixedRes) ? 0x01 : 0x00); // FIXED
	}
}