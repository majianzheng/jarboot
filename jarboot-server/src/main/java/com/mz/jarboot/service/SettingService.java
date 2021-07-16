package com.mz.jarboot.service;

import com.mz.jarboot.dto.*;

public interface SettingService {

    ServerSettingDTO getServerSetting(String server);

    void submitServerSetting(String server, ServerSettingDTO setting);

    GlobalSettingDTO getGlobalSetting();

    void submitGlobalSetting(GlobalSettingDTO setting);

    String getVmOptions(String server, String file);

    void saveVmOptions(String server, String file, String content);
}
