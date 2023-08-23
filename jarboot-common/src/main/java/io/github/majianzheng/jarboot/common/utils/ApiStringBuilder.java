package io.github.majianzheng.jarboot.common.utils;

import java.util.HashMap;

/**
 * @author majianzheng
 */
public class ApiStringBuilder {
    private final String api;
    private final HashMap<String, String> params = new HashMap<>(16);
    public ApiStringBuilder(String api) {
        this.api = api;
    }

    public ApiStringBuilder(String context, String api) {
        if (api.startsWith(StringUtils.SLASH)) {
            this.api = context + api;
        } else {
            this.api = context + StringUtils.SLASH + api;
        }
    }

    public ApiStringBuilder add(String param, String value) {
        params.put(param, value);
        return this;
    }

    public String build() {
        if (params.isEmpty()) {
            return api;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(api);
        final String questionMark = "?";
        final char andMark = '&';
        if (!api.endsWith(questionMark)) {
            sb.append(questionMark);
        }
        params.forEach((k, v) -> sb.append(k).append('=').append(v).append(andMark));
        return StringUtils.trimTrailingCharacter(sb.toString(), andMark);
    }
}
