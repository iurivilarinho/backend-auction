package com.br.auction.integration.connector.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.br.auction.integration.mapping.FieldMapping;

/**
 * Motor de transformacao do de->para: aplica as regras de {@link FieldMapping} sobre o
 * payload da fonte e produz o payload destino (no formato esperado pelos modelos internos).
 *
 * <p>Suporta um pipeline de transformacoes encadeadas separadas por '|'
 * (ex.: "TRIM|UPPER|LEFT_PAD:11:0").</p>
 */
@Component
public class FieldMapper {

	private static final String FIELD_REF_PREFIX = "$.";

	public Map<String, Object> map(Map<String, Object> source, List<FieldMapping> mappings) {
		Map<String, Object> target = new LinkedHashMap<>();
		if (mappings == null || mappings.isEmpty()) {
			return target;
		}
		for (FieldMapping mapping : mappings) {
			Object raw = JsonPaths.get(source, mapping.getSourceField());
			if (isMissing(raw) && mapping.getDefaultValue() != null) {
				raw = mapping.getDefaultValue();
			}
			if (raw == null && Boolean.TRUE.equals(mapping.getRequired())) {
				throw new IllegalArgumentException(
						"Campo obrigatorio ausente: " + mapping.getSourceField() + " -> " + mapping.getTargetField());
			}
			Object transformed = applyTransform(raw, mapping.getTransform(), source);
			JsonPaths.set(target, mapping.getTargetField(), transformed);
		}
		return target;
	}

	public Object applyTransform(Object value, String transform, Map<String, Object> source) {
		if (transform == null || transform.isBlank()) {
			return value;
		}
		Object current = value;
		for (String step : transform.split("\\|")) {
			String trimmed = step.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			current = applySingleTransform(current, trimmed, source);
		}
		return current;
	}

	private Object applySingleTransform(Object value, String transform, Map<String, Object> source) {
		int sepIndex = transform.indexOf(':');
		String name;
		String[] args;
		if (sepIndex < 0) {
			name = transform;
			args = new String[0];
		} else {
			name = transform.substring(0, sepIndex);
			String rest = transform.substring(sepIndex + 1);
			args = rest.split(":", -1);
		}
		String upperName = name.toUpperCase(Locale.ROOT);
		if (value == null && !"COALESCE".equals(upperName)) {
			return null;
		}
		return switch (upperName) {
			case "TRIM" -> String.valueOf(value).trim();
			case "UPPER" -> String.valueOf(value).toUpperCase(Locale.ROOT);
			case "LOWER" -> String.valueOf(value).toLowerCase(Locale.ROOT);
			case "ONLY_DIGITS" -> String.valueOf(value).replaceAll("\\D", "");
			case "TO_INT" -> Integer.parseInt(String.valueOf(value).trim());
			case "TO_LONG" -> Long.parseLong(String.valueOf(value).trim());
			case "TO_DECIMAL" -> new BigDecimal(String.valueOf(value).trim());
			case "TO_BOOLEAN" -> parseBoolean(String.valueOf(value));
			case "MONEY_BR" -> parseMoneyBr(String.valueOf(value));
			case "TO_LIST" -> Arrays.stream(String.valueOf(value).split(","))
					.map(String::trim)
					.filter(item -> !item.isEmpty())
					.toList();
			case "NULL_IF_BLANK" -> {
				String t = String.valueOf(value).trim();
				yield t.isEmpty() ? null : t;
			}
			case "NULL_IF_NOT_EMAIL" -> {
				String t = String.valueOf(value).trim();
				yield t.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") ? t : null;
			}
			case "CONCAT" -> applyConcat(value, args, source);
			case "REPLACE" -> applyReplace(value, args);
			case "LEFT_PAD" -> applyLeftPad(value, args);
			case "RIGHT_PAD" -> applyRightPad(value, args);
			case "SUBSTRING" -> applySubstring(value, args);
			case "FORMAT_DATE" -> applyFormatDate(value, args);
			case "COALESCE" -> applyCoalesce(value, args, source);
			default -> value;
		};
	}

	private String applyConcat(Object value, String[] args, Map<String, Object> source) {
		if (args.length == 0) {
			return String.valueOf(value);
		}
		String separator = args[0];
		StringBuilder sb = new StringBuilder();
		sb.append(value == null ? "" : String.valueOf(value));
		for (int i = 1; i < args.length; i++) {
			Object resolved = resolveArg(args[i], source);
			if (resolved == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(separator);
			}
			sb.append(String.valueOf(resolved));
		}
		return sb.toString();
	}

