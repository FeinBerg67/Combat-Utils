package combatutils.modules;

import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class JumpReset extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");

    // General Settings
    private final Setting<Integer> chance = sgGeneral.add(new IntSetting.Builder()
        .name("chance")
        .description("The percentage chance of the module activating to attempt a perfect jump when hit.")
        .defaultValue(100)
        .min(0)
        .max(100)
        .sliderMax(100)
        .build()
    );

    private final Setting<Integer> accuracy = sgGeneral.add(new IntSetting.Builder()
        .name("accuracy")
        .description("If a jump will be attempted, this determines if the jump will be accurately timed. Lower values may fail timing intentionally.")
        .defaultValue(100)
        .min(0)
        .max(100)
        .sliderMax(100)
        .build()
    );

    // Timing Settings
    private final Setting<Integer> perfectWindow = sgTiming.add(new IntSetting.Builder()
        .name("perfect-window")
        .description("Perfect jump timing window in milliseconds (lower = harder but more effective).")
        .defaultValue(75)
        .min(25)
        .max(150)
        .sliderMax(150)
        .build()
    );

    private final Setting<Integer> minDelay = sgTiming.add(new IntSetting.Builder()
        .name("min-delay")
        .description("Minimum delay before jump in milliseconds.")
        .defaultValue(0)
        .min(0)
        .max(100)
        .sliderMax(100)
        .build()
    );

    private final Setting<Integer> maxDelay = sgTiming.add(new IntSetting.Builder()
        .name("max-delay")
        .description("Maximum delay before jump in milliseconds.")
        .defaultValue(50)
        .min(0)
        .max(200)
        .sliderMax(200)
        .build()
    );

    // Targeting Settings
    private final Setting<Boolean> onlyWhenTargeting = sgTargeting.add(new BoolSetting.Builder()
        .name("only-when-targeting")
        .description("Activates the module only when the opponent hitting you is near your crosshair.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> targetingFov = sgTargeting.add(new DoubleSetting.Builder()
        .name("targeting-fov")
        .description("Field of view to consider the attacker as targeted.")
        .defaultValue(60.0)
        .min(10.0)
        .max(180.0)
        .sliderMax(180.0)
        .visible(onlyWhenTargeting::get)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgTargeting.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Only activate when you are on the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyFromPlayers = sgTargeting.add(new BoolSetting.Builder()
        .name("only-from-players")
        .description("Only activate when damaged by players.")
        .defaultValue(true)
        .build()
    );

    public JumpReset() {
        super(Categories.Combat, "jump-reset", "Abuses a mechanic in vanilla MC to reduce knockback by timing a jump before being hit.");
    }

    @EventHandler
    private void onDamage(DamageEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.entity != mc.player) return;

        // Check if only on ground
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        // Check if only from players
        Entity attacker = event.entity.getAttacker();
        if (onlyFromPlayers.get() && !(attacker instanceof PlayerEntity)) return;

        // Check if targeting attacker
        if (onlyWhenTargeting.get() && attacker != null) {
            double angle = getAngleTo(attacker);
            if (angle > targetingFov.get()) return;
        }

        // Roll for chance
        if (Math.random() * 100 > chance.get()) return;

        // Determine if we should accurately time the jump or intentionally fail
        boolean shouldBeAccurate = Math.random() * 100 <= accuracy.get();

        if (shouldBeAccurate) {
            // Calculate timing for perfect jump reset (within perfect window)
            int delay = (int) (Math.random() * perfectWindow.get());
            scheduleJump(delay);
        } else {
            // Intentionally mistimed jump (outside perfect window)
            int delay = minDelay.get() + (int) (Math.random() * (maxDelay.get() - minDelay.get()));
            // Add extra delay to make it intentionally late
            delay += perfectWindow.get() + (int) (Math.random() * 100);
            scheduleJump(delay);
        }
    }

    private void scheduleJump(int delayMs) {
        if (mc.player == null) return;

        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                
                // Execute jump on the main thread
                mc.execute(() -> {
                    if (mc.player != null && mc.player.isOnGround()) {
                        mc.player.jump();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private double getAngleTo(Entity entity) {
        if (mc.player == null) return 180.0;
        
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
        Vec3d direction = targetPos.subtract(eyePos).normalize();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        double dot = lookVec.dotProduct(direction);
        
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }
}
