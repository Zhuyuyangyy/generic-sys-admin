# Generic-Sys-Admin: Causal-Inference-Driven Natural Language Business Flow Construction

## 1. Introduction

### 1.1 Problem Statement

Current low-code platforms (DingTalk Yida, Power Platform) and generic management frameworks (RuoYi, el-admin) suffer from fundamental limitations:

| System | Core Method | Fundamental Defect |
|--------|------------|-------------------|
| DingTalk Yida | Visual drag-drop | Cannot express causal dependency chains between business modules |
| Power Platform | Power Apps + Automate | Flows must be manually defined; cannot infer causal relationships from NL |
| RuoYi/el-admin | Code templates | Requires Java code for extensions; NL input cannot be converted to functional modules |
| LLM + Code Gen | NL → Code | Generated code is incoherent; cannot automatically discover implicit dependencies |

**Core Defect:** Existing systems only have "event triggering" (Event A happened → do Action B), not "causal inference" (If A then B because C). They cannot handle missing links in business flows.

### 1.2 Core Innovation: Causal Program Synthesis

This paper proposes a **Causal Program Synthesis** framework that:

1. Converts natural language business descriptions into **Causal Dependency Graphs**
2. Identifies **key decision nodes** in business flows via causal inference
3. Generates **executable causal programs** that automatically handle cross-module propagation
4. Provides **causal anomaly detection** to verify execution results against causal expectations

### 1.3 Contributions

**C1 (NL → Causal Graph):** First work to convert natural language business descriptions into causal dependency graphs, inferring both explicit and implicit causal relationships.

**C2 (Causal Key Node Identification):** Using causal inference to identify critical decision nodes in business flows—nodes where small changes have large downstream effects.

**C3 (Automatic Causal Program Generation):** Generate executable code from causal graphs, with automatic cross-module causal propagation handled by a runtime causal propagation engine.

