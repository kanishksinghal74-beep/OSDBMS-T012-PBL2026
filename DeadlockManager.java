package com.smartwarehouse;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DeadlockManager {

    private DeadlockManager() {
    }

    // Key robot jis robot ka wait kar raha hai, woh value mein hota hai.
    public static boolean hasDeadlock(Map<String, String> waitsFor) {
        Set<String> completed = new HashSet<>();

        for (String robot : waitsFor.keySet()) {
            if (hasCycle(robot, waitsFor, new HashSet<>(), completed)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCycle(
            String robot,
            Map<String, String> waitsFor,
            Set<String> checking,
            Set<String> completed) {

        if (checking.contains(robot)) {
            return true; // Robot wait-cycle mein wapas aa gaya
        }
        if (completed.contains(robot)) {
            return false;
        }

        checking.add(robot);
        String waitingFor = waitsFor.get(robot);

        if (waitingFor != null
                && hasCycle(waitingFor, waitsFor, checking, completed)) {
            return true;
        }

        checking.remove(robot);
        completed.add(robot);
        return false;
    }
}