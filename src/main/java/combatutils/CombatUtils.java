package combatutils;

import combatutils.modules.AimAssist;
import combatutils.modules.JumpReset;
import combatutils.modules.WTap;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatUtils extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Combat-Utils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Combat-Utils");

        Modules.get().add(new AimAssist());
        Modules.get().add(new JumpReset());
        Modules.get().add(new WTap());
        
        LOG.info("Combat-Utils initialized with {} modules", 3);
    }

    @Override
    public void onRegisterCategories() {
    }

    @Override
    public String getPackage() {
        return "combatutils";
    }
}
