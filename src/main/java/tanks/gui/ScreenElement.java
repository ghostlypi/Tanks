package tanks.gui;

import basewindow.InputCodes;
import tanks.*;

import java.util.ArrayList;

public abstract class ScreenElement
{
    public double duration;
    public double age = 0;

    public static class Notification extends ScreenElement
    {
        public ArrayList<String> text;
        public double sizeY;
        public double removeDuration = 100;
        public double width = 250;

        public NotificationAction action;
        public boolean isHovered = false;
        public boolean progressActive = false;
        public boolean addedToHistory = false;

        public String rawText;
        private boolean textChanged = false;

        public Notification(String text)
        {
            this(text, 1000, 250);
        }

        public Notification(String text, NotificationAction action)
        {
            this(text, 1000, 250);
            this.action = action;
        }

        public Notification(String text, double duration)
        {
            this(text, duration, 250);
        }

        public Notification(String text, double duration, NotificationAction action)
        {
            this(text, duration, 250);
            this.action = action;
        }

        public Notification(String text, double duration, double width)
        {
            Drawing.drawing.playSound("toast.ogg", 1, Game.soundVolume);
            this.rawText = text;
            this.text = Drawing.drawing.wrapText(text, width, 16);
            this.width = width;
            this.duration = duration;
            this.sizeY = Math.max(2, this.text.size() + 1) * 20;
        }

        public Notification(String text, double duration, double width, NotificationAction action)
        {
            this(text, duration, width);
            this.action = action;
        }

        public synchronized void setText(String text)
        {
            this.rawText = text;
            this.textChanged = true;
        }

