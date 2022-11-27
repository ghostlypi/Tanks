package tanks.gui.screen;

import tanks.Drawing;
import tanks.Game;
import tanks.gui.Button;
import tanks.tank.TankMimic;
import tanks.tank.TankModels;

public class ScreenDebug extends Screen
{
    public String traceText = "Trace rays: ";
    public String firstPersonText = "First person: ";
    public String followingCamText = "Immersive camera: ";
    public String tankIDsText = "Show tank IDs: ";
    public String invulnerableText = "Invulnerable: ";

    public ScreenDebug()
    {
        this.music = "menu_options.ogg";
        this.musicID = "menu";

        if (Game.traceAllRays)
            traceAllRays.setText(traceText, ScreenOptions.onText);
        else
            traceAllRays.setText(traceText, ScreenOptions.offText);

        if (Game.firstPerson)
            firstPerson.setText(firstPersonText, ScreenOptions.onText);
        else
            firstPerson.setText(firstPersonText, ScreenOptions.offText);

        if (Game.followingCam)
            followingCam.setText(followingCamText, ScreenOptions.onText);
        else
            followingCam.setText(followingCamText, ScreenOptions.offText);

        if (Game.showTankIDs)
            tankIDs.setText(tankIDsText, ScreenOptions.onText);
        else
            tankIDs.setText(tankIDsText, ScreenOptions.offText);

        if (Game.invulnerable)
            invulnerable.setText(invulnerableText, ScreenOptions.onText);
        else
            invulnerable.setText(invulnerableText, ScreenOptions.offText);
    }

    Button back = new Button(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 + 210, this.objWidth, this.objHeight, "Back", () -> Game.screen = new ScreenTitle()
    );

    Button keyboardTest = new Button(Drawing.drawing.interfaceSizeX / 2 - this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 - this.objYSpace * 1.5, this.objWidth, this.objHeight, "Test keyboard", () -> Game.screen = new ScreenTestKeyboard()
    );

    Button textboxTest = new Button(Drawing.drawing.interfaceSizeX / 2 - this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 - this.objYSpace * 0.5, this.objWidth, this.objHeight, "Test text boxes", () -> Game.screen = new ScreenTestTextbox()
    );

    Button modelTest = new Button(Drawing.drawing.interfaceSizeX / 2 - this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 + this.objYSpace * 0.5, this.objWidth, this.objHeight, "Test models", () -> Game.screen = new ScreenTestModel(Drawing.drawing.createModel("/models/tankcamoflauge/base/"))
    );

    Button traceAllRays = new Button(Drawing.drawing.interfaceSizeX / 2 + this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 - this.objYSpace * 1.5, this.objWidth, this.objHeight, "", new Runnable()
    {
        @Override
        public void run()
        {
            Game.traceAllRays = !Game.traceAllRays;

            if (Game.traceAllRays)
                traceAllRays.setText(traceText, ScreenOptions.onText);
            else
                traceAllRays.setText(traceText, ScreenOptions.offText);
        }
    });

    Button firstPerson = new Button(Drawing.drawing.interfaceSizeX / 2 + this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 + this.objYSpace * 0.5, this.objWidth, this.objHeight, "", new Runnable()
    {
        @Override
        public void run()
        {
            Game.firstPerson = !Game.firstPerson;

            if (Game.firstPerson)
                firstPerson.setText(firstPersonText, ScreenOptions.onText);
            else
                firstPerson.setText(firstPersonText, ScreenOptions.offText);
        }
    });

    Button followingCam = new Button(Drawing.drawing.interfaceSizeX / 2 + this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 - this.objYSpace * 0.5, this.objWidth, this.objHeight, "", new Runnable()
    {
        @Override
        public void run()
        {
            Game.followingCam = !Game.followingCam;

            if (Game.followingCam)
                followingCam.setText(followingCamText, ScreenOptions.onText);
            else
                followingCam.setText(followingCamText, ScreenOptions.offText);
        }
    });

    Button tankIDs = new Button(Drawing.drawing.interfaceSizeX / 2 + this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 + this.objYSpace * 1.5, this.objWidth, this.objHeight, "", new Runnable()
    {
        @Override
        public void run()
        {
            Game.showTankIDs = !Game.showTankIDs;

            if (Game.showTankIDs)
                tankIDs.setText(tankIDsText, ScreenOptions.onText);
            else
                tankIDs.setText(tankIDsText, ScreenOptions.offText);
        }
    });

    Button invulnerable = new Button(Drawing.drawing.interfaceSizeX / 2 - this.objXSpace / 2, Drawing.drawing.interfaceSizeY / 2 + this.objYSpace * 1.5, this.objWidth, this.objHeight, "", new Runnable()
    {
        @Override
        public void run()
        {
            Game.invulnerable = !Game.invulnerable;

            if (Game.invulnerable)
                invulnerable.setText(invulnerableText, ScreenOptions.onText);
            else
                invulnerable.setText(invulnerableText, ScreenOptions.offText);
        }
    });

    @Override
    public void update()
    {
        keyboardTest.update();
        textboxTest.update();
        modelTest.update();
        traceAllRays.update();
        followingCam.update();
        firstPerson.update();
        invulnerable.update();
        tankIDs.update();
        back.update();
    }

    @Override
    public void draw()
    {
        this.drawDefaultBackground();
        Drawing.drawing.setInterfaceFontSize(this.titleSize);
        Drawing.drawing.setColor(0, 0, 0);
        Drawing.drawing.displayInterfaceText(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 - 210, "Debug menu");

        firstPerson.draw();
        followingCam.draw();
        modelTest.draw();
        keyboardTest.draw();
        textboxTest.draw();
        traceAllRays.draw();
        tankIDs.draw();
        invulnerable.draw();
        back.draw();
    }
}
