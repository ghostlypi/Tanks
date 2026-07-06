package basewindow;

public class PlatformType
{
    public final String name;
    public final PlatformType parent;

    public PlatformType(String name, PlatformType parent)
    {
        this.name = name;
        this.parent = parent;
    }

    public boolean is(PlatformType type)
    {
        PlatformType current = this;
        while (current != null)
        {
            if (current == type)
                return true;
            
            current = current.parent;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    public static final PlatformType Unknown = new PlatformType("Unknown", null);
    public static final PlatformType LWJGL = new PlatformType("LWJGL", Unknown);
    public static final PlatformType lwjgl = LWJGL;
    public static final PlatformType LibGDX = new PlatformType("LibGDX", Unknown);
    public static final PlatformType IOS = new PlatformType("IOS", LibGDX);
    public static final PlatformType Android = new PlatformType("Android", LibGDX);
}
