package com.zyy.nl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HybridNLParser {

    private static final double CONFIDENCE_THRESHOLD = 0.6;

    @Autowired
    private NLParser ruleParser;

    private LLMNLParser llmParser;

    private boolean enableLLMFallback = true;

    private boolean llmAvailable = false;

    public HybridNLParser() {}

    public HybridNLParser(NLParser ruleParser, LLMNLParser llmParser) {
        this.ruleParser = ruleParser;
        this.setLlmParser(llmParser);
    }

    @Autowired
    public void setLlmParser(LLMNLParser llmParser) {
        this.llmParser = llmParser;
        this.llmAvailable = llmParser.isAvailable();
        if (llmAvailable) {
            log.info("HybridNLParser: LLM fallback enabled (DeepSeek API key found)");
        } else {
            log.info("HybridNLParser: LLM fallback disabled (no DeepSeek API key)");
        }
    }

    public void setLlmParserManual(LLMNLParser llmParser, boolean available) {
        this.llmParser = llmParser;
        this.llmAvailable = available;
    }

    public NLParser.ParseResult parse(String input) {
        NLParser.ParseResult ruleResult = ruleParser.parse(input);

        if (!enableLLMFallback || !llmAvailable
                || ruleResult.getConfidence() >= CONFIDENCE_THRESHOLD) {
            return ruleResult.withSource("RULE");
        }

        try {
            NLParser.ParseResult llmResult = llmParser.parse(input);
            return llmResult.withSource("LLM").withConfidence(0.95);
        } catch (Exception e) {
            log.warn("LLM fallback failed, degrading to rule result: {}", e.getMessage());
            return ruleResult.withSource("RULE_FALLBACK");
        }
    }

    public void setEnableLLMFallback(boolean enable) {
        this.enableLLMFallback = enable;
    }

    public boolean isEnableLLMFallback() {
        return enableLLMFallback;
    }

    public void setRuleOnly(boolean ruleOnly) {
        this.enableLLMFallback = !ruleOnly;
    }

    public String getCurrentMode() {
        return enableLLMFallback ? "HYBRID" : "RULE_ONLY";
    }

    public boolean isLlmAvailable() {
        return llmAvailable;
    }
}
