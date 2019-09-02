package tanks.tank;

import tanks.Game;
import tanks.bullets.Bullet;

public class TankMint extends TankAIControlled
{
	public TankMint(String name, double x, double y, double angle)
	{
		super(name, x, y, Game.tank_size, 60, 180, 140, angle, ShootAI.straight);

		this.enableMovement = true;
		this.speed = 1;
		this.enableMineLaying = false;
		this.enablePredictiveFiring = false;
		this.liveBulletMax = 1;
		this.cooldownRandom = 60;
		this.cooldownBase = 240;
		this.aimTurretSpeed = 0.02;
		this.bulletBounces = 0;
		this.bulletEffect = Bullet.BulletEffect.fire;
		this.bulletSpeed = 25.0 / 2;
		this.enableLookingAtTargetEnemy = false;
		this.motionChangeChance = 0.001;
		
		this.coinValue = 2;
	}
}