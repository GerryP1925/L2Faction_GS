package net.sf.l2j.gameserver.model.actor.attack;

import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.faction.EventListeners;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class groups all attack data related to a {@link Creature}.
 * @param <T> : The {@link Playable} used as actor.
 */
public class PlayableAttack<T extends Playable> extends CreatureAttack<T>
{
	public PlayableAttack(T actor)
	{
		super(actor);
	}
	
	@Override
	public boolean canDoAttack(Creature target)
	{
		if (!super.canDoAttack(target))
			return false;
		
		if (target instanceof Playable && (!_actor.getActingPlayer().isInDuel() || _actor.getActingPlayer().getDuelId() != target.getActingPlayer().getDuelId()))
		{
			if (_actor.isInsideZone(ZoneId.PEACE))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_ATK_PEACEZONE));
				return false;
			}
			
			if (target.isInsideZone(ZoneId.PEACE))
			{
				_actor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				return false;
			}
			
			if (_actor.getActingPlayer().getFaction() == target.getActingPlayer().getFaction())
			{
				_actor.sendMessage("You may not attack a player of the same faction.");
				return false;
			}
			
			if (!EventListeners.canAttack(_actor, (Playable)target))
				return false;
		}
		
		return true;
	}
}