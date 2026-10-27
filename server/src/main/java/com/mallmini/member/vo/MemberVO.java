package com.mallmini.member.vo;

public record MemberVO(
        int points,
        int totalPoints,
        String level,
        String levelLabel,
        Integer nextLevelPoints,
        String nextLevelLabel
) {
}
