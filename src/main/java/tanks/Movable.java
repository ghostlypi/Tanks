package tanks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tanks.effect.AttributeModifier;
import tanks.effect.EffectManager;
import tanks.gui.screen.ScreenGame;
import tanks.gui.screen.leveleditor.selector.SelectorTeam;
import tanks.tank.NameTag;
import tanks.tankson.MetadataProperty;
import tanks.tankson.Property;

import java.lang.reflect.Field;

public abstract class Movable extends SolidGameObject implements IDrawableForInterface
{
	private EffectManager em;

	public double lastPosX, lastPosY, lastPosZ = 0;
	public double vX, vY, vZ = 0;
	public double lastFinalVX, lastFinalVY, lastFinalVZ;
	public double lastVX, lastVY, lastVZ;
	public double lastOriginalVX, lastOriginalVY, lastOriginalVZ;

	public boolean destroy = false;
	public boolean dealsDamage = true;

	public NameTag nameTag;
	public boolean showName = false;

	public boolean affectedByFrameFrequency = true;

	public boolean skipNextUpdate = false;

	public int drawLevel = 3;
	public boolean isRemote = false;
	public boolean managedMotion = true;

	@MetadataProperty(id = "team", name = "Team", selector = SelectorTeam.selector_name, image = "team.png", keybind = "editor.team")
	public Team team;

	public Movable(double x, double y)
	{
		this.posX = x;
		this.posY = y;

		this.lastPosX = x;
		this.lastPosY = y;
	}

	public void preUpdate()
	{
		double frameFrequency = affectedByFrameFrequency ? Panel.frameFrequency : 1;
		this.lastVX = (this.posX - this.lastPosX) / frameFrequency;
		this.lastVY = (this.posY - this.lastPosY) / frameFrequency;
		this.lastVZ = (this.posZ - this.lastPosZ) / frameFrequency;

		this.lastOriginalVX = this.vX;
		this.lastOriginalVY = this.vY;
		this.lastOriginalVZ = this.vZ;

		this.lastPosX = this.posX;
		this.lastPosY = this.posY;
		this.lastPosZ = this.posZ;
	}

	public void update()
	{
		double frameFrequency = affectedByFrameFrequency ? Panel.frameFrequency : 1;

		if (!destroy)
		{
			em().update();

			double vX2 = this.vX;
			double vY2 = this.vY;
			double vZ2 = this.vZ;

			if (this.managedMotion)
			{
				// Apply velocity modifiers
				vX2 = em.getAttributeValue(AttributeModifier.velocity, vX2);
				vY2 = em.getAttributeValue(AttributeModifier.velocity, vY2);
				vZ2 = em.getAttributeValue(AttributeModifier.velocity, vZ2);

				this.lastFinalVX = vX2 * ScreenGame.finishTimer / ScreenGame.finishTimerMax;
				this.lastFinalVY = vY2 * ScreenGame.finishTimer / ScreenGame.finishTimerMax;
				this.lastFinalVZ = vZ2 * ScreenGame.finishTimer / ScreenGame.finishTimerMax;

				this.posX += this.lastFinalVX * frameFrequency;
				this.posY += this.lastFinalVY * frameFrequency;
				this.posZ += this.lastFinalVZ * frameFrequency;
			}
		}
	}

	public void initEffectManager(EffectManager em)
	{

	}

	@Override
	public double getSize()
	{
		return 0;
	}

	/** Alias for {@link #getEffectManager()} */
	public EffectManager em()
	{
		return getEffectManager();
	}

	public EffectManager getEffectManager()
	{
		if (em == null)
		{
			em = new EffectManager(this);
			initEffectManager(em);
		}
		return em;
	}

	public void setMotionInDirection(double x, double y, double velocity)
	{
		double angle = getAngleInDirection(x, y);
		this.vX = velocity * Math.cos(angle);
		this.vY = velocity * Math.sin(angle);
	}

	public void setMotionAwayFromDirection(double x, double y, double velocity)
	{
		double angle = getAngleInDirection(x, y);
		angle += Math.PI;
		this.vX = velocity * Math.cos(angle);
		this.vY = velocity * Math.sin(angle);
	}

	public void setMotionInDirectionWithOffset(double x, double y, double velocity, double a)
	{
		double angle = getAngleInDirection(x, y);
		angle += a;
		this.vX = velocity * Math.cos(angle);
		this.vY = velocity * Math.sin(angle);
	}

	static double pi_over_4 = Math.PI / 4;
	static double fastAtan(double a)
	{
		if (a < -1 || a > 1)
			return Math.atan(a);

		return pi_over_4 * a - a * (Math.abs(a) - 1) * (0.2447 + 0.0663 * Math.abs(a));
	}

	public double getAngleInDirection(double x, double y)
	{
		x -= this.posX;
		y -= this.posY;

		double angle = 0;
		if (x > 0)
			angle = fastAtan(y/x);
		else if (x < 0)
			angle = fastAtan(y/x) + Math.PI;
		else
		{
			if (y > 0)
				angle = Math.PI / 2;
			else if (y < 0)
				angle = Math.PI * 3 / 2;
		}

		return angle;
	}

	public double getPolarDirection()
	{
		return Movable.getPolarDirection(this.vX, this.vY);
	}

	public double getPolarPitch()
	{
		return Math.atan(this.vZ / this.getSpeed());
	}

	public double getLastPolarDirection()
	{
		return Movable.getPolarDirection(this.lastVX, this.lastVY);
	}

