package com.realestate;

import static org.assertj.core.api.Assertions.*;

import com.realestate.shared.application.Changes;
import com.realestate.shared.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class DomainRulesTest {
    @Test
    void moneyRejectsFractionalPenniesAndOverflow() {
        assertThat(Rules.money(new BigDecimal("0.10").add(new BigDecimal("0.20")), true))
                .isEqualByComparingTo("0.30");
        assertThatThrownBy(() -> Rules.money(new BigDecimal("1.001"), false))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Rules.money(new BigDecimal("100000000000000000"), false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void patchesDistinguishAbsentAndExplicitNull() {
        var patch = new Changes(Map.of("is_VAT", false, "amount", 0));
        assertThat(patch.string("description", "kept")).isEqualTo("kept");
        assertThat(patch.bool("is_VAT", true)).isFalse();
        var values = new HashMap<String, Object>();
        values.put("description", null);
        assertThatThrownBy(() -> new Changes(values).only("description"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new Changes(Map.of("password", "secret")).only("name"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void invalidDatesAndWeakPasswordsAreRejected() {
        assertThatThrownBy(
                        () ->
                                Rules.dates(
                                        Instant.parse("2026-02-01T00:00:00Z"),
                                        Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> Rules.password("password")).isInstanceOf(BusinessException.class);
        assertThatCode(() -> Rules.password("ValidPassword123!")).doesNotThrowAnyException();
    }
}
