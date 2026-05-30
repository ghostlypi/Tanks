package tanks.gui;

import tanks.Game;
import tanks.gui.ScreenElement.Notification;
import tanks.gui.screen.Screen;

import java.util.function.Supplier;

/**
 * A notification action that navigates to a specific screen in the game.
 */
public class NavigateScreenAction implements NotificationAction
{
    private final Supplier<Screen> screenSupplier;

    /**
     * Constructs a NavigateScreenAction with a supplier for the target screen.
     *
     * @param screenSupplier A supplier that returns the target screen to navigate to.
     */
    public NavigateScreenAction(Supplier<Screen> screenSupplier)
    {
        this.screenSupplier = screenSupplier;
    }

    @Override
    public boolean onClick(Notification notification)
    {
        Screen targetScreen = this.screenSupplier.get();
        if (targetScreen != null)
        {
            Game.screen = targetScreen;
        }

        return true;
    }
}
