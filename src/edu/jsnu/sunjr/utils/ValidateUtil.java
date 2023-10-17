package edu.jsnu.sunjr.utils;

/**
 * @Author sunjr
 * @Date 2022/4/21/0021 14:57
 * @Version 1.0
 */
public class ValidateUtil {

    public static void validateParams_FTWStream(int dim, double lambda, double radius, double beta, double minWeight, int gapTime, int k, int alpha, int dataUse, int outputTimeInterval) {
        System.out.println("dim=" + dim);
        System.out.println("lambda=" + lambda);
        System.out.println("radius=" + radius);
        System.out.println("beta=" + beta);
        System.out.println("minWeight=" + minWeight);
        System.out.println("gapTime=" + gapTime);
        System.out.println("k=" + k);
        System.out.println("alpha=" + alpha);
        System.out.println("dataUse=" + dataUse);
        System.out.println("outputTimeInterval=" + outputTimeInterval);

        if (dim <= 0 || lambda <= 0 || radius <= 0 || beta <= 0 || minWeight <= 0 || gapTime <= 0 || k <= 0 || alpha <= 0 || dataUse <= 0 || outputTimeInterval <= 0) {
            System.out.println("Parameter error, program exits!");
            System.exit(0);
        }
    }
}
