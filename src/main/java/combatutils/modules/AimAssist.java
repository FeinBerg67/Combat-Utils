package combatutils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgSmoothing = settings.createGroup("Smoothing");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgControl = settings.createGroup("Control");
    private final SettingGroup sgLegit = settings.createGroup("Legit");

    // General Settings  
    private final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()  
        .name("distance")  
        .description("Maximum distance to assist aim.")  
        .defaultValue(3.0)  
        .min(1.0)  
        .max(10.0)  
        .sliderMax(10.0)  
        .build()  
    );  

    private final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()  
        .name("fov")  
        .description("Field of view to assist aim within.")  
        .defaultValue(20.0)  
        .min(5.0)  
        .max(360.0)  
        .sliderMax(360.0)  
        .build()  
    );  

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()  
        .name("only-on-click")  
        .description("Only assist when holding attack button.")  
        .defaultValue(false)  
        .build()  
    );  

    private final Setting<Boolean> ignoreWalls = sgGeneral.add(new BoolSetting.Builder()  
        .name("ignore-walls")  
        .description("Assist aim through walls.")  
        .defaultValue(false)  
        .build()  
    );  

    // Speed Settings  
    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()  
        .name("horizontal-speed")  
        .description("Horizontal aim assistance speed.")  
        .defaultValue(0.15)  
        .min(0.01)  
        .max(2.0)  
        .sliderMax(2.0)  
        .build()  
    );  

    private final Setting<Double> verticalSpeed = sgSpeed.add(new DoubleSetting.Builder()  
        .name("vertical-speed")  
        .description("Vertical aim assistance speed.")  
        .defaultValue(0.12)  
        .min(0.01)  
        .max(2.0)  
        .sliderMax(2.0)  
        .build()  
    );  

    // Smoothing Settings  
    private final Setting<SmoothingMode> smoothingMode = sgSmoothing.add(new EnumSetting.Builder<SmoothingMode>()  
        .name("smoothing-mode")  
        .description("Smoothing algorithm.")  
        .defaultValue(SmoothingMode.Exponential)  
        .build()  
    );  

    private final Setting<Double> smoothness = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("smoothness")  
        .description("How smooth the aim is (higher = smoother but slower). Set to 0 to disable smoothing.")  
        .defaultValue(15.0)  
        .min(0.0)  
        .max(50.0)  
        .sliderMax(50.0)  
        .build()  
    );  

    private final Setting<Boolean> accelerate = sgSmoothing.add(new BoolSetting.Builder()  
        .name("accelerate")  
        .description("Speed up when closer to target.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Double> acceleration = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("acceleration")  
        .description("How much to accelerate.")  
        .defaultValue(1.5)  
        .min(1.0)  
        .max(5.0)  
        .sliderMax(5.0)  
        .visible(accelerate::get)  
        .build()  
    );  

    private final Setting<Boolean> randomization = sgSmoothing.add(new BoolSetting.Builder()  
        .name("randomization")  
        .description("Add micro-adjustments for human-like movement.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Double> randomStrength = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("random-strength")  
        .description("Strength of randomization.")  
        .defaultValue(0.03)  
        .min(0.0)  
        .max(0.5)  
        .sliderMax(0.5)  
        .visible(randomization::get)  
        .build()  
    );  

    private final Setting<Double> strafeIncrease = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("strafe-increase")  
        .description("Increases aim speed when you or target are strafing.")  
        .defaultValue(0.0)  
        .min(0.0)  
        .max(100.0)  
        .sliderMax(100.0)  
        .build()  
    );  

    private final Setting<Boolean> easeIn = sgSmoothing.add(new BoolSetting.Builder()  
        .name("ease-in")  
        .description("Gradually start aiming.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Boolean> easeOut = sgSmoothing.add(new BoolSetting.Builder()  
        .name("ease-out")  
        .description("Gradually stop aiming when near target.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Boolean> adaptiveSpeed = sgSmoothing.add(new BoolSetting.Builder()  
        .name("adaptive-speed")  
        .description("Adjust aim speed based on distance to target.")  
        .defaultValue(false)  
        .build()  
    );  

    private final Setting<Double> adaptiveMinSpeed = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("adaptive-min-speed")  
        .description("Minimum speed percentage when close to target.")  
        .defaultValue(40.0)  
        .min(10.0)  
        .max(90.0)  
        .sliderMax(90.0)  
        .visible(adaptiveSpeed::get)  
        .build()  
    );  

    private final Setting<Double> adaptiveRange = sgSmoothing.add(new DoubleSetting.Builder()  
        .name("adaptive-range")  
        .description("Distance in degrees where adaptive speed applies.")  
        .defaultValue(30.0)  
        .min(5.0)  
        .max(90.0)  
        .sliderMax(90.0)  
        .visible(adaptiveSpeed::get)  
        .build()  
    );  

    // Targeting Settings
    private final Setting<TargetPriority> priority = sgTargeting.add(new EnumSetting.Builder<TargetPriority>()
        .name("priority")
        .description("How to select targets.")
        .defaultValue(TargetPriority.ClosestAngle)
        .build()
    );

    private final Setting<AimPoint> aimPoint = sgTargeting.add(new EnumSetting.Builder<AimPoint>()
        .name("aim-point")
        .description("Where to aim on the target.")
        .defaultValue(AimPoint.Chest)
        .build()
    );

    private final Setting<TargetArea> targetArea = sgTargeting.add(new EnumSetting.Builder<TargetArea>()
        .name("target-area")
        .description("Which part of the target's hitbox to aim towards.")
        .defaultValue(TargetArea.Center)
        .build()
    );

    private final Setting<Boolean> predictMovement = sgTargeting.add(new BoolSetting.Builder()
        .name("predict-movement")
        .description("Predict target movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> predictionMultiplier = sgTargeting.add(new DoubleSetting.Builder()
        .name("prediction-multiplier")
        .description("Movement prediction multiplier.")
        .defaultValue(0.5)
        .min(0.0)
        .max(2.0)
        .sliderMax(2.0)
        .visible(predictMovement::get)
        .build()
    );

    // Control Settings
    private final Setting<Boolean> checkBlockBreak = sgControl.add(new BoolSetting.Builder()
        .name("check-block-break")
        .description("Pauses aim assist while breaking blocks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disableInGUI = sgControl.add(new BoolSetting.Builder()
        .name("disable-in-gui")
        .description("Disables aim assist when in inventories, chests, signs, chat, etc.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableWhileUsing = sgControl.add(new BoolSetting.Builder()
        .name("disable-while-using")
        .description("Disables aim assist while eating, drinking, blocking with shield, etc.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> allowVerticalControl = sgControl.add(new BoolSetting.Builder()
        .name("allow-vertical-control")
        .description("Allow free vertical mouse movement.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> verticalControlThreshold = sgControl.add(new DoubleSetting.Builder()
        .name("vertical-threshold")
        .description("How much vertical movement to detect manual control.")
        .defaultValue(2.0)
        .min(0.5)
        .max(10.0)
        .sliderMax(10.0)
        .visible(allowVerticalControl::get)
        .build()
    );

    private final Setting<Boolean> allowHorizontalControl = sgControl.add(new BoolSetting.Builder()
        .name("allow-horizontal-control")
        .description("Allow free horizontal mouse movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> horizontalControlThreshold = sgControl.add(new DoubleSetting.Builder()
        .name("horizontal-threshold")
        .description("How much horizontal movement to detect manual control.")
        .defaultValue(5.0)
        .min(0.5)
        .max(20.0)
        .sliderMax(20.0)
        .visible(allowHorizontalControl::get)
        .build()
    );

    // Legit Settings  
    private final Setting<Boolean> gcdFix = sgLegit.add(new BoolSetting.Builder()  
        .name("gcd-fix")  
        .description("Fix rotations to match mouse sensitivity.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Boolean> reactionDelay = sgLegit.add(new BoolSetting.Builder()  
        .name("reaction-delay")  
        .description("Reaction time delay.")  
        .defaultValue(true)  
        .build()  
    );  

    private final Setting<Integer> reactionTime = sgLegit.add(new IntSetting.Builder()
        .name("reaction-time")
        .description("Reaction time till aim assist works.")
        .defaultValue(80)
        .min(1)
        .max(500)
        .sliderMax(500)
        .visible(reactionDelay::get)
        .build()
    );

    private final Setting<Boolean> checkVisible = sgLegit.add(new BoolSetting.Builder()
        .name("check-visible")
        .description("Aim if target is visible.")
        .defaultValue(false)
        .visible(reactionDelay::get)
        .build()
    );

    // State tracking
    private PlayerEntity target;
    private float smoothYaw, smoothPitch;
    private float lastYaw, lastPitch;
    private Vec3d lastPlayerPos;
    private Vec3d lastTargetVelocity;
    private long visibleSinceTime = 0;
    private boolean targetWasVisible = false;

    public AimAssist() {
        super(Categories.Combat, "aim-assist", "Assists your aim.");
    }

    @Override
    public void onActivate() {
        target = null;
        smoothYaw = smoothPitch = 0;
        if (mc.player != null) updateLastRotation();
        visibleSinceTime = 0;
        targetWasVisible = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Disable in GUI
        if (disableInGUI.get() && mc.currentScreen != null) {
            target = null;
            smoothYaw = smoothPitch = 0;
            updateLastRotation();
            return;
        }

        // Disable while using items
        if (disableWhileUsing.get() && mc.player.isUsingItem()) {
            target = null;
            smoothYaw = smoothPitch = 0;
            updateLastRotation();
            return;
        }

        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) {
            target = null;
            smoothYaw = smoothPitch = 0;
            visibleSinceTime = 0;
            targetWasVisible = false;
            updateLastRotation();
            return;
        }

        // Check if breaking blocks
        if (checkBlockBreak.get() && mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
            return;
        }

        detectManualMovement();
        target = findTarget();

        // Reaction delay logic
        if (target != null && reactionDelay.get()) {
            boolean isVisible = !checkVisible.get() || mc.player.canSee(target);

            if (isVisible && !targetWasVisible) {
                visibleSinceTime = System.currentTimeMillis();
                targetWasVisible = true;
            } else if (!isVisible) {
                visibleSinceTime = 0;
                targetWasVisible = false;
            }

            if (targetWasVisible && checkVisible.get()) {
                long timeSinceVisible = System.currentTimeMillis() - visibleSinceTime;
                if (timeSinceVisible < reactionTime.get()) {
                    updateLastRotation();
                    return;
                }
            }
        }

        if (target != null) applyAimAssist();
        updateLastRotation();
    }

    private void detectManualMovement() {  
        if (mc.player == null) return;  

        float currentYaw = mc.player.getYaw();  
        float currentPitch = mc.player.getPitch();  

        float yawDelta = Math.abs(MathHelper.wrapDegrees(currentYaw - lastYaw));  
        float pitchDelta = Math.abs(currentPitch - lastPitch);  

        if (allowVerticalControl.get() && pitchDelta > verticalControlThreshold.get()) smoothPitch = 0;  
        if (allowHorizontalControl.get() && yawDelta > horizontalControlThreshold.get()) smoothYaw = 0;  
    }  

    private void updateLastRotation() {  
        lastYaw = mc.player.getYaw();  
        lastPitch = mc.player.getPitch();  
    }  

    private PlayerEntity findTarget() {  
        PlayerEntity bestTarget = null;  
        double bestValue = Double.MAX_VALUE;  

        for (Entity entity : mc.world.getEntities()) {  
            if (!(entity instanceof PlayerEntity player)) continue;  
            if (player == mc.player || player.isDead() || player.getHealth() <= 0) continue;  
            if (mc.player.distanceTo(player) > distance.get()) continue;  
            if (!ignoreWalls.get() && !mc.player.canSee(player)) continue;  

            double angle = getAngleTo(player);  
            if (angle > fov.get()) continue;  

            double value = switch (priority.get()) {  
                case ClosestAngle -> angle;  
                case ClosestDistance -> mc.player.distanceTo(player);  
                case LowestHealth -> player.getHealth();  
            };  

            if (value < bestValue) {  
                bestValue = value;  
                bestTarget = player;  
            }  
        }  

        return bestTarget;  
    }  

    private void applyAimAssist() {  
        if (target == null) return;  

        Vec3d targetPos = getAimPosition(target);  

        // Target Area 
        if (targetArea.get() == TargetArea.Closest) {  
            Vec3d eyePos = mc.player.getEyePos();  
            double width = target.getWidth() / 2;  
            double height = target.getHeight();  

            // Clamp to hitbox bounds  
            double closestX = Math.max(targetPos.x - width, Math.min(eyePos.x, targetPos.x + width));  
            double closestY = Math.max(targetPos.y, Math.min(eyePos.y, targetPos.y + height));  
            double closestZ = Math.max(targetPos.z - width, Math.min(eyePos.z, targetPos.z + width));  

            targetPos = new Vec3d(closestX, closestY, closestZ);  
        } else if (targetArea.get() == TargetArea.Hybrid) {
            // Hybrid mode
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d lookVec = mc.player.getRotationVec(1.0f);

            // Get hitbox bounds
            double width = target.getWidth() / 2;
            double height = target.getHeight();
            Vec3d targetCenter = target.getPos().add(0, height / 2, 0);

            // Check if player's crosshair is aiming inside the hitbox
            Vec3d aimIntersection = raycastToHitbox(eyePos, lookVec, targetCenter, width, height);

            if (aimIntersection == null) {
                double closestX = Math.max(targetCenter.x - width, Math.min(eyePos.x, targetCenter.x + width));
                double closestZ = Math.max(targetCenter.z - width, Math.min(eyePos.z, targetCenter.z + width));

                // Keep the vertical aim point from aimPoint setting
                targetPos = new Vec3d(closestX, targetPos.y, closestZ);
            } else {
                // Player is aiming inside hitbox
                return;
            }
        }  

        if (predictMovement.get()) {  
            Vec3d velocity = target.getVelocity();  
            double predictionTicks = mc.player.distanceTo(target) * predictionMultiplier.get();   
            targetPos = targetPos.add(velocity.multiply(predictionTicks));  
        }  

        Vec3d eyePos = mc.player.getEyePos();  
        Vec3d direction = targetPos.subtract(eyePos).normalize();  

        float targetYaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);  
        float targetPitch = (float) -Math.toDegrees(Math.asin(direction.y));  

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());  
        float deltaPitch = MathHelper.wrapDegrees(targetPitch - mc.player.getPitch());  

        float smoothFactor = (float) Math.min(1.0, 5.0 / Math.max(1.0, smoothness.get()));  

        if (adaptiveSpeed.get()) {  
            float distFactor = (float) Math.min(1.0, Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch) / adaptiveRange.get());  
            smoothFactor *= adaptiveMinSpeed.get().floatValue() / 100f + (1f - adaptiveMinSpeed.get().floatValue() / 100f) * distFactor;  
            smoothFactor = MathHelper.clamp(smoothFactor, 0.08f, 0.5f);  
        } else smoothFactor = Math.max(smoothFactor, 0.1f);  

        float easedFactor = easeIn.get() ? smoothFactor : 1.0f;  

        smoothYaw = smoothYaw * (1f - easedFactor) + deltaYaw * easedFactor;  
        smoothPitch = smoothPitch * (1f - easedFactor) + deltaPitch * easedFactor;  

        if (randomization.get() && Math.random() < 0.3) {  
            smoothYaw += ((float)Math.random() - 0.5f) * randomStrength.get().floatValue();  
            smoothPitch += ((float)Math.random() - 0.5f) * randomStrength.get().floatValue();  
        }  

        // Detect strafing for strafe increase  
        float strafeMultiplier = 1.0f;  
        if (strafeIncrease.get() > 0) {  
            boolean playerStrafing = false;  
            boolean targetStrafing = false;  

            // Check if player is strafing  
            if (lastPlayerPos != null) {  
                Vec3d playerMovement = mc.player.getPos().subtract(lastPlayerPos);  
                double lateralSpeed = Math.sqrt(playerMovement.x * playerMovement.x + playerMovement.z * playerMovement.z);  
                playerStrafing = lateralSpeed > 0.1;  
            }  

            // Check if target is strafing  
            if (lastTargetVelocity != null) {  
                Vec3d targetVel = target.getVelocity();  
                double targetLateralSpeed = Math.sqrt(targetVel.x * targetVel.x + targetVel.z * targetVel.z);  
                targetStrafing = targetLateralSpeed > 0.1;  
            }  

            if (playerStrafing || targetStrafing) {  
                strafeMultiplier = 1.0f + (strafeIncrease.get().floatValue() / 100.0f);  
            }  

            // Update positions for next tick  
            lastPlayerPos = mc.player.getPos();  
            lastTargetVelocity = target.getVelocity();  
        }  

        // Apply horizontal aim
        float newYaw = mc.player.getYaw() + (smoothYaw * horizontalSpeed.get().floatValue() * strafeMultiplier);  

        // Apply vertical aim only if allowVerticalControl is disabled
        float newPitch = mc.player.getPitch();
        if (!allowVerticalControl.get()) {
            newPitch += (smoothPitch * verticalSpeed.get().floatValue() * strafeMultiplier);
        }  

        if (gcdFix.get()) {  
            double gcd = Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;  
            newYaw -= (newYaw - mc.player.getYaw()) % gcd;  
            newPitch -= (newPitch - mc.player.getPitch()) % gcd;  
        }  

        mc.player.setYaw(newYaw);  
        mc.player.setPitch(MathHelper.clamp(newPitch, -90.0f, 90.0f));  
    }  

    // Ray-box intersection method for Hybrid mode
    private Vec3d raycastToHitbox(Vec3d rayOrigin, Vec3d rayDir, Vec3d boxCenter, double halfWidth, double halfHeight) {
        // AABB bounds
        Vec3d boxMin = new Vec3d(boxCenter.x - halfWidth, boxCenter.y - halfHeight, boxCenter.z - halfWidth);
        Vec3d boxMax = new Vec3d(boxCenter.x + halfWidth, boxCenter.y + halfHeight, boxCenter.z + halfWidth);

        // Avoid division by zero
        double dirX = rayDir.x == 0 ? 0.0001 : rayDir.x;
        double dirY = rayDir.y == 0 ? 0.0001 : rayDir.y;
        double dirZ = rayDir.z == 0 ? 0.0001 : rayDir.z;

        // Calculate intersection distances for each axis
        double t1 = (boxMin.x - rayOrigin.x) / dirX;
        double t2 = (boxMax.x - rayOrigin.x) / dirX;
        double t3 = (boxMin.y - rayOrigin.y) / dirY;
        double t4 = (boxMax.y - rayOrigin.y) / dirY;
        double t5 = (boxMin.z - rayOrigin.z) / dirZ;
        double t6 = (boxMax.z - rayOrigin.z) / dirZ;

        double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        // No intersection if tmax < 0 or tmin > tmax
        if (tmax < 0 || tmin > tmax) {
            return null;
        }

        // Return intersection point
        double t = tmin < 0 ? tmax : tmin; // If inside box, use exit point
        return rayOrigin.add(rayDir.multiply(t));
    }

    private Vec3d getAimPosition(PlayerEntity entity) {  
        Vec3d pos = entity.getPos();  
        double height = entity.getHeight();  
        return switch (aimPoint.get()) {  
            case Head -> pos.add(0, height * 0.9, 0);  
            case Chest -> pos.add(0, height * 0.6, 0);  
            case Body -> pos.add(0, height * 0.5, 0);  
            case Legs -> pos.add(0, height * 0.2, 0);  
        };  
    }  

    private double getAngleTo(Entity entity) {  
        Vec3d eyePos = mc.player.getEyePos();  
        Vec3d targetPos = entity.getPos().add(0, entity.getHeight() / 2, 0);  
        Vec3d direction = targetPos.subtract(eyePos).normalize();  
        Vec3d lookVec = mc.player.getRotationVec(1.0f);  
        double dot = lookVec.dotProduct(direction);  
        return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));  
    }  

    public enum SmoothingMode { Linear, Exponential, Cubic }  
    public enum TargetPriority { ClosestAngle, ClosestDistance, LowestHealth }  
    public enum AimPoint { Head, Chest, Body, Legs }  
    public enum TargetArea { Center, Closest, Hybrid }
}
