package edu.jsnu.sunjr;

import edu.jsnu.sunjr.utils.FileWriterUtil;
import edu.jsnu.sunjr.utils.ValidateUtil;

import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

/**
 * @Author sunjr
 * @Date 2022/6/18/0018 14:14
 * @Version 1.0
 * <p>
 * "TWStream: Three-Way Stream Clustering"
 */

@Getter
@Setter
public class TWStream {
    private int streamSpeed = 1;
    private int dim;
    private double lambda;
    private double radius;
    private double beta;
    private int gapTime;
    private int k;
    private int alpha;
    private double tau;
    private double minWeight;

    private int mcId = 0;
    private static OutlierPool outlierPool;
    private static Engine engine;
    private static Set<Cluster> clusterSet;

    public static void main(String[] args) {
        String simpleClassName = TWStream.class.getSimpleName();
        String inputFilePath = "data/DS2-2.csv";
        String outputDirPath = ".";

        int dim = 2;
        int dataUse = 220000;
        int outputTimeInterval = 5000;
        int gapTime = 100;
        int alpha = 2;

        double radius = 0.02;
        double lambda = 0.0028;
        double beta = 1 - Math.pow(2, -lambda) + 0.0001;
        int k = 9;
        double tau = 0.7;

        TWStream ftwStream = new TWStream(dim, lambda, radius, beta, gapTime, k, alpha, tau, dataUse, outputTimeInterval);
        long start = System.currentTimeMillis();
        ftwStream.process(inputFilePath, outputDirPath, dataUse, outputTimeInterval);
        long end = System.currentTimeMillis();
        System.out.println(simpleClassName + " clustering execute over! >>> total cost time(ms): " + (end - start));
    }

    public TWStream(int dim, double lambda, double radius, double beta, int gapTime, int k, int alpha, double tau, int dataUse, int outputTimeInterval) {
        this.dim = dim;
        this.lambda = lambda;
        this.radius = radius;
        this.beta = beta;
        this.gapTime = gapTime;
        minWeight = beta / (1 - Math.pow(2, -lambda));
        outlierPool = new OutlierPool(gapTime);
        this.k = k;
        this.alpha = alpha;
        this.tau = tau;
        engine = new Engine(dim, k, alpha, lambda, tau, minWeight);

        ValidateUtil.validateParams_FTWStream(dim, lambda, radius, beta, minWeight, gapTime, k, alpha, dataUse, outputTimeInterval);
    }

    public void process(String inputFilePath, String outputDirPath, int dataUse, int outputTimeInterval) {
        String separator = System.getProperty("file.separator");

        StringBuilder builder = new StringBuilder();
        builder.append(outputDirPath).append(separator).append("point2MC").append(".csv");
        String point2MCFilePath = builder.toString();

        builder.setLength(0);
        builder.append(outputDirPath).append(separator).append("MC2Cluster");
        String MC2ClusterFilePath = builder.toString();

        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader(inputFilePath));
            bw = new BufferedWriter(new FileWriter(point2MCFilePath));

            long start = System.currentTimeMillis();
            int pointId = 0;
            int tc = 0;
            String line = null;
            while ((line = br.readLine()) != null && tc < dataUse) {
                StringTokenizer stk = new StringTokenizer(line, ",");
                double[] pointCoord = new double[dim];
                for (int i = 0; i < dim; i++) {
                    pointCoord[i] = Double.parseDouble(stk.nextToken());
                }

                Point p = new Point(pointId, tc, pointCoord);

                incrementalUpdate(p);

                // point -> micro-cluster
                bw.write(p.getId() + "," + p.getMcId() + "\n");

                // micro-cluster -> cluster
                if (pointId > 0 && pointId % outputTimeInterval == 0) {
                    clusterSet = engine.threeWayClustering();
                    String resFilePath = MC2ClusterFilePath + pointId + ".csv";
                    FileWriterUtil.MC2Cluster_FTWStream(resFilePath, clusterSet, radius);
                    long end = System.currentTimeMillis();
                    System.out.println("clustering 0 - " + pointId + " >>> cost time(ms): " + (end - start));
                    System.out.println("---------------------------------------------------------------------");
                }

                // control the stream speed
                if (pointId > 0 && pointId % streamSpeed == 0) {
                    tc++;
                }
                pointId++;
            }


            clusterSet = engine.threeWayClustering();
            String resFilePath = MC2ClusterFilePath + "final" + ".csv";
            FileWriterUtil.MC2Cluster_FTWStream(resFilePath, clusterSet, radius);

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void incrementalUpdate(Point p) {
        boolean updated = false;
        if (engine.size() > 0) {
            MicroCluster nearestMC = engine.getNearestMC(p, radius);
            if (nearestMC != null) {
                boolean isMoved = nearestMC.add(radius, lambda, p, false);
                if (isMoved) {
                    engine.updateGraph(nearestMC);
                }

                updated = true;
            }

            if (p.getArrivalTime() > 0 && p.getArrivalTime() % gapTime == 0) {
                engine.checkActive(outlierPool);
            }
        }

        if (!updated && outlierPool.size() > 0) {
            MicroCluster nearestMC = outlierPool.getNearestMC(p, radius);
            if (nearestMC != null) {
                nearestMC.add(radius, lambda, p, true);
                if (nearestMC.getWeight() >= minWeight) {
                    nearestMC.setActive(true);
                    outlierPool.remove(nearestMC);
                    engine.add(nearestMC);
                }
                updated = true;
            }
        }

        if (!updated) {
            MicroCluster mc = new MicroCluster(mcId++, p);
            outlierPool.add(mc);
        }
    }
}


