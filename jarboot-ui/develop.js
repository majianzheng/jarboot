const fs = require("fs");

function log(...args) {
    console.info('[JARBOOT] - ', ...args)
};

const distBase = './dist/';
const resBase = '../jarboot-service/src/main/resources/static/';

log("Start developing...");

log(">>>Copy umi.js...");
let file = fs.createReadStream(distBase + 'umi.js');
let out = fs.createWriteStream(resBase + 'umi.js');
file.pipe(out);
log(">>>Copy umi.js finished.");

log(">>>Copy umi.css...");
file = fs.createReadStream(distBase + 'umi.css');
out = fs.createWriteStream(resBase + 'umi.css');
file.pipe(out);
log(">>>Copy umi.css finished.");

log(">>>Copy index.html...");
file = fs.createReadStream(distBase + 'index.html');
out = fs.createWriteStream(resBase + 'index.html');
file.pipe(out);
log(">>>Copy index.html finished.");

log(">>>Copy login.html...");
file = fs.createReadStream(distBase + 'login.html');
out = fs.createWriteStream(resBase + 'login.html');
file.pipe(out);
log(">>>Copy login.html finished.");

log("Developing finished.");
