package com.br.auction.analytics.assistant;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

/**
 * Provedor de IA via <b>Claude CLI</b> local (modo headless {@code claude -p}). O
 * prompt vai por stdin (evita limite/escape de argumentos no Windows) e a resposta
 * sai no stdout. Nao usa token; o binario e resolvido no PATH.
 */
@Component
public class ClaudeCliCompletion implements AiProviderClient {

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final String DEFAULT_COMMAND = "claude";

    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "bi-assistant-claude");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public AiProvider provider() {
        return AiProvider.CLAUDE_CLI;
    }

    @Override
    public String complete(AiSettings settings, String systemPrompt, String userPrompt) {
        String command = command(settings);
        int timeoutSeconds = settings.getTimeoutSeconds() > 0 ? settings.getTimeoutSeconds() : 120;
        String prompt = systemPrompt + "\n\n" + userPrompt;
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> runClaude(command, prompt), executor);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AiUnavailableException("Claude CLI: timeout de " + timeoutSeconds + "s.", e);
        } catch (Exception e) {
            throw new AiUnavailableException("Claude CLI indisponivel: " + e.getMessage(), e);
        }
    }

    private String runClaude(String command, String prompt) {
        try {
            Process process = newProcess(command, List.of("-p", "--output-format", "text"));
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(prompt.getBytes(StandardCharsets.UTF_8));
            }
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("claude saiu com codigo " + code);
            }
            return out.trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Process newProcess(String command, List<String> claudeArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        if (IS_WINDOWS) {
            cmd.add("cmd.exe");
            cmd.add("/c");
        }
        cmd.add(command);
        cmd.addAll(claudeArgs);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        // Roda fora do repositorio para nao herdar CLAUDE.md/hooks/MCP do projeto.
        builder.directory(new java.io.File(System.getProperty("java.io.tmpdir", ".")));
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        return builder.start();
    }

    /** Disponivel se o CLI responde a {@code --version} rapidamente. */
    @Override
    public boolean isAvailable(AiSettings settings) {
        try {
            Process process = newProcess(command(settings), List.of("--version"));
            process.getOutputStream().close();
            process.getInputStream().readAllBytes();
            return process.waitFor(8, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String command(AiSettings settings) {
        String command = settings.getClaudeCommand();
        return command == null || command.isBlank() ? DEFAULT_COMMAND : command.trim();
    }
}
