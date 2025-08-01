package tanks.gui.screen;

import basewindow.BaseFile;
import tanks.*;
import tanks.bullet.Bullet;
import tanks.gui.*;
import tanks.item.Item;
import tanks.item.ItemBullet;
import tanks.registry.RegistryItem;
import tanks.tank.*;
import tanks.tankson.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ScreenEditorPlayerTankBuild<T extends TankPlayer> extends ScreenEditorTanksONable<T>
{
    public boolean writeTank(Tank t)
    {
        return this.writeTank(t, false);
    }

    public boolean writeTank(Tank t, boolean overwrite)
    {
        BaseFile f = Game.game.fileManager.getFile(Game.homedir + Game.buildDir + "/" + t.name.replace(" ", "_") + ".tanks");

        if (!f.exists() || overwrite)
        {
            try
            {
                if (!f.exists())
                    f.create();

                f.startWriting();
                f.println(t.toString());
                f.stopWriting();

                return true;
            }
            catch (IOException e)
            {
                Game.exitToCrash(e);
            }
        }

        return false;
    }

    public void writeTankAndShowConfirmation(TankPlayer t, boolean overwrite)
    {
        if (this.writeTank(t, overwrite))
            Game.screen = new ScreenTankSavedInfo(this, t, new ArrayList<>(), new ArrayList<>());
        else
            Game.screen = new ScreenTankSaveOverwrite(this, t);
    }

    public Button save = new Button(this.centerX + this.objXSpace, this.centerY + this.objYSpace * 6.5, this.objWidth, this.objHeight, "Save to template", () ->
    {
        TankPlayer t = target.get();
        this.writeTankAndShowConfirmation(t, false);
    }
    );

    public Button dismissMessage = new Button(Drawing.drawing.interfaceSizeX / 2, Drawing.drawing.interfaceSizeY / 2 + Drawing.drawing.objHeight, Drawing.drawing.objWidth, Drawing.drawing.objHeight, "Ok", () -> message = null);

    @Override
    public void setupLayoutParameters()
    {
        this.interfaceScaleZoomOverride = 1;
        centerX = Drawing.drawing.baseInterfaceSizeX / 2;
        centerY = Drawing.drawing.baseInterfaceSizeY / 2;
        Drawing.drawing.interfaceSizeX = Drawing.drawing.baseInterfaceSizeX;
        Drawing.drawing.interfaceSizeY = Drawing.drawing.baseInterfaceSizeY;
    }

    public void resetLayout()
    {
        Drawing.drawing.interfaceScaleZoom = Drawing.drawing.interfaceScaleZoomDefault;
        Drawing.drawing.interfaceSizeX = Drawing.drawing.interfaceSizeX / Drawing.drawing.interfaceScaleZoom;
        Drawing.drawing.interfaceSizeY = Drawing.drawing.interfaceSizeY / Drawing.drawing.interfaceScaleZoom;
    }

    @Override
    public void setupTabs()
    {
        Tab general = new TabGeneral(this, "General", TankPropertyCategory.general);
        Tab appearance = new TabAppearance(this, "Appearance", TankPropertyCategory.appearanceGeneral);
        Tab movement = new TabTankBuild(this, "Movement", TankPropertyCategory.movementGeneral);
        Tab abilities = new TabAbilities(this, "Abilities", TankPropertyCategory.abilities);

        new TabPartPicker(this, appearance, "Emblem", TankPropertyCategory.appearanceEmblem, 4);
        new TabPartPicker(this, appearance, "Turret base", TankPropertyCategory.appearanceTurretBase, 3);
        new TabPartPicker(this, appearance, "Turret barrel", TankPropertyCategory.appearanceTurretBarrel, 2);
        new TabPartPicker(this, appearance, "Tank body", TankPropertyCategory.appearanceBody, 1);
        new TabPartPicker(this, appearance, "Tank treads", TankPropertyCategory.appearanceTreads, 2);
        new TabGlow(this, appearance, "Glow", TankPropertyCategory.appearanceGlow);
        new TabTankBuild(this, appearance, "Tracks", TankPropertyCategory.appearanceTracks);

        this.currentTab = general;
        this.iconPrefix = "tankeditor";

        this.delete.function = () ->
        {
            setTarget(null);

            this.quit.function.run();

            if (this.prevScreen instanceof ScreenEditorTanksONable<?> || this.prevScreen instanceof ScreenSelectorArraylist)
            {
                if (!target.nullable)
                    target.cast().set(new TankReference("dummy"));

                ScreenSelectorTankReference s = new ScreenSelectorTankReference(this.objName, this.target.cast(), this.prevScreen);
                s.onComplete = this.onComplete;
                Game.screen = s;
            }
        };

        if (!(this.prevScreen instanceof ScreenEditorTanksONable<?>))
            this.deleteText = "Delete";

        if (this.prevScreen instanceof ScreenSelectorArraylist)
        {
            this.deleteText = "Select tank to link";
            this.showDeleteObj = false;
        }
    }

    public ScreenEditorPlayerTankBuild(Pointer<T> t, Screen screen)
    {
        super(t, screen);

        this.allowClose = false;

        this.controlsMusic = true;

        this.title = "Edit %s";
        this.objName = "player build";
    }

    public class TabTankBuild extends Tab
    {
        public TabTankBuild(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
        }

        public TabTankBuild(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category)
        {
            super(screen, parent, name, category);
        }

        public void addFields()
        {
            for (Field f: this.screen.fields)
            {
                Property p = f.getAnnotation(Property.class);
                TankBuildProperty p1 = f.getAnnotation(TankBuildProperty.class);

                if (p1 != null && p != null && ((p1.category().equals("default") && p.category().equals(this.category)) || p1.category().equals(this.category)))
                    this.uiElements.add(screen.getUIElementForField(new FieldPointer<>(target.get(), f), p));
            }
        }
    }

    public class TabGeneral extends Tab
    {
        public TextBox description;

        public TabGeneral(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
        }

        public TabGeneral(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category)
        {
            super(screen, parent, name, category);
        }

        public void addFields()
        {
            for (Field f: this.screen.fields)
            {
                Property p = f.getAnnotation(Property.class);
                TankBuildProperty p1 = f.getAnnotation(TankBuildProperty.class);

                if (p1 != null && p != null && ((p1.category().equals("default") && p.category().equals(this.category)) || p1.category().equals(this.category)) &&
                !(target instanceof ArrayListIndexPointer && ((ArrayListIndexPointer<T>) target).getIndex() == 0 && p.miscType() == Property.MiscType.defaultBuildForbidden))
                {
                    if (p.miscType() == Property.MiscType.description)
                    {
                        TextBox t = (TextBox) screen.getUIElementForField(new FieldPointer<>(target.get(), f), p);
                        t.posX = this.screen.centerX;
                        t.posY = this.screen.centerY + 270;
                        t.enableCaps = true;
                        t.allowSpaces = true;
                        t.enableSpaces = true;
                        t.enablePunctuation = true;
                        t.maxChars = 100;
                        t.sizeX *= 3;
                        this.description = t;
                    }
                    else
                        this.uiElements.add(screen.getUIElementForField(new FieldPointer<>(target.get(), f), p));
                }
            }
        }

        @Override
        public void updateUIElements()
        {
            super.updateUIElements();

            if (this.description != null)
                this.description.update();
        }

        @Override
        public void drawUIElements()
        {
            super.drawUIElements();

            if (this.description != null)
                this.description.draw();
        }
    }

    public class TabWithPreview extends TabTankBuild
    {
        public TankPlayer preview;

        public TabWithPreview(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category)
        {
            super(screen, parent, name, category);
        }

        public TabWithPreview(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
        }

        public void set()
        {
            this.preview = new TankPlayer(Drawing.drawing.sizeX / 2, Drawing.drawing.sizeY / 2 - 315 * Drawing.drawing.interfaceScaleZoom, 0);

            this.preview.posX = this.screen.centerX;
            this.preview.posY = this.screen.centerY - 30;
        }

        @Override
        public void sortUIElements()
        {
            while (this.uiElements.size() < 12)
                this.uiElements.add(new EmptySpace());

            super.sortUIElements();
        }

        @Override
        public void updateUIElements()
        {
            super.updateUIElements();
        }

        @Override
        public void drawUIElements()
        {
            TankPlayer tank = this.screen.target.get();
            this.preview.baseModel = tank.baseModel;
            this.preview.colorModel = tank.colorModel;
            this.preview.turretBaseModel = tank.turretBaseModel;
            this.preview.turretModel = tank.turretModel;
            this.preview.size = tank.size;
            this.preview.turretSize = tank.turretSize;
            this.preview.turretLength = tank.turretLength;
            this.preview.emblem = tank.emblem;
            this.preview.color.set(tank.color);
            this.preview.secondaryColor.set(tank.secondaryColor);
            this.preview.tertiaryColor.set(tank.tertiaryColor);
            this.preview.emblemColor.set(tank.emblemColor);
            this.preview.luminance = tank.luminance;
            this.preview.glowIntensity = tank.glowIntensity;
            this.preview.glowSize = tank.glowSize;
            this.preview.lightSize = tank.lightSize;
            this.preview.lightIntensity = tank.lightIntensity;
            this.preview.multipleTurrets = tank.multipleTurrets;

            if (this.preview.size > Game.tile_size * 2)
                this.preview.size = Game.tile_size * 2;

            this.preview.enableTertiaryColor = true;

            this.preview.size *= 2;
            this.preview.invulnerable = true;
            this.preview.drawAge = 50;
            this.preview.depthTest = false;

            this.preview.drawTank(true, Game.enable3d);

            super.drawUIElements();
        }
    }

    public class TabGlow extends TabWithPreview
    {
        public TabGlow(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category)
        {
            super(screen, parent, name, category);
        }

        public TabGlow(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
        }

        public void set()
        {
            super.set();
            preview.posX = screen.centerX + screen.objXSpace / 2;
            preview.posY += 60;
        }
    }

    public class TabAppearance extends TabWithPreview
    {
        double margin = screen.centerX - screen.objXSpace / 2 - screen.objWidth / 2 + 30;
        double margin2 = screen.centerX - screen.objXSpace / 2;
        double margin3 = screen.centerX + screen.objXSpace / 2;

        double space = 60;

        public TabAppearance(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category)
        {
            super(screen, parent, name, category);
        }

        public TabAppearance(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
        }

        @Override
        public void sortUIElements()
        {
            int in = 0;
            for (Tab t: this.subMenus)
            {
                this.uiElements.add(in, new Button(0, 0, 350, 40, t.name, () -> screen.setTab(t)));
                in++;
            }

            for (int i = 0; i < this.uiElements.size(); i++)
            {
                if (i < in)
                    this.uiElements.get(i).setPosition(margin2, Drawing.drawing.interfaceSizeY / 2 + yoffset + i * 60);
                else
                    this.uiElements.get(i).setPosition(margin3, Drawing.drawing.interfaceSizeY / 2 + yoffset + (i - in + 3) * 90);
            }

            for (Tab t: this.subMenus)
            {
                t.sortUIElements();
            }
        }

        @Override
        public void set()
        {
            super.set();
            this.preview.posX = margin3;
            this.preview.posY = Drawing.drawing.interfaceSizeY / 2 + yoffset + 90;
        }

        @Override
        public void drawUIElements()
        {
            super.drawUIElements();

            TankPlayer tank = screen.target.get();

            double s = Game.tile_size * 0.8;

            if (tank.emblem != null)
            {
                Drawing.drawing.setColor(tank.emblemColor);
                Drawing.drawing.drawInterfaceImage(tank.emblem, margin, screen.centerY + 60 - space * 3, s, s);
            }

            double offmul = 1;

            if (Game.framework == Game.Framework.libgdx)
                offmul = 0;

            Drawing.drawing.setColor(tank.tertiaryColor, 255, 0.5);
            Drawing.drawing.drawInterfaceModel2D(tank.turretBaseModel, margin, screen.centerY + 60 - space * 2 + 4 * offmul, 0, s, s, s);

            Drawing.drawing.setColor(tank.secondaryColor, 255, 0.5);
            Drawing.drawing.drawInterfaceModel2D(tank.turretModel, margin, screen.centerY + 60 - space * 1, 0, s, s, s);

            Drawing.drawing.setColor(tank.color, 255, 0.5);
            Drawing.drawing.drawInterfaceModel2D(tank.colorModel, margin, screen.centerY + 60 - space * 0 + 7 * offmul, 0, s, s, s);

            Drawing.drawing.setColor(tank.secondaryColor, 255, 0.5);
            Drawing.drawing.drawInterfaceModel2D(tank.baseModel, margin, screen.centerY + 60 + space * 1 + 7 * offmul, 0, s, s, s);

            Drawing.drawing.setColor(80, 80, 80);
            Drawing.drawing.fillInterfaceOval(margin, screen.centerY + 60 + space * 2, s * 1.5, s * 1.5);
            Drawing.drawing.setColor(tank.secondaryColor.red * preview.glowIntensity, tank.secondaryColor.green * preview.glowIntensity, tank.secondaryColor.blue * preview.glowIntensity, 255, 1);
            Drawing.drawing.fillInterfaceGlow(margin, screen.centerY + 60 + space * 2, s * 1.5 * preview.glowSize / 4, s * 1.5 * preview.glowSize / 4);
            Drawing.drawing.setColor(255, 255, 255, 255 * preview.lightIntensity, 1);
            Drawing.drawing.fillInterfaceGlow(margin, screen.centerY + 60 + space * 2, s * 1.5 * preview.lightSize / 4, s * 1.5 * preview.lightSize / 4, false, true);

            Drawing.drawing.setColor(0, 0, 0, 64);
            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 2; j++)
                {
                    Drawing.drawing.fillInterfaceRect(margin + (i - 1) * s * tank.trackSpacing, screen.centerY + 60 + space * 3 + (j - 0.5) * s * 0.6, s / 5, s / 5);
                }
            }
        }
    }

    public class TabPartPicker extends TabWithPreview
    {
        public int colorIndex;
        public SelectorColor colorPicker;

        String enableColorText = "Override color: ";
        Button enableColor = new Button(0, 0, this.screen.objWidth, this.screen.objHeight, "", new Runnable()
        {
            @Override
            public void run()
            {
                boolean enable = false;
                TankPlayer tank = screen.target.get();

                if (colorIndex == 1)
                {
                    tank.overridePrimaryColor = !tank.overridePrimaryColor;
                    enable = tank.overridePrimaryColor;
                }
                else if (colorIndex == 2)
                {
                    tank.overrideSecondaryColor = !tank.overrideSecondaryColor;
                    enable = tank.overrideSecondaryColor;
                }
                else if (colorIndex == 3)
                {
                    tank.overrideTertiaryColor = !tank.overrideTertiaryColor;
                    enable = tank.overrideTertiaryColor;
                }
                else if (colorIndex == 4)
                {
                    tank.overrideEmblemColor = !tank.overrideEmblemColor;
                    enable = tank.overrideEmblemColor;
                }

                setColorText(enable);
            }
        },
                "Overrides a player's selection of color---for this part with a specified color.");

        Button autoColor = new Button(0, 0, this.screen.objWidth, this.screen.objHeight, "Auto-calculate color", () ->
        {
            TankPlayer tank = screen.target.get();

            if (colorIndex == 2)
                Turret.setSecondary(tank.color, tank.secondaryColor);
            else if (colorIndex == 3)
                Turret.setTertiary(tank.color, tank.secondaryColor, tank.tertiaryColor);
            else if (colorIndex == 4)
            {
                if (tank.overrideSecondaryColor)
                    tank.emblemColor.set(tank.secondaryColor);
                else
                    Turret.setSecondary(tank.color, tank.emblemColor);
            }

            setColorTextboxes();
        }, "Sets the color automatically based on---the way default tank color schemes are---calculated");

        public void setColorText(boolean enable)
        {
            if (enable)
                enableColor.setText(enableColorText, ScreenOptions.onText);
            else
                enableColor.setText(enableColorText, ScreenOptions.offText);
        }

        public TabPartPicker(ScreenEditorPlayerTankBuild screen, Tab parent, String name, String category, int colorIndex)
        {
            super(screen, parent, name, category);
            this.colorIndex = colorIndex;
        }

        public TabPartPicker(ScreenEditorPlayerTankBuild screen, String name, String category, int colorIndex)
        {
            this(screen, null, name, category, colorIndex);
        }

        @Override
        public void set()
        {
            super.set();

            this.setColorTextboxes();
        }

        public void setColorTextboxes()
        {
            TankPlayer tank = screen.target.get();

            if (colorIndex == 1)
                this.setColorText(tank.overridePrimaryColor);
            else if (colorIndex == 2)
                this.setColorText(tank.overrideSecondaryColor);
            else if (colorIndex == 3)
                this.setColorText(tank.overrideTertiaryColor);
            else if (colorIndex == 4)
                this.setColorText(tank.overrideEmblemColor);

            this.colorPicker.updateColors();
        }

        @Override
        public void sortUIElements()
        {
            while (this.uiElements.size() < 8)
                this.uiElements.add(new EmptySpace());

            this.uiElements.add(enableColor);
            this.uiElements.add(colorPicker);

            super.sortUIElements();

            this.uiElements.remove(enableColor);
            this.uiElements.remove(colorPicker);

            autoColor.posX = colorPicker.colorBlue.posX - this.screen.objXSpace;
            autoColor.posY = colorPicker.colorBlue.posY;
        }

        @Override
        public void updateUIElements()
        {
            super.updateUIElements();
            TankPlayer tank = screen.target.get();

            if (!tank.overridePrimaryColor)
                tank.color.set(TankPlayer.default_primary_color);

            if (!tank.overrideSecondaryColor)
                tank.secondaryColor.set(TankPlayer.default_secondary_color);

            if (!tank.overrideTertiaryColor)
                tank.tertiaryColor.set(TankPlayer.default_tertiary_color);

            if (!tank.overrideEmblemColor)
                tank.emblemColor.set(TankPlayer.default_secondary_color);

            if ((this.colorIndex == 1 && tank.overridePrimaryColor) || (this.colorIndex == 2 && tank.overrideSecondaryColor) ||  (this.colorIndex == 3 && tank.overrideTertiaryColor) || (this.colorIndex == 4 && tank.emblem != null && tank.overrideEmblemColor))
            {
                this.colorPicker.update();

                if ((this.colorIndex == 2 && tank.overridePrimaryColor) ||
                        (this.colorIndex == 3 && tank.overridePrimaryColor && tank.overrideSecondaryColor) ||
                        (this.colorIndex == 4 && (tank.overrideSecondaryColor || tank.overridePrimaryColor)))
                    autoColor.update();
            }

            if (!(this.colorIndex == 4 && tank.emblem == null))
                this.enableColor.update();
        }

        @Override
        public void drawUIElements()
        {
            TankPlayer tank = screen.target.get();

            if (!(this.colorIndex == 4 && tank.emblem == null))
                this.enableColor.draw();

            if (this.colorIndex == 2)
            {
                Drawing.drawing.setInterfaceFontSize(24);
                if (Level.isDark())
                    Drawing.drawing.setColor(255, 255, 255);
                else
                    Drawing.drawing.setColor(0, 0, 0);

                Drawing.drawing.displayInterfaceText(this.screen.centerX, this.screen.centerY + 200, "Note: Tank treads and turret barrel share colors.");
                Drawing.drawing.displayInterfaceText(this.screen.centerX, this.screen.centerY + 230, "Tanks on a team with a color will use that color for their treads.");
            }

            super.drawUIElements();

            if ((this.colorIndex == 1 && tank.overridePrimaryColor) || (this.colorIndex == 2 && tank.overrideSecondaryColor) ||  (this.colorIndex == 3 && tank.overrideTertiaryColor) || (this.colorIndex == 4 && tank.emblem != null && tank.overrideEmblemColor))
            {
                this.colorPicker.draw();

                if ((this.colorIndex == 2 && tank.overridePrimaryColor) ||
                        (this.colorIndex == 3 && tank.overridePrimaryColor && tank.overrideSecondaryColor) ||
                        (this.colorIndex == 4 && (tank.overrideSecondaryColor || tank.overridePrimaryColor)))
                    autoColor.draw();
            }
        }

        @Override
        public void addFields()
        {
            this.uiElements.clear();
            for (Field f : this.screen.fields)
            {
                Property p = f.getAnnotation(Property.class);
                if (p != null && p.category().equals(this.category))
                {
                    ITrigger t = screen.getUIElementForField(new FieldPointer<>(target.get(), f), p);

                    if (p.miscType() != Property.MiscType.colorRGB)
                    {
                        this.uiElements.add(t);
                        for (int i = 1; i < t.getSize(); i++)
                        {
                            this.uiElements.add(new EmptySpace());
                        }
                    }
                    else if (t instanceof SelectorColor)
                        this.colorPicker = (SelectorColor) t;
                }
            }

            try
            {
                if (this.colorIndex == 2 && this.colorPicker == null)
                {
                    Field f = Tank.class.getField("secondaryColor");
                    this.colorPicker = (SelectorColor) screen.getUIElementForField(new FieldPointer<>(target.get(), f), f.getAnnotation(Property.class));
                }
            }
            catch (Exception e)
            {
                Game.exitToCrash(e);
            }

            if (this.colorPicker == null)
                throw new RuntimeException("lmao no color picker " + this.name);
        }
    }

    public class TabAbilities extends TabTankBuild
    {
        public ArrayList<Button> deleteButtons = new ArrayList<>();
        public ArrayList<Button> upButtons = new ArrayList<>();
        public ArrayList<Button> downButtons = new ArrayList<>();

        public Selector itemSelector;

        public Button create = new Button(screen.centerX, -1000, 60, 60, "+", () ->
        {
            itemSelector.setScreen();
        });

        public TabAbilities(ScreenEditorPlayerTankBuild screen, String name, String category)
        {
            super(screen, name, category);
            this.rows += 1;

            String[] itemNames = new String[Game.registryItem.itemEntries.size()];
            String[] itemImages = new String[Game.registryItem.itemEntries.size()];

            for (int i = 0; i < Game.registryItem.itemEntries.size(); i++)
            {
                RegistryItem.ItemEntry r = Game.registryItem.getEntry(i);
                itemNames[i] = r.name;
                itemImages[i] = r.image;
            }

            itemSelector = new Selector(0, 0, 0, 0, "item type", itemNames, () ->
            {
                Consumer<Item.ItemStack<?>> addItem = (Item.ItemStack<?> i) ->
                {
                    target.get().abilities.add(i);

                    ScreenEditorItem s = new ScreenEditorItem(new ArrayListIndexPointer<>(target.get().abilities, target.get().abilities.size() - 1), screen);
                    s.onComplete = () ->
                    {
                        uiElements.clear();
                        addFields();
                        sortUIElements();
                    };
                    s.showLoadFromTemplate = true;
                    Game.screen = s;
                };

                Game.screen = new ScreenAddSavedItem(screen, addItem, Game.formatString(itemSelector.options[itemSelector.selectedOption]), Game.registryItem.getEntry(itemSelector.selectedOption).item);
            });

            itemSelector.images = itemImages;
            itemSelector.quick = true;
        }

        @Override
        public void addFields()
        {
            this.deleteButtons.clear();
            this.upButtons.clear();
            this.downButtons.clear();

            TankPlayer t = target.get();
            for (int i = 0; i < t.abilities.size(); i++)
            {
                int j = i;
                Property p = new Property()
                {
                    @Override public Class<? extends Annotation> annotationType() { return Property.class; }
                    @Override public String id() { return "ability_" + (j + 1); }
                    @Override public String name() { return "Ability " + (j + 1); }
                    @Override public String desc() { return ""; }
                    @Override public String category() { return ""; }
                    @Override public MiscType miscType() { return MiscType.none; }
                    @Override public boolean nullable() { return false; }
                    @Override public double minValue() { return 0; }
                    @Override public double maxValue() { return 0; }
                };
                SelectorDrawable s = (SelectorDrawable) getUIElementForField(new ArrayListIndexPointer<>(t.abilities, i), p);
                s.sizeX *= 1.5;
                s.imageXOffset = - s.sizeX / 2 + s.sizeY / 2;
                this.uiElements.add(s);

                Button delete = new Button(-1000, -1000, 60, 60, "x", () ->
                {
                    t.abilities.remove(j);
                    uiElements.clear();
                    addFields();
                    sortUIElements();
                });

                delete.textOffsetY = -2.5;

                delete.bgColR = 160;
                delete.bgColG = 160;
                delete.bgColB = 160;

                delete.selectedColR = 255;
                delete.selectedColG = 0;
                delete.selectedColB = 0;

                delete.textColR = 255;
                delete.textColG = 255;
                delete.textColB = 255;

                deleteButtons.add(delete);

                Button up = new Button(-1000, -1000, 60, 60, "", () ->
                {
                    t.abilities.add(j - 1, t.abilities.remove(j));
                    uiElements.clear();
                    addFields();
                    sortUIElements();
                });

                up.imageSizeX = 30;
                up.imageSizeY = 30;
                up.image = "icons/arrow_up.png";

                Button down = new Button(-1000, -1000, 60, 60, "", () ->
                {
                    t.abilities.add(j, t.abilities.remove(j + 1));
                    uiElements.clear();
                    addFields();
                    sortUIElements();
                });

                down.imageSizeX = 30;
                down.imageSizeY = 30;
                down.image = "icons/arrow_down.png";

                upButtons.add(up);
                downButtons.add(down);

            }

            if (t.abilities.size() < TankPlayer.max_abilities)
                this.uiElements.add(create);
        }

        public void update()
        {
            super.update();

            for (int i = 0; i < this.deleteButtons.size(); i++)
            {
                Button b = this.deleteButtons.get(i);
                b.update();

                if (this.upButtons.size() <= i)
                    break;

                Button up = this.upButtons.get(i);
                Button down = this.downButtons.get(i);

                up.enabled = i > 0;
                down.enabled = i < target.get().abilities.size() - 1;
                up.update();
                down.update();
            }
        }

        public void draw()
        {
            super.draw();
            for (int i = 0; i < this.deleteButtons.size(); i++)
            {
                SelectorDrawable d = ((SelectorDrawable) this.uiElements.get(i));

                Button b = this.deleteButtons.get(i);
                b.posX = d.posX - screen.objXSpace * 0.85;
                b.posY = d.posY - screen.objHeight / 4;
                b.draw();

                Button up = this.upButtons.get(i);
                up.posX = d.posX + screen.objXSpace * 0.85;
                up.posY = d.posY - screen.objHeight / 4;
                up.draw();

                Button down = this.downButtons.get(i);
                down.posX = up.posX + screen.objYSpace * 1.25;
                down.posY = d.posY - screen.objHeight / 4;
                down.draw();
            }
        }
    }

    @Override
    public void addMusicTracks()
    {
        if (this.target.get() != null && (Game.screen == this || Game.screen instanceof ScreenSelectorArraylist || Game.screen instanceof ScreenSelectorTankReference))
            this.musics.addAll(this.target.get().musicTracks);
    }

    @Override
    public void updateOverlay()
    {
        if (!this.target.nullable && !(target instanceof ArrayListIndexPointer && ((ArrayListIndexPointer<T>) target).getIndex() == 0))
            this.delete.update();

        this.save.update();
    }

    @Override
    public void drawOverlay()
    {
        if (!this.target.nullable && !(target instanceof ArrayListIndexPointer && ((ArrayListIndexPointer<T>) target).getIndex() == 0))
            this.delete.draw();

        this.save.draw();
    }
}

