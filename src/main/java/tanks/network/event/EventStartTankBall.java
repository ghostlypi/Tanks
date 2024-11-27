package tanks.network.event;

import io.netty.buffer.ByteBuf;
import tanks.Game;
import tanks.Level;
import tanks.minigames.TankBall;
import tanks.network.NetworkUtils;

import java.nio.charset.StandardCharsets;

public class EventStartTankBall extends PersonalEvent {
    public String levelString;
    public long time = System.currentTimeMillis();

    public EventStartTankBall() {

    }

    public EventStartTankBall (TankBall b) {
        this.levelString = b.levelString;
    }

    public void write(ByteBuf b) {
        NetworkUtils.writeString(b, levelString);
    }

    public void read(ByteBuf b) {
        this.levelString = NetworkUtils.readString(b);
    }

    public void execute() {
        if (this.clientID == null) {
            Level l = new Level(this.levelString);
            l.loadLevel(true);
        }
    }
}