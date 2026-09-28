package lwjglwindow;

import basewindow.InputCodes;
import org.lwjgl.glfw.GLFWGamepadState;

import java.util.ArrayList;
import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Polls the first connected GLFW gamepad (e.g. the Steam Frame controllers exposed through Steam Input)
 * and translates it into the keyboard/mouse input the rest of the game already understands.
 *
 * Buttons are injected as virtual key and mouse presses, the right stick (and the left stick outside of gameplay)
 * moves a virtual mouse pointer, and the left stick is exposed as an analog value for tank movement.
 */
public class GamepadHandler
{
    public static final double stickDeadzone = 0.2;
    public static final double triggerThreshold = 0.5;

    /** Pointer speed at full stick deflection, in fractions of the window's larger dimension per second */
    public static double pointerSpeed = 1.2;

    /** Set the TANKS_GAMEPAD_DEBUG environment variable to log detected joysticks and every button/axis change */
    public static final boolean debug = System.getenv("TANKS_GAMEPAD_DEBUG") != null;

    protected static final String[] buttonNames = {"A", "B", "X", "Y", "LB", "RB", "Back/View", "Start/Menu", "Guide",
        "L3", "R3", "DPad Up", "DPad Right", "DPad Down", "DPad Left"};
    protected static final String[] axisNames = {"Left X", "Left Y", "Right X", "Right Y", "LT", "RT"};
    protected final boolean[] debugButtons = new boolean[buttonNames.length];
    protected final int[] debugAxes = new int[axisNames.length];

    protected final LWJGLWindow window;
    protected final GLFWGamepadState state = GLFWGamepadState.create();

    protected int joystick = -1;

    /** Button to key code, using GLFW_GAMEPAD_BUTTON_* indices */
    protected final HashMap<Integer, Integer> buttonKeys = new HashMap<>();
    /** Button to mouse button, using GLFW_GAMEPAD_BUTTON_* indices */
    protected final HashMap<Integer, Integer> buttonMouse = new HashMap<>();

    protected ArrayList<Integer> heldKeys = new ArrayList<>();
    protected ArrayList<Integer> heldButtons = new ArrayList<>();

    protected boolean prevScrollUp = false;
    protected boolean prevScrollDown = false;

    protected double pointerX;
    protected double pointerY;
    protected double lastRealMouseX = Double.NaN;
    protected double lastRealMouseY = Double.NaN;

