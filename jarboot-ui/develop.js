const fs = require("fs");
const path = require("path");

/**
 * build 完成后自动清理jarboot-server/static下的文件并拷贝更新
 * @author majianzheng
 */

const distBase = './dist/';
const staticDir = '../jarboot-server/src/main/resources/static/';

console.info("Start developing...");

if (!fs.existsSync(staticDir)) {
    fs.mkdirSync(staticDir);
    console.info(`static dir created.`);
}
function clean(dir) {
    const files = fs.readdirSync(dir);
    files && files.length > 0 && files.forEach(file => {
        const p = path.join(dir, file);
        if(fs.statSync(p).isDirectory()) {
            clean(p);
        } else {
            fs.unlinkSync(p);
        }
    });
}
// 清理旧资源
console.info("Cleaning old static...");
clean(staticDir);
console.info("Clean old static success!");

// 开始拷贝
fs.readdir(distBase, (err, files) => {
    if (err) {
        console.error(err);
        return;
    }
    files.forEach(file => {
        console.info(`>>>Copying ${file}...`);
        let rs = fs.createReadStream(path.join(distBase, file));
        let out = fs.createWriteStream(path.join(staticDir, file));
        rs.pipe(out);
        console.info(`>>>Copied ${file}.`);
    });
    console.info("Developing finished.");
});
