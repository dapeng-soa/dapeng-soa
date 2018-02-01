package com.github.dapeng.core.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version
 *
 * @author craneding
 * @date 16/3/24
 */
public class Version {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(\\.(\\d+))?(\\.(\\d+))?(.*)");

    private final String fullName;
    private final int majorNum;
    private final int minorNum;
    private final int patchNum;

    private Version(String fullName, int majorNum, int minorNum, int patchNum) {
        this.fullName = fullName;
        this.majorNum = majorNum;
        this.minorNum = minorNum;
        this.patchNum = patchNum;
    }

    /**
     * 只能调更新的接口，不能调旧的接口
     *
     * @param required
     * @return
     */
    public boolean compatibleTo(Version required) {
        if (this.majorNum != required.majorNum) return false;

        if (this.minorNum > required.minorNum) return false;

        if (this.minorNum == required.minorNum) return this.patchNum <= required.patchNum;

        return true;
    }

    public static Version toVersion(String fullName) {
        Matcher matcher = VERSION_PATTERN.matcher(fullName);

        if (!matcher.matches())
            throw new IllegalArgumentException("版本格式错误:" + fullName);

        String majorName = matcher.group(1);
        String minorName = matcher.group(3);
        String patchName = matcher.group(5);
        //String others = matcher.group(6);

        if (minorName == null || minorName.trim().isEmpty())
            minorName = "0";
        if (patchName == null || patchName.trim().isEmpty())
            patchName = "0";

        return new Version(fullName, Integer.parseInt(majorName), Integer.parseInt(minorName), Integer.parseInt(patchName));
    }

    @Override
    public String toString() {
        return fullName;
    }

    public static void main(String[] args) {

        System.out.println(Version.toVersion("1.0.0").compatibleTo(Version.toVersion("1.1.0-SNAPSHOT")));

    }

}
