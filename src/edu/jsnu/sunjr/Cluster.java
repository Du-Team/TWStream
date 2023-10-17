package edu.jsnu.sunjr;

import edu.jsnu.sunjr.utils.distance.DistanceMeasure;
import edu.jsnu.sunjr.utils.distance.impl.EuclideanDistance;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class Cluster {
    protected int id;
    protected Set<MicroCluster> set = new HashSet<>();
    protected DistanceMeasure distanceMeasure;
    public static int autoIncrement = 0;   // 自增的簇id号

    public Cluster() {
        this.id = autoIncrement++;
        this.distanceMeasure = new EuclideanDistance();
    }

    public void add(final MicroCluster mc) {
        set.add(mc);
    }

    public boolean remove(final MicroCluster mc) {
        return set.remove(mc);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

}
