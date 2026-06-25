package com.br.auction.garage.acquisition;

import com.br.auction.garage.enums.AcquisitionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Atualizacao de status do veiculo adquirido")
public class AcquisitionStatusUpdateRequest {

	@NotNull(message = "O status e obrigatorio")
	private AcquisitionStatus status;

	public AcquisitionStatus getStatus() {
		return status;
	}

	public void setStatus(AcquisitionStatus status) {
		this.status = status;
	}
}
