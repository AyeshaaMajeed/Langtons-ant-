package com.example.langtonsant.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

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
        int antCount = ants.size();

        // Only use parallel processing if we have enough ants to make it worthwhile
        if (antCount < threadCount * 2) {
            singleStep(); // Fall back to single-threaded for small workloads
            return;
        }

        // Use a master task that splits the work
        AntMasterTask masterTask = new AntMasterTask(ants);
        forkJoinPool.invoke(masterTask);
    }

    // Master task that handles dividing workload
    private class AntMasterTask extends RecursiveAction {
        private final List<Ant> allAnts;

        public AntMasterTask(List<Ant> allAnts) {
            this.allAnts = allAnts;
        }

        @Override
        protected void compute() {
            int antCount = allAnts.size();
            int batchSize = Math.max(1, antCount / threadCount);

            List<RecursiveAction> tasks = new ArrayList<>();

            for (int i = 0; i < antCount; i += batchSize) {
                int end = Math.min(i + batchSize, antCount);
                List<Ant> batch = allAnts.subList(i, end);
                tasks.add(new AntBatchTask(batch));
            }

            // This properly executes all tasks in parallel
            invokeAll(tasks);
        }
    }

    // Task that processes a batch of ants
    private class AntBatchTask extends RecursiveAction {
        private final List<Ant> antBatch;

        public AntBatchTask(List<Ant> antBatch) {
            this.antBatch = antBatch;
        }

        @Override
        protected void compute() {
            for (Ant ant : antBatch) {
                // Process each ant in this batch
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
}