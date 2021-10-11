public class Pollack extends Hunter {
    // stage one
    int findobj= 0;// flag, indicate if we have find the object: 0 - not found, 1 - object found, finish search
    LinkedList<Long> lst= new LinkedList<>(); 

    public void huntOrbOriginal(HuntState state) {
        if (state.distanceToOrb() == 0) return;
        long u= state.currentLocation(); /* At every step, the hunter only knows his current tile's ID and the ID of all open neighbor tiles, <br>
        /* as well as the distance to the orb at each of these tiles (ignoring walls and obstacles). */
        /* get information about the current state, use functions, currentLocation(), neighbors(), and distanceToOrb() in HuntState (contained in someother file)).*/
        lst.add(u); // store current location into list lst
        for (NodeStatus neigh : state.neighbors()) // for every neighbor of this current location
            if (!lst.contains(neigh.getId())) { // if not visited
                state.moveTo(neigh.getId()); // move to the neighbor, now state object has changed
                if (state.distanceToOrb() == 0) findobj= 1; // judgement, find the object(treasure)
                lst.add(neigh.getId()); // add the neighbor (neigh) into lst
                huntOrb(state); // call huntOrb function
                if (findobj == 1) return; // when the hunter find the object(treasure), return
                state.moveTo(u); // move back to the original tile, one step before
            }
    }

    @Override // modified depth-first search, always search nearer nodes first
    public void huntOrb(HuntState state) { // state is the neighbor tile of u
        if (state.distanceToOrb() == 0) return;
        long u= state.currentLocation(); // local variable
        lst.add(u); // have paid a visit
        List<NodeStatus> order= new ArrayList<>(); // order array contains all the neighbors
        for (NodeStatus neigh : state.neighbors()) order.add(neigh);
        Collections.sort(order, new Comparator<NodeStatus>() {
            @Override // override the function sort, by returning the difference between to locations' distance to target
            public int compare(NodeStatus o1, NodeStatus o2) {
                return o1.getDistanceToTarget() - o2.getDistanceToTarget();
            }
        });
        for (NodeStatus node : order) if (!lst.contains(node.getId())) { // if not visited
            state.moveTo(node.getId()); // move to a neighboring tile by its ID
            if (state.distanceToOrb() == 0) findobj= 1;
            lst.add(node.getId()); // add to the visited list
            huntOrb(state); // recursive
            if (findobj == 1) return;
            state.moveTo(u); // u is the neighboring tile we should move to
        }
    // stage two
    @Override // get out of the cave before it collapse, trying to collect as much gold as possible along the way.
    public void scram(ScramState state) { // have access to the entire underground graph, accessed through ScramState.
        getCoins(state);
        return;
    }

    public void getCoins(ScramState state) { // to grab as much coins as possible, as long as enough steps remaining, after which exit through the shortest path.
        Node exit= state.getExit(); // get the exit position
        List<Node> path= Path.shortest(state.currentNode(), exit); // dijkstra shortest path algorithm, contained in Path class
        while (true) {
            ArrayList<Double> goldDis= new ArrayList<>();
            ArrayList<Node> nodes= new ArrayList<>(); // list of nodes containing gold

            for (Node node : state.allNodes()) if (node.getTile().gold() != 0) { // allNodes() returns a collection of all nodes on the graph.
                nodes.add(node);
                List<Node> pathTogold= Path.shortest(state.currentNode(), node); // from the current loc, to the gold position's shortest path
                double dist= Path.pathSum(pathTogold); // shortest path distance
                double golds= node.getTile().gold(); // quantity of gold
                if (dist != 0) goldDis.add(golds / dist); // count the efficiency
            }
            if (goldDis.isEmpty()) break;

            int ind= goldDis.indexOf(Collections.max(goldDis)); // most efficient node index in nodes and goldDis arrays
            Node max= nodes.get(ind); // get the most efficient node
            List<Node> pathMax= Path.shortest(state.currentNode(), max); // find the shortest path to the most efficient node, max
            List<Node> maxExit= Path.shortest(max, exit); // find the shortest path from the most efficient node to exit
            goldDis.remove(ind); // never consider node max again
            nodes.remove(ind);

            if (Path.pathSum(pathMax) + Path.pathSum(maxExit) <= state.stepsLeft()) { // if affordable
                pathMax.remove(0);
                for (Node nei : pathMax) state.moveTo(nei); // go to the most efficient node, max
                path= maxExit; // reconsider exiting while picking gold
            } else break;
        }

        path.remove(0); // path = maxExit, the way the hunter gets out, and no more efficient gold picking up
        for (Node nod : path) { 
            state.moveTo(nod); // go along the exiting path
            for (Node nodenow : state.currentNode().getNeighbors()) {
                int nnd= nod.getEdge(nodenow).length;
                int nngold= nodenow.getTile().gold();
                int nodeOut= Path.pathSum(path.subList(path.indexOf(nod), path.size()));

                if (nodeOut + nnd * 2 <= state.stepsLeft() && nngold != 0) { // afford to take extra steps and collect gold
                    state.moveTo(nodenow); // may not be efficient gold collection, like, small quantities
                    state.moveTo(nod); // back to exit path
                }
            }
        }
        return;
    }
}
