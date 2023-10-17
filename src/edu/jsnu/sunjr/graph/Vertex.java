package edu.jsnu.sunjr.graph;

import edu.jsnu.sunjr.Cluster;
import edu.jsnu.sunjr.MicroCluster;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author sunjr
 * @Date 2022/7/7/0007 13:38
 * @Version 1.0
 */
@Getter
@Setter
public class Vertex {
    private MicroCluster mc;
    private boolean visited;
    private boolean peeled;
    private double radius;
    private double phi;
    private double density;
    private List<Cluster> labels;
    private boolean checkLabel;
    private Vertex rep;

    public Vertex(MicroCluster mc) {
        this.mc = mc;
        visited = false;
        peeled = false;
        radius = 0;
        phi = 0;
        density = 0;
        labels = null;
        checkLabel = false;
        rep = null;
    }

    public void addLabel(Cluster c) {
        if (labels == null) {
            labels = new ArrayList<>();
        }
        labels.add(c);
    }

    public String getRegion() {
        return this.getMc().getRegion();
    }

    public void setRegion(String region) {
        this.getMc().setRegion(region);
    }

    public void addProb(int clusterId, double prob) {
        this.getMc().addProb(clusterId, prob);
    }

    public double getDistTo(Vertex v) {
        return mc.getDistTo(v.mc);

    }

    public double getDimDiffTo(Vertex v, int d) {
        MicroCluster mc1 = this.mc;
        MicroCluster mc2 = v.mc;
        return mc1.getCenter()[d] - mc2.getCenter()[d];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof Vertex) {
            Vertex v = (Vertex) obj;
            return mc.equals(v.mc);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mc.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.valueOf(mc.getId());
    }
}
