package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

public class FactionEventDummy extends Folk
{
    private int _checkRadius = 0;

    public FactionEventDummy(int objectId, NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public void showChatWindow(Player player)
    {
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    public int getCheckRadius()
    {
        return _checkRadius;
    }

    public void setCheckRadius(int val)
    {
        _checkRadius = val;
    }
}
