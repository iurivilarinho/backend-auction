package com.br.auction.integration.connector.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitario de navegacao em estruturas JSON (Map/List) usando dot-path com suporte a
 * indices de array (ex.: "cliente.endereco.cidade", "itens[0].id", "itens[*].id").
 */
public final class JsonPaths {

	private static final Pattern ARRAY_SEGMENT = Pattern.compile("^\\[(\\d+|\\*)\\]$");

	private JsonPaths() {
	}

	public static Object get(Map<String, Object> source, String path) {
		if (source == null || path == null || path.isBlank()) {
			return null;
		}
		String normalized = path.replaceAll("(?<!\\.)\\[", ".[");
		if (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}
		String[] parts = normalized.split("\\.");
		return resolve(source, parts, 0);
	}

	@SuppressWarnings("unchecked")
	private static Object resolve(Object current, String[] parts, int index) {
		for (int i = index; i < parts.length; i++) {
			if (current == null) {
				return null;
			}
			String part = parts[i];
			Matcher arrayMatcher = ARRAY_SEGMENT.matcher(part);
			if (arrayMatcher.matches()) {
				if (!(current instanceof List<?> list)) {
					return null;
				}
				String token = arrayMatcher.group(1);
				if ("*".equals(token)) {
					if (list.isEmpty()) {
						return null;
					}
					if (i == parts.length - 1) {
						for (Object element : list) {
							if (element != null) {
								return element;
							}
						}
						return null;
					}
					for (Object element : list) {
						Object resolved = resolve(element, parts, i + 1);
						if (resolved != null) {
							return resolved;
						}
					}
					return null;
				}
				int idx = Integer.parseInt(token);
				if (idx < 0 || idx >= list.size()) {
					return null;
				}
				current = list.get(idx);
				continue;
			}
			if (current instanceof Map<?, ?> map) {
				current = ((Map<String, Object>) map).get(part);
			} else if (current instanceof List<?> list && isInteger(part)) {
				int idx = Integer.parseInt(part);
				if (idx < 0 || idx >= list.size()) {
					return null;
				}
				current = list.get(idx);
			} else {
				return null;
			}
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	public static void set(Map<String, Object> target, String path, Object value) {
		if (target == null || path == null || path.isBlank()) {
			return;
		}
		String[] parts = path.split("\\.");
		Map<String, Object> current = target;
		for (int i = 0; i < parts.length - 1; i++) {
			Object next = current.get(parts[i]);
			if (!(next instanceof Map)) {
				Map<String, Object> nested = new LinkedHashMap<>();
				current.put(parts[i], nested);
				current = nested;
			} else {
				current = (Map<String, Object>) next;
			}
		}
		current.put(parts[parts.length - 1], value);
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> asItemList(Object value) {
		if (value instanceof List<?> list) {
			List<Map<String, Object>> result = new ArrayList<>(list.size());
			for (Object element : list) {
				if (element instanceof Map<?, ?> map) {
					result.add((Map<String, Object>) map);
				}
			}
			return result;
		}
		return List.of();
	}

	public static Map<String, Object> newMap() {
		return new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public static void remove(Map<String, Object> target, String path) {
		if (target == null || path == null || path.isBlank()) {
			return;
		}
		String[] parts = path.split("\\.");
		Map<String, Object> current = target;
		for (int i = 0; i < parts.length - 1; i++) {
			Object next = current.get(parts[i]);
			if (!(next instanceof Map)) {
				return;
			}
			current = (Map<String, Object>) next;
		}
		current.remove(parts[parts.length - 1]);
	}

	private static boolean isInteger(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isDigit(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
