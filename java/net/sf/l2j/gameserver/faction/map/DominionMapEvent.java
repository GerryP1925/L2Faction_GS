package net.sf.l2j.gameserver.faction.map;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.gameserver.faction.*;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.FactionEventDummy;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ExServerPrimitive;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class DominionMapEvent extends MapEvent
{
    private ArrayList<StatSet> _flagTemplates;
    private ConcurrentHashMap<Npc, FlagLine> _flags;

    public DominionMapEvent(int id, String name, int duration, ArrayList<Location> teamOneSpawns, ArrayList<Location> teamTwoSpawns, Location radar, ArrayList<StatSet> flagTemplates)
    {
        super(id, name, duration, teamOneSpawns, teamTwoSpawns, radar);

        _flagTemplates = flagTemplates;
        _flags = new ConcurrentHashMap<>();
        _resTask = new RessurectionTask(getRandomSpawn(1), getRandomSpawn(2));
    }

    @Override
    public void run()
    {
        super.run();

        // spawn flags and start check task
        int i = 1;
        for (StatSet flagTemplate : _flagTemplates)
        {
            FactionEventDummy flag = (FactionEventDummy) spawnNpc(flagTemplate.getInteger("id"), new Location(flagTemplate.getInteger("x"), flagTemplate.getInteger("y"), flagTemplate.getInteger("z")), flagTemplate.getInteger("r"));
            flag.setCheckRadius(flagTemplate.getInteger("r"));
            flag.setTitle("Control Point");

            ExServerPrimitive linePacket = new ExServerPrimitive("DOMINION"+(i++), new Location(100, 100, -16000));
            for (int j = 0; j < 2; j++)
            {
                Location p1 = null;
                int step = 12;
                for (int theta = 0; theta <= 360; theta += step)
                {
                    double cos = Math.cos(Math.toRadians(theta));
                    double sin = Math.sin(Math.toRadians(theta));
                    double pX = flag.getX() + (flag.getCheckRadius() * cos);
                    double pY = flag.getY() + (flag.getCheckRadius() * sin);
                    Location p2 = new Location((int) pX, (int) pY, flag.getZ() - (20 + (j * 2)));
                    if (p1 != null)
                        linePacket.addLine(Color.WHITE, p1, p2);

                    p1 = p2;
                }
            }

            _flags.put(flag, new FlagLine("DOMINION"+(i++), linePacket, Color.WHITE));
        }
        registerTask("Flag Check", () ->
        {
            for (Npc flag : _flags.keySet())
            {
                FactionEventDummy dFlag = (FactionEventDummy) flag;
                int team1Points = 0, team2Points = 0;
                for (Player player : dFlag.getKnownTypeInRadius(Player.class, dFlag.getCheckRadius()))
                {
                    if (player.isDead()) // dead near flag
                        continue;
                    if (EventManager.getInstance().getActiveEvent().getParticipant(player) == null) // non participant near flag
                        continue;

                    if (EventManager.getInstance().getActiveEvent().getParticipant(player).getActingPlayer().getFaction() == 1)
                        team1Points++;
                    else
                        team2Points++;
                }

                _teamOneScore += team1Points;
                _teamTwoScore += team2Points;

                Color color = Color.WHITE;
                if (team1Points > team2Points)
                    color = Color.GREEN;
                else if (team1Points < team2Points)
                    color = Color.RED;

                if (_flags.get(flag).getColor() != color) // color is changed update for existing players and check if anyone doesn't have packet
                {
                    _flags.get(flag).setColor(color);
                    for (Player player : dFlag.getKnownType(Player.class))
                    {
                        ExServerPrimitive linePacket = player.getLinePacket(_flags.get(flag).getName());
                        if (linePacket != null) // player has the line packet sent before
                        {
                            linePacket.setEnterWorldLoc(player.getEnterWorldLoc());
                            linePacket.sendTo(player); // resend for color update
                        }
                        else // first time sending the line packet
                        {
                            linePacket = _flags.get(flag).getPacket();
                            linePacket.setEnterWorldLoc(player.getEnterWorldLoc());
                            player.addLinePacket(_flags.get(flag).getName(), linePacket);
                            linePacket.sendTo(player);
                        }
                    }
                }
                else // color is same, check if anyone doesn't have packet
                {
                    for (Player player : dFlag.getKnownType(Player.class))
                    {
                        ExServerPrimitive linePacket = player.getLinePacket(_flags.get(flag).getName());
                        if (linePacket == null) // player doesn't have the line packets
                        {
                            linePacket = _flags.get(flag).getPacket();
                            linePacket.setEnterWorldLoc(player.getEnterWorldLoc());
                            player.addLinePacket(_flags.get(flag).getName(), linePacket);
                            linePacket.sendTo(player);
                        }
                    }
                }
            }
        }, 3000, 3000);
    }

    @Override
    public void onInterract(Player player, Npc npc)
    {
        player.sendMessage("Use Capture Fort skill to capture it!");
    }

    @Override
    public synchronized void addParticipant(Player player)
    {
        super.addParticipant(player);

        /*for (FlagLine line : _flags.values())
        {
            player.addLinePacket(line.getName(), line.getPacket());
            line.getPacket().sendTo(player);
        }*/
    }

    @Override
    protected void endEvent()
    {
        for (Npc flag : _flags.keySet())
        {
            _flags.get(flag).getPacket().reset();
            for (Player player : flag.getKnownType(Player.class))
            {
                player.getActingPlayer().resetLinePackets();
                _flags.get(flag).getPacket().sendTo(player.getActingPlayer());
            }
        }
        _flags.clear();

        super.endEvent();
    }

    @Override
    protected String getEventPlayerInfo()
    {
        return "Anakim: "+_teamOneScore+" Lilith: "+_teamTwoScore+" Time Left: "+EventManager.getInstance().getEventRemainingMins().replace(" min ", ":").replace(" sec", "");
    }

    @Override
    public EventType getType()
    {
        return EventType.MAP;
    }

    @Override
    protected void endBattle()
    {
        super.endBattle();
        cancelTask("Flag Check");
    }

    private class FlagLine
    {
        String _name;
        private ExServerPrimitive _packet;
        private Color _color;

        public FlagLine(String name, ExServerPrimitive packet, Color color)
        {
            _name = name;
            _packet = packet;
            _color = color;
        }

        public String getName()
        {
            return _name;
        }

        public ExServerPrimitive getPacket()
        {
            return _packet;
        }

        public Color getColor()
        {
            return _color;
        }

        public void setColor(Color color)
        {
            _color = color;
            _packet.updateColorForAllLines(color);
        }
    }
}
