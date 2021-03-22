package net.sf.l2j.gameserver.faction.mini;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.faction.EventPlayerData;
import net.sf.l2j.gameserver.faction.EventState;
import net.sf.l2j.gameserver.faction.EventType;
import net.sf.l2j.gameserver.faction.FactionEvent;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.location.Location;

import java.util.ArrayList;

public abstract class MiniEvent extends FactionEvent
{
    protected Location _eventLoc;

    public MiniEvent(int id, String name, int duration, Location eventLoc, ArrayList<EventPlayerData> participants)
    {
        super(id, name, duration);

        _eventLoc = eventLoc;
        _participants = participants; // mini events begin with map event winners/all participants
    }

    @Override
    public void run()
    {
        _state = EventState.REGISTERING;
        System.out.println("Registering begin for "+getName());
        try
        {
            Thread.sleep(30 * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        super.run();

        _state = EventState.RUNNING;

        // teleport players
        _participants.forEach(p -> p.getActingPlayer().teleportTo(_eventLoc, 20));

        // event end task
        if (getDuration() > 0)
            registerTask("End", () -> endEvent(), getDuration() * 1000 * 60);
    }

    @Override
    protected void endEvent()
    {
        // teleport players to town
        for (EventPlayerData player : _participants)
        	player.getActingPlayer().teleportTo(MapRegionData.getInstance().getLocationToTeleport(player.getActingPlayer(), MapRegionData.TeleportType.TOWN), 20);

        super.endEvent();
    }

    @Override
    public EventType getType()
    {
        return EventType.MINI;
    }

    @Override
    public boolean allowRecall()
    {
        return false;
    }

    @Override
    public boolean canAttack(Playable attacker, Playable victim)
    {
        return false;
    }

    @Override
    public boolean showRespawnButton()
    {
        return false;
    }

    @Override
    public boolean showToTownButton()
    {
        return false;
    }
}
