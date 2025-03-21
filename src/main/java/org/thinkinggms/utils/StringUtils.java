package org.thinkinggms.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private final static char[] first = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
    private final static char[] second = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
    private final static char[] third = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();

    private static final JsonArray rules = FileUtils.getRules();

    public static String censor(String s) {
        for (JsonElement rule : rules) {
            for (JsonElement singleRule : rule.getAsJsonObject().getAsJsonArray("elements")) {
                Matcher matcher = Pattern
                        .compile(singleRule.getAsJsonObject().get("regex").getAsString())
                        .matcher(normalize(singleRule.getAsJsonObject().has("separate") ? divideKorean(s) : s));
                Matcher matcher2 = Pattern
                        .compile(singleRule.getAsJsonObject().get("regex").getAsString())
                        .matcher(singleRule.getAsJsonObject().has("separate") ? divideKorean(s) : s);
                if (matcher.find() || matcher2.find()) return singleRule.getAsJsonObject().get("name").getAsString();
            }
        }
        return null;
    }

    public static @NotNull String normalize(@NotNull String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC)
                // 일반 기호 및 숫자 제거
                .replaceAll("[!?@#$%^&*():;+-=~{}<>_\\[\\]|\\\\\"',./`₩\\d]", "")

                // 특수 기호 제거
                // U+2000-U+206F 일반적으로 사용되는 문장 부호
                // U+2070—U+209F 첨자 및 아래 첨자
                // U+20A0—U+20CF 통화 기호
                // U+20D0—U+20FF 마크와 함께
                // U+2100—U+214F 수식 기호 문자
                // U+2150—U+218F 디지털 형태
                // ...
                // U+2B00—U+2BFF 기타 기호와 화살표
                // U+FF00—U+FFEF 반 폭 및 전체 폭 양식
                // U+3000—U+303F CJK 기호 및 구두점
                // U+00A0-U+00BB 라틴-1 보충: 라틴어 1 문장 부호 및 기호
                // U+00F7 라틴-1 보충: 나눗셈 기호
                // U+00D7 라틴-1 보충: 곱셈 기호
                .replaceAll("[\\u2000-\\u2BFF\\uFF00-\\uFFEF\\u3000-\\u303F\\u00A0-\\u00BB\\u00F7\\u00D7]", "")

                // 공백 문자 제거
                // U+200B 제로 너비 공간
                // U+115F 한글 조성 필러
                // U+1160 한글 정성 필러
                // U+3164 한글 필러
                // U+FFA0 반쪽 한글 필러
                // U+2800 점자 패턴 공백
                // U+17B5 크메르어 모음 고유의 Aa
                // U+1CBB, U+1CBC Georgian Extended: 빈 셀
                .replaceAll("[\\s\\t\\d\\u200B\\u115F\\u1160\\u3164\\uFFA0\\u2800\\u17B5\\u1CBB\\u1CBC]", "")

                // 숫자
                .replaceAll("\\d", "");
    }
    public static char[] divideKorean(char c) {
        if (!isKorean(c)) return new char[] {c};
        if (isSingle(c)) return new char[] {c};
        char[] result = new char[hasSupport(c) ? 3 : 2];
        int d = c - 44032;
        if (hasSupport(c)) {
            result[2] = third[d % 28 - 1];
        }
        result[1] = second[d / 28 % 21];
        result[0] = first[d / 588 % 19];
        return result;
    }
    public static String divideKorean(String s) {
        char[] chars = s.toCharArray();
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            result.append(new String(divideKorean(c)));
        }
        return result.toString();
    }
    public static boolean isSingle(char c) {
        return 12643 >= (int) c && (int) c >= 12593;
    }
    public static boolean isKorean(char c) {
        if (isSingle(c)) return true;
        return 55203 >= (int) c && (int) c >= 44032;
    }
    public static boolean hasSupport(char c) {
        if ((c - 44032) % 28 == 0) return false;
        return 55203 >= (int) c && (int) c >= 44032;
    }
}
