package com.stackus.dto;

/** name은 선택값이다. 비어있으면 서버가 기본 이름을 붙인다. */
public record CreateRoomRequest(String name) {
}