        public double draw(double prevSY)
        {
            synchronized (this)
            {
                if (this.textChanged)
                {
                    this.text = Drawing.drawing.wrapText(this.rawText, this.width, 16);
                    this.sizeY = Math.max(2, this.text.size() + 1) * 20;
                    this.textChanged = false;
                }
            }

            if (!this.progressActive && !this.addedToHistory)
            {
                Panel.notificationHistory.add(this);
                this.addedToHistory = true;
            }

            if (!this.progressActive)
            {
                this.age += Panel.frameFrequency;
            }

            double mult = Math.sin(Math.min(1, this.age / 50.0) * Math.PI / 2);
            double addX = (1 - mult) * 400;
            double colA = mult * 255 * Math.min(1, 2.0 - this.age / (this.duration * 0.5));
            double x = Drawing.drawing.interfaceSizeX - 70 - this.width;
            double y = Drawing.drawing.interfaceSizeY - Drawing.drawing.statsHeight - sizeY - 80 - prevSY;

            double closeCenterX = x + this.width + 45 + addX;
            double closeCenterY = y + 20;
            double closeRadius = 10;

            boolean isHoveringClose = false;
            if (this.age < this.duration)
            {
                double mx = Drawing.drawing.getInterfaceMouseX();
                double my = Drawing.drawing.getInterfaceMouseY();
                isHoveringClose = (mx >= closeCenterX - closeRadius &&
                    mx <= closeCenterX + closeRadius &&
                    my >= closeCenterY - closeRadius &&
                    my <= closeCenterY + closeRadius);

                double px = x + addX;
                double py = y;
                double pw = this.width + 66;
                double ph = sizeY + 10;

                this.isHovered = (mx >= px && mx <= px + pw && my >= py && my <= py + ph);

                if (this.isHovered || isHoveringClose)
                {
                    if (Game.game.window.validPressedButtons.contains(InputCodes.MOUSE_BUTTON_1))
                    {
                        Game.game.window.validPressedButtons.remove((Integer) InputCodes.MOUSE_BUTTON_1);
                        Drawing.drawing.playSound("click.ogg", 1, Game.soundVolume);

                        if (isHoveringClose)
                        {
                            this.age = this.duration + this.removeDuration + 0.0001;
                            this.progressActive = false;
                            Panel.notificationHistory.remove(this);
                        }
                        else
                        {
                            if (this.action != null)
                            {
                                this.action.onClick(this);
                            }
                            this.age = this.duration + this.removeDuration + 0.0001;
                            this.progressActive = false;
                            Panel.notificationHistory.remove(this);
                        }
                    }
                }
            }
            else
            {
                this.isHovered = false;
            }

            double bg = Level.isDark() ? 0 : 255;
            double fg = Level.isDark() ? 255 : 0;
            double bgAlpha = colA / 2;

            if (this.isHovered)
            {
                bgAlpha = colA * 0.75;
            }

            Drawing.drawing.setColor(bg, bg, bg, bgAlpha);
            Drawing.drawing.drawConcentricPopup(x + this.width / 2 + 33 + addX, y + sizeY / 2, this.width + 65, sizeY + 10, 5, 27);

            if (this.isHovered)
            {
                Drawing.drawing.setColor(0, 150, 255, colA);
                Drawing.drawing.drawInterfaceRect(x + this.width / 2 + 33 + addX, y + sizeY / 2, this.width + 65, sizeY + 10, 3, 27);
            }

            Drawing.drawing.setInterfaceFontSize(16);

            Drawing.drawing.setColor(fg, fg, fg, colA);
            for (int i = 0; i < this.text.size(); i++)
            {
                double r = Game.game.window.colorR;
                double g = Game.game.window.colorG;
                double b = Game.game.window.colorB;
                Drawing.drawing.setColor(fg, fg, fg, colA);
                Drawing.drawing.drawUncenteredInterfaceText(x + 50 + addX, y + i * 20 + 12, String.format("\u00A7%03d%03d%03d255", (int) (r * 255), (int) (g * 255), (int) (b * 255)) + this.text.get(i));
            }

            // Draw close ('x') button
            if (isHoveringClose)
            {
                Drawing.drawing.setColor(255, 0, 0, colA);
            }
            else
            {
                Drawing.drawing.setColor(fg, fg, fg, colA * 0.3);
            }
            Drawing.drawing.fillInterfaceOval(closeCenterX, closeCenterY, closeRadius * 2, closeRadius * 2);

            Drawing.drawing.setColor(255, 255, 255, colA);
            Drawing.drawing.setInterfaceFontSize(12);
            Drawing.drawing.drawInterfaceText(closeCenterX, closeCenterY, "x");

            Drawing.drawing.setColor(0, 150, 255, colA);
            Drawing.drawing.fillInterfaceOval(x + 27 + addX, y + 20, 25, 25);

            Drawing.drawing.setColor(255, 255, 255, colA);
            Drawing.drawing.setInterfaceFontSize(16);
            Drawing.drawing.drawInterfaceText(x + 27 + addX, y + 20, "!");

            double deteriorationProgress = Math.max(this.age - this.duration, 0) / this.removeDuration;
            return (this.sizeY + 15) * (Math.sin(Math.PI * (0.5 - deteriorationProgress)) + 1) / 2;
        }
    }

    public static class CenterMessage extends ScreenElement
    {
        public boolean previous;
        public TextWithStyling styling;
        public double baseColorA = -1;

        public CenterMessage(String message, int duration, Object... objects)
        {
            message = String.format(message, objects);
            int brightness = Level.isDark() ? 255 : 0;
            this.styling = new TextWithStyling(message, brightness, brightness, brightness, 80 - Math.max(8, message.length() * 2));
            this.styling.colorA = 128;
            this.duration = duration;
            this.previous = Panel.currentMessage != null;
        }

        public void draw()
        {
            this.age += Panel.frameFrequency;
            this.styling.drawInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 - 200);

            if (this.age < 50 && !previous)
            {
                if (this.baseColorA < 0)
                    this.baseColorA = this.styling.colorA;

                this.styling.colorA = this.baseColorA * Math.min(1, this.age / 50);
            }
            else if (this.age > this.duration - 50)
                this.styling.colorA = this.baseColorA * Math.max(0, (this.duration - this.age) / 50);
            else
                this.baseColorA = this.styling.colorA;

            if (this.age > this.duration)
                Panel.currentMessage = null;
        }
    }
}
