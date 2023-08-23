package io.github.majianzheng.jarboot.client.utlis;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

/**
 * @author majianzheng
 */
public class ResponseUtils {
    public static JsonNode parseResult(JsonNode jsonNode, String api) {
        checkResponse(api, jsonNode);
        JsonNode result = jsonNode.get(ClientConst.RESULT_KEY);
        if (null == result) {
            String msg = String.format("Request %s empty. response:%s", api, jsonNode);
            throw new JarbootRunException(msg);
        }
        return result;
    }

    public static void checkResponse(String api, JsonNode jsonNode) {
        if (null == jsonNode) {
            throw new JarbootRunException("Request failed!" + api);
        }
        final int resultCode = jsonNode.get(ClientConst.RESULT_CODE_KEY).asInt(ResultCodeConst.INTERNAL_ERROR);
        if (ResultCodeConst.SUCCESS != resultCode) {
            String msg = String.format("Request %s failed. resultMsg:%s",
                    api, jsonNode.get("").asText(StringUtils.EMPTY));
            throw new JarbootRunException(msg);
        }
    }

    private ResponseUtils() {}
}
