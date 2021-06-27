// src/locales/zh-CN.js
export default {
    'navbar.lang': 'English',

    //Tab标题
    SERVICES_MGR: '服务管理',
    SERVICES_CONF: '服务配置',
    SETTING: '设置',
    HELP_DOC: '帮助',

    //服务管理
    ONE_KEY_START: '一键启动',
    ONE_KEY_STOP: '一键停止',
    ONE_KEY_RESTART: '一键重启',
    NAME: '名字',
    STATUS: '状态',
    CLEAR: '清空',
    CLOSE: '关闭',
    CMD_PLACEHOLDER: '输入调试命令执行',
    SELECT_UPLOAD_SERVER_TITLE: '输入要更新或新增的服务的名称',
    UPLOAD_STAGE_TITLE: '上传{server}文件',
    //进程状态
    RUNNING: '运行中',
    STOPPED: '已停止',
    STARTING: '启动中',
    STOPPING: '停止中',

    //通用
    TYPE: '类型',
    SUBMIT_BTN: '提交',
    RESET_BTN: '重置',
    REFRESH_BTN: '刷新',
    SERVER_EMPTY: '服务目录下未发现任何jar启动文件，请添加后刷新。',

    //服务配置
    SERVER_LIST_TITLE: '服务列表',
    JAR_LABEL: '启动的jar文件',
    JVM_OPT_LABEL: 'JVM参数',
    MAIN_ARGS_LABEL: 'Main入口参数',
    WORK_HOME_LABEL: '工作目录',
    ENV_LABEL: '环境变量',
    PRIORITY_LABEL: '启动优先级',
    DAEMON_LABEL: '进程守护',
    JAR_UPDATE_WATCH_LABEL: '文件路径监控',

    //全局配置
    SERVERS_PATH: '服务根目录',
    DEFAULT_VM_OPT: '默认的VM启动参数',
    MAX_START_TIME: '最大启动超时等待',

    //用户登录
    USER_NAME: '用户',
    PASSWORD: '密码',
    LOGIN: '登录',

    //交互提示信息
    SELECT_ONE_SERVER_INFO: '请选择一个服务后操作',
    NAME_NOT_EMPTY: `名字不能为空`,
    UPLOAD_FILE_EMPTY: `成功上传的文件为空`,

    //帮助
    BASIC: '基础',
    QUICK_START: '快速开始',
    ADVANCED: '进阶',
    COMMAND_LIST: '命令列表',
    PROP_FILE: '配置文件',
    //快速开始
    QUICK_START_P1: `项目主页：{github}或者Gitee镜像仓库`,
    QUICK_START_P2: `当你进入到这个界面时，说明你已经搭建并启动好了Jarboot，接下来你要做的是将你自己开发的jar文件放入到约定到目录下。在未做任何配置的默认情况下，在jarboot的目录下创建一个名为{dir}文件夹，作为服务的根目录。在此根目录下，创建的子文件夹的名称即为服务的名称，在子文件下放入要启动的jar文件。`,
    QUICK_START_P3: `默认的目录结构：`,
    QUICK_START_P4: `可以通过{key1}界面中的{key2}修改默认的目录。如果有多个jar文件，则需要在{key3}界面中的{key4}设定Main Class所在的jar文件名称。`,
    QUICK_START_P5: `到{key}界面启动服务。`,
    QUICK_START_P6: `点击对应的按钮即可一键完成所有服务的启动与停止。`,
    QUICK_START_P7: `也可以选择一个或若干个服务后，点击对应的按钮启动与停止选择的服务。`,
    QUICK_START_P8: `刷新服务列表，更新服务数量与状态。`,
    //设置
    SETTING_P1: `通用配置`,
    SETTING_P2: `：服务默认根目录设置，可以更改默认的服务根目录位置，默认情况下为jarboot下的services目录。`,
    SETTING_P3: `：当服务没有设定JVM的启动参数时，会使用该默认的全局配置。`,
    SETTING_P4: `：当服务进程启动后多长时间没有输出信息时认定为启动完成。`,
    SETTING_P5: `：隐藏功能，可以在界面上通过第三方的Arthas对目标服务进行调试，地址为：`,
    SETTING_P6: `：当服务目录下有多个jar文件时需要指定使用哪一个jar文件启动，即Main Class所在的jar文件的名字。`,
    SETTING_P7: `：指定服务启动时的JVM参数，比如内存大小、垃圾收集器等。`,
    SETTING_P8: `：启动的参数，即传给Main入口函数的参数。`,
    SETTING_P9: `：启动的优先级，值越大的先启动，值相同的并行启动，会等待前一级别的服务先启动完成才会进行下一等级的启动。服务关闭时则按照相反的顺序依次停止。`,
    SETTING_P10: `：指定该服务是否需要守护，如果开启了守护，则当该服务异常关闭时jarboot会自动将它重新启动。`,
    SETTING_P11: `：指定是否监控服务目录的更新，开启后，如果监控到服务目录下的文件有更新，会自动重启服务。如果一段时间内更新了很多次，则只会在最后重启依次（防抖动设计）。另外建议日志等临时文件不要在服务的目录下，因为子文件的更新会误判为有可执行文件的更新。`,
    //进阶
    USAGE_DEMO: '使用参考',
    PROP_FILE_DESC: '配置文件jarboot.properties',
    CMD_LIST_DESC: `执行调试命令`,
    CMD_LIST_HELP: `支持的调试命令, 更多的帮助请访问在线文档。`,
};
