package net.sf.l2j.gameserver.network.serverpackets;

public class ShowMiniMap extends L2GameServerPacket
{
	public static final ShowMiniMap REGULAR_MAP = new ShowMiniMap(1665);
	
	private final int _mapId;
	
	public ShowMiniMap(int mapId)
	{
		_mapId = mapId;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0x9d);
		writeD(_mapId);
		writeD(0x00);//writeD(SevenSignsManager.getInstance().getCurrentPeriod().ordinal());
	}
}