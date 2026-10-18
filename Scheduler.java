package com.smartwarehouse;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Scheduler — Warehouse Task & Order Scheduling Engine.
 *
 * Implements:
 * 1. AI Order Priority Scheduling with FCFS Tie-Breaking (Prompt Section 6):
 *    - Priority 1 is HIGHEST priority (sorted 1 before 2, 3, 4).
 *    - FCFS tie-breaker: Orders created earlier arrive first in the queue.
 *    - Automated queue position assignment (1, 2, 3...).
 * 2. Operating System Task Scheduling (Prompt Section 1):
 *    - FCFS (First-Come, First-Served)
 *    - SJF (Shortest Job First - Non-preemptive)
 *    - Priority Scheduling
 *    - Round Robin (Time-shared with Quantum = 2)
 *    - Performance evaluation & algorithm recommendations
 */
public class Scheduler {

    private static final int RR_QUANTUM = 2;

    private Scheduler() {
        // Utility class
    }

    // =========================================================
    // 1. ORDER SCHEDULING DATA MODEL & ENGINE (SECTION 6)
    // =========================================================

    /**
     * Represents an order in the warehouse scheduling queue.
     */
    public static class ScheduledOrder {
        private int orderId;
        private int userId;
        private int aiPriority; // 1 = Highest, 2 = High, 3 = Medium, 4 = Normal
        private Timestamp arrivalTime;
        private int queuePosition;
        private String recommendation;
        private String reason;
        private double totalAmount;
        private int itemCount;

        public ScheduledOrder(int orderId, int userId, int aiPriority, Timestamp arrivalTime,
                              String recommendation, String reason, double totalAmount, int itemCount) {
            this.orderId = orderId;
            this.userId = userId;
            this.aiPriority = Math.max(1, aiPriority);
            this.arrivalTime = arrivalTime;
            this.recommendation = recommendation != null ? recommendation : "FCFS";
            this.reason = reason != null ? reason : "Standard arrival order.";
            this.totalAmount = totalAmount;
            this.itemCount = itemCount;
            this.queuePosition = 0;
        }

        public int getOrderId() { return orderId; }
        public int getUserId() { return userId; }
        public int getAiPriority() { return aiPriority; }
        public Timestamp getArrivalTime() { return arrivalTime; }
        public int getQueuePosition() { return queuePosition; }
        public String getRecommendation() { return recommendation; }
        public String getReason() { return reason; }
        public double getTotalAmount() { return totalAmount; }
        public int getItemCount() { return itemCount; }

