package com.numbaseball.dto;

import java.util.List;

/** 클라이언트가 제출하는 추측. 자릿수·범위·중복 검증은 모두 서버가 한다. */
public record GuessMessage(List<Integer> digits) {
}
