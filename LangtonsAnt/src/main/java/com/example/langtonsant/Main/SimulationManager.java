package com.example.langtonsant.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.concurrent.ForkJoinTask.invokeAll;

public class SimulationManager {

    private final Grid grid;
    private final List<Ant> ants;
    private boolean useParallel = false;
    private final ForkJoinPool forkJoinPool;
    private final int threadCount;

    // Performance monitoring
    private AtomicInteger boundaryConflicts = new AtomicInteger(0);

    public SimulationManager(Grid grid, List<Ant> ants) {
        this.grid = grid;
        this.ants = ants;
        this.threadCount = Runtime.getRuntime().availableProcessors();
        this.forkJoinPool = new ForkJoinPool(threadCount);
    }

    public void step() {
        if (useParallel) {
            parallelStep();
        } else {
            singleStep();
        }
    }

    private void singleStep() {
        for (Ant ant : ants) {
            ant.move(grid);
        }
    }

    private void parallelStep() {
        if (ants.isEmpty()) {
            return;
        }

        if (ants.size() < threadCount * 2) {
            singleStep();
            return;
        }

        List<RecursiveAction> antTasks = new ArrayList<>();

        // Divide ants among tasks
        int antsPerTask = Math.max(1, ants.size() / threadCount);
        for (int i = 0; i < ants.size(); i += antsPerTask) {
            final int startIdx = i;
            final int endIdx = Math.min(i + antsPerTask, ants.size());

            antTasks.add(new RecursiveAction() {
                @Override
                protected void compute() {
                    for (int j = startIdx; j < endIdx; j++) {
                        Ant ant = ants.get(j);
                        moveAntSafely(ant);
                    }
                }
            });
        }

        // Execute all tasks in parallel
        forkJoinPool.invoke(new RecursiveAction() {
            @Override
            protected void compute() {
                invokeAll(antTasks);
            }
        });
    }

    // Thread-safe method for moving an ant that properly handles region boundaries
    private void moveAntSafely(Ant ant) {
        int x = ant.getX();
        int y = ant.getY();
        int direction = ant.getDirection();

        // Calculate potential new position
        int newX = x;
        int newY = y;

        switch (direction) {
            case 0: newY--; break; // North
            case 1: newX++; break; // East
            case 2: newY++; break; // South
            case 3: newX--; break; // West
        }

        // Determine if this move crosses region boundaries
        int currentRegionX = grid.getRegionIndex(x);
        int currentRegionY = grid.getRegionIndex(y);
        int newRegionX = grid.getRegionIndex(newX);
        int newRegionY = grid.getRegionIndex(newY);

        if (currentRegionX != newRegionX || currentRegionY != newRegionY) {
            // Crossing region boundary - need to lock both regions
            Object currentRegionLock = grid.getRegionLock(x, y);
            Object newRegionLock = grid.getRegionLock(newX, newY);

            // Avoid deadlocks by acquiring locks in consistent order
            Object firstLock, secondLock;
            if (System.identityHashCode(currentRegionLock) < System.identityHashCode(newRegionLock)) {
                firstLock = currentRegionLock;
                secondLock = newRegionLock;
            } else {
                firstLock = newRegionLock;
                secondLock = currentRegionLock;
            }

            // Acquire both locks in proper order
            synchronized (firstLock) {
                synchronized (secondLock) {
                    // We have both locks, safe to move the ant
                    ant.move(grid);
                    boundaryConflicts.incrementAndGet();
                }
            }
        } else {
            // Not crossing boundaries, only need one lock
            Object regionLock = grid.getRegionLock(x, y);
            synchronized (regionLock) {
                ant.move(grid);
            }
        }
    }

    public void setUseParallel(boolean useParallel) {
        this.useParallel = useParallel;
    }

    public boolean isUseParallel() {
        return useParallel;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getBoundaryConflicts() {
        return boundaryConflicts.get();
    }

    public void resetBoundaryConflicts() {
        boundaryConflicts.set(0);
    }

    public void shutdown() {
        forkJoinPool.shutdown();
    }

    public void multiStep(int steps) {
        if (!useParallel || ants.size() < threadCount * 2) {
            // Single-threaded multiple steps
            for (int i = 0; i < steps; i++) {
                singleStep();
            }
        } else {
            // Perform multiple steps with parallel processing
            for (int i = 0; i < steps; i++) {
                parallelStep();
            }
        }
    }
}