package com.virjar.geetest.crack;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.geetest.crack.net.HttpInvoker;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by virjar on 2017/9/8.<br/>
 * 跳转代码使用图的数据结构来描述
 */
public class Graph {
    // 初始化状态
    private List<Integer> initState;

    // 终止状态
    private List<Integer> exitState;

    // 所有跳转节点
    private Map<String, CodeEntry> nodes;

    private boolean isGraphValid = true;

    private static int testIndex = 0;

    // 2,5,6 有问题
    private static Set<Integer> sets = Sets.newHashSet(0, 1, 3, 4, 7, 8);

    public Graph(List<Integer> initState, List<Integer> exitState, List<CodeEntry> nodeList) {
        this.initState = initState;
        this.exitState = exitState;
        this.nodes = Maps.newHashMapWithExpectedSize(nodeList.size());

        for (CodeEntry codeEntry : nodeList) {
            nodes.put(StringUtils.join(codeEntry.getEntryID(), "_"), codeEntry);
        }

        // 构建跳转关系
        for (CodeEntry codeEntry : nodes.values()) {
            calcNextStates(codeEntry);
        }

    }

    public JSONArray merge() {
        if (!isGraphValid) {
            System.out.println("图不合法");
            return null;
        }

        if (!hasCycle(initState, Sets.<CodeEntry>newHashSet())) {
            // 是一个有向无环图，合并方案很简单，先尝试此优化方案
            return noCycleMerge(initState);
        }

        return cycleMerge(initState, exitState, Sets.<CodeEntry>newLinkedHashSet());
        // CodeEntry codeEntry = calcEntry(initState);// 第一份执行的代码
        // List<CodeEntry> mainRoute = findMainRoute(initState, Sets.<CodeEntry> newHashSet());
        // System.out.println(mainRoute == null ? "有return" : "无return");
        // if (mainRoute == null) {
        // return null;
        // }
        //
        // return null;

    }

