// src/locales/zh-CN.js
export default {
  'navbar.lang': 'English',

  //Tab标题
  SERVICES_MGR: '服務管理',
  ONLINE_DEBUG: '線上診斷',
  AUTH_CONTROL: '權限控制',
  PLUGINS: '挿件',
  SETTING: '設置',
  ABOUT: '關於',
  MENU_DOCS: '文檔',
  TERMINAL: '终端',
  TOOLS: '工具',
  FILE_MGR: '文件管理',

  //服務管理
  ONE_KEY_START: '一鍵啟動',
  ONE_KEY_STOP: '一鍵停止',
  ONE_KEY_RESTART: '一鍵重啓',
  START: '啟動',
  STOP: '停止',
  RESTART: '重啓',
  UPLOAD_NEW: '上傳或新增',
  NAME: '名字',
  STATUS: '狀態',
  CLEAR: '清空',
  CLOSE: '關閉',
  GROUP: '組',
  TREE_VIEW: '樹',
  LIST_VIEW: '列錶',
  CONSOLE_VIEW: '控制台',
  SERVICES_CONF: '配置',
  GROUP_PLACEHOLDER: '輸入分組名，便於管理',
  CMD_PLACEHOLDER: '輸入調試命令執行',
  SELECT_UPLOAD_SERVER_TITLE: '輸入要更新或新增的服務的名稱',
  UPLOAD_STAGE_TITLE: '上傳{server}文件',
  UPLOAD_TIPS: '點擊或拖拽文件到此區域上傳',
  FILE_SIZE_OVER_TIPS: '文件大小必須小於{size}',
  COMMAND_PLACEHOLDER: '輸入命令，示例：help',
  MORE_SETTING_INFO: '更多的配置信息，請到服務配置頁面。',
  UPLOAD_ERROR: '更新服務失败！',
  UPLOAD_SUCCESS: '成功更新服務！',
  UPLOAD_HINT: '支持單個或批量上傳。',
  DELETE_INFO: '该操作將會徹底删除服務的相关信息，是否繼續？',
  UPLOAD_DESC: '點擊或拖拽文件到此區域上傳',
  LOCAL: '本地',
  REMOTE: '遠程',
  DEFAULT_GROUP: '默認組',
  DETACH_MSG: 'Detach将斷開遠程连接，斷開后将从列錶中移除，是否繼續？',
  //進程状态
  RUNNING: '運行中',
  STOPPED: '已停止',
  STARTING: '啟動中',
  STOPPING: '停止中',

  //通用
  TYPE: '類型',
  SUBMIT_BTN: '提交',
  RESET_BTN: '重置',
  SEARCH_BTN: '搜索',
  FILTER_BTN: '過濾',
  REFRESH_BTN: '刷新',
  NEXT_BTN: '下一步',
  DASHBOARD: '仪錶盘',
  SERVER_EMPTY: '當前工作空間下未搜索到服務。',
  MODIFY: '编辑',
  SAVE: '保存',
  DELETE: '删除',
  CREATE: '新增',
  SUCCESS: '成功！',
  LOADING: '加載中...',
  SUBMITTING: '提交中...',
  WARN: '警告',
  CANCEL: '取消',
  EXPORT: '導出',
  IMPORT: '導入',
  OPERATOR: '操作',
  IMPORT_INFO: `導入的服務{name}已经存在，是否覆蓋？`,
  UPLOAD_INFO: `上傳更新{name}前是否备份服務，以便部署出錯后恢复？`,
  START_UPLOAD_INFO: `開始上傳文件{name}...`,
  TRUSTED_SUCCESS: '获取授權成功！',
  UNTRUSTED_MODEL_BODY: `未知的遠程主机{host}，是否信任？`,
  TRUST_ONCE: '信任一次',
  TRUST_ALWAYS: '始终信任',
  TEXT_WRAP: '自動换行',
  AUTO_SCROLL_END: '自動滚動到底部',
  SCROLL_TO_TOP: '滚動到顶部',
  APP_TYPE: '应用类型',
  ADD_FILE: '新增文件',
  ADD_FOLDER: '新增資料夾',
  NOT_TEXT_FILE: '檔案不是文字類型，是否繼續編輯？',
  SAVE_CONFIG_AND_ENABLE_FILE: '保存配寘以便啟用文件管理',
  DOWNLOAD: '下载',
  FAILED: '失败',
  CREATE_TERM: '新建终端',
  USER_DIR: '用戶目錄',
  MODIFY_USER: '修改用戶',
  AVATAR: '头像',
  CLICK_MODIFY: '点击修改',
  PREVIEW: '预览',
  SELECT_AVATAR: '选择头像',
  UPLOAD_IMG: '上传图片',
  RE_UPLOAD_IMG: '重新上传',
  SIZE: '大小',
  MODIFY_TIME: '修改时间',
  COUNT: '数量',
  TIP_UPLOAD_IMG: '请上传图片',

  //服務配置
  SERVER_LIST_TITLE: '服務列錶',
  COMMAND_LABEL: '啟動命令',
  VM_OPT_LABEL: 'VM參數',
  MAIN_ARGS_LABEL: '程序传入參數',
  WORK_HOME_LABEL: '工作目錄',
  ENV_LABEL: '环境变量',
  PRIORITY_LABEL: '啟動優先級',
  DAEMON_LABEL: '進程守護',
  JAR_UPDATE_WATCH_LABEL: '文件路徑監控',
  COMMAND_EXAMPLE: '示例： 1) -jar xx.jar    2) MainClassName    3) -cp xx.jar *.*.MainClass mainMethod    4) -classpath **.jar *.*ClassName',

  //挿件
  UPLOAD_TITLE: '上傳挿件',
  FILE: '文件',
  UPLOAD_BUTTON: '點擊上傳',

  //全局配置
  SYSTEM_SETTING: '系統设置',
  SERVERS_PATH: '工作空間',
  DEFAULT_VM_OPT: '默認的VM參數',
  AUTO_START_AFTER_INIT: 'jarboot啟動后自動啟動服務',
  TRUSTED_HOSTS: '信任的服務器',
  EMPTY_INPUT_MSG: '輸入的內容为空！',
  DELETE_HOST_MSG: '是否要删除信任的服務器？',

  //用戶登錄
  USER_NAME: '用戶',
  FULL_NAME: '姓名',
  PASSWORD: '密碼',
  LOGIN: '登錄',
  RE_PASSWORD: '確認密碼',
  MODIFY_PWD: '修改密碼',
  CREATE_USER: '創建用戶',
  SIGN_OUT: '退出',
  INTERNAL_SYS_TIP: `內部系統，不可暴露到公網`,
  INTERNAL_SYS_TIP1: ` `,
  OLD_PASSWORD: '請輸入旧密碼',
  REPEAT_PASSWORD: '請輸入確認密碼',
  INPUT_PASSWORD: '請輸入密碼',
  INPUT_USERNAME: '請輸入用戶名',
  INPUT_ROLE: '請輸入角色',
  PWD_NOT_MATCH: '两次輸入密碼不一致!',
  USER_LIST: '用戶列錶',
  ROLE_MGR: '角色管理',
  PRIVILEGE_MGR: '權限管理',
  ROLE: '角色',
  BIND_ROLE: '綁定角色',
  DELETE_USER: `是否要删除该用戶({user})？`,
  DELETE_ROLE: `是否要删除该角色？`,
  CAN_NOT_REMOVE_SELF: '不可以删除當前登錄的用戶！',
  PERMISSION_CONTROL_TITLE: `访问權限控制`,
  RESET_PASSWORD: '重置密碼',
  PRIVILEGE_CONF: '權限配置',
  ACCESS_PRIVILEGE: '訪問權限',

  //交互提示信息
  SELECT_ONE_SERVER_INFO: '請选择一個服務后操作',
  NAME_NOT_EMPTY: `名字不能为空`,
  UPLOAD_FILE_EMPTY: `成功上傳的文件为空`,
  SELECT_ONE_OP: '請选择要操作的項',
  COMMAND_RUNNING: '正在執行命令："{command}"，請先停止命令再執行',
  SAVE_OR_CANCEL: '是否要保存對檔案的更改？',
  PLEASE_INPUT: '請輸入',

  //帮助
  HELP: '帮助',
  QUICK_START: '快速開始',
  ABOUT_TEXT: 'Jarboot 是一個Java進程啟動、調試、诊斷的平台，可以管理、監控及诊斷運行的的Java進程。',
  THREAD: '線程',
  RUNNABLE: '運行中',
  BLOCKED: '阻塞中',
  TOTAL: '全部',
  OVERVIEW: '概览',
  ACTIVE_THREAD: '活動線程',
  HEAP_USED: '堆內存使用量',
  NON_HEAP_USED: '非堆內存使用量',
  HEAP: '堆內存',
  NON_HEAP: '非堆內存',
  CPU_USED: 'CPU佔用率',
  ACTIVE: '活動',
  PEAK_VALUE: '峰值',
  USED: '已用',
  SUBMITTED: '已提交',
  MAX: '最大',
  RUNTIME_INFO: '運行时信息',
  MEMORY: '內存',
  MEMORY_INFO: '当前內存分代信息展示',
};
