import {defineConfig} from 'umi';

export default defineConfig({
    nodeModulesTransform: {
        type: 'none',
    },
    title: 'Jarboot',
    routes: [
        {path: '/', component: '@/pages/index'},
        {path: '/login', component: '@/pages/login/Login'},
        {path: '/debugPlugin', component: '@/pages/plugin/DebugPlugin'},
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
