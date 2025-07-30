package com.jinx.statistics.utility;

public class StringUtility {

    public static void appendLine(StringBuffer sb, String s, Object[] args) {
        sb.append(String.format(s, args)).append("\r\n");
    }

    public static String urlConcat(String delimiter, String[] elements){
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].endsWith("/")){
                elements[i] = elements[i].substring(0, elements[i].length()-1);
            }
        }
        return String.join(delimiter, elements);
    }
}
