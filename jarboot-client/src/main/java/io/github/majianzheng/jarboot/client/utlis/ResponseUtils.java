package io.github.majianzheng.jarboot.client.utlis;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.api.exception.JarbootRunException;
import io.github.majianzheng.jarboot.common.pojo.ResultCodeConst;
import io.github.majianzheng.jarboot.common.utils.JsonUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

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
                    api, jsonNode.get("msg").asText(StringUtils.EMPTY));
            throw new JarbootRunException(msg);
        }
    }

    public static <T> List<T> getListResult(JsonNode response, Class<T> cls) {
        JsonNode result = ResponseUtils.parseResult(response, CommonConst.SERVICE_MGR_CONTEXT);
        List<T> list = new ArrayList<>();
        final int size = result.size();
        for (int i = 0; i < size; ++i) {
            JsonNode node = result.get(i);
            T serviceInstance = JsonUtils.treeToValue(node, cls);
            list.add(serviceInstance);
        }
        return list;
    }

    private ResponseUtils() {}
}
