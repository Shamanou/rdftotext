package com.sequenomics.rdftotext.services;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

@Service
public class RdfQueryService {
    private final Logger logger = LoggerFactory.getLogger(RdfQueryService.class);
    private StringBuilder stringBuilder;
    private ArrayList<String> previousUris = new ArrayList<>();

    public String rdfToText(String rdf) throws IOException {
        stringBuilder = new StringBuilder();

        final Model model = ModelFactory.createDefaultModel();
        model.read(IOUtils.toInputStream(rdf, "UTF-8"), null, "TTL");

        getStringsFromModel(model);

        return stringBuilder.toString();
    }

    private void getStringsFromModel(Model model) {
        StmtIterator stmts = model.listStatements(null, null, (RDFNode) null);
        iterateAndGetStrings(stmts);
    }

    private void getStringsFromModel(Model model, RDFNode rdfNode) {
        StmtIterator stmts = model.listStatements(rdfNode.asResource(), null, (RDFNode) null);
        iterateAndGetStrings(stmts);
    }

    private void iterateAndGetStrings(StmtIterator stmts) {
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource predicate = stmt.getPredicate();
            if (predicate != null) {
                if (predicate.isURIResource() && !previousUris.contains(predicate.getURI())) {
                    previousUris.add(predicate.getURI());
                    getLabelsFromURIResource(predicate);
                    logger.info("getting uri: " + predicate.getURI());
                } else if (predicate.isLiteral()) {
                    stringBuilder.append(predicate.asLiteral().getString()).append(" ");
                    logger.info(stringBuilder.toString());
                }
            }
            RDFNode object = stmt.getObject();
            if (object != null) {
                if (object.isURIResource() && !previousUris.contains(object.asResource().getURI())) {
                    previousUris.add(object.asResource().getURI());
                    getLabelsFromURIResource(object.asResource());
                    logger.info("getting uri: " + object.asResource().getURI());
                } else if (object.isLiteral()) {
                    stringBuilder.append(object.asLiteral().getString()).append(" ");
                    logger.info(stringBuilder.toString());
                }
            }
        }
    }

    private void getLabelsFromURIResource(Resource rdfNode) {
        final Model model = ModelFactory.createDefaultModel();
        try {
            URL url = new URL(rdfNode.getURI());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            String contentType = connection.getContentType();
            String format = null;

            if (contentType.contains(RDFFormat.TURTLE.getDefaultMIMEType())) {
                format = "TTL";
            }
            if (contentType.contains(RDFFormat.JSONLD.getDefaultMIMEType())) {
                format = "JSON-LD";
            }
            if (contentType.contains(RDFFormat.RDFXML.getDefaultMIMEType())) {
                format = "RDF/XML";
            }
            if (contentType.contains(RDFFormat.RDFJSON.getDefaultMIMEType())) {
                format = "RDF/JSON";
            }
            if (contentType.contains(RDFFormat.N3.getDefaultMIMEType())) {
                format = "N3";
            }
            if (contentType.contains(RDFFormat.NTRIPLES.getDefaultMIMEType())) {
                format = "NT";
            }

            if (format != null
                    && !rdfNode.getURI().contains("/owl")
                    && !rdfNode.getURI().contains("dc/terms")
                    && !rdfNode.getURI().contains("rdf-schema")
                    && !rdfNode.getURI().contains("rdf-syntax-ns")
                    && !rdfNode.getURI().contains("/dc/")) {
                logger.info("started writing text for " + rdfNode.toString());
                model.read(rdfNode.getURI() + "?output=xml", format);
                getStringsFromModel(model, rdfNode);
                previousUris.remove(rdfNode.getURI());
                stringBuilder.append("\n\n");
                logger.info("finished writing text for " + rdfNode.toString());
            }
        } catch (Exception exception) {
            logger.error(stringBuilder.toString(), exception);
        }
    }
}
