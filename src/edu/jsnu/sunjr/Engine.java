package edu.jsnu.sunjr;

import edu.jsnu.sunjr.graph.Edge;
import edu.jsnu.sunjr.graph.Graph;
import edu.jsnu.sunjr.graph.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @Author sunjr
 * @Date 2022/7/9/0009 22:23
 * @Version 1.0
 */
@Getter
@Setter
public class Engine {
    private int dim;
    private int k;
    private int alpha;
    private Graph augmentedKnnGraph;
    private List<Vertex> nodes;
    private double tau;
    private double lambda;
    private double minWeight;
    private int tu;

    public Engine(final int dim, final int k, final int alpha, final double lambda, final double tau, final double minWeight) {
        this.dim = dim;
        this.k = k;
        this.alpha = alpha;
        augmentedKnnGraph = new Graph(k);
        nodes = new ArrayList<>();
        this.lambda = lambda;
        this.tau = tau;
        this.minWeight = minWeight;
        tu = 0;
    }

    public int size() {
        return nodes.size();
    }

    public void add(MicroCluster mc) {
        Vertex v = new Vertex(mc);
        addVertex2Graph(v);
        nodes.add(v);
    }

    public void checkActive(final OutlierPool outlierPool) {
        Iterator<Vertex> vIter = nodes.iterator();
        while (vIter.hasNext()) {
            Vertex v = vIter.next();
            MicroCluster mc = v.getMc();
            if (mc.getWeight() < minWeight) {
                vIter.remove();
                mc.setActive(false);
                outlierPool.add(mc);
                deleteVertexFromGraph(v);
            }
        }
    }

    public void updateGraph(MicroCluster mc) {
        Vertex v = new Vertex(mc);
        nodes.remove(v);
        deleteVertexFromGraph(v);
        addVertex2Graph(v);
        nodes.add(v);
    }

    private void addVertex2Graph(Vertex v) {
        List<Edge> newEdgeList = new ArrayList<>();
        if (augmentedKnnGraph.isEmpty()) {
            augmentedKnnGraph.putEntry(v, newEdgeList);
            return;
        }

        for (Vertex node : nodes) {
            List<Edge> edgeList = augmentedKnnGraph.edgeList(node);
            Edge newEdge = null;
            if (edgeList.size() < alpha * k) {
                newEdge = new Edge(node, v);
                int loc1 = augmentedKnnGraph.insertEdge(edgeList, newEdge);
                if (edgeList.size() <= k) {
                    if (loc1 == edgeList.size() - 1) {
                        node.setRadius(newEdge.getLength());
                    }
                } else {
                    if (loc1 < k) {
                        node.setRadius(edgeList.get(k - 1).getLength());
                    }
                }
            } else {
                double diffDist = node.getMc().getDistTmp() - v.getMc().getDistTmp();
                double distTo2KthNN1 = edgeList.get(alpha * k - 1).getLength();
                if (diffDist < distTo2KthNN1) {
                    newEdge = new Edge(node, v);
                    if (newEdge.getLength() < distTo2KthNN1) {
                        int loc1 = augmentedKnnGraph.insertEdge(edgeList, newEdge);
                        edgeList.remove(alpha * k);
                        if (loc1 < k) {
                            node.setRadius(edgeList.get(k - 1).getLength());
                        }
                    }
                }
            }

            if (newEdgeList.size() < alpha * k) {
                if (newEdge == null) {
                    newEdge = new Edge(node, v);
                }
                int loc2 = augmentedKnnGraph.insertEdge(newEdgeList, newEdge);
                if (newEdgeList.size() < k) {
                    if (loc2 == newEdgeList.size() - 1) {
                        v.setRadius(newEdge.getLength());
                    }
                } else {
                    if (loc2 < k) {
                        v.setRadius(newEdgeList.get(k - 1).getLength());
                    }
                }
            } else {
                double diffDist = node.getMc().getDistTmp() - v.getMc().getDistTmp();
                double distTo2KthNN2 = newEdgeList.get(alpha * k - 1).getLength();
                if (newEdge == null) {
                    if (diffDist < distTo2KthNN2) {
                        newEdge = new Edge(node, v);
                        if (newEdge.getLength() < distTo2KthNN2) {
                            int loc2 = augmentedKnnGraph.insertEdge(newEdgeList, newEdge);
                            newEdgeList.remove(alpha * k);
                            if (loc2 < k) {
                                v.setRadius(newEdgeList.get(k - 1).getLength());
                            }
                        }
                    }
                } else {
                    if (newEdge.getLength() < distTo2KthNN2) {
                        int loc2 = augmentedKnnGraph.insertEdge(newEdgeList, newEdge);
                        newEdgeList.remove(alpha * k);
                        if (loc2 < k) {
                            v.setRadius(newEdgeList.get(k - 1).getLength());
                        }
                    }
                }
            }
        }

        augmentedKnnGraph.putEntry(v, newEdgeList);
    }

