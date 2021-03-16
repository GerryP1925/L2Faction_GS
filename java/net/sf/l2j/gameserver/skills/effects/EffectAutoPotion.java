package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.enums.skills.EffectType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.AbstractEffect;

/**
 * @author An4rchy
 *
 */
public class EffectAutoPotion extends AbstractEffect
{
	private long _lastPotion = 0;
	
	public EffectAutoPotion(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.AUTO_POTION;
	}
	
	@Override
	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;
		
		if (System.currentTimeMillis() - _lastPotion < getTemplate().getValue() * 1000)
			return true;
		
		double currentStat = 0, maxStat = 0;
		L2Skill potionSkill = null;
		switch (getTemplate().getStackType())
		{
			case "cp":
			{
				currentStat = getEffected().getActingPlayer().getStatus().getCp();
				maxStat = getEffected().getActingPlayer().getStatus().getMaxCp();
				potionSkill = SkillTable.getInstance().getInfo(2166, 2);
				break;
			}
			case "hp":
			{
				currentStat = getEffected().getActingPlayer().getStatus().getHp();
				maxStat = getEffected().getActingPlayer().getStatus().getMaxHp();
				potionSkill = SkillTable.getInstance().getInfo(2037, 1);
				break;
			}
			case "mp":
			{
				currentStat = getEffected().getActingPlayer().getStatus().getMp();
				maxStat = getEffected().getActingPlayer().getStatus().getMaxMp();
				potionSkill = SkillTable.getInstance().getInfo(2279, 3);
				break;
			}
		}
		
		if (potionSkill == null) // never happens, but better safe than sorry
			return false;
		
		if ((currentStat * 100) / maxStat < getTemplate().getStackOrder())
		{
			getEffected().getActingPlayer().getCast().doAutoPotionCast(potionSkill);
			_lastPotion = System.currentTimeMillis();
		}
		
		return true;
	}
}
