package com.mz.jarboot.service.impl;

import com.mz.jarboot.api.pojo.JvmProcess;
import com.mz.jarboot.api.service.OnlineDebugService;
import com.mz.jarboot.base.AgentManager;
import com.mz.jarboot.utils.TaskUtils;
import com.mz.jarboot.utils.VMUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author majianzheng
 */
@Service
public class OnlineDebugServiceImpl implements OnlineDebugService {
    @Override
    public List<JvmProcess> getJvmProcesses() {
        List<JvmProcess> result = new ArrayList<>();
        Map<Integer, String> vms = VMUtils.getInstance().listVM();
        vms.forEach((k, v) -> {
            if (AgentManager.getInstance().isManageredServer(k)) {
                return;
            }
            JvmProcess process = new JvmProcess();
            process.setPid(k);
            process.setAttached(AgentManager.getInstance().isOnline(String.valueOf(k)));
            process.setFullName(v);
            //解析获取简略名字
            process.setName(parseFullName(v));
            result.add(process);
        });
        return result;
    }

    @Override
    public void attach(int pid, String name) {
        TaskUtils.attach(name, String.valueOf(pid));
    }

    private String parseFullName(String fullName) {
        int p = fullName.indexOf(' ');
        if (p > 0) {
            fullName = fullName.substring(0, p);
        }
        final int length = fullName.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = length - 1; i > -1; --i) {
            char c = fullName.charAt(i);
            if (' ' == c) {
                sb = new StringBuilder(i);
            } else if ('\\' == c || '/' == c || '.' == c) {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.reverse().toString();
    }
}