	public static double getPolarDirection(double x, double y)
	{
		double angle = 0;
		if (x > 0)
			angle = Math.atan(y/x);
		else if (x < 0)
			angle = Math.atan(y/x) + Math.PI;
		else
		{
			if (y > 0)
				angle = Math.PI / 2;
			else if (y < 0)
				angle = Math.PI * 3 / 2;
		}

		return angle;
	}

	public void setPolarMotion(double angle, double velocity)
	{
		double velX = velocity * Math.cos(angle);
		double velY = velocity * Math.sin(angle);
		this.vX = velX;
		this.vY = velY;
	}

	public void set3dPolarMotion(double angle1, double angle2, double velocity)
	{
		double velX = velocity * Math.cos(angle1) * Math.cos(angle2);
		double velY = velocity * Math.sin(angle1) * Math.cos(angle2);
		double velZ = velocity * Math.sin(angle2);

		this.vX = velX;
		this.vY = velY;
		this.vZ = velZ;
	}

	public void addPolarMotion(double angle, double velocity)
	{
		double velX = velocity * Math.cos(angle);
		double velY = velocity * Math.sin(angle);
		this.vX += velX;
		this.vY += velY;
	}

	public void add3dPolarMotion(double angle1, double angle2, double velocity)
	{
		double velX = velocity * Math.cos(angle1) * Math.cos(angle2);
		double velY = velocity * Math.sin(angle1) * Math.cos(angle2);
		double velZ = velocity * Math.sin(angle2);

		this.vX += velX;
		this.vY += velY;
		this.vZ += velZ;
	}

	public void moveInDirection(double x, double y, double amount)
	{
		this.posX += amount * x;
		this.posY += amount * y;
	}

	public void moveInAngle(double a, double amount)
	{
		this.posX += amount * Math.cos(a);
		this.posY += amount * Math.sin(a);
	}

	public double getSpeed()
	{
		return Math.sqrt(Math.pow(this.vX, 2) + Math.pow(this.vY, 2));
	}

	public double getLastSpeed()
	{
		return Math.sqrt(Math.pow(this.lastVX, 2) + Math.pow(this.lastVY, 2));
	}

	public double getMotionInDirection(double angle)
	{
		return this.getSpeed() * Math.cos(this.getPolarDirection() - angle);
	}

	public double getLastMotionInDirection(double angle)
	{
		return this.getLastSpeed() * Math.cos(this.getLastPolarDirection() - angle);
	}

	public void drawTeam()
	{
		Drawing.drawing.setFontSize(20);
		if (this.team != null)
			Drawing.drawing.drawText(this.posX, this.posY + 35, this.team.name);
	}

	public static double[] getLocationInDirection(double angle, double distance)
	{
		return new double[]{distance * Math.cos(angle), distance * Math.sin(angle)};	
	}

	public abstract void draw();
	
	public void drawAt(double x, double y)
	{	
		double x1 = this.posX;
		double y1 = this.posY;
		this.posX = x;
		this.posY = y;
		this.draw();
		this.posX = x1;
		this.posY = y1;
	}

	/** Field to cache the movable array for reuse */
	private static final ObjectArrayList<Movable> movableOut = new ObjectArrayList<>();

	/** Expects all pixel coordinates.
	 * @return all the movables within the specified range */
	public static ObjectArrayList<Movable> getMovablesInRange(double x1, double y1, double x2, double y2)
	{
		movableOut.clear();
		for (Movable m : Game.movables)
		{
			if (Game.isOrdered(true, x1, m.posX, x2) && Game.isOrdered(true, y1, m.posY, y2))
				movableOut.add(m);
		}
		return movableOut;
	}


	/** Expects all pixel coordinates.
	 * @return all the movables within a certain radius of the position */
	public static ObjectArrayList<Movable> getMovablesInRadius(double posX, double posY, double radius)
	{
		movableOut.clear();
		for (Movable o : Game.movables)
		{
			if (Movable.sqDistBetw(o.posX, o.posY, posX, posY) < radius * radius)
				movableOut.add(o);
		}
		return movableOut;
	}

	public static Movable findMovable(double x, double y)
	{
		ObjectArrayList<Movable> movables = Movable.getMovablesInRadius(x, y, 1);
		if (!movables.isEmpty())
			return movables.get(0);
		return null;
	}

	public void drawForInterface(double x, double y)
	{
		this.drawAt(x, y);
	}

	public void setEffectManager(EffectManager em)
	{
		this.em = em;
	}

	public void randomize()
	{
		try
		{
			for (Field f: this.getClass().getFields())
			{
				if (f.getAnnotation(Property.class) == null || Math.random() < 0.999)
					continue;

				if (f.getType().equals(double.class))
					f.set(this, (double) (f.get(this)) * Math.random() * 1.5 + 0.5);
				else if (f.getType().equals(int.class))
					f.set(this, (int) ((int)(f.get(this)) * Math.random() * 1.5 + 0.5));
				else if (f.getType().isEnum())
				{
					Enum[] els = ((Enum) f.get(this)).getClass().getEnumConstants();
					f.set(this, els[(int) (Math.random() * els.length)]);
				}
				else if (Movable.class.isAssignableFrom(f.getType()) && f.get(this) != null)
				{
					((Movable) (f.get(this))).randomize();
				}
			}
		}
		catch (Exception e)
		{
			Game.exitToCrash(e);
		}
	}
}
