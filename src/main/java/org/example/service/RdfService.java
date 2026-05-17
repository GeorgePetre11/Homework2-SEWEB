package org.example.service;

import org.apache.jena.rdf.model.*;
import org.example.model.GraphData;
import org.example.model.RdfTriple;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class RdfService {

    public GraphData parse(InputStream rdfXml) {
        Model model = ModelFactory.createDefaultModel();
        model.read(rdfXml, null, "RDF/XML");

        Set<String> nodeIds = new LinkedHashSet<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        List<RdfTriple> triples = new ArrayList<>();
        int edgeId = 0;

        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();

            String subjectLabel = shorten(subject.getURI(), model);
            nodeIds.add(subjectLabel);

            String predicateLabel = shorten(predicate.getURI(), model);

            String objectLabel;
            if (object.isResource()) {
                objectLabel = shorten(object.asResource().getURI(), model);
                nodeIds.add(objectLabel);
            } else {
                objectLabel = object.asLiteral().getString();
                nodeIds.add(objectLabel);
            }

            triples.add(new RdfTriple(subjectLabel, predicateLabel, objectLabel));

            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", edgeId++);
            edge.put("from", subjectLabel);
            edge.put("to", objectLabel);
            edge.put("label", predicateLabel);
            edges.add(edge);
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (String id : nodeIds) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", id);
            node.put("label", id);
            nodes.add(node);
        }

        return new GraphData(nodes, edges, triples);
    }

    private String shorten(String uri, Model model) {
        if (uri == null) return "blank";
        String prefix = model.shortForm(uri);
        if (!prefix.equals(uri)) return prefix;
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }
}
