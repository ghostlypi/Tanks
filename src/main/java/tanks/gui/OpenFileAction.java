package tanks.gui;

import tanks.Game;
import tanks.gui.ScreenElement.Notification;

/**
 * A notification action that opens a system directory or file in the default file manager.
 */
public class OpenFileAction implements NotificationAction
{
    private final String path;

    /**
     * Constructs an OpenFileAction with the specified target path.
     *
     * @param path The absolute path to open in the system file manager.
     */
    public OpenFileAction(String path)
    {
        this.path = path;
    }

    @Override
    public boolean onClick(Notification notification)
    {
        try
        {
            Game.game.fileManager.openFileManager(this.path);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return true;
    }
}
