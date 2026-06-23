package com.zyy.nl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConfirmationStore {

    private static final long TTL_MINUTES = 5;

    private final ConcurrentHashMap<String, ConfirmationEntry> store = new ConcurrentHashMap<>();

    public String put(DryRunResult dryRunResult) {
        String confirmationId = dryRunResult.getConfirmationId();
        ConfirmationEntry entry = new ConfirmationEntry();
        entry.dryRunResult = dryRunResult;
        entry.expiresAt = LocalDateTime.now().plusMinutes(TTL_MINUTES);
        store.put(confirmationId, entry);

        log.debug("Confirmation stored - confirmationId={}, expiresAt={}", confirmationId, entry.expiresAt);

        return confirmationId;
    }

    public DryRunResult get(String confirmationId) {
        if (confirmationId == null) {
            return null;
        }

        ConfirmationEntry entry = store.get(confirmationId);
        if (entry == null) {
            return null;
        }

        if (LocalDateTime.now().isAfter(entry.expiresAt)) {
            store.remove(confirmationId);
            log.debug("Confirmation expired - confirmationId={}", confirmationId);
            return null;
        }

        return entry.dryRunResult;
    }

    public DryRunResult consume(String confirmationId) {
        DryRunResult result = get(confirmationId);
        if (result != null) {
            store.remove(confirmationId);
            log.debug("Confirmation consumed - confirmationId={}", confirmationId);
        }
        return result;
    }

    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<String, ConfirmationEntry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ConfirmationEntry> entry = it.next();
            if (now.isAfter(entry.getValue().expiresAt)) {
                it.remove();
            }
        }
    }

    private static class ConfirmationEntry {
        DryRunResult dryRunResult;
        LocalDateTime expiresAt;
    }
}
