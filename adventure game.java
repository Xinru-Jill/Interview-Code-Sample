public class Pollack extends Hunter {
    int findobj= 0;// to indicate if we have find the object 0 - not found, 1 - object found, finish search
    LinkedList<Long> lst= new LinkedList<>();

    public void huntOrbOriginal(HuntState state) {
        if (state.distanceToOrb() == 0) return;
        long u= state.currentLocation();
        lst.add(u);
        for (NodeStatus neigh : state.neighbors())
            if (!lst.contains(neigh.getId())) {
                state.moveTo(neigh.getId());
                if (state.distanceToOrb() == 0) findobj= 1;
                lst.add(neigh.getId());
                huntOrb(state);
                if (findobj == 1) return;
                state.moveTo(u);
            }
    }

    @Override
    public void huntOrb(HuntState state) {
        if (state.distanceToOrb() == 0) return;
        long u= state.currentLocation();
        lst.add(u);
        List<NodeStatus> order= new ArrayList<>();
        for (NodeStatus neigh : state.neighbors()) order.add(neigh);
        Collections.sort(order, new Comparator<NodeStatus>() {
            @Override
            public int compare(NodeStatus o1, NodeStatus o2) {
                return o1.getDistanceToTarget() - o2.getDistanceToTarget();
            }
        });
        for (NodeStatus node : order) if (!lst.contains(node.getId())) {
            state.moveTo(node.getId());
            if (state.distanceToOrb() == 0) findobj= 1;
            lst.add(node.getId());
            huntOrb(state);
            if (findobj == 1) return;
            state.moveTo(u);
        }

    @Override
    public void scram(ScramState state) {
        getCoins(state);
        return;
    }

    public void getCoins(ScramState state) {
        Node exit= state.getExit();
        List<Node> path= Path.shortest(state.currentNode(), exit);

        while (true) {
            ArrayList<Double> goldDis= new ArrayList<>();
            ArrayList<Node> nodes= new ArrayList<>();

            for (Node node : state.allNodes()) if (node.getTile().gold() != 0) {
                nodes.add(node);
                List<Node> pathTogold= Path.shortest(state.currentNode(), node);
                double dist= Path.pathSum(pathTogold);
                double golds= node.getTile().gold();
                if (dist != 0) goldDis.add(golds / dist);
            }
            if (goldDis.isEmpty()) break;

            int ind= goldDis
                .indexOf(Collections.max(goldDis));

            Node max= nodes.get(ind);
            List<Node> pathMax= Path.shortest(state.currentNode(), max);
            List<Node> maxExit= Path.shortest(max, exit);
            goldDis.remove(ind);
            nodes.remove(ind);

            if (Path.pathSum(pathMax) + Path.pathSum(maxExit) <= state
                .stepsLeft()) {
                pathMax.remove(0);
                for (Node nei : pathMax) state.moveTo(nei);
                path= maxExit;
            } else break;
        }

        path.remove(0);
        for (Node nod : path) {
            state.moveTo(nod);
            for (Node nodenow : state.currentNode().getNeighbors()) {

                int nnd= nod.getEdge(nodenow).length;
                int nngold= nodenow.getTile().gold();
                int nodeOut= Path
                    .pathSum(path.subList(path.indexOf(nod), path.size()));

                if (nodeOut + nnd * 2 <= state.stepsLeft() && nngold != 0) {
                    state.moveTo(nodenow);
                    state.moveTo(nod);
                }
            }
        }
        return;
    }
}