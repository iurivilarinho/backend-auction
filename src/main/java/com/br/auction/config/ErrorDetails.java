package com.br.auction.config;

import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ErrorDetails {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDetails.class);

	public static class ErrorResponse {
		private OffsetDateTime timestamp = OffsetDateTime.now();
		private List<String> message;

		public ErrorResponse(List<String> message) {
			this.message = message;
		}

		public List<String> getMessage() {
			return message;
		}

		public void setMessage(List<String> message) {
			this.message = message;
		}

		public OffsetDateTime getTimestamp() {
			return timestamp;
		}
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ErrorResponse> tratarErro404(EntityNotFoundException ex) {
		String msg = ex.getMessage() != null ? ex.getMessage() : "Recurso nao encontrado.";
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(List.of(msg)));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> tratarErro409(DataIntegrityViolationException ex) {
		String msg = ex.getMessage() != null ? ex.getMessage() : "Violacao de integridade dos dados.";
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(List.of(msg)));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> tratarErro403(AccessDeniedException ex) {
		String msg = ex.getMessage() != null ? ex.getMessage() : "Acesso negado.";
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(List.of(msg)));
	}

//	@ExceptionHandler(AuthenticationException.class)
//	public ResponseEntity<ErrorResponse> tratarErro401(AuthenticationException ex) {
//		String msg = ex.getMessage() != null ? ex.getMessage() : "Nao autenticado.";
//		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(List.of(msg)));
//	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
		List<String> errors = new ArrayList<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(err -> errors.add(err.getField() + ": " + err.getDefaultMessage()));
		ex.getBindingResult().getGlobalErrors()
				.forEach(err -> errors.add(err.getObjectName() + ": " + err.getDefaultMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(errors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
		List<String> errors = new ArrayList<>();
		ex.getConstraintViolations().forEach(v -> errors.add(v.getPropertyPath() + ": " + v.getMessage()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(errors));
	}

	// Opcional, mas util para padronizar respostas 500.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> tratarErro500(Exception ex) {
		LOGGER.error("Erro interno inesperado.", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse(List.of("Erro interno inesperado.")));
	}
}
