package io.github.majianzheng.jarboot.inner.controller;

import io.github.majianzheng.jarboot.api.constant.CommonConst;
import io.github.majianzheng.jarboot.common.notify.NotifyReactor;
import io.github.majianzheng.jarboot.common.pojo.AgentClient;
import io.github.majianzheng.jarboot.common.JarbootException;
import io.github.majianzheng.jarboot.common.PidFileHelper;
import io.github.majianzheng.jarboot.common.pojo.ResponseSimple;
import io.github.majianzheng.jarboot.common.protocol.CommandResponse;
import io.github.majianzheng.jarboot.common.utils.HttpResponseUtils;
import io.github.majianzheng.jarboot.common.utils.NetworkUtils;
import io.github.majianzheng.jarboot.common.utils.StringUtils;
import io.github.majianzheng.jarboot.event.AgentResponseEvent;
import io.github.majianzheng.jarboot.event.ServiceStartedEvent;
import io.github.majianzheng.jarboot.utils.SettingUtils;
import io.github.majianzheng.jarboot.utils.TaskUtils;
import io.github.majianzheng.jarboot.utils.CommonUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 内部接口，与jarboot-core交互，非开放
 * @author majianzheng
 */
@RequestMapping(value = CommonConst.AGENT_CLIENT_CONTEXT)
@Controller
public class AgentClientController {

    /**
     * 命令执行结果反馈接口
     * @param serviceName 服务名
     * @param sid sid
     * @param raw 执行结果数据
     * @return 处理结果
     */
    @PostMapping(value="/response")
    @ResponseBody
    public ResponseSimple onResponse(
            @RequestParam(required = false) String userDir,
            @RequestParam String serviceName,
            @RequestParam String sid,
            @RequestBody byte[] raw) {
        CommandResponse resp = CommandResponse.createFromRaw(raw);
        NotifyReactor
                .getInstance()
                .publishEvent(new AgentResponseEvent(userDir, serviceName, sid, resp, null));
        return HttpResponseUtils.success();
    }

    /**
     * 通知启动成功完成
     * @param serviceName 服务名
     * @param sid sid
     * @return 处理结果
     */
    @GetMapping(value="/setStarted")
    @ResponseBody
    public ResponseSimple setStarted(@RequestParam String userDir, @RequestParam String serviceName, @RequestParam String sid) {
        NotifyReactor
                .getInstance()
                .publishEvent(new ServiceStartedEvent(userDir, serviceName, sid));
        return HttpResponseUtils.success();
    }

    /**
     * 生成Agent客户端信息
     * @param code 加密码
     * @return Agent客户端信息
     */
    @PostMapping(value="/agentClient")
    @ResponseBody
    public AgentClient getAgentClientInfo(HttpServletRequest request, @RequestBody String code) {
        final int limit = 3;
        String[] array = code.split(CommonConst.COMMA_SPLIT, limit);
        if (limit != array.length) {
            throw new JarbootException("协议格式错误，无法找到分隔符！");
        }
        String pid = array[0];
        String instanceName = array[1];
        String command = array[2];

        AgentClient agentClient = new AgentClient();
        agentClient.setDiagnose(true);
        String server = StringUtils.isEmpty(command) ? ("NoName-" + pid) : TaskUtils.parseCommandSimple(command);
        agentClient.setServiceName(server);
        String clientAddr = CommonUtils.getActualIpAddr(request);
        agentClient.setClientAddr(clientAddr);
        final char atSplit = '@';
        int i = instanceName.indexOf(atSplit);
        String machineName = instanceName.substring(i);
        i = PidFileHelper.INSTANCE_NAME.indexOf(atSplit);
        String localMachineName = PidFileHelper.INSTANCE_NAME.substring(i);
        boolean isLocal = NetworkUtils.hostLocal(clientAddr) && localMachineName.equalsIgnoreCase(machineName);
        agentClient.setLocal(isLocal);
        if (isLocal) {
            agentClient.setSid(CommonUtils.createJvmSid(pid));
        } else {
            //先产生一个未知的hash然后再和nanoTime组合，减少sid重合的几率
            int h = Objects.hash(instanceName, System.nanoTime(), SettingUtils.getUuid());
            //远程进程，使用pid + ip + name + hash地址作为sid
            StringBuilder sb = new StringBuilder();
            sb
                    .append(CommonConst.REMOTE_SID_PREFIX)
                    .append(CommonConst.COMMA_SPLIT)
                    .append(pid)
                    .append(CommonConst.COMMA_SPLIT)
                    .append(clientAddr)
                    .append(CommonConst.COMMA_SPLIT)
                    .append(server)
                    .append(CommonConst.COMMA_SPLIT)
                    .append(String.format("%08x-%08x", h, System.nanoTime()));
            agentClient.setSid(sb.toString());
        }

        return agentClient;
    }
}