    private JSONArray cycleMerge(List<Integer> initRoute, List<Integer> exitState,
                                 LinkedHashSet<CodeEntry> accessedEntry) {
        JSONArray ret = new JSONArray();
        if (isEntryEqual(initRoute, exitState)) {
            return ret;
        }
        CodeEntry entry = calcEntry(initRoute);// 入口
        if (entry == null) {
            return ret;// 死代码
        }
        while (true) {
            accessedEntry.add(entry);
            if (entry.isReturnEntry()) {
                ret.addAll(calcCodeLine(entry));
                return ret;
            }

            if (entry.getSingleNext() != null) {
                ret.addAll(calcCodeLine(entry));
                if (isEntryEqual(entry.getSingleNext(), exitState)) {
                    return ret;
                }

                // 单挑转
                entry = calcEntry(entry.getSingleNext());
                if (entry == null) {
                    // 死路径
                    // 这个日志答应挤出来代表可能存在问题
                    System.out.println("dead code");
                    return ret;
                }
            } else {
                boolean leftCycle = hasCycle(entry.getTestNext(), wrapAccessedEntrySet(accessedEntry));
                if (!leftCycle && !hasCycle(entry.getTestNotNext(), wrapAccessedEntrySet(accessedEntry))) {
                    JSONArray ifCode = noCycleMerge(entry.getTestNext());
                    JSONArray notCode = noCycleMerge(entry.getTestNotNext());
                    ret.add(Util.buildIfBlock(entry.getTestCondition(), ifCode, notCode));
                    return ret;
                }
                if (leftCycle) {
                    CodeEntry codeEntry = calcCyclePoint(entry.getTestNext(), wrapAccessedEntrySet(accessedEntry));
                    int i = Util.indexForLinkedHashSet(accessedEntry, codeEntry);
                    if (i < 0) {
                        JSONArray ifCode = cycleMerge(entry.getTestNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));
                        JSONArray notCode = cycleMerge(entry.getTestNotNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));
                        if (ifCode == null || notCode == null) {
                            return null;
                        }
                        ret.add(Util.buildIfBlock(entry.getTestCondition(), ifCode, notCode));
                        return ret;
                    } else if (i < accessedEntry.size() - 1) {
                        System.out.println("出现do while 循环");
                        if (sets.contains(testIndex++)) {
                            JSONArray ifCode = cycleMerge(entry.getTestNext(), codeEntry.getEntryID(),
                                    wrapAccessedEntrySet(accessedEntry));
                            if (ifCode == null) {
                                return null;
                            }

                            List<CodeEntry> codeEntries = Lists.newArrayList(accessedEntry);
                            for (int j = i; j < accessedEntry.size() - 1; j++) {
                                ifCode.addAll(calcCodeLine(codeEntries.get(j)));
                            }

                            ret.add(Util.buildWhileBlock(entry.getTestCondition(), ifCode));

                            JSONArray notCode = cycleMerge(entry.getTestNotNext(), exitState,
                                    wrapAccessedEntrySet(accessedEntry));
                            if (notCode == null) {
                                return null;
                            }
                            ret.addAll(notCode);
                            return ret;
                        } else {
                            return null;
                        }

                    } else {
                        JSONArray ifCode = cycleMerge(entry.getTestNext(), entry.getEntryID(),
                                wrapAccessedEntrySet(accessedEntry));
                        if (ifCode == null) {
                            return null;
                        }

                        ret.add(Util.buildWhileBlock(entry.getTestCondition(), ifCode));

                        JSONArray notCode = cycleMerge(entry.getTestNotNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));
                        if (notCode == null) {
                            return null;
                        }
                        ret.addAll(notCode);
                        return ret;
                    }
                } else {
                    CodeEntry codeEntry = calcCyclePoint(entry.getTestNotNext(), wrapAccessedEntrySet(accessedEntry));
                    int i = Util.indexForLinkedHashSet(accessedEntry, codeEntry);
                    if (i < 0) {
                        JSONArray ifCode = cycleMerge(entry.getTestNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));
                        JSONArray notCode = cycleMerge(entry.getTestNotNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));
                        if (ifCode == null || notCode == null) {
                            return null;
                        }
                        ret.add(Util.buildIfBlock(entry.getTestCondition(), ifCode, notCode));
                        return ret;
                    } else if (i < accessedEntry.size() - 1) {
                        System.out.println("出现do while 循环");
                        if (sets.contains(testIndex++)) {
                            JSONArray ifCode = cycleMerge(entry.getTestNext(), exitState,
                                    wrapAccessedEntrySet(accessedEntry));

                            if (ifCode == null) {
                                return null;
                            }

                            JSONArray notCode = cycleMerge(entry.getTestNotNext(), codeEntry.getEntryID(),
                                    wrapAccessedEntrySet(accessedEntry));

                            if (notCode == null) {
                                return null;
                            }

                            List<CodeEntry> codeEntries = Lists.newArrayList(accessedEntry);
                            for (int j = i; j < accessedEntry.size(); j++) {
                                notCode.addAll(calcCodeLine(codeEntries.get(j)));
                            }

                            ret.add(Util.buildWhileBlock(Util.revertTestCondition(entry.getTestCondition()), notCode));

                            ret.addAll(ifCode);
                            return ret;
                        } else {
                            return null;
                        }
                    } else {
                        JSONArray ifCode = cycleMerge(entry.getTestNext(), exitState,
                                wrapAccessedEntrySet(accessedEntry));

                        if (ifCode == null) {
                            return null;
                        }

                        JSONArray notCode = cycleMerge(entry.getTestNotNext(), entry.getEntryID(),
                                wrapAccessedEntrySet(accessedEntry));

                        if (notCode == null) {
                            return null;
                        }
                        ret.add(Util.buildWhileBlock(Util.revertTestCondition(entry.getTestCondition()), notCode));

                        ret.addAll(ifCode);
                        return ret;
                    }
                }

            }
        }
    }

    private LinkedHashSet<CodeEntry> wrapAccessedEntrySet(LinkedHashSet<CodeEntry> input) {
        return Sets.newLinkedHashSet(input);
    }

    private JSONArray noCycleMerge(List<Integer> initRoute) {
        JSONArray ret = new JSONArray();
        if (isEntryEqual(initRoute, exitState)) {
            return ret;
        }
        CodeEntry entry = calcEntry(initRoute);// 入口
        if (entry == null) {
            return ret;// 死代码
        }
        while (true) {
            ret.addAll(calcCodeLine(entry));
            if (entry.isReturnEntry()) {
                return ret;
            }
            if (entry.getSingleNext() != null) {
                if (isEntryEqual(entry.getSingleNext(), exitState)) {
                    return ret;
                }

                // 单挑转
                entry = calcEntry(entry.getSingleNext());
                if (entry == null) {
                    // 死路径
                    // 这个日志答应挤出来代表可能存在问题
                    System.out.println("dead code");
                    return ret;
                }

            } else {
                JSONArray ifCode = noCycleMerge(entry.getTestNext());
                JSONArray notCode = noCycleMerge(entry.getTestNotNext());
                ret.add(Util.buildIfBlock(entry.getTestCondition(), ifCode, notCode));
                return ret;
            }
        }
    }

    private JSONArray calcCodeLine(CodeEntry codeEntry) {
        JSONArray codeSegment = codeEntry.getCodeSegment();
        JSONArray ret = new JSONArray();
        for (int i = 0; i < codeSegment.size(); i++) {
            JSONObject codeLine = codeSegment.getJSONObject(i);
            if (StringUtils.equalsIgnoreCase(codeLine.getString("type"), "BreakStatement")) {
                return ret;
            }
            if (Util.isAssignStateControllStatement(codeLine, codeEntry.getSateControlValName())) {
                return ret;
            }
            ret.add(codeLine);
        }
        return ret;
    }

    private CodeEntry calcCyclePoint(List<Integer> initRoute, Set<CodeEntry> accessedEntry) {
        if (isEntryEqual(initRoute, exitState)) {
            return null;
        }
        CodeEntry entry = calcEntry(initRoute);// 入口
        if (entry == null) {
            return null;// 死代码
        }

        while (true) {
            if (accessedEntry.contains(entry)) {
                return entry;
            }
            accessedEntry.add(entry);
            if (entry.isReturnEntry()) {
                return null;
            }
            if (entry.getSingleNext() != null) {
                if (isEntryEqual(entry.getSingleNext(), exitState)) {
                    return null;
                }

                // 单挑转
                entry = calcEntry(entry.getSingleNext());
                if (entry == null) {
                    // 死路径
                    System.out.println("dead code");
                    return null;
                }

            } else {
                Set<CodeEntry> tempAccessEntry = Sets.newHashSet(accessedEntry);
                CodeEntry codeEntry = calcCyclePoint(entry.getTestNext(), tempAccessEntry);
                if (codeEntry != null) {
                    return codeEntry;
                }
                tempAccessEntry = Sets.newHashSet(accessedEntry);
                codeEntry = calcCyclePoint(entry.getTestNotNext(), tempAccessEntry);
                if (codeEntry != null) {
                    return codeEntry;
                }

                return null;
            }

        }
    }

    /**
     * 跳转环检查，如果没有环构成的话，可以放心的合并代码
     *
     * @param initRoute     入口
     * @param accessedEntry 所有访问过的节点，用来标记遍历过的节点，判断环的产生
     * @return 是否包含环
     */
    private boolean hasCycle(List<Integer> initRoute, Set<CodeEntry> accessedEntry) {
        if (isEntryEqual(initRoute, exitState)) {
            return false;
        }
        CodeEntry entry = calcEntry(initRoute);// 入口
        if (entry == null) {
            return false;// 死代码
        }

        while (true) {
            if (accessedEntry.contains(entry)) {
                return true;
            }
            accessedEntry.add(entry);
            if (entry.isReturnEntry()) {
                return false;
            }
            if (entry.getSingleNext() != null) {
                if (isEntryEqual(entry.getSingleNext(), exitState)) {
                    return false;
                }

                // 单挑转
                entry = calcEntry(entry.getSingleNext());
                if (entry == null) {
                    // 死路径
                    System.out.println("dead code");
                    return false;
                }

            } else {
                Set<CodeEntry> tempAccessEntry = Sets.newHashSet(accessedEntry);
                if (hasCycle(entry.getTestNext(), tempAccessEntry)) {
                    return true;
                }
                tempAccessEntry = Sets.newHashSet(accessedEntry);
                if (hasCycle(entry.getTestNotNext(), tempAccessEntry)) {
                    return true;
                }
                return false;
            }

        }
    }

    // 构建一条包含出口代码的主线代码，从入口到出口
    private List<CodeEntry> findMainRoute(List<Integer> initRoute, Set<CodeEntry> accessedEntry) {
        List<CodeEntry> ret = Lists.newLinkedList();
        CodeEntry entry = calcEntry(initRoute);// 入口
        if (entry == null) {
            return null;
        }
        while (true) {
            if (accessedEntry.contains(entry)) {
                // System.out.println("有环代码");
                return null;// 成环了
            }

            ret.add(entry);
            accessedEntry.add(entry);

            if (entry.isReturnEntry()) {
                return null;
            }

            if (entry.getSingleNext() != null) {
                if (isEntryEqual(entry.getSingleNext(), exitState)) {
                    return ret;
                }

                // 单挑转
                entry = calcEntry(entry.getSingleNext());
                if (entry == null) {
                    // 死路径
                    System.out.println("dead code");
                    return null;
                }

            } else {
                Set<CodeEntry> tempAccessEntry = Sets.newHashSet(accessedEntry);
                List<CodeEntry> route = findMainRoute(entry.getTestNext(), tempAccessEntry);
                if (route != null) {
                    ret.addAll(route);
                    return ret;
                }

                tempAccessEntry = Sets.newHashSet(accessedEntry);
                route = findMainRoute(entry.getTestNotNext(), tempAccessEntry);
                if (route != null) {
                    ret.addAll(route);
                    return ret;
                }
            }

        }
    }

    private void calcNextStates(CodeEntry codeEntry) {
        if (codeEntry.isReturnEntry()) {
            return;// return 语句没有下一步状态
        }

        // 分析代码内容
        JSONArray codeSegment = codeEntry.getCodeSegment();
        for (int i = 0; i < codeSegment.size(); i++) {
            JSONObject codeLine = codeSegment.getJSONObject(i);
            if (Util.isAssignStateControllStatement(codeLine, codeEntry.getSateControlValName())) {
                // 在对状态机变量进行赋值
                // 拿到赋值对代码
                JSONObject assignCode = codeLine.getJSONObject("expression").getJSONObject("right");
                if (StringUtils.equalsIgnoreCase(assignCode.getString("type"), "MemberExpression")) {
                    // 如果是直接访问状态。说明是单跳转
                    codeEntry.setSingleNext(Util.calcStateMachine(assignCode));
                    setUpPreStates(codeEntry.getSingleNext(), codeEntry);
                } else if (StringUtils.equalsIgnoreCase(assignCode.getString("type"), "ConditionalExpression")) {
                    // 通过三目表达式操作状态机
                    codeEntry.setTestCondition(assignCode.getJSONObject("test"));
                    codeEntry.setTestNext(Util.calcStateMachine(assignCode.getJSONObject("consequent")));
                    codeEntry.setTextNotNext(Util.calcStateMachine(assignCode.getJSONObject("alternate")));
                    setUpPreStates(codeEntry.getTestNext(), codeEntry);
                    setUpPreStates(codeEntry.getTestNotNext(), codeEntry);
                } else {
                    System.out.println("状态机操作语句不识别");
                    isGraphValid = false;
                }
                return;
            }
        }
    }

    private void setUpPreStates(List<Integer> nextState, CodeEntry codeEntry) {
        CodeEntry nextStateEntry = calcEntry(nextState);
        if (nextStateEntry == null) {
            return;
        }
        nextStateEntry.getPreStates().add(codeEntry.getEntryID());

    }

    private CodeEntry calcEntry(List<Integer> entryID) {
        for (CodeEntry codeEntry : nodes.values()) {
            if (isEntryEqual(entryID, codeEntry.getEntryID())) {
                return codeEntry;
            }
        }
        return null;
    }

    private boolean isEntryEqual(List<Integer> id1, List<Integer> id2) {
        // curl "http://127.0.0.1:8889?fun=compare&id1=40_36_36&id2=26_12_36"
        return StringUtils.endsWithIgnoreCase(HttpInvoker.get("http://127.0.0.1:8889/?id1=" + StringUtils.join(id1, "_")
                + "&id2=" + StringUtils.join(id2, "_") + "&fun=compare"), "true");
    }

    public List<Integer> getInitState() {
        return initState;
    }

    public void setInitState(List<Integer> initState) {
        this.initState = initState;
    }

    public List<Integer> getExitState() {
        return exitState;
    }

    public void setExitState(List<Integer> exitState) {
        this.exitState = exitState;
    }

    public Map<String, CodeEntry> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, CodeEntry> nodes) {
        this.nodes = nodes;
    }

    public boolean isGraphValid() {
        return isGraphValid;
    }
}
