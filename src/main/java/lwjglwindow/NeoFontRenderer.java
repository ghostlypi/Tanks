package lwjglwindow;

import basewindow.BaseFontRenderer;
import basewindow.BaseWindow;

/**
 * TTF Font Renderer using STB Truetype
 */
public class NeoFontRenderer extends BaseFontRenderer
{

    public NeoFontRenderer(LWJGLWindow b, String path) {
        super(b);
    }

    @Override
    public boolean supportsChar(char c)
    {
        return false;
    }

    @Override
    public void drawString(double x, double y, double z, double sX, double sY, String s, boolean depth)
    {

    }

    @Override
    public void drawString(double x, double y, double z, double sX, double sY, String s)
    {

    }

    @Override
    public void drawString(double x, double y, double sX, double sY, String s)
    {

    }

    @Override
    public double getStringSizeX(double sX, String s)
    {
        return 0;
    }

    @Override
    public double getStringSizeY(double sY, String s)
    {
        return 0;
    }

    @Override
    public void addFont(String imageFile, String chars, int[] charSizes)
    {

    }
}
