package com.virjar.geetest.crack;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by virjar on 2017/9/8.
 */
public class Util {
    public static List<Integer> calcStateMachine(JSONObject init) {

        if (!StringUtils.equalsIgnoreCase(init.getString("type"), "MemberExpression")) {
            return null;
        }

        List<Integer> memberAccessSequence = Lists.newLinkedList();
        try {
            while (StringUtils.equalsIgnoreCase(init.getString("type"), "MemberExpression")) {
                if (init.getJSONObject("property") == null) {
                    return null;
                }
                memberAccessSequence.add(init.getJSONObject("property").getInteger("value"));
                init = init.getJSONObject("object");// 这个对象的属性访问
            }
        } catch (NumberFormatException e) {
            return null;
        }
        // 必须是一个函数调用的结果的属性访问
        if (!StringUtils.equalsIgnoreCase(init.getString("type"), "CallExpression")) {
            return null;
        }

        JSONObject callee = init.getJSONObject("callee");
        if (!StringUtils.equalsIgnoreCase(callee.getString("type"), "MemberExpression")) {
            return null;
        }
        String functionName = callee.getJSONObject("property").getString("name");
        if (!(StringUtils.equalsIgnoreCase(functionName, "k9r") || StringUtils.equalsIgnoreCase(functionName, "L9r"))) {
            return null;
        }
        return Lists.reverse(memberAccessSequence);
    }

    public static JSONObject buildIfBlock(JSONObject testCondition, JSONArray consequent, JSONArray alternate) {
        JSONObject ret = new JSONObject();
        ret.put("type", "IfStatement");
        ret.put("test", testCondition);
        JSONObject consequentJson = new JSONObject();
        consequentJson.put("type", "BlockStatement");
        consequentJson.put("body", consequent);
        ret.put("consequent", consequentJson);

        JSONObject alternateJson = new JSONObject();
        alternateJson.put("type", "BlockStatement");
        alternateJson.put("body", alternate);
        ret.put("alternate", alternateJson);

        return ret;
    }

    public static JSONObject buildWhileBlock(JSONObject testCondition, JSONArray body) {
        JSONObject ret = new JSONObject();
        ret.put("type", "WhileStatement");
        ret.put("test", testCondition);
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("type", "BlockStatement");
        bodyJson.put("body", body);
        ret.put("body", bodyJson);
        return ret;
    }

    public static JSONObject revertTestCondition(JSONObject testCondition) {
        JSONObject ret = new JSONObject();
        ret.put("type", "UnaryExpression");
        ret.put("operator", "!");
        ret.put("argument", testCondition);
        return ret;
    }

    public static boolean isAssignStateControllStatement(JSONObject codeLine, String stateControlValName) {
        return StringUtils.equalsIgnoreCase(codeLine.getString("type"), "ExpressionStatement")
                && StringUtils.equalsIgnoreCase(codeLine.getJSONObject("expression").getString("type"),
                "AssignmentExpression")
                && StringUtils.equalsIgnoreCase(codeLine.getJSONObject("expression").getString("operator"), "=")
                && StringUtils.equalsIgnoreCase(
                codeLine.getJSONObject("expression").getJSONObject("left").getString("name"),
                stateControlValName);
    }

    public static <E> int indexForLinkedHashSet(LinkedHashSet<E> linkedHashSet, E entry) {
        int i = 0;
        for (E e : linkedHashSet) {
            if (e.equals(entry)) {
                return i;
            }
            i++;
        }
        return -1;
    }

}
