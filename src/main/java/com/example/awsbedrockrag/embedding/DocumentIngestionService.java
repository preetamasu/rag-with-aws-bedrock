package com.example.awsbedrockrag.embedding;


import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DocumentIngestionService implements CommandLineRunner {

    @Value("classpath:/pdf/article_thebeatoct2024.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    private final EmbeddingModel embeddingModel;

    @Override
    public void run(String... args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources("classpath:/pdf/*.pdf");

        TextSplitter textSplitter = new TokenTextSplitter();

        List<Document> allDocs = new ArrayList<>();

        for(Resource resource1: resources){
            System.out.println("Reading:" + resource1.getFilename());
            TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource1);

            List<Document> chunks = textSplitter.split(tikaDocumentReader.read());

            List<Document> nonBlankChunks  = chunks.stream().filter(doc -> doc.getText()!=null && !doc.getText().isBlank()).toList();
            System.out.println("  -> " + nonBlankChunks.size() + " chunks");
            allDocs.addAll(nonBlankChunks );
        }

        System.out.println("Total chunks across all PDFs: " + allDocs.size());
        vectorStore.add(allDocs);

    }
}
