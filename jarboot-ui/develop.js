const fsExtra = require('fs-extra');
const fs = require('fs');
const path = require('path');

/**
 * build 完成后自动清理jarboot-server/static下的文件并拷贝更新
 * @author majianzheng
 */

const meta = '<meta charset="utf-8"/>\n    ' +
    '<meta name="viewport" content="width=device-width, initial-scale=1, ' +
    'maximum-scale=1, minimum-scale=1, user-scalable=no"/>';

const template = (cssFile, jsFile) => (`<!DOCTYPE html>
<html>
  <head>
    ${meta}
    <link rel="shortcut icon" href="/jarboot/favicon.ico" type="image/x-icon">
    <link rel="stylesheet" href="/jarboot/${cssFile}"/>
    <script>window.routerBase = "/";</script>
    <title>Jarboot</title>
  </head>
  <body>
    <div id="root"></div>
    <script src="/jarboot/${jsFile}"></script>
  </body>
</html>`);


const DIST = './dist/';
const STATIC_DIR = '../jarboot-server/src/main/resources/static/';
const JARBOOT_DIR = STATIC_DIR + path.sep + 'jarboot';
const UI_PREFIX = 'jarboot-ui';
const icon = 'favicon.ico';
const INDEX_FILE = 'index.html';
const LOGIN_FILE = 'login.html';
const HTML_FILES = [
    JARBOOT_DIR + path.sep + INDEX_FILE,
    JARBOOT_DIR + path.sep + LOGIN_FILE,
    STATIC_DIR + path.sep + INDEX_FILE,
    STATIC_DIR + path.sep + LOGIN_FILE,
];

console.info("Start developing...");

// 清理旧资源
console.info("Cleaning old static...");
fsExtra.emptyDirSync(STATIC_DIR);
console.info("Clean old static success!");
fsExtra.mkdirsSync(JARBOOT_DIR);
let cssFileName = '';
let jsFileName = '';
fs
    .readdirSync(path.resolve(DIST))
    .filter(value => (value.endsWith('.js') || value.endsWith('.css'))).forEach(value => {
    const src = DIST + path.sep + value;
    const outName = value.replace('umi', UI_PREFIX);
    if (outName.endsWith('.js')) {
        jsFileName = outName;
    }
    if (outName.endsWith('.css')) {
        cssFileName = outName;
    }
    const dist = JARBOOT_DIR + path.sep + outName;
    fsExtra.copySync(src, dist);
});
//拷贝ico
fsExtra.copySync(DIST + path.sep + icon, JARBOOT_DIR + path.sep + icon);

//更新html文件
const content = template(cssFileName, jsFileName);
HTML_FILES.forEach(file => fsExtra.outputFileSync(file, content));

console.info("Developing finished.");
