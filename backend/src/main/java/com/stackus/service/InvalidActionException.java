package com.stackus.service;

public class InvalidActionException extends RuntimeException {

	public InvalidActionException(String detail) {
		super("Invalid action: " + detail);
	}
}
