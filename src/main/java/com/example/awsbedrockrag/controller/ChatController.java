package com.example.awsbedrockrag.controller;

import com.example.awsbedrockrag.config.BedrockModelRegistrys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@CrossOrigin
@Tag(name="Chat")
public class ChatController {

    private final VectorStore vectorStore;


    private final BedrockModelRegistrys bedrockModelRegistrys;
    private final ChatModel chatModel;


    @Operation(
            description = "Post endpoint for manager",
            summary = "This is basically used to get the answer from different chat models",
            responses = {
                    @ApiResponse(
                    description = "Success",
                    responseCode = "200"
            ),
                    @ApiResponse(
                            description = "Internal-Error",
                            responseCode = "500"
                    )
            }
    )
    @PostMapping()
    public String ask(@RequestParam String model, @RequestBody String question){

        String modelId = bedrockModelRegistrys.bedrockModelRegistry().get(model);

        if (modelId == null) {
            throw new IllegalArgumentException("Unknown model: " + model);
        }

        List<Document> relevant = vectorStore.similaritySearch(SearchRequest.builder().query(question).build());


        String context = relevant.stream().map(Document::getText).collect(Collectors.joining("\n\n"));

         BedrockChatOptions options = BedrockChatOptions.builder()
                 .model(modelId)
                 .temperature(0.5)
                 .maxTokens(1000)
                 .build();



        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("Answer using only the context below. If the answer isn't in the context, say you don't know.\n\nContext:\n" + context),
                        new UserMessage(question)
                ),
                options
        );

        return chatModel.call(prompt).getResult().getOutput().getText();

    }
}
