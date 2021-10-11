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
                state.moveTo(neigh.getId()); // add the current location into visited list lst
                if (state.distanceToOrb() == 0) findobj= 1; // find the object(treasure)
                lst.add(neigh.getId()); // add the neighbor (neigh) into lst
                huntOrb(state); // call huntOrb function
                if (findobj == 1) return; // when the hunter find the object(treasure), return
                state.moveTo(u); // move to a neighboring tile by its ID
            }
    }

    @Override // modified depth-first search, always search nearer nodes first
    public void huntOrb(HuntState state) {
        if (state.distanceToOrb() == 0) return;
        long u= state.currentLocation();
        lst.add(u);
        List<NodeStatus> order= new ArrayList<>();
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
            state.moveTo(u); // move to a neighboring tile by its ID
        }
    // stage two
    @Override // get out of the cave before it collapse, trying to collect as much gold as possible along the way.
    public void scram(ScramState state) { // have access to the entire underlying graph, accessed through ScramState.
        getCoins(state);
        return;
    }

    public void getCoins(ScramState state) { // to grab as much coins as possible as long as enough steps remaining, after which exit through the shortest path.
        Node exit= state.getExit(); // get the exit position
        List<Node> path= Path.shortest(state.currentNode(), exit); // dijkstra shortest path algorithm, contained in Path class

        while (true) {
            ArrayList<Double> goldDis= new ArrayList<>();
            ArrayList<Node> nodes= new ArrayList<>();

            for (Node node : state.allNodes()) if (node.getTile().gold() != 0) {
                nodes.add(node);
                List<Node> pathTogold= Path.shortest(state.currentNode(), node); // update dijkstra shortest path, contained in Path class
                double dist= Path.pathSum(pathTogold);
                double golds= node.getTile().gold();
                if (dist != 0) goldDis.add(golds / dist); // count the efficiency
            }
            if (goldDis.isEmpty()) break;

            int ind= goldDis.indexOf(Collections.max(goldDis)); // most efficient node

            Node max= nodes.get(ind);
            List<Node> pathMax= Path.shortest(state.currentNode(), max); // the shortest path to the most efficient node
            List<Node> maxExit= Path.shortest(max, exit); // the shortest path from the most efficient node to exit
            goldDis.remove(ind); // have visited the most efficient node
            nodes.remove(ind);

            if (Path.pathSum(pathMax) + Path.pathSum(maxExit) <= state.stepsLeft()) { // if affordable
                pathMax.remove(0);
                for (Node nei : pathMax) state.moveTo(nei); // go to the most efficient node
                path= maxExit; // we can exit now
            } else break;
        }

        path.remove(0);
        for (Node nod : path) { 
            state.moveTo(nod); // go along the exiting path
            for (Node nodenow : state.currentNode().getNeighbors()) {
                int nnd= nod.getEdge(nodenow).length;
                int nngold= nodenow.getTile().gold();
                int nodeOut= Path.pathSum(path.subList(path.indexOf(nod), path.size()));

                if (nodeOut + nnd * 2 <= state.stepsLeft() && nngold != 0) { // afford to take extra steps and collect gold
                    state.moveTo(nodenow); // may not be efficient gold collection, like, small quantities
                    state.moveTo(nod);
                }
            }
        }
        return;
    }
}
