package com.cis.indoorlocalization

import java.util.PriorityQueue

class Vertex(val id: Char, var distance: Int) : Comparable<Vertex> {

    override fun compareTo(other: Vertex): Int {
        return when {
            this.distance < other.distance -> -1
            this.distance > other.distance -> 1
            else -> this.id.compareTo(other.id)
        }
    }

    override fun hashCode(): Int {
        return id.hashCode() * 31 + distance
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val vertex = other as Vertex
        return id == vertex.id && distance == vertex.distance
    }

    override fun toString(): String {
        return "Vertex(id=$id, distance=$distance)"
    }
}

class Graph {

    private val vertices = HashMap<Char, List<Vertex>>()

    fun addVertex(character: Char, vertex: List<Vertex>) {
        vertices[character] = vertex
    }

    fun getShortestPath(start: Char, finish: Char): List<Char> {
        val distances = HashMap<Char, Int>()
        val previous = HashMap<Char, Vertex?>()
        val nodes = PriorityQueue<Vertex>()

        for (vertex in vertices.keys) {
            if (vertex == start) {
                distances[vertex] = 0
                nodes.add(Vertex(vertex, 0))
            } else {
                distances[vertex] = Int.MAX_VALUE
                nodes.add(Vertex(vertex, Int.MAX_VALUE))
            }
            previous[vertex] = null
        }

        while (nodes.isNotEmpty()) {
            val smallest = nodes.poll()
            if (smallest.id == finish) {
                val path = ArrayList<Char>()
                var currentVertex: Vertex? = smallest
                while (currentVertex != null) {
                    path.add(currentVertex.id)
                    currentVertex = previous[currentVertex.id]
                }
                return path.reversed()
            }

            if (distances[smallest.id] == Int.MAX_VALUE) break

            for (neighbor in vertices[smallest.id] ?: emptyList()) {
                val alt = distances[smallest.id]!! + neighbor.distance
                if (alt < distances[neighbor.id]!!) {
                    distances[neighbor.id] = alt
                    previous[neighbor.id] = smallest

                    nodes.remove(neighbor)
                    neighbor.distance = alt
                    nodes.add(neighbor)
                }
            }
        }
        return ArrayList(distances.keys)
    }
}
