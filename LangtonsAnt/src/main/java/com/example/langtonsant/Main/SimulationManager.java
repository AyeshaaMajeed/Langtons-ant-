package com.example.langtonsant.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
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

    // Replace the parallelStep() method with this implementation
    private void parallelStep() {
        int antCount = ants.size();

        // Only use parallel processing if we have enough ants
        if (antCount < threadCount * 4) {
            singleStep();
            return;
        }

        // Use parallel streams for simpler, effective parallelization
        ants.parallelStream().forEach(ant -> ant.move(grid));
    }
    private int calculateOptimalStepsPerBatch(int antCount) {
        // More ants = fewer steps per batch to maintain responsiveness
        if (antCount > 500) return 2;
        if (antCount > 200) return 5;
        return 10;
    }

    private List<List<Ant>> groupAntsByPosition(List<Ant> allAnts, int groups) {
        List<List<Ant>> result = new ArrayList<>(groups);
        for (int i = 0; i < groups; i++) {
            result.add(new ArrayList<>());
        }

        // Group ants based on position in grid
        for (Ant ant : allAnts) {
            // Simple spatial hashing to keep nearby ants in the same group
            int x = ant.getX();
            int y = ant.getY();
            int groupIndex = Math.abs((x * 73 + y * 151) % groups);
            result.get(groupIndex).add(ant);
        }

        return result;
    }

    // Updated batch task to process multiple steps at once
    private class AntBatchTask extends RecursiveAction {
        private final List<Ant> antBatch;
        private final int stepsPerBatch;

        public AntBatchTask(List<Ant> antBatch, int stepsPerBatch) {
            this.antBatch = antBatch;
            this.stepsPerBatch = stepsPerBatch;
        }

        @Override
        protected void compute() {
            // Process multiple steps before synchronizing
            for (int step = 0; step < stepsPerBatch; step++) {
                for (Ant ant : antBatch) {
                    ant.move(grid);
                }
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

    // Add this method to the SimulationManager class for batch processing
    public void multiStep(int steps) {
        if (!useParallel || ants.size() < threadCount * 4) {
            // Single-threaded multiple steps
            for (int i = 0; i < steps; i++) {
                for (Ant ant : ants) {
                    ant.move(grid);
                }
            }
        } else {
            // Optimized parallel approach for multiple steps
            // Each thread processes all steps for its assigned ants
            List<Runnable> tasks = new ArrayList<>();
            int batchSize = Math.max(1, ants.size() / threadCount);

            for (int i = 0; i < ants.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, ants.size());

                tasks.add(() -> {
                    for (int step = 0; step < steps; step++) {
                        for (int j = start; j < end; j++) {
                            ants.get(j).move(grid);
                        }
                    }
                });
            }

            // Submit all tasks and wait for completion
            try {
                List<Future<?>> futures = new ArrayList<>();
                for (Runnable task : tasks) {
                    futures.add(forkJoinPool.submit(task));
                }
                for (Future<?> future : futures) {
                    future.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}