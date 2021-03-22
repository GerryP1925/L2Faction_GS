package net.sf.l2j.gameserver.faction.mini;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.faction.*;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.FactionEventDummy;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.location.Point2D;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class LuckyChestsMini extends MiniEvent
{
    private StatSet _chestInfo;
    private CopyOnWriteArrayList<FactionEventDummy> _chests;

    public LuckyChestsMini(int id, String name, int duration, ArrayList<EventPlayerData> participants, StatSet chestInfo, Location eventLoc)
    {
        super(id, name, duration, eventLoc, participants);

        _chestInfo = chestInfo;
        _chests = new CopyOnWriteArrayList<>();
    }

    @Override
    public void run()
    {
        super.run();

        // spawn chests
        for (int i = 0; i < _chestInfo.getInteger("amount"); i++)
        {
            Point2D p1 = new Point2D(_chestInfo.getInteger("p1x"), _chestInfo.getInteger("p1y"));
            Point2D p2 = new Point2D(_chestInfo.getInteger("p2x"), _chestInfo.getInteger("p2y"));
            Point2D p3 = new Point2D(_chestInfo.getInteger("p3x"), _chestInfo.getInteger("p3y"));
            Point2D p4 = new Point2D(_chestInfo.getInteger("p4x"), _chestInfo.getInteger("p4y"));
            Point2D randomPoint = getRandomPoint(p1, p2, p3, p4);
            _chests.add((FactionEventDummy) spawnNpc(_chestInfo.getInteger("id"), new Location(randomPoint.getX(), randomPoint.getY(), _chestInfo.getInteger("z")), 0));
        }
    }

    @Override
    public void onInterract(Player player, Npc npc)
    {
        if (!(npc instanceof FactionEventDummy))
            return;
        FactionEventDummy chest = (FactionEventDummy) npc;
        EventPlayerData p = getParticipant(player); // can't be null, just checked by EventListeners

        if (_state != EventState.RUNNING)
            return;

        _chests.remove(chest);
        chest.deleteMe();
        p.setScore(p.getScore() + 1);

        if (Rnd.get(100) < 30) // 30% chest will kill player and need respawn
            player.doDie(null);

        if (_chests.isEmpty()) // chests opened, end event
        {
            World.announceToOnlinePlayers("All chests have been opened! The event is over, you will be teleported back in 5 seconds.");
            for (EventPlayerData pa : _participants)
                if (pa.getActingPlayer().isDead())
                    pa.getActingPlayer().doRevive();
            cancelTask("Info");
            cancelTask("End");
            registerTask("End", () -> endEvent(), 5000);
        }
    }

    @Override
    protected String getEventPlayerInfo()
    {
        return "Chests Left: "+_chests.size()+" Time Left: "+EventManager.getInstance().getEventRemainingMins().replace(" min ", ":").replace(" sec", "");
    }

    private static Point2D getRandomPoint(Point2D p1, Point2D p2, Point2D p3, Point2D p4)
    {
        Point2D triangleP1 = p1, triangleP2 = p2, triangleP3 = p4;
        if (Rnd.nextBoolean()) // rnd triangle of rectangle
        {
            triangleP1 = p2;
            triangleP2 = p3;
            triangleP3 = p4;
        }

        Point2D a = new Point2D(triangleP2.getX() - triangleP1.getX(), triangleP2.getY() - triangleP1.getY());
        Point2D b = new Point2D(triangleP3.getX() - triangleP1.getX(), triangleP3.getY() - triangleP1.getY());
        double u1 = Rnd.nextDouble();
        double u2 = Rnd.nextDouble();
        if (u1 + u2 > 1)
        {
            u1 = 1 - u1;
            u2 = 1 - u2;
        }

        Point2D w = new Point2D((int) ((a.getX() * u1) + (b.getX() * u2)), (int) ((a.getY() * u1) + (b.getY() * u2)));

        return new Point2D(w.getX() + triangleP1.getX(), w.getY() + triangleP1.getY());
    }

    @Override
    protected void endEvent()
    {
        super.endEvent();

        _chests.clear();
    }

    @Override
    public boolean showRespawnButton()
    {
        return true;
    }

    @Override
    public void onRespawnButton(Player player)
    {
        player.doRevive();
        player.teleportTo(_eventLoc, 20);
    }
}
