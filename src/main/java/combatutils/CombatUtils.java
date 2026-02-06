package combatutils;

import combatutils.modules.AimAssist;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatUtils extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Combat-Utils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Combat-Utils");

        // Register modules directly into Meteor's Combat category
        Modules.get().add(new AimAssist());
    }

    @Override
    public void onRegisterCategories() {
    }

    @Override
    public String getPackage() {
        return "combatutils";
    }
}
