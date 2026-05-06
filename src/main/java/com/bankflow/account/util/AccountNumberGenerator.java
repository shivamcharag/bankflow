package com.bankflow.account.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component      // Spring Bean — one instance shared across app (Singleton pattern)
public class AccountNumberGenerator {

    // AtomicLong — thread safe counter (memory management concept!)
    // Regular long++ is NOT thread safe in concurrent environment
    private final AtomicLong counter = new AtomicLong(1000);

    public String generate() {
        // Format: BF + year + 8-digit counter
        // Example: BF20240000001001
        return String.format("BF%d%010d",
                LocalDateTime.now().getYear(),
                counter.incrementAndGet()
        );
    }
}