import {defineConfig} from 'umi';

export default defineConfig({
    nodeModulesTransform: {
        type: 'none',
    },
    title: 'Jarboot',
    routes: [
        {path: '/', component: '@/pages/index'},
        {path: '/login', component: '@/pages/login/Login'},
    ],
    exportStatic: {dynamicRoot: false, htmlSuffix: true},
    fastRefresh: {},
    inlineLimit: 100000,
    hash: true,
    //devtool: 'source-map',
    locale: {
        default: 'en-US',
        antd: true,
        title: true,
        baseNavigator: true,
        baseSeparator: '-',
    },
    theme: {
        "primary-color": "#1DA57A",
        "tree-node-selected-bg": "#dcf4ff",
        "tree-directory-selected-color": "#000",
        "tree-directory-selected-bg": "#dcf4ff",
    },
    antd: {
        //dark: true,
        compact: true, // 开启紧凑主题
    },
    proxy: {
        '/api': {
            'target': 'http://localhost:9899/api/',
            'changeOrigin': true,
            'pathRewrite': {'^/api': ''}
        },
        '/plugins': {
            'target': 'http://localhost:9899/plugins/',
            'changeOrigin': true,
            'pathRewrite': {'^/plugins': ''}
        },
    },
});