        public void setQueuePosition(int queuePosition) {
            this.queuePosition = queuePosition;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "Pos: %d | Order: ORD-%d | Priority: %d | Arrival: %s | Total: ₹%.2f | Items: %d",
                    queuePosition, orderId, aiPriority,
                    arrivalTime != null ? arrivalTime.toString() : "Recent",
                    totalAmount, itemCount);
        }
    }

    /**
     * Schedules a list of orders according to:
     * 1. Recommended AI Priority (Priority 1 goes BEFORE Priority 2 and Priority 3).
     * 2. FCFS Tie-Breaker: When priorities are equal, earlier arrivalTime goes first.
     * 3. Secondary Tie-Breaker: Order ID ascending.
     *
     * Automatically assigns sequential queue positions (1, 2, 3...).
     *
     * Example:
     * Order ID | AI Priority | Arrival Time | Queue Position
     * ORD-1002 | 1           | 10:05        | 1
     * ORD-1001 | 1           | 10:08        | 2
     * ORD-1003 | 2           | 10:02        | 3
     */
    public static List<ScheduledOrder> scheduleOrders(List<ScheduledOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<ScheduledOrder> result = new ArrayList<>(orders);

        result.sort((a, b) -> {
            // 1. Priority: Lower number = Higher priority (1 before 2)
            int pCmp = Integer.compare(a.getAiPriority(), b.getAiPriority());
            if (pCmp != 0) {
                return pCmp;
            }

            // 2. FCFS Tie-breaker: Earlier arrival time first
            if (a.getArrivalTime() != null && b.getArrivalTime() != null) {
                int tCmp = a.getArrivalTime().compareTo(b.getArrivalTime());
                if (tCmp != 0) {
                    return tCmp;
                }
            } else if (a.getArrivalTime() != null) {
                return -1;
            } else if (b.getArrivalTime() != null) {
                return 1;
            }

            // 3. Order ID tie-breaker
            return Integer.compare(a.getOrderId(), b.getOrderId());
        });

        // Automatically assign Queue Positions: 1, 2, 3...
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setQueuePosition(i + 1);
        }

        return result;
    }

    /**
     * Maps TabNet recommendation string to explicit priority levels.
     * Priority 1 = Highest.
     */
    public static int mapTabNetToPriority(String recommendation) {
        if (recommendation == null || recommendation.isBlank()) {
            return 3;
        }

        String norm = recommendation.trim().toUpperCase(Locale.ROOT);
        if (norm.contains("PRIORITY")) {
            return 1; // Highest priority
        } else if (norm.contains("SJF") || norm.contains("SHORT")) {
            return 2; // High priority (quick turnaround)
        } else if (norm.contains("ROUND ROBIN") || norm.contains("RR")) {
            return 3; // Medium priority (time-shared)
        } else {
            return 4; // Standard FCFS normal priority
        }
    }

    // =========================================================
    // 2. OS TASK SCHEDULING (PRESERVED & ENHANCED)
    // =========================================================

    private static class Segment {
        Task task;
        int start;
        int finish;

        Segment(Task task, int start, int finish) {
            this.task = task;
            this.start = start;
            this.finish = finish;
        }
    }

    private static class Metrics {
        double averageWaiting;
        double averageTurnaround;
        int makespan;
        List<Segment> segments = new ArrayList<>();

        double score() {
            return 100.0 / (1.0 + averageWaiting);
        }
    }

    // ================= FCFS =================
    public static List<Task> fcfs(List<Task> tasks) {
        List<Task> result = copy(tasks);

        result.sort(
            Comparator.comparingInt(
                (Task t) -> Math.max(0, t.getArrival())
            ).thenComparingInt(Task::getId)
        );

        return result;
    }

    // ================= SJF (SHORTEST JOB FIRST) =================
    public static List<Task> sjf(List<Task> tasks) {
        return nonPreemptive(tasks, "SJF");
    }

    // ================= PRIORITY SCHEDULING =================
    // Lower number = higher priority.
    // Tie-breaker: arrival time, then task ID.
    public static List<Task> priority(List<Task> tasks) {
        return nonPreemptive(tasks, "PRIORITY");
    }

    private static List<Task> nonPreemptive(List<Task> tasks, String algorithm) {
        List<Task> pending = copy(tasks);
        List<Task> result = new ArrayList<>();

        int time = 0;

        while (!pending.isEmpty()) {
            Task selected = null;

            for (Task task : pending) {
                int arrival = Math.max(0, task.getArrival());

                if (arrival > time) {
                    continue;
                }

                if (selected == null || compare(task, selected, algorithm) < 0) {
                    selected = task;
                }
            }

            if (selected == null) {
                int nextArrival = Integer.MAX_VALUE;

                for (Task task : pending) {
                    nextArrival = Math.min(
                        nextArrival,
                        Math.max(0, task.getArrival())
                    );
                }

                time = nextArrival;
                continue;
            }

            result.add(selected);
            pending.remove(selected);

            time += Math.max(1, selected.getBurst());
        }

        return result;
    }

    private static int compare(Task a, Task b, String algorithm) {
        int comparison;

        if ("SJF".equalsIgnoreCase(algorithm)) {
            comparison = Integer.compare(
                Math.max(1, a.getBurst()),
                Math.max(1, b.getBurst())
            );
        } else {
            // Lower priority number = higher priority (Priority 1 < Priority 2)
            comparison = Integer.compare(
                a.getPriority(),
                b.getPriority()
            );
        }

        if (comparison == 0) {
            comparison = Integer.compare(
                Math.max(0, a.getArrival()),
                Math.max(0, b.getArrival())
            );
        }

        if (comparison == 0) {
            comparison = Integer.compare(a.getId(), b.getId());
        }

        return comparison;
    }

    private static List<Task> copy(List<Task> tasks) {
        return tasks == null ? new ArrayList<>() : new ArrayList<>(tasks);
    }

    // ================= ROUND ROBIN =================
    public static Metrics roundRobin(List<Task> tasks) {
        Metrics metrics = new Metrics();
        List<Task> input = fcfs(tasks);

        if (input.isEmpty()) {
            return metrics;
        }

        int n = input.size();
        int[] remaining = new int[n];
        int[] completion = new int[n];

        for (int i = 0; i < n; i++) {
            remaining[i] = Math.max(1, input.get(i).getBurst());
        }

        Deque<Integer> queue = new ArrayDeque<>();

        int next = 0;
        int time = 0;
        double totalWaiting = 0;
        double totalTurnaround = 0;

        while (next < n || !queue.isEmpty()) {

            if (queue.isEmpty() && next < n) {
                time = Math.max(
                    time,
                    Math.max(0, input.get(next).getArrival())
                );
            }

            while (next < n && Math.max(0, input.get(next).getArrival()) <= time) {
                queue.addLast(next);
                next++;
            }

            if (queue.isEmpty()) {
                continue;
            }

            int index = queue.removeFirst();
            Task task = input.get(index);

            int duration = Math.min(
                RR_QUANTUM,
                remaining[index]
            );

            int start = time;
            time += duration;
            remaining[index] -= duration;

            metrics.segments.add(
                new Segment(task, start, time)
            );

            while (next < n && Math.max(0, input.get(next).getArrival()) <= time) {
                queue.addLast(next);
                next++;
            }

            if (remaining[index] > 0) {
                queue.addLast(index);
            } else {
                completion[index] = time;

                int arrival = Math.max(0, task.getArrival());
                int burst = Math.max(1, task.getBurst());

                int turnaround = completion[index] - arrival;

                totalTurnaround += turnaround;
                totalWaiting += turnaround - burst;
            }
        }

        metrics.averageWaiting = totalWaiting / n;
        metrics.averageTurnaround = totalTurnaround / n;
        metrics.makespan = time;

        return metrics;
    }

    // ================= EVALUATION =================
    private static Metrics evaluate(List<Task> tasks, String algorithm) {
        Metrics metrics = new Metrics();
        List<Task> scheduled;

        switch (normalize(algorithm)) {
            case "FCFS":
                scheduled = fcfs(tasks);
                break;

            case "SJF":
                scheduled = sjf(tasks);
                break;

            case "PRIORITY":
                scheduled = priority(tasks);
                break;

            case "RR":
            case "ROUND ROBIN":
                return roundRobin(tasks);

            default:
                throw new IllegalArgumentException(
                    "Unsupported algorithm: " + algorithm
                );
        }

        if (scheduled.isEmpty()) {
            return metrics;
        }

        int time = 0;
        double totalWaiting = 0;
        double totalTurnaround = 0;

        for (Task task : scheduled) {
            int arrival = Math.max(0, task.getArrival());
            int burst = Math.max(1, task.getBurst());

            time = Math.max(time, arrival);

            int start = time;
            int finish = start + burst;

            totalWaiting += start - arrival;
            totalTurnaround += finish - arrival;

            metrics.segments.add(
                new Segment(task, start, finish)
            );

            time = finish;
        }

        metrics.averageWaiting = totalWaiting / scheduled.size();
        metrics.averageTurnaround = totalTurnaround / scheduled.size();
        metrics.makespan = time;

        return metrics;
    }

    private static String normalize(String algorithm) {
        return algorithm == null ? "" : algorithm.trim().toUpperCase(Locale.ROOT);
    }

    // ================= SCORE =================
    public static double score(List<Task> tasks, String algorithm) {
        if (tasks == null || tasks.isEmpty()) {
            return 0.0;
        }
        return evaluate(tasks, algorithm).score();
    }

    // ================= ALGORITHM RECOMMENDATION =================
    public static String recommend(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "No waiting tasks available.";
        }

        double fcfsWaiting = evaluate(tasks, "FCFS").averageWaiting;
        double priorityWaiting = evaluate(tasks, "PRIORITY").averageWaiting;

        if (priorityWaiting < fcfsWaiting) {
            return "PRIORITY";
        }

        return "FCFS";
    }

    // ================= RUN SCHEDULER =================
    public static String run(String algorithm) throws SQLException {
        String selected = normalize(algorithm);

        if (selected.equals("ROUND ROBIN")) {
            selected = "RR";
        }

        if (!selected.equals("FCFS") &&
            !selected.equals("SJF") &&
            !selected.equals("PRIORITY") &&
            !selected.equals("RR")) {

            return "Unsupported algorithm: " + algorithm;
        }

        TaskDAO taskDAO = new TaskDAO();
        List<Task> tasks = taskDAO.getWaitingTasks();

        if (tasks == null || tasks.isEmpty()) {
            return "No waiting tasks found in the database.";
        }

        Metrics result = evaluate(tasks, selected);

        StringBuilder output = new StringBuilder();
        output.append("========================================================\n");
        output.append("          WAREHOUSE OS SCHEDULING SIMULATION            \n");
        output.append("========================================================\n");
        output.append("Algorithm Executed : ").append(selected).append("\n");
        output.append("Tasks In Queue     : ").append(tasks.size()).append("\n");
        if (selected.equals("RR")) {
            output.append("Round Robin Quantum: ").append(RR_QUANTUM).append(" time units\n");
        }
        output.append("\nEXECUTION TIMELINE:\n");
        output.append("--------------------------------------------------------\n");

        for (int i = 0; i < result.segments.size(); i++) {
            Segment segment = result.segments.get(i);
            output.append(String.format(" %2d. Task #%-4d | Type: %-10s | Start: %-3d | Finish: %-3d\n",
                    i + 1,
                    segment.task.getId(),
                    segment.task.getType(),
                    segment.start,
                    segment.finish));
        }

        output.append("--------------------------------------------------------\n");
        output.append(String.format("Average Waiting Time    : %s\n", format(result.averageWaiting)));
        output.append(String.format("Average Turnaround Time : %s\n", format(result.averageTurnaround)));
        output.append(String.format("Total Makespan          : %d\n", result.makespan));
        output.append(String.format("Scheduler Score         : %.2f / 100\n", result.score()));

        output.append("\nCOMPARATIVE BENCHMARK:\n");
        output.append(String.format(" • FCFS Waiting Time     : %s\n", format(evaluate(tasks, "FCFS").averageWaiting)));
        output.append(String.format(" • PRIORITY Waiting Time : %s\n", format(evaluate(tasks, "PRIORITY").averageWaiting)));
        output.append("Recommended OS Algorithm: ").append(recommend(tasks)).append("\n");
        output.append("========================================================\n");

        return output.toString();
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}