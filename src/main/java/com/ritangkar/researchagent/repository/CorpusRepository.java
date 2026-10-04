package com.ritangkar.researchagent.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritangkar.researchagent.model.SourceDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Repository
public class CorpusRepository {

    private final List<SourceDocument> documents;

    public CorpusRepository() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = new ClassPathResource("corpus/documents.json").getInputStream()) {
            SourceDocument[] docs = mapper.readValue(in, SourceDocument[].class);
            this.documents = List.of(docs);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the bundled demo corpus", e);
        }
    }

    public List<SourceDocument> findAll() {
        return documents;
    }
}
