package tanks.minigames;

import basewindow.Model;
import jdk.internal.foreign.abi.Binding;
import tanks.*;
import tanks.bullet.Bullet;
import tanks.gui.screen.ScreenPartyHost;
import tanks.gui.screen.ScreenPartyLobby;
import tanks.network.event.EventStartTankBall;
import tanks.network.event.EventTankBallUpdate;
import tanks.network.event.EventTankBallUpdateScore;
import tanks.obstacle.Obstacle;
import tanks.tank.Mine;
import tanks.tank.Tank;
import tanks.tankson.Property;
import tanks.translation.Translation;

import java.util.HashMap;

public class TankBall extends Minigame {

    public static class Ball extends Movable {

        public double size;
        public int id;
        public static int idCounter = 0;
        public static Ball ball;
        private double xangle;
        private double yangle;

        public Ball(double x, double y, double size) {
            super(x,y);
            this.size = size;
            this.id = idCounter++;
            ball = this;
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
            if (!ScreenPartyLobby.isClient) {
                for (Movable m : Game.movables) {
                    if (m instanceof Bullet) {
                        Bullet b = (Bullet) m;
                        if (!b.destroy) {
                            dx = this.posX - b.posX;
                            dy = this.posY - b.posY;
                            d = dx * dx + dy * dy;
                            if (d < (this.size / 2 + b.size / 2) * (this.size / 2 + b.size / 2)) {
                                b.destroy = true;
                                execute_spherical_collision(b);
                            }
                        }
                    } else if (m instanceof Tank) {
                        Tank t = (Tank) m;
                        dx = this.posX - t.posX;
                        dy = this.posY - t.posY;
                        d = dx * dx + dy * dy;
                        if (d < (this.size / 2 + t.size / 2) * (this.size / 2 + t.size / 2)) {
                            execute_spherical_collision(t);
                        }
                    }
                }
            }

            //Apply Friction
            this.vX *= Math.pow(1 - 0.003, Panel.frameFrequency);
            this.vY *= Math.pow(1 - 0.003, Panel.frameFrequency);

            //Update position
            super.update();
            if (ScreenPartyHost.isServer)
                Game.eventsOut.add(new EventTankBallUpdate(this));
        }

        @Override
        public void draw() {
            Model m = Drawing.drawing.createModel("/models/tankball/");
            Drawing.drawing.setColor(255,255,255);
            this.yangle -= (this.posX - this.lastPosX)/Game.tile_size;
            this.xangle -= (this.posY - this.lastPosY)/Game.tile_size;
            Drawing.drawing.drawModel(m, this.posX, this.posY, Game.tile_size/2, Game.tile_size, Game.tile_size, Game.tile_size, this.xangle, this.yangle, 0);
        }

        public void execute_spherical_collision(Bullet m) {
            double toAngle = this.getAngleInDirection(m.posX, m.posY);

            double ourSpeed = this.getSpeed();
            double theirSpeed = m.getSpeed();

            double ourDir = this.getPolarDirection();
            double theirDir = m.getPolarDirection();

            double ourMass = this.size * this.size;
            double theirMass = m.size * m.size * 3;

            double co1 = (ourSpeed * Math.cos(ourDir - toAngle) * (ourMass - theirMass) + 2 * theirMass * theirSpeed * Math.cos(theirDir - toAngle)) / Math.max(1, ourMass + theirMass);
            double vX1 = co1 * Math.cos(toAngle) + ourSpeed * Math.sin(ourDir - toAngle) * Math.cos(toAngle + Math.PI / 2);
            double vY1 = co1 * Math.sin(toAngle) + ourSpeed * Math.sin(ourDir - toAngle) * Math.sin(toAngle + Math.PI / 2);

            this.vX = vX1;
            this.vY = vY1;
            m.destroy = true;
        }

        public void execute_spherical_collision(Tank m) {
            double toAngle = this.getAngleInDirection(m.posX, m.posY);

            double ourSpeed = this.getSpeed();
            double theirSpeed = m.getSpeed();

            double ourDir = this.getPolarDirection();
            double theirDir = m.getPolarDirection();

            double ourMass = this.size * this.size;
            double theirMass = m.size * m.size;

            double co1 = (ourSpeed * Math.cos(ourDir - toAngle) * (ourMass - theirMass) + 2 * theirMass * theirSpeed * Math.cos(theirDir - toAngle)) / Math.max(1, ourMass + theirMass);
            double vX1 = co1 * Math.cos(toAngle) + ourSpeed * Math.sin(ourDir - toAngle) * Math.cos(toAngle + Math.PI / 2);
            double vY1 = co1 * Math.sin(toAngle) + ourSpeed * Math.sin(ourDir - toAngle) * Math.sin(toAngle + Math.PI / 2);

            double co2 = (theirSpeed * Math.cos(theirDir - toAngle) * (theirMass - ourMass) + 2 * ourMass * ourSpeed * Math.cos(ourDir - toAngle)) / Math.max(1, theirMass + ourMass);
            double vX2 = co2 * Math.cos(toAngle) + theirSpeed * Math.sin(theirDir - toAngle) * Math.cos(toAngle + Math.PI / 2);
            double vY2 = co2 * Math.sin(toAngle) + theirSpeed * Math.sin(theirDir - toAngle) * Math.sin(toAngle + Math.PI / 2);

            this.vX = vX1;
            this.vY = vY1;
            m.vX = vX2;
            m.vY = vY2;
            this.posX += this.vX;
            this.posY += this.vY;
        }
    }

