package tanks.network.event;

import io.netty.buffer.ByteBuf;
import tanks.minigames.TankBall;
import tanks.tank.Tank;

public class EventTankBallUpdate extends PersonalEvent implements IStackableEvent {

    public int ball;
    public double posX;
    public double posY;
    public double vX;
    public double vY;
    public long time = System.currentTimeMillis();

    public EventTankBallUpdate() {

    }

    public EventTankBallUpdate(TankBall.Ball b)
    {
        this.ball = b.id;
        this.posX = b.posX;
        this.posY = b.posY;
        this.vX = b.vX;
        this.vY = b.vY;
    }

    @Override
    public int getIdentifier() {return this.ball;}

    @Override
    public void write(ByteBuf b) {
        b.writeInt(this.ball);
        b.writeDouble(this.posX);
        b.writeDouble(this.posY);
        b.writeDouble(this.vX);
        b.writeDouble(this.vY);
    }

    @Override
    public void read(ByteBuf b) {
        this.ball = b.readInt();
        this.posX = b.readDouble();
        this.posY = b.readDouble();
        this.vX = b.readDouble();
        this.vY = b.readDouble();
    }

    @Override
    public void execute() {
        if (this.clientID == null) {
            TankBall.Ball b = TankBall.Ball.idMap.get(this.ball);
            if (b != null) {
                b.posX = this.posX;
                b.posY = this.posY;
                b.vX = this.vX;
                b.vY = this.vY;
            }
        }
    }
}
