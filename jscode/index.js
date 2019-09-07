'use strict';
const fs = require('fs');
require('colors');
const escodegen = require('escodegen');

var source_json = "/Users/virjar/git/geeEtacsufbo/jscode/resources/geetest.6.0.9_beauty.json";
var dst = "/Users/virjar/git/geeEtacsufbo/jscode/resources/geetest.6.0.9_beauty.js";

let ast_json_string = fs.readFileSync(source_json).toString('utf8');
let ast = JSON.parse(ast_json_string);
let code = escodegen.generate(ast);
if (dst)//如果有输出路径，则往指定文件路径写，否则写入到控制台
    fs.writeFileSync(dst, code);
else {
    console.log(code)
}