package com.example.awsbedrockrag.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class BedrockModelRegistrys {

    @Bean
    public Map<String,String> bedrockModelRegistry(){
        return Map.of("claude","us.anthropic.claude-haiku-4-5-20251001-v1:0",
                "openai","openai.gpt-oss-120b-1:0",
                "google-gemma","google.gemma-3-27b-it",
                "deepseek","deepseek.v3.2");
    }
}
