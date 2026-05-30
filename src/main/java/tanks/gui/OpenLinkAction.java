package tanks.gui;

import tanks.Game;
import tanks.gui.ScreenElement.Notification;

import java.net.URL;

/**
 * A notification action that opens a URL link in the user's web browser.
 */
public class OpenLinkAction implements NotificationAction
{
    private final String url;

    /**
     * Constructs an OpenLinkAction for the specified URL.
     *
     * @param url The URL link to open in the browser.
     */
    public OpenLinkAction(String url)
    {
        this.url = url;
    }

    @Override
    public boolean onClick(Notification notification)
    {
        try
        {
            Game.game.window.openLink(new URL(this.url));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return true;
    }
}
