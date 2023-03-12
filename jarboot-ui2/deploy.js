const fsExtra = require('fs-extra');
const path = require('path');

/**
 * build 完成后自动清理jarboot-server/static下的文件并拷贝更新
 * @author majianzheng
 */

const DIST = './dist/';
const STATIC_DIR = '../jarboot-server/src/main/resources/static/';
const JARBOOT_DIR = STATIC_DIR + path.sep + 'jarboot';

console.info("Start developing...");

// 清理旧资源
console.info("Cleaning old static...");
fsExtra.emptyDirSync(STATIC_DIR);
console.info("Clean old static success!");
fsExtra.mkdirsSync(JARBOOT_DIR);
//拷贝ico
fsExtra.copySync(DIST, JARBOOT_DIR);

console.info("Developing finished.");
