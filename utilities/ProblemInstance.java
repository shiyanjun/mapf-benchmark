package utilities;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import solvers.astar.TDHeuristic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.stream.Stream;

public class ProblemInstance {
	private Graph graph;
	private List<Agent> agents;
	private List<Node> goalPositions;

    private String mapTitle;
	private Connected connectedness;
	private TDHeuristic trueDistanceHeuristic;

	/**
	 * Constructor that creates a problem instance using the given
     * graph, with agents from a file of serialized agents
	 * @param agentsFile The file with serialized agent objects
	 */
	// This constructor is deprecated and should not be used
	@Deprecated
	public ProblemInstance(Graph gr, File agentsFile){
		agents = deserializeAgents(agentsFile);
        graph = gr;
		goalPositions = agentGoals();
        if (!mapTitle.equals(graph.getMapTitle()))
            throw new IllegalArgumentException("Map " + mapTitle + " not compatible with problem instance!\n " +
                    "Expected " + graph.getMapTitle());
        trueDistanceHeuristic = new TDHeuristic(this);
    }

	/**
	 * Constructor that creates a problem instance using the given problem File.
	 * File contains agent data, map title, and connectedness
	 * @param problem_file
	 * @throws FileNotFoundException
     */
	public ProblemInstance(File problem_file) throws FileNotFoundException {
		// Pull the agents and map title out of the file
		agents = deserializeAgents(problem_file);
		graph = new Graph(connectedness, new ProblemMap(new File("src/maps/" + mapTitle)));
		goalPositions = agentGoals();
		trueDistanceHeuristic = new TDHeuristic(this);
	}

    /**
     * Constructor that creates a problem instance using the given
     * graph and with the given agents
     * @param graph the graph to use
     * @param agents the agents in the problem
     */
	public ProblemInstance(Graph graph, List<Agent> agents) {
		this(graph, agents, true);
	}

	public ProblemInstance(Graph graph, List<Agent> agents, boolean useHeuristic) {
	    this.graph = graph;
        this.agents = agents;
        goalPositions = agentGoals();
        if (duplicateGoalsOrStarts(agents)) throw new IllegalArgumentException("Agents share goals or start positions!"
                + agents);
        if (useHeuristic) trueDistanceHeuristic = new TDHeuristic(this);
    }

    public ProblemInstance(Graph graph, List<Agent> agents, TDHeuristic heuristic) {
        this(graph, agents, false);
        this.trueDistanceHeuristic = heuristic;
    }

    /**
     * Constructs a subproblem using the specified agents with the same heuristic
     * @param parent the problem instance that should be copied
     */
    public ProblemInstance(ProblemInstance parent, List<Integer> agentIDs) {
        this.graph = parent.getGraph();
        this.agents = agentsFromList(parent, agentIDs);
        goalPositions = agentGoals();
        if (duplicateGoalsOrStarts(agents)) throw new IllegalArgumentException("Agents share goals or start positions!"
                + agents);
        this.trueDistanceHeuristic = parent.getTrueDistanceHeuristic();
    }

    private List<Agent> agentsFromList(ProblemInstance problemInstance, List<Integer> agentIDs) {
        List<Agent> agents = new ArrayList<>();
        int newId = 0;
        for (int id : agentIDs) {
            Agent oldAgent = problemInstance.getAgents().get(id);
            Agent newAgent = new Agent(oldAgent.position(), oldAgent.goal(), newId++);
            agents.add(newAgent);
        }
        return agents;
    }

    /**
     * Constructor that creates a problem instance with the specified graph
     * and nAgents randomly generated agents
     * @param graph the graph
     * @param nAgents number of agents
     */
	public ProblemInstance(Graph graph, int nAgents) {
        this.graph = graph;
        this.agents = graph.generateRandomAgents(nAgents);
        while (duplicateGoalsOrStarts(agents)) {
            agents = graph.generateRandomAgents(nAgents);
        }
		goalPositions = agentGoals();
        trueDistanceHeuristic = new TDHeuristic(this);
	}

	public TDHeuristic getTrueDistanceHeuristic() {
	    return trueDistanceHeuristic;
    }

