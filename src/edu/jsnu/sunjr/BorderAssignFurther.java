package edu.jsnu.sunjr;

import edu.jsnu.sunjr.graph.Graph;
import edu.jsnu.sunjr.graph.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * @Author sunjr
 * @Date 2023/7/27/0027 21:40
 * @Version 1.0
 */
@Getter
@Setter
public class BorderAssignFurther implements Runnable {
    private List<Vertex> unassignedborderNodes;
    private Graph augmentedKnnGraph;
    private Set<Cluster> clusterSet;

    public BorderAssignFurther(List<Vertex> unassignedborderNodes, Graph augmentedKnnGraph, Set<Cluster> clusterSet) {
        this.unassignedborderNodes = unassignedborderNodes;
        this.augmentedKnnGraph = augmentedKnnGraph;
        this.clusterSet = clusterSet;
    }

    @Override
    public void run() {
        if (unassignedborderNodes.size() > 0) {
            for (Vertex v : unassignedborderNodes) {
                assign(v);
            }
        }

    }

    /**
     * 未完成初始分配的边界微簇，需要根据已分配点来进一步完成分配(只能分配到簇的边界区域)
     *
     * @param v
     */
    private void assign(Vertex v) {
        // 分配到距离最近的核心区域点所在的簇的边界区域
        Vertex nearestCore = null;
        double minDist = Double.MAX_VALUE;
        for (Vertex vertex : augmentedKnnGraph.vertexes()) {
            if (vertex.equals(v) || !"core".equals(vertex.getRegion())) {
                continue;
            }
            double dist = vertex.getDistTo(v);
            if (dist < minDist) {
                minDist = dist;
                nearestCore = vertex;
            }
        }

        if (nearestCore != null) {
            v.setRegion("border");
            Cluster label = nearestCore.getLabels().get(0);
            v.addLabel(label);
            v.addProb(label.getId(), 0);
            label.add(v.getMc());
        }

        if (v.getLabels() == null) {
            System.out.println("border assignment failed!");
        }
    }
}
