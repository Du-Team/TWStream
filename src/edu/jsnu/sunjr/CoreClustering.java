package edu.jsnu.sunjr;

import edu.jsnu.sunjr.graph.Edge;
import edu.jsnu.sunjr.graph.Graph;
import edu.jsnu.sunjr.graph.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * @Author sunjr
 * @Date 2022/7/15/0015 8:00
 * @Version 1.0
 *
 * <p>
 * 得到各簇的核心区域
 */
@Getter
@Setter
public class CoreClustering implements Runnable {
    private List<Vertex> coreNodes;
    private Graph augmentedKnnGraph;
    private Set<Cluster> clusterSet;

    public CoreClustering(List<Vertex> coreNodes, Graph augmentedKnnGraph, Set<Cluster> clusterSet) {
        this.coreNodes = coreNodes;
        this.augmentedKnnGraph = augmentedKnnGraph;
        this.clusterSet = clusterSet;
    }

    @Override
    public void run() {
//        System.out.println("-------" + Thread.currentThread().getName() + "正在操作CoreClustering-----------");
        if (coreNodes.size() == 1 && coreNodes.get(0).getLabels() == null) {
            Cluster newCluster = new Cluster();
            clusterSet.add(newCluster);
            coreNodes.get(0).setRegion("core");
            coreNodes.get(0).addLabel(newCluster);
            coreNodes.get(0).addProb(newCluster.getId(), 1);
            newCluster.add(coreNodes.get(0).getMc());
            return;
        }

        for (Vertex v : coreNodes) {
            if (v.isCheckLabel()) {
                continue;
            }
            v.setCheckLabel(true);
            if (v.getLabels() == null) {
                Cluster newCluster = new Cluster();
                clusterSet.add(newCluster);
                v.setRegion("core");
                v.addLabel(newCluster);
                v.addProb(newCluster.getId(), 1);
                newCluster.add(v.getMc());
            }
            Queue<Vertex> queue = new LinkedList<>();
            queue.add(v);
            while (!queue.isEmpty()) {                                   // 根据互KNN图扩展簇
                Vertex seed = queue.poll();
                double r1 = seed.getRadius();
                Cluster cluster = seed.getLabels().get(0);               // 自身的标签
                List<Edge> edgeList = augmentedKnnGraph.edgeList(seed);
                if (edgeList.isEmpty()) {
                    continue;
                }
                for (Edge e : edgeList) {
                    if (e.getLength() <= r1) {                               // 为knn
                        Vertex another = e.anotherVertex(seed);
                        double r2 = another.getRadius();
                        if (e.getLength() <= r2 && !another.isPeeled()) {    // 同时为反knn
                            if (!another.isCheckLabel()) {                   // 互近邻中未检查过label的核心点
                                another.setCheckLabel(true);
                                queue.add(another);
                                // 将互为kNN的核心点全部合并到同一个簇中去(簇的合并)
                                another.setRegion("core");
                                another.addLabel(cluster);
                                another.addProb(cluster.getId(), 1);
                                cluster.add(another.getMc());
                            }
                        }
                    }
                }
            }
        }
    }
}