    /**
     * Convenience method to add an agent to a problem instance
     * without creating a new problem instance
     * @param newAgent the agent to add
     */
    public void addAgent(Agent newAgent) {
        List<Agent> current = new ArrayList<>(agents);
        current.add(new Agent(newAgent.position(), newAgent.goal(), current.size()));
        if (duplicateGoalsOrStarts(current)) throw new IllegalArgumentException("Agent with this goal or start location" +
                "already exists!");
        this.agents = current;
		updateGoalPositions();
    }

	public void addRandomAgents(int nAgents) {
		List<Agent> newAgents =  graph.generateRandomAgents(nAgents);
		for (Agent agent : agents)
			newAgents.add(agent);
		while (duplicateGoalsOrStarts(newAgents)) {
			newAgents = graph.generateRandomAgents(nAgents);
			for (Agent agent : agents)
				newAgents.add(agent);
		}
		agents = newAgents;
	}

    /**
     * Returns a problem instance that
     * represents the union of two others
     * @param other the other problem instance
     * @return the union of this problem instance with the other one
     */
    public ProblemInstance join(ProblemInstance other, boolean recomputeHeuristic) {
        List<Agent> joinAgents = new ArrayList<>(agents);
        for (Agent agent : other.agents) {
            Agent newAgent = new Agent(agent.position(), agent.goal(), joinAgents.size());
            joinAgents.add(newAgent);
        }
        if (recomputeHeuristic) {
            return new ProblemInstance(graph, joinAgents);
        } else {
            return new ProblemInstance(graph, joinAgents, this.getTrueDistanceHeuristic());
        }
    }

    /**
     * Returns the map associated to this problem instance
     * @return the map associated to this problem instance
     */
	public ProblemMap getMap() {
		return graph.getMap();
	}

    /**
     * Returns the list of agents associated to this problem instance
     * @return the list of agents associated to this problem instance
     */
	public List<Agent> getAgents() {
		return agents;
	}

    /**
     * Returns the graph associated to this problem instance
     * @return the graph associated to this problem instance
     */
	public Graph getGraph() {
		return graph;
	}

    /**
     * Returns the nodes that are goal locations for agents in
     * this problem instance
     * @return the goal nodes in this problem instance
     */
	public List<Node> getGoal() {
		return goalPositions;
	}
	
	private List<Node> agentGoals() {
		List<Node> goalList = new ArrayList<Node>();
		for (Agent agent : this.agents) {
			goalList.add(graph.getNodes().get(agent.goal()));
		}
		return goalList;
	}

    /**
     * Serialize this problem instance and
     * store it in a file with the given name
     * in a directory
     * @param path the path to the directory
     * @param fileName name to give the problem instance
     */
	public void serialize(String path, String fileName) {
		try {
			FileOutputStream fileOut = new FileOutputStream(path + fileName + ".prob");
			ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(graph.getMapTitle());
			objectOut.writeObject(graph.getConnectedness());
			objectOut.writeObject(agents.size());
			for (Agent a : agents)
				objectOut.writeObject(a);
			fileOut.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Agent> deserializeAgents(File agentsFile) {
		List<Agent> agentList = new ArrayList<Agent>();
		Integer numAgents = 0;
		try {
			FileInputStream fileIn = new FileInputStream(agentsFile);
			BufferedInputStream buffer = new BufferedInputStream(fileIn);
			ObjectInputStream objectIn = new ObjectInputStream(buffer);
            mapTitle = (String) objectIn.readObject();
			connectedness = (Connected) objectIn.readObject();
            numAgents = (Integer) objectIn.readObject();

            for (int i = 0; i < numAgents; i++)
                agentList.add((Agent) objectIn.readObject());
		}
		catch(ClassNotFoundException e) {
            e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println(numAgents);
		return agentList;
	}


	private boolean duplicateGoalsOrStarts(List<Agent> agentList) {
		HashSet<Integer> goals = new HashSet<Integer>();
		HashSet<Integer> starts = new HashSet<Integer>();
		for (Agent a : agentList) {
			goals.add(a.goal());
			starts.add(a.position());
		}
		return !(goals.size() == agentList.size()
				&& starts.size() == agentList.size());
	}

	private void updateGoalPositions() {
		goalPositions.clear();
		for (Agent agent : agents) {
			goalPositions.add(graph.getNodes().get(agent.goal()));
		}
	}

}