**C4 (Causal Anomaly Detection):** Detect "causal anomalies" (effects that don't match causal expectations) rather than just statistical anomalies.

---

## 2. System Architecture

### 2.1 Processing Pipeline

```
User Input: Natural Language Business Description
    ↓
Causal Dependency Graph Builder
    ↓ (explicit + implicit causal edges)
Causal Dependency Graph (DAG)
    ↓
Causal Program Generator
    ↓ (Python/Django code or JSON workflow)
Causal Propagation Engine (Runtime)
    ↓
Causal Anomaly Detector
    ↓
User Dashboard: Causal链路图 + Execution Log + Anomaly Alerts
```

### 2.2 Core Data Structures

```python
class CausalDependencyGraphBuilder:
    """
    Builds causal dependency graph from natural language.
    Key innovation: infers implicit dependencies that users don't explicitly state.
    """
    
    def build_from_nl(self, nl_description: str) -> CausalDAG:
        # Step 1: Parse NL → extract business entities and actions
        parsed = self.parse_nl(nl_description)
        
        # Step 2: Identify causal connectors ("because", "so", "after", "leads to", "if...then...")
        explicit_edges = self.extract_causal_patterns(parsed)
        
        # Step 3: Infer implicit dependencies using domain ontology
        implicit_edges = self.infer_implicit_dependencies(parsed)
        
        # Step 4: Build causal DAG
        graph = self.construct_causal_dag(explicit_edges, implicit_edges)
        
        # Step 5: Detect circular dependencies
        cycles = self.detect_cycles(graph)
        if cycles:
            raise BusinessLogicError(f"Circular dependency detected: {' -> '.join(cycles[0])}")
        
        return graph

class CausalEdge:
    source: BusinessAction      # e.g., "PurchaseOrder.approval"
    target: BusinessAction     # e.g., "Inventory.increase"
    relation: str              # e.g., "causes", "enables", "prevents"
    source_type: str           # "explicit" or "implicit_from_ontology"
    condition: str             # e.g., "price > 5000"
    confidence: float          # inference confidence
```

---

## 3. Causal Inference Framework

### 3.1 Causal Dependency Graph Construction

Users describe surface-level business steps, not deep causal relationships. Example:

> User: "After approval, automatically generate purchase order; after purchase order confirmation, inventory increases; if inventory below safety stock, automatically generate replenishment warning"

Surface causal chain: approval → purchase_order → inventory_increase → warning

Deep causal chain (inferred):
```
approval → purchase_order (explicit)
purchase_order → purchase_status=confirmed (implicit: state transition)
purchase_status=confirmed → inventory_increase (implicit: goods received)
inventory_increase → inventory_level (implicit: level recalculation)
inventory_level < safety_stock → warning (explicit, conditional)
```

The **implicit edges** are the core innovation—they are inferred from the domain ontology, not stated by the user.

### 3.2 Domain Ontology for Implicit Dependency Inference

```python
class DomainOntology:
    """
    Predefined causal primitive templates for business entity types.
    Used to infer implicit causal successors.
    """
    
    CAUSAL_SUCCESSORS = {
        "PurchaseOrder": {
            "approval": [
                CausalSuccessor(entity_type="PurchaseOrder", action="status=confirmed", 
                               condition=None, confidence=0.95),
                CausalSuccessor(entity_type="Inventory", action="increase", 
                               condition="purchase_type='stock'", confidence=0.80),
                CausalSuccessor(entity_type="Finance", action="create_payable",
                               condition=None, confidence=0.90),
            ],
            "rejection": [
                CausalSuccessor(entity_type="PurchaseOrder", action="status=rejected",
                               condition=None, confidence=0.95),
            ],
        },
        "Inventory": {
            "increase": [
                CausalSuccessor(entity_type="Inventory", action="level_recalculate",
                               condition=None, confidence=0.99),
            ],
            "decrease": [
                CausalSuccessor(entity_type="Inventory", action="level_recalculate",
                               condition=None, confidence=0.99),
                CausalSuccessor(entity_type="Warning", action="generate_replenishment",
                               condition="level < safety_stock", confidence=0.85),
            ],
        },
    }
```

### 3.3 Causal Program Generation

The causal DAG is converted into an executable causal program:

```python
class CausalProgramGenerator:
    """
    Generates executable causal programs from causal DAGs.
    Key: automatically generates "listeners" and "triggers" for cross-module causal propagation.
    """
    
    def generate(self, causal_dag: CausalDAG, target_language="python") -> str:
        if target_language == "python":
            return self._generate_python(causal_dag)
    
    def _generate_python(self, dag: CausalDAG) -> str:
        code_lines = [
            "# Causal Program (Auto-Generated)",
            "# Generation time: " + datetime.now().isoformat(),
            "",
            "from django.db.models.signals import post_save",
            "from django.dispatch import receiver",
            "",
            "# ==================== Causal Listeners Auto-Registration ===================="
        ]
        
        for node in dag.topological_sort():
            entity_type = node.entity_type
            action = node.action
            
            # Generate signal listener
            if entity_type == "PurchaseOrder":
                model_class = "PurchaseOrder"
                signal = "post_save"
            elif entity_type == "Inventory":
                model_class = "Inventory"
                signal = "post_save"
            
            handler_body = self._generate_handler_body(node, dag)
            code_lines.append(f"""
@receiver({signal}, sender={model_class})
def causal_handler_{node.id}(sender, instance, **kwargs):
    \"\"\"Causal Trigger: {node.description}\"\"\"
    {handler_body}
""")
        
        return "\n".join(code_lines)
```

### 3.4 Causal Propagation Engine (Runtime)

```python
class CausalPropagationEngine:
    """
    Runtime causal propagation engine.
    Key innovation: causal propagation is "broadcast" not "trigger."
    When PurchaseOrder is confirmed, not only Inventory.update is triggered,
    but also Inventory_level recalculation → comparison with safety_stock →
    decision on replenishment warning.
    The entire causal chain propagates automatically without manual configuration at each step.
    """
    
    def on_entity_event(self, entity_type, entity_id, event_type, event_data):
        # Get current entity state
        entity_state = self._get_entity_state(entity_type, entity_id)
        
        # Start causal propagation
        self._propagate(
            source_entity=entity_type,
            source_id=entity_id,
            triggering_event=event_type,
            triggering_data=event_data,
            current_state=entity_state,
            chain_length=0,
            visited=set()
        )
    
    def _propagate(self, source_entity, source_id, triggering_event,
                   triggering_data, current_state, chain_length, visited):
        """
        Recursive causal propagation.
        visited set prevents circular propagation.
        """
        if chain_length >= self.max_chain_length:
            return  # Prevent infinite causal chains
        
        # Find all direct causal edges from this entity state
        causal_edges = self.program.dag.get_edges_from(source_entity, triggering_event)
        
        for edge in causal_edges:
            # Evaluate trigger condition
            if not self._evaluate_condition(edge.condition, current_state, triggering_data):
                continue
            
            # Execute causal effect
            effect_result = self._execute_effect(edge.target, current_state)
            
            # Recursively propagate (if target entity state changed)
            if effect_result["state_changed"]:
                self._propagate(
                    source_entity=edge.target.entity_type,
                    source_id=effect_result["entity_id"],
                    triggering_event=edge.target.action,
                    triggering_data=effect_result,
                    current_state=effect_result["new_state"],
                    chain_length=chain_length + 1,
                    visited=visited | {f"{edge.target.entity_type}:{edge.target.id}"}
                )
```

---

## 4. Key Algorithms

### 4.1 Causal Key Node Identification

Key decision nodes are identified using **causal influence measures**:

```
KeyNode_Score(v) = Σ_{downstream_node u} CausalStrength(v→u) × DownstreamImpact(u)

CausalStrength(v→u) = conf(v→u) × necessary(v, u)^0.5
necessary(v, u) = P(u | do(v̄)) / P(u)  # necessity of cause
```

Nodes with high KeyNode_Score are:
- High-leverage points where interventions have large effects
- Critical for business process robustness
- Candidates for additional monitoring/error handling

### 4.2 Causal Anomaly Detection

Traditional anomaly detection finds statistical outliers. Causal anomaly detection finds **causal deviations**:

```python
class CausalAnomalyDetector:
    """
    Detects causal anomalies: execution results that don't match causal expectations.
    Traditional methods: detect statistical anomalies (deviation from mean).
    This invention: detect causal anomalies (actual effect doesn't match expected causal chain).
    
    Example: PurchaseOrder confirmed but inventory didn't increase → causal anomaly
    (not a statistical anomaly)
    """
    
    def detect_causal_anomaly(self, execution_log):
        anomalies = []
        
        for entry in execution_log:
            if entry["type"] != "CAUSAL_EFFECT":
                continue
            
            edge = self.program.dag.get_edge_by_description(entry["trigger"], entry["effect"])
            expected_state = edge.target.get_expected_state()
            actual_state = self._get_actual_state(edge.target)
            
            if not self._states_match(expected_state, actual_state):
                anomalies.append({
                    "type": "MISSING_EFFECT",
                    "trigger": entry["trigger"],
                    "expected": expected_state,
                    "actual": actual_state,
                    "causal_path": edge.causal_path
                })
        
        return anomalies
```

---

## 5. Comparison with Existing Approaches

| Capability | Traditional Low-Code | LLM + Code Gen | Generic-Sys-Admin (Causal) |
|------------|---------------------|----------------|---------------------------|
| NL Business Description | Single-point operations | Incoherent code | Complete causal chains |
| Inter-Module Dependencies | Manual configuration | Not discovered | Automatically inferred |
| Causal Propagation | Manual trigger编写 | Not handled | Automatic broadcast |
| Anomaly Detection | Statistical outliers | None | Causal anomalies |
| Explainability | Low (black-box) | Medium | High (causal graph) |
| Missing Link Handling | Requires manual config | Cannot | Automatic inference |

---

## 6. Conclusion

This work introduces **causal inference** into business process construction. By treating business descriptions as causal programs rather than event sequences, we can:

1. **Infer** implicit dependencies that users don't explicitly state
2. **Generate** executable causal programs that handle cross-module propagation automatically
3. **Identify** key decision nodes where interventions have the largest effects
4. **Detect** causal anomalies where execution results don't match causal expectations

The framework transforms natural language business requirements into executable causal programs, bridging the gap between what users describe and what systems actually execute.

---

*Document version: v1.0*
*Generation time: 2026-05-16*