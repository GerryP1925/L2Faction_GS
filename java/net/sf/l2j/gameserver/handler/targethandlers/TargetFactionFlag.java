package net.sf.l2j.gameserver.handler.targethandlers;

import net.sf.l2j.gameserver.enums.skills.SkillTargetType;
import net.sf.l2j.gameserver.handler.ITargetHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.instance.FactionEventDummy;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.skills.L2Skill;

public class TargetFactionFlag implements ITargetHandler
{
    @Override
    public SkillTargetType getTargetType()
    {
        return SkillTargetType.FACTION_FLAG;
    }

    @Override
    public Creature[] getTargetList(Creature caster, Creature target, L2Skill skill)
    {
        return new Creature[]
        {
                target
        };
    }

    @Override
    public Creature getFinalTarget(Creature caster, Creature target, L2Skill skill)
    {
        return target;
    }

    @Override
    public boolean meetCastConditions(Playable caster, Creature target, L2Skill skill, boolean isCtrlPressed)
    {
        if (!(target instanceof FactionEventDummy))
        {
            caster.sendPacket(SystemMessageId.INVALID_TARGET);
            return false;
        }
        return true;
    }
}
