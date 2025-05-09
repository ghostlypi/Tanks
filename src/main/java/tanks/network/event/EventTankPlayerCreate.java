package tanks.network.event;

import io.netty.buffer.ByteBuf;
import tanks.Crusade;
import tanks.Game;
import tanks.Player;
import tanks.Team;
import tanks.gui.screen.ScreenGame;
import tanks.gui.screen.ScreenPartyHost;
import tanks.gui.screen.ScreenPartyLobby;
import tanks.minigames.Arcade;
import tanks.minigames.RampageTrial;
import tanks.network.ConnectedPlayer;
import tanks.network.NetworkUtils;
import tanks.tank.*;

import java.util.UUID;

public class EventTankPlayerCreate extends PersonalEvent
{
	public Player player;

	public UUID clientIdTarget;
	public String username;
	public double posX;
	public double posY;
	public double angle;
	public String team;

	public int colorR;
	public int colorG;
	public int colorB;

	public int colorR2;
	public int colorG2;
	public int colorB2;

	public int colorR3;
	public int colorG3;
	public int colorB3;

	public int networkID;

	public double drawAge;

	public EventTankPlayerCreate()
	{
	
	}
	
	public EventTankPlayerCreate(Player p, double x, double y, double angle, Team t, int networkID, double drawAge)
	{
		this.player = p;
		this.clientIdTarget = p.clientID;
		this.posX = x;
		this.posY = y;
		this.angle = angle;
		this.networkID = networkID;
		this.drawAge = drawAge;

		if (t == null)
			this.team = "*";
		else if (t == Game.playerTeam)
			this.team = "**";
		else if (t == Game.playerTeamNoFF)
			this.team = "***";
		else
			this.team = t.name;

		this.username = p.username;

		this.colorR = p.colorR;
		this.colorG = p.colorG;
		this.colorB = p.colorB;

		this.colorR2 = p.colorR2;
		this.colorG2 = p.colorG2;
		this.colorB2 = p.colorB2;

		this.colorR3 = p.colorR3;
		this.colorG3 = p.colorG3;
		this.colorB3 = p.colorB3;
	}
	
	@Override
	public void execute()
	{
		Tank t;

		if (this.clientID != null)
			return;

		if (ScreenPartyHost.isServer)
			ScreenPartyHost.includedPlayers.add(this.clientIdTarget);
		else if (ScreenPartyLobby.isClient)
			ScreenPartyLobby.includedPlayers.add(this.clientIdTarget);

		if (player != null && player.isBot)
		{
			t = new TankPlayerBot(posX, posY, angle, player);
			Game.currentLevel.playerBuilds.get(0).clonePropertiesTo((TankPlayerBot) t);
		}
		else if (clientIdTarget.equals(Game.clientID))
		{
			if (!ScreenPartyLobby.isClient)
				t = new TankPlayer(posX, posY, angle);
			else
				t = new TankPlayerController(posX, posY, angle, clientIdTarget);

			Game.playerTank = (TankPlayer) t;
			Game.player.tank = t;

			if (!Crusade.crusadeMode)
				Game.player.buildName = Game.currentLevel.playerBuilds.get(0).name;
		}
		else
		{
			if (ScreenPartyLobby.isClient)
			{
				TankPlayer t2 = new TankPlayer(posX, posY, angle);
				t2.player = new Player(clientIdTarget, "");
				setColor(t2);
				t2.saveColors();
				Game.currentLevel.playerBuilds.get(0).clonePropertiesTo(t2);
				t = new TankRemote(t2);
			}
			else
			{
				t = new TankPlayerRemote(posX, posY, angle, this.player);
				((TankPlayerRemote) t).refreshAmmo();
			}

			t.showName = true;
			t.nameTag.name = this.username;

			if (Game.enableChatFilter)
				t.nameTag.name = Game.chatFilter.filterChat(t.nameTag.name);
		}

		setColor(t);

		if (t instanceof TankPlayable)
			Game.currentLevel.playerBuilds.get(0).clonePropertiesTo((TankPlayable) t);

		if (team.equals("**"))
			t.team = Game.playerTeam;
		else if (team.equals("***"))
			t.team = Game.playerTeamNoFF;
		else if (team.equals("*"))
			t.team = null;
		else
			t.team = Game.currentLevel.teamsMap.get(team);

		if (player != null)
			player.tank = t;

		if (ScreenPartyLobby.isClient)
		{
			for (ConnectedPlayer c: ScreenPartyLobby.connections)
			{
				if (c.clientId.equals(clientIdTarget))
				{
					if (t.team != null && t.team.enableColor)
					{
						c.teamColorR = t.team.teamColorR;
						c.teamColorG = t.team.teamColorG;
						c.teamColorB = t.team.teamColorB;
					}
					else
					{
						c.teamColorR = 255;
						c.teamColorG = 255;
						c.teamColorB = 255;
					}
				}
			}
		}

		t.drawAge = this.drawAge;

		t.setNetworkID(this.networkID);

		if ((Game.currentLevel instanceof Arcade || Game.currentLevel instanceof RampageTrial) && Game.screen instanceof ScreenGame && ((ScreenGame) Game.screen).playing)
			t.invulnerabilityTimer = 250;

		Game.movables.add(t);
	}

	public void setColor(Tank t)
	{
		t.colorR = this.colorR;
		t.colorG = this.colorG;
		t.colorB = this.colorB;

		t.secondaryColorR = this.colorR2;
		t.secondaryColorG = this.colorG2;
		t.secondaryColorB = this.colorB2;

		t.enableTertiaryColor = true;
		t.tertiaryColorR = this.colorR3;
		t.tertiaryColorG = this.colorG3;
		t.tertiaryColorB = this.colorB3;

		t.emblemR = this.colorR2;
		t.emblemG = this.colorG2;
		t.emblemB = this.colorB2;

		if (t instanceof TankPlayable)
			((TankPlayable) t).saveColors();
	}

	@Override
	public void write(ByteBuf b) 
	{
		NetworkUtils.writeString(b, this.clientIdTarget.toString());
		NetworkUtils.writeString(b, this.username);
		b.writeDouble(this.posX);
		b.writeDouble(this.posY);
		b.writeDouble(this.angle);
		NetworkUtils.writeString(b, this.team);
		b.writeInt(this.networkID);

		b.writeInt(this.colorR);
		b.writeInt(this.colorG);
		b.writeInt(this.colorB);
		b.writeInt(this.colorR2);
		b.writeInt(this.colorG2);
		b.writeInt(this.colorB2);
		b.writeInt(this.colorR3);
		b.writeInt(this.colorG3);
		b.writeInt(this.colorB3);

		b.writeDouble(this.drawAge);
	}

	@Override
	public void read(ByteBuf b) 
	{
		this.clientIdTarget = UUID.fromString(NetworkUtils.readString(b));
		this.username = NetworkUtils.readString(b);
		this.posX = b.readDouble();
		this.posY = b.readDouble();
		this.angle = b.readDouble();
		this.team = NetworkUtils.readString(b);
		this.networkID = b.readInt();

		this.colorR = b.readInt();
		this.colorG = b.readInt();
		this.colorB = b.readInt();
		this.colorR2 = b.readInt();
		this.colorG2 = b.readInt();
		this.colorB2 = b.readInt();
		this.colorR3 = b.readInt();
		this.colorG3 = b.readInt();
		this.colorB3 = b.readInt();

		this.drawAge = b.readDouble();
	}
}
