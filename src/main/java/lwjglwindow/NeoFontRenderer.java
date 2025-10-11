package lwjglwindow;

import basewindow.BaseFontRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * TTF Font Renderer using STB Truetype
 */
public class NeoFontRenderer extends BaseFontRenderer
{
    private final int textureId;
    private final STBTTPackedchar.Buffer packedChars;
    private final int bitmapWidth;
    private final int bitmapHeight;
    private final float fontSize;

    private final FloatBuffer xPos = memAllocFloat(1);
    private final FloatBuffer yPos = memAllocFloat(1);
    private final STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();

    /**
     * Create a new TTF font renderer
     * @param window The LWJGL window
     * @param ttfPath Path to the TTF font file (resource path)
     * @param fontSize Font size in pixels
     * @param bitmapWidth Texture atlas width (e.g., 512)
     * @param bitmapHeight Texture atlas height (e.g., 512)
     */
    public NeoFontRenderer(LWJGLWindow window, String ttfPath, float fontSize, int bitmapWidth, int bitmapHeight)
    {
        super(window);
        this.fontSize = fontSize;
        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;

        // Load TTF file from resources
        try
        {
            ByteBuffer ttfData = loadTTFFile(ttfPath);

            // Allocate character data for 95 characters (ASCII 32-126)
            this.packedChars = STBTTPackedchar.malloc(95);

            // Create bitmap for texture atlas
            ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

            // Pack font into texture atlas
            try (STBTTPackContext pc = STBTTPackContext.malloc()) {
                stbtt_PackBegin(pc, bitmap, bitmapWidth, bitmapHeight, 0, 1, NULL);
                stbtt_PackSetOversampling(pc, 2, 2); // 2x2 oversampling for better quality
                // Pack characters from ASCII 32 (space) to ASCII 126 (tilde)
                stbtt_PackFontRange(pc, ttfData, 0, fontSize, 32, packedChars);
                stbtt_PackEnd(pc);
            }

            // Reset bitmap buffer position to read from the beginning
            bitmap.rewind();

            // Diagnostic: Check if bitmap actually has data
            int nonZeroCount = 0;
            int maxAlpha = 0;
            int firstNonZeroIndex = -1;
            for (int i = 0; i < bitmap.capacity(); i++) {
                byte alpha = bitmap.get(i);
                int unsignedAlpha = alpha & 0xFF;  // Convert to unsigned
                if (unsignedAlpha != 0) {
                    nonZeroCount++;
                    if (firstNonZeroIndex == -1) firstNonZeroIndex = i;
                    if (unsignedAlpha > maxAlpha) maxAlpha = unsignedAlpha;
                }
            }
            System.out.println("[NeoFontRenderer] Bitmap analysis:");
            System.out.println("  Non-zero pixels: " + nonZeroCount + " / " + bitmap.capacity());
            System.out.println("  Max alpha value: " + maxAlpha);
            System.out.println("  First non-zero at index: " + firstNonZeroIndex);
            bitmap.rewind(); // Rewind again after diagnostic check

            // Create OpenGL texture
            this.textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            // Use GL_ALPHA format directly (matching working TruetypeOversample example)
            // With GL_ALPHA: OpenGL uses glColor for RGB, texture provides only alpha channel
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, bitmapWidth, bitmapHeight,
                0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, bitmap);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

            System.out.println("  Using GL_ALPHA texture format");
        }
        catch (IOException e)
        {
            System.err.println("Failed to load TTF font: " + ttfPath);
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize font renderer", e);
        }
    }

    /**
     * Convenience constructor using default texture size
     */
    public NeoFontRenderer(LWJGLWindow window, String ttfPath, float fontSize)
    {
        this(window, ttfPath, fontSize, 512, 512);
    }

    /**
     * Convenience constructor using Bullet.ttf with default size
     */
    public NeoFontRenderer(LWJGLWindow window, String ttfPath)
    {
        this(window, ttfPath, 24.0f, 512, 512);
    }

    private ByteBuffer loadTTFFile(String path) throws IOException
    {
        InputStream stream = NeoFontRenderer.class.getResourceAsStream(path);
        if (stream == null)
            throw new IOException("Could not find font: " + path);

        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        stream.close();

        byte[] bytes = buffer.toByteArray();
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public boolean supportsChar(char c)
    {
        // Support characters from ASCII 32 (space) to ASCII 126 (tilde)
        return c >= 32 && c <= 126;
    }

    @Override
    public void addFont(String imageFile, String chars, int[] charSizes)
    {
        // Not applicable for TTF renderer - bitmap fonts are not supported
        // This method is kept for compatibility but does nothing
        System.err.println("Warning: NeoFontRenderer.addFont() called but TTF renderer does not support bitmap fonts. Ignoring: " + imageFile);
    }

    private static boolean firstCharRendered = false;

    private float drawChar(double x, double y, double z, double sX, double sY, char c, boolean depthtest)
    {
        // Convert character to index in our packed character array (ASCII 32-126 -> 0-94)
        int charIndex = c - 32;
        if (charIndex < 0 || charIndex >= 95)
            charIndex = '?' - 32; // Use '?' as fallback

        // Reset position buffers
        xPos.put(0, 0);
        yPos.put(0, 0);

        // Get packed quad for this character
        packedChars.position(0); // Position at start of character data
        stbtt_GetPackedQuad(packedChars, bitmapWidth, bitmapHeight, charIndex, xPos, yPos, quad, false);
        packedChars.clear();

        float advance = xPos.get(0);

        // Calculate scaled coordinates
        double x0 = x + quad.x0() * sX;
        double y0 = y + quad.y0() * sY;
        double x1 = x + quad.x1() * sX;
        double y1 = y + quad.y1() * sY;

        // Diagnostic for first rendered character
        if (!firstCharRendered) {
            System.out.println("[NeoFontRenderer] First character render:");
            System.out.println("  Char: '" + c + "' (code " + (int)c + ")");
            System.out.println("  Texture coords: s0=" + quad.s0() + " t0=" + quad.t0() + " s1=" + quad.s1() + " t1=" + quad.t1());
            System.out.println("  Quad size: " + (quad.x1() - quad.x0()) + " x " + (quad.y1() - quad.y0()));
            System.out.println("  Screen coords: (" + x0 + "," + y0 + ") to (" + x1 + "," + y1 + ")");
            System.out.println("  Texture ID: " + textureId);
            System.out.println("  Color: R=" + this.window.colorR + " G=" + this.window.colorG + " B=" + this.window.colorB + " A=" + this.window.colorA);
            firstCharRendered = true;
        }

        // Draw debug box if enabled
        if (this.drawBox)
        {
            this.window.shapeRenderer.drawRect(x0, y0, x1 - x0, y1 - y0);
        }

        // Save current texture binding
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        // Render the character quad
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Apply current window color (important for colored text!)
        GL11.glColor4d(this.window.colorR, this.window.colorG, this.window.colorB, this.window.colorA);

        if (depthtest)
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        else
            GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(quad.s0(), quad.t0()); GL11.glVertex3d(x0, y0, z);
        GL11.glTexCoord2f(quad.s1(), quad.t0()); GL11.glVertex3d(x1, y0, z);
        GL11.glTexCoord2f(quad.s1(), quad.t1()); GL11.glVertex3d(x1, y1, z);
        GL11.glTexCoord2f(quad.s0(), quad.t1()); GL11.glVertex3d(x0, y1, z);
        GL11.glEnd();

        // Restore previous texture binding
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);

        return advance;
    }

    public void drawString(double x, double y, double z, double sX, double sY, String s)
    {
        drawString(x, y, z, sX, sY, s, true);
    }

    @Override
    public void drawString(double x, double y, double z, double sX, double sY, String s, boolean depth)
    {
        if (depth)
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        else
            GL11.glDisable(GL11.GL_DEPTH_TEST);

        double curX = x;
        char[] chars = s.toCharArray();

        // Store original color for reset
        double r0 = this.window.colorR;
        double g0 = this.window.colorG;
        double b0 = this.window.colorB;
        double a0 = this.window.colorA;
        double opacity = a0;

        for (int i = 0; i < chars.length; i++)
        {
            // Skip special unicode character
            if (chars[i] == 'Â')
                continue;

            // Handle color codes (§)
            if (chars[i] == '§')
            {
                if (s.length() <= i + 1)
                    continue;

                // Reset color code (§r)
                if (chars[i + 1] == 'r')
                {
                    i++;
                    this.window.setColor(r0 * 255, g0 * 255, b0 * 255, a0 * 255);
                    continue;
                }

                // Color code format: §RRRGGGBBBAAA (12 digits)
                if (s.length() <= i + 12)
                    continue;

                try
                {
                    int r = Integer.parseInt("" + chars[i + 1] + chars[i + 2] + chars[i + 3]);
                    int g = Integer.parseInt("" + chars[i + 4] + chars[i + 5] + chars[i + 6]);
                    int b = Integer.parseInt("" + chars[i + 7] + chars[i + 8] + chars[i + 9]);
                    int a = Integer.parseInt("" + chars[i + 10] + chars[i + 11] + chars[i + 12]);
                    this.window.setColor(r, g, b, a * opacity);
                    i += 12;
                } catch (NumberFormatException e)
                {
                    // Invalid color code, just continue
                }
                continue;
            }

            // Draw the character
            float advance = drawChar(curX, y, z, sX, sY, chars[i], depth);
            curX += advance * sX;
        }

        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void drawString(double x, double y, double sX, double sY, String s)
    {
        drawString(x, y, 0, sX, sY, s, false);
    }

    @Override
    public double getStringSizeX(double sX, String s)
    {
        double width = 0;
        char[] chars = s.toCharArray();

        for (int i = 0; i < chars.length; i++)
        {
            // Skip special unicode character
            if (chars[i] == '\u00C2')
                continue;

            // Handle color codes
            if (chars[i] == '\u00A7')
            {
                if (s.length() <= i + 1)
                    continue;

                if (chars[i + 1] == 'r')
                {
                    i++;
                    continue;
                }

                if (s.length() <= i + 12)
                    continue;

                i += 12;
                continue;
            }

            // Calculate character width
            int charIndex = chars[i] - 32;
            if (charIndex >= 0 && charIndex < 95)
            {
                xPos.put(0, 0);
                yPos.put(0, 0);
                packedChars.position(0); // Position at start of character data
                stbtt_GetPackedQuad(packedChars, bitmapWidth, bitmapHeight, charIndex, xPos, yPos, quad, false);
                packedChars.clear();
                width += xPos.get(0) * sX;
            }
        }

        return width;
    }

    @Override
    public double getStringSizeY(double sY, String s)
    {
        return fontSize * sY;
    }

    /**
     * Cleanup resources
     */
    public void cleanup()
    {
        if (packedChars != null)
            packedChars.free();
        if (quad != null)
            quad.free();
        if (xPos != null)
            memFree(xPos);
        if (yPos != null)
            memFree(yPos);
        GL11.glDeleteTextures(textureId);
    }
}
