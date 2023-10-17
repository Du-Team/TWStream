package edu.jsnu.sunjr;

import edu.jsnu.sunjr.utils.distance.DistanceMeasure;
import edu.jsnu.sunjr.utils.distance.impl.EuclideanDistance;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class MicroCluster {
    private int id;
    private int dim;
    private double[] center;
    private DistanceMeasure distanceMeasure;
    private boolean visited = false;
    private double weight;
    private boolean active;
    private int tu;
    private double distTmp;
    private String region;
    private Map<Integer, Double> probs;


    public MicroCluster(final int id, final Point p) {
        this.id = id;
        dim = p.dim;
        center = new double[dim];
        System.arraycopy(p.coord, 0, center, 0, dim);
        this.distanceMeasure = new EuclideanDistance();
        p.setMcId(id);
        weight = 1;
        active = false;
        tu = p.getArrivalTime();
        distTmp = 0;
        region = null;
        probs = null;
    }

    public void addProb(int clusterId, double prob){
        if (probs == null) {
            probs = new HashMap<>();
        }
        probs.put(clusterId, prob);
    }


    public boolean add(final double r, final double lambda, final Point p, final boolean decay) {
        boolean isMoved = false;
        p.setMcId(id);
        if (decay) {
            weight = getDecayedWeight(lambda, p.getArrivalTime());
        }

        if (distTmp <= r / 2) {
            double[] pCoord = p.getCoord();
            for (int i = 0; i < dim; i++) {
                center[i] = (weight * center[i] + pCoord[i]) / (weight + 1);
            }
            weight++;
            isMoved = true;
        } else {
            double v1 = -Math.pow(distTmp - r / 2, 2);
            double sigma = 0.5 * r / 3;
            double v2 = 2 * Math.pow(sigma, 2);
            weight += Math.exp(v1 / v2);
        }

        tu = p.getArrivalTime();

        return isMoved;
    }


    public void decayWeight(final double lambda, final int tc) {
        weight = getDecayedWeight(lambda, tc);
    }


    public double getDecayedWeight(final double lambda, final int tc) {
        return weight * getDecayCoeff(lambda, tc);
    }


    public double getDecayCoeff(final double lambda, final int tc) {
        return Math.pow(2, -lambda * (tc - tu));
    }

    public double getDistTo(final Point p) {
        return distanceMeasure.compute(center, p.coord);
    }

    public double getDistTo(final MicroCluster mc) {
        return distanceMeasure.compute(center, mc.center);
    }
}
