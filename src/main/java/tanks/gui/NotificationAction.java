package tanks.gui;

import tanks.gui.ScreenElement.Notification;

/**
 * Interface representing an action that can be executed when a clickable notification is clicked.
 */
@FunctionalInterface
public interface NotificationAction
{
    /**
     * Called when the notification is clicked.
     *
     * @param notification The notification that was clicked.
     * @return true if the notification should be dismissed (fade out) after the click; false to keep it active.
     */
    boolean onClick(Notification notification);
}
