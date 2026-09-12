package com.realestate.shared.application;

import java.util.*;

public final class Data {
    private Data() {}

    public static Map<String, Object> map(Object... pairs) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2)
            if (pairs[i + 1] != null) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
