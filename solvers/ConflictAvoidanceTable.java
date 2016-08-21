package solvers;

import solvers.states.MultiAgentState;
import solvers.states.SingleAgentState;
import utilities.Conflict;
import utilities.Coordinate;
import utilities.Node;
import utilities.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictAvoidanceTable {

    private static final int NO_CONFLICT = -1;

    // coordinate => prev(s)
    private Map<Coordinate, List<Coordinate>> coordinateTable;
    // coordinate => group at coordinate
    private Map<Coordinate, List<Integer>> groupOccupantTable;

    private Map<Node, int[]> agentDestinations;
    private static final int DEST_TIME_STEP = 0;
    private static final int DEST_GROUP = 1;

    private Conflict earliestConflictWhileAdding;

    public ConflictAvoidanceTable() {
        coordinateTable = new HashMap<>();
        groupOccupantTable = new HashMap<>();
        agentDestinations = new HashMap<>();
    }

    public int violation(SingleAgentState state) {
        SingleAgentState singleAgentState = state;
        Coordinate thisCoordinate = state.coordinate();
        Coordinate prevCoordinate = state.isRoot() ?
                null : ((SingleAgentState) state.predecessor()).coordinate();

        int result = coordinateConflict(thisCoordinate);
        if (result == NO_CONFLICT) {
            result = findTransposition(prevCoordinate, thisCoordinate);
        }
        if (result == NO_CONFLICT) {
            result = destinationConflict(thisCoordinate);
        }
        return result;
    }

    public int violation(MultiAgentState state) {
        int result = NO_CONFLICT;
        for (int i = 0; i < state.getSingleAgentStates().size() && result == NO_CONFLICT; i++) {
            SingleAgentState singleAgentState = state.getSingleAgentStates().get(i);
            result = violation(singleAgentState);
        }
        return result;
    }

    private int findTransposition(Coordinate previous, Coordinate coordinate) {
        int conflictingGroup = NO_CONFLICT;
        if (!(previous == null)) {
            coordinate.setTimeStep(coordinate.getTimeStep() - 1);
            previous.setTimeStep(previous.getTimeStep() + 1);
            int index = coordinateTable.get(previous).indexOf(coordinate);
            if (index != -1) {
                conflictingGroup = groupOccupantTable.get(previous).get(index);
            }
            coordinate.setTimeStep(coordinate.getTimeStep() + 1);
            previous.setTimeStep(previous.getTimeStep() - 1);
        }
        return conflictingGroup;
    }

    private int coordinateConflict(Coordinate coordinate) {
        return groupOccupantTable.get(coordinate).isEmpty() ?
                NO_CONFLICT : groupOccupantTable.get(coordinate).get(0);
    }

    private int destinationConflict(Coordinate coordinate) {
        int conflictingGroup = NO_CONFLICT;
        Node node = coordinate.getNode();
        if (agentDestinations.containsKey(node)) {
            int[] data = agentDestinations.get(node);
            if (data[DEST_TIME_STEP] <= coordinate.getTimeStep()) {
                conflictingGroup = data[DEST_GROUP];
            }
        }
        return conflictingGroup;
    }

    public void addPath(Path path, int group) {
        if (path.getLast() instanceof SingleAgentState) {
            path.forEach(state -> addSingleAgentStateCoordinate((SingleAgentState) state, group));
            SingleAgentState finalState = (SingleAgentState) path.getLast();
            addDestination(finalState.coordinate(), group);
        } else {
            path.forEach(state -> ((MultiAgentState) state).getSingleAgentStates()
                    .forEach(singleAgentState -> addSingleAgentStateCoordinate(singleAgentState, group)));
            MultiAgentState finalState = (MultiAgentState) path.getLast();
            finalState.getSingleAgentStates()
                    .forEach(singleAgentState -> addDestination(singleAgentState.coordinate(), group));
        }
    }

    public Conflict simulatePath(Path path, int group) {
        Conflict result = earliestConflictWhileAdding;

        final int TIME_LIMIT = earliestConflictWhileAdding != null ?
                earliestConflictWhileAdding.getTimeStep() : path.size();

        for (int time = 0; time < TIME_LIMIT && result == earliestConflictWhileAdding; time++) {
            MultiAgentState multiAgentState = (MultiAgentState) path.get(time);
            int violation = violation(multiAgentState);
            if (violation != NO_CONFLICT) {
                result = new Conflict(time, group, violation);
            }
        }
        return result;
    }

    public void addSingleAgentStateCoordinate(SingleAgentState singleAgentState, int group) {
        SingleAgentState pred = (SingleAgentState) singleAgentState.predecessor();
        Coordinate prev = singleAgentState.isRoot() ? null : pred.coordinate();
        addCoordinate(singleAgentState.coordinate(), prev, group);
    }

    public void addCoordinate(Coordinate coordinate, Coordinate prev, int group) {
        if (!coordinateTable.containsKey(coordinate)) {
            ArrayList<Coordinate> prevList = new ArrayList<>();
            prevList.add(prev);
            ArrayList<Integer> groupList = new ArrayList<>();
            groupList.add(group);
            coordinateTable.put(coordinate, prevList);
            groupOccupantTable.put(coordinate, groupList);
        } else {
            coordinateTable.get(coordinate).add(prev);
            groupOccupantTable.get(coordinate).add(group);
            int otherGroup = groupOccupantTable.get(coordinate).get(0);
            Conflict newConflict = new Conflict(coordinate.getTimeStep(), group, otherGroup);
            boolean earlier = earliestConflictWhileAdding == null
                            || newConflict.getTimeStep() < earliestConflictWhileAdding.getTimeStep();
            earliestConflictWhileAdding = earlier ? newConflict : earliestConflictWhileAdding;
        }
    }

    public void addDestination(Coordinate coordinate, int group) {
        agentDestinations.put(coordinate.getNode(),
                                new int[] {coordinate.getTimeStep(), group});
    }

    public String toString() {
        return "Coordinate table: " + coordinateTable + "\n"
                + "Groups table: " + groupOccupantTable + "\n"
                + "Agent destinations: " + agentDestinations;
    }
}
