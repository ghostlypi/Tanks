package tanks.gui.screen;

import tanks.Drawing;
import tanks.Game;
import tanks.Panel;
import tanks.gui.*;
import tanks.tank.Tank;

public class ScreenTestDebug extends Screen
{
    Button keyboardTest = new Button(this.centerX - this.objXSpace / 2, this.centerY - this.objYSpace, this.objWidth, this.objHeight, "Test keyboard",
        () -> Game.screen = new ScreenTestKeyboard());

    Button textboxTest = new Button(this.centerX - this.objXSpace / 2, this.centerY, this.objWidth, this.objHeight, "Test text boxes",
        () -> Game.screen = new ScreenTestTextbox());

    Button modelTest = new Button(this.centerX - this.objXSpace / 2, this.centerY + this.objYSpace, this.objWidth, this.objHeight, "Test models",
        () -> Game.screen = new ScreenTestModel(Tank.health_model));

    Button fontTest = new Button(this.centerX + this.objXSpace / 2, this.centerY - this.objYSpace, this.objWidth, this.objHeight, "Test fonts",
        () -> Game.screen = new ScreenTestFonts());

    Button fireworks = new Button(this.centerX, this.centerY + this.objYSpace * 2, this.objWidth, this.objHeight, "Test fireworks",
        () -> Game.screen = new ScreenTestFireworks());

    Button shapeTest = new Button(this.centerX + this.objXSpace / 2, this.centerY, this.objWidth, this.objHeight, "Test shapes",
        () -> Game.screen = new ScreenTestShapes());

    Button rainbowTest = new Button(this.centerX + this.objXSpace / 2, this.centerY + this.objYSpace, this.objWidth, this.objHeight, "Test rainbow",
        () -> Game.screen = new ScreenTestRainbow());

    Button notificationsTest = new Button(this.centerX, this.centerY - this.objYSpace * 2, this.objWidth, this.objHeight, "Test notifications", () ->
    {
        Panel.notifications.add(new ScreenElement.Notification(
            "Click to navigate to Window Options!",
            1000,
            300,
            new NavigateScreenAction(() -> new ScreenOptionsWindow())
        ));

        Panel.notifications.add(new ScreenElement.Notification(
            "Click to open the Tanks GitHub repo!",
            1200,
            300,
            new OpenLinkAction("https://github.com/aehmttw/Tanks")
        ));

        Panel.notifications.add(new ScreenElement.Notification(
            "Click to download a test asset!",
            1400,
            300,
            new DownloadAssetAction(
                "https://raw.githubusercontent.com/aehmttw/Tanks/master/README.md",
                Game.homedir + Game.directoryPath + "/test_downloaded_asset.txt",
                "test asset"
            )
        ));

        Panel.notifications.add(new ScreenElement.Notification(
            "This is a standard, non-clickable notification!",
            1600,
            300
        ));
    });

    Button back = new Button(this.centerX, this.centerY + this.objYSpace * 3.5, this.objWidth, this.objHeight, "Back", () -> Game.screen = new ScreenDebug());

    public ScreenTestDebug()
    {
        this.music = "menu_options.ogg";
        this.musicID = "menu";
    }

    @Override
    public void update()
    {
        keyboardTest.update();
        textboxTest.update();
        modelTest.update();
        fontTest.update();
        shapeTest.update();
        fireworks.update();
        rainbowTest.update();
        notificationsTest.update();

        back.update();
    }

    @Override
    public void draw()
    {
        this.drawDefaultBackground();

        Drawing.drawing.setInterfaceFontSize(this.titleSize);
        Drawing.drawing.setColor(0, 0, 0);
        Drawing.drawing.displayInterfaceText(this.centerX, this.centerY - 210, "Test stuff");

        modelTest.draw();
        keyboardTest.draw();
        textboxTest.draw();
        fontTest.draw();
        fireworks.draw();
        shapeTest.draw();
        rainbowTest.draw();
        notificationsTest.draw();

        back.draw();
    }
}
