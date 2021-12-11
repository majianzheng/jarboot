const fs = require('fs-extra')

/**
 * build 完成后自动清理jarboot-server/static下的文件并拷贝更新
 * @author majianzheng
 */

const distBase = './dist/';
const staticDir = '../jarboot-server/src/main/resources/static/';

console.info("Start developing...");

// 清理旧资源
console.info("Cleaning old static...");
fs.emptyDirSync(staticDir)
console.info("Clean old static success!");

// 开始拷贝
fs.copySync(distBase, staticDir, {overwrite: true});
console.info("Developing finished.");
