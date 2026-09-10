package com.fedstack.spending.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * Contract placeholder for a future AI-generated insight stream. Emits only
 * deterministic, hard-coded text; makes no LLM call and persists nothing.
 */
@RestController
@RequestMapping("/api/v1/insights")
public class InsightStreamController {
	private static final List<String> PLACEHOLDER_CHUNKS = List.of(
			"Reviewing this month's spending...",
			"This is placeholder insight text. No model was called."
	);

	@PostMapping(path = "/stream", produces = "text/event-stream")
	SseEmitter stream() {
		SseEmitter emitter = new SseEmitter();
		try {
			for (String chunk : PLACEHOLDER_CHUNKS) {
				emitter.send(SseEmitter.event().name("insight").data(chunk));
			}
			emitter.send(SseEmitter.event().name("done").data("complete"));
			emitter.complete();
		} catch (IOException exception) {
			emitter.completeWithError(exception);
		}
		return emitter;
	}
}
