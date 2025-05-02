package com.example.langtonsant.Main;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class RegionTask extends RecursiveAction {

    private final Grid grid;
    private final List<Ant> ants;

    public RegionTask(Grid grid, List<Ant> ants) {
        this.grid = grid;
        this.ants = ants;
    }

    @Override
    protected void compute() {
        // Process all ants in this region
        for (Ant ant : ants) {
            ant.move(grid);
        }
    }
}