    public int redTeamScore;
    public int blueTeamScore;

    public static String generateLevelString(){
        int h;
        if (!ScreenPartyLobby.isClient) {
            h = ScreenPartyHost.includedPlayers.size() + 9;
        } else {
            h = ScreenPartyLobby.includedPlayers.size() + 9;
        }

        int w = h*2;

        return "{"+w+","+h+",125,204,0,20,0,20,0,100,50||"+(w-1)+"-"+h/2+"-player-2-blue,"+0+"-"+h/2+"-player-0-red|red-false-255.0-0.0-0.0,enemy-true,blue-false-0.0-100.0-255.0}";
    }

    public TankBall(){
        super(ScreenPartyLobby.isClient ? Game.currentLevelString : generateLevelString());
        this.customLevelEnd = true;
        Ball ball = new Ball(Game.tile_size*this.sizeX/2,Game.tile_size*this.sizeY/2, Game.tile_size);
        Game.movables.add(ball);
        if (ScreenPartyHost.isServer)
            Game.eventsOut.add(new EventStartTankBall(this));
        this.redTeamScore = 0;
        this.blueTeamScore = 0;
    }

    @Override
    public void update() {
        if (!ScreenPartyLobby.isClient) {
            super.update();
            for (Movable m : Game.movables)
                if (m instanceof Tank) {
                    ((Tank) m).resistBullets = true;
                    ((Tank) m).resistExplosions = true;
                } else if (m instanceof Ball) {
                    Ball b = (Ball) m;
                    if (b.posX - b.size / 2 > (Game.currentSizeX * Game.tile_size)) {
                        for (Movable n : Game.movables)
                            if (n instanceof Bullet)
                                ((Bullet) n).destroy=true;
                            else if (n instanceof Mine)
                                ((Mine) n).explode();
                        this.redTeamScore++;
                        b.posX = Game.tile_size*this.sizeX/2;
                        b.posY = Game.tile_size*this.sizeY/2;
                        b.vX = 0;
                        b.vY = 0;
                    } else if ((b.posX + b.size / 2 <= 0)) {
                        for (Movable n : Game.movables)
                            if (n instanceof Bullet)
                                ((Bullet) n).destroy=true;
                            else if (n instanceof Mine)
                                ((Mine) n).explode();
                        this.blueTeamScore++;
                        b.posX = Game.tile_size*this.sizeX/2;
                        b.posY = Game.tile_size*this.sizeY/2;
                        b.vX = 0;
                        b.vY = 0;
                    }
                    if (ScreenPartyHost.isServer)
                        Game.eventsOut.add(new EventTankBallUpdateScore(this));
                }
        }
    }

    @Override
    public void draw() {
        super.draw();
        Drawing.drawing.setColor(255,255,255);
        double posX = Drawing.drawing.interfaceSizeX / 2;
        double posY = 50;

        String s = this.redTeamScore+"-"+this.blueTeamScore;
        Drawing.drawing.setInterfaceFontSize(36 * (1 + 0.25 * 1.5));
        double size = Game.game.window.fontRenderer.getStringSizeX(Drawing.drawing.fontSize, s) / Drawing.drawing.interfaceScale;
        Drawing.drawing.displayInterfaceText(posX - size / 2, posY, false, s);
    }

    @Override
    public boolean levelEnded() {
        if (!ScreenPartyLobby.isClient) {
            Team red = this.teamsMap.get("red");
            Team blue = this.teamsMap.get("blue");
            if (this.redTeamScore > this.blueTeamScore
                    && this.redTeamScore >= 5
                    && this.redTeamScore - this.blueTeamScore >= 2) {
                for (Movable p : Game.movables) {
                    if (p instanceof Tank) {
                        if (p.team.equals(blue)) {
                            ((Tank) p).health = 0;
                        }
                    }
                }
                return true;
            } else if (this.blueTeamScore > this.redTeamScore
                    && this.blueTeamScore >= 5
                    && this.blueTeamScore - this.redTeamScore >= 2) {
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
        return false;
    }
}