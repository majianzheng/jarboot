const fsExtra = require('fs-extra')
const fs = require('fs');
const path = require('path');

/**
 * build 完成后自动清理jarboot-server/static下的文件并拷贝更新
 * @author majianzheng
 */

const template = (cssFile, jsFile) => (`<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta
      name="viewport"
      content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no"
    />
    <link rel="stylesheet" href="/jarboot/${cssFile}" />
    <script>
      window.routerBase = "/";
    </script>
    <script>
      //! umi version: 3.5.20
    </script>
    <title>Jarboot</title>
  </head>
  <body>
    <div id="root"></div>

    <script src="/jarboot/${jsFile}"></script>
  </body>
</html>`);


const distBase = './dist/';
const staticDir = '../jarboot-server/src/main/resources/static/';
const jarbootDir = staticDir + path.sep + 'jarboot';
const uiPrefix = 'jarboot-ui';
const icon = 'favicon.ico';

console.info("Start developing...");

// 清理旧资源
console.info("Cleaning old static...");
fsExtra.emptyDirSync(staticDir);
console.info("Clean old static success!");
fsExtra.mkdirsSync(jarbootDir);
let cssFileName = '';
let jsFileName = '';
fs
    .readdirSync(path.resolve(distBase))
    .filter(value => (value.endsWith('.js') || value.endsWith('.css'))).forEach(value => {
    const src = distBase + path.sep + value;
    const outName = value.replace('umi', uiPrefix);
    if (outName.endsWith('.js')) {
        jsFileName = outName;
    }
    if (outName.endsWith('.css')) {
        cssFileName = outName;
    }
    const dist = jarbootDir + path.sep + outName;
    fsExtra.copySync(src, dist);
});
//拷贝ico
fsExtra.copySync(distBase + path.sep + icon, staticDir + path.sep + icon);
const content = template(cssFileName, jsFileName);
//更新html文件
fsExtra.outputFileSync(jarbootDir + path.sep + 'index.html', content);
fsExtra.outputFileSync(jarbootDir + path.sep + 'login.html', content);
fsExtra.outputFileSync(staticDir + path.sep + 'index.html', content);
fsExtra.outputFileSync(staticDir + path.sep + 'login.html', content);

console.info("Developing finished.");
