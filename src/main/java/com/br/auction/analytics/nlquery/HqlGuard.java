package com.br.auction.analytics.nlquery;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Blindagem do HQL gerado pela IA. Garante uma unica consulta de leitura
 * ({@code SELECT}) sobre entidades permitidas: bloqueia escrita (update/delete/
 * insert), multiplos comandos e entidades fora da whitelist. Sem SQL nativo —
 * portavel.
 */
@Component
public class HqlGuard {

    private static final Pattern FORBIDDEN = Pattern.compile("\\b(update|delete|insert)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Captura o alvo apos FROM/JOIN; alvos com ponto sao navegacao de associacao. */
    private static final Pattern ENTITY_REF = Pattern.compile("\\b(from|join)\\s+([A-Za-z_][A-Za-z0-9_\\.]*)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Valida o HQL. {@code allowed} e o conjunto de entidades consultaveis
     * (curadas no modo normal; todas no bypass).
     */
    public String sanitize(String rawHql, Set<String> allowed) {
        if (rawHql == null || rawHql.isBlank()) {
            throw new HqlGuardException("Consulta vazia.");
        }
        String hql = rawHql.trim();
        while (hql.endsWith(";")) {
            hql = hql.substring(0, hql.length() - 1).trim();
        }
        if (hql.contains(";")) {
            throw new HqlGuardException("Apenas um comando e permitido (sem ';').");
        }
        if (!hql.toLowerCase().startsWith("select")) {
            throw new HqlGuardException("A consulta deve comecar com SELECT.");
        }
        if (FORBIDDEN.matcher(hql).find()) {
            throw new HqlGuardException("Operacao nao permitida. Somente leitura.");
        }
        Matcher matcher = ENTITY_REF.matcher(hql);
        boolean referencedFrom = false;
        while (matcher.find()) {
            String keyword = matcher.group(1).toLowerCase();
            String target = matcher.group(2);
            if (target.contains(".")) {
                // navegacao de associacao (ex.: join v.imagens) — nao e entidade-raiz.
                continue;
            }
            if (keyword.equals("from")) {
                referencedFrom = true;
            }
            if (!containsIgnoreCase(allowed, target)) {
                throw new HqlGuardException("Entidade nao permitida: '" + target + "'. Use apenas: "
                        + String.join(", ", allowed) + ".");
            }
        }
        if (!referencedFrom) {
            throw new HqlGuardException("A consulta precisa referenciar uma entidade no FROM.");
        }
        return hql;
    }

    private boolean containsIgnoreCase(Set<String> set, String value) {
        for (String s : set) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
