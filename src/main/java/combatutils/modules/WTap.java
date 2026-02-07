package combatutils.modules;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class WTap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgConditions = settings.createGroup("Conditions");

    // General Settings
    private final Setting<Integer> chance = sgGeneral.add(new IntSetting.Builder()
        .name("chance")
        .description("The chance of activating WTap when possible or beneficial.")
        .defaultValue(100)
        .min(0)
        .max(100)
        .sliderMax(100)
        .build()
    );

    private final Setting<WTapMode> mode = sgGeneral.add(new EnumSetting.Builder<WTapMode>()
        .name("mode")
        .description("How to reset sprint.")
        .defaultValue(WTapMode.Release)
        .build()
    );

    // Timing Settings
    private final Setting<Integer> releaseDelay = sgTiming.add(new IntSetting.Builder()
        .name("release-delay")
        .description("Delay before releasing W after hitting a target (in milliseconds).")
        .defaultValue(0)
        .min(0)
        .max(200)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> repressDelay = sgTiming.add(new IntSetting.Builder()
        .name("repress-delay")
        .description("Delay after releasing W before pressing it again (in milliseconds).")
        .defaultValue(50)
        .min(20)
        .max(300)
        .sliderMax(300)
        .build()
    );

    private final Setting<Integer> randomization = sgTiming.add(new IntSetting.Builder()
        .name("randomization")
        .description("Add random variance to delays (in milliseconds).")
        .defaultValue(20)
        .min(0)
        .max(100)
        .sliderMax(100)
        .build()
    );

    // Conditions Settings
    private final Setting<Boolean> selectHits = sgConditions.add(new BoolSetting.Builder()
        .name("select-hits")
        .description("Activates WTap only when the target is vulnerable (helps close distance and prevent being combo'd).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyWhenSprinting = sgConditions.add(new BoolSetting.Builder()
        .name("only-when-sprinting")
        .description("Only activate when you are sprinting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyOnCooldown = sgConditions.add(new BoolSetting.Builder()
        .name("only-on-cooldown")
        .description("Only activate when attack cooldown is ready (1.9+ combat).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> minCooldown = sgConditions.add(new DoubleSetting.Builder()
        .name("min-cooldown")
        .description("Minimum attack cooldown percentage to activate.")
        .defaultValue(0.9)
        .min(0.5)
        .max(1.0)
        .sliderMax(1.0)
        .visible(onlyOnCooldown::get)
        .build()
    );

    private final Setting<Boolean> pauseWhileEating = sgConditions.add(new BoolSetting.Builder()
        .name("pause-while-eating")
        .description("Pause WTap while eating or drinking.")
        .defaultValue(true)
        .build()
    );

    // State tracking
    private boolean isReleasing = false;
    private boolean isRepressing = false;
    private long releaseStartTime = 0;
    private long repressStartTime = 0;
    private Entity lastTarget = null;
    private long lastHitTime = 0;

    public WTap() {
        super(Categories.Combat, "w-tap", "Automates the w-tapping PVP strategy, useful in 1v1 combat scenarios.");
    }

    @Override
    public void onDeactivate() {
        // Reset movement key when disabled
        if (mc.player != null && mc.options != null) {
            mc.options.forwardKey.setPressed(false);
        }
        isReleasing = false;
        isRepressing = false;
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;

        Entity target = event.entity;
        if (!(target instanceof LivingEntity)) return;

        // Check conditions
        if (onlyWhenSprinting.get() && !mc.player.isSprinting()) return;
        if (pauseWhileEating.get() && mc.player.isUsingItem()) return;
        
        // Check cooldown
        if (onlyOnCooldown.get()) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown < minCooldown.get()) return;
        }

        // Check if target is vulnerable (select hits)
        if (selectHits.get()) {
            // Only w-tap if target just got hit or is in vulnerable state
            long timeSinceLastHit = System.currentTimeMillis() - lastHitTime;
            if (timeSinceLastHit < 500 && lastTarget == target) {
                // Skip - target was hit recently, might be in combo
                return;
            }
        }

        // Roll for chance
        if (Math.random() * 100 > chance.get()) return;

        // Update tracking
        lastTarget = target;
        lastHitTime = System.currentTimeMillis();

        // Trigger W-tap
        triggerWTap();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.options == null) return;

        long currentTime = System.currentTimeMillis();

        // Handle release phase
        if (isReleasing) {
            long elapsed = currentTime - releaseStartTime;
            int actualReleaseDelay = releaseDelay.get() + getRandomVariance();
            
            if (elapsed >= actualReleaseDelay) {
                // Start repress phase
                isReleasing = false;
                isRepressing = true;
                repressStartTime = currentTime;
            }
        }

        // Handle repress phase
        if (isRepressing) {
            long elapsed = currentTime - repressStartTime;
            int actualRepressDelay = repressDelay.get() + getRandomVariance();
            
            if (elapsed >= actualRepressDelay) {
                // Re-enable forward movement
                mc.options.forwardKey.setPressed(true);
                isRepressing = false;
            }
        }
    }

    private void triggerWTap() {
        if (mc.options == null) return;

        switch (mode.get()) {
            case Release -> {
                // Release W key
                mc.options.forwardKey.setPressed(false);
                isReleasing = true;
                releaseStartTime = System.currentTimeMillis();
            }
            case Sneak -> {
                // Press sneak to reset sprint
                mc.options.sneakKey.setPressed(true);
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                        mc.execute(() -> {
                            if (mc.options != null) {
                                mc.options.sneakKey.setPressed(false);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            case STap -> {
                // Press S briefly to reset sprint
                mc.options.backKey.setPressed(true);
                new Thread(() -> {
                    try {
                        Thread.sleep(30 + getRandomVariance());
                        mc.execute(() -> {
                            if (mc.options != null) {
                                mc.options.backKey.setPressed(false);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }

    private int getRandomVariance() {
        int variance = randomization.get();
        if (variance == 0) return 0;
        return (int) ((Math.random() * variance * 2) - variance);
    }

    public enum WTapMode {
        Release,  // Release and repress W key
        Sneak,    // Use sneak to reset sprint
        STap      // Temporary S tap to reset sprint
    }
}
