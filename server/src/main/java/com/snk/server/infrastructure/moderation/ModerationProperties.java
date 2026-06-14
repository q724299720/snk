package com.snk.server.infrastructure.moderation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snk.moderation")
public class ModerationProperties {

	private final AutoAudit autoAudit = new AutoAudit();

	public AutoAudit getAutoAudit() {
		return autoAudit;
	}

	public static class AutoAudit {

		private boolean enabled = true;
		private int pendingAgeHours = 24;
		private long fixedDelayMs = 3_600_000L;
		private long initialDelayMs = 300_000L;
		private List<String> rejectKeywords = new ArrayList<>(List.of(
			"test",
			"junk",
			"garbage",
			"unknown",
			"asdf",
			"qwer"
		));

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getPendingAgeHours() {
			return pendingAgeHours;
		}

		public void setPendingAgeHours(int pendingAgeHours) {
			this.pendingAgeHours = pendingAgeHours;
		}

		public long getFixedDelayMs() {
			return fixedDelayMs;
		}

		public void setFixedDelayMs(long fixedDelayMs) {
			this.fixedDelayMs = fixedDelayMs;
		}

		public long getInitialDelayMs() {
			return initialDelayMs;
		}

		public void setInitialDelayMs(long initialDelayMs) {
			this.initialDelayMs = initialDelayMs;
		}

		public List<String> getRejectKeywords() {
			return rejectKeywords;
		}

		public void setRejectKeywords(List<String> rejectKeywords) {
			this.rejectKeywords = rejectKeywords == null ? new ArrayList<>() : new ArrayList<>(rejectKeywords);
		}
	}
}
