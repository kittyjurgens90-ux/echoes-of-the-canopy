package com.example.echoesofthecanopy.grapple;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Tuning values + shared math (used by BOTH logical sides so they agree). */
public final class GrappleConstants {
    public static final float LAUNCH_SPEED = 2.4F;          // blocks/tick
    public static final double MAX_RANGE = 32.0D;
    public static final int MAX_FLIGHT_TICKS = 25;
    /** Must stay well under vanilla's ~80 tick "Flying is not enabled" kick window. */
    public static final int MAX_PULL_TICKS = 60;
    public static final double PULL_SPEED = 0.85D;          // blocks/tick
    public static final double PULL_RESPONSE = 0.40D;       // lerp factor toward desired velocity
    public static final double ARRIVE_DISTANCE = 2.0D;
    /** Server rejects/clamps any client-reported speed above this. */
    public static final double MAX_ACCEPTED_SPEED = 2.0D;

    /** Returns null for NaN/Infinity; otherwise clamps to MAX_ACCEPTED_SPEED. */
    @Nullable
    public static Vec3 sanitize(Vec3 v) {
        if (!Double.isFinite(v.x) || !Double.isFinite(v.y) || !Double.isFinite(v.z)) return null;
        double len = v.length();
        return len > MAX_ACCEPTED_SPEED ? v.scale(MAX_ACCEPTED_SPEED / len) : v;
    }

    /** Momentum kept when letting go, plus a small hop so players can climb onto the branch. */
    public static Vec3 exitVelocity(Vec3 v) {
        double hop = v.y > 0.0D ? 0.25D : 0.0D;
        return new Vec3(v.x * 0.6D, v.y * 0.6D + hop, v.z * 0.6D);
    }

    private GrappleConstants() {}
}