    private void deleteVertexFromGraph(Vertex v) {
        augmentedKnnGraph.remove(v);
        for (Vertex node : nodes) {
            List<Edge> edgeList = augmentedKnnGraph.edgeList(node);
            Iterator<Edge> edgeIter = edgeList.iterator();
            while (edgeIter.hasNext()) {
                Edge edge = edgeIter.next();
                if (edge.anotherVertex(node).equals(v)) {
                    edgeIter.remove();
                    if (edge.getLength() <= node.getRadius()) {
                        int size = edgeList.size();
                        if (size == 0) {
                            node.setRadius(0);
                        } else {
                            if (size <= k) {
                                node.setRadius(edgeList.get(size - 1).getLength());
                            } else {
                                node.setRadius(edgeList.get(k - 1).getLength());
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private double computeEta_v1(Vertex v) {
        double eta = 0.0;
        HashMap<Quadrant, Integer> quadrants = new HashMap<>();
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        for (Edge e : edgeList) {
            if (e.getLength() <= r1) {
                Vertex neighbor = e.anotherVertex(v);
                Quadrant quadrant = new Quadrant();
                for (int d = 0; d < dim; d++) {
                    double diff = neighbor.getDimDiffTo(v, d);
                    if (diff >= 0) {
                        quadrant.add(1);
                    } else {
                        quadrant.add(0);
                    }
                }

                if (!quadrants.containsKey(quadrant)) {
                    quadrants.put(quadrant, 1);
                } else {
                    Integer num = quadrants.get(quadrant);
                    quadrants.put(quadrant, num + 1);
                }
            } else {
                break;
            }
        }

        double quadrantNumAll = Math.pow(2, dim);
        double mu = k / quadrantNumAll;
        for (int num : quadrants.values()) {
            eta += Math.pow(num - mu, 2);
        }
        double quadrantNumEmpty = quadrantNumAll - quadrants.size();
        eta += quadrantNumEmpty * Math.pow(mu, 2);
        eta /= quadrantNumAll;

        return eta;
    }

    private double computeEta_v2(Vertex v) {
        double eta = 0.0;
        HashMap<Quadrant, Integer> quadrants = new HashMap<>();
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        for (Edge e : edgeList) {
            if (e.getLength() <= r1) {
                Vertex neighbor = e.anotherVertex(v);
                Quadrant quadrant = new Quadrant();
                for (int d = 0; d < dim; d++) {
                    double diff = neighbor.getDimDiffTo(v, d);
                    if (diff >= 0) {
                        quadrant.add(1);
                    } else {
                        quadrant.add(0);
                    }
                }

                if (!quadrants.containsKey(quadrant)) {
                    quadrants.put(quadrant, 1);
                } else {
                    Integer num = quadrants.get(quadrant);
                    quadrants.put(quadrant, num + 1);
                }
            } else {
                break;
            }
        }

        double quadrantNumAll = Math.pow(2, dim);
        double mu = k / quadrantNumAll;
        for (int num : quadrants.values()) {
            eta += Math.pow(num - mu, 2);
        }

        eta /= k;

        return eta;
    }

    private double computeGamma_v1(Vertex v) {
        double gamma = 0.0;
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        for (int d = 0; d < dim; d++) {
            double gamma_d = 0.0;
            for (Edge e : edgeList) {
                if (e.getLength() <= r1) {
                    gamma_d += v.getDimDiffTo(e.anotherVertex(v), d);
                } else {
                    break;
                }
            }
            gamma += Math.abs(gamma_d);
        }
        return gamma;
    }

    private double computeGamma_v2(Vertex v) {
        double gamma = 0.0;
        double gamma_mean = 0.0;
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        List<Double> gamma_d_list = new ArrayList<>();
        for (int d = 0; d < dim; d++) {
            double gamma_d = 0.0;
            for (Edge e : edgeList) {
                if (e.getLength() <= r1) {
                    gamma_d += Math.abs(v.getDimDiffTo(e.anotherVertex(v), d));
                } else {
                    break;
                }
            }
            gamma_d_list.add(gamma_d);
            gamma_mean += gamma_d;
        }
        gamma_mean /= dim;

        for (double gamma_d : gamma_d_list) {
            gamma += Math.pow(gamma_d - gamma_mean, 2);
        }
        gamma /= dim;

        return gamma;
    }

    private double computeGamma_v3(Vertex v) {
        double gamma = 0.0;
        double gamma_mean = 0.0;
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        List<Double> gamma_d_list = new ArrayList<>();
        for (int d = 0; d < dim; d++) {
            double gamma_d_positive = 0.0;
            double gamma_d_negative = 0.0;
            for (Edge e : edgeList) {
                if (e.getLength() <= r1) {
                    Vertex neighbor = e.anotherVertex(v);
                    double diff = neighbor.getDimDiffTo(v, d);
                    if (diff >= 0) {
                        gamma_d_positive += diff;
                    } else {
                        gamma_d_negative += -diff;
                    }
                } else {
                    break;
                }
            }

            gamma_d_list.add(gamma_d_positive);
            gamma_d_list.add(gamma_d_negative);
            gamma_mean += gamma_d_positive;
            gamma_mean += gamma_d_negative;
        }
        gamma_mean /= gamma_d_list.size();

        for (double gamma_d : gamma_d_list) {
            gamma += Math.pow(gamma_d - gamma_mean, 2);
        }
        gamma /= gamma_d_list.size();
        gamma = Math.sqrt(gamma) / gamma_mean;

        return gamma;
    }

    private double computeGamma_v4(Vertex v) {
        double gamma_mean = 0.0;
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        List<Double> gamma_d_list = new ArrayList<>();
        for (int d = 0; d < dim; d++) {
            double gamma_d = 0.0;
            for (Edge e : edgeList) {
                if (e.getLength() <= r1) {
                    Vertex neighbor = e.anotherVertex(v);
                    double diff = neighbor.getDimDiffTo(v, d);
                    gamma_d += diff;
                } else {
                    break;
                }
            }

            gamma_d_list.add(gamma_d);
            gamma_mean += gamma_d;
        }
        gamma_mean /= dim;

        double gamma_1 = 0.0;
        for (double gamma_d : gamma_d_list) {
            gamma_1 += Math.pow(gamma_d - gamma_mean, 3);
        }
        gamma_1 /= dim;

        double gamma_2 = 0.0;
        for (double gamma_d : gamma_d_list) {
            gamma_2 += Math.pow(gamma_d - gamma_mean, 2);
        }
        gamma_2 /= dim;

        double gamma = gamma_1 / Math.pow(Math.sqrt(gamma_2), 3);

        return Math.abs(gamma);
    }

    private double computeRho(Vertex v) {
        double rho = 0.0;
        double r1 = v.getRadius();
        List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
        for (Edge e : edgeList) {
            if (e.getLength() <= r1) {
                double r2 = e.anotherVertex(v).getRadius();
                if (e.getLength() <= r2) {
                    rho += gaussianKernelFunc(e.getLength(), r2);
                }
            } else {
                break;
            }
        }

        return rho;
    }

    private void computePhi() {
        for (Vertex node : nodes) {
            double eta = computeEta_v2(node);
            double gamma = computeGamma_v1(node);
            double rho = computeRho(node);
            double density = rho * node.getMc().getWeight();
            node.setDensity(density);
            double phi = Double.MAX_VALUE;
            if (density > 0) {
                phi = eta * gamma / density;
            }
            node.setPhi(phi);
        }
    }

    private double gaussianKernelFunc(double dist, double sigma) {
        return Math.exp(-Math.pow(dist, 2) / Math.pow(sigma, 2));
    }

    class PhiComparator implements Comparator<Vertex> {
        @Override
        public int compare(Vertex o1, Vertex o2) {
            double diff = o1.getPhi() - o2.getPhi();
            if (diff < 0) {
                return 1;
            } else if (diff > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public Set<Cluster> threeWayClustering() throws ExecutionException, InterruptedException {

        Set<Cluster> clusterSet = new HashSet<>();

        if (nodes.isEmpty()) {
            System.out.println("Engine has no active micro-clusters!");
            return clusterSet;
        }

        computePhi();
        Collections.sort(nodes, new PhiComparator());

        List<Vertex> borderNodes = new ArrayList<>();
        List<Vertex> coreNodes = new ArrayList<>();
        int cut_index = (int) (nodes.size() * tau);
        double cut_phi = nodes.get(cut_index).getPhi();
        for (Vertex node : nodes) {
            if (node.getPhi() >= cut_phi) {
                node.setPeeled(true);
                borderNodes.add(node);
            } else {
                node.setPeeled(false);
                coreNodes.add(node);
            }

            node.setCheckLabel(false);
            node.setRegion(null);
            node.setRep(null);
            node.setLabels(null);
        }


        CoreClustering coreClustering = new CoreClustering(coreNodes, augmentedKnnGraph, clusterSet);
        coreClustering.run();


        BorderAssignInitial borderAssignInitial = new BorderAssignInitial(k, borderNodes, augmentedKnnGraph, clusterSet);
        borderAssignInitial.run();

        List<Vertex> unassignedborderNodes = borderAssignInitial.getUnassignedborderNodes();
        BorderAssignFurther borderAssignFurther = new BorderAssignFurther(unassignedborderNodes, augmentedKnnGraph, clusterSet);
        borderAssignFurther.run();

        return clusterSet;
    }


    public MicroCluster getNearestMC(final Point p, final double radius) {
        MicroCluster result = null;
        double minDist = Double.MAX_VALUE;
        for (Vertex node : nodes) {
            MicroCluster mc = node.getMc();
            double dist = mc.getDistTo(p);
            mc.setDistTmp(dist);
            if (dist <= radius && dist < minDist) {
                minDist = dist;
                result = mc;
            }
            periodicDecay(mc, p.getArrivalTime());
        }

        tu = p.getArrivalTime();
        return result;
    }


    private void periodicDecay(MicroCluster mc, final int tc) {
        double decayedWeight = mc.getWeight() * getDecayCoeff(tc);
        mc.setWeight(decayedWeight);
    }

    public double getDecayCoeff(final int tc) {
        return Math.pow(2, -lambda * (tc - tu));
    }
}
