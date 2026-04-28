package dlindustries.vigillant.system.module.setting;

import java.awt.Color;

public final class ColorSetting extends Setting {

    private Color value;

    public ColorSetting(String name, Color defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public Color getValue() {
        return value;
    }

    public void setValue(Color value) {
        this.value = value;
    }

    public int getRGB() {
        return value.getRGB();
    }
}