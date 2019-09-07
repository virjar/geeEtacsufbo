var http = require("http");
var geetest = require("./geetest.6.0.9.js");
var url = require('url');

http.createServer(function (request, response) {

    // 发送 HTTP 头部
    // HTTP 状态值: 200 : OK
    // 内容类型: text/plain
    response.writeHead(200, {'Content-Type': 'application/json;charset=utf8'});


    var params = url.parse(request.url, true).query;
    var num = params.num;

    var body = {};

    if (params.fun === 'Literal') {


        if (params.type == 'R8z') {
            var value = geetest.R8z(num);
            console.log("转化:R8z 参数 " + num + " 结果: " + value);
            body.value = value;
            body.type = typeof value;

        } else if (params.type == 'C8z') {
            var value = geetest.C8z(num);
            console.log("转化:C8z 参数 " + num + " 结果: " + value);
            body.value = value;
            body.type = typeof value;

        }

        response.end(JSON.stringify(body));
    } else if (params.fun == 'compare') {
        console.log("request compare:" + request.url);
        //f2Z.V2Z()[36][30][6]
        var num1 = params.id1.split("_");
        var num2 = params.id2.split("_");
        var ret;
        if (getState(num1) == getState(num2)) {
            ret = "true";
        } else {
            ret = "false";
        }
        console.log("compare result: " + ret);
        response.end(ret);
    }


}).listen(8889);


function getState(state) {
    var root = geetest.k9r();
    for (var i in state) {
        root = root[parseInt(state[i])];
    }
    return root;
}

console.log('Server running at http://127.0.0.1:8889/');
