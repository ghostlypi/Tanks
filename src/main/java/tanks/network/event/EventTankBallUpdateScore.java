package tanks.network.event;

import io.netty.buffer.ByteBuf;
import tanks.Game;
import tanks.Movable;
import tanks.minigames.TankBall;

public class EventTankBallUpdateScore extends PersonalEvent{

    public int redTeamScore;
    public int blueTeamScore;

    public EventTankBallUpdateScore() {

    }

    public EventTankBallUpdateScore(TankBall b) {
        this.redTeamScore = b.redTeamScore;
        this.blueTeamScore = b.blueTeamScore;
    }

    @Override
    public void write(ByteBuf b) {
        b.writeInt(redTeamScore);
        b.writeInt(blueTeamScore);
    }

    @Override
    public void read(ByteBuf b) {
        this.redTeamScore = b.readInt();
        this.blueTeamScore = b.readInt();
    }

    @Override
    public void execute() {
        ((TankBall) Game.currentLevel).redTeamScore = this.redTeamScore;
        ((TankBall) Game.currentLevel).blueTeamScore = this.blueTeamScore;
    }
}
