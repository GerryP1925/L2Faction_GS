package net.sf.l2j.gameserver.faction;

import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author An4rchy
 *
 */
public class EventPlayerData
{
	private Player _actingPlayer;
	private int _score;
	
	public EventPlayerData(Player actingPlayer)
	{
		_actingPlayer = actingPlayer;
		_score = 0;
	}
	
	public Player getActingPlayer()
	{
		return _actingPlayer;
	}
	
	public int getScore()
	{
		return _score;
	}
	
	public void setScore(int score)
	{
		_score = score;
	}
}
