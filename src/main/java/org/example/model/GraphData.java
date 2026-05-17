package org.example.model;

import java.util.List;
import java.util.Map;

public record GraphData(List<Map<String, Object>> nodes, List<Map<String, Object>> edges, List<RdfTriple> triples) {
}
