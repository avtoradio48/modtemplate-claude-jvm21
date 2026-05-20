package {{ package }};

/**
 * Generated constants. Values are filled in by Blossom at build time from
 * gradle.properties — do not edit them here.
 */
public final class Reference {
    private Reference() {}

    public static final String MOD_ID = "{{ mod_id }}";
    public static final String MOD_NAME = "{{ mod_name }}";
    public static final String VERSION = "{{ mod_version }}";

    public static final String DEPENDENCIES = "required-after:forestry";

    public static final String FORESTRY_MOD_ID = "forestry";
    public static final String GENDUSTRY_MOD_ID = "gendustry";
    public static final String BINNIE_GENETICS_MOD_ID = "genetics";
    public static final String EXTRATREES_MOD_ID = "extratrees";

    public static final String PROXY_CLIENT = "com.dbudnik.arboriculturemill.proxy.ClientProxy";
    public static final String PROXY_COMMON = "com.dbudnik.arboriculturemill.proxy.CommonProxy";

    public static final int GUI_ID_MILL = 1;
}
