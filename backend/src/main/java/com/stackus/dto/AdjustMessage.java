package com.stackus.dto;

/** 클라이언트가 보내는 조작 요청. digitIndex는 자리(0부터), direction은 "UP" 또는 "DOWN". */
public record AdjustMessage(int digitIndex, String direction) {
}
