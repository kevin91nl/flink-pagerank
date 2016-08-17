package com.datablogger

import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.scala._
import org.apache.flink.graph.library.PageRank
import org.apache.flink.graph.scala.Graph
import java.lang.{Double => JDouble}

import org.apache.flink.graph.spargel._
import org.apache.flink.graph._
import org.apache.flink.graph.utils.Tuple3ToEdgeMap

/**
  * Created by kevin on 17.08.16.
  */
object FlinkPageRank {

    val DAMPENING_FACTOR = 0.85

    def main(args: Array[String]): Unit = {
        // Setup the execution environment
        val env = ExecutionEnvironment.getExecutionEnvironment

//        val network = Graph.fromDataSet(edges, new MapFunction[String, JDouble]() {
//            override def map(value: String): JDouble = 1.0
//        }, env)
        val edges = env.readCsvFile[(String, String)]("/home/kevin/IdeaProjects/flink-pagerank/src/main/resources/example.csv")
            .map((tuple: (String, String)) => new Edge[String, JDouble](tuple._1, tuple._2, 1.0))

        val network = Graph.fromDataSet(edges, new MapFunction[String, JDouble]() {
            override def map(value: String): JDouble = 1.0
        }, env)

        val sumEdgeWeights = network
            .reduceOnEdges(new ReduceEdgesFunction[JDouble] {
                override def reduceEdges(firstEdgeValue: JDouble, secondEdgeValue: JDouble): JDouble = firstEdgeValue + secondEdgeValue
            }, EdgeDirection.OUT)

        val networkWithWeights = network.joinWithEdgesOnSource[JDouble](sumEdgeWeights,
            new EdgeJoinFunction[JDouble, JDouble] {
                override def edgeJoin(v1: JDouble, v2: JDouble): JDouble = v1.toDouble / v2.toDouble
            })

        print(networkWithWeights.getEdgesAsTuple3().collect())
        print(networkWithWeights.getVerticesAsTuple2().collect())

        val maxIterations = 100

        val parameters: ScatterGatherConfiguration = new ScatterGatherConfiguration
        parameters.setOptNumVertices(true)

        val pageRanks = networkWithWeights.runScatterGatherIteration[JDouble](new Scatterer, new Gatherer, maxIterations, parameters)
        print(pageRanks.getEdgesAsTuple3().collect())
        print(pageRanks.getVerticesAsTuple2().collect())

        // Execute
        env.execute("FlinkPageRank")
    }

    final class Scatterer extends ScatterFunction[String, JDouble, JDouble, JDouble] {

        override def sendMessages(vertex: Vertex[String, JDouble]) = {
            if (getSuperstepNumber() == 1) {
                vertex.setValue(1.0 / getNumberOfVertices.toDouble)
            }

            val it = getEdges().iterator()
            while (it.hasNext()) {
                val edge = it.next()
                sendMessageTo(edge.getTarget, vertex.getValue * edge.getValue)
            }
        }
    }

    final class Gatherer extends GatherFunction[String, JDouble, JDouble] {

        override def updateVertex(vertex: Vertex[String, JDouble], inMessages: MessageIterator[JDouble]) = {
            var rankSum = 0.0
            while (inMessages.hasNext()) {
                rankSum += inMessages.next().toDouble
            }

            val newValue = (DAMPENING_FACTOR * rankSum) + (1.0 - DAMPENING_FACTOR) / getNumberOfVertices.toDouble
            setNewVertexValue(newValue)
        }
    }

}
