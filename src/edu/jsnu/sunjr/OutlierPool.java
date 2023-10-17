package edu.jsnu.sunjr;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author sunjr
 * @Date 2022/7/9/0009 15:38
 * @Version 1.0
 */
@Getter
@Setter
public class OutlierPool {

    private List<MicroCluster> outliers;
    private int tolerantT;

    public OutlierPool(final int tolerantT) {
        outliers = new ArrayList<>();
        this.tolerantT = tolerantT;
    }


    public int size() {
        return outliers.size();
    }

    public boolean add(MicroCluster mc) {
        return outliers.add(mc);
    }

    public boolean remove(MicroCluster mc) {
        return outliers.remove(mc);
    }

    public MicroCluster getNearestMC(final Point p, final double radius) {
        MicroCluster result = null;
        double minDist = Double.MAX_VALUE;
        Iterator<MicroCluster> mcItr = outliers.iterator();
        while (mcItr.hasNext()) {
            MicroCluster mc = mcItr.next();
            if (p.getArrivalTime() - mc.getTu() > tolerantT) {
                mcItr.remove();
                continue;
            }

            double dist = mc.getDistTo(p);
            if (dist <= radius && dist < minDist) {
                minDist = dist;
                mc.setDistTmp(dist);
                result = mc;
            }
        }

        return result;
    }
}
