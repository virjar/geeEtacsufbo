# 极验滑块验证码js逆向

## 背景介绍

2016年由于一些爱好就在刚极验的滑块，其实一直都破解了极验的协议，极验在6.0.0之后，使用了控制流平坦化混淆。当时（可能到现在）应该就我一个人能够实现他的控制流平坦化混淆的脱壳。
不过现在有过去一年多了，看起来协议对抗这块业界没有什么进展啊，正好我也不太可能做和极验对抗的工作了。所以代码放到那里起灰多浪费，所以开源了吧。我觉得里面的思路还是可以给大家一些参考的。

项目基于node 和java，node实现ast的处理已经通过node服务模拟符号混淆函数和控制流混淆器的流程控制引擎。

这个项目我当时也就一周做出来，其实是demo状态，在控制流还原那里的控制流反转ast代码模型的算法那里无法处理循环+break的代码结构。比如 for + break 和while+break将会处理失败。
做这个东西的时候并没有完整的代码生成理论知识，知道当时协议破解完成之后，才对流程逆向生成这里有一些认识，后来总结了一个ppt。https://virjar-comon.oss-cn-beijing.aliyuncs.com/02%20极验滑块.pptx 感兴趣的同学可以把里面的理论实现一遍。

对了，除了极验，还有一个淘宝滑块，也有控制流混淆。理论上，都是可以搞定的。emmm



## 操作步骤，由于是demo，很多地址直接绝对路径了
1. 安装esprima
```
npm install esprima
```

2. 将极验的代码转化为ast对象
```
esparse /Users/virjar/git/geeEtacsufbo/jscode/resources/geetest.6.0.9.js > /Users/virjar/git/geeEtacsufbo/jscode/resources/geetest.6.0.9.json
```
3. 构造node server 
```
node /Users/virjar/git/geeEtacsufbo/jscode/node_server/geetest-http-server.js
```

4. 启动java代码处理ast对象
运行java项目：/Users/virjar/git/geeEtacsufbo/ast-java/src/main/java/com/virjar/geetest/crack/AstTransfer.java

5. 脱壳后的代码，反转为js
运行node项目 /Users/virjar/git/geeEtacsufbo/jscode/index.js

6. 最后的脱壳js文件： /Users/virjar/git/geeEtacsufbo/jscode/resources/geetest.6.0.9_beauty.js



# 基本原理

使用esprima构造ast对象，然后解析ast里面的代码特征，对符号混淆和控制流平坦化混淆进行处理。最终得到一个相对好看的代码
