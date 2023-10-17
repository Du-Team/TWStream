package edu.jsnu.sunjr;

import edu.jsnu.sunjr.graph.Edge;
import edu.jsnu.sunjr.graph.Graph;
import edu.jsnu.sunjr.graph.Vertex;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @Author sunjr
 * @Date 2022/7/15/0015 7:59
 * @Version 1.0
 *
 * <p>
 * borders assignment
 */
@Getter
@Setter
public class BorderAssignInitial implements Runnable {
    private int k;
    private List<Vertex> borderNodes;
    private Graph augmentedKnnGraph;
    private Set<Cluster> clusterSet;
    List<Vertex> unassignedborderNodes;  // 剩余未分配的边界微簇

    public BorderAssignInitial(int k, List<Vertex> borderNodes, Graph augmentedKnnGraph, Set<Cluster> clusterSet) {
        this.k = k;
        this.borderNodes = borderNodes;
        this.augmentedKnnGraph = augmentedKnnGraph;
        this.clusterSet = clusterSet;
        this.unassignedborderNodes = new ArrayList<>();
    }

    @Override
    public void run() {
//        System.out.println("-------" + Thread.currentThread().getName() + "正在操作BorderAssign-----------");
        for (Vertex v : borderNodes) {
            List<Edge> edgeList = augmentedKnnGraph.edgeList(v);
            if (edgeList.isEmpty()) {
                continue;
            }

            int coreNum = 0;
            Map<Cluster, Double> probs = new HashMap<>();  // 存储该边界对象的KNN近邻core中属于各簇的核心区域的概率
            double r1 = v.getRadius();
            for (Edge e : edgeList) {
                if (e.getLength() <= r1) {      // knn范围内
                    Vertex another = e.anotherVertex(v);
                    if (!another.isPeeled()) {
                        Cluster cluster = another.getLabels().get(0);
                        if (!probs.containsKey(cluster)) {
                            probs.put(cluster, 1.0);
                        } else {
                            Double cnt = probs.get(cluster);
                            probs.put(cluster, cnt + 1.0);
                        }
                        coreNum++;
                    }
                } else {
                    break;
                }
            }

            // 计算概率值
            if (coreNum > 0) {
                Iterator<Map.Entry<Cluster, Double>> entryIter = probs.entrySet().iterator();
                while (entryIter.hasNext()) {
                    Map.Entry<Cluster, Double> entry = entryIter.next();
                    Double p = entry.getValue();
                    p /= edgeList.size();
//                    if (edgeList.size() >= k) {
//                        p /= k;
//                    } else {
//                        p /= edgeList.size();
//                    }
                    entry.setValue(p);
                }


                // 1.若KNN中的所有近邻core只存在于某单个簇的核心区域 -> 依据概率大小将其分配到该簇的核心区域或边界区域
                int K = clusterSet.size(); // 簇数
                if (probs.size() == 1) {
                    Cluster cluster = (Cluster) probs.keySet().toArray()[0];
                    double p = probs.get(cluster);
                    if (p >= 1.0 / K) {
                        v.setRegion("core");
                    } else {
                        v.setRegion("border");
                    }
                    v.addLabel(cluster);
                    v.addProb(cluster.getId(), p);
                    cluster.add(v.getMc());
                } else {
                    // 2.若KNN中的近邻core来自多个簇的核心区域
                    List<Map.Entry<Cluster, Double>> entries = new ArrayList<>(probs.entrySet());
                    Collections.sort(entries, (e1, e2) -> (int) (e2.getValue() - e1.getValue()));  // 按概率值降序排序
                    List<Cluster> candidates = new ArrayList<>();  // 候选簇
                    candidates.add(entries.get(0).getKey());
                    for (int i = 1; i < entries.size(); i++) {
                        // 概率差异判断
                        double diff = entries.get(0).getValue() - entries.get(i).getValue();
                        if (diff < 1.0 / K) {
                            candidates.add(entries.get(i).getKey());
                        }
                    }

                    // a.若概率差异依然较大，可分配至概率最大的簇的核心区域
                    if (candidates.size() == 1) {
                        v.setRegion("core");
                        Cluster cluster = candidates.get(0);
                        v.addLabel(cluster);
                        v.addProb(cluster.getId(), probs.get(cluster));
                        cluster.add(v.getMc());
                    } else { // b.若概率差异较小，可分配至概率较大的几个簇的边界区域
                        v.setRegion("border");
                        for (Cluster cluster : candidates) {
                            v.addLabel(cluster);
                            v.addProb(cluster.getId(), probs.get(cluster));
                            cluster.add(v.getMc());
                        }
                    }
                }
            } else {
                // 若该边界对象的KNN近邻中无core（该边界对象只能是属于簇的边界区域，因为其距离核心区域太远了）
                unassignedborderNodes.add(v);
            }
        }
    }
}
