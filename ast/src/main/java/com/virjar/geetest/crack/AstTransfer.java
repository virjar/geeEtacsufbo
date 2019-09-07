package com.virjar.geetest.crack;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.virjar.geetest.crack.net.HttpInvoker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by virjar on 2017/9/7.
 */
public class AstTransfer {
    static {

    }

    public static void main(String[] args) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(
                new File("/Users/virjar/git/gt2/src/main/webapp/static/js/geetest.6.0.9.json"));

        String s = IOUtils.toString(fileInputStream, Charsets.UTF_8);
        IOUtils.closeQuietly(fileInputStream);

        JSONObject ast = JSONObject.parseObject(s);

        walkSymbol(ast);

        walkStatement(ast);

        Files.write(
                JSONObject.toJSONString(ast, SerializerFeature.WriteMapNullValue,
                        SerializerFeature.DisableCircularReferenceDetect),
                new File("/Users/virjar/git/gt2/src/main/webapp/static/js/geetest.6.0.9_beauty_02.json"),
                Charsets.UTF_8);
    }


    private static void walkStatement(JSONObject ast) {
        walk(ast, new Walker() {
            @Override
            public boolean walk(JSONObject ast) {
                // // 需要是函数
                // if (!StringUtils.equalsIgnoreCase(ast.getString("type"), "FunctionExpression")) {
                // return true;
                // }
                //
                // // 获取函数体
                // JSONObject body = ast.getJSONObject("body");
                if (!StringUtils.equalsIgnoreCase(ast.getString("type"), "BlockStatement")) {
                    // System.out.println(ast.getString("type"));
                    // System.out.println("不是块级代码，暂不明确代码结构，忽略");
                    return true;
                }

                JSONArray codeLines = ast.getJSONArray("body");// 每一项都是可以看作一行代码
                if (codeLines == null) {
                    System.out.println("12345");
                }
                int i = 0;
                List<Integer> memberAccessSequence = null;
                String varName = null;

                for (; i < codeLines.size(); i++) {
                    // 寻找状态机调用代码行，代码上下一行应该就是while循环
                    JSONObject line = codeLines.getJSONObject(i);
                    if (!StringUtils.equalsIgnoreCase(line.getString("type"), "VariableDeclaration")) {
                        continue;
                    }
                    JSONArray declarations = line.getJSONArray("declarations");
                    int j = 0;
                    for (; j < declarations.size(); j++) {
                        // 一个变量声明
                        JSONObject declaration = declarations.getJSONObject(j);
                        if (declaration.getJSONObject("init") == null) {
                            continue;
                        }

                        // 变量初始化动作，观察他是否在访问状态机
                        JSONObject init = declaration.getJSONObject("init");
                        memberAccessSequence = Util.calcStateMachine(init);
                        if (memberAccessSequence == null) {
                            continue;
                        }
                        // 调用k9r函数，或者L9r函数，并且执行数组访问，访问逻辑命中
                        varName = declaration.getJSONObject("id").getString("name");// 变量名
                        break;
                    }
                    if (j != declarations.size()) {
                        break;
                    }
                }

                if (i == codeLines.size()) {
                    // 没有找到初始化代码
                    return true;
                }

                i++;
                if (i >= codeLines.size()) {
                    return true;// 赋值语句后面没有语句了
                }

                JSONObject whileStatement = codeLines.getJSONObject(i);
                if (!StringUtils.equalsIgnoreCase(whileStatement.getString("type"), "WhileStatement")) {
                    return true;
                }

                JSONObject whileCondition = whileStatement.getJSONObject("test");
                if (!StringUtils.equalsIgnoreCase(whileCondition.getString("type"), "BinaryExpression")
                        || !StringUtils.equalsIgnoreCase(whileCondition.getString("operator"), "!==") || !StringUtils
                        .equalsIgnoreCase(whileCondition.getJSONObject("left").getString("name"), varName)) {
                    return true;
                }

                // while循环结束条件
                List<Integer> exitCondition = Util.calcStateMachine(whileCondition.getJSONObject("right"));
                JSONObject whileBody = whileStatement.getJSONObject("body");// while的代码块
                if (!StringUtils.equalsIgnoreCase(whileBody.getString("type"), "BlockStatement")) {
                    System.out.println("while 代码块内部不是块级代码，不识别");
                    return true;
                }
                JSONArray whileBlockBody = whileBody.getJSONArray("body");
                if (whileBlockBody.size() != 1) {
                    System.out.println("while 中不只一行代码");
                    return true;
                }

                JSONObject switchStatement = whileBlockBody.getJSONObject(0);
                if (!StringUtils.endsWithIgnoreCase(switchStatement.getString("type"), "SwitchStatement")) {
                    System.out.println("while 中不是switch语句");
                    return true;
                }

                if (!StringUtils.equalsIgnoreCase(switchStatement.getJSONObject("discriminant").getString("name"),
                        varName)) {
                    System.out.println("switch 条件不是 指定变量:" + varName);
                }

                JSONArray cases = switchStatement.getJSONArray("cases");// 所有的case语句
                List<CodeEntry> codeEntries = parseEntry(cases, varName);

                Graph graph = new Graph(memberAccessSequence, exitCondition, codeEntries);

                JSONArray merge = graph.merge();
                if (merge != null) {
                    JSONArray jsonArray = new JSONArray();
                    for (int k = 0; k < i - 1; k++) {
                        jsonArray.add(codeLines.getJSONObject(k));
                    }

                    jsonArray.addAll(merge);
                    ast.put("body", jsonArray);
                }
                return true;
            }
        });
    }

    private static List<CodeEntry> parseEntry(JSONArray cases, String varName) {
        List<CodeEntry> ret = Lists.newLinkedList();
        for (int i = 0; i < cases.size(); i++) {

            JSONObject switchCase = cases.getJSONObject(i);
            if (!StringUtils.equalsIgnoreCase(switchCase.getString("type"), "SwitchCase")) {
                return Lists.newArrayList();
            }
            JSONObject test = switchCase.getJSONObject("test");
            List<Integer> entryID = Util.calcStateMachine(test);// 状态ID
            JSONArray consequent = switchCase.getJSONArray("consequent");
            cleanDeadLock(consequent);

            CodeEntry codeEntry = new CodeEntry();
            codeEntry.setSateControlValName(varName);
            ret.add(codeEntry);

            codeEntry.setEntryID(entryID);
            if (StringUtils.equalsIgnoreCase(consequent.getJSONObject(consequent.size() - 1).getString("type"),
                    "ReturnStatement")) {
                codeEntry.setReturnEntry(true);
            }
            codeEntry.setCodeSegment(consequent);

        }
        return ret;
    }

    /**
     * 删除无效代码，如果终于有return，则删除return后面的所有代码
     *
     * @param consequent 多行代码
     */
    private static void cleanDeadLock(JSONArray consequent) {
        int i = 0;
        for (; i < consequent.size(); i++) {
            JSONObject codeLine = consequent.getJSONObject(i);
            if (StringUtils.equalsIgnoreCase(codeLine.getString("type"), "ReturnStatement")) {
                break;
            }
        }
        for (int j = consequent.size() - 1; j > i; j--) {
            consequent.remove(j);
        }
    }

    private static void walk(JSONObject ast, Walker walker) {

        if (!walker.walk(ast)) {
            return;
        }

        for (Map.Entry<String, Object> next : ast.entrySet()) {
            Object value = next.getValue();
            if (value instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) value).size(); i++) {
                    walk(((JSONArray) value).getJSONObject(i), walker);
                }
            } else if (value instanceof JSONObject) {
                walk((JSONObject) value, walker);
            }

        }
    }

    private interface Walker {
        boolean walk(JSONObject ast);
    }

    private static void walkSymbol(JSONObject ast) {
        walk(ast, new Walker() {
            @Override
            public boolean walk(JSONObject ast) {
                // 处理常量表映射。（将常量用字面量替换，而非使用函数加标记索引的方式。）
                //M9r.C8z(699)
                //M9r.R8z(40)
                if (StringUtils.equalsIgnoreCase(ast.getString("type"), "CallExpression")
                        && StringUtils.equalsIgnoreCase(ast.getJSONObject("callee").getString("type"),
                        "MemberExpression")
                        && StringUtils.equalsIgnoreCase(
                        ast.getJSONObject("callee").getJSONObject("property").getString("type"),
                        "Identifier") && StringUtils.equalsIgnoreCase(
                        ast.getJSONObject("callee").getJSONObject("object").getString("name"),
                        "M9r")) {
                    if (StringUtils.equalsIgnoreCase(
                            ast.getJSONObject("callee").getJSONObject("property").getString("name"), "C8z")
                            || StringUtils.equalsIgnoreCase(
                            ast.getJSONObject("callee").getJSONObject("property").getString("name"), "R8z")) {

                        String param = ast.getJSONArray("arguments").getJSONObject(0).getString("raw");
                        String type = ast.getJSONObject("callee").getJSONObject("property").getString("name");
                        String s = HttpInvoker
                                .get("http://127.0.0.1:8889/?type=" + type + "&num=" + param + "&fun=Literal");
                        JSONObject value = JSONObject.parseObject(s);
                        ast.clear();// 删除原来的代码
                        ast.put("type", "Literal");
                        ast.put("raw", value.getString("value"));
                        if (value.getString("type").equals("string")) {
                            ast.put("value", value.getString("value"));
                        } else {
                            System.out.println("unknown type: " + value.getString("type"));
                        }
                        return false;
                    }

                }

                return true;
            }
        });
    }
}
