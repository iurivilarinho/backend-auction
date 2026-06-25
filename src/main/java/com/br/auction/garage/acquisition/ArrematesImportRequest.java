package com.br.auction.garage.acquisition;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Importacao de arremates a partir do HTML da pagina /arremates (logada)")
public class ArrematesImportRequest {

	@Schema(description = "HTML da pagina de arremates do painel (copiado enquanto logado)")
	private String html;

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}
}