	private String applyReplace(Object value, String[] args) {
		if (args.length < 1) {
			return String.valueOf(value);
		}
		String from = args[0];
		String to = args.length >= 2 ? args[1] : "";
		return String.valueOf(value).replace(from, to);
	}

	private String applyLeftPad(Object value, String[] args) {
		String text = String.valueOf(value);
		if (args.length < 1) {
			return text;
		}
		int length = parseIntSafe(args[0], text.length());
		char padChar = args.length >= 2 && !args[1].isEmpty() ? args[1].charAt(0) : ' ';
		if (text.length() >= length) {
			return text;
		}
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length - text.length(); i++) {
			sb.append(padChar);
		}
		sb.append(text);
		return sb.toString();
	}

	private String applyRightPad(Object value, String[] args) {
		String text = String.valueOf(value);
		if (args.length < 1) {
			return text;
		}
		int length = parseIntSafe(args[0], text.length());
		char padChar = args.length >= 2 && !args[1].isEmpty() ? args[1].charAt(0) : ' ';
		if (text.length() >= length) {
			return text;
		}
		StringBuilder sb = new StringBuilder(length);
		sb.append(text);
		while (sb.length() < length) {
			sb.append(padChar);
		}
		return sb.toString();
	}

	private String applySubstring(Object value, String[] args) {
		String text = String.valueOf(value);
		if (args.length < 1) {
			return text;
		}
		int start = parseIntSafe(args[0], 0);
		int end = args.length >= 2 && !args[1].isBlank() ? parseIntSafe(args[1], text.length()) : text.length();
		if (start < 0) {
			start = 0;
		}
		if (end > text.length()) {
			end = text.length();
		}
		if (start >= end || start >= text.length()) {
			return "";
		}
		return text.substring(start, end);
	}

	private Object applyFormatDate(Object value, String[] args) {
		if (args.length < 2) {
			return value;
		}
		String toPattern = args[args.length - 1];
		StringBuilder fromBuilder = new StringBuilder(args[0]);
		for (int i = 1; i < args.length - 1; i++) {
			fromBuilder.append(':').append(args[i]);
		}
		String fromPattern = fromBuilder.toString();
		String text = String.valueOf(value);
		try {
			DateTimeFormatter from = DateTimeFormatter.ofPattern(fromPattern);
			DateTimeFormatter to = DateTimeFormatter.ofPattern(toPattern);
			try {
				LocalDateTime dateTime = LocalDateTime.parse(text, from);
				return dateTime.format(to);
			} catch (DateTimeParseException ignored) {
				LocalDate date = LocalDate.parse(text, from);
				return date.format(to);
			}
		} catch (DateTimeParseException | IllegalArgumentException ex) {
			return value;
		}
	}

	private Object applyCoalesce(Object value, String[] args, Map<String, Object> source) {
		if (!isEmpty(value)) {
			return value;
		}
		for (String arg : args) {
			Object resolved = resolveArg(arg, source);
			if (!isEmpty(resolved)) {
				return resolved;
			}
		}
		return null;
	}

	private Object resolveArg(String arg, Map<String, Object> source) {
		if (arg == null) {
			return null;
		}
		if (arg.startsWith(FIELD_REF_PREFIX)) {
			return JsonPaths.get(source, arg.substring(FIELD_REF_PREFIX.length()));
		}
		return arg;
	}

	/**
	 * Converte um valor monetario no formato brasileiro ("R$ 12.345,67") em BigDecimal.
	 * Util para mapear lances e valores FIPE vindos da fonte.
	 */
	private BigDecimal parseMoneyBr(String value) {
		String normalized = value.replace("R$", "").replace(".", "").replace(",", ".").trim();
		if (normalized.isBlank()) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(normalized);
		} catch (NumberFormatException ex) {
			return BigDecimal.ZERO;
		}
	}

	private boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String text) {
			return text.isEmpty();
		}
		return false;
	}

	private int parseIntSafe(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	private boolean isMissing(Object value) {
		return value == null || value instanceof String text && text.isBlank();
	}

	private boolean parseBoolean(String text) {
		String normalized = text.trim().toLowerCase(Locale.ROOT);
		return normalized.equals("true") || normalized.equals("1") || normalized.equals("s") || normalized.equals("sim")
				|| normalized.equals("y") || normalized.equals("yes");
	}
}
