package com.zyy.causal;

import java.util.Objects;

/**
 * 因果图有向边。
 *
 * @author ZYY Agent
 * @since Java 17
 */
public class CausalEdge {

    private final String fromId;
    private final String toId;
    private final CausalMechanism mechanism;

    public CausalEdge(String fromId, String toId, CausalMechanism mechanism) {
        this.fromId = fromId;
        this.toId = toId;
        this.mechanism = mechanism;
    }

    public String getFromId()  { return fromId; }
    public String getToId()    { return toId; }
    public CausalMechanism getMechanism() { return mechanism; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CausalEdge that = (CausalEdge) o;
        return Objects.equals(fromId, that.fromId)
                && Objects.equals(toId, that.toId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId);
    }

    @Override
    public String toString() {
        return "CausalEdge{" + fromId + " --[" + mechanism + "]--> " + toId + "}";
    }
}
