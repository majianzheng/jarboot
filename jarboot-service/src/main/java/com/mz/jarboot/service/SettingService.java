package com.mz.jarboot.service;

import com.mz.jarboot.dto.*;

import java.util.List;

public interface SettingService {

    ServerSettingDTO getServerSetting(String server);

    void submitServerSetting(String server, ServerSettingDTO setting);

    GlobalSettingDTO getGlobalSetting();

    void submitGlobalSetting(GlobalSettingDTO setting);
}
