package net.sf.l2j.gameserver.handler;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.handler.targethandlers.*;

public class TargetHandler
{
	private final Map<SkillTargetType, ITargetHandler> _entries = new HashMap<>();
	
	protected TargetHandler()
	{
		registerHandler(new TargetAlly());
		registerHandler(new TargetArea());
		registerHandler(new TargetAreaCorpseMob());
		registerHandler(new TargetAreaSummon());
		registerHandler(new TargetAura());
		registerHandler(new TargetAuraUndead());
		registerHandler(new TargetBehindAura());
		registerHandler(new TargetClan());
		registerHandler(new TargetCorpseAlly());
		registerHandler(new TargetCorpseMob());
		registerHandler(new TargetCorpsePet());
		registerHandler(new TargetCorpsePlayer());
		registerHandler(new TargetEnemySummon());
		registerHandler(new TargetFrontArea());
		registerHandler(new TargetFrontAura());
		registerHandler(new TargetGround());
		registerHandler(new TargetHoly());
		registerHandler(new TargetOne());
		registerHandler(new TargetOwnerPet());
		registerHandler(new TargetParty());
		registerHandler(new TargetPartyMember());
		registerHandler(new TargetPartyOther());
		registerHandler(new TargetSelf());
		registerHandler(new TargetSummon());
		registerHandler(new TargetUndead());
		registerHandler(new TargetUnlockable());
		registerHandler(new TargetFactionFlag());
	}
	
	private void registerHandler(ITargetHandler handler)
	{
		_entries.put(handler.getTargetType(), handler);
	}
	
	public ITargetHandler getHandler(SkillTargetType targetType)
	{
		return _entries.get(targetType);
	}
	
	public int size()
	{
		return _entries.size();
	}
	
	public static TargetHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TargetHandler INSTANCE = new TargetHandler();
	}
}