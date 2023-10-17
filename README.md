# TWStream
A Java implementation of "TWStream: Three-Way Stream Clustering".

## Introduction
TWStream is a two-stage clustering algorithm based on the three-way decision theory. The main contributions of this work can be summarized as follows:

- A density-based two-stage stream clustering algorithm is proposed. In the online stage, an augmented $k$nn graph is kept in memory. In the offline stage, it performs clustering based on boundary confidence and three-way decision theory, improving cluster quality.
- A boundary confidence concept is proposed to detect cluster boundaries efficiently and reveal potential cores of clusters. It integrates the skewness and sparsity of the data distribution, as well as the evolving trend of the stream.
- A micro-cluster-based three-way clustering strategy is proposed to reconstruct potential clusters effectively, which improves the clustering quality of boundary ambiguous clusters in a stream.

The purposed TWStream framework is outlined in Fig. \ref{fig:framework}.
TWStream consists of two stages with a total of five components that collaborate with each other to cluster data streams efficiently.
The functions of these five components are described as follows:

![framework](fig/framework.jpg?v=1&type=image)
Fig. 1: The framework of TWStream.


- **Data Stream Absorber:** It is responsible for receiving data objects from a stream and identifying which micro-clusters they belong to.
- **Outlier Pool:** It caches micro-clusters (inactive status) with weights below $W_{min}$, which may be reactivated in the future.
- **Graph Manager:** It maintains an augmented $k$nn graph incrementally to accelerate the update of the $k$nn graph.
- **Confidence Detector:** It detects cluster boundaries efficiently and reveals potential cores of clusters in a stream environment.
- **Three-Way Clustering Engine:** It reconstructs potential clusters employing a micro-cluster-based three-way clustering strategy effectively.

## Environment
- JDK 1.8
- Apache Maven 3.6.0