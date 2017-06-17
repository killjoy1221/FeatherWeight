package mnm.plugins.featherweight;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

@ConfigSerializable
public class Config {

    @Setting
    public Speed speed = new Speed();
    @Setting
    public Fly fly = new Fly();

    @ConfigSerializable
    public static class Speed {

        @Setting(comment = "Enables fast flying with an item")
        public boolean enabled = true;
        @Setting(comment = "When this item is held, fast flying will be on")
        public ItemType item = ItemTypes.FEATHER;
        @Setting(comment = "The max speed normal players are limited to. Admins can go above this.")
        public double max = 15D;
    }

    @ConfigSerializable
    public static class Fly {

        @Setting
        public boolean enabled = true;
        @Setting
        public boolean autofly = false;
    }
}
