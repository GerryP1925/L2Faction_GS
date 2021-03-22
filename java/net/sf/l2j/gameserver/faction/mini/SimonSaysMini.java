package net.sf.l2j.gameserver.faction.mini;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.faction.EventPlayerData;
import net.sf.l2j.gameserver.faction.EventState;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.FactionEventDummy;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimonSaysMini extends MiniEvent
{
    // each round random characters from the sequence will be chosen
    private final String _eventCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*().,/=+-_";

    private StatSet _simonTemplate;
    private FactionEventDummy _simon;
    private int _round;
    private String _roundWord;
    private CopyOnWriteArrayList<Player> _roundWinners;

    public SimonSaysMini(int id, String name, ArrayList<EventPlayerData> participants, StatSet simonTemplate, Location eventLoc)
    {
        super(id, name, 0, eventLoc, participants);

        _simonTemplate = simonTemplate;
        _round = 0;
        _roundWinners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void run()
    {
        super.run();

        _state = EventState.PAUSING;

        // spawn simon
        _simon = (FactionEventDummy) spawnNpc(_simonTemplate.getInteger("id"), new Location(_simonTemplate.getInteger("x"), _simonTemplate.getInteger("y"), _simonTemplate.getInteger("z")), 0);

        // greet players
        registerTask("Greet", () -> simonSay("Welcome everyone! My name is Simon and we are going to play a game!"), 6000);

        // inform rules to players
        registerTask("Inform", () -> simonSay("You have to repeat EXACTLY what I say as fast as you can to stay alive! Get ready!"), 11000);

        // begin round 1
        registerTask("Round", () -> round(), 18000);
    }

    private void simonSay(String text)
    {
        CreatureSay cs = new CreatureSay(_simon, SayType.ALL, text);
        for (Player player : _simon.getKnownType(Player.class))
            player.sendPacket(cs);
    }

    private void round()
    {
        // cleanup from previous round
        _roundWinners.clear();
        _round++;
        _roundWord = "";

        // words increase by 2 letters each round
        for (int i = 0; i < 2 + (_round * 2); i++)
        {
            char newChar = _eventCharacters.charAt(Rnd.get(_eventCharacters.length()));
            if (_roundWord.isEmpty()) // if it's the first character of the word, it can't be symbol to avoid chat confusion
                while (!StringUtil.isAlphaNumeric(""+newChar))
                    newChar = _eventCharacters.charAt(Rnd.get(_eventCharacters.length()));

            _roundWord += newChar;
        }

        simonSay(_roundWord);
        _state = EventState.RUNNING;

        registerTask("RoundEnd", () -> roundEnd(), 9000);
    }

    private void roundEnd()
    {
        _state = EventState.PAUSING;

        if (_roundWinners.isEmpty()) // no winners this round, game over
        {
            for (EventPlayerData p : _participants)
                p.getActingPlayer().doDie(null);
            simonSay("No winners this round, the event is over!");
            registerTask("End", () -> endEvent(), 7000);
            return;
        }

        if (_round >= 7) // it was the final round, game over
        {
            simonSay("The event is over! Congratulations to the winners!");
            registerTask("End", () -> endEvent(), 7000);
            return;
        }

        // kill losers and remove them from participants
        ArrayList<Player> losers = new ArrayList<>();
        for (EventPlayerData loser : _participants)
        {
            if (_roundWinners.contains(loser.getActingPlayer()))
                continue;

            loser.getActingPlayer().doDie(null);
            losers.add(loser.getActingPlayer());
        }
        for (Player loser : losers)
            removeParticipant(loser);

        // schedule next round
        registerTask("Round"+_round, () -> round(), 7000);
    }

    public void checkPlayerInput(Player player, String input)
    {
        if (_state != EventState.RUNNING) // too late, round over
            return;

        if (!input.equals(_roundWord)) // wrong word
        {
            removeParticipant(player);
            player.doDie(null);
            return;
        }

        player.sendMessage("Correct!"); // replace with reward
        _roundWinners.add(player);
    }

    @Override
    protected void endEvent()
    {
        super.endEvent();

        _simon = null;
        _roundWinners.clear();
        _round = 0;
        _roundWord = "";
    }
}