    public GamepadHandler(LWJGLWindow window)
    {
        this.window = window;

        buttonMouse.put(GLFW_GAMEPAD_BUTTON_A, InputCodes.MOUSE_BUTTON_1);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_B, InputCodes.KEY_ESCAPE);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_START, InputCodes.KEY_ESCAPE);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_Y, InputCodes.KEY_LEFT_SHIFT);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_BACK, InputCodes.KEY_TAB);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_X, InputCodes.KEY_PERIOD);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, InputCodes.KEY_I);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_DPAD_UP, InputCodes.KEY_UP);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_DPAD_DOWN, InputCodes.KEY_DOWN);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_DPAD_LEFT, InputCodes.KEY_LEFT);
        buttonKeys.put(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT, InputCodes.KEY_RIGHT);
    }

    /** Finds the first joystick that GLFW recognizes as a gamepad. Plain joysticks (such as sensors) are skipped. */
    protected int findGamepad()
    {
        if (joystick >= 0 && glfwJoystickIsGamepad(joystick))
            return joystick;

        for (int i = GLFW_JOYSTICK_1; i <= GLFW_JOYSTICK_LAST; i++)
        {
            if (glfwJoystickIsGamepad(i))
                return i;
        }

        return -1;
    }

    /**
     * Must be called once per frame after the real cursor position has been read into the window.
     * @param dt seconds since the last frame
     */
    public void update(double dt)
    {
        int j = findGamepad();

        if (j != joystick)
        {
            joystick = j;
            if (j >= 0)
                System.out.println("Gamepad connected: " + glfwGetGamepadName(j));

            if (debug)
                logJoysticks();
        }

        ArrayList<Integer> keys = new ArrayList<>();
        ArrayList<Integer> buttons = new ArrayList<>();
        boolean scrollUp = false;
        boolean scrollDown = false;

        window.gamepadConnected = joystick >= 0 && glfwGetGamepadState(joystick, state);

        if (debug && window.gamepadConnected)
            logState();

        if (window.gamepadConnected)
        {
            for (int b: buttonKeys.keySet())
            {
                if (state.buttons(b) == GLFW_PRESS && !keys.contains(buttonKeys.get(b)))
                    keys.add(buttonKeys.get(b));
            }

            for (int b: buttonMouse.keySet())
            {
                if (state.buttons(b) == GLFW_PRESS && !buttons.contains(buttonMouse.get(b)))
                    buttons.add(buttonMouse.get(b));
            }

            // Triggers rest at -1 and go to 1 when fully pressed
            if (state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) > triggerThreshold && !buttons.contains(InputCodes.MOUSE_BUTTON_1))
                buttons.add(InputCodes.MOUSE_BUTTON_1);

            if (state.axes(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) > triggerThreshold && !buttons.contains(InputCodes.MOUSE_BUTTON_2))
                buttons.add(InputCodes.MOUSE_BUTTON_2);

            // Scrolling down selects the next hotbar item, so the right bumper scrolls down
            scrollUp = state.buttons(GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER) == GLFW_PRESS;
            scrollDown = state.buttons(GLFW_GAMEPAD_BUTTON_LEFT_BUMPER) == GLFW_PRESS;
        }

        applyHeld(keys, heldKeys, window.pressedKeys, window.validPressedKeys, true);
        applyHeld(buttons, heldButtons, window.pressedButtons, window.validPressedButtons, false);
        heldKeys = keys;
        heldButtons = buttons;

        if (scrollUp && !prevScrollUp)
            window.validScrollUp = true;
        if (scrollDown && !prevScrollDown)
            window.validScrollDown = true;

        prevScrollUp = scrollUp;
        prevScrollDown = scrollDown;

        double[] move = window.gamepadConnected ? deadzone(state.axes(GLFW_GAMEPAD_AXIS_LEFT_X), state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y)) : new double[2];
        window.gamepadMoveX = move[0];
        window.gamepadMoveY = move[1];

        updatePointer(dt, move);
    }

    protected void applyHeld(ArrayList<Integer> now, ArrayList<Integer> before, ArrayList<Integer> pressed, ArrayList<Integer> valid, boolean text)
    {
        for (int i: now)
        {
            if (!before.contains(i))
            {
                pressed.add(i);
                valid.add(i);

                if (text)
                {
                    window.textPressedKeys.add(i);
                    window.textValidPressedKeys.add(i);
                }
            }
        }

        for (int i: before)
        {
            if (!now.contains(i))
            {
                pressed.remove((Integer) i);
                valid.remove((Integer) i);

                if (text)
                {
                    window.textPressedKeys.remove((Integer) i);
                    window.textValidPressedKeys.remove((Integer) i);
                }
            }
        }
    }

    /**
     * Moves the virtual pointer. The real mouse takes over again as soon as it moves,
     * so the OS cursor never needs to be warped (which Wayland compositors may ignore).
     */
    protected void updatePointer(double dt, double[] leftStick)
    {
        double realX = window.absoluteMouseX;
        double realY = window.absoluteMouseY;
        boolean realMoved = Double.isNaN(lastRealMouseX) || realX != lastRealMouseX || realY != lastRealMouseY;
        lastRealMouseX = realX;
        lastRealMouseY = realY;

        if (realMoved || !window.gamepadPointerActive)
        {
            pointerX = realX;
            pointerY = realY;
            window.gamepadPointerActive = false;
        }

        if (!window.gamepadConnected)
            return;

        double[] look = deadzone(state.axes(GLFW_GAMEPAD_AXIS_RIGHT_X), state.axes(GLFW_GAMEPAD_AXIS_RIGHT_Y));
        double vx = look[0];
        double vy = look[1];

        if (window.gamepadLeftStickPointer)
        {
            vx += leftStick[0];
            vy += leftStick[1];
        }

        if (vx != 0 || vy != 0)
        {
            double mag = Math.min(1, Math.sqrt(vx * vx + vy * vy));

            // Squared response curve for finer control near the center of the stick
            double speed = pointerSpeed * Math.max(window.absoluteWidth, window.absoluteHeight) * mag * dt;
            double len = Math.sqrt(vx * vx + vy * vy);
            pointerX = Math.max(0, Math.min(window.absoluteWidth, pointerX + vx / len * speed * mag));
            pointerY = Math.max(0, Math.min(window.absoluteHeight, pointerY + vy / len * speed * mag));
            window.gamepadPointerActive = true;
        }

        if (window.gamepadPointerActive)
        {
            window.absoluteMouseX = pointerX;
            window.absoluteMouseY = pointerY;
        }
    }

    protected void logJoysticks()
    {
        for (int i = GLFW_JOYSTICK_1; i <= GLFW_JOYSTICK_LAST; i++)
        {
            if (glfwJoystickPresent(i))
                System.out.println("[gamepad] joystick " + i + ": " + glfwGetJoystickName(i) + " guid=" + glfwGetJoystickGUID(i)
                    + " gamepad mapping=" + glfwJoystickIsGamepad(i));
        }
    }

    protected void logState()
    {
        for (int b = 0; b < buttonNames.length; b++)
        {
            boolean pressed = state.buttons(b) == GLFW_PRESS;
            if (pressed != debugButtons[b])
            {
                debugButtons[b] = pressed;
                System.out.println("[gamepad] button " + buttonNames[b] + (pressed ? " down" : " up"));
            }
        }

        for (int a = 0; a < axisNames.length; a++)
        {
            // Log in steps of 0.25 so the output stays readable
            int bucket = Math.round(state.axes(a) * 4);
            if (bucket != debugAxes[a])
            {
                debugAxes[a] = bucket;
                System.out.println("[gamepad] axis " + axisNames[a] + " = " + String.format("%.2f", state.axes(a)));
            }
        }
    }

    /** Applies a radial deadzone and rescales the remaining range to [0, 1] */
    protected static double[] deadzone(double x, double y)
    {
        double mag = Math.sqrt(x * x + y * y);

        if (mag < stickDeadzone)
            return new double[2];

        double scaled = Math.min(1, (mag - stickDeadzone) / (1 - stickDeadzone));
        return new double[]{x / mag * scaled, y / mag * scaled};
    }
}
