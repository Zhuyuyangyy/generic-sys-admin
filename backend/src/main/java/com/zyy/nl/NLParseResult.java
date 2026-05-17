package com.zyy.nl;

import lombok.Data;
import java.util.List;

@Data
public class NLParseResult {
    private String intent;
    private String entityId;
    private String entityType;
    private boolean causalCheckPerformed;
    private CausalGraph causalGraph;
}
