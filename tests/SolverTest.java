package tests;

import solvers.ConflictAvoidanceTable;
import solvers.astar.*;
import solvers.cbs.ConflictBasedSearch;
import solvers.independence_detection.EnhancedID;
import solvers.independence_detection.IndependenceDetection;
import solvers.states.ODState;
import utilities.*;
import visuals.MapPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.io.*;
import java.util.*;

import javax.swing.*;

import constants.CostFunction;
import solvers.c_astar.CAStar;
import solvers.states.MultiAgentState;
import solvers.states.SingleAgentState;

public class SolverTest {

	public static void main(String[] args) throws FileNotFoundException {
		//testSingleAgent();
        //testMultiAgent();
        //testIndependenceDetection();
        //testReservation();
	    testCBS();
	}

	public static void testSingleAgent() throws FileNotFoundException {
		ProblemMap problemMap = new ProblemMap(new File("src/maps/test.map"));
		Graph graph = new Graph(Connected.EIGHT, problemMap);
		ProblemInstance problemInstance = new ProblemInstance(graph, Collections.singletonList(new Agent(0, 4, 0)));
		SingleAgentAStar solver = new SingleAgentAStar();
		if (solver.solve(problemInstance)) {
			System.out.println(solver.getPath());
		} else {
			System.out.println("Failure");
		}
	}

	public static void testMultiAgent() throws FileNotFoundException {
        ProblemMap problemMap = new ProblemMap(new File("src/maps/test.map"));
        Graph graph = new Graph(Connected.EIGHT, problemMap);
        Agent a1 = new Agent(0, 4, 0);
        Agent a2 = new Agent(5, 9, 1);
        Agent a3 = new Agent(10, 14, 2);
        ProblemInstance problemInstance = new ProblemInstance(graph, Arrays.asList(a1, a2, a3));
        MultiAgentAStar solver = new MultiAgentAStar(CostFunction.SUM_OF_COSTS);
        if (solver.solve(problemInstance)) {
            System.out.println(solver.getPath());
        } else {
            System.out.println("Failure");
        }
	}

    public static void testIndependenceDetection() throws FileNotFoundException {
        ProblemMap problemMap = new ProblemMap(new File("src/maps/arena.map"));
        Graph graph = new Graph(Connected.EIGHT, problemMap);
        Agent a1 = new Agent(0, 11, 0);
        Agent a2 = new Agent(5, 5, 1);
        Agent a3 = new Agent(10, 12, 2);
        ProblemInstance problemInstance = new ProblemInstance(graph,30);
        IndependenceDetection solver = new EnhancedID(new OperatorDecomposition());
        long start = System.currentTimeMillis();
        if (solver.solve(problemInstance)) {
            long enhancedTime = System.currentTimeMillis() - start;
            System.out.printf("Enhanced:\n\tTime: %dms\n\tCost: %5.2f",
                    enhancedTime, solver.getPath().cost());
        } else {
            System.out.println("Failure");
        }
    }

    public static void testReservation() throws FileNotFoundException {
        ProblemMap problemMap = new ProblemMap(new File("src/maps/reservation_test.map"));
        Graph graph = new Graph(Connected.EIGHT, problemMap);
        ProblemInstance problemInstance = new ProblemInstance(graph, Collections.singletonList(new Agent(0, 1, 0)));
        MultiAgentAStar solver = new MultiAgentAStar(CostFunction.SUM_OF_COSTS);
        solver.getReservation().reserveCoordinate(null, new Coordinate(1, graph.getNodes().get(1)));
        System.out.println("Last time step of reservation = " + solver.getReservation().getLastTimeStep());
        System.out.println(solver.solve(problemInstance));
        System.out.println(solver.getPath().cost());
    }

    public static void testCAT() throws FileNotFoundException {
        ConflictAvoidanceTable cat = new ConflictAvoidanceTable();
        ProblemMap problemMap = new ProblemMap(new File("src/maps/test.map"));
        Graph graph = new Graph(Connected.EIGHT, problemMap);
        Agent agent = new Agent(0, 2, 0);
        Agent other = new Agent(0, 2, 0);
        ProblemInstance problemInstance = new ProblemInstance(graph, Collections.singletonList(agent));
        SingleAgentAStar solver = new SingleAgentAStar();
        solver.solve(problemInstance);
        Path path = solver.getPath();
        cat.addPath(path, 0);

        problemInstance = new ProblemInstance(graph, Collections.singletonList(other));
        solver.solve(problemInstance);
        path = solver.getPath();
        cat.addPath(path, 1);
        System.out.println(cat);
    }

    public static void testCBS() throws FileNotFoundException {
        ConflictBasedSearch cbs = new ConflictBasedSearch();
        EnhancedID id = new EnhancedID(new OperatorDecomposition());
        ProblemMap problemMap = new ProblemMap(new File("src/maps/arena.map"));
        Graph graph = new Graph(Connected.EIGHT, problemMap);

        for (int i = 0; i < 1; i++) {
            ProblemInstance problemInstance = new ProblemInstance(graph, 20);
            long t = System.currentTimeMillis();
            cbs.solve(problemInstance);
            System.out.println(System.currentTimeMillis() - t);
            System.out.println("CBS: " + cbs.getPath().cost());
            //System.out.println(cbs.getPath());
            for (State s : cbs.getPath()) {
                s.printIndices();
            }
            System.out.println();
            t = System.currentTimeMillis();
            id.solve(problemInstance);
            System.out.println(System.currentTimeMillis() - t);
            System.out.println("ID: " + id.getPath().cost());
            for (State s : id.getPath()) {
                s.printIndices();
            }
            System.out.println();
            //System.out.println(id.getPath());
        }
    }

}