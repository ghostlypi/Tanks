package tanks.minigames;

import jdk.internal.foreign.abi.Binding;
import tanks.Drawing;
import tanks.Game;
import tanks.Movable;
import tanks.Team;
import tanks.bullet.Bullet;
import tanks.gui.screen.ScreenPartyHost;
import tanks.gui.screen.ScreenPartyLobby;
import tanks.network.event.EventTankBallUpdate;
import tanks.obstacle.Obstacle;
import tanks.tank.Tank;
import tanks.tankson.Property;

import java.util.HashMap;

public class TankBall extends Minigame {

    public static class Ball extends Movable {

        public double size;
        public int id;
        public static int idCounter = 0;
        public static HashMap<Integer, Ball> idMap = new HashMap<>();

        public Ball(double x, double y, double size) {
            super(x,y);
            this.size = size;
            this.id = idCounter++;
            idMap.put(this.id, this);
        }

        @Override
        public void update(){
            // Pre-define collision variables
            double dx, dy, d;

            // Handle Collisions with Walls
//            if (this.posX + this.size/2 >= (Game.currentSizeX * Game.tile_size)) {
//                this.vX *= -1;
//                this.posX = Game.currentSizeX * Game.tile_size - this.size/2;
//            } else if (this.posX - this.size/2 <= 0) {
//                this.vX *= -1;
//                this.posX = this.size/2;
//            }
            if (this.posY + this.size/2 >= (Game.currentSizeY * Game.tile_size)) {
                this.vY *= -1;
                this.posY = Game.currentSizeY * Game.tile_size - this.size/2;
            } else if (this.posY - this.size/2 <= 0) {
                this.vY *= -1;
                this.posY = this.size/2;
            }

            // Handle Collisions with Moveables (Bullets & Tanks)
            for (Movable m : Game.movables) {
                if (m instanceof Bullet) {
                    Bullet b = (Bullet) m;
                    if (!b.destroy) {
                        dx = (this.posX - b.posX);
                        dy = (this.posY - b.posY);
                        d = Math.sqrt(dx * dx + dy * dy);
                        if (d <= b.size / 2 + this.size / 2) {
                            b.destroy = true;
                            double massDiff = (b.size*b.size*3) / (this.size*this.size);
                            this.vX += b.vX * massDiff;
                            this.vY += b.vY * massDiff;
                        }
                    }
                } else if (m instanceof Tank) {
                    Tank t = (Tank) m;
                    dx = (this.posX - t.posX);
                    dy = (this.posY - t.posY);
                    d = Math.sqrt(dx * dx + dy * dy);
                    if (d <= t.size / 2 + this.size / 2) {
                        double massDiff = (t.size*t.size) / (this.size*this.size);
                        if (t.vX * this.vX <= 0) {
                            this.vX *= -0.9;
                        }
                        if (t.vY * this.vY <= 0) {
                            this.vY *= -0.9;
                        }
                        double vX = this.vX, vY = this.vY;
                        this.vX += t.vX * massDiff;
                        this.vY += t.vY * massDiff;
                        t.vX += vX * massDiff;
                        t.vY += vY * massDiff;
                    }
                }
            }

            //Apply Friction
            this.vX *= 1 - 0.002;
            this.vY *= 1 - 0.002;

            //Update position
            super.update();
            if (ScreenPartyHost.isServer)
                Game.eventsOut.add(new EventTankBallUpdate(this));
        }

        @Override
        public void draw() {
            Drawing.drawing.setColor(255,150,0);
            Drawing.drawing.fillOval(this.posX,this.posY,Game.tile_size,Game.tile_size);
        }
    }

    public TankBall(){
        super("{36,18,125,204,0,20,0,20,0,100,50||33-9-player-2-blue,2-9-player-0-red|red-false-255.0-0.0-0.0,enemy-true,blue-false-0.0-100.0-255.0}");
        this.customLevelEnd = true;
        Ball ball = new Ball(Game.tile_size*18,Game.tile_size*9, Game.tile_size);
        Game.movables.add(ball);
    }

    @Override
    public void update() {
        if (!ScreenPartyLobby.isClient) {
            super.update();
            for (Movable m : Game.movables)
                if (m instanceof Bullet)
                    ((Bullet) m).damage = 0;
        }
    }

    @Override
    public boolean levelEnded(){
        if (!ScreenPartyLobby.isClient) {
            Team red = this.teamsMap.get("red");
            Team blue = this.teamsMap.get("blue");
            for (Movable m : Game.movables) {
                if (m instanceof Ball) {
                    Ball b = (Ball) m;
                    if (b.posX - b.size / 2 > (Game.currentSizeX * Game.tile_size)) {
                        for (Movable p : Game.movables) {
                            if (p instanceof Tank) {
                                if (p.team.equals(blue)) {
                                    ((Tank) p).health = 0;
                                }
                            }
                        }
                        return true;
                    } else if ((b.posX + b.size / 2 <= 0)) {
                        for (Movable p : Game.movables) {
                            if (p instanceof Tank) {
                                if (p.team.equals(red)) {
                                    ((Tank) p).health = 0;
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}