package com.jorianwoltjer.liveoverflowmod.helper;

import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.LinkedList;

public class Utils {

    public static double roundCoordinate(double n) {
        n = Math.round(n * 100) / 100d;  // Round to 1/100th
        return Math.nextAfter(n, n + Math.signum(n));  // Fix floating point errors
    }

    public static void onPositionPacket(Args args) {
        double x = args.get(0);
        double y = args.get(1);
        double z = args.get(2);

        // Round to 100ths for Anti-Human check
        x = roundCoordinate(x);
        z = roundCoordinate(z);

        args.set(0, x);
        args.set(1, y);
        args.set(2, z);
    }

    @SuppressWarnings("SameParameterValue")
    public static <T> void insertToCenter(LinkedList<T> list, T object) {
        int middle = (list.size() + 1) / 2;  // Rounded up
        list.add(middle, object);
    }

}
