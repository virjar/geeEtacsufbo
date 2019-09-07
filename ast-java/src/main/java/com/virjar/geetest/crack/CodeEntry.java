package com.virjar.geetest.crack;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by virjar on 2017/9/8.
 */
public class CodeEntry {
    private List<Integer> entryID;// 路径ID，代表状态机状态ID
    private JSONArray codeSegment; // 对应的执行片段
    private boolean isReturnEntry = false;// 是否是结束语句
    private String sateControlValName;
    private List<Integer> singleNext;
    private List<Integer> testNext;
    private List<Integer> textNotNext;
    private JSONObject testCondition;
    private List<List<Integer>> preStates = Lists.newLinkedList();

    public List<Integer> getSingleNext() {
        return singleNext;
    }

    public void setSingleNext(List<Integer> singleNext) {
        this.singleNext = singleNext;
    }

    public List<Integer> getTestNext() {
        return testNext;
    }

    public void setTestNext(List<Integer> testNext) {
        this.testNext = testNext;
    }

    public List<Integer> getTestNotNext() {
        return textNotNext;
    }

    public void setTextNotNext(List<Integer> textNotNext) {
        this.textNotNext = textNotNext;
    }

    public JSONObject getTestCondition() {
        return testCondition;
    }

    public void setTestCondition(JSONObject testCondition) {
        this.testCondition = testCondition;
    }

    public List<Integer> getEntryID() {
        return entryID;
    }

    public void setEntryID(List<Integer> entryID) {
        this.entryID = entryID;
    }

    public JSONArray getCodeSegment() {
        return codeSegment;
    }

    public void setCodeSegment(JSONArray codeSegment) {
        if (codeSegment == null) {
            System.out.println("null");
        }
        this.codeSegment = codeSegment;

    }

    public boolean isReturnEntry() {
        return isReturnEntry;
    }

    public void setReturnEntry(boolean returnEntry) {
        isReturnEntry = returnEntry;
    }

    public String getSateControlValName() {
        return sateControlValName;
    }

    public void setSateControlValName(String sateControlValName) {
        this.sateControlValName = sateControlValName;
    }

    public List<List<Integer>> getPreStates() {
        return preStates;
    }
}
