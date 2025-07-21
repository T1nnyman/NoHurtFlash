package mod.tinnyman.nohurtflash;

public class ModCompat {
    public static final boolean GECKOLIB_LOADED;

    static {
        boolean loaded;
        try {
            Class.forName("software.bernie.geckolib.GeckoLib");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        GECKOLIB_LOADED = loaded;
    }
}
