package com.mallmini.member.enums;

public enum MemberLevel {
    NORMAL("普通会员", 0),
    SILVER("银卡会员", 100),
    GOLD("金卡会员", 500),
    PLATINUM("钻石会员", 2000);

    private final String label;
    private final int threshold;

    MemberLevel(String label, int threshold) {
        this.label = label;
        this.threshold = threshold;
    }

    public String label() {
        return label;
    }

    public int threshold() {
        return threshold;
    }

    public static MemberLevel fromTotal(int totalPoints) {
        MemberLevel result = NORMAL;
        for (MemberLevel level : values()) {
            if (totalPoints >= level.threshold) {
                result = level;
            }
        }
        return result;
    }

    public MemberLevel next() {
        return switch (this) {
            case NORMAL -> SILVER;
            case SILVER -> GOLD;
            case GOLD -> PLATINUM;
            case PLATINUM -> null;
        };
    }
}
