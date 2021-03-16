package net.sf.l2j.gameserver.network.serverpackets;

public class SSQStatus extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xf5);
		writeC(0x00);
		writeC(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeC(0x00);
		writeC(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeC(0x00);
		writeD(0x00);
		writeD(0x00);
		writeD(0x00);
		writeC(0x00);
	}
